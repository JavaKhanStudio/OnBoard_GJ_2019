package jks.verify;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;

/**
 * Boots the real game on a real OpenGL context and renders it, which is the only thing that
 * proves an engine upgrade actually worked: assets bind, shaders compile, the level loads and
 * something recognisable appears on screen.
 *
 * Tagged "gl" and excluded from the default test run, because it needs a display. Run it with
 * ./gradlew :verify:test -PwithGl
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GameRunTest
{
	// On the game's clock, not a frame index. Frame 150 was 2.5s at the 60fps this was
	// first recorded at; on a faster or slower surface - a headless compositor, say - the
	// same frame number lands somewhere else entirely in the parallax scroll.
	private static final double CAPTURE_AFTER_SECONDS = 2.5;
	private static final double EXIT_AFTER_SECONDS    = 2.9;
	private static final int    EXIT_FRAME            = 100_000;

	/** Loose on purpose - see Frames. This catches "the scene broke", not "a pixel moved". */
	private static final double MAX_DIFFERENCE = 0.12;

	// Resolved absolutely: the test runs with desktop/ as its working directory, matching the game.
	private static final File MODULE = new File(System.getProperty("onboard.verify"));
	private static final File GOLDEN = new File(MODULE, "src/test/resources/golden/level1.png");
	private static final File OUTPUT = new File(MODULE, "build/frames");

	private static GameHarness harness;

	@BeforeAll
	void runTheGame()
	{
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1280, 720);   // cage's headless output size; fits any real display too
		config.setTitle("On Board - verification run");
		config.setResizable(false);

		// Pin the entry point: the game now starts at the logos, and this test is about
		// level 1. The golden frame was recorded on libGDX 1.9.10 and must keep meaning
		// the same thing.
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, EXIT_FRAME);
		harness.captureAfterSeconds = CAPTURE_AFTER_SECONDS;
		harness.exitAfterSeconds = EXIT_AFTER_SECONDS;
		new Lwjgl3Application(harness, config);
	}

	@Test
	@DisplayName("the game boots, loads level 1 and renders without throwing")
	void runsCleanly()
	{
		if (harness.error != null)
			harness.error.printStackTrace();

		assertNull(harness.error, harness.error == null ? null
			: "the game threw during startup or rendering: " + harness.error);
		assertTrue(harness.gameSeconds >= CAPTURE_AFTER_SECONDS,
			"only simulated " + harness.gameSeconds + "s, expected at least " + CAPTURE_AFTER_SECONDS);
	}

	@Test
	@DisplayName("the rendered frame actually contains the scene")
	void frameIsNotBlank() throws Exception
	{
		BufferedImage frame = harness.capture;
		assertNotNull(frame, "no frame was captured");
		Frames.write(frame, new File(OUTPUT, "level1-actual.png"));

		double variety = Frames.distinctColourRatio(frame);
		assertTrue(variety > 0.25,
			"the frame is nearly uniform (distinct-colour ratio " + String.format("%.3f", variety)
			+ ") - the scene did not draw. See " + new File(OUTPUT, "level1-actual.png").getAbsolutePath());
	}

	@Test
	@DisplayName("level 1 still looks like level 1")
	void matchesGoldenFrame() throws Exception
	{
		BufferedImage frame = harness.capture;
		assertNotNull(frame, "no frame was captured");

		if (Boolean.getBoolean("onboard.golden.record") || !GOLDEN.isFile())
		{
			Frames.write(frame, GOLDEN);
			System.out.println("Recorded golden frame at " + GOLDEN.getAbsolutePath());
			return;
		}

		BufferedImage golden = Frames.read(GOLDEN);
		double difference = Frames.difference(golden, frame);

		if (difference > MAX_DIFFERENCE)
		{
			Frames.write(frame, new File(OUTPUT, "level1-actual.png"));
			Frames.write(golden, new File(OUTPUT, "level1-expected.png"));
		}

		assertTrue(difference <= MAX_DIFFERENCE, String.format(
			"level 1 renders differently: mean difference %.4f exceeds %.4f.%n"
			+ "  expected: %s%n  actual:   %s%n"
			+ "If the change is intentional, re-record with -PwithGl -PrecordGolden.",
			difference, MAX_DIFFERENCE,
			new File(OUTPUT, "level1-expected.png").getAbsolutePath(),
			new File(OUTPUT, "level1-actual.png").getAbsolutePath()));
	}
}

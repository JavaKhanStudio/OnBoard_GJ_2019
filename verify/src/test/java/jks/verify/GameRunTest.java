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
	private static final int CAPTURE_FRAME = 150;
	private static final int EXIT_FRAME    = 170;

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
		config.setWindowedMode(1600, 900);
		config.setTitle("On Board - verification run");
		config.setResizable(false);

		// Pin the entry point: the game now starts at the logos, and this test is about
		// level 1. The golden frame was recorded on libGDX 1.9.10 and must keep meaning
		// the same thing.
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		harness = new GameHarness(new Main_Application(), CAPTURE_FRAME, EXIT_FRAME);
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
		assertTrue(harness.framesRendered >= CAPTURE_FRAME,
			"only rendered " + harness.framesRendered + " frames, expected at least " + CAPTURE_FRAME);
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

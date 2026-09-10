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
 * The ending. Reached by finishing the fourth carriage, and branching on karma: leave, or
 * stay. startAtOutro sets karma to 4, so this covers the "leave" ending and the outro art
 * that goes with it - none of which anything else in the suite touches.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndingRenderTest
{
	// The ending fades up from black, so capture on the game's clock rather than a frame
	// index: at a fixed frame the fade sits wherever the machine's frame rate left it, and
	// the golden then swings by 40% between an idle run and a busy one.
	private static final double CAPTURE_AFTER_SECONDS = 4.0;
	private static final double EXIT_AFTER_SECONDS    = 4.5;
	private static final int    EXIT_FRAME            = 100_000;

	private static final double MAX_DIFFERENCE = 0.12;

	private static final File MODULE = new File(System.getProperty("onboard.verify"));
	private static final File GOLDEN = new File(MODULE, "src/test/resources/golden/ending.png");
	private static final File OUTPUT = new File(MODULE, "build/frames");

	private static GameHarness harness;

	@BeforeAll
	void runTheEnding()
	{
		Main_Application.startPoint = Main_Application.StartPoint.OUTRO;

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1280, 720);   // cage's headless output size; fits any real display too
		config.setTitle("On Board - ending verification");
		config.setResizable(false);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, EXIT_FRAME);
		harness.captureAfterSeconds = CAPTURE_AFTER_SECONDS;
		harness.exitAfterSeconds = EXIT_AFTER_SECONDS;
		new Lwjgl3Application(harness, config);
	}

	@Test
	@DisplayName("the ending renders without throwing")
	void endingRuns()
	{
		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null : "the ending threw: " + harness.error);
	}

	@Test
	@DisplayName("the ending still looks the same")
	void matchesGoldenEnding() throws Exception
	{
		BufferedImage frame = harness.capture;
		assertNotNull(frame, "no frame was captured");
		Frames.write(frame, new File(OUTPUT, "ending-actual.png"));

		if (Boolean.getBoolean("onboard.golden.record") || !GOLDEN.isFile())
		{
			Frames.write(frame, GOLDEN);
			System.out.println("Recorded golden ending frame at " + GOLDEN.getAbsolutePath());
			return;
		}

		double difference = Frames.difference(Frames.read(GOLDEN), frame);
		assertTrue(difference <= MAX_DIFFERENCE, String.format(
			"the ending renders differently: mean difference %.4f exceeds %.4f (see %s)",
			difference, MAX_DIFFERENCE, new File(OUTPUT, "ending-actual.png").getAbsolutePath()));
	}
}

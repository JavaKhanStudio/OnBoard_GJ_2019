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
import jks.vinterface.Block_Sound;
import jks.vinterface.GVars_UI;

/**
 * The start screen, and the sound options on it.
 *
 * Both are new ground for the suite: the menu only became reachable again when the opening
 * was switched back on, and the sound block only became real when the volume setting was
 * given something to control. VisUI widgets resolve their styles out of the skin, and this
 * project loads VisUI with libGDX's stock skin rather than VisUI's own, so a slider that
 * builds at all is worth asserting rather than assuming.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartScreenRenderTest
{
	private static final double CAPTURE_AFTER_SECONDS = 1.5;
	private static final double EXIT_AFTER_SECONDS    = 2.0;
	private static final double MAX_DIFFERENCE        = 0.12;

	private static final File MODULE = new File(System.getProperty("onboard.verify"));
	private static final File GOLDEN = new File(MODULE, "src/test/resources/golden/startscreen.png");
	private static final File OUTPUT = new File(MODULE, "build/frames");

	private static GameHarness harness;
	private static volatile Throwable soundBlockError;
	private static volatile Float soundBlockVolume;

	@BeforeAll
	void runTheStartScreen()
	{
		Main_Application.startPoint = Main_Application.StartPoint.START_SCREEN;

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1280, 720);
		config.setTitle("On Board - start screen verification");
		config.useVsync(false);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 100_000);
		harness.captureAfterSeconds = CAPTURE_AFTER_SECONDS;
		harness.exitAfterSeconds = EXIT_AFTER_SECONDS;
		harness.frameHook = frame ->
		{
			if (frame != 20 || soundBlockVolume != null) return;
			try
			{
				Block_Sound block = new Block_Sound();
				GVars_UI.mainUi.addActor(block);
				soundBlockVolume = block.chosenVolume();
			}
			catch (Throwable t)
			{
				soundBlockError = t;
				soundBlockVolume = -1f;
			}
		};

		new Lwjgl3Application(harness, config);
	}

	@Test
	@DisplayName("the start screen renders without throwing")
	void startScreenRuns()
	{
		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null
			: "the start screen threw: " + harness.error);
	}

	@Test
	@DisplayName("the sound options build against the skin actually in use")
	void soundBlockBuilds()
	{
		if (soundBlockError != null) soundBlockError.printStackTrace();
		assertNull(soundBlockError, soundBlockError == null ? null
			: "building the sound options threw - most likely a widget style the skin in use "
			+ "does not define: " + soundBlockError);
		assertNotNull(soundBlockVolume, "the sound block was never built");
		assertTrue(soundBlockVolume >= 0f && soundBlockVolume <= 1f,
			"reported volume out of range: " + soundBlockVolume);
	}

	@Test
	@DisplayName("the start screen still looks the same")
	void matchesGolden() throws Exception
	{
		BufferedImage frame = harness.capture;
		assertNotNull(frame, "no frame was captured");
		Frames.write(frame, new File(OUTPUT, "startscreen-actual.png"));

		if (Boolean.getBoolean("onboard.golden.record") || !GOLDEN.isFile())
		{
			Frames.write(frame, GOLDEN);
			System.out.println("Recorded golden start screen at " + GOLDEN.getAbsolutePath());
			return;
		}

		double difference = Frames.difference(Frames.read(GOLDEN), frame);
		assertTrue(difference <= MAX_DIFFERENCE, String.format(
			"the start screen renders differently: mean difference %.4f exceeds %.4f (see %s)",
			difference, MAX_DIFFERENCE, new File(OUTPUT, "startscreen-actual.png").getAbsolutePath()));
	}
}

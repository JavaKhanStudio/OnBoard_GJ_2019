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
import jks.vinterface.GVars_UI;
import jks.vinterface.tools.DialogBubble;
import jks.vinterface.tools.DialogBubble.DialogSize;

/**
 * Renders a speech bubble inside the running game.
 *
 * Dialogue is the whole point of this game and it is drawn by a third-party typing-label
 * widget, which is the single most upgrade-fragile thing in the project: markup tokens,
 * font metrics and wrapping all live in that library. The level-1 golden frame does not
 * cover it, because no bubble is on screen while Ross is just standing there.
 *
 * The text below deliberately uses the markup the real content uses: a colour token, the
 * EASE entry effect the game prefixes to every line, and the WAVE effect from wa1.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DialogRenderTest
{
	private static final String SAMPLE =
		DialogBubble.textStartUp + "Voila qui devrait remplir son {WAVE}estomac{ENDWAVE} !";

	// Timings on the game's clock rather than frame indices - the typing effect advances
	// on delta, so a frame number means different things at different frame rates.
	private static final double SHOW_AFTER_SECONDS    = 0.5;
	private static final double CAPTURE_AFTER_SECONDS = 3.3;
	private static final double EXIT_AFTER_SECONDS    = 3.7;
	private static final int    EXIT_FRAME            = 100_000;

	private static final double MAX_DIFFERENCE = 0.12;

	private static final File MODULE = new File(System.getProperty("onboard.verify"));
	private static final File GOLDEN = new File(MODULE, "src/test/resources/golden/dialog.png");
	private static final File OUTPUT = new File(MODULE, "build/frames");

	private static GameHarness harness;
	private static volatile Throwable bubbleError;
	private static volatile boolean shown;

	@BeforeAll
	void runTheGame()
	{
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1280, 720);   // cage's headless output size; fits any real display too
		config.setTitle("On Board - dialogue verification");
		config.setResizable(false);
		// The launcher's context, not LWJGL3's default. A GL 3.2 core profile has no
		// GL_LUMINANCE_ALPHA, so a grey+alpha PNG draws as a black square there while the
		// default context draws it fine - which is how the bubble went black unnoticed (r37).
		config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);

		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, EXIT_FRAME);
		harness.captureAfterSeconds = CAPTURE_AFTER_SECONDS;
		harness.exitAfterSeconds = EXIT_AFTER_SECONDS;
		harness.frameHook = frame ->
		{
			if (shown || harness.gameSeconds < SHOW_AFTER_SECONDS) return;
			shown = true;
			try
			{
				DialogBubble bubble = new DialogBubble(SAMPLE, DialogSize.BUBBLE_LARGE_TEXT_LARGE, true);
				bubble.setBubblePosition(560, 340);
				GVars_UI.mainUi.addActor(bubble);
			}
			catch (Throwable t)
			{
				bubbleError = t;
			}
		};

		new Lwjgl3Application(harness, config);
	}

	@Test
	@DisplayName("a speech bubble with markup builds and renders without throwing")
	void bubbleRenders()
	{
		if (bubbleError != null) bubbleError.printStackTrace();
		assertNull(bubbleError, bubbleError == null ? null
			: "building the dialogue bubble threw: " + bubbleError);

		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null
			: "the game threw while a bubble was on screen: " + harness.error);
	}

	@Test
	@DisplayName("the dialogue bubble still looks the same")
	void matchesGoldenDialog() throws Exception
	{
		BufferedImage frame = harness.capture;
		assertNotNull(frame, "no frame was captured");
		Frames.write(frame, new File(OUTPUT, "dialog-actual.png"));

		if (Boolean.getBoolean("onboard.golden.record") || !GOLDEN.isFile())
		{
			Frames.write(frame, GOLDEN);
			System.out.println("Recorded golden dialogue frame at " + GOLDEN.getAbsolutePath());
			return;
		}

		BufferedImage golden = Frames.read(GOLDEN);
		double difference = Frames.difference(golden, frame);

		if (difference > MAX_DIFFERENCE) Frames.write(golden, new File(OUTPUT, "dialog-expected.png"));

		assertTrue(difference <= MAX_DIFFERENCE, String.format(
			"the dialogue bubble renders differently: mean difference %.4f exceeds %.4f.%n"
			+ "  expected: %s%n  actual:   %s%n"
			+ "Text metrics and wrapping live in the typing-label library, so this is the test "
			+ "that moves when that library is swapped. Review the images before re-recording.",
			difference, MAX_DIFFERENCE,
			new File(OUTPUT, "dialog-expected.png").getAbsolutePath(),
			new File(OUTPUT, "dialog-actual.png").getAbsolutePath()));
	}
}

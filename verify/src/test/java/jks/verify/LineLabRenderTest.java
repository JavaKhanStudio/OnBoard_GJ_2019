package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;
import jks.index.Index_Text;
import jks.vars.GVars_Heart;
import jks.vue.models.Vue_LineLab;

/**
 * The line lab (r74) opens, lists a carriage's items with their lines, and flips to each of
 * the four carriages without throwing. One frame per carriage is kept for a person to look at;
 * the last has the cage's long line typed whole in the preview bubble.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LineLabRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;
	private static volatile BufferedImage first;
	private static volatile int width, height, carriagesShown;
	private static volatile Throwable error;

	@BeforeAll
	void openTheLab()
	{
		Main_Application.startPoint = Main_Application.StartPoint.LINE_LAB;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - line lab verification");
		gl.useVsync(false);

		int[] step = {0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 20;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				// A carriage a second, each shown for a frame or two before it is grabbed.
				double t = harness.gameSeconds;
				int carriage = step[0] / 2 + 1;
				if (step[0] % 2 == 0 && t >= 1.0 + step[0] * 0.5)
				{
					Vue_LineLab lab = (Vue_LineLab) GVars_Heart.vue;
					// The longest line, typed from the start: whole in the bubble by the last frame.
					if (step[0] == 0)
						lab.say(Index_Text.get("wa1.cage.message1"));
					lab.showCarriage(carriage);
					step[0]++;
				}
				else if (step[0] % 2 == 1 && t >= 1.0 + step[0] * 0.5)
				{
					BufferedImage frameImage = GameHarness.grab();
					if (first == null)
					{
						first = frameImage;
						width = Gdx.graphics.getWidth();
						height = Gdx.graphics.getHeight();
					}
					Frames.write(frameImage, new File(OUTPUT, "line-lab-carriage-" + carriage + ".png"));
					carriagesShown++;
					step[0]++;
					if (carriage == 4)
						Gdx.app.exit();
				}
			}
			catch (Throwable e)
			{
				if (error == null) error = e;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	@Test
	@DisplayName("the line lab shows each of the four carriages")
	void everyCarriageShows()
	{
		assertNull(harness.error, harness.error == null ? null : "the lab threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertNotNull(first, "no frame was taken");
		assertEquals(1280, width, "the window is not the size the frames assume");
		assertEquals(720, height, "the window is not the size the frames assume");
		assertEquals(4, carriagesShown, "not every carriage was shown");
		assertTrue(harness.framesRendered > 0);
	}
}

package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
import com.badlogic.gdx.math.Vector3;

import jks.amain.Main_Application;
import jks.camera.GVars_Camera;
import jks.input.GVars_Inputs;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.ItemOutline;
import jks.vue.models.game.WagonLevel;

/**
 * The four carriages with every item outlined at once (r71), so the colours can be judged
 * together: gold and unbroken for a piece of the key, blue and red strokes for the two sides of
 * a choice, yellow for the rest. Which item gets which is pinned by ItemOutlineKindTest; this
 * is the picture of it, both ends of each carriage, in build/frames/outline-kinds.png.
 *
 * Also checks that a lit carriage differs from an unlit one where it should: the lines draw.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemOutlineKindRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");
	private static final int CARRIAGES = GVars_Game.LEVEL_COUNT;
	/** nextLevel fades out for a second and in over two (r44). */
	private static final double SETTLE = 3.5;

	private static GameHarness harness;
	private static final BufferedImage[][] shots = new BufferedImage[CARRIAGES][2];
	private static final BufferedImage[] unlit = new BufferedImage[CARRIAGES];
	private static volatile Throwable error;

	@BeforeAll
	void walkEveryCarriageLit()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - item outline kinds");
		gl.useVsync(false);

		int[] step = {0};
		int[] carriage = {0};
		double[] due = {1.5};

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 1_000_000);
		harness.exitAfterSeconds = 120;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				if (harness.gameSeconds < due[0]) return;
				int c = carriage[0];
				switch (step[0])
				{
					case 0 :   // at the left end, unlit, then lit on the next frame
						ItemOutline.lightAll = false;
						unlit[c] = GameHarness.grab();
						ItemOutline.lightAll = true;
						step[0] = 1;
						break;
					case 1 :
						shots[c][0] = GameHarness.grab();
						GVars_Inputs.rightPressed = true;
						step[0] = 2;
						break;
					case 2 :   // until the view's right edge is on the end of the carriage
						if (viewRight() < WagonLevel.WIDTH - 1 && harness.gameSeconds < due[0] + 15) return;
						GVars_Inputs.rightPressed = false;
						due[0] = harness.gameSeconds + 0.5;
						step[0] = 3;
						break;
					case 3 :
						shots[c][1] = GameHarness.grab();
						ItemOutline.lightAll = false;
						if (c + 1 == CARRIAGES)
						{
							step[0] = 4;
							Gdx.app.exit();
							break;
						}
						carriage[0] = c + 1;
						GVars_Game.nextLevel();
						due[0] = harness.gameSeconds + SETTLE;
						step[0] = 0;
						break;
					default :
						break;
				}
			}
			catch (Throwable t)
			{
				if (error == null) error = t;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	private static float viewRight()
	{
		return GVars_Camera.camera.unproject(new Vector3(Gdx.graphics.getWidth(), 0, 0)).x;
	}

	@Test
	@DisplayName("every carriage is drawn at both ends with its items outlined by kind")
	void everyCarriageLit() throws Exception
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);

		int w = 640, h = 360;
		BufferedImage sheet = new BufferedImage(w * 2, h * CARRIAGES, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = sheet.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		for (int c = 0 ; c < CARRIAGES ; c++)
			for (int end = 0 ; end < 2 ; end++)
			{
				BufferedImage shot = shots[c][end];
				assertTrue(shot != null, "carriage " + (c + 1) + (end == 0 ? " left" : " right") + " end was never drawn");
				Frames.write(shot, new File(OUTPUT, "outline-kinds-wa" + (c + 1) + (end == 0 ? "-left" : "-right") + ".png"));
				g.drawImage(shot, end * w, c * h, w, h, null);
			}
		g.dispose();
		Frames.write(sheet, new File(OUTPUT, "outline-kinds.png"));

		for (int c = 0 ; c < CARRIAGES ; c++)
			assertTrue(Frames.difference(unlit[c], shots[c][0]) > 0.0005,
				"carriage " + (c + 1) + ": lighting every item changed nothing at its left end");
		assertEquals(false, ItemOutline.lightAll, "the test left every item lit");
	}
}

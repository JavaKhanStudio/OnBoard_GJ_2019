package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;

import jks.amain.Main_Application;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * The black bar under the carriage, and the carried items centred on it, follow a window resize
 * made mid-carriage (r109): the bar is measured in the pixels drawn, not read from the field that
 * sizes it, at the window the game opened at and again after the window changes size.
 *
 * cage, which the GL tests run in, keeps a window at its output's size, so there the resize never
 * happens and this test is SKIPPED, saying so. It runs for real through
 * tools/offscreen_resizable.sh (see there), which gives it an X server that lets windows resize.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WagonResizeRenderTest
{
	private static final String[] PICK = {"sac.png", "cube.png"};
	/** Opened at the first, resized to each of the others in turn: all 16:9, as the options offer. */
	private static final int[][] SIZES = {{1280, 720}, {960, 540}, {1152, 648}};

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;
	private static final List<int[]> seen = new ArrayList<>();
	private static volatile Throwable error;
	/** The window size asked for that never came: the display does not let windows resize. */
	private static volatile String stuck;

	@BeforeAll
	void resizeMidCarriage()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(SIZES[0][0], SIZES[0][1]);
		gl.setTitle("On Board - resize verification");
		gl.useVsync(false);
		gl.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);

		int[] step = {0};
		double[] at = {2.0}, asked = {0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 20;
		harness.frameHook = frame ->
		{
			if (error != null || harness.gameSeconds < at[0]) return;
			try
			{
				if (step[0] == 0)
					for (String name : PICK) GVars_Game.pickItem(item(name));
				else
				{
					int index = step[0] - 1;
					int[] size = SIZES[index];
					if (Gdx.graphics.getBackBufferWidth() != size[0])
					{
						// Not there yet: the window is still changing, or never will.
						if (harness.gameSeconds > asked[0] + 3)
						{
							stuck = size[0] + "x" + size[1] + " was asked, the window stayed "
								+ Gdx.graphics.getBackBufferWidth() + "x" + Gdx.graphics.getBackBufferHeight();
							Gdx.app.exit();
						}
						else
							at[0] = harness.gameSeconds + 0.5;
						return;
					}
					measure("resize-" + size[0] + "x" + size[1] + ".png");
					if (index + 1 == SIZES.length)
					{
						Gdx.app.exit();
						return;
					}
					Gdx.graphics.setWindowedMode(SIZES[index + 1][0], SIZES[index + 1][1]);
					asked[0] = harness.gameSeconds;
				}
				step[0]++;
				at[0] = harness.gameSeconds + 1.0;
			}
			catch (Throwable t)
			{
				if (error == null) error = t;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	/** {window width, window height, black rows at the bottom, row bottom, row top}. */
	private static void measure(String name) throws Exception
	{
		BufferedImage frame = GameHarness.grab();
		Frames.write(frame, new File(OUTPUT, name));

		// Up the left edge, clear of the centred row: black until the carriage's art starts.
		int black = 0;
		for (int y = frame.getHeight() - 1; y >= 0 && brightness(frame.getRGB(4, y)) < 12; y--)
			black++;

		float bottom = Float.MAX_VALUE, top = 0;
		for (Actor carried : GVars_Game.inventory.getChildren())
		{
			bottom = Math.min(bottom, carried.getY());
			top = Math.max(top, carried.getY() + carried.getHeight());
		}
		seen.add(new int[] {frame.getWidth(), frame.getHeight(), black, Math.round(bottom), Math.round(top)});
	}

	private static int brightness(int p)
	{
		return (((p >> 16) & 0xFF) + ((p >> 8) & 0xFF) + (p & 0xFF)) / 3;
	}

	private static GameItem item(String name)
	{
		for (GameItem item : GVars_Game.currentLevel.listItems)
			if (name.equals(item.name)) return item;
		throw new AssertionError(name + " is not in carriage " + GVars_Game.currentLevelInt);
	}

	@Test
	@DisplayName("after a window resize the bar is a ninth of the new height, and the carried row is centred on it")
	void barFollowsTheWindow()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assumeTrue(stuck == null, "the display would not resize the window (" + stuck + "): cage "
			+ "does that. Run this test through tools/offscreen_resizable.sh.");
		assertEquals(SIZES.length, seen.size(), "not every size was reached");

		for (int[] s : seen)
		{
			String at = s[0] + "x" + s[1] + ": ";
			System.out.println(at + "bar " + s[2] + " px, row " + s[3] + ".." + s[4]);
			assertTrue(Math.abs(s[2] - s[1] / 9f) <= 2, at + "the bar is " + s[2] + " px, not a ninth of " + s[1]);
			float middle = (s[3] + s[4]) / 2f;
			assertTrue(Math.abs(middle - s[2] / 2f) <= 2, at + "the row's middle is at " + middle + ", the bar's at " + s[2] / 2f);
			assertTrue(s[4] < s[2], at + "the row's top " + s[4] + " is above the bar");
		}
	}
}

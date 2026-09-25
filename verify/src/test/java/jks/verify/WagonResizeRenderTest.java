package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;

import jks.amain.Main_Application;
import jks.vinterface.GVars_UI;
import jks.vinterface.font.GVars_Font;
import jks.vinterface.font.Index_Fonts.Enum_Fonts;
import jks.vinterface.tools.DialogBubble;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * What is laid out from the window's size follows a window resize made mid-carriage: the black bar
 * and the carried row centred on it (r109), measured in the pixels drawn; the stage, the pause
 * plank and the key in their corners, and Ross's bubble with its font rasterised afresh (r117). Checked at the
 * window the game opened at and again after each change of size.
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
					if (seen.size() == index)
						measure("resize-" + size[0] + "x" + size[1] + ".png");
					if (index + 1 == SIZES.length)
					{
						// The pause screen over the resized carriage, for a person to look at.
						if (!jks.vars.GVars_Heart.isPaused)
						{
							jks.vars.GVars_Heart.togglePauseMenu();
							at[0] = harness.gameSeconds + 1.5;
							return;
						}
						Frames.write(GameHarness.grab(), new File(OUTPUT, "resize-pause-" + size[0] + "x" + size[1] + ".png"));
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

	/**
	 * {window width, window height, black rows at the bottom, row bottom, row top (both in pixels
	 * up from the window's bottom), stage width, stage height, plank x, plank top gap, plank
	 * width, bubble width, bubble font line height, 1 when the bubble types in the current font,
	 * the key's gap to the right edge, its gap to the top}.
	 */
	private static void measure(String name) throws Exception
	{
		BufferedImage frame = GameHarness.grab();
		Frames.write(frame, new File(OUTPUT, name));
		int h = frame.getHeight();

		// Up the left edge, clear of the centred row: black until the carriage's art starts.
		int black = 0;
		for (int y = h - 1; y >= 0 && brightness(frame.getRGB(4, y)) < 12; y--)
			black++;

		// The carried row as drawn: the rows of the bar holding anything but black.
		int bottom = Integer.MAX_VALUE, top = -1;
		for (int up = 0; up < black; up++)
			for (int x = 0; x < frame.getWidth(); x++)
				if (brightness(frame.getRGB(x, h - 1 - up)) > 40)
				{
					bottom = Math.min(bottom, up);
					top = Math.max(top, up + 1);
					break;
				}

		Stage stage = GVars_UI.mainUi;
		Actor pause = stage.getRoot().findActor("pauseButton");
		DialogBubble bubble = GVars_Game.dialogBubble;
		Font shipped = GVars_Font.buildTextraFont(Enum_Fonts.BUBBLE_LARGE_TEXT_MEDIUM);
		Field typing = DialogBubble.class.getDeclaredField("typing");
		typing.setAccessible(true);
		boolean current = ((TypingLabel) typing.get(bubble)).getFont() == shipped;

		seen.add(new int[] {frame.getWidth(), h, black, bottom, top,
			Math.round(stage.getWidth()), Math.round(stage.getHeight()),
			Math.round(pause.getX()), Math.round(stage.getHeight() - pause.getY() - pause.getHeight()), Math.round(pause.getWidth()),
			Math.round(bubble.getWidth()),
			Math.round(GVars_Font.buildLabel(Enum_Fonts.BUBBLE_LARGE_TEXT_MEDIUM).font.getLineHeight()),
			current ? 1 : 0,
			Math.round(stage.getWidth() - GVars_Game.clef.getX() - GVars_Game.clef.getWidth()),
			Math.round(stage.getHeight() - GVars_Game.clef.getY() - GVars_Game.clef.getHeight())});
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
	@DisplayName("after a window resize the bar, the carried row, the pause plank and the bubble all follow the new size")
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
			System.out.println(at + "bar " + s[2] + " px, row " + s[3] + ".." + s[4] + " px, stage " + s[5] + "x" + s[6]
				+ ", plank at " + s[7] + " from the left and " + s[8] + " from the top, " + s[9] + " wide"
				+ ", key " + s[13] + " from the right and " + s[14] + " from the top"
				+ ", bubble " + s[10] + " wide, its font " + s[11] + " px a line" + (s[12] == 1 ? "" : " (NOT the current one)"));
			assertTrue(Math.abs(s[2] - s[1] / 9f) <= 2, at + "the bar is " + s[2] + " px, not a ninth of " + s[1]);
			float middle = (s[3] + s[4]) / 2f;
			assertTrue(Math.abs(middle - s[2] / 2f) <= 2, at + "the row's middle is " + middle + " px up, the bar's " + s[2] / 2f);
			// Actors are placed in stage units: they are pixels only while the stage is the window.
			assertEquals(s[0], s[5], at + "the stage is " + s[5] + " wide: it is stretched, not resized");
			assertEquals(s[1], s[6], at + "the stage is " + s[6] + " high: it is stretched, not resized");
			float margin = s[0] / 100f;
			assertTrue(Math.abs(s[7] - margin) <= 1 && Math.abs(s[8] - margin) <= 1,
				at + "the plank is " + s[7] + " from the left and " + s[8] + " from the top, not in the " + margin + " px margin");
			assertTrue(Math.abs(s[9] - s[0] / 6.5f) <= 1, at + "the plank is " + s[9] + " wide, not a 6.5th of the window");
			assertTrue(Math.abs(s[10] - s[0] / 6.5f) <= 1, at + "the bubble is " + s[10] + " wide, not a 6.5th of the window");
			assertEquals(1, s[12], at + "the bubble still types in the font of another window size");
			assertTrue(Math.abs(s[13]) <= 1 && Math.abs(s[14] - s[0] / 100) <= 1,
				at + "the key is " + s[13] + " from the right edge and " + s[14] + " from the top, not in its corner");
		}
		// Re-rasterised, not scaled: the line height follows the window (FreeType rounds to whole pixels).
		for (int[] s : seen)
			assertTrue(Math.abs(s[11] / (float) s[1] - seen.get(0)[11] / (float) seen.get(0)[1]) < 0.003f,
				s[0] + "x" + s[1] + ": the bubble's font is " + s[11] + " px a line, not in proportion to "
				+ seen.get(0)[11] + " at " + seen.get(0)[1]);
	}
}

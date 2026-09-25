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

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.Vector3;

import jks.amain.Main_Application;
import jks.index.Index_Text;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * The choice items past the cage speak when used (r83). Until then only the cage had lines, and
 * the portrait, the hole in the wall and the glass took their use in silence.
 *
 * Throws the grenade into the hole in carriage 3 - its second use, the one that counts karma -
 * the way a click does, and checks the bubble types wa3.trou.message2.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChoiceLineRenderTest
{
	private static final int CARRIAGE = 3;
	private static final String KEY = "wa3.trou.message2";

	private static final double USE_AT   = 2.0;
	/** Long enough for the bubble to fade in and the line to be typed out. */
	private static final double SHOWN_AT = USE_AT + 6.0;

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;

	private static volatile BufferedImage before, said;
	private static volatile String named, shownText;
	private static volatile float shownAlpha;
	private static volatile float[] cloud;
	private static volatile Throwable error;

	@BeforeAll
	void throwTheGrenadeIntoTheHole()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = CARRIAGE;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - choice line verification");
		gl.useVsync(false);

		boolean[] done = {false, false};

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 60;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				if (!done[0] && harness.gameSeconds >= USE_AT)
				{
					done[0] = true;
					before = GameHarness.grab();
					Frames.write(before, new File(OUTPUT, "choice-line-before.png"));

					GameItem grenade = item("grenade.png"), trou = item("trou.png");
					named = trou.message_Crucial_2;
					GVars_Game.pickItem(grenade);
					GVars_Game.selectedItem = grenade;
					Vector3 middle = new Vector3(trou.posX + trou.objectTexture.getWidth() / 2f,
						trou.posY + trou.objectTexture.getHeight() / 2f, 0);
					trou.tryTouch(middle, grenade, false);
				}
				else if (done[0] && !done[1] && harness.gameSeconds >= SHOWN_AT)
				{
					done[1] = true;
					shownText = GVars_Game.dialogBubble.getText();
					shownAlpha = GVars_Game.dialogBubble.getColor().a;
					cloud = GVars_Game.dialogBubble.cloudBox();
					said = GameHarness.grab();
					Frames.write(said, new File(OUTPUT, "choice-line-said.png"));
					harness.exitAfterSeconds = harness.gameSeconds + 0.2;
				}
			}
			catch (Throwable t)
			{
				error = t;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	private static GameItem item(String name)
	{
		for (GameItem item : GVars_Game.currentLevel.listItems)
			if (name.equals(item.name)) return item;
		throw new AssertionError(name + " is not in carriage " + GVars_Game.currentLevelInt);
	}

	@Test
	@DisplayName("throwing the grenade into the hole types the hole's second line into the bubble")
	void theHoleSpeaks()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertNotNull(said, "no frame was taken after the grenade was used");

		assertEquals(KEY, named, "wa3.wa's trou.png does not name its second use line");
		assertEquals(Index_Text.get(KEY), shownText, "the bubble is not saying the hole's line");
		assertEquals(1f, shownAlpha, 0.01f, "the bubble is not fully shown");
		assertTrue(Frames.difference(before, said) > 0.002,
			"the frame did not change when the line was said - the bubble is not drawn");
	}

	/**
	 * Carriage 3 is the grown-up Ross, whose cloud already reached the top of the screen, and
	 * this is the longest line: the case where bigger letters (r121) could push the cloud off the
	 * screen. Measured in the frame's pixels: the runs of rows holding ink in the middle of the
	 * cloud, one run per line of text. The stage is the window, so its y goes up the frame.
	 */
	@Test
	@DisplayName("the hole's line is lettered big enough to read, in a cloud that stays on the screen")
	void theLineIsReadable()
	{
		assertNotNull(said, "no frame was taken after the grenade was used");
		int w = said.getWidth(), h = said.getHeight();
		int left = Math.round(cloud[0]), right = Math.round(cloud[0] + cloud[2]);
		int top = Math.round(h - cloud[1] - cloud[3]), bottom = Math.round(h - cloud[1]);
		String box = left + "," + top + " to " + right + "," + bottom + " of " + w + "x" + h;
		assertTrue(top >= 0, "the cloud runs off the top of the screen: " + box);
		assertTrue(left >= 0 && right <= w && bottom <= h, "the cloud runs off the screen: " + box);

		// Ink rows in the middle of the cloud, clear of its outline and its tail.
		int x0 = left + (right - left) / 4, x1 = right - (right - left) / 4;
		java.util.List<Integer> runs = new java.util.ArrayList<>();
		int run = 0;
		for (int y = top + (bottom - top) / 10; y < top + (bottom - top) * 3 / 4; y++)
		{
			boolean ink = false;
			for (int x = x0; x < x1 && !ink; x++)
			{
				int rgb = said.getRGB(x, y);
				ink = ((rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255)) < 240;
			}
			if (ink) run++;
			else if (run > 0) { runs.add(run); run = 0; }
		}
		if (run > 0) runs.add(run);
		java.util.Collections.sort(runs);
		System.out.println("r121 lines of ink " + runs + " in the cloud " + box);
		assertTrue(runs.size() >= 2, "found " + runs + " lines of ink in the cloud " + box);
		int line = runs.get(runs.size() / 2);
		// Mansalva at the window's width / 108 (before r121) inked 9 rows a line at 1280 wide, 12 at / 80.
		assertTrue(line >= w / 120, "a line of text is " + line + " px tall at " + w + " wide: too small to read (r121)");
	}
}

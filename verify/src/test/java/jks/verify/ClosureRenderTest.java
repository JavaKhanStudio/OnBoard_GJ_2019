package jks.verify;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;
import jks.vars.GVars_Heart;
import jks.vue.models.Vue_Scenematic_Outro;
import jks.vue.models.Vue_StartScreen;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * The end of the game (r60). The last outro page used to fade out onto nothing, and the screen
 * stayed white for good. Now a closing card follows, chosen by karma, and the game goes back to
 * the start screen with the run put away.
 *
 * One process plays both endings back to back: staying (karma 1) from a carriage whose items
 * have been taken, then leaving (karma 4) - the second pass is itself the loop being used.
 * The two cards are written to build/frames/closure-stay.png and closure-leave.png.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClosureRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	/** The card fades up over 2 s; this is past it. */
	private static final double CARD_SETTLED = 2.5;
	/** Between clicks on the pictures: long enough for a fade out, short enough to be quick. */
	private static final double CLICK_EVERY = 1.2;

	private static GameHarness harness;

	private static final List<String> seen = new ArrayList<>();
	private static volatile Throwable error;
	private static volatile boolean finished;

	@BeforeAll
	void playBothEndings()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = 1;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - closure verification");
		gl.useVsync(false);

		boolean[] leaving = {false};
		int[] phase = {0};           // 0 set up, 1 pictures, 2 card, 3 waiting for the menu
		double[] mark = {0};

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 1_000_000);
		harness.captureAfterSeconds = 0.1;
		harness.exitAfterSeconds = 90;
		harness.frameHook = frame ->
		{
			if (error != null || finished || harness.gameSeconds < 1.5) return;
			try
			{
				double now = harness.gameSeconds;
				switch (phase[0])
				{
					case 0:
						if (!leaving[0])
						{
							// What a played run leaves behind in the cached carriage.
							for (GameItem item : GVars_Game.currentLevel.listItems)
								picked().setBoolean(item, true);
							record("carriage 1 has items to take", !GVars_Game.currentLevel.listItems.isEmpty());
						}
						GVars_Game.currentLevelInt = GVars_Game.LEVEL_COUNT;
						GVars_Game.karma = leaving[0] ? GVars_Game.LEVEL_COUNT : 1;
						GVars_Heart.changeVue(new Vue_Scenematic_Outro(), true);
						phase[0] = 1;
						mark[0] = now;
						break;

					case 1:
						Vue_Scenematic_Outro outro = (Vue_Scenematic_Outro) GVars_Heart.vue;
						if (outro.isClosing())
						{
							phase[0] = 2;
							mark[0] = now;
						}
						else if (now - mark[0] > CLICK_EVERY)
						{
							GVars_Heart.inCinematic_Click = true;
							mark[0] = now;
						}
						break;

					case 2:
						if (now - mark[0] < CARD_SETTLED) break;
						String name = leaving[0] ? "closure-leave" : "closure-stay";
						BufferedImage card = GameHarness.grab();
						Frames.write(card, new File(OUTPUT, name + ".png"));
						record(name + ": the card is on black, not white", corner(card) < 16);
						record(name + ": the card has words on it", brightPixels(card) > 2000);
						record(name + ": still on the ending", GVars_Heart.vue instanceof Vue_Scenematic_Outro);
						phase[0] = 3;
						mark[0] = now;
						break;

					case 3:
						if (GVars_Heart.vue instanceof Vue_StartScreen)
						{
							String end = leaving[0] ? "leave" : "stay";
							record(end + ": the ending goes back to the start screen by itself", true);
							record(end + ": the run is reset", GVars_Game.karma == 0 && GVars_Game.currentLevelInt == 1
								&& GVars_Game.playerInventory.isEmpty());
							boolean anyPicked = false;
							for (var level : GVars_Game.preloadedlevel.values())
								for (GameItem item : level.listItems)
									anyPicked |= item.isPicked();
							record(end + ": the cached carriages have their items back", !anyPicked);

							if (leaving[0])
							{
								finished = true;
								Gdx.app.exit();
							}
							else
							{
								leaving[0] = true;
								phase[0] = 0;
							}
						}
						else if (now - mark[0] > 12)
						{
							record("the card handed back to the start screen within 12 s", false);
							finished = true;
							Gdx.app.exit();
						}
						break;
				}
			}
			catch (Throwable t)
			{
				error = t;
				Gdx.app.exit();
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	@AfterAll
	void restore()
	{
		Main_Application.startLevel = 1;
		GVars_Game.karma = 0;
	}

	private static Field picked() throws Exception
	{
		Field field = GameItem.class.getDeclaredField("picked");
		field.setAccessible(true);
		return field;
	}

	private static int corner(BufferedImage image)
	{
		int rgb = image.getRGB(4, 4);
		return Math.max((rgb >> 16) & 0xFF, Math.max((rgb >> 8) & 0xFF, rgb & 0xFF));
	}

	private static int brightPixels(BufferedImage image)
	{
		int count = 0;
		for (int y = 0; y < image.getHeight(); y += 1)
			for (int x = 0; x < image.getWidth(); x += 1)
				if ((image.getRGB(x, y) & 0xFF) > 128) count++;
		return count;
	}

	private static void record(String step, boolean ok)
	{
		seen.add((ok ? "ok   " : "FAIL ") + step);
	}

	@Test
	@DisplayName("both endings close on their card and loop back to a fresh start screen")
	void endingsCloseAndLoop()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		String report = String.join("\n", seen);
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + "\n" + report);
		assertNull(error, error == null ? null : "a step threw: " + error + "\n" + report);
		assertTrue(finished, "not every step ran:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);
		assertTrue(seen.size() == 13, "expected 13 checks:\n" + report);
	}
}

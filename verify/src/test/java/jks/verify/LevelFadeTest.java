package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.Vector3;

import jks.amain.Main_Application;
import jks.camera.GVars_Camera;
import jks.input.GVars_Inputs;
import jks.vars.GVars_Heart;
import jks.vue.GVars_Fade;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * Finishing a carriage fades it to black and brings the next one up out of the black (r44). It
 * used to swap in the same frame. Nothing in the carriage can be clicked or walked meanwhile.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LevelFadeTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;

	private static final List<String> seen = new ArrayList<>();
	private static final List<double[]> darkness = new ArrayList<>();
	private static volatile Throwable error;
	private static volatile boolean finished;

	/** Seconds after nextLevel for each grab: clear, half out, black, half in, clear again. */
	private static final double[] GRABS = {0, 0.5, 1.05, 2.0, 3.3};

	@BeforeAll
	void finishACarriage()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = 1;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - level fade verification");
		gl.useVsync(false);

		double start = 2.0;
		int[] grabbed = {0};
		boolean[] triggered = {false}, probed = {false};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = start + GRABS[GRABS.length - 1] + 0.3;
		harness.captureAfterSeconds = harness.exitAfterSeconds - 0.1;
		harness.frameHook = frame ->
		{
			try
			{
				double t = harness.gameSeconds - start;
				if (t < 0 || error != null) return;

				if (grabbed[0] < GRABS.length && t >= GRABS[grabbed[0]])
				{
					BufferedImage image = GameHarness.grab();
					Frames.write(image, new File(OUTPUT, "fade-level-" + grabbed[0] + ".png"));
					darkness.add(new double[] {GRABS[grabbed[0]], meanBrightness(image)});
					grabbed[0]++;
				}

				if (!triggered[0])
				{
					triggered[0] = true;
					GVars_Game.nextLevel();
					record("nextLevel starts a fade", GVars_Fade.isFading());
					record("and leaves the carriage in place until black", GVars_Game.currentLevelInt == 1);
					GVars_Game.nextLevel();
					record("a second nextLevel during the fade is ignored", GVars_Game.currentLevelInt == 1);
				}
				else if (!probed[0] && t >= 0.5)
				{
					probed[0] = true;
					GameItem item = pickableItem();
					Vector3 screen = GVars_Camera.camera.project(new Vector3(item.posX + 5, item.posY + 5, 0));
					int x = (int)screen.x, y = Gdx.graphics.getHeight() - (int)screen.y;
					Gdx.input.getInputProcessor().touchDown(x, y, 0, Buttons.LEFT);
					Gdx.input.getInputProcessor().touchUp(x, y, 0, Buttons.LEFT);
					record("a click during the fade picks nothing", GVars_Game.playerInventory.isEmpty());
					Gdx.input.getInputProcessor().keyDown(Keys.RIGHT);
					record("a key during the fade does not walk Ross", !GVars_Inputs.rightPressed);
					Gdx.input.getInputProcessor().keyDown(Keys.ESCAPE);
					record("nor pause", !GVars_Heart.isPaused);
				}
				else if (!finished && t >= GRABS[GRABS.length - 1])
				{
					record("the next carriage is in", GVars_Game.currentLevelInt == 2
						&& "wa2".equals(GVars_Game.currentLevel.path_meta));
					record("the fade is over", !GVars_Fade.isFading());
					Gdx.input.getInputProcessor().keyDown(Keys.RIGHT);
					record("and the keys are back", GVars_Inputs.rightPressed);
					Gdx.input.getInputProcessor().keyUp(Keys.RIGHT);
					finished = true;
				}
			}
			catch (Throwable t)
			{
				error = t;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	private static GameItem pickableItem()
	{
		for (GameItem item : GVars_Game.currentLevel.listItems)
			if (item.pickable) return item;
		throw new AssertionError("no pickable item in level " + GVars_Game.currentLevelInt);
	}

	private static void record(String step, boolean ok)
	{
		seen.add((ok ? "ok   " : "FAIL ") + step);
	}

	/** Mean of R+G+B over the thumbnail, 0 to 765. */
	private static double meanBrightness(BufferedImage image)
	{
		BufferedImage t = Frames.thumbnail(image);
		long sum = 0;
		for (int y = 0; y < t.getHeight(); y++)
			for (int x = 0; x < t.getWidth(); x++)
			{
				int p = t.getRGB(x, y);
				sum += ((p >> 16) & 0xFF) + ((p >> 8) & 0xFF) + (p & 0xFF);
			}
		return sum / (double) (t.getWidth() * t.getHeight());
	}

	@Test
	@DisplayName("a finished carriage fades out and the next fades in, with the input held off")
	void fadesBetweenCarriages()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		String report = String.join("\n", seen);
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + "\n" + report);
		assertNull(error, error == null ? null : "a step threw: " + error + "\n" + report);
		assertTrue(finished, "not every step ran:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);
		assertEquals(9, seen.size(), report);
	}

	@Test
	@DisplayName("the frames go dark and come back")
	void framesGoDarkAndBack()
	{
		StringBuilder curve = new StringBuilder();
		for (double[] d : darkness) curve.append(String.format("%.2fs=%.0f ", d[0], d[1]));
		assertEquals(GRABS.length, darkness.size(), "not every frame was grabbed: " + curve);
		double clear = darkness.get(0)[1], half = darkness.get(1)[1], black = darkness.get(2)[1],
			rising = darkness.get(3)[1], after = darkness.get(4)[1];
		assertTrue(black < 15, "the screen is not black at the swap: " + curve);
		assertTrue(half < clear * 0.8 && half > black, "halfway out is not between: " + curve);
		assertTrue(rising > black, "halfway in is still black: " + curve);
		// Not against the first grab: wa2 is a much darker carriage than wa1.
		assertTrue(rising < after * 0.8, "the next carriage did not come the rest of the way up: " + curve);
	}
}

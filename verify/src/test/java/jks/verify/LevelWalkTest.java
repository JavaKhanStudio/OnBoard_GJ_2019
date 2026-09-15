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

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;
import jks.vue.models.game.GVars_Game;

/**
 * Walks the carriages in order, the way a player does: each key found calls
 * GVars_Game.nextLevel(). GameRunTest only ever draws level 1, so until r24 nothing noticed
 * that level 2 could not load at all - its backdrop named an atlas region that no longer
 * existed, and the game died on "Asset not loaded: game/wagon/wa2/WAGON.png".
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LevelWalkTest
{
	private static final int LAST_LEVEL = 4;
	/** Long enough for nextLevel's fade out and back in (r44), so each grab is the carriage in full. */
	private static final double SECONDS_PER_LEVEL = 3.5;

	private static final File MODULE = new File(System.getProperty("onboard.verify"));
	private static final File OUTPUT = new File(MODULE, "build/frames");

	private static GameHarness harness;

	private static final List<BufferedImage> frames = new ArrayList<>();
	private static volatile Throwable error;

	@BeforeAll
	void walkTheLevels()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - level walk verification");
		gl.useVsync(false);

		double[] due = {SECONDS_PER_LEVEL};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 100_000);
		harness.exitAfterSeconds = SECONDS_PER_LEVEL * LAST_LEVEL + 0.2;
		harness.captureAfterSeconds = harness.exitAfterSeconds - 0.1;
		harness.frameHook = frame ->
		{
			if (error != null || frames.size() >= LAST_LEVEL || harness.gameSeconds < due[0]) return;
			try
			{
				frames.add(GameHarness.grab());
				if (GVars_Game.currentLevelInt < LAST_LEVEL)
					GVars_Game.nextLevel();
			}
			catch (Throwable t)
			{
				error = t;
			}
			due[0] = harness.gameSeconds + SECONDS_PER_LEVEL;
		};

		new Lwjgl3Application(harness, gl);
	}

	@Test
	@DisplayName("every level loads after the one before it, and draws")
	void everyLevelLoadsAndDraws() throws Exception
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		String reached = "reached level " + GVars_Game.currentLevelInt + " of " + LAST_LEVEL;
		// The step's error first: a level that failed to load goes on to throw every frame after.
		assertNull(error, error == null ? null : "moving to the next level threw: " + error + " - " + reached);
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + " - " + reached);
		assertEquals(LAST_LEVEL, frames.size(), "captured " + frames.size() + " levels - " + reached);

		for (int i = 0; i < frames.size(); i++)
		{
			File file = new File(OUTPUT, "walk-level" + (i + 1) + ".png");
			Frames.write(frames.get(i), file);
			double variety = Frames.distinctColourRatio(frames.get(i));
			assertTrue(variety > 0.25, "level " + (i + 1) + " is nearly uniform (distinct-colour ratio "
				+ String.format("%.3f", variety) + ") - it did not draw. See " + file.getAbsolutePath());
		}
	}
}

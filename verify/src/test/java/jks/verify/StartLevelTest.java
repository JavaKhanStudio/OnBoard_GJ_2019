package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.Vue_Game;
import jks.vars.GVars_Heart;

/**
 * -Donboard.level starts the game in a later carriage, which is how the board's lab opens each
 * one (r43). Until then only carriage 1 could be reached without playing through.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartLevelTest
{
	private static GameHarness harness;

	private static final List<String> seen = new ArrayList<>();
	private static volatile Throwable error;
	private static volatile boolean finished;

	@BeforeAll
	void startInCarriageThree()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = 3;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - start level verification");
		gl.useVsync(false);

		boolean[] moved = {false};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 3.5;
		harness.captureAfterSeconds = 3.4;
		harness.frameHook = frame ->
		{
			try
			{
				if (!moved[0] && harness.gameSeconds >= 1.5)
				{
					record("the game view is up", GVars_Heart.vue instanceof Vue_Game);
					record("it counts from carriage 3", GVars_Game.currentLevelInt == 3);
					record("carriage 3 is the one loaded", "wa3".equals(GVars_Game.currentLevel.path_meta));
					GVars_Game.nextLevel();
					moved[0] = true;
				}
				else if (moved[0] && !finished && harness.gameSeconds >= 3.0)
				{
					record("the next one after it is 4", GVars_Game.currentLevelInt == 4
						&& "wa4".equals(GVars_Game.currentLevel.path_meta));
					finished = true;
				}
			}
			catch (Throwable t)
			{
				if (error == null) error = t;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	@AfterAll
	void restore()
	{
		Main_Application.startLevel = 1;
	}

	private static void record(String step, boolean ok)
	{
		seen.add((ok ? "ok   " : "FAIL ") + step);
	}

	@Test
	@DisplayName("the game starts in the carriage asked for, and goes on from there")
	void startsInTheCarriageAskedFor()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		String report = String.join("\n", seen);
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + "\n" + report);
		assertNull(error, error == null ? null : "a step threw: " + error + "\n" + report);
		assertTrue(finished, "not every step ran:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);
		assertEquals(4, seen.size(), report);
	}
}

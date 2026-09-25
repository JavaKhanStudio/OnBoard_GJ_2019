package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.Vector3;

import jks.amain.Main_Application;
import jks.vars.GVars_Heart;
import jks.vue.models.Vue_StartScreen;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;
import jks.vue.models.game.Vue_Game;

/**
 * Jouer starts a new game from its first page, whatever the last one left behind (r110).
 * The carriages are cached (GVars_Game.preloadedlevel) and keep what the player did to them;
 * Jouer used to load carriage 1 as it was left, items taken, with currentLevelInt still on the
 * carriage the last game reached - so the next key found skipped ahead.
 *
 * Takes the bag in carriage 1, moves on to carriage 2 with some karma, goes back to the menu
 * without passing through the ending (which resets on its own, r60), and presses Jouer.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NewRunTest
{
	private static final String BAG = "sac.png";

	private static GameHarness harness;
	private static volatile Throwable error;
	private static volatile boolean bagTaken, finished, inGame, bagBack, sameCarriage;
	private static volatile int levelBeforeMenu = -1, levelAfter = -1, karmaAfter = -1, carriedAfter = -1;

	private interface Step { void run() throws Exception; }

	@BeforeAll
	void playTwice()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		GVars_Game.thinkAtEachStep = false;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - new run verification");
		gl.useVsync(false);

		List<Step> steps = new ArrayList<>();
		steps.add(() ->
		{
			GameItem bag = item(BAG);
			bag.tryTouch(new Vector3(bag.posX + bag.objectTexture.getWidth() / 2f,
				bag.posY + bag.objectTexture.getHeight() / 2f, 0), null, false);
			bagTaken = bag.isPicked();
			GVars_Game.karma = 3;
			GVars_Game.nextLevel();
		});
		// The carriage change fades out and in (r44).
		steps.add(() -> {});
		steps.add(() ->
		{
			levelBeforeMenu = GVars_Game.currentLevelInt;
			GVars_Heart.changeVue(new Vue_StartScreen().alreadyUp(), true);
		});
		steps.add(() ->
		{
			// The first enter shows the focus, the second presses Jouer.
			press(Keys.ENTER);
			press(Keys.ENTER);
		});
		// The menu fades out, the first carriage fades in.
		steps.add(() -> {});
		steps.add(() -> {});
		steps.add(() ->
		{
			inGame = GVars_Heart.vue instanceof Vue_Game;
			levelAfter = GVars_Game.currentLevelInt;
			karmaAfter = GVars_Game.karma;
			carriedAfter = GVars_Game.playerInventory == null ? 0 : GVars_Game.playerInventory.size();
			sameCarriage = GVars_Game.currentLevel == GVars_Game.preloadedlevel.get(1);
			bagBack = !item(BAG).isPicked();
			finished = true;
		});

		int[] next = {0};
		double[] due = {1.5};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 1.5 + steps.size() * 1.2 + 0.5;
		harness.frameHook = frame ->
		{
			if (error != null || next[0] >= steps.size() || harness.gameSeconds < due[0]) return;
			try
			{
				steps.get(next[0]++).run();
			}
			catch (Throwable t)
			{
				error = t;
			}
			due[0] = harness.gameSeconds + 1.2;
		};

		new Lwjgl3Application(harness, gl);
	}

	private static void press(int key)
	{
		Gdx.input.getInputProcessor().keyDown(key);
		Gdx.input.getInputProcessor().keyUp(key);
	}

	private static GameItem item(String name)
	{
		for (GameItem item : GVars_Game.currentLevel.listItems)
			if (name.equals(item.name)) return item;
		throw new AssertionError(name + " is not in carriage " + GVars_Game.currentLevelInt);
	}

	@Test
	@DisplayName("Jouer after a game starts carriage 1 afresh: its items back, the count and karma reset")
	void jouerStartsAfresh()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertTrue(bagTaken, "clicking the bag in carriage 1 did not take it");
		assertEquals(2, levelBeforeMenu, "the first game never reached carriage 2");
		assertTrue(finished, "the second game was never checked");

		assertTrue(inGame, "Jouer did not start the game");
		assertTrue(sameCarriage, "Jouer did not open carriage 1");
		assertEquals(1, levelAfter, "carriage 1 is on screen but the game counts carriage " + levelAfter
			+ ": the next key would skip ahead");
		assertTrue(bagBack, "carriage 1 opens with the bag already taken");
		assertEquals(0, karmaAfter, "the new game starts with the last one's karma");
		assertEquals(0, carriedAfter, "the new game starts carrying something");
	}
}

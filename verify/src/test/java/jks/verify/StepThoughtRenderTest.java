package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import jks.index.Index_Text;
import jks.vue.GVars_Steam;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * What Ross says, and when, through a carriage (r73, r91). He thinks the first hint as the
 * carriage opens, and then nothing by himself: a pickup flashes the pieces of the key still
 * missing, and a piece under the mouse shows its hint - or, once held, what was said as it came.
 * The last piece brings the steam creeping in over the time its line needs; a click hurries it.
 *
 * Carriage 1 is played through the items themselves: the bundle (piece 1), the cube into its
 * mould (piece 2), the cage's key into the cage (piece 3, the good action), then a click. In
 * carriage 2 the key is given whole after a long line, and nobody clicks.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StepThoughtRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	/** The first thought waits 1.5 s, then fades in and types: read by then. */
	private static final double OPENED_AT = 4.0;
	private static final double PIECE_AT = 5.0;
	private static final double AFTER_PIECE_AT = 8.5;
	private static final double LAST_PIECE_AT = 9.0;
	/** Into the creep, well before the line is read. */
	private static final double CREEPING_AT = LAST_PIECE_AT + 2.0;

	private static GameHarness harness;
	private static volatile String onOpening, afterPiece, piece1Line, piece2Line, piece3Line;
	private static volatile boolean flashing1, flashing2, flashing3;
	private static volatile int carriageWhileCreeping, carriageAfterClick, carriageBeforeRead, carriageAfterRead;
	private static volatile boolean creepingBeforeClick;
	private static volatile float lastLineSeconds;
	private static volatile Throwable error;

	@BeforeAll
	void playACarriage()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - step thought verification");
		gl.useVsync(false);

		int[] step = {0};
		double[] at = {0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 1_000_000);
		harness.exitAfterSeconds = 60;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				double t = harness.gameSeconds;
				if (step[0] == 0 && t >= OPENED_AT)
				{
					step[0] = 1;
					onOpening = GVars_Game.dialogBubble.getText();
					Frames.write(GameHarness.grab(), new File(OUTPUT, "step-thought-opening.png"));
				}
				else if (step[0] == 1 && t >= PIECE_AT)
				{
					step[0] = 2;
					click(item("baluchon.png"));
					flashing1 = GVars_Game.clef.isFlashing(1);
					flashing2 = GVars_Game.clef.isFlashing(2);
					flashing3 = GVars_Game.clef.isFlashing(3);
				}
				else if (step[0] == 2 && t >= PIECE_AT + 0.1)
				{
					step[0] = 3;
					Frames.write(GameHarness.grab(), new File(OUTPUT, "step-thought-key-flash.png"));
				}
				else if (step[0] == 3 && t >= AFTER_PIECE_AT)
				{
					step[0] = 4;
					afterPiece = GVars_Game.dialogBubble.getText();
					click(item("cube.png"));
					click(item("clefCage.png"));
					GVars_Game.selectedItem = item("cube.png");
					click(item("cubeM.png"));
				}
				else if (step[0] == 4 && t >= LAST_PIECE_AT)
				{
					step[0] = 5;
					GVars_Game.selectedItem = item("clefCage.png");
					click(item("cage.png"));
					lastLineSeconds = GVars_Game.dialogBubble.secondsBusy();
					piece1Line = GVars_Game.clef.lineOf(1);
					piece2Line = GVars_Game.clef.lineOf(2);
					piece3Line = GVars_Game.clef.lineOf(3);
				}
				else if (step[0] == 5 && t >= CREEPING_AT)
				{
					step[0] = 6;
					carriageWhileCreeping = GVars_Game.currentLevelInt;
					creepingBeforeClick = GVars_Steam.isCreeping();
					Frames.write(GameHarness.grab(), new File(OUTPUT, "step-thought-creeping.png"));
					// A click anywhere, through whatever the game is listening with.
					Gdx.input.getInputProcessor().touchDown(640, 360, 0, 0);
					at[0] = t;
				}
				else if (step[0] == 6 && t >= at[0] + 2.0)
				{
					step[0] = 7;
					carriageAfterClick = GVars_Game.currentLevelInt;
					// A long line, then the whole key: the steam waits for the line.
					GVars_Game.sayItemMessage("wa1.cage.message2");
					float reading = GVars_Game.dialogBubble.secondsBusy();
					GVars_Game.addKey(1);
					GVars_Game.addKey(2);
					GVars_Game.addKey(3);
					at[0] = t + reading;
				}
				else if (step[0] == 7 && t >= at[0] - 0.5)
				{
					step[0] = 8;
					carriageBeforeRead = GVars_Game.currentLevelInt;
				}
				else if (step[0] == 8 && t >= at[0] + 1.5)
				{
					step[0] = 9;
					carriageAfterRead = GVars_Game.currentLevelInt;
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

	private static GameItem item(String name)
	{
		for (GameItem item : GVars_Game.currentLevel.listItems)
			if (name.equals(item.name)) return item;
		throw new AssertionError(name + " is not in carriage " + GVars_Game.currentLevelInt);
	}

	/** The game's own click on an item, with whatever is in hand. */
	private static void click(GameItem item)
	{
		item.tryTouch(new Vector3(item.posX + 2, item.posY + 2, 0), GVars_Game.selectedItem, false);
	}

	@Test
	@DisplayName("Ross thinks the first hint as a carriage opens, and says nothing more by himself")
	void rossThinksOnlyAtTheStart()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);

		assertEquals(Index_Text.get("wa1.hint1"), onOpening, "carriage 1 opened without a thought");
		assertNotEquals(Index_Text.get("wa1.hint2"), afterPiece, "piece 1 came and Ross said the next hint by himself");
	}

	@Test
	@DisplayName("A pickup flashes the pieces still missing")
	void aPickupFlashesTheKey()
	{
		assertFalse(flashing1, "piece 1 is held: it has nothing to guide towards");
		assertTrue(flashing2 && flashing3, "the missing pieces did not flash on a pickup");
	}

	@Test
	@DisplayName("A held piece shows what was said as it came, a line or none")
	void aHeldPieceShowsItsResult()
	{
		assertEquals(Index_Text.get("wa1.baluchon.click"), piece1Line, "piece 1 still shows its hint");
		// The cube says nothing going into its mould: its piece keeps the hint.
		assertEquals(Index_Text.get("wa1.hint2"), piece2Line, "a piece won in silence lost its hint");
		assertEquals(Index_Text.get("wa1.cage.message2"), piece3Line, "piece 3 does not show the good action's line");
	}

	@Test
	@DisplayName("The last line is read under creeping steam, and a click hurries it")
	void theSteamWaitsForTheLastLine()
	{
		assertTrue(lastLineSeconds > 3, "the last piece came with nothing to read: " + lastLineSeconds);
		assertTrue(creepingBeforeClick, "no steam creeping 2 s after the last piece");
		assertEquals(1, carriageWhileCreeping, "the carriage went before its last line was read");
		assertEquals(2, carriageAfterClick, "a click did not hurry the steam");
		assertEquals(2, carriageBeforeRead, "without a click, the carriage went before its line was read");
		assertEquals(3, carriageAfterRead, "the steam never let carriage 2 go");
	}
}

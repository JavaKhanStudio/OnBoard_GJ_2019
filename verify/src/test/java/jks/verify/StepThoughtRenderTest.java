package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;
import jks.index.Index_Text;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * What Ross has on his mind at each step (r73): as a carriage opens he thinks the hint of the
 * first key piece he lacks, and again each time a piece comes. Until r73 the bubble only ever
 * spoke under the mouse, so a player who never hovered the key was never told anything.
 *
 * Opens carriage 1, picks up the bundle (key piece 1), then moves on to carriage 2.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StepThoughtRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	/** The first thought waits 1.5 s, then fades in and types: read by then. */
	private static final double OPENED_AT = 4.0;
	private static final double PIECE_AT = 5.0;
	private static final double NEXT_THOUGHT_AT = 8.0;
	/** Carriage 2 is swapped in after the 1 s fade out, and thinks 1.5 s later. */
	private static final double CARRIAGE_2_AT = NEXT_THOUGHT_AT + 1.0 + 4.5;

	private static GameHarness harness;
	private static volatile String onOpening, afterPiece, inCarriage2;
	private static volatile Throwable error;

	@BeforeAll
	void playTheFirstSteps()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - step thought verification");
		gl.useVsync(false);

		int[] step = {0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 1_000_000);
		harness.exitAfterSeconds = CARRIAGE_2_AT + 1;
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
					GVars_Game.pickItem(item("baluchon.png"));
				}
				else if (step[0] == 2 && t >= NEXT_THOUGHT_AT)
				{
					step[0] = 3;
					afterPiece = GVars_Game.dialogBubble.getText();
					Frames.write(GameHarness.grab(), new File(OUTPUT, "step-thought-after-piece.png"));
					GVars_Game.nextLevel();
				}
				else if (step[0] == 3 && t >= CARRIAGE_2_AT)
				{
					step[0] = 4;
					inCarriage2 = GVars_Game.dialogBubble.getText();
					Frames.write(GameHarness.grab(), new File(OUTPUT, "step-thought-carriage-2.png"));
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

	@Test
	@DisplayName("Ross thinks of the first missing piece as a carriage opens, and of the next once he has it")
	void rossThinksAtEachStep()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);

		assertEquals(Index_Text.get("wa1.hint1"), onOpening, "carriage 1 opened without a thought");
		assertEquals(Index_Text.get("wa1.hint2"), afterPiece, "piece 1 came and Ross did not think of piece 2");
		assertEquals(Index_Text.get("wa2.hint1"), inCarriage2, "carriage 2 opened without its own first thought");
	}
}

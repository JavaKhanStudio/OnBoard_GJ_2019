package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import com.badlogic.gdx.math.Vector3;

import jks.amain.Main_Application;
import jks.index.Index_Text;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * What Ross says when an item is clicked (r74). In carriage 1: clicking the cube takes it and
 * says its line; clicking the empty slot with nothing in hand says the slot's; once the cube
 * is in the slot, clicking the slot again says nothing more about a missing cube.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemClickRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static final double TAKE_AT = 3.0, SLOT_AT = 5.0, FILL_AT = 7.0, AGAIN_AT = 12.0;

	private static GameHarness harness;
	private static volatile String onTake, onSlot, onFilledSlot;
	private static volatile Throwable error;

	@BeforeAll
	void clickThroughCarriage1()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		// Only the clicks speak here: no thought of the next step typing over them (r73).
		GVars_Game.thinkAtEachStep = false;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - item click verification");
		gl.useVsync(false);

		int[] step = {0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = AGAIN_AT + 3;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				double t = harness.gameSeconds;
				if (step[0] == 0 && t >= TAKE_AT)
				{
					step[0] = 1;
					click(item("cube.png"), null);
				}
				else if (step[0] == 1 && t >= TAKE_AT + 1.5)
				{
					step[0] = 2;
					onTake = GVars_Game.dialogBubble.getText();
					Frames.write(GameHarness.grab(), new File(OUTPUT, "item-click-cube.png"));
				}
				else if (step[0] == 2 && t >= SLOT_AT)
				{
					step[0] = 3;
					click(item("cubeM.png"), null);
				}
				else if (step[0] == 3 && t >= SLOT_AT + 1.5)
				{
					step[0] = 4;
					onSlot = GVars_Game.dialogBubble.getText();
					Frames.write(GameHarness.grab(), new File(OUTPUT, "item-click-slot.png"));
				}
				else if (step[0] == 4 && t >= FILL_AT)
				{
					step[0] = 5;
					GameItem cube = item("cube.png");
					GVars_Game.selectedItem = cube;
					click(item("cubeM.png"), cube);
				}
				else if (step[0] == 5 && t >= AGAIN_AT)
				{
					step[0] = 6;
					GVars_Game.selectedItem = null;
					click(item("cubeM.png"), null);
				}
				else if (step[0] == 6 && t >= AGAIN_AT + 1.0)
				{
					step[0] = 7;
					onFilledSlot = GVars_Game.dialogBubble.getText();
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

	private static void click(GameItem item, GameItem with)
	{
		Vector3 middle = new Vector3(item.posX + item.objectTexture.getWidth() / 2f,
			item.posY + item.objectTexture.getHeight() / 2f, 0);
		item.tryTouch(middle, with, false);
	}

	private static GameItem item(String name)
	{
		for (GameItem item : GVars_Game.currentLevel.listItems)
			if (name.equals(item.name)) return item;
		throw new AssertionError(name + " is not in carriage " + GVars_Game.currentLevelInt);
	}

	@Test
	@DisplayName("a clicked item says its line, and a used one does not say it again")
	void clickedItemsSpeak()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);

		assertEquals(Index_Text.get("wa1.cube.click"), onTake, "taking the cube said nothing of it");
		assertEquals(Index_Text.get("wa1.cubeM.click"), onSlot, "the empty slot, clicked, said nothing of itself");
		assertNotEquals(Index_Text.get("wa1.cubeM.click"), onFilledSlot, "the filled slot still says a cube is missing");
	}
}

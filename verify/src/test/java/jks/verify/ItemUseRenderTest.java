package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.util.List;

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
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * An item, once used, is spent (r77). In carriage 1 both the seed bag and the cage key are
 * taken, the bag is used on the cage: it leaves the hand and the bar, the key stays. Then the
 * key is used too, and the bar is empty. Before r77 the bag stayed selected and could be
 * used on the cage again, and the key's choice could add karma twice.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemUseRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static final double TAKE_AT = 3.0, USE_BAG_AT = 4.0, USE_KEY_AT = 6.0, AGAIN_AT = 7.0;

	private static GameHarness harness;
	private static volatile GameItem selectedAfterBag, selectedAfterKey;
	private static volatile List<String> carriedAfterBag, carriedAfterKey;
	private static volatile int barAfterBag = -1, barAfterKey = -1, karmaAfterKey = -1, karmaAfterAgain = -1;
	private static volatile Throwable error;

	@BeforeAll
	void useTwoThingsOnTheCage()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		GVars_Game.thinkAtEachStep = false;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - item use verification");
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
					click(item("sac.png"), null);
					click(item("clefCage.png"), null);
				}
				else if (step[0] == 1 && t >= TAKE_AT + 0.3)
				{
					step[0] = 2;
					Frames.write(GameHarness.grab(), new File(OUTPUT, "item-use-before.png"));
				}
				else if (step[0] == 2 && t >= USE_BAG_AT)
				{
					step[0] = 3;
					GVars_Game.selectedItem = item("sac.png");
					click(item("cage.png"), GVars_Game.selectedItem);
					selectedAfterBag = GVars_Game.selectedItem;
					carriedAfterBag = names();
					barAfterBag = GVars_Game.inventory.getChildren().size;
				}
				else if (step[0] == 3 && t >= USE_BAG_AT + 0.3)
				{
					step[0] = 4;
					Frames.write(GameHarness.grab(), new File(OUTPUT, "item-use-after-bag.png"));
				}
				else if (step[0] == 4 && t >= USE_KEY_AT)
				{
					step[0] = 5;
					GVars_Game.selectedItem = item("clefCage.png");
					click(item("cage.png"), GVars_Game.selectedItem);
					selectedAfterKey = GVars_Game.selectedItem;
					carriedAfterKey = names();
					barAfterKey = GVars_Game.inventory.getChildren().size;
					karmaAfterKey = GVars_Game.karma;
				}
				else if (step[0] == 5 && t >= AGAIN_AT)
				{
					step[0] = 6;
					// Whatever is in hand now - nothing - clicking the cage again gives nothing.
					click(item("cage.png"), GVars_Game.selectedItem);
					karmaAfterAgain = GVars_Game.karma;
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

	private static List<String> names()
	{
		return GVars_Game.playerInventory.stream().map(item -> item.name).toList();
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
	@DisplayName("a used item leaves the hand and the bar, and what was not used stays")
	void usedItemsAreSpent()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);

		assertNull(selectedAfterBag, "the seed bag is still in hand after it was used");
		assertEquals(List.of("clefCage.png"), carriedAfterBag, "only the cage key should still be carried");
		assertEquals(1, barAfterBag, "the bar should show the cage key alone");

		assertNull(selectedAfterKey, "the cage key is still in hand after it was used");
		assertEquals(List.of(), carriedAfterKey, "nothing should be carried once both were used");
		assertEquals(0, barAfterKey, "the bar should be empty");
		assertEquals(1, karmaAfterKey, "the cage key's choice gives one karma");
		assertEquals(1, karmaAfterAgain, "clicking the cage again with nothing in hand gave karma again");
	}
}

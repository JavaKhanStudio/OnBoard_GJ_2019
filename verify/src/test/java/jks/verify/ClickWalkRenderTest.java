package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
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
import jks.amain.Utils_Config;
import jks.camera.GVars_Camera;
import jks.input.GVars_Inputs;
import jks.vue.models.game.ClickMarker;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;
import jks.vue.models.game.WagonLevel;

/**
 * Click-to-walk (r229), through the game's own input multiplexer, in carriage 1. With the
 * option on, a click on the floor shows a ring there and walks Ross until his middle is under
 * it; a click on an item acts on the item and does not move him; an arrow key takes over from
 * a click; a click on the inventory bar is not floor. With the option off, as by default, a
 * click moves nobody.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClickWalkRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");
	private static final float FLOOR_X = 900, FLOOR_Y = 300;

	private static GameHarness harness;
	private static Steps steps;

	private static volatile boolean ringOnClick, targetOnClick;
	private static volatile float arrivedMiddle = Float.NaN, arrivedVelocity = Float.NaN;
	private static volatile boolean arrivedStill;
	private static volatile String clickedItem;
	private static volatile float beforeItemX, afterItemX;
	private static volatile boolean itemActed, targetOnItem, ringOnItem;
	private static volatile boolean targetAfterArrow;
	private static volatile boolean targetOnBar;
	private static volatile float beforeOffX, afterOffX;
	private static volatile boolean targetWhenOff, ringWhenOff;

	@BeforeAll
	void clickAroundCarriage1()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		GVars_Game.thinkAtEachStep = false;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - click to walk verification");
		gl.useVsync(false);

		steps = new Steps(2.5, 1.0);
		steps.add(0.15, () ->
		{
			Utils_Config.current.clickToWalk = true;
			click(FLOOR_X, FLOOR_Y);
			ringOnClick = ClickMarker.isShowing();
			targetOnClick = GVars_Inputs.isWalkingToTarget();
		});
		steps.add(4.0, () -> Frames.write(GameHarness.grab(), new File(OUTPUT, "click-walk-1-ring.png")));
		steps.add(0.5, () ->
		{
			arrivedMiddle = GVars_Game.ross.position.x + GVars_Game.ross.getFrameWidth() / 2f;
			arrivedVelocity = GVars_Game.ross.velocity.x;
			arrivedStill = !GVars_Inputs.isWalkingToTarget();
			Frames.write(GameHarness.grab(), new File(OUTPUT, "click-walk-2-arrived.png"));
		});
		steps.add(1.5, () ->
		{
			GameItem item = itemOnScreen();
			clickedItem = item.name;
			beforeItemX = GVars_Game.ross.position.x;
			click(item.posX + item.objectTexture.getWidth() / 2f, item.posY + item.objectTexture.getHeight() / 2f);
			itemActed = item.isPicked() || GVars_Game.dialogBubble != null;
			targetOnItem = GVars_Inputs.isWalkingToTarget();
			ringOnItem = ClickMarker.isShowing();
		});
		steps.add(0.3, () ->
		{
			afterItemX = GVars_Game.ross.position.x;
			click(GVars_Game.ross.position.x - 300, FLOOR_Y);
		});
		steps.add(0.3, () -> Gdx.input.getInputProcessor().keyDown(Keys.RIGHT));
		steps.add(0.5, () ->
		{
			targetAfterArrow = GVars_Inputs.isWalkingToTarget();
			Gdx.input.getInputProcessor().keyUp(Keys.RIGHT);
		});
		steps.add(1.0, () ->
		{
			click(FLOOR_X, WagonLevel.BAR / 2f);
			targetOnBar = GVars_Inputs.isWalkingToTarget();
		});
		steps.add(1.0, () ->
		{
			Utils_Config.current.clickToWalk = false;
			ClickMarker.clear();
			beforeOffX = GVars_Game.ross.position.x;
			click(GVars_Game.ross.position.x + 400, FLOOR_Y);
			targetWhenOff = GVars_Inputs.isWalkingToTarget();
			ringWhenOff = ClickMarker.isShowing();
		});
		steps.add(0.5, () -> afterOffX = GVars_Game.ross.position.x);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = steps.seconds() + 0.5;
		steps.drive(harness);

		new Lwjgl3Application(harness, gl);
	}

	@AfterAll
	void optionBackOff()
	{
		Utils_Config.current.clickToWalk = false;
	}

	/** A left click where this world point is on screen, sent the way a mouse sends it. */
	private static void click(float worldX, float worldY)
	{
		Vector3 screen = GVars_Camera.camera.project(new Vector3(worldX, worldY, 0));
		int x = Math.round(screen.x), y = Gdx.graphics.getHeight() - Math.round(screen.y);
		Gdx.input.getInputProcessor().touchDown(x, y, 0, Buttons.LEFT);
		Gdx.input.getInputProcessor().touchUp(x, y, 0, Buttons.LEFT);
	}

	/** An item still there whose middle is in view, away from the corners the HUD holds. */
	private static GameItem itemOnScreen()
	{
		for (GameItem item : GVars_Game.currentLevel.listItems)
		{
			if (item.isPicked() || item.objectTexture == null) continue;
			Vector3 middle = GVars_Camera.camera.project(new Vector3(item.posX + item.objectTexture.getWidth() / 2f,
				item.posY + item.objectTexture.getHeight() / 2f, 0));
			if (middle.x > 250 && middle.x < Gdx.graphics.getWidth() - 250
				&& middle.y > 150 && middle.y < Gdx.graphics.getHeight() - 150)
				return item;
		}
		throw new AssertionError("no item of carriage 1 in view to click");
	}

	@Test
	@DisplayName("with the option on, a click on the floor rings and walks Ross there; on an item it does not move him")
	void clickWalks()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(steps.error, steps.error == null ? null : "a step threw: " + steps.error);

		assertTrue(ringOnClick, "a click on the floor showed no ring");
		assertTrue(targetOnClick, "a click on the floor sent Ross nowhere");
		assertTrue(arrivedStill, "Ross was still walking to the click 4 s later");
		assertEquals(FLOOR_X, arrivedMiddle, 2f, "Ross stopped with his middle away from the click");
		assertEquals(0f, arrivedVelocity, 0f, "Ross reached the click and kept moving");

		assertTrue(itemActed, "the click on " + clickedItem + " did nothing to it");
		assertFalse(targetOnItem, "a click on " + clickedItem + " sent Ross walking");
		assertFalse(ringOnItem, "a click on " + clickedItem + " showed the floor ring");
		assertEquals(beforeItemX, afterItemX, 0f, "Ross moved on a click on " + clickedItem);

		assertFalse(targetAfterArrow, "an arrow key did not take over from the click");
		assertFalse(targetOnBar, "a click on the inventory bar sent Ross walking");
	}

	@Test
	@DisplayName("on in a fresh config; with the option off, a click moves nobody")
	void onByDefaultAndOff()
	{
		assertTrue(new jks.amain.GameConfigs().clickToWalk, "click-to-walk is off in a fresh config");
		assertFalse(targetWhenOff, "a click sent Ross walking with the option off");
		assertFalse(ringWhenOff, "a click showed a ring with the option off");
		assertEquals(beforeOffX, afterOffX, 0f, "Ross moved on a click with the option off");
	}
}

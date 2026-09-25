package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.kotcrab.vis.ui.widget.VisImageButton;

import jks.amain.Main_Application;
import jks.camera.GVars_Camera;
import jks.index.Index_Interface;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * Taking the knife in carriage 2 (r86) leaves its empty mount on the wall, couteauSocle_1.png,
 * and puts the bare knife, couteau.png, in the bar. Before r86 the whole mount vanished and the
 * bar carried knife and mount together. The empty mount is not clickable and gives nothing a
 * second time; the knife still cuts the portrait (tableau choice 2, one karma); a new run puts
 * the full mount back. Writes knife-mount-before.png and knife-mount-after.png.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KnifeMountRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");
	private static final String DIR = "game/wagon/wa2/";

	/** The carriage change fades out and back in (r44): carriage 2 is in full by then. */
	private static final double NEXT_AT = 1.0, BEFORE_AT = 4.5, TAKE_AT = 5.0, AGAIN_AT = 6.0, CUT_AT = 7.0, RESET_AT = 8.0;

	private static GameHarness harness;
	private static volatile Throwable error;
	private static volatile int level = -1, barAfterTake = -1, barAfterAgain = -1, karmaBeforeCut = -1, karmaAfterCut = -1;
	private static volatile boolean pickedAfterTake, hoveredAfterTake, pickedAfterReset;
	private static volatile Texture onWallAfterTake, inBar, onWallAfterReset;
	private static volatile List<String> carriedAfterCut;

	@BeforeAll
	void takeTheKnife()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		GVars_Game.thinkAtEachStep = false;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - knife mount verification");
		gl.useVsync(false);

		int[] step = {0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = RESET_AT + 3;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				double t = harness.gameSeconds;
				// The mount hangs past the first screen of the carriage: the view is held on it
				// so the two captures show it (a frame after this, since this frame is drawn).
				if (step[0] >= 1 && t >= BEFORE_AT - 0.5 && GVars_Game.currentLevelInt == 2)
				{
					GameItem knife = item("couteauSocle.png");
					GVars_Camera.camera.position.x = knife.posX + knife.objectTexture.getWidth() / 2f;
					GVars_Camera.camera.update();
				}
				if (step[0] == 0 && t >= NEXT_AT)
				{
					step[0] = 1;
					GVars_Game.nextLevel();
				}
				else if (step[0] == 1 && t >= BEFORE_AT)
				{
					step[0] = 2;
					level = GVars_Game.currentLevelInt;
					Frames.write(GameHarness.grab(), new File(OUTPUT, "knife-mount-before.png"));
				}
				else if (step[0] == 2 && t >= TAKE_AT)
				{
					step[0] = 3;
					GameItem knife = item("couteauSocle.png");
					click(knife, null);
					pickedAfterTake = knife.isPicked();
					onWallAfterTake = knife.objectTexture;
					barAfterTake = GVars_Game.inventory.getChildren().size;
					if (barAfterTake > 0)
					{
						VisImageButton button = (VisImageButton) GVars_Game.inventory.getChildren().first();
						inBar = ((TextureRegionDrawable) button.getImage().getDrawable()).getRegion().getTexture();
					}
					knife.updateHover(middle(knife));
					hoveredAfterTake = knife.isHovered();
				}
				else if (step[0] == 3 && t >= TAKE_AT + 0.3)
				{
					step[0] = 4;
					Frames.write(GameHarness.grab(), new File(OUTPUT, "knife-mount-after.png"));
				}
				else if (step[0] == 4 && t >= AGAIN_AT)
				{
					step[0] = 5;
					// The empty mount, clicked again with nothing in hand, gives nothing.
					click(item("couteauSocle.png"), null);
					barAfterAgain = GVars_Game.inventory.getChildren().size;
				}
				else if (step[0] == 5 && t >= CUT_AT)
				{
					step[0] = 6;
					karmaBeforeCut = GVars_Game.karma;
					GVars_Game.selectedItem = item("couteauSocle.png");
					click(item("tableau.png"), GVars_Game.selectedItem);
					karmaAfterCut = GVars_Game.karma;
					carriedAfterCut = GVars_Game.playerInventory.stream().map(item -> item.name).toList();
				}
				else if (step[0] == 6 && t >= RESET_AT)
				{
					step[0] = 7;
					GVars_Game.resetForNewRun();
					GameItem knife = item("couteauSocle.png");
					pickedAfterReset = knife.isPicked();
					onWallAfterReset = knife.objectTexture;
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

	private static Vector3 middle(GameItem item)
	{
		return new Vector3(item.posX + item.objectTexture.getWidth() / 2f,
			item.posY + item.objectTexture.getHeight() / 2f, 0);
	}

	private static void click(GameItem item, GameItem with)
	{
		item.tryTouch(middle(item), with, false);
	}

	private static GameItem item(String name)
	{
		for (GameItem item : GVars_Game.currentLevel.listItems)
			if (name.equals(item.name)) return item;
		throw new AssertionError(name + " is not in carriage " + GVars_Game.currentLevelInt);
	}

	private static Texture texture(String png)
	{
		return Index_Interface.manager.get(DIR + png, Texture.class);
	}

	@Test
	@DisplayName("taking the knife leaves its empty mount on the wall and puts the bare knife in the bar")
	void theMountStaysAndTheKnifeGoes()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertEquals(2, level, "the test never reached carriage 2");

		assertTrue(pickedAfterTake, "clicking the knife did not take it");
		assertSame(texture("couteauSocle_1.png"), onWallAfterTake, "the mount's place does not show the empty mount");
		assertFalse(hoveredAfterTake, "the empty mount lights up under the mouse, as if it could be clicked");
		assertEquals(1, barAfterTake, "the bar should carry the knife alone");
		assertSame(texture("couteau.png"), inBar, "the bar does not show the bare knife");

		assertEquals(1, barAfterAgain, "clicking the empty mount again gave something a second time");

		assertEquals(karmaBeforeCut + 1, karmaAfterCut, "cutting the portrait with the knife no longer gives its karma");
		assertEquals(List.of(), carriedAfterCut, "the knife is still carried after cutting the portrait");

		assertFalse(pickedAfterReset, "a new run still finds the knife taken");
		assertSame(texture("couteauSocle.png"), onWallAfterReset, "a new run does not put the full mount back");
	}
}

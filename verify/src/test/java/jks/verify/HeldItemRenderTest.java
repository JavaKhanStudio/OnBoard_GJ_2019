package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

import jks.amain.Main_Application;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * The item in hand is marked in the bar (r84). Clicking a carried item used to set
 * GVars_Game.selectedItem and change nothing on screen, so the player could not tell what, if
 * anything, a click in the carriage would use.
 *
 * Carries the sac and the cube of carriage 1, then clicks them in the bar the way the stage
 * does - a touchDown and a touchUp fired on the button - and uses the sac on the cage.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HeldItemRenderTest
{
	private static final double PICK_AT = 2.0;
	/** Half a second between steps, so each frame shows the step before it. */
	private static final double STEP = 0.5;

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;

	private static volatile BufferedImage nothingHeld, sacHeld;
	private static volatile Object heldAtFirst, heldAfterSac, heldAfterCube, heldAfterCubeAgain, heldAfterUse;
	private static volatile GameItem sac, cube, selectedAfterCubeAgain;
	private static volatile int width, height;
	/** Where the two buttons sit in the bar, in stage pixels (y up): x, y, width, height. */
	private static volatile float[] sacBox, cubeBox;
	private static volatile Throwable error;

	@BeforeAll
	void clickTheCarriedItems()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = 1;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - item in hand verification");
		gl.useVsync(false);

		int[] step = {0};

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 60;
		harness.frameHook = frame ->
		{
			if (error != null || harness.gameSeconds < PICK_AT + step[0] * STEP) return;
			try
			{
				switch (step[0]++)
				{
					case 0 ->
					{
						width = Gdx.graphics.getWidth();
						sac = item("sac.png");
						cube = item("cube.png");
						GVars_Game.pickItem(sac);
						GVars_Game.pickItem(cube);
					}
					// A newly carried item flashes for 1 s (r93, r104): the frames below
					// measure the mark, so they wait for it to end.
					case 1, 2 -> {}
					case 3 ->
					{
						heldAtFirst = userObject(GVars_Game.inventory.held());
						height = Gdx.graphics.getHeight();
						sacBox = box(sac);
						cubeBox = box(cube);
						nothingHeld = GameHarness.grab();
						Frames.write(nothingHeld, new File(OUTPUT, "held-item-none.png"));
						click(sac);
					}
					case 4 ->
					{
						heldAfterSac = userObject(GVars_Game.inventory.held());
						sacHeld = GameHarness.grab();
						Frames.write(sacHeld, new File(OUTPUT, "held-item-sac.png"));
						click(cube);
					}
					case 5 ->
					{
						heldAfterCube = userObject(GVars_Game.inventory.held());
						click(cube);
					}
					case 6 ->
					{
						heldAfterCubeAgain = userObject(GVars_Game.inventory.held());
						selectedAfterCubeAgain = GVars_Game.selectedItem;
						click(sac);
						GameItem cage = item("cage.png");
						Vector3 middle = new Vector3(cage.posX + cage.objectTexture.getWidth() / 2f,
							cage.posY + cage.objectTexture.getHeight() / 2f, 0);
						cage.tryTouch(middle, GVars_Game.selectedItem, false);
					}
					case 7 ->
					{
						heldAfterUse = userObject(GVars_Game.inventory.held());
						Frames.write(GameHarness.grab(), new File(OUTPUT, "held-item-used.png"));
						harness.exitAfterSeconds = harness.gameSeconds + 0.2;
					}
					default -> {}
				}
			}
			catch (Throwable t)
			{
				error = t;
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

	private static Object userObject(Actor actor)
	{
		return actor == null ? null : actor.getUserObject();
	}

	private static float[] box(GameItem item)
	{
		for (Actor button : GVars_Game.inventory.getChildren())
			if (button.getUserObject() == item)
				return new float[] {GVars_Game.inventory.getX() + button.getX(), GVars_Game.inventory.getY() + button.getY(),
					button.getWidth(), button.getHeight()};
		throw new AssertionError(item.name + " is not in the bar");
	}

	/**
	 * How bright the band just outside a button is, 0 to 255: where the mark is drawn and the
	 * item is not. The carriage above the bar keeps moving (the thought bubble types), so the
	 * whole frame cannot be compared.
	 */
	private static double ringBrightness(BufferedImage frame, float[] b)
	{
		int pad = Math.max(2, Math.round(b[2] * 0.06f));
		long sum = 0; int n = 0;
		for (int x = Math.round(b[0]) - pad; x < Math.round(b[0] + b[2]) + pad; x++)
			for (int y = Math.round(b[1]) - pad; y < Math.round(b[1] + b[3]) + pad; y++)
			{
				boolean inside = x >= b[0] && x < b[0] + b[2] && y >= b[1] && y < b[1] + b[3];
				int row = height - 1 - y;
				if (inside || x < 0 || x >= frame.getWidth() || row < 0 || row >= frame.getHeight()) continue;
				int p = frame.getRGB(x, row);
				sum += ((p >> 16) & 0xFF) + ((p >> 8) & 0xFF) + (p & 0xFF);
				n++;
			}
		return n == 0 ? 0 : sum / (3.0 * n);
	}

	/** A click on the item's button in the bar, as the stage delivers one. */
	private static void click(GameItem item)
	{
		for (Actor button : GVars_Game.inventory.getChildren())
			if (button.getUserObject() == item)
			{
				for (InputEvent.Type type : new InputEvent.Type[] {InputEvent.Type.touchDown, InputEvent.Type.touchUp})
				{
					InputEvent event = new InputEvent();
					event.setType(type);
					event.setStage(button.getStage());
					event.setPointer(0);
					event.setButton(0);
					button.fire(event);
				}
				return;
			}
		throw new AssertionError(item.name + " is not in the bar");
	}

	@Test
	@DisplayName("a clicked item is the one marked in the bar, and clicking it again puts it back")
	void theItemInHandIsMarked()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertEquals(1280, width, "the window is not the size asked for, so the frames test nothing");
		assertNotNull(sacHeld, "no frame was taken with the sac in hand");

		assertNull(heldAtFirst, "something is marked before anything was clicked");
		assertSame(sac, heldAfterSac, "clicking the sac did not mark it");
		assertSame(cube, heldAfterCube, "clicking the cube did not move the mark to it");
		assertNull(heldAfterCubeAgain, "clicking the cube a second time did not put it back");
		assertNull(selectedAfterCubeAgain, "clicking the cube a second time left it in hand");

		double sacBefore = ringBrightness(nothingHeld, sacBox), sacAfter = ringBrightness(sacHeld, sacBox);
		double cubeBefore = ringBrightness(nothingHeld, cubeBox), cubeAfter = ringBrightness(sacHeld, cubeBox);
		assertTrue(sacAfter - sacBefore > 30,
			"no mark drawn round the sac in hand: its edge went from " + sacBefore + " to " + sacAfter);
		assertEquals(cubeBefore, cubeAfter, 2.0, "the cube, not in hand, is marked too");
	}

	@Test
	@DisplayName("using the item in hand takes the mark away with it")
	void usingItClearsTheMark()
	{
		assertNull(heldAfterUse, "the sac was used on the cage and something is still marked");
	}
}

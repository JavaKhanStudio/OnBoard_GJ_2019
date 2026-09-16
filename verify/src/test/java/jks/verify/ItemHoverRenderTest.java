package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;

import jks.amain.Main_Application;
import jks.camera.GVars_Camera;
import jks.vinterface.GVars_UI;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * A carriage item lights up while the mouse is on it, the way the menus do (r40). Nothing in a
 * level reacted to the mouse before a click. The mouse goes through the level's own input
 * processor, as a real one would.
 *
 * Lit means a yellow line around the item, not over it (d10): the bundle is pale, and the
 * additive glow r40 first drew washed it toward white.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemHoverRenderTest
{
	/** On screen from the start of level 1, and away from Ross and the widgets. */
	private static final String ITEM = "baluchon.png";

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;

	private static final List<String> seen = new ArrayList<>();
	private static volatile BufferedImage resting, lit;
	private static volatile int[] itemOnScreen;
	private static volatile Throwable error;
	private static volatile boolean finished;

	private interface Step { void run() throws Exception; }

	@BeforeAll
	void hoverAnItem()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - item hover verification");
		gl.useVsync(false);

		List<Step> steps = new ArrayList<>();
		steps.add(() -> {
			GameItem item = item();
			record("nothing is lit before the mouse moves", !item.isHovered());
			resting = GameHarness.grab();
			Frames.write(resting, new File(OUTPUT, "hover-resting.png"));
			int[] centre = screenCentre(item);
			Gdx.input.getInputProcessor().mouseMoved(centre[0], centre[1]);
		});
		steps.add(() -> {
			record("the item under the mouse is lit", item().isHovered());
			lit = GameHarness.grab();
			Frames.write(lit, new File(OUTPUT, "hover-item.png"));
			Gdx.input.getInputProcessor().mouseMoved(640, 20);
		});
		steps.add(() -> {
			record("it goes out when the mouse leaves it", !item().isHovered());
			int[] centre = screenCentre(item());
			Gdx.input.getInputProcessor().mouseMoved(centre[0], centre[1]);
			press(Keys.ESCAPE);
		});
		steps.add(() -> {
			record("nothing is lit under the pause screen", !item().isHovered());
			Frames.write(GameHarness.grab(), new File(OUTPUT, "hover-paused.png"));
			press(Keys.ESCAPE);
		});
		steps.add(() -> {
			record("and it lights again on resuming", item().isHovered());
			// The pause sign takes a click before the carriage does, so it must not show a
			// lit item through it either.
			Actor button = GVars_UI.mainUi.getRoot().findActor("pauseButton");
			Vector2 centre = button.localToStageCoordinates(new Vector2(button.getWidth() / 2, button.getHeight() / 2));
			GVars_UI.mainUi.stageToScreenCoordinates(centre);
			Gdx.input.getInputProcessor().mouseMoved((int)centre.x, (int)centre.y);
			GameItem item = item();
			Vector3 under = GVars_Camera.camera.unproject(new Vector3(centre.x, centre.y, 0));
			item.posX = under.x - item.objectTexture.getWidth() / 2f;
			item.posY = under.y - item.objectTexture.getHeight() / 2f;
			item.setPosition(item.posX, item.posY);
		});
		steps.add(() -> {
			record("an item under the pause sign stays unlit", !item().isHovered());
			finished = true;
		});

		int[] next = {0};
		double[] due = {2.0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 100_000);
		harness.exitAfterSeconds = 2.0 + steps.size() * 0.5 + 0.4;
		harness.captureAfterSeconds = harness.exitAfterSeconds - 0.1;
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
			due[0] = harness.gameSeconds + 0.5;
		};

		new Lwjgl3Application(harness, gl);
	}

	private static GameItem item()
	{
		for (GameItem item : GVars_Game.currentLevel.listItems)
			if (ITEM.equals(item.name)) return item;
		throw new AssertionError(ITEM + " is not in level " + GVars_Game.currentLevelInt);
	}

	/** The item's centre in screen pixels, y down as the mouse reports it; also keeps its box. */
	private static int[] screenCentre(GameItem item)
	{
		int w = item.objectTexture.getWidth(), h = item.objectTexture.getHeight();
		Vector3 low = GVars_Camera.camera.project(new Vector3(item.posX, item.posY, 0));
		Vector3 high = GVars_Camera.camera.project(new Vector3(item.posX + w, item.posY + h, 0));
		int screenH = Gdx.graphics.getHeight();
		itemOnScreen = new int[] {(int)low.x, screenH - (int)high.y, (int)high.x, screenH - (int)low.y};
		return new int[] {(int)((low.x + high.x) / 2), screenH - (int)((low.y + high.y) / 2)};
	}

	private static void press(int key)
	{
		Gdx.input.getInputProcessor().keyDown(key);
		Gdx.input.getInputProcessor().keyUp(key);
	}

	private static void record(String step, boolean ok)
	{
		seen.add((ok ? "ok   " : "FAIL ") + step);
	}

	private static int yellowness(int p)
	{
		return (((p >> 16) & 0xFF) + ((p >> 8) & 0xFF)) / 2 - (p & 0xFF);
	}

	private static int brightness(int p)
	{
		return ((p >> 16) & 0xFF) + ((p >> 8) & 0xFF) + (p & 0xFF);
	}

	@Test
	@DisplayName("an item lights up under the mouse, and not where a click would not reach it")
	void lightsUnderTheMouse()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		String report = String.join("\n", seen);
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + "\n" + report);
		assertNull(error, error == null ? null : "a step threw: " + error + "\n" + report);
		assertTrue(finished, "not every step ran:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);
		assertEquals(6, seen.size(), report);
	}

	@Test
	@DisplayName("the lit item gains a yellow line outside its shape, and its own pixels stay as they were")
	void looksOutlined() throws IOException
	{
		assertNotNull(lit, "the lit item was never captured");
		// Drawn at its own size in a 1600x900 world stretched to the window (r64), so a screen
		// pixel is a texel only at 1600x900: go through the item's box on screen to find it.
		BufferedImage texture = ImageIO.read(new File(System.getProperty("onboard.assets"), "game/wagon/wa1/" + ITEM));
		double texelsX = texture.getWidth() / (double) (itemOnScreen[2] - itemOnScreen[0]);
		double texelsY = texture.getHeight() / (double) (itemOnScreen[3] - itemOnScreen[1]);
		int margin = 8, yellowed = 0, yellowedOutside = 0, inside = 0;
		long insideChange = 0;
		for (int y = itemOnScreen[1] - margin; y < itemOnScreen[3] + margin; y++)
			for (int x = itemOnScreen[0] - margin; x < itemOnScreen[2] + margin; x++)
			{
				if (x < 0 || y < 0 || x >= lit.getWidth() || y >= lit.getHeight()) continue;
				int tx = (int) Math.floor((x + 0.5 - itemOnScreen[0]) * texelsX), ty = (int) Math.floor((y + 0.5 - itemOnScreen[1]) * texelsY);
				int alpha = tx < 0 || ty < 0 || tx >= texture.getWidth() || ty >= texture.getHeight()
					? 0 : (texture.getRGB(tx, ty) >>> 24);
				int before = resting.getRGB(x, y), after = lit.getRGB(x, y);
				if (yellowness(after) - yellowness(before) > 30)
				{
					yellowed++;
					if (alpha < 128) yellowedOutside++;
				}
				if (alpha == 255 && opaqueAround(texture, tx, ty, 2))
				{
					inside++;
					insideChange += Math.abs(brightness(after) - brightness(before));
				}
			}
		String see = new File(OUTPUT, "hover-item.png").getAbsolutePath();
		assertTrue(yellowed > 150, "hardly any yellow line around the hovered item: " + yellowed + " pixels (see " + see + ")");
		assertTrue(yellowedOutside > yellowed * 0.9, String.format(
			"the yellow is on the item rather than around it: %d of %d pixels outside (see %s)", yellowedOutside, yellowed, see));
		double meanChange = insideChange / (double) Math.max(1, inside);
		assertTrue(inside > 1000 && meanChange < 3, String.format(
			"the item's own pixels changed when lit: mean %.1f over %d pixels (see %s)", meanChange, inside, see));
	}

	private static boolean opaqueAround(BufferedImage texture, int tx, int ty, int radius)
	{
		for (int y = ty - radius; y <= ty + radius; y++)
			for (int x = tx - radius; x <= tx + radius; x++)
				if (x < 0 || y < 0 || x >= texture.getWidth() || y >= texture.getHeight() || (texture.getRGB(x, y) >>> 24) < 255)
					return false;
		return true;
	}
}

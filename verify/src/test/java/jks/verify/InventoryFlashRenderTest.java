package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;

import jks.amain.Main_Application;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * An item reaching the inventory for the first time flashes (r93): a faint white glow behind it
 * rises and falls twice, with the key's next piece (r91, softened in r104), then the bar is black
 * around it again.
 *
 * Read on the screen, frame by frame: the ring between the item's square and the glow's edge is
 * the black bar at rest, so its brightness is the flash and nothing else.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InventoryFlashRenderTest
{
	private static final double PICK_AT = 2.0;
	/** Two pulses of 0.5 s, and a margin. */
	private static final double WATCH_FOR = 1.4;

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;
	private static volatile Throwable error;

	private static Actor carried;
	/** Brightness of the ring round the item, one per frame after the pickup, with its time. */
	private static final List<double[]> ring = new ArrayList<>();
	private static volatile boolean flashingAtPick, flashingAfter = true, flashingOnSecondCarry = true;
	private static volatile BufferedImage brightest;
	private static double brightestLevel = -1;

	@BeforeAll
	void pickSomethingUp()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = 1;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - inventory flash verification");
		gl.useVsync(false);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = PICK_AT + WATCH_FOR + 0.5;
		harness.captureAfterSeconds = harness.exitAfterSeconds - 0.1;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				double t = harness.gameSeconds - PICK_AT;
				if (carried == null && t >= 0)
				{
					GVars_Game.pickItem(item("sac.png"));
					carried = GVars_Game.inventory.getChildren().peek();
					flashingAtPick = GVars_Game.inventory.isFlashing(carried);
				}
				else if (carried != null && t < WATCH_FOR)
				{
					BufferedImage frameNow = GameHarness.grab();
					double level = ringBrightness(frameNow);
					ring.add(new double[] {t, level});
					if (level > brightestLevel)
					{
						brightestLevel = level;
						brightest = frameNow;
					}
				}
				else if (carried != null && flashingAfter)
				{
					flashingAfter = GVars_Game.inventory.isFlashing(carried);
					ring.add(new double[] {t, ringBrightness(GameHarness.grab())});
					// The same item back in the row is not its first time.
					GVars_Game.inventory.removeActor(carried);
					GVars_Game.inventory.carry(carried);
					flashingOnSecondCarry = GVars_Game.inventory.isFlashing(carried);
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

	/** Mean brightness of a strip just left of the item, inside the glow's box, on screen. */
	private static double ringBrightness(BufferedImage frame)
	{
		float out = carried.getWidth() * 0.12f;
		int x0 = Math.round(carried.getX() - out + 1), x1 = Math.round(carried.getX() - 1);
		int y0 = Math.round(carried.getY() + carried.getHeight() * 0.25f), y1 = Math.round(carried.getY() + carried.getHeight() * 0.75f);
		double sum = 0;
		int n = 0;
		for (int x = x0; x < x1; x++)
			for (int y = y0; y < y1; y++)
			{
				int p = frame.getRGB(x, frame.getHeight() - 1 - y);
				sum += (((p >> 16) & 0xFF) + ((p >> 8) & 0xFF) + (p & 0xFF)) / 3.0;
				n++;
			}
		return n == 0 ? 0 : sum / n;
	}

	@Test
	@DisplayName("a newly carried item flashes twice, faintly, behind it, then the bar is black again")
	void twoFaintPulses() throws Exception
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		if (brightest != null) Frames.write(brightest, new File(OUTPUT, "inventory-flash.png"));

		StringBuilder trace = new StringBuilder();
		for (double[] sample : ring) trace.append(String.format("%.2fs:%.0f ", sample[0], sample[1]));
		String see = "ring brightness by time: " + trace;

		assertTrue(flashingAtPick, "the item did not start flashing as it reached the bar");
		assertTrue(brightestLevel > 30, "the glow never showed round the item. " + see);
		// A little flash, not a strobe (r104): at 0.6 white it read near 150 on the black bar.
		assertTrue(brightestLevel < 90, "the glow is too bright. " + see);

		// Count the pulses: rises through the middle brightness.
		double middle = brightestLevel / 2;
		int pulses = 0;
		boolean above = false;
		for (double[] sample : ring)
		{
			if (!above && sample[1] > middle) pulses++;
			above = sample[1] > middle;
		}
		assertEquals(2, pulses, "the glow should rise twice. " + see);

		assertFalse(flashingAfter, "still flashing after " + WATCH_FOR + " s");
		assertTrue(ring.get(ring.size() - 1)[1] < 10, "the bar round the item is not black again. " + see);
		assertFalse(flashingOnSecondCarry, "the same item flashed a second time");
	}
}

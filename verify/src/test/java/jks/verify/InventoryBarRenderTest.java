package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
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
 * What the player carries: a row centred on the black bar along the bottom, and nothing left in
 * it once the carriage changes (r59).
 *
 * The items used to be laid out from the left edge of the bar, bar-height squares under Ross,
 * and nothing ever cleared them - so carriage 2 opened still showing carriage 1's things, which
 * none of its interactions will take.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InventoryBarRenderTest
{
	/** Both are pickable in carriage 1, and neither hands over a piece of the key. */
	private static final String[] PICK = {"sac.png", "cube.png"};

	private static final double PICK_AT   = 2.0;
	private static final double SHOW_AT   = 2.6;
	private static final double NEXT_AT   = 2.8;
	/** The carriage change fades out for a second and the next one back in over two (r44). */
	private static final double CHECK_AT  = 6.2;

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;

	private static volatile BufferedImage resting, carried, afterChange;
	private static volatile float barHeight;
	private static volatile int carriedCount, leftOverCount, levelAfter;
	private static volatile float rowLeft, rowRight, rowBottom, rowTop, stageWidth;
	private static volatile Throwable error;

	@BeforeAll
	void carryItemsIntoTheNextCarriage()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - inventory bar verification");
		gl.useVsync(false);

		boolean[] done = {false, false, false, false};

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 100_000);
		harness.exitAfterSeconds = CHECK_AT + 0.4;
		harness.captureAfterSeconds = harness.exitAfterSeconds - 0.1;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				if (!done[0] && harness.gameSeconds >= PICK_AT)
				{
					done[0] = true;
					barHeight = GVars_Game.currentLevel.decalYBot;
					resting = GameHarness.grab();
					Frames.write(resting, new File(OUTPUT, "inventory-resting.png"));
					for (String name : PICK) GVars_Game.pickItem(item(name));
				}
				else if (!done[1] && harness.gameSeconds >= SHOW_AT)
				{
					done[1] = true;
					carried = GameHarness.grab();
					Frames.write(carried, new File(OUTPUT, "inventory-carried.png"));
					measureRow();
				}
				else if (!done[2] && harness.gameSeconds >= NEXT_AT)
				{
					done[2] = true;
					GVars_Game.nextLevel();
				}
				else if (!done[3] && harness.gameSeconds >= CHECK_AT)
				{
					done[3] = true;
					levelAfter = GVars_Game.currentLevelInt;
					leftOverCount = GVars_Game.inventory.getChildren().size + GVars_Game.playerInventory.size();
					afterChange = GameHarness.grab();
					Frames.write(afterChange, new File(OUTPUT, "inventory-next-carriage.png"));
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

	/** The box the whole row occupies, in the stage's pixels. */
	private static void measureRow()
	{
		stageWidth = GVars_Game.inventory.getStage().getWidth();
		carriedCount = GVars_Game.inventory.getChildren().size;
		rowLeft = Float.MAX_VALUE;
		rowBottom = Float.MAX_VALUE;
		rowRight = 0;
		rowTop = 0;
		for (Actor carrying : GVars_Game.inventory.getChildren())
		{
			rowLeft = Math.min(rowLeft, carrying.getX());
			rowBottom = Math.min(rowBottom, carrying.getY());
			rowRight = Math.max(rowRight, carrying.getX() + carrying.getWidth());
			rowTop = Math.max(rowTop, carrying.getY() + carrying.getHeight());
		}
	}

	private static int brightness(int p)
	{
		return (((p >> 16) & 0xFF) + ((p >> 8) & 0xFF) + (p & 0xFF)) / 3;
	}

	/** Columns of the bottom bar holding anything but its black, as screen x. */
	private static int[] litColumns(BufferedImage frame)
	{
		int bar = Math.round(barHeight);
		int left = Integer.MAX_VALUE, right = -1, count = 0;
		for (int x = 0; x < frame.getWidth(); x++)
			for (int y = frame.getHeight() - bar + 2; y < frame.getHeight() - 2; y++)
				if (brightness(frame.getRGB(x, y)) > 40)
				{
					count++;
					left = Math.min(left, x);
					right = Math.max(right, x);
					break;
				}
		return new int[] {left, right, count};
	}

	private static BufferedImage bar(BufferedImage frame)
	{
		int bar = Math.round(barHeight);
		return frame.getSubimage(0, frame.getHeight() - bar, frame.getWidth(), bar);
	}

	private static void writeStrip(File target) throws Exception
	{
		BufferedImage[] shown = {resting, carried, afterChange};
		String[] says = {"carriage 1, empty-handed", "carrying two items", "carriage 2, dropped"};
		int pad = 6, label = 18, w = resting.getWidth(), h = Math.round(barHeight);
		BufferedImage strip = new BufferedImage(w + pad * 2, (h + label + pad) * shown.length + pad,
			BufferedImage.TYPE_INT_RGB);
		Graphics2D g = strip.createGraphics();
		g.setColor(new Color(0x20, 0x20, 0x20));
		g.fillRect(0, 0, strip.getWidth(), strip.getHeight());
		for (int i = 0; i < shown.length; i++)
		{
			int y = pad + i * (h + label + pad);
			g.drawImage(bar(shown[i]), pad, y, null);
			g.setColor(Color.WHITE);
			g.drawString(says[i], pad, y + h + label - 4);
		}
		g.dispose();
		Frames.write(strip, target);
	}

	@Test
	@DisplayName("what the player carries sits centred on the bottom bar, and is dropped at the next carriage")
	void centredThenDropped() throws Exception
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertNotNull(resting, "the game never reached carriage 1");
		assertNotNull(carried, "no frame was taken with the items picked up");
		assertNotNull(afterChange, "no frame was taken in the next carriage");

		writeStrip(new File(OUTPUT, "inventory-bar.png"));
		String see = "see " + new File(OUTPUT, "inventory-bar.png").getAbsolutePath();

		assertEquals(PICK.length, carriedCount, "not everything picked up reached the bar");

		// The row against the middle of the bar, not the corner it used to sit in.
		float middle = (rowLeft + rowRight) / 2;
		assertTrue(Math.abs(middle - stageWidth / 2) <= 1, String.format(
			"the row is not centred: it runs %.0f to %.0f on a %.0f-wide stage. %s",
			rowLeft, rowRight, stageWidth, see));
		assertTrue(rowBottom > 0 && rowTop < barHeight, String.format(
			"the row does not sit inside the %.0f-high bar: %.0f to %.0f. %s",
			barHeight, rowBottom, rowTop, see));

		// And the same thing in pixels, because a widget can be placed where nothing is drawn.
		int[] lit = litColumns(carried);
		assertTrue(lit[2] > 0, "the bar is still all black with two items picked up. " + see);
		int litMiddle = (lit[0] + lit[1]) / 2;
		assertTrue(Math.abs(litMiddle - carried.getWidth() / 2) < barHeight / 2, String.format(
			"what is drawn in the bar is not near the middle: columns %d to %d of %d. %s",
			lit[0], lit[1], carried.getWidth(), see));

		// Nothing carried into the next carriage, on the stage or on the screen.
		assertEquals(2, levelAfter, "the next carriage was never reached");
		assertEquals(0, leftOverCount, "items were still carried into carriage 2. " + see);
		int[] after = litColumns(afterChange);
		assertTrue(after[2] == 0, String.format(
			"something is still drawn in carriage 2's bar: %d columns, %d to %d. %s",
			after[2], after[0], after[1], see));
	}
}

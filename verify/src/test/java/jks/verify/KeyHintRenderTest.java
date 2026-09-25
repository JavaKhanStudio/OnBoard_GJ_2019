package jks.verify;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.Vector2;

import jks.amain.Main_Application;
import jks.vinterface.GVars_UI;
import jks.vue.models.game.GVars_Game;

/**
 * How soon a hint reads after the mouse lands on a key part (r57).
 *
 * The three key pieces top right are the game's only hover-to-read text: Clef.buildListener
 * hands the carriage's hint to the shared bubble, which fades in and types. Simon's complaint
 * was that the text "takes a long time before appearing" - the bubble used to hold the text
 * back until the fade had finished, so a whole second passed with an empty cloud on screen.
 *
 * Ink is counted rather than asked of the label: the question is what is legible at a given
 * moment, which is the fade and the typing together, and neither of them reports it. Ink is
 * dark pixels inside the white core of the cloud - the mask comes from the bubble's own
 * texture, eroded, so the cloud's black outline is never mistaken for a letter.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeyHintRenderTest
{
	/** Level 1 has stood still for two seconds by then, so the resting frame is a fair baseline. */
	private static final double HOVER_AT = 2.0;

	/** Seconds after the mouse enters the key part. */
	private static final double[] AFTER = {0.1, 0.25, 0.5, 0.75, 1.0, 1.5};

	/**
	 * A quarter of a second is about as long as a hover can stay blank before it reads as
	 * broken. By then the cloud is fully there and the first word is under way - some seven
	 * characters at 30 a second, which measures around 55 ink pixels at this bubble's size,
	 * against nothing at all while the text waited for the fade.
	 */
	private static final int STARTED_BY = 1;    // index into AFTER: 0.25 s
	private static final int INK_STARTED = 30;
	private static final int READING_BY = 2;    // index into AFTER: 0.50 s
	private static final int INK_READING = 100;

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");
	private static final File BUBBLE =
		new File(System.getProperty("onboard.assets"), "tools/dialog/bubble_think_2.png");

	private static GameHarness harness;

	private static final List<String> seen = new ArrayList<>();
	private static volatile BufferedImage resting;
	private static final BufferedImage[] frames = new BufferedImage[AFTER.length];
	private static final double[] taken = new double[AFTER.length];
	/** The bubble's square on screen: left, top, right, bottom, y down. */
	private static volatile int[] bubbleOnScreen;
	private static volatile boolean mirrored;
	private static volatile boolean[][] interior;
	private static volatile String hintShown;
	private static volatile Throwable error;

	@BeforeAll
	void hoverAKeyPart()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		// The bubble is measured against a still frame: no thought of the next step (r73).
		GVars_Game.thinkAtEachStep = false;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - key hint verification");
		gl.useVsync(false);

		double[] hoveredAt = {-1};
		int[] next = {0};

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = HOVER_AT + AFTER[AFTER.length - 1] + 0.4;
		harness.captureAfterSeconds = harness.exitAfterSeconds - 0.1;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				if (hoveredAt[0] < 0)
				{
					if (harness.gameSeconds < HOVER_AT) return;
					resting = GameHarness.grab();
					Frames.write(resting, new File(OUTPUT, "hint-resting.png"));
					measureBubble();
					int[] part = firstKeyPart();
					Gdx.input.getInputProcessor().mouseMoved(part[0], part[1]);
					hoveredAt[0] = harness.gameSeconds;
					return;
				}

				if (next[0] >= AFTER.length) return;
				double since = harness.gameSeconds - hoveredAt[0];
				if (since < AFTER[next[0]]) return;
				if (hintShown == null) hintShown = GVars_Game.dialogBubble.getText();
				taken[next[0]] = since;
				frames[next[0]] = GameHarness.grab();
				next[0]++;
			}
			catch (Throwable t)
			{
				error = t;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	/**
	 * The centre of the first key piece in screen pixels, y down as the mouse reports it.
	 * Clef lays its three pieces out itself and keeps them to itself, so this reads the widget's
	 * own box and takes the first third of it.
	 */
	private static int[] firstKeyPart()
	{
		Vector2 centre = new Vector2(GVars_Game.clef.getX() + GVars_Game.clef.getWidth() / 6f,
			GVars_Game.clef.getY() + GVars_Game.clef.getHeight() / 2f);
		GVars_UI.mainUi.stageToScreenCoordinates(centre);
		return new int[] {(int) centre.x, (int) centre.y};
	}

	/**
	 * Where the bubble is on screen, and which of its pixels are its white core. It is drawn
	 * mirrored when Ross faces the other way, and then it hangs off the left of its position.
	 */
	private static void measureBubble() throws Exception
	{
		float w = GVars_Game.dialogBubble.getWidth(), h = GVars_Game.dialogBubble.getHeight();
		mirrored = !GVars_Game.ross.getIsReserve();
		float left = mirrored ? GVars_Game.dialogBubble.getX() - w : GVars_Game.dialogBubble.getX();
		Vector2 low = new Vector2(left, GVars_Game.dialogBubble.getY());
		Vector2 high = new Vector2(left + w, GVars_Game.dialogBubble.getY() + h);
		GVars_UI.mainUi.stageToScreenCoordinates(low);
		GVars_UI.mainUi.stageToScreenCoordinates(high);
		bubbleOnScreen = new int[] {
			(int) Math.min(low.x, high.x), (int) Math.min(low.y, high.y),
			(int) Math.max(low.x, high.x), (int) Math.max(low.y, high.y)};

		BufferedImage texture = ImageIO.read(BUBBLE);
		int bw = bubbleOnScreen[2] - bubbleOnScreen[0], bh = bubbleOnScreen[3] - bubbleOnScreen[1];
		boolean[][] solid = new boolean[bh][bw];
		for (int y = 0; y < bh; y++)
			for (int x = 0; x < bw; x++)
			{
				float u = (x + 0.5f) / bw, v = (y + 0.5f) / bh;
				if (mirrored) u = 1 - u;
				int texel = texture.getRGB((int) (u * texture.getWidth()), (int) (v * texture.getHeight()));
				// The cloud's core is white but its alpha is 251, not 255 - it is drawn slightly see-through.
				solid[y][x] = (texel >>> 24) > 200 && brightness(texel) > 200;
			}

		// Eroded, so the cloud's own black outline and its antialiasing stay outside the mask.
		int margin = 4;
		interior = new boolean[bh][bw];
		for (int y = 0; y < bh; y++)
			for (int x = 0; x < bw; x++)
				interior[y][x] = allSolid(solid, x, y, margin);
	}

	private static boolean allSolid(boolean[][] solid, int cx, int cy, int radius)
	{
		for (int y = cy - radius; y <= cy + radius; y++)
			for (int x = cx - radius; x <= cx + radius; x++)
				if (y < 0 || x < 0 || y >= solid.length || x >= solid[0].length || !solid[y][x]) return false;
		return true;
	}

	private static int brightness(int p)
	{
		return (((p >> 16) & 0xFF) + ((p >> 8) & 0xFF) + (p & 0xFF)) / 3;
	}

	/**
	 * Dark pixels inside the cloud's white core: letters, and nothing else the bubble draws.
	 * Dark on its own is not enough - a half-faded cloud still shows the dark carriage through
	 * it, so a pixel only counts as ink if it also went darker than it was before the bubble
	 * came up. A fading cloud only ever brightens what is behind it.
	 */
	private static int ink(BufferedImage frame)
	{
		int count = 0;
		for (int y = 0; y < interior.length; y++)
			for (int x = 0; x < interior[0].length; x++)
			{
				if (!interior[y][x]) continue;
				int fx = bubbleOnScreen[0] + x, fy = bubbleOnScreen[1] + y;
				if (fx < 0 || fy < 0 || fx >= frame.getWidth() || fy >= frame.getHeight()) continue;
				int now = brightness(frame.getRGB(fx, fy));
				if (now < 100 && brightness(resting.getRGB(fx, fy)) - now > 60) count++;
			}
		return count;
	}

	private static BufferedImage crop(BufferedImage frame)
	{
		int margin = 10;
		int x = Math.max(0, bubbleOnScreen[0] - margin), y = Math.max(0, bubbleOnScreen[1] - margin);
		int w = Math.min(frame.getWidth(), bubbleOnScreen[2] + margin) - x;
		int h = Math.min(frame.getHeight(), bubbleOnScreen[3] + margin) - y;
		return frame.getSubimage(x, y, w, h);
	}

	/** One image of every capture in order, each with the time since the hover written on it. */
	private static void writeStrip(File target) throws Exception
	{
		BufferedImage first = crop(frames[0]);
		int pad = 6, label = 18;
		BufferedImage strip = new BufferedImage(
			(first.getWidth() + pad) * frames.length + pad, first.getHeight() + label + pad * 2,
			BufferedImage.TYPE_INT_RGB);
		Graphics2D g = strip.createGraphics();
		g.setColor(new Color(0x20, 0x20, 0x20));
		g.fillRect(0, 0, strip.getWidth(), strip.getHeight());
		for (int i = 0; i < frames.length; i++)
		{
			int x = pad + i * (first.getWidth() + pad);
			g.drawImage(crop(frames[i]), x, pad, null);
			g.setColor(Color.WHITE);
			g.drawString(String.format("%.2fs  ink %d", taken[i], ink(frames[i])), x, first.getHeight() + pad + label - 4);
		}
		g.dispose();
		Frames.write(strip, target);
	}

	@Test
	@DisplayName("a hint starts reading a quarter of a second after the mouse lands on a key part")
	void readsAlmostAtOnce() throws Exception
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertNotNull(resting, "the game never reached the resting frame");
		for (int i = 0; i < frames.length; i++)
			assertNotNull(frames[i], "no frame was captured " + AFTER[i] + "s after the hover");

		assertTrue(hintShown != null && !hintShown.isEmpty(),
			"the mouse did not land on a key part: the bubble was given no text");

		File strip = new File(OUTPUT, "hint-timing.png");
		writeStrip(strip);
		for (int i = 0; i < frames.length; i++)
		{
			Frames.write(frames[i], new File(OUTPUT, String.format("hint-%04dms.png", Math.round(AFTER[i] * 1000))));
			seen.add(String.format("%.2fs after the hover: ink %d", taken[i], ink(frames[i])));
		}
		// Where ink was looked for, in case a later change makes these counts look wrong.
		BufferedImage maskImage = new BufferedImage(interior[0].length, interior.length, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < interior.length; y++)
			for (int x = 0; x < interior[0].length; x++)
				maskImage.setRGB(x, y, interior[y][x] ? 0xFFFFFF : 0);
		Frames.write(maskImage, new File(OUTPUT, "hint-mask.png"));
		String report = "hint: " + hintShown + "\n" + String.join("\n", seen) + "\nsee " + strip.getAbsolutePath();
		System.out.println(report);

		assertTrue(ink(frames[STARTED_BY]) >= INK_STARTED, String.format(
			"the hint is still blank %.2fs after the mouse lands on a key part: ink %d, wanted %d.%n%s%n"
			+ "The bubble must type while it fades in rather than after it (DialogBubble.applyText).",
			taken[STARTED_BY], ink(frames[STARTED_BY]), INK_STARTED, report));

		assertTrue(ink(frames[READING_BY]) >= INK_READING, String.format(
			"there is barely a word in the bubble %.2fs after the hover: ink %d, wanted %d.%n%s",
			taken[READING_BY], ink(frames[READING_BY]), INK_READING, report));

		int last = frames.length - 1;
		assertTrue(ink(frames[last]) > ink(frames[READING_BY]),
			"the hint stopped growing after it started typing:\n" + report);
	}
}

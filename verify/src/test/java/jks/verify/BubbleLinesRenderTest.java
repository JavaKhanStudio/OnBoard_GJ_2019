package jks.verify;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.github.tommyettinger.textra.Layout;
import com.github.tommyettinger.textra.Line;
import com.github.tommyettinger.textra.TypingLabel;

import jks.amain.Main_Application;
import jks.index.Index_Text;
import jks.vinterface.tools.DialogBubble;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;
import jks.vue.models.game.WagonLevel;

/**
 * Every line Ross says, in the bubble he says it in (r163): each carriage in turn, each of its
 * lines typed out in the game's own bubble over the carriage, at 1280 x 720 - the smallest
 * window the options offer, where the letters are smallest - and again in each language of the
 * table (r162). Each cloud is cropped into build/frames/bubble-lines-<language>-<carriage>.png,
 * a sheet a person reads to judge the lines.
 *
 * And each line must fit its cloud: the typed text no taller than the room the cloud leaves it,
 * measured on the label as it laid the line out.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BubbleLinesRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");
	private static final int COLUMNS = 4;

	private static GameHarness harness;
	private static final List<String[]> queue = new ArrayList<>();
	private static final List<String> overflowing = new ArrayList<>();
	private static final List<BufferedImage> crops = new ArrayList<>();
	private static final List<String> cropKeys = new ArrayList<>();
	private static volatile int carriage = 0, sheets, languageAt = 0;
	private static volatile double shownAt = -1, loadedAt;
	private static volatile boolean finished;
	private static volatile Throwable error;

	@BeforeAll
	void sayEveryLine()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = 1;
		GVars_Game.thinkAtEachStep = false;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - bubble lines");
		gl.useVsync(false);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 600;
		harness.frameHook = frame ->
		{
			if (error != null || finished) return;
			try
			{
				double t = harness.gameSeconds;
				if (carriage == 0 && t >= 2.0)
				{
					Index_Text.setLanguage(Index_Text.languages().get(0));
					enter(1);
				}
				else if (carriage > 0 && shownAt < 0 && t >= loadedAt + 1.5)
					next();
				else if (shownAt >= 0 && t >= shownAt + 0.6)
				{
					grab();
					next();
				}
			}
			catch (Throwable e)
			{
				if (error == null) error = e;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	/** Loads the carriage and queues every line of it: hints, then each item's lines. */
	private static void enter(int n)
	{
		boolean fresh = carriage == 0;
		carriage = n;
		if (!fresh)
		{
			GVars_Game.currentLevelInt = n;
			GVars_Game.loadLevel(n);
		}
		WagonLevel level = GVars_Game.currentLevel;
		queue.clear();
		for (String key : new String[] {level.hint1, level.hint2, level.hint3})
			queue.add(new String[] {key});
		for (GameItem item : level.listItems)
		{
			queue.add(new String[] {GameItem.clickKey(n, item.name)});
			if (item.message_Crucial_1 != null) queue.add(new String[] {item.message_Crucial_1});
			if (item.message_Crucial_2 != null) queue.add(new String[] {item.message_Crucial_2});
			if (item.name_Interaction_1 != null && item.name_Interaction_2 != null)
			{
				queue.add(new String[] {GameItem.afterKey(n, item.name, 1)});
				queue.add(new String[] {GameItem.afterKey(n, item.name, 2)});
			}
		}
		queue.removeIf(k -> !Index_Text.has(k[0]));
		loadedAt = harness.gameSeconds;
		shownAt = -1;
	}

	private static void next() throws Exception
	{
		if (queue.isEmpty())
		{
			writeSheet();
			if (carriage < GVars_Game.LEVEL_COUNT)
				enter(carriage + 1);
			else if (languageAt + 1 < Index_Text.languages().size())
			{
				Index_Text.setLanguage(Index_Text.languages().get(++languageAt));
				enter(1);
			}
			else
			{
				finished = true;
				Gdx.app.exit();
			}
			return;
		}
		String key = queue.remove(0)[0];
		cropKeys.add(Index_Text.getLanguage() + " " + key);
		DialogBubble bubble = GVars_Game.dialogBubble;
		bubble.applyText(Index_Text.get(key));
		typing(bubble).skipToTheEnd();
		shownAt = harness.gameSeconds;
	}

	private static void grab() throws Exception
	{
		DialogBubble bubble = GVars_Game.dialogBubble;
		TypingLabel typing = typing(bubble);
		String key = cropKeys.get(cropKeys.size() - 1), text = Index_Text.get(key.substring(key.indexOf(' ') + 1));
		float needed = typing.getPrefHeight(), room = typing.getHeight();
		System.out.println("r163 " + key + ": " + text.length() + " chars, text "
			+ Math.round(needed) + " px tall in " + Math.round(room) + ", cloud " + Math.round(bubble.cloudBox()[2]) + " wide");
		System.out.println("r182 " + key + " | cloud " + Math.round(bubble.cloudBox()[2]) + " | " + wrapped(typing));
		if (needed > room + 1)
			overflowing.add(key + " (" + Math.round(needed) + " px in " + Math.round(room) + ")");

		BufferedImage frame = GameHarness.grab();
		float[] cloud = bubble.cloudBox();
		int h = frame.getHeight(), margin = 12;
		int x0 = Math.max(0, Math.round(cloud[0]) - margin), x1 = Math.min(frame.getWidth(), Math.round(cloud[0] + cloud[2]) + margin);
		int y0 = Math.max(0, Math.round(h - cloud[1] - cloud[3]) - margin), y1 = Math.min(h, Math.round(h - cloud[1]) + margin);
		crops.add(frame.getSubimage(x0, y0, x1 - x0, y1 - y0));
		shownAt = -1;
	}

	/** One sheet a carriage: its clouds in a grid, the key under each. */
	private static void writeSheet() throws Exception
	{
		if (crops.isEmpty()) return;
		int cw = 0, ch = 0;
		for (BufferedImage c : crops) { cw = Math.max(cw, c.getWidth()); ch = Math.max(ch, c.getHeight()); }
		int rows = (crops.size() + COLUMNS - 1) / COLUMNS, label = 18;
		BufferedImage sheet = new BufferedImage(cw * COLUMNS, (ch + label) * rows, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = sheet.createGraphics();
		g.setColor(new Color(90, 90, 90));
		g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
		for (int i = 0; i < crops.size(); i++)
		{
			int x = (i % COLUMNS) * cw, y = (i / COLUMNS) * (ch + label);
			g.drawImage(crops.get(i), x, y, null);
			g.setColor(Color.WHITE);
			g.drawString(cropKeys.get(i), x + 4, y + ch + 14);
		}
		g.dispose();
		Frames.write(sheet, new File(OUTPUT, "bubble-lines-" + Index_Text.getLanguage() + "-" + carriage + ".png"));
		sheets++;
		crops.clear();
		cropKeys.clear();
	}

	/**
	 * The line as the label broke it, its rows joined by " / ": what atelier's Lines page is
	 * held against (r182, tests/renders/r182/lines_vs_game.py reads it).
	 */
	private static String wrapped(TypingLabel typing) throws Exception
	{
		Field field = TypingLabel.class.getDeclaredField("workingLayout");
		field.setAccessible(true);
		Layout layout = (Layout) field.get(typing);
		StringBuilder rows = new StringBuilder();
		for (int i = 0; i < layout.lines(); i++)
		{
			Line line = layout.getLine(i);
			if (i > 0) rows.append(" / ");
			for (int g = 0; g < line.glyphs.size; g++)
			{
				char c = (char) line.glyphs.get(g);
				if (c != '\n') rows.append(c);
			}
		}
		return rows.toString();
	}

	private static TypingLabel typing(DialogBubble bubble) throws Exception
	{
		Field field = DialogBubble.class.getDeclaredField("typing");
		field.setAccessible(true);
		return (TypingLabel) field.get(bubble);
	}

	@Test
	@DisplayName("every line of the four carriages, in every language, is typed into the bubble and kept on a sheet")
	void everyLineIsDrawn()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertTrue(finished && sheets == GVars_Game.LEVEL_COUNT * Index_Text.languages().size(), "only " + sheets + " carriages were drawn");
	}

	@Test
	@DisplayName("every line fits its cloud")
	void everyLineFits()
	{
		assertTrue(finished, "the lines were not all drawn");
		assertTrue(overflowing.isEmpty(), "lines taller than their cloud leaves room for: " + overflowing);
	}
}

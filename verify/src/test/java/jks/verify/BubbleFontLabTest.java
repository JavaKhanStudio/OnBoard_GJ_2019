package jks.verify;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.github.tommyettinger.textra.Font;
import com.github.tommyettinger.textra.TypingLabel;
import com.kotcrab.vis.ui.widget.VisLabel;

import jks.amain.Main_Application;
import jks.index.Index_Text;
import jks.vinterface.GVars_UI;
import jks.vinterface.font.GVars_Font;
import jks.vinterface.font.Index_Fonts.Enum_Fonts;
import jks.vinterface.tools.DialogBubble;
import jks.vinterface.tools.DialogBubble.DialogSize;

/**
 * A lab, not a gate (r103): the game's own thought bubble, once in the face it ships with and
 * once in every candidate in lab-assets/fonts/bubble/ (tools/fetch_bubble_fonts.sh), at the size
 * and over the carriage the player reads it at. Each face is sized to the shipped face's capital height,
 * so they compare as faces and not as sizes. The frames are for a person to choose from:
 * bubble-font-candidates-<page>.png, fourteen candidates a page, the shipped face first on each.
 * Every other bubble types the longest line of the carriages, which must still fit the cloud.
 * With no candidates it draws only the shipped face.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BubbleFontLabTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");
	private static final File CANDIDATES = new File(System.getProperty("onboard.labAssets", "../lab-assets"), "fonts/bubble");
	/** A line of middling length, and the longest the carriages have: what must still fit. */
	private static final String TEXT = "wa1.cage.message1";
	private static final int PER_ROW = 5, PER_PAGE = 14;

	private static GameHarness harness;
	private static final List<String> drawn = new ArrayList<>();
	private static final List<File> faces = new ArrayList<>();
	private static final List<com.badlogic.gdx.scenes.scene2d.Actor> onPage = new ArrayList<>();
	private static volatile int page = -1, pages;
	private static volatile double pageAt;
	private static volatile boolean grabbed;
	private static volatile Throwable error;

	@BeforeAll
	void drawTheBubbles()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - bubble font lab");
		gl.useVsync(false);
		gl.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 30;
		harness.frameHook = frame ->
		{
			if (error != null || grabbed) return;
			try
			{
				if (page < 0 && harness.gameSeconds >= 2.5)
					showPage(page = 0);
				else if (page >= 0 && harness.gameSeconds >= pageAt + 2.5)
				{
					Frames.write(GameHarness.grab(), new File(OUTPUT, "bubble-font-candidates-" + (page + 1) + ".png"));
					if (page + 1 < pages)
						showPage(++page);
					else
					{
						grabbed = true;
						Gdx.app.exit();
					}
				}
			}
			catch (Throwable e)
			{
				if (error == null) error = e;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	/** Hides the carriage's own bubble, then lays one page of faces out in rows, name underneath. */
	private static void showPage(int index) throws Exception
	{
		jks.vue.models.game.GVars_Game.dialogBubble.setVisible(false);
		if (faces.isEmpty())
		{
			File[] found = CANDIDATES.listFiles((d, n) -> n.endsWith(".ttf"));
			if (found != null)
			{
				Arrays.sort(found);
				faces.addAll(Arrays.asList(found));
			}
			pages = Math.max(1, (faces.size() + PER_PAGE - 1) / PER_PAGE);
		}
		for (com.badlogic.gdx.scenes.scene2d.Actor actor : onPage)
			actor.remove();
		onPage.clear();

		List<File> shown = new ArrayList<>();
		shown.add(null);   // the shipped face, on every page
		shown.addAll(faces.subList(Math.min(faces.size(), index * PER_PAGE), Math.min(faces.size(), (index + 1) * PER_PAGE)));

		int pixels = Gdx.graphics.getWidth() / Enum_Fonts.BUBBLE_LARGE_TEXT_MEDIUM.basedSizeDevide;
		float capHeight = GVars_Font.buildLabel(Enum_Fonts.BUBBLE_LARGE_TEXT_MEDIUM).font.getCapHeight();
		for (int i = 0; i < shown.size(); i++)
		{
			File face = shown.get(i);
			DialogBubble bubble = new DialogBubble(DialogSize.BUBBLE_LARGE_TEXT_MEDIUM);
			TypingLabel typing = typing(bubble);
			String name = "shipped: " + new File(Enum_Fonts.BUBBLE_LARGE_TEXT_MEDIUM.path).getName().replace(".ttf", "").replace("-Regular", "");
			if (face != null)
			{
				FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.absolute(face.getAbsolutePath()));
				FreeTypeFontParameter parameter = new FreeTypeFontParameter();
				parameter.characters = GVars_Font.CHARACTERS;
				parameter.size = pixels;
				BitmapFont font = generator.generateFont(parameter);
				int matched = Math.round(pixels * capHeight / font.getCapHeight());
				if (matched != pixels)
				{
					parameter.size = matched;
					font = generator.generateFont(parameter);
				}
				typing.setFont(new Font(font));
				name = face.getName().replace(".ttf", "").replace("-Regular", "") + " " + matched + "px";
			}
			int x = 30 + (i % PER_ROW) * 250, y = 720 - 225 - (i / PER_ROW) * 225;
			bubble.setBubblePosition(x, y);
			bubble.applyText(Index_Text.get(i % 2 == 0 ? TEXT : longest()));
			typing.skipToTheEnd();
			GVars_UI.mainUi.addActor(bubble);

			VisLabel label = new VisLabel(name, com.badlogic.gdx.graphics.Color.BLACK);
			label.setPosition(x + 20, y + 200);
			GVars_UI.mainUi.addActor(label);
			onPage.add(bubble);
			onPage.add(label);
			drawn.add(name);
		}
		pageAt = harness.gameSeconds;
	}

	private static String longest()
	{
		String longest = "";
		for (String key : Index_Text.rows().keySet())
			if (key.startsWith("wa") && Index_Text.get(key).length() > longest.length())
				longest = Index_Text.get(key);
		return longest;
	}

	private static TypingLabel typing(DialogBubble bubble) throws Exception
	{
		Field field = DialogBubble.class.getDeclaredField("typing");
		field.setAccessible(true);
		return (TypingLabel) field.get(bubble);
	}

	@Test
	@DisplayName("every candidate face builds a bubble without throwing, and the frame is kept")
	void everyFaceDraws()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a bubble threw: " + error);
		assertTrue(grabbed, "the frame was never grabbed");
		System.out.println("Drew " + drawn + " into build/frames/bubble-font-candidates-*.png");
	}
}

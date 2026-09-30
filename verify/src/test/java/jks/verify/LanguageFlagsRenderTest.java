package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

import jks.amain.Main_Application;
import jks.index.Index_Text;
import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vinterface.overlay.OverlayOptions;

/**
 * French or English, picked with a flag on the start menu (r162). Clicks the English flag the
 * way the mouse does - through the game's input processor, at the flag's place on the screen -
 * and checks the menu letters itself again, the pick is written to the config, and the Options
 * screen opened after it is in English too. The frames are kept for a person to look at.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LanguageFlagsRenderTest
{
	private static final File OUTPUT = new File(System.getProperty("onboard.verify"), "build/frames");

	private static GameHarness harness;
	private static TempConfig config;

	private static final Checks seen = new Checks();
	private static Steps steps;
	private static volatile boolean finished;

	@BeforeAll
	void clickTheEnglishFlag() throws Exception
	{
		// The pick is saved as it is made; keep the real config out of it.
		config = TempConfig.use();
		Files.delete(config.path);

		Main_Application.startPoint = Main_Application.StartPoint.START_SCREEN;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - language flags verification");
		gl.useVsync(false);

		steps = new Steps(2.5, 1.2);
		steps.add(() -> {
			Frames.write(GameHarness.grab(), new File(OUTPUT, "language-fr-menu.png"));
			seen.record("the menu starts in French", Index_Text.getLanguage().equals("fr") && labelSays("Jouer"));
			seen.record("both flags are on the menu", find("flag.fr") != null && find("flag.en") != null);
			seen.record("the French flag is the one lit", find("flag.fr").getColor().a > find("flag.en").getColor().a);
			click(find("flag.en"));
		});
		steps.add(() -> {
			seen.record("a click on the English flag reads English", Index_Text.getLanguage().equals("en"));
			seen.record("the menu letters itself again", labelSays("Play") && labelSays("Quit") && !labelSays("Jouer"));
			seen.record("the English flag is the one lit now", find("flag.en").getColor().a > find("flag.fr").getColor().a);
			String saved = Files.exists(config.path) ? Files.readString(config.path, StandardCharsets.UTF_8) : "";
			seen.record("the pick is kept in the config", saved.replaceAll("\\s", "").contains("\"language\":\"en\""));
			Frames.write(GameHarness.grab(), new File(OUTPUT, "language-en-menu.png"));
			press(Keys.DOWN); press(Keys.DOWN); press(Keys.ENTER);
		});
		steps.add(() -> {
			seen.record("Options opened", GVars_Heart.vue.overlay instanceof OverlayOptions);
			seen.record("and its words are English", labelSays("Graphics") && labelSays("Resolution"));
			seen.record("the flags are not over the Options", !find("languageFlags").isVisible() || find("languageFlags").getColor().a < 0.05f);
			Frames.write(GameHarness.grab(), new File(OUTPUT, "language-en-options.png"));
			finished = true;
		});

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = steps.seconds() + 0.5;
		steps.drive(harness);

		new Lwjgl3Application(harness, gl);
	}

	@AfterAll
	void restore() throws Exception
	{
		config.restore();
	}

	private static InputProcessor processor()
	{
		return Gdx.input.getInputProcessor();
	}

	private static void press(int key)
	{
		processor().keyDown(key);
		processor().keyUp(key);
	}

	/** A left click in the middle of the actor, in screen pixels, through the game's own input. */
	private static void click(Actor actor)
	{
		Vector2 middle = actor.localToStageCoordinates(new Vector2(actor.getWidth() / 2, actor.getHeight() / 2));
		GVars_UI.mainUi.stageToScreenCoordinates(middle);
		processor().mouseMoved((int) middle.x, (int) middle.y);
		processor().touchDown((int) middle.x, (int) middle.y, 0, 0);
		processor().touchUp((int) middle.x, (int) middle.y, 0, 0);
	}

	private static Actor find(String name)
	{
		return GVars_UI.mainUi.getRoot().findActor(name);
	}

	private static boolean labelSays(String text)
	{
		return labelSays(GVars_UI.mainUi.getRoot(), text);
	}

	private static boolean labelSays(Group group, String text)
	{
		for (Actor child : group.getChildren())
		{
			if (child instanceof Label && ((Label) child).getText().toString().equals(text) && child.isVisible())
				return true;
			if (child instanceof Group && labelSays((Group) child, text))
				return true;
		}
		return false;
	}

	@Test
	@DisplayName("the English flag letters the menu and the screens after it in English, and is kept")
	void englishIsPicked()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(steps.error, steps.error == null ? null : "a step threw: " + steps.error);
		String report = String.join("\n", seen);
		System.out.println(report);
		assertTrue(finished, "the steps did not all run:\n" + report);
		assertEquals(List.of(), seen.stream().filter(s -> s.startsWith("FAIL")).toList(), report);
	}
}

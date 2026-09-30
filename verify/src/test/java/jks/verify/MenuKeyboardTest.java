package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;

import jks.amain.Main_Application;
import jks.index.Index_Text;
import jks.input.GVars_Inputs;
import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vinterface.StartScreen_SmoothSideSelect;
import jks.vinterface.controlling.Utils_Controllable;
import jks.vinterface.overlay.OverlayCredits;
import jks.vinterface.overlay.OverlayOptions;
import jks.vue.GVars_Fade;
import jks.vue.models.game.Vue_Game;

/**
 * The menus from the keyboard alone. Built as stubs in 2019 and never called, so until r11
 * every menu needed the mouse.
 *
 * The keys go in through the game's own input processor - the stage and the keyboard
 * handler together - so this is the path a real key press takes, minus GLFW.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MenuKeyboardTest
{
	private static final File OUTPUT = new File(System.getProperty("onboard.verify"), "build/frames");

	private static GameHarness harness;
	private static TempConfig config;

	/** What each step saw, in order. A failed step names itself. */
	private static final Checks seen = new Checks();
	private static Steps steps;
	private static volatile boolean finished;

	@BeforeAll
	void driveTheMenus() throws Exception
	{
		// The sound and mipmaps settings save as they change; keep the real config out of it.
		config = TempConfig.use();

		Main_Application.startPoint = Main_Application.StartPoint.START_SCREEN;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - keyboard menu verification");
		gl.useVsync(false);

		// Each step runs once the one before it has had this long on the game's clock, so
		// the slide-in animations have somewhere to go and the frames show the real thing.
		steps = new Steps(1.8, 0.8);
		steps.add(() -> {
			seen.record("hidden before any key", !GVars_UI.focusShown);
			press(Keys.DOWN);
			seen.record("first key only shows the focus", GVars_UI.focusShown && focusedIs(0, 0));
		});
		steps.add(() -> {
			press(Keys.DOWN);
			seen.record("down moves to Options", focusedIs(0, 1));
		});
		steps.add(() -> {
			Frames.write(GameHarness.grab(), new File(OUTPUT, "menu-keyboard-start.png"));
			press(Keys.ENTER);
			seen.record("enter opens the options", GVars_Heart.vue.overlay instanceof OverlayOptions);
			seen.record("options start on the resolution box", focusedIs(1, 0));
		});
		steps.add(() -> {
			press(Keys.DOWN); press(Keys.DOWN);
			seen.record("down reaches full screen", focusedIs(1, 2));
			// Full screen is the third row; the sound board's third row is the effects slider (r39).
			press(Keys.RIGHT);
			seen.record("right crosses to the effects slider", focused() instanceof Slider);
			float before = ((Slider)focused()).getValue();
			press(Keys.LEFT);
			seen.record("left turns the effects down a step", ((Slider)focused()).getValue() < before);
			press(Keys.UP);
			seen.record("up reaches the volume slider", focused() instanceof Slider);
			before = ((Slider)focused()).getValue();
			press(Keys.LEFT);
			seen.record("left turns the volume down a step", ((Slider)focused()).getValue() < before);
			press(Keys.UP);
			Button mute = (Button)focused();
			press(Keys.ENTER);
			seen.record("enter ticks the mute box", mute.isChecked());
			press(Keys.ENTER);
			seen.record("enter again unticks it", !mute.isChecked());
			press(Keys.LEFT); press(Keys.DOWN); press(Keys.DOWN); press(Keys.DOWN); press(Keys.DOWN);
			seen.record("down reaches mipmaps", focusedIs(1, 4));
		});
		steps.add(() -> {
			Frames.write(GameHarness.grab(), new File(OUTPUT, "menu-keyboard-options.png"));
			press(Keys.ESCAPE);
			seen.record("escape leaves the options", GVars_Heart.vue.overlay == null);
			seen.record("and hands the keys back to the menu",
				GVars_UI.currentControllable instanceof StartScreen_SmoothSideSelect && focusedIs(0, 0));
		});
		steps.add(() -> {
			press(Keys.DOWN); press(Keys.DOWN);
			press(Keys.ENTER);
			seen.record("enter on Credits opens them", GVars_Heart.vue.overlay instanceof OverlayCredits);
		});
		steps.add(() -> {
			Frames.write(GameHarness.grab(), new File(OUTPUT, "menu-keyboard-credits.png"));
			press(Keys.ENTER);
			seen.record("enter on the return sign closes the credits", GVars_Heart.vue.overlay == null);
		});
		// The language flags (r162) are columns of their own right of the entries (r170).
		steps.add(() -> {
			press(Keys.RIGHT);
			seen.record("right reaches the French flag", focusedIs(1, 0) && "flag.fr".equals(focused().getName()));
			press(Keys.RIGHT);
			seen.record("right again reaches the English flag", focusedIs(2, 0) && "flag.en".equals(focused().getName()));
		});
		steps.add(() -> {
			Frames.write(GameHarness.grab(), new File(OUTPUT, "menu-keyboard-flag.png"));
			press(Keys.ENTER);
			seen.record("enter on it plays in English", "en".equals(Index_Text.getLanguage()));
			seen.record("and the menu reads English", menuSays("Play") && !menuSays("Jouer"));
		});
		steps.add(() -> {
			Frames.write(GameHarness.grab(), new File(OUTPUT, "menu-keyboard-english.png"));
			press(Keys.LEFT);
			press(Keys.ENTER);
			seen.record("left and enter on the French flag play in French again",
				"fr".equals(Index_Text.getLanguage()) && menuSays("Jouer"));
			press(Keys.LEFT);
			seen.record("left goes back to the entries", focusedIs(0, 0));
		});
		steps.add(() -> {
			processor().mouseMoved(20, 20);
			seen.record("moving the mouse puts the focus away", !GVars_UI.focusShown);
			press(Keys.ENTER);
			seen.record("so enter shows it again rather than starting the game",
				GVars_UI.focusShown && !(GVars_Heart.vue instanceof Vue_Game));
			press(Keys.ENTER);
			seen.record("and the next enter on Jouer fades the menu out", GVars_Fade.isFading());
			press(Keys.ENTER);
		});
		// A second to black, where the game view takes over.
		steps.add(() -> {});
		steps.add(() -> {
			Frames.write(GameHarness.grab(), new File(OUTPUT, "menu-keyboard-fading-in.png"));
			seen.record("the game view took over at black", GVars_Heart.vue instanceof Vue_Game);
			seen.record("the game has no menu driving the keys", GVars_UI.currentControllable == null);
		});
		// The first carriage fades in for two seconds, and the keys wait for it.
		steps.add(() -> {});
		steps.add(() -> {
			seen.record("the fade is over", !GVars_Fade.isFading());
			processor().keyDown(Keys.RIGHT);
			seen.record("right walks Ross again in the game", GVars_Inputs.rightPressed);
			processor().keyUp(Keys.RIGHT);
			finished = true;
		});

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = steps.seconds() + 0.5;
		harness.captureAfterSeconds = harness.exitAfterSeconds - 0.1;
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

	private static Actor focused()
	{
		return Utils_Controllable.getFocused();
	}

	/** Whether one of the start menu's entries is lettered this word. */
	private static boolean menuSays(String word)
	{
		return says(GVars_UI.mainUi.getRoot(), word);
	}

	private static boolean says(Actor actor, String word)
	{
		if (actor instanceof Label && word.contentEquals(((Label)actor).getText()))
			return true;
		if (actor instanceof Group)
			for (Actor child : ((Group)actor).getChildren())
				if (says(child, word)) return true;
		return false;
	}

	private static boolean focusedIs(int column, int row)
	{
		return GVars_UI.cursorPos != null && GVars_UI.cursorPos.x == column && GVars_UI.cursorPos.y == row;
	}

	@Test
	@DisplayName("the whole menu can be driven from the keyboard")
	void menusFromTheKeyboard()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (steps.error != null) steps.error.printStackTrace();
		String report = String.join("\n", seen);
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + "\n" + report);
		assertNull(steps.error, steps.error == null ? null : "a step threw: " + steps.error + "\n" + report);
		assertTrue(finished, "not every step ran:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);
		assertEquals(30, seen.size(), report);
	}
}

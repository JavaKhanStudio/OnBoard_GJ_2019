package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;

import jks.amain.Main_Application;
import jks.input.GVars_Inputs;
import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vinterface.StartScreen_SmoothSideSelect;
import jks.vinterface.controlling.Utils_Controllable;
import jks.vinterface.overlay.OverlayCredits;
import jks.vinterface.overlay.OverlayOptions;
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
	private static Path config;
	private static String previousConfig;

	/** What each step saw, in order. A failed step names itself. */
	private static final List<String> seen = new ArrayList<>();
	private static volatile Throwable error;
	private static volatile boolean finished;

	private interface Step { void run() throws Exception; }

	@BeforeAll
	void driveTheMenus() throws Exception
	{
		// The sound and mipmaps settings save as they change; keep the real config out of it.
		config = Files.createTempFile("onboard-config", "");
		previousConfig = System.getProperty("onboard.config");
		System.setProperty("onboard.config", config.toString());

		Main_Application.startPoint = Main_Application.StartPoint.START_SCREEN;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - keyboard menu verification");
		gl.useVsync(false);

		// Each step runs once the one before it has had this long on the game's clock, so
		// the slide-in animations have somewhere to go and the frames show the real thing.
		List<Step> steps = new ArrayList<>();
		steps.add(() -> {
			record("hidden before any key", !GVars_UI.focusShown);
			press(Keys.DOWN);
			record("first key only shows the focus", GVars_UI.focusShown && focusedIs(0, 0));
		});
		steps.add(() -> {
			press(Keys.DOWN);
			record("down moves to Options", focusedIs(0, 1));
		});
		steps.add(() -> {
			Frames.write(GameHarness.grab(), new File(OUTPUT, "menu-keyboard-start.png"));
			press(Keys.ENTER);
			record("enter opens the options", GVars_Heart.vue.overlay instanceof OverlayOptions);
			record("options start on the resolution box", focusedIs(1, 0));
		});
		steps.add(() -> {
			press(Keys.DOWN); press(Keys.DOWN);
			record("down reaches full screen", focusedIs(1, 2));
			press(Keys.RIGHT);
			record("right crosses to the volume slider", focused() instanceof Slider);
			float before = ((Slider)focused()).getValue();
			press(Keys.LEFT);
			record("left turns the volume down a step", ((Slider)focused()).getValue() < before);
			press(Keys.UP);
			Button mute = (Button)focused();
			press(Keys.ENTER);
			record("enter ticks the mute box", mute.isChecked());
			press(Keys.ENTER);
			record("enter again unticks it", !mute.isChecked());
			press(Keys.LEFT); press(Keys.DOWN); press(Keys.DOWN); press(Keys.DOWN); press(Keys.DOWN);
			record("down reaches mipmaps", focusedIs(1, 4));
		});
		steps.add(() -> {
			Frames.write(GameHarness.grab(), new File(OUTPUT, "menu-keyboard-options.png"));
			press(Keys.ESCAPE);
			record("escape leaves the options", GVars_Heart.vue.overlay == null);
			record("and hands the keys back to the menu",
				GVars_UI.currentControllable instanceof StartScreen_SmoothSideSelect && focusedIs(0, 0));
		});
		steps.add(() -> {
			press(Keys.DOWN); press(Keys.DOWN);
			press(Keys.ENTER);
			record("enter on Credits opens them", GVars_Heart.vue.overlay instanceof OverlayCredits);
		});
		steps.add(() -> {
			Frames.write(GameHarness.grab(), new File(OUTPUT, "menu-keyboard-credits.png"));
			press(Keys.ENTER);
			record("enter on the return sign closes the credits", GVars_Heart.vue.overlay == null);
		});
		steps.add(() -> {
			processor().mouseMoved(20, 20);
			record("moving the mouse puts the focus away", !GVars_UI.focusShown);
			press(Keys.ENTER);
			record("so enter shows it again rather than starting the game",
				GVars_UI.focusShown && !(GVars_Heart.vue instanceof Vue_Game));
			press(Keys.ENTER);
			record("and the next enter on Jouer starts the game", GVars_Heart.vue instanceof Vue_Game);
			record("the game has no menu driving the keys", GVars_UI.currentControllable == null);
		});
		steps.add(() -> {
			processor().keyDown(Keys.RIGHT);
			record("right walks Ross again in the game", GVars_Inputs.rightPressed);
			processor().keyUp(Keys.RIGHT);
			finished = true;
		});

		int[] next = {0};
		double[] due = {1.8};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 100_000);
		harness.exitAfterSeconds = 1.8 + steps.size() * 0.8 + 0.5;
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
			due[0] = harness.gameSeconds + 0.8;
		};

		new Lwjgl3Application(harness, gl);
	}

	@AfterAll
	void restore() throws Exception
	{
		if (previousConfig == null) System.clearProperty("onboard.config");
		else System.setProperty("onboard.config", previousConfig);
		Files.deleteIfExists(config);
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

	private static boolean focusedIs(int column, int row)
	{
		return GVars_UI.cursorPos != null && GVars_UI.cursorPos.x == column && GVars_UI.cursorPos.y == row;
	}

	private static void record(String step, boolean ok)
	{
		seen.add((ok ? "ok   " : "FAIL ") + step);
	}

	@Test
	@DisplayName("the whole menu can be driven from the keyboard")
	void menusFromTheKeyboard()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		String report = String.join("\n", seen);
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + "\n" + report);
		assertNull(error, error == null ? null : "a step threw: " + error + "\n" + report);
		assertTrue(finished, "not every step ran:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);
		assertEquals(20, seen.size(), report);
	}
}

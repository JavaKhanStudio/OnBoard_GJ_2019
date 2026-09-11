package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
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
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;

import jks.amain.Main_Application;
import jks.amain.Utils_Config;
import jks.sounds.GVars_Audio;
import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vinterface.overlay.OverlayPause;

/**
 * The pause screen: Escape opens it in a level, it shows the painted panel with its
 * "Couper le son" box, and it closes again. Until r5 nothing could open it, and its art sat
 * unused in ui/icon/pause/. The little PAUSE sign in the corner of a level opens it with the
 * mouse (r18).
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PauseRenderTest
{
	private static final double MAX_DIFFERENCE = 0.12;

	private static final File MODULE = new File(System.getProperty("onboard.verify"));
	private static final File GOLDEN = new File(MODULE, "src/test/resources/golden/pause.png");
	private static final File OUTPUT = new File(MODULE, "build/frames");

	private static GameHarness harness;
	private static Path config;
	private static String previousConfig;

	private static final List<String> seen = new ArrayList<>();
	private static volatile BufferedImage paused;
	private static volatile Throwable error;
	private static volatile boolean finished;

	private interface Step { void run() throws Exception; }

	@BeforeAll
	void pauseALevel() throws Exception
	{
		// The mute box saves as it changes; keep the real config out of it.
		config = Files.createTempFile("onboard-config", "");
		previousConfig = System.getProperty("onboard.config");
		System.setProperty("onboard.config", config.toString());

		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - pause verification");
		gl.useVsync(false);

		List<Step> steps = new ArrayList<>();
		steps.add(() -> {
			GVars_Audio.muted = false;
			press(Keys.ESCAPE);
			record("escape pauses the level", GVars_Heart.isPaused && GVars_Heart.vue.overlay instanceof OverlayPause);
		});
		steps.add(() -> {
			paused = GameHarness.grab();
			press(Keys.UP);
			press(Keys.UP);
			record("up reaches the mute box", GVars_UI.cursorPos.y == 0);
			press(Keys.ENTER);
			record("enter mutes", GVars_Audio.muted && Utils_Config.load().volume == 0f);
		});
		steps.add(() -> {
			Frames.write(GameHarness.grab(), new File(OUTPUT, "pause-muted.png"));
			press(Keys.ENTER);
			record("enter again unmutes, at a volume you can hear",
				!GVars_Audio.muted && Utils_Config.load().volume > 0f);
		});
		steps.add(() -> {
			press(Keys.ESCAPE);
			record("escape resumes", !GVars_Heart.isPaused && GVars_Heart.vue.overlay == null);
			record("and gives the keys back to the level", GVars_UI.currentControllable == null);
		});
		steps.add(() -> {
			Actor button = GVars_UI.mainUi.getRoot().findActor("pauseButton");
			record("the level shows a pause button", button != null && button.isVisible() && button.getStage() != null);
			Frames.write(GameHarness.grab(), new File(OUTPUT, "pause-button.png"));

			// A real click, through the same input processor as the mouse: the stage, then
			// the level's handler, which is what touches the carriage items.
			Vector2 centre = button.localToStageCoordinates(new Vector2(button.getWidth() / 2, button.getHeight() / 2));
			GVars_UI.mainUi.stageToScreenCoordinates(centre);
			boolean taken = Gdx.input.getInputProcessor().touchDown((int)centre.x, (int)centre.y, 0, Buttons.LEFT);
			// The level's handler only claims a touch while paused or in a cinematic, and it is
			// neither yet: so a claimed touch is the button's, and the items never saw it.
			record("the click stops at the button, not on the carriage", taken);
			Gdx.input.getInputProcessor().touchUp((int)centre.x, (int)centre.y, 0, Buttons.LEFT);
			record("the pause button pauses the level", GVars_Heart.isPaused && GVars_Heart.vue.overlay instanceof OverlayPause);
		});
		steps.add(() -> {
			press(Keys.ESCAPE);
			record("and escape resumes from there too", !GVars_Heart.isPaused && GVars_Heart.vue.overlay == null);
			finished = true;
		});

		int[] next = {0};
		double[] due = {2.0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 100_000);
		harness.exitAfterSeconds = 2.0 + steps.size() * 0.6 + 0.4;
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
			due[0] = harness.gameSeconds + 0.6;
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

	private static void press(int key)
	{
		Gdx.input.getInputProcessor().keyDown(key);
		Gdx.input.getInputProcessor().keyUp(key);
	}

	private static void record(String step, boolean ok)
	{
		seen.add((ok ? "ok   " : "FAIL ") + step);
	}

	@Test
	@DisplayName("escape and the pause button pause a level, it resumes, and the mute box works")
	void pausesAndResumes()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		String report = String.join("\n", seen);
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + "\n" + report);
		assertNull(error, error == null ? null : "a step threw: " + error + "\n" + report);
		assertTrue(finished, "not every step ran:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);
		assertEquals(10, seen.size(), report);
	}

	@Test
	@DisplayName("the pause screen still looks the same")
	void matchesGolden() throws Exception
	{
		assertNotNull(paused, "the pause screen was never captured");
		Frames.write(paused, new File(OUTPUT, "pause-actual.png"));

		if (Boolean.getBoolean("onboard.golden.record") || !GOLDEN.isFile())
		{
			Frames.write(paused, GOLDEN);
			System.out.println("Recorded golden pause screen at " + GOLDEN.getAbsolutePath());
			return;
		}

		double difference = Frames.difference(Frames.read(GOLDEN), paused);
		assertTrue(difference <= MAX_DIFFERENCE, String.format(
			"the pause screen renders differently: mean difference %.4f exceeds %.4f (see %s)",
			difference, MAX_DIFFERENCE, new File(OUTPUT, "pause-actual.png").getAbsolutePath()));
	}
}

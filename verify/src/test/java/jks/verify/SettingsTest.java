package jks.verify;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.utils.Array;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jks.amain.GameConfigs;
import jks.amain.Main_Application;
import jks.amain.Utils_Config;
import jks.sounds.GVars_Audio;
import jks.sounds.GVars_AudioManager;
import jks.vars.GVars_Heart;
import jks.vinterface.overlay.OverlayOptions;

/**
 * Every setting on the Options screen, in the running game: each widget starts on what the
 * config says, each change lands in the config file, and Apply neither throws nor undoes
 * anything.
 *
 * Apply threw a NullPointerException on any monitor without a 16:9 mode at exactly 60 Hz
 * (r14), and until then the Graphics widgets always started on the first resolution with
 * vsync and full screen off, so Apply quietly changed back whatever you had not touched (n4).
 * The frame rate box was never applied nor saved at all.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsTest
{
	private static final File OUTPUT = new File(System.getProperty("onboard.verify"), "build/frames");

	private static GameHarness harness;
	private static Path config;
	private static String previousConfig;

	private static final List<String> seen = new ArrayList<>();
	private static volatile Throwable error;
	private static volatile boolean finished;

	private interface Step { void run() throws Exception; }

	/** A step, and how long the game's clock runs before the next one. */
	private static final class Timed
	{
		final Step step;
		final double wait;
		Timed(double wait, Step step) { this.step = step; this.wait = wait; }
	}

	// The widgets, in Block_Resolution.focusOrder() and Block_Sound.focusOrder() order.
	private static SelectBox<String> resolution, fps;
	private static Button fullScreen, vsync, mipmaps, apply, muted;
	private static Slider volume, effects;

	@BeforeAll
	void driveTheSettings() throws Exception
	{
		config = Files.createTempFile("onboard-config", "");
		previousConfig = System.getProperty("onboard.config");
		System.setProperty("onboard.config", config.toString());

		// Nothing here is a default, so a widget that ignores the config shows it. 1366x768
		// is not the first entry the box offers, and it is what the launcher would have read.
		GameConfigs start = new GameConfigs();
		start.width = 1366;
		start.height = 768;
		start.isFullScreen = false;
		start.useVsynch = false;
		start.fps = 30;
		start.volume = 0.4f;
		start.effectsVolume = 0.55f;
		start.useMipmaps = true;
		Utils_Config.current = start;
		Utils_Config.save();
		Utils_Config.load();

		Main_Application.startPoint = Main_Application.StartPoint.START_SCREEN;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - settings verification");
		gl.useVsync(false);

		List<Timed> steps = new ArrayList<>();
		steps.add(new Timed(0.8, () -> {
			openOptions();
		}));
		steps.add(new Timed(0.8, () -> {
			StringBuilder modes = new StringBuilder();
			for (DisplayMode mode : Gdx.graphics.getDisplayModes()) modes.append(mode).append(' ');
			System.out.println("display modes: " + modes + "\nresolution box: " + resolution.getItems());

			Array<String> items = resolution.getItems();
			record("the resolution box is never empty", items.size > 0);
			HashSet<String> unique = new HashSet<>();
			for (String item : items) unique.add(item);
			record("and lists each size once", unique.size() == items.size);

			record("resolution starts on the saved size", "1366x768".equals(resolution.getSelected()));
			record("frame rate starts on the saved one", "30".equals(fps.getSelected()));
			record("full screen starts as saved", !fullScreen.isChecked() && !resolution.isDisabled());
			record("vsync starts as saved", !vsync.isChecked());
			record("mipmaps start as saved", mipmaps.isChecked());
			record("mute starts as saved", !muted.isChecked());
			record("volume starts as saved", Math.abs(volume.getValue() - 0.4f) < 1e-4);
			record("effects start as saved", Math.abs(effects.getValue() - 0.55f) < 1e-4);
			Frames.write(GameHarness.grab(), new File(OUTPUT, "settings-options.png"));

			apply.toggle();
			GameConfigs saved = reread();
			record("apply with nothing changed keeps the size", saved.width == 1366 && saved.height == 768);
			record("and the frame rate", saved.fps == 30);
			record("and vsync and full screen", !saved.useVsynch && !saved.isFullScreen);
			record("and the mipmaps and volume", saved.useMipmaps && Math.abs(saved.volume - 0.4f) < 1e-4);
			resolution.showScrollPane();
		}));
		steps.add(new Timed(0.8, () -> {
			Frames.write(GameHarness.grab(), new File(OUTPUT, "settings-dropdown.png"));
			resolution.hideScrollPane();
			mipmaps.setChecked(false);
			record("mipmaps save the moment they change", !reread().useMipmaps);

			volume.setValue(0.7f);
			record("the volume saves as it changes", Math.abs(reread().volume - 0.7f) < 1e-4);
			record("and reaches the audio", Math.abs(GVars_Audio.masterVolume - 0.7f) < 1e-4);

			effects.setValue(0.25f);
			record("the effects volume saves as it changes", Math.abs(reread().effectsVolume - 0.25f) < 1e-4);
			record("and reaches the audio, apart from the music", Math.abs(GVars_Audio.effectVolume - 0.25f) < 1e-4
				&& Math.abs(GVars_Audio.masterVolume - 0.7f) < 1e-4 && Math.abs(reread().volume - 0.7f) < 1e-4);
			// d9 = A: Volume is the master over both, and the music has no slider of its own.
			record("volume scales the music and the effects alike", Math.abs(GVars_AudioManager.musicVolume() - 0.7f * GVars_Audio.musiqueVolume) < 1e-4
				&& Math.abs(GVars_AudioManager.effectsVolume() - 0.7f * 0.25f) < 1e-4);

			muted.setChecked(true);
			record("mute saves a volume of zero", reread().volume == 0f && GVars_Audio.muted);
			muted.setChecked(false);
			record("unmuting brings the volume back", Math.abs(reread().volume - 0.7f) < 1e-4 && !GVars_Audio.muted);

			resolution.setSelected("1280x720");
			fps.setSelected("60");
			vsync.setChecked(true);
			apply.toggle();
			GameConfigs saved = reread();
			record("apply saves the resolution", saved.width == 1280 && saved.height == 720);
			record("apply saves the frame rate", saved.fps == 60);
			record("apply saves vsync", saved.useVsynch && !saved.isFullScreen);
			record("apply left the other settings alone", !saved.useMipmaps && Math.abs(saved.volume - 0.7f) < 1e-4);
		}));
		// Long enough for Gdx.graphics.getFramesPerSecond(), which counts whole seconds.
		steps.add(new Timed(2.5, () -> {
			fps.setSelected("30");
			vsync.setChecked(false);
			apply.toggle();
			record("the frame rate saves", reread().fps == 30 && !reread().useVsynch);
		}));
		steps.add(new Timed(0.8, () -> {
			// Uncapped with vsync off, this draws hundreds of frames a second offscreen.
			int measured = Gdx.graphics.getFramesPerSecond();
			System.out.println("frames per second with the 30 cap: " + measured);
			record("the frame rate cap is applied (" + measured + " fps)", measured > 0 && measured <= 33);

			fullScreen.setChecked(true);
			DisplayMode desktop = Gdx.graphics.getDisplayMode();
			String desktopSize = desktop.width + "x" + desktop.height;
			record("full screen locks the resolution box", resolution.isDisabled());
			record("on the desktop's size (" + desktopSize + ")",
				!resolution.getItems().contains(desktopSize, false) || desktopSize.equals(resolution.getSelected()));
			fullScreen.setChecked(false);
			record("unticking it gives the window size back",
				!resolution.isDisabled() && "1280x720".equals(resolution.getSelected()));

			fullScreen.setChecked(true);
			apply.toggle();
			GameConfigs saved = reread();
			record("apply saves full screen", saved.isFullScreen);
			record("without overwriting the window size", saved.width == 1280 && saved.height == 720);
		}));
		steps.add(new Timed(0.8, () -> {
			fullScreen.setChecked(false);
			apply.toggle();
			GameConfigs saved = reread();
			record("apply goes back to a window", !saved.isFullScreen && saved.width == 1280 && saved.height == 720);
			record("and the game agrees", !Gdx.graphics.isFullscreen() && !GVars_Heart.isFullScreen);
		}));
		steps.add(new Timed(0.8, () -> {
			press(Keys.ESCAPE);
			record("escape leaves the options", GVars_Heart.vue.overlay == null);
		}));
		steps.add(new Timed(0.8, () -> {
			// Escape hands the keys back to the menu, focus shown, on Jouer.
			press(Keys.DOWN);
			press(Keys.ENTER);
			record("options open again", GVars_Heart.vue.overlay instanceof OverlayOptions);
			findWidgets();
		}));
		steps.add(new Timed(0.8, () -> {
			record("reopened, the resolution is the one applied", "1280x720".equals(resolution.getSelected()));
			record("the frame rate too", "30".equals(fps.getSelected()));
			record("full screen and vsync too", !fullScreen.isChecked() && !vsync.isChecked());
			record("mipmaps and sound too",
				!mipmaps.isChecked() && !muted.isChecked() && Math.abs(volume.getValue() - 0.7f) < 1e-4
				&& Math.abs(effects.getValue() - 0.25f) < 1e-4);
			Frames.write(GameHarness.grab(), new File(OUTPUT, "settings-reopened.png"));
			finished = true;
		}));

		int[] next = {0};
		double[] due = {1.8};
		double total = 1.8;
		for (Timed t : steps) total += t.wait;
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = total + 0.5;
		harness.captureAfterSeconds = harness.exitAfterSeconds - 0.1;
		harness.frameHook = frame ->
		{
			if (error != null || next[0] >= steps.size() || harness.gameSeconds < due[0]) return;
			Timed step = steps.get(next[0]++);
			try
			{
				step.step.run();
			}
			catch (Throwable t)
			{
				error = t;
			}
			due[0] = harness.gameSeconds + step.wait;
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

	/** From the start screen: the first key shows the focus on Jouer, the next one moves to Options. */
	private static void openOptions()
	{
		press(Keys.DOWN);
		press(Keys.DOWN);
		press(Keys.ENTER);
		record("enter opens the options", GVars_Heart.vue.overlay instanceof OverlayOptions);
		findWidgets();
	}

	@SuppressWarnings("unchecked")
	private static void findWidgets()
	{
		OverlayOptions options = (OverlayOptions) GVars_Heart.vue.overlay;
		List<Actor> graphics = options.mapInterface().get(1);
		List<Actor> sound = options.mapInterface().get(2);
		resolution = (SelectBox<String>) graphics.get(0);
		fps = (SelectBox<String>) graphics.get(1);
		fullScreen = (Button) graphics.get(2);
		vsync = (Button) graphics.get(3);
		mipmaps = (Button) graphics.get(4);
		apply = (Button) graphics.get(5);
		muted = (Button) sound.get(0);
		volume = (Slider) sound.get(1);
		effects = (Slider) sound.get(2);
	}

	/** The config file as the next launch would read it, not Utils_Config.current. */
	private static GameConfigs reread() throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return mapper.readValue(config.toFile(), GameConfigs.class);
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

	private static void record(String step, boolean ok)
	{
		seen.add((ok ? "ok   " : "FAIL ") + step);
	}

	@Test
	@DisplayName("every setting starts on the config, saves, and survives Apply")
	void everySetting()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		String report = String.join("\n", seen);
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + "\n" + report);
		assertNull(error, error == null ? null : "a step threw: " + error + "\n" + report);
		assertTrue(finished, "not every step ran:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);
	}
}

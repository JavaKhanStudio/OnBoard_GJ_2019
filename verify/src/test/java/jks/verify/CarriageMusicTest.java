package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.kotcrab.vis.ui.widget.VisTextButton;

import jks.amain.Main_Application;
import jks.sounds.Enum_Music;
import jks.sounds.GVars_Audio;
import jks.sounds.GVars_AudioManager;
import jks.vinterface.GVars_UI;
import jks.vue.models.game.CarriageMusicSwitch;
import jks.vue.models.game.GVars_Game;

/**
 * The music under the carriages (r94, r97). A plain run plays the main song aged for each
 * carriage (GVars_Audio.SHIPPED_CARRIAGE_VARIANT, Simon's pick from the r94 comparison), and a
 * carriage change crossfades the two under the fade to black instead of cutting. A Carriage lab
 * switches it between the main song, the main song aged for the carriage, and a track written
 * for it. The main and modulated songs are one song, so a switch - or the next carriage - goes
 * on from the same bar instead of starting over; an ideal track nobody has made yet falls back
 * to the main song. The ideal tracks are lab-only files in lab-assets/ (r99), absent on a fresh
 * clone: this passes both ways, and says which one it proved. `-PlabAssets=<dir>` points the run
 * at a lab-assets/ elsewhere (a worktree has none of its own).
 *
 * The plain run comes first, with the lab off; the lab's panel is opened over carriage 2 after.
 * One JVM holds one game (forkEvery = 1), so both are one run and one capture.
 *
 * Reads back what came out of the audio device, like MusicTest: neither the carriage change nor
 * the switches may leave a hole in the music. The effects are at zero so the rails cannot hide one.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CarriageMusicTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;

	private static final List<String> seen = new ArrayList<>();
	private static volatile Throwable error;
	private static volatile boolean finished;
	private static AudioCapture capture;
	private static double firstStepSeconds = 1.5;
	/** When nextLevel was called, and so when the fade to carriage 2 began. */
	private static volatile double carriageChangeSeconds = -1;

	/** Whether this run had wa2_ideal in lab-assets/: the report says which path it proved. */
	static volatile boolean idealHere;

	private interface Step { void run() throws Exception; }

	/** A step, and how long to wait after it before the next. */
	private static final class Timed
	{
		final Step step; final double wait;
		Timed(double wait, Step step) { this.step = step; this.wait = wait; }
	}

	@BeforeAll
	void switchTheMusicInTheCarriages() throws Exception
	{
		File captured = AudioCapture.configuredFile();
		if (captured != null) Files.deleteIfExists(captured.toPath());

		GVars_Audio.muted = false;
		GVars_Audio.masterVolume = 1f;
		GVars_Audio.musiqueVolume = 1f;
		GVars_Audio.effectVolume = 0f;
		GVars_Audio.carriageVariant = GVars_Audio.SHIPPED_CARRIAGE_VARIANT;
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = 1;
		Main_Application.lab = false;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - carriage music verification");
		gl.useVsync(false);

		float[] carried = {0};
		List<Timed> steps = new ArrayList<>();
		// The plain run: no lab, the shipped music.
		steps.add(new Timed(1.5, () -> {
			record("a plain run has no music switch", panel() == null);
			record("carriage 1 plays its aged song", playing().equals("wa1_modulated.ogg"));
			record("the entrance crossfade from the menu's song is over", !GVars_AudioManager.isCrossfading());
			carried[0] = position();
			carriageChangeSeconds = harness.gameSeconds;
			GVars_Game.nextLevel();
		}));
		// Black comes at GVars_Fade.OUT_SECONDS; half a crossfade later:
		steps.add(new Timed(1.0, () -> {
			record("carriage 2 plays its own aged song", playing().equals("wa2_modulated.ogg"));
			record("with carriage 1's still fading out under it", GVars_AudioManager.isCrossfading());
			record("from the same bar (" + carried[0] + " s, then " + position() + " s)", position() > carried[0] + 1f);
		}));
		steps.add(new Timed(0.6, () -> {
			record("the crossfade is over", !GVars_AudioManager.isCrossfading());
			record("and carriage 2's song plays alone", playing().equals("wa2_modulated.ogg"));
			CarriageMusicSwitch.open();
		}));
		// The lab, over carriage 2.
		steps.add(new Timed(0.6, () -> {
			record("the lab's switch is over the carriage", panel() != null);
			idealHere = GVars_AudioManager.idealFile(2) != null;
			record("Ideal is dark exactly when wa2_ideal is not in lab-assets", button("Ideal").isDisabled() == !idealHere);
			Frames.write(GameHarness.grab(), new File(OUTPUT, "carriage-music-switch.png"));
			carried[0] = position();
			click("Main");
		}));
		steps.add(new Timed(0.6, () -> {
			record("Main is intro.mp3", playing().equals("intro.mp3"));
			record("going on from the same bar (" + position() + " s)", position() > carried[0]);
			click("Ideal");
		}));
		steps.add(new Timed(0.6, () -> {
			record("Ideal plays wa2's own track, or a dark Ideal leaves the main song on",
				playing().equals(idealHere ? GVars_AudioManager.idealFile(2).name() : "intro.mp3"));
			click("Modulated");
		}));
		steps.add(new Timed(0.6, () -> {
			record("Modulated is carriage 2's aged song again", playing().equals("wa2_modulated.ogg"));
			record("the carriage track is what is asked for", GVars_AudioManager.currentMusic() == Enum_Music.CARRIAGE_2);
			finished = true;
		}));

		int[] next = {0};
		double[] due = {firstStepSeconds};
		double total = firstStepSeconds;
		for (Timed step : steps) total += step.wait;
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = total;
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

		if (captured != null && captured.isFile())
			capture = AudioCapture.read(captured);
	}

	@AfterAll
	void restore()
	{
		Main_Application.lab = false;
		GVars_Audio.carriageVariant = GVars_Audio.SHIPPED_CARRIAGE_VARIANT;
		GVars_Audio.effectVolume = 1f;
	}

	private static Group panel()
	{
		return GVars_UI.mainUi.getRoot().findActor("carriageMusicSwitch");
	}

	private static VisTextButton button(String text)
	{
		for (Actor child : panel().getChildren())
			if (child instanceof VisTextButton && ((VisTextButton) child).getText().toString().equals(text))
				return (VisTextButton) child;
		throw new AssertionError("no " + text + " button in the music switch");
	}

	private static void click(String text)
	{
		// Through the button's own ChangeListener, as a click would.
		button(text).toggle();
	}

	private static String playing()
	{
		Enum_Music track = GVars_AudioManager.currentMusic();
		return track == null ? "nothing" : GVars_AudioManager.fileFor(track).name();
	}

	private static float position()
	{
		return GVars_AudioManager.musicPosition();
	}

	private static void record(String step, boolean ok)
	{
		seen.add((ok ? "ok   " : "FAIL ") + step + (ok ? "" : "  [playing " + playing() + "]"));
	}

	@Test
	@DisplayName("a plain run plays each carriage's aged song, and the lab switches it between the main song, the aged one and its own track")
	void theSwitchChangesTheMusic()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		String report = "lab-assets: wa2 ideal " + (idealHere ? "here" : "absent") + "\n" + String.join("\n", seen);
		System.out.println(report);
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + "\n" + report);
		assertNull(error, error == null ? null : "a step threw: " + error + "\n" + report);
		assertTrue(finished, "not every step ran:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);
		assertEquals(15, seen.size(), report);
	}

	@Test
	@DisplayName("neither the carriage change nor a switch leaves a hole in the music that comes out")
	void noHoleAtTheSwitches()
	{
		File file = AudioCapture.configuredFile();
		if (file == null) return; // to the speakers: nothing to read back
		assertNotNull(capture, "nothing was written to " + file);
		// From just after the first switch to the end: 50 ms windows, none of them silent.
		double[] levels = capture.rms(0.05);
		int from = (int) ((firstStepSeconds + 0.2) / 0.05), to = levels.length - 8;
		StringBuilder profile = new StringBuilder();
		int silent = 0;
		for (int i = from; i < to; i++)
		{
			boolean quiet = levels[i] < 1e-4;
			if (quiet) silent++;
			profile.append(quiet ? '.' : '#');
		}
		assertTrue(silent <= 2, "the music dropped out at a switch ('.' is 50 ms of silence): " + profile);

		// Across the carriage change, from the fade's start to the crossfade's end: no dip.
		// A cut, or a linear crossfade of two tracks, shows as a window far under the rest.
		assertTrue(carriageChangeSeconds > 0, "the carriage change never happened");
		int changeFrom = (int) (carriageChangeSeconds / 0.05), changeTo = (int) ((carriageChangeSeconds + 1.0 + 1.2) / 0.05);
		double[] around = java.util.Arrays.copyOfRange(levels, changeFrom, Math.min(changeTo, levels.length));
		double[] sorted = around.clone();
		java.util.Arrays.sort(sorted);
		double median = sorted[sorted.length / 2], lowest = sorted[0];
		StringBuilder change = new StringBuilder();
		for (double level : around) change.append(' ').append(AudioCapture.db(level));
		System.out.println("carriage change, 50 ms windows:" + change);
		assertTrue(lowest > median * 0.25, "the music dips at the carriage change: lowest " + AudioCapture.db(lowest)
			+ " against a median of " + AudioCapture.db(median) + ":" + change);
	}
}

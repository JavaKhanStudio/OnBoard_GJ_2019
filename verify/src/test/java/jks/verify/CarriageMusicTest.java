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
import jks.vue.models.game.GVars_Game;

/**
 * The music under the carriages (r94). A Carriage lab (-Donboard.lab=true) switches it between
 * the main song, the main song aged for the carriage, and a track written for it. The main and
 * modulated songs are one song, so a switch - or the next carriage - goes on from the same bar
 * instead of starting over; an ideal track nobody has made yet falls back to the main song.
 *
 * Reads back what came out of the audio device, like MusicTest: the switches must not leave
 * a hole in the music.
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

	private interface Step { void run() throws Exception; }

	@BeforeAll
	void switchTheMusicInTheCarriages() throws Exception
	{
		File captured = AudioCapture.configuredFile();
		if (captured != null) Files.deleteIfExists(captured.toPath());

		GVars_Audio.muted = false;
		GVars_Audio.masterVolume = 1f;
		GVars_Audio.musiqueVolume = 1f;
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = 1;
		Main_Application.lab = true;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - carriage music verification");
		gl.useVsync(false);

		List<Step> steps = new ArrayList<>();
		steps.add(() -> {
			record("the music switch is over the carriage", panel() != null);
			record("carriage 1 starts on the main song", playing().equals("intro.mp3"));
			click("Modulated");
		});
		steps.add(() -> {
			record("Modulated plays carriage 1's aged song", playing().equals("wa1_modulated.ogg"));
			// About two seconds in: had it started over it would be under one.
			record("and goes on from the same bar (" + position() + " s)", position() > 1f);
			Frames.write(GameHarness.grab(), new File(OUTPUT, "carriage-music-switch.png"));
			GVars_Game.currentLevelInt = 2;
			GVars_Game.loadLevel(2);
		});
		steps.add(() -> {
			record("carriage 2 plays its own aged song", playing().equals("wa2_modulated.ogg"));
			record("still from the same bar (" + position() + " s)", position() > 1.5f);
			click("Ideal");
		});
		steps.add(() -> {
			boolean made = GVars_AudioManager.idealFile(2) != null;
			record("Ideal plays wa2's own track, or the main song until it is made",
				playing().equals(made ? GVars_AudioManager.idealFile(2).name() : "intro.mp3"));
			click("Main");
		});
		steps.add(() -> {
			record("Main is intro.mp3 again", playing().equals("intro.mp3"));
			record("the carriage track is what is asked for", GVars_AudioManager.currentMusic() == Enum_Music.CARRIAGE_2);
			finished = true;
		});

		int[] next = {0};
		double[] due = {firstStepSeconds};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = firstStepSeconds + steps.size() * 0.6 + 0.4;
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

		if (captured != null && captured.isFile())
			capture = AudioCapture.read(captured);
	}

	@AfterAll
	void restore()
	{
		Main_Application.lab = false;
		GVars_Audio.carriageVariant = Enum_Music.CarriageVariant.MAIN;
	}

	private static Group panel()
	{
		return GVars_UI.mainUi.getRoot().findActor("carriageMusicSwitch");
	}

	private static void click(String text)
	{
		for (Actor child : panel().getChildren())
			if (child instanceof VisTextButton && ((VisTextButton) child).getText().toString().equals(text))
			{
				// Through the button's own ChangeListener, as a click would.
				((VisTextButton) child).toggle();
				return;
			}
		throw new AssertionError("no " + text + " button in the music switch");
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
	@DisplayName("the lab switches each carriage between the main song, its aged version and its own track")
	void theSwitchChangesTheMusic()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		String report = String.join("\n", seen);
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + "\n" + report);
		assertNull(error, error == null ? null : "a step threw: " + error + "\n" + report);
		assertTrue(finished, "not every step ran:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);
		assertEquals(9, seen.size(), report);
	}

	@Test
	@DisplayName("switching leaves no hole in the music that comes out")
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
	}
}

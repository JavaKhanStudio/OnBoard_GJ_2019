package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.GameConfigs;
import jks.amain.Main_Application;
import jks.amain.Utils_Config;
import jks.sounds.Enum_Effect_Sound;
import jks.sounds.Enum_Effect_Sound.Slot;
import jks.sounds.GVars_Audio;
import jks.sounds.GVars_AudioManager;
import jks.vars.GVars_Heart;
import jks.vue.models.Vue_SoundLab;
import jks.vue.models.game.GVars_Game;

/**
 * The game's sound effects: the rails bed and departure (r39), a key piece and a completed level
 * (r45), and the effects volume over all of them.
 *
 * The music is turned off for the whole run, so whatever reaches the capture is an effect. The
 * game view starts the rails by itself; the test then drags the effects volume to zero and back,
 * mutes and unmutes, stops the train and plays a departure. Then it gains the key through the
 * game's own GVars_Game.addKey, piece by piece, and finally opens the sound lab - checking both
 * what the audio manager says and what actually came out of the device.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EffectSoundTest
{
	private static final double CAPTURE_AFTER_SECONDS = 11.8;
	private static final double EXIT_AFTER_SECONDS    = 12.2;

	private static final File MODULE = new File(System.getProperty("onboard.verify"));
	private static final File OUTPUT = new File(MODULE, "build/frames");

	private static GameHarness harness;
	private static boolean audioAvailable;
	private static final Map<String, Enum_Effect_Sound> railsAt = new LinkedHashMap<>();
	private static final Map<String, Enum_Effect_Sound> keyAt = new LinkedHashMap<>();
	private static final Map<String, Enum_Effect_Sound> levelAt = new LinkedHashMap<>();
	private static final List<String> steps = new ArrayList<>();
	private static Enum_Effect_Sound chosenAndPlayed, choiceInTheGame;
	private static GameConfigs savedAfterChoosing;
	private static int levelAfterTheKey;

	private static AudioCapture capture;
	private static Throwable captureError;

	private static Path config;
	private static String previousConfig;

	@BeforeAll
	void runTheGame() throws Exception
	{
		File captured = AudioCapture.configuredFile();
		if (captured != null) Files.deleteIfExists(captured.toPath());

		// Choosing in the lab saves; keep that out of the real config file.
		config = Files.createTempFile("onboard-config", "");
		previousConfig = System.getProperty("onboard.config");
		System.setProperty("onboard.config", config.toString());
		Utils_Config.current = new GameConfigs();

		GVars_Audio.muted = false;
		GVars_Audio.masterVolume = 1f;
		GVars_Audio.musiqueVolume = 0f;
		GVars_Audio.effectVolume = 1f;
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - sound effect verification");
		gl.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);
		gl.useVsync(false);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 100_000);
		harness.captureAfterSeconds = CAPTURE_AFTER_SECONDS;
		harness.exitAfterSeconds = EXIT_AFTER_SECONDS;
		harness.frameHook = frame ->
		{
			double t = harness.gameSeconds;
			if (frame == 5)
			{
				audioAvailable = Gdx.audio != null;
				choiceInTheGame = GVars_Audio.choice(Slot.RAILS);
				railsAt.put("in the game", GVars_AudioManager.currentRails());
			}
			step(t, 1.0, "effects at zero", () -> GVars_AudioManager.setEffectsVolume(0f));
			step(t, 1.5, "effects back up", () -> GVars_AudioManager.setEffectsVolume(1f));
			step(t, 2.0, "muted", () -> GVars_AudioManager.setMuted(true));
			step(t, 2.5, "unmuted", () -> GVars_AudioManager.setMuted(false));
			step(t, 3.0, "stopped", GVars_AudioManager::StopTrain);
			step(t, 3.5, "departure", () -> GVars_AudioManager.PlayEffect(Slot.DEPARTURE));
			step(t, 4.3, "quiet before the key", GVars_AudioManager::StopEffects);
			step(t, 4.8, "first piece", () -> GVars_Game.addKey(1));
			// The chime has rung out by now. Silence it anyway, so a replay would show.
			step(t, 6.5, "first piece again", () -> { GVars_AudioManager.StopEffects(); GVars_Game.addKey(1); });
			step(t, 6.9, "second piece", () -> GVars_Game.addKey(2));
			step(t, 8.8, "last piece", () -> { GVars_AudioManager.StopEffects(); GVars_Game.addKey(3); });
			// The next carriage comes in once the fade to black is over (r44).
			step(t, 10.2, "after the fade", () -> levelAfterTheKey = GVars_Game.currentLevelInt);
			step(t, 11.4, "in the sound lab", () ->
			{
				GVars_Heart.changeVue(new Vue_SoundLab(), true);
				Vue_SoundLab.choose(Enum_Effect_Sound.RAILS_SYNTH);
				Vue_SoundLab.choose(Enum_Effect_Sound.KEY_WOOD);
				((Vue_SoundLab) GVars_Heart.vue).showSlot(Slot.KEY_PIECE);
				savedAfterChoosing = Utils_Config.load();
				GVars_AudioManager.StartRails();
				chosenAndPlayed = GVars_AudioManager.currentRails();
				GVars_AudioManager.StopEffects();
			});
		};

		new Lwjgl3Application(harness, gl);
		steps.forEach(System.out::println);

		if (captured != null && captured.isFile())
		{
			try
			{
				capture = AudioCapture.read(captured);
				System.out.println("captured " + String.format("%.1f", capture.seconds()) + "s: " + profile(capture));
			}
			catch (Throwable t)
			{
				captureError = t;
			}
		}
	}

	private static void step(double now, double at, String name, Runnable action)
	{
		if (now < at || steps.stream().anyMatch(s -> s.endsWith("|" + name))) return;
		action.run();
		railsAt.put(name, GVars_AudioManager.currentRails());
		keyAt.put(name, GVars_AudioManager.lastPlayed(Slot.KEY_PIECE));
		levelAt.put(name, GVars_AudioManager.lastPlayed(Slot.LEVEL_COMPLETE));
		steps.add(String.format("%.2fs rails=%s key=%s level=%s |%s", now,
			railsAt.get(name), keyAt.get(name), levelAt.get(name), name));
	}

	@AfterAll
	void restoreConfig() throws Exception
	{
		if (previousConfig == null) System.clearProperty("onboard.config");
		else System.setProperty("onboard.config", previousConfig);
		Files.deleteIfExists(config);
		GVars_Audio.musiqueVolume = 1f;
		GVars_Audio.loadChoices(new GameConfigs());
	}

	private static final double LOUD = 1e-3;     // -60 dB
	private static final double SILENT = 1e-4;   // -80 dB

	/** One character per 50 ms: '#' sound, '.' silence, '-' in between. */
	private static String profile(AudioCapture capture)
	{
		StringBuilder out = new StringBuilder();
		for (double level : capture.rms(0.05))
			out.append(level >= LOUD ? '#' : level < SILENT ? '.' : '-');
		return out.toString();
	}

	@Test
	@DisplayName("the game view starts the chosen rails bed, and nothing throws")
	void railsStartWithTheGame()
	{
		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		assertEquals(choiceInTheGame, railsAt.get("in the game"), "Vue_Game did not start the chosen rails bed");
	}

	@Test
	@DisplayName("effects volume and mute silence the rails, and both bring them back")
	void volumeAndMuteReachTheRails()
	{
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		assertNull(railsAt.get("effects at zero"), "an effects volume of zero left the rails playing");
		assertNotNull(railsAt.get("effects back up"), "raising the effects volume did not bring the rails back");
		assertNull(railsAt.get("muted"), "muting left the rails playing");
		assertNotNull(railsAt.get("unmuted"), "unmuting did not bring the rails back");
		assertNull(railsAt.get("stopped"), "StopTrain left the rails playing");
		assertNull(railsAt.get("departure"), "a departure should not restart the rails StopTrain forgot");
	}

	@Test
	@DisplayName("a new piece of the key chimes, a piece already held does not, and the last one completes the level")
	void theKeyIsHeard()
	{
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		Enum_Effect_Sound key = Enum_Effect_Sound.defaultFor(Slot.KEY_PIECE);
		assertEquals(key, keyAt.get("first piece"), "gaining a piece of the key made no sound");
		assertNull(keyAt.get("first piece again"), "a piece already held chimed again");
		assertEquals(key, keyAt.get("second piece"), "the second piece made no sound");
		assertNull(keyAt.get("last piece"), "the last piece should play the level's sound, not the piece's");
		assertEquals(Enum_Effect_Sound.defaultFor(Slot.LEVEL_COMPLETE), levelAt.get("last piece"),
			"completing the key did not play the level-complete sound");
		assertEquals(2, levelAfterTheKey, "the whole key should have moved the game to level 2");
	}

	@Test
	@DisplayName("leaving the game silences the train, and the lab's choices are saved and played")
	void theLabChooses()
	{
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		assertNull(railsAt.get("in the sound lab"), "the train kept playing after leaving the game view");
		assertNotNull(savedAfterChoosing, "the lab step never ran");
		assertEquals("RAILS_SYNTH", savedAfterChoosing.railsSound, "Use did not save the rails choice");
		assertEquals("KEY_WOOD", savedAfterChoosing.keyPieceSound, "Use did not save the key piece choice");
		assertEquals(Enum_Effect_Sound.RAILS_SYNTH, chosenAndPlayed, "the rails started were not the ones chosen");
	}

	@Test
	@DisplayName("the effects come out of the audio device, and go quiet where they should")
	void theOutputIsHeard() throws Exception
	{
		File file = AudioCapture.configuredFile();
		Assumptions.assumeTrue(file != null, "the sound went to the speakers, so there is nothing to read back");
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		assertNull(captureError, captureError == null ? null : "could not read the capture: " + captureError);
		assertNotNull(capture, "nothing was written to " + file);

		if (harness.capture != null) Frames.write(harness.capture, new File(OUTPUT, "soundlab-effects.png"));

		// Rails; silence at zero effects; rails; silence muted; rails; silence stopped; departure,
		// which fades in over its first 300 ms; silence; the first piece; silence, through the
		// repeated piece; the second piece; silence; the level.
		String profile = profile(capture);
		String sound = "-?#{4,}[-#]*";
		String quiet = "\\.{4,}";
		assertTrue(Pattern.compile("#{4,}" + quiet + "#{4,}" + quiet + "#{4,}" + quiet
			+ sound + quiet + sound + quiet + sound + "\\.+" + sound).matcher(profile).find(),
			"expected rails, silence, rails, silence, rails, silence, departure, silence, key piece, "
			+ "silence, key piece, silence, level complete.\n"
			+ "Each character is 50 ms, '#' sound, '.' silence: " + profile);
	}
}

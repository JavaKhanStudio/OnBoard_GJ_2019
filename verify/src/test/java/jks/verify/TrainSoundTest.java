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
import jks.sounds.Enum_Train_Sound;
import jks.sounds.GVars_Audio;
import jks.sounds.GVars_AudioManager;
import jks.vars.GVars_Heart;
import jks.vue.models.Vue_SoundLab;

/**
 * The train sounds (r39): a rails bed under the game, a departure, and the effects volume.
 *
 * The music is turned off for the whole run, so whatever reaches the capture is the train. The
 * game view starts the rails by itself; the test then drags the effects volume to zero and back,
 * mutes and unmutes, stops the train, plays a departure, and leaves for the sound lab - checking
 * both what the audio manager says and what actually came out of the device.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrainSoundTest
{
	private static final double CAPTURE_AFTER_SECONDS = 4.4;
	private static final double EXIT_AFTER_SECONDS    = 4.8;

	private static final File MODULE = new File(System.getProperty("onboard.verify"));
	private static final File OUTPUT = new File(MODULE, "build/frames");

	private static GameHarness harness;
	private static boolean audioAvailable;
	private static final Map<String, Enum_Train_Sound> railsAt = new LinkedHashMap<>();
	private static final List<String> steps = new ArrayList<>();
	private static Enum_Train_Sound chosenAndPlayed, choiceInTheGame;
	private static GameConfigs savedAfterChoosing;

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
		gl.setTitle("On Board - train sound verification");
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
				choiceInTheGame = GVars_Audio.railsChoice;
				railsAt.put("in the game", GVars_AudioManager.currentRails());
			}
			step(t, 1.0, "effects at zero", () -> GVars_AudioManager.setEffectsVolume(0f));
			step(t, 1.5, "effects back up", () -> GVars_AudioManager.setEffectsVolume(1f));
			step(t, 2.0, "muted", () -> GVars_AudioManager.setMuted(true));
			step(t, 2.5, "unmuted", () -> GVars_AudioManager.setMuted(false));
			step(t, 3.0, "stopped", GVars_AudioManager::StopTrain);
			step(t, 3.5, "departure", GVars_AudioManager::PlayDeparture);
			step(t, 3.9, "in the sound lab", () ->
			{
				GVars_Heart.changeVue(new Vue_SoundLab(), true);
				Vue_SoundLab.choose(Enum_Train_Sound.RAILS_SYNTH);
				savedAfterChoosing = Utils_Config.load();
				GVars_AudioManager.StartRails();
				chosenAndPlayed = GVars_AudioManager.currentRails();
				GVars_AudioManager.StopTrain();
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
		if (now < at || railsAt.containsKey(name)) return;
		action.run();
		railsAt.put(name, GVars_AudioManager.currentRails());
		steps.add(String.format("%.2fs %s: rails=%s", now, name, GVars_AudioManager.currentRails()));
	}

	@AfterAll
	void restoreConfig() throws Exception
	{
		if (previousConfig == null) System.clearProperty("onboard.config");
		else System.setProperty("onboard.config", previousConfig);
		Files.deleteIfExists(config);
		GVars_Audio.musiqueVolume = 1f;
		GVars_Audio.railsChoice = Enum_Train_Sound.defaultFor(Enum_Train_Sound.Slot.RAILS);
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
	@DisplayName("leaving the game silences the train, and the lab's choice is saved and played")
	void theLabChooses()
	{
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		assertNull(railsAt.get("in the sound lab"), "the train kept playing after leaving the game view");
		assertNotNull(savedAfterChoosing, "the lab step never ran");
		assertEquals("RAILS_SYNTH", savedAfterChoosing.railsSound, "Use did not save the choice");
		assertEquals(Enum_Train_Sound.RAILS_SYNTH, chosenAndPlayed, "the rails started were not the ones chosen");
	}

	@Test
	@DisplayName("the train comes out of the audio device, and goes quiet where it should")
	void theOutputIsHeard() throws Exception
	{
		File file = AudioCapture.configuredFile();
		Assumptions.assumeTrue(file != null, "the sound went to the speakers, so there is nothing to read back");
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		assertNull(captureError, captureError == null ? null : "could not read the capture: " + captureError);
		assertNotNull(capture, "nothing was written to " + file);

		if (harness.capture != null) Frames.write(harness.capture, new File(OUTPUT, "soundlab-train.png"));

		// Rails; silence at zero effects; rails; silence muted; rails; silence stopped; departure,
		// which fades in over its first 300 ms.
		String profile = profile(capture);
		assertTrue(Pattern.compile("#{4,}\\.{4,}#{4,}\\.{4,}#{4,}\\.{4,}-?#{4,}").matcher(profile).find(),
			"expected rails, silence, rails, silence, rails, silence, departure.\n"
			+ "Each character is 50 ms, '#' sound, '.' silence: " + profile);
	}
}

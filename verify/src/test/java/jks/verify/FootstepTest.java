package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;
import jks.input.GVars_Inputs;
import jks.sounds.Enum_Effect_Sound;
import jks.sounds.Enum_Effect_Sound.Slot;
import jks.sounds.GVars_Audio;
import jks.sounds.GVars_AudioManager;
import jks.vars.GVars_Heart;
import jks.vue.models.Vue_SoundLab;
import jks.vue.models.game.GVars_Game;

/**
 * Ross's footsteps (r87): one each time a heel comes down in his walk, none when he stands.
 *
 * The music is off for the whole run. The test stops the rails, walks Ross right through the
 * game's own input path, lets him stand, walks on with the effects volume at zero, then
 * starts the rails and walks him again under them - checking what the audio manager played and,
 * for the walk on his own, the rhythm of what actually came out of the device. Last, the sound
 * lab walks a footstep candidate, the way Simon compares them.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FootstepTest
{
	/** The harness only exits once it has a frame, so it takes one: the lab's footsteps tab. */
	private static final double CAPTURE_AFTER_SECONDS = 10.2;
	private static final double EXIT_AFTER_SECONDS = 10.4;

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");
	/** Two heel strikes in each 8-frame cycle of 'move', at 0.106 s a frame. */
	private static final double STEP_SECONDS = 4 * 0.106;

	private static GameHarness harness;
	private static boolean audioAvailable;
	private static final Map<String, Integer> stepsAt = new LinkedHashMap<>();
	private static final Map<String, Enum_Effect_Sound> railsAt = new LinkedHashMap<>();
	private static final List<String> log = new ArrayList<>();
	private static Enum_Effect_Sound lastStep, labStep;

	private static AudioCapture capture;
	private static Throwable captureError;

	@BeforeAll
	void runTheGame() throws Exception
	{
		File captured = AudioCapture.configuredFile();
		if (captured != null) Files.deleteIfExists(captured.toPath());

		GVars_Audio.muted = false;
		GVars_Audio.masterVolume = 1f;
		GVars_Audio.musiqueVolume = 0f;
		GVars_Audio.effectVolume = 1f;
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - footstep verification");
		gl.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);
		gl.useVsync(false);
		// Ross's speed is per frame, as in the game at its 60 fps cap. Uncapped, he crosses the
		// carriage before a heel comes down.
		gl.setForegroundFPS(60);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 100_000);
		harness.captureAfterSeconds = CAPTURE_AFTER_SECONDS;
		harness.exitAfterSeconds = EXIT_AFTER_SECONDS;
		harness.frameHook = frame ->
		{
			double t = harness.gameSeconds;
			if (frame == 5) audioAvailable = Gdx.audio != null;
			at(t, 0.8, "rails stopped", GVars_AudioManager::StopTrain);
			at(t, 1.0, "walking right", () -> GVars_Inputs.rightPressed = true);
			at(t, 3.5, "standing", () -> GVars_Inputs.rightPressed = false);
			at(t, 4.5, "still standing", () -> {});
			at(t, 4.6, "walking at zero volume", () -> { GVars_AudioManager.setEffectsVolume(0f); GVars_Inputs.rightPressed = true; });
			at(t, 5.6, "rails back", () ->
			{
				GVars_Inputs.rightPressed = false;
				GVars_AudioManager.setEffectsVolume(1f);
				GVars_AudioManager.StartRails();
			});
			at(t, 6.0, "walking under the rails", () -> GVars_Inputs.leftPressed = true);
			at(t, 8.0, "stopped under the rails", () ->
			{
				GVars_Inputs.leftPressed = false;
				lastStep = GVars_AudioManager.lastPlayed(Slot.FOOTSTEPS);
			});
			at(t, 8.2, "walking in the lab", () ->
			{
				GVars_Heart.changeVue(new Vue_SoundLab(), true);
				((Vue_SoundLab) GVars_Heart.vue).showSlot(Slot.FOOTSTEPS);
				((Vue_SoundLab) GVars_Heart.vue).play(Enum_Effect_Sound.STEPS_BOOT);
			});
			at(t, 10.0, "the lab walked", () -> labStep = GVars_AudioManager.lastPlayed(Slot.FOOTSTEPS));
		};

		try
		{
			new Lwjgl3Application(harness, gl);
		}
		finally
		{
			GVars_Inputs.rightPressed = GVars_Inputs.leftPressed = false;
			GVars_Audio.musiqueVolume = 1f;
		}
		log.forEach(System.out::println);

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

	private static void at(double now, double when, String name, Runnable action)
	{
		if (now < when || stepsAt.containsKey(name)) return;
		action.run();
		stepsAt.put(name, GVars_AudioManager.footstepsPlayed());
		railsAt.put(name, GVars_AudioManager.currentRails());
		log.add(String.format("%.2fs steps=%d rails=%s x=%.0f anim=%s |%s", now, stepsAt.get(name), railsAt.get(name),
			GVars_Game.ross.position.x, GVars_Game.ross.currentAnimState, name));
	}

	private static int stepsBetween(String from, String to)
	{
		return stepsAt.get(to) - stepsAt.get(from);
	}

	private static final double WINDOW = 0.02;
	private static final double LOUD = 1e-3;     // -60 dB
	private static final double SILENT = 1e-4;   // -80 dB

	/** One character per 20 ms: '#' sound, '.' silence, '-' in between. */
	private static String profile(AudioCapture capture)
	{
		StringBuilder out = new StringBuilder();
		for (double level : capture.rms(WINDOW))
			out.append(level >= LOUD ? '#' : level < SILENT ? '.' : '-');
		return out.toString();
	}

	@Test
	@DisplayName("walking plays a footstep on every heel strike, and standing plays none")
	void walkingIsHeard()
	{
		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		assertTrue(stepsAt.containsKey("stopped under the rails"), "the walk never finished: " + log);

		// 2.5 s of walking is five or six heel strikes, whatever frame the cycle was on.
		int walked = stepsBetween("rails stopped", "standing");
		assertTrue(walked >= 5 && walked <= 7, "expected about 2.5 / " + STEP_SECONDS + " steps walking right, got " + walked);
		assertEquals(0, stepsBetween("standing", "still standing"), "footsteps went on after Ross stopped");
	}

	@Test
	@DisplayName("footsteps follow the effects volume, and play under the rails")
	void stepsAreEffects()
	{
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		assertEquals(0, stepsBetween("walking at zero volume", "rails back"), "footsteps sounded with the effects volume at zero");
		assertNotNull(railsAt.get("stopped under the rails"), "the rails were not playing during the last walk");
		int underRails = stepsBetween("walking under the rails", "stopped under the rails");
		assertTrue(underRails >= 4, "expected four or more steps in 2 s under the rails, got " + underRails);
		assertEquals(Enum_Effect_Sound.defaultFor(Slot.FOOTSTEPS), lastStep,
			"the step played was not the slot's chosen candidate");
	}

	@Test
	@DisplayName("the sound lab plays a footstep candidate as a walk")
	void theLabWalks()
	{
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		int walked = stepsBetween("walking in the lab", "the lab walked");
		assertTrue(walked >= 4, "the lab's Play should walk a candidate for eight steps; 1.8 s in, it had played " + walked);
		assertEquals(Enum_Effect_Sound.STEPS_BOOT, labStep, "the lab walked a different candidate than the one played");
	}

	@Test
	@DisplayName("the steps come out of the audio device, at the pace of the walk")
	void theOutputIsHeard() throws Exception
	{
		File file = AudioCapture.configuredFile();
		Assumptions.assumeTrue(file != null, "the sound went to the speakers, so there is nothing to read back");
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		assertNull(captureError, captureError == null ? null : "could not read the capture: " + captureError);
		assertNotNull(capture, "nothing was written to " + file);

		if (harness.capture != null) Frames.write(harness.capture, new File(OUTPUT, "footsteps.png"));

		// The walk on his own ends in the longest silence of the run: Ross standing, then walking
		// at zero volume. Before it, back to the rails, each burst is one step.
		String profile = profile(capture);
		int quiet = profile.indexOf(".".repeat((int) (1.2 / WINDOW)), Math.max(0, profile.indexOf('#')));
		assertTrue(quiet > 0, "no silence while Ross stood: " + profile);

		List<Integer> onsets = new ArrayList<>();
		int i = quiet - 1;
		while (i >= 0)
		{
			while (i >= 0 && profile.charAt(i) == '.') i--;
			int end = i;
			while (i >= 0 && profile.charAt(i) != '.') i--;
			if (end < 0 || (end - i) * WINDOW > 0.4) break;   // the rails, or the start of the file
			onsets.add(0, i + 1);
		}
		assertTrue(onsets.size() >= 5, "expected five or more separate steps before the silence, heard "
			+ onsets.size() + ".\nEach character is 20 ms, '#' sound, '.' silence: " + profile);

		for (int n = 1; n < onsets.size(); n++)
		{
			double gap = (onsets.get(n) - onsets.get(n - 1)) * WINDOW;
			assertEquals(STEP_SECONDS, gap, 0.06, "steps " + n + " and " + (n + 1) + " were " + gap
				+ " s apart, the walk cycle puts them " + STEP_SECONDS + " s apart: " + profile);
		}
	}
}

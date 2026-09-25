package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;
import jks.sounds.Enum_Music;
import jks.sounds.GVars_Audio;
import jks.sounds.GVars_AudioManager;

/**
 * Music, which until now was never covered by anything.
 *
 * It needs a real audio device, so it runs alongside the GL tests rather than headless.
 * If the machine has no working output the assertions are skipped rather than failed -
 * a build agent without a sound card should not fail the build - but the playback state
 * machine below is checked regardless of whether anything is audible.
 *
 * The device is OpenAL Soft's WAV writer, not the speakers (gradle/offscreen.gradle), so
 * the last test reads back what actually came out: the music, a silence where it was
 * muted, then the music again.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MusicTest
{
	private static final double CAPTURE_AFTER_SECONDS = 1.5;
	private static final double EXIT_AFTER_SECONDS    = 2.0;

	private static GameHarness harness;
	private static final List<String> observations = new ArrayList<>();

	private static boolean audioAvailable;
	private static boolean playingDuringOpening;
	private static Enum_Music trackDuringOpening;
	private static boolean survivedViewChange = true;
	private static Object firstMusicObject;
	private static Object musicObjectAtEnd;
	private static Boolean playingAfterMute;
	private static Boolean playingAfterUnmute;

	/** What came out of the audio device; null when it went to the speakers instead. */
	private static AudioCapture capture;
	private static Throwable captureError;

	@BeforeAll
	void runTheGame() throws Exception
	{
		// Whatever the last test class played is in there too. Only this run's sound counts.
		File captured = AudioCapture.configuredFile();
		if (captured != null) Files.deleteIfExists(captured.toPath());

		GVars_Audio.muted = false;
		GVars_Audio.masterVolume = 1f;
		GVars_Audio.musiqueVolume = 1f;
		Main_Application.startPoint = Main_Application.StartPoint.LOGO;

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1280, 720);
		config.setTitle("On Board - music verification");
		config.useVsync(false);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.captureAfterSeconds = CAPTURE_AFTER_SECONDS;
		harness.exitAfterSeconds = EXIT_AFTER_SECONDS;
		harness.frameHook = frame ->
		{
			if (frame == 5)
			{
				audioAvailable = Gdx.audio != null;
				playingDuringOpening = GVars_AudioManager.isMusicPlaying();
				trackDuringOpening = GVars_AudioManager.currentMusic();
				firstMusicObject = currentMusicObject();
				observations.add("frame 5: audio=" + audioAvailable
					+ " playing=" + playingDuringOpening + " track=" + trackDuringOpening);
			}
			double t = harness.gameSeconds;

			// Up to 0.8s: ask again for what is already playing. It must not be rebuilt.
			if (frame > 5 && t < 0.8)
			{
				GVars_AudioManager.PlayMusic(Enum_Music.GAME_INTRO);
				Object now = currentMusicObject();
				if (firstMusicObject != null && now != firstMusicObject) survivedViewChange = false;
				musicObjectAtEnd = now;
			}

			// Then mute, and check it actually went quiet.
			if (t >= 0.8 && playingAfterMute == null)
			{
				GVars_AudioManager.setMuted(true);
				playingAfterMute = GVars_AudioManager.isMusicPlaying();
				observations.add("after mute: playing=" + playingAfterMute);
			}

			// Then unmute, and check it came back without anyone changing screen.
			if (t >= 1.2 && playingAfterMute != null && playingAfterUnmute == null)
			{
				GVars_AudioManager.setMuted(false);
				playingAfterUnmute = GVars_AudioManager.isMusicPlaying();
				observations.add("after unmute: playing=" + playingAfterUnmute
					+ " track=" + GVars_AudioManager.currentMusic());
			}
		};

		new Lwjgl3Application(harness, config);
		observations.forEach(System.out::println);

		// The application has closed its audio device by now, so the file is complete.
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

	// Loudness bands for the capture. intro.mp3 never drops below about -56 dB in its first
	// seconds; a muted device writes digital silence.
	private static final double WINDOW_SECONDS = 0.05;
	private static final double LOUD = 1e-3;     // -60 dB
	private static final double SILENT = 1e-4;   // -80 dB

	/** One character per 50 ms: '#' music, '.' silence, '-' in between. */
	private static String profile(AudioCapture capture)
	{
		StringBuilder out = new StringBuilder();
		for (double level : capture.rms(WINDOW_SECONDS))
			out.append(level >= LOUD ? '#' : level < SILENT ? '.' : '-');
		return out.toString();
	}

	/** The Music instance itself, so the test can tell "still playing" from "restarted". */
	private static Object currentMusicObject()
	{
		try
		{
			java.lang.reflect.Field f = GVars_AudioManager.class.getDeclaredField("currentlyRunningMusic");
			f.setAccessible(true);
			return f.get(null);
		}
		catch (Exception e) { return null; }
	}

	@Test
	@DisplayName("the opening starts the music")
	void musicStartsWithTheGame()
	{
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		assertTrue(playingDuringOpening,
			"nothing was playing during the opening. Every view asks for music in init(), so "
			+ "either the request never happened or it was muted.");
		assertNotNull(trackDuringOpening, "music is playing but no track is recorded as current");
	}

	@Test
	@DisplayName("asking again for the track already playing does not restart it")
	void repeatedRequestsDoNotRestart()
	{
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		Assumptions.assumeTrue(firstMusicObject != null, "music never started");
		assertTrue(survivedViewChange,
			"the music was torn down and rebuilt on a repeat request. That is what made the "
			+ "same track restart at every screen in the opening.");
		assertSame(firstMusicObject, musicObjectAtEnd, "the Music instance was replaced");
	}

	@Test
	@DisplayName("muting stops the music and unmuting brings it back")
	void mutingIsReversible()
	{
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		Assumptions.assumeTrue(firstMusicObject != null, "music never started");

		assertNotNull(playingAfterMute, "the mute step never ran");
		assertNotNull(playingAfterUnmute, "the unmute step never ran");

		assertFalse(playingAfterMute, "muting did not stop the music");
		assertTrue(playingAfterUnmute,
			"unmuting did not bring the music back. The requested track is remembered "
			+ "separately from what is playing precisely so this can resume without "
			+ "waiting for the next screen change.");
	}

	@Test
	@DisplayName("the volume the config asks for is the volume the music gets")
	void volumeReachesTheMusic()
	{
		GVars_Audio.masterVolume = 0.5f;
		GVars_Audio.musiqueVolume = 0.5f;
		assertEquals(0.25f, GVars_AudioManager.musicVolume(), 1e-6,
			"master and music volume should multiply");

		GVars_Audio.masterVolume = 2f;
		assertTrue(GVars_AudioManager.musicVolume() <= 1f, "volume must be clamped for the audio API");

		GVars_Audio.masterVolume = 1f;
		GVars_Audio.musiqueVolume = 1f;
	}

	@Test
	@DisplayName("a silent configuration reports no track")
	void silenceReportsNoTrack()
	{
		GVars_Audio.muted = true;
		assertNull(GVars_AudioManager.currentMusic(),
			"muted, so nothing should be reported as playing");
		GVars_Audio.muted = false;
	}

	@Test
	@DisplayName("the music comes out of the audio device, and muting silences it there")
	void theOutputIsHeard()
	{
		File file = AudioCapture.configuredFile();
		Assumptions.assumeTrue(file != null,
			"the sound went to the speakers (ONBOARD_NO_OFFSCREEN=1), so there is nothing to read back");
		Assumptions.assumeTrue(audioAvailable, "no audio device on this machine");
		assertNull(captureError, captureError == null ? null : "could not read the capture: " + captureError);
		assertNotNull(capture, "nothing was written to " + file + ": OpenAL Soft did not open its WAV device");

		String profile = profile(capture);
		assertTrue(profile.contains("#"), "the audio device never received any music: " + profile);
		// Music, at least 200 ms of silence while muted (it is muted for 400), then music.
		assertTrue(Pattern.compile("#.*\\.{4,}.*#").matcher(profile).find(),
			"muting did not silence the output, or unmuting did not bring it back.\n"
			+ "Each character is 50 ms, '#' music, '.' silence: " + profile);
	}
}

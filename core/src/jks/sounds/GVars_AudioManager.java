package jks.sounds;

import jks.tools.Utils_Debug;

import static jks.sounds.GVars_Audio.masterVolume;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;

import jks.debug.GVars_Debug;

public class GVars_AudioManager 
{	
	private static ArrayList<Sound> jumpingSounds;

	private static Sound runningSound;


	private static FileHandle musicFile = Gdx.files.internal("musics/intro.mp3");

	private static Music currentlyRunningMusic;
	private static Music currentlyRunningMusicSecondary;
	private static Music currentlyRunningAmbiance;

	public static void init() 
	{
		
	}

	private static void PreLoadAllSounds()
	{
//		jumpingSounds = new ArrayList<>();
//		jumpingSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/Player_Jump_01.wav")));
//		jumpingSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/Player_Jump_02.wav")));
//		jumpingSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/Player_Jump_03.wav")));
//
//		runningSound = Gdx.audio.newSound(Gdx.files.internal("sounds/Player_Run_Loop.wav"));

	}

	public static void PlayGameSound(Enum_Sounds_Game whichOne) 
	{
		if(GVars_Audio.muted)
			return ;
		
		if(GVars_Debug.soundDebug)
			Utils_Debug.log("Trying to play sound : " + whichOne);
		
		switch (whichOne) 
		{
			case Jumping:
				int randomizedIndex = MathUtils.random.nextInt(jumpingSounds.size());
				runningSound.stop();
				jumpingSounds.get(randomizedIndex).play(masterVolume);
				break;
	
			case Running:
				runningSound.stop();
				runningSound.loop(masterVolume, 1.1f, 0) ;
				break;
	
			case Idlling:
				runningSound.stop();
				break;
						
			default:
				Utils_Debug.log("Unknown Sound requested in PlaySound : " + whichOne);
				break;
		}
	}
	
	public static void PlayInterfaceSound(Enum_Sounds_Game whichOne) 
	{
		if(GVars_Audio.muted)
			return ;
		
		if(GVars_Debug.soundDebug)
			Utils_Debug.log("Trying to play sound : " + whichOne);
		
		switch (whichOne) 
		{
			case Jumping:
				int randomizedIndex = MathUtils.random.nextInt(jumpingSounds.size());
				runningSound.stop();
				jumpingSounds.get(randomizedIndex).play(masterVolume);
				break;
	
			case Running:
				runningSound.stop();
				runningSound.loop(masterVolume, 1.1f, 0) ;
				break;
	
			case Idlling:
				runningSound.stop();
				break;
						
			default:
				Utils_Debug.log("Unknown Sound requested in PlaySound : " + whichOne);
				break;
		}
	}

	/**
	 * What the game currently wants playing, as opposed to what is actually coming out of
	 * the speakers. They differ while the game is muted or the volume is at zero, and
	 * keeping the request means unmuting can pick the track back up rather than waiting
	 * for the next screen.
	 */
	private static Enum_Music requestedTrack ;
	private static FileHandle currentlyRunningFile ;

	/**
	 * Which recording each track maps to. Both entries are the same file today - it is the
	 * only music that was ever made for this - but this is the one place to change when
	 * there is more of it.
	 */
	private static FileHandle fileFor(Enum_Music whichOne)
	{
		switch(whichOne)
		{
			case STARTING_SCREEN :
			case GAME_INTRO :
			default :
				return musicFile ;
		}
	}

	public static void PlayMusic(Enum_Music whichOne) 
	{
		if(whichOne == null)
			return ;
		
		// Already playing this recording: leave it be. Every view asks for music in its
		// init(), and the opening changes view three times before the game starts, so
		// restarting here meant nobody ever heard past the first few seconds of a track
		// that runs for two and a half minutes.
		//
		// The comparison is on the file, not the enum. STARTING_SCREEN and GAME_INTRO are
		// two names for the same recording, so treating them as different tracks would
		// restart it when the menu appears, for no audible reason. If they ever point at
		// different files this starts switching properly on its own.
		if(isMusicPlaying() && sameFile(fileFor(whichOne), currentlyRunningFile))
		{
			requestedTrack = whichOne ;
			return ;
		}
		
		requestedTrack = whichOne ;
		startRequestedTrack() ;
	}

	private static void startRequestedTrack()
	{
		StopAndDisposeMusic() ;
		
		if(requestedTrack == null || GVars_Audio.muted || musicVolume() <= 0f)
			return ;
		
		if(GVars_Debug.soundDebug)
			Utils_Debug.log("Playing music : " + requestedTrack);
		
		currentlyRunningFile = fileFor(requestedTrack) ;
		currentlyRunningMusic = Gdx.audio.newMusic(currentlyRunningFile) ;
		// Looping, because the game outlasts the recording. It used to play once and leave
		// the rest of the session in silence.
		currentlyRunningMusic.setLooping(true) ;
		currentlyRunningMusic.setVolume(musicVolume()) ;
		currentlyRunningMusic.play() ;
	}

	/** Music was the one thing the volume setting never reached - it only ever fed sound effects. */
	private static boolean sameFile(FileHandle a, FileHandle b)
	{
		return a != null && b != null && a.path().equals(b.path()) ;
	}

	public static float musicVolume()
	{
		return MathUtils.clamp(GVars_Audio.masterVolume * GVars_Audio.musiqueVolume, 0f, 1f) ;
	}

	public static void setMasterVolume(float volume)
	{
		GVars_Audio.masterVolume = MathUtils.clamp(volume, 0f, 1f) ;
		applyVolumeChange() ;
	}

	public static void setMuted(boolean muted)
	{
		GVars_Audio.muted = muted ;
		applyVolumeChange() ;
	}

	/** Takes effect on what is playing right now, so a volume slider does something while you drag it. */
	private static void applyVolumeChange()
	{
		if(GVars_Audio.muted || musicVolume() <= 0f)
			StopAndDisposeMusic() ;
		else if(currentlyRunningMusic == null)
			startRequestedTrack() ;
		else
			currentlyRunningMusic.setVolume(musicVolume()) ;
	}

	public static boolean isMusicPlaying()
	{
		return currentlyRunningMusic != null && currentlyRunningMusic.isPlaying() ;
	}

	/** The track actually coming out of the speakers, or null when nothing is. */
	public static Enum_Music currentMusic()
	{
		return currentlyRunningMusic == null ? null : requestedTrack ;
	}

	public static void StopAndDisposeMusic() 
	{
		if(currentlyRunningMusic != null)
		{
			currentlyRunningMusic.stop() ;
			currentlyRunningMusic.dispose() ;
			currentlyRunningMusic = null ;
			currentlyRunningFile = null ;
		}
		if(currentlyRunningMusicSecondary != null)
		{
			currentlyRunningMusicSecondary.stop() ;
			currentlyRunningMusicSecondary.dispose() ;
			currentlyRunningMusicSecondary = null ;
		}
		if(currentlyRunningAmbiance != null)
		{
			currentlyRunningAmbiance.stop() ;
			currentlyRunningAmbiance.dispose() ;
			currentlyRunningAmbiance = null ;
		}
	}
}

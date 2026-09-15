package jks.sounds;

import jks.tools.Utils_Debug;

import static jks.sounds.GVars_Audio.masterVolume;

import java.util.ArrayList;
import java.util.EnumMap;

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
	public static FileHandle fileFor(Enum_Music whichOne)
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
		
		applyEffectsVolumeChange() ;
	}

	// --- Train sounds (r39) ----------------------------------------------------------------
	//
	// A rails bed loops under every level and a departure plays once as a new game opens
	// level 1. They are Sounds, decoded whole into memory, rather than Music: a Sound loops
	// sample-exactly, and the beds were cut to loop without a seam.
	//
	// Same rules as the music. The rails are a request that outlives muting, so unmuting
	// brings them back, and volume changes reach what is already playing.

	/** The recordings are levelled alike; these set how far under the music each one sits. */
	static final float RAILS_GAIN = 0.35f ;
	static final float DEPARTURE_GAIN = 0.8f ;

	private static final EnumMap<Enum_Train_Sound, Sound> trainSounds = new EnumMap<>(Enum_Train_Sound.class) ;

	private static Enum_Train_Sound requestedRails ;
	private static Enum_Train_Sound playingRails ;
	private static long railsId = -1 ;
	private static Enum_Train_Sound playingDeparture ;
	private static long departureId = -1 ;

	public static float effectsVolume()
	{
		return MathUtils.clamp(GVars_Audio.masterVolume * GVars_Audio.effectVolume, 0f, 1f) ;
	}

	public static void setEffectsVolume(float volume)
	{
		GVars_Audio.effectVolume = MathUtils.clamp(volume, 0f, 1f) ;
		applyEffectsVolumeChange() ;
	}

	private static boolean effectsSilent()
	{
		return GVars_Audio.muted || effectsVolume() <= 0f ;
	}

	/** The chosen rails bed, looping until StopTrain. Asking again while it plays changes nothing. */
	public static void StartRails()
	{
		PlayRails(GVars_Audio.railsChoice) ;
	}

	/** A particular rails bed - the sound lab plays candidates through this. */
	public static void PlayRails(Enum_Train_Sound which)
	{
		if(which == null || which.slot != Enum_Train_Sound.Slot.RAILS)
			return ;
		
		requestedRails = which ;
		if(railsId != -1 && playingRails == which)
			return ;
		
		startRequestedRails() ;
	}

	private static void startRequestedRails()
	{
		stopRailsPlayback() ;
		if(requestedRails == null || effectsSilent())
			return ;
		
		Sound sound = trainSound(requestedRails) ;
		if(sound == null)
			return ;
		
		railsId = sound.loop(effectsVolume() * RAILS_GAIN) ;
		playingRails = railsId == -1 ? null : requestedRails ;
	}

	/** The chosen departure, once. */
	public static void PlayDeparture()
	{
		PlayDeparture(GVars_Audio.departureChoice) ;
	}

	public static void PlayDeparture(Enum_Train_Sound which)
	{
		if(which == null || which.slot != Enum_Train_Sound.Slot.DEPARTURE)
			return ;
		
		stopDeparturePlayback() ;
		if(effectsSilent())
			return ;
		
		Sound sound = trainSound(which) ;
		if(sound == null)
			return ;
		
		departureId = sound.play(effectsVolume() * DEPARTURE_GAIN) ;
		playingDeparture = departureId == -1 ? null : which ;
	}

	/** Stops the rails and any departure, and forgets the request: leaving the train, not muting it. */
	public static void StopTrain()
	{
		requestedRails = null ;
		stopRailsPlayback() ;
		stopDeparturePlayback() ;
	}

	/** The rails bed coming out of the speakers, or null. */
	public static Enum_Train_Sound currentRails()
	{
		return railsId == -1 ? null : playingRails ;
	}

	private static void applyEffectsVolumeChange()
	{
		if(effectsSilent())
		{
			stopRailsPlayback() ;
			stopDeparturePlayback() ;
			return ;
		}
		
		if(railsId == -1)
			startRequestedRails() ;
		else
			trainSounds.get(playingRails).setVolume(railsId, effectsVolume() * RAILS_GAIN) ;
		
		if(departureId != -1)
			trainSounds.get(playingDeparture).setVolume(departureId, effectsVolume() * DEPARTURE_GAIN) ;
	}

	private static void stopRailsPlayback()
	{
		if(railsId != -1)
			trainSounds.get(playingRails).stop(railsId) ;
		railsId = -1 ;
		playingRails = null ;
	}

	private static void stopDeparturePlayback()
	{
		if(departureId != -1)
			trainSounds.get(playingDeparture).stop(departureId) ;
		departureId = -1 ;
		playingDeparture = null ;
	}

	/** Loaded on first use and kept: switching candidates in the lab should not decode again. */
	private static Sound trainSound(Enum_Train_Sound which)
	{
		Sound sound = trainSounds.get(which) ;
		if(sound == null)
		{
			if(Gdx.audio == null)
				return null ;
			sound = Gdx.audio.newSound(Gdx.files.internal(which.path)) ;
			trainSounds.put(which, sound) ;
		}
		return sound ;
	}

	/** Releases every decoded train sound and its OpenAL buffer. */
	public static void DisposeTrainSounds()
	{
		StopTrain() ;
		for(Sound sound : trainSounds.values())
			sound.dispose() ;
		trainSounds.clear() ;
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

	/** Seconds into the track that is playing, or -1 when nothing is. */
	public static float musicPosition()
	{
		return currentlyRunningMusic == null ? -1f : currentlyRunningMusic.getPosition() ;
	}

	/**
	 * Stop and forget the request. StopAndDisposeMusic alone keeps it, so the next volume
	 * change would start the track again - right for muting, wrong for a Stop button.
	 */
	public static void StopMusic()
	{
		requestedTrack = null ;
		StopAndDisposeMusic() ;
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

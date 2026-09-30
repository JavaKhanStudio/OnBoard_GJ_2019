package jks.sounds;

import jks.tools.Utils_Debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;

import jks.amain.GVars_Platform;
import jks.debug.GVars_Debug;

/**
 * The music half of GVars_AudioManager (r197): the one song, its carriage variants and the
 * crossfade between recordings. Callers go through GVars_AudioManager.
 */
final class GVars_Music
{
	private GVars_Music() {}

	private static FileHandle musicFile = Gdx.files.internal("musics/intro.mp3");

	private static Music currentlyRunningMusic;
	private static Music currentlyRunningMusicSecondary;

	/**
	 * What the game currently wants playing, as opposed to what is actually coming out of
	 * the speakers. They differ while the game is muted or the volume is at zero, and
	 * keeping the request means unmuting can pick the track back up rather than waiting
	 * for the next screen.
	 */
	private static Enum_Music requestedTrack ;
	private static FileHandle currentlyRunningFile ;

	/**
	 * Which recording each track maps to. The menu, the intro and - unless a lab has chosen
	 * otherwise - every carriage are the same file, the only music made for the game. The
	 * carriages follow GVars_Audio.carriageVariant (r94).
	 */
	public static FileHandle fileFor(Enum_Music whichOne)
	{
		return whichOne.carriage == 0 ? musicFile : carriageFile(whichOne.carriage, GVars_Audio.carriageVariant) ;
	}

	/** A carriage's recording in one variant. An ideal track nobody has dropped in yet falls back to the main song. */
	public static FileHandle carriageFile(int carriage, Enum_Music.CarriageVariant variant)
	{
		switch(variant)
		{
			case MODULATED :
				return GVars_Platform.current.soundFile("musics/wagons/wa" + carriage + "_modulated.ogg") ;
			case IDEAL :
				FileHandle ideal = idealFile(carriage) ;
				return ideal != null ? ideal : musicFile ;
			case MAIN :
			default :
				return musicFile ;
		}
	}


	/** The formats libGDX's Music reads, in the order they are looked for. */
	private static final String[] IDEAL_FORMATS = {"ogg", "mp3", "wav"} ;

	/**
	 * lab-assets/musics/wagons/wa<n>_ideal.(ogg|mp3|wav), or null while it has not been made -
	 * always, on a fresh clone and on a platform with no lab-assets/ (r135). The ideal tracks are
	 * lab-only (r99); the modulated ones ship.
	 */
	public static FileHandle idealFile(int carriage)
	{
		if(Gdx.files == null)
			return null ;
		for(String format : IDEAL_FORMATS)
		{
			FileHandle file = GVars_Platform.current.labAsset("musics/wagons/wa" + carriage + "_ideal." + format) ;
			if(file != null && file.exists())
				return file ;
		}
		return null ;
	}

	/**
	 * How fast a track's recording runs against intro.mp3, or 0 when it is another piece of music.
	 * Two recordings of the same song hand the position over (r94): moving to the next carriage,
	 * or flipping a lab from the main song to the modulated one, goes on from the same bar.
	 */
	static float songTempo(Enum_Music track)
	{
		FileHandle file = fileFor(track) ;
		if(sameFile(file, musicFile))
			return 1f ;
		return GVars_Audio.carriageVariant.tempo(track.carriage) ;
	}

	private static float currentlyRunningTempo ;

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
		// Where the song had got to, in intro.mp3's seconds, if what plays next is the same song.
		float songPosition = -1f ;
		float nextTempo = requestedTrack == null ? 0f : songTempo(requestedTrack) ;
		if(currentlyRunningMusic != null && currentlyRunningTempo > 0f && nextTempo > 0f)
			songPosition = currentlyRunningMusic.getPosition() * currentlyRunningTempo ;
		
		if(requestedTrack == null || GVars_Audio.muted || musicVolume() <= 0f)
		{
			StopAndDisposeMusic() ;
			return ;
		}
		
		if(GVars_Debug.soundDebug)
			Utils_Debug.log("Playing music : " + requestedTrack);
		
		// What was playing fades out under the new track rather than being cut (r97).
		// A crossfade still running when the next one starts drops its outgoing track.
		boolean crossfade = currentlyRunningMusic != null ;
		float outgoingGain = crossfade ? incomingGain() : 0f ;
		disposeOutgoing() ;
		if(crossfade)
		{
			currentlyRunningMusicSecondary = currentlyRunningMusic ;
			outgoingStartGain = outgoingGain ;
		}
		crossfadeProgress = crossfade ? 0f : 1f ;
		
		currentlyRunningFile = fileFor(requestedTrack) ;
		currentlyRunningMusic = Gdx.audio.newMusic(currentlyRunningFile) ;
		// Looping, because the game outlasts the recording. It used to play once and leave
		// the rest of the session in silence.
		currentlyRunningMusic.setLooping(true) ;
		applyCrossfadeVolumes() ;
		currentlyRunningMusic.play() ;
		currentlyRunningTempo = nextTempo ;
		if(songPosition > 0f)
			currentlyRunningMusic.setPosition(songPosition / nextTempo) ;
	}

	// --- Crossfade (r97) ----------------------------------------------------------------------
	//
	// A carriage change used to stop one Music and start the next: a hard cut in the middle of
	// the fade to black. Now the outgoing track is kept in currentlyRunningMusicSecondary and
	// the two cross over CROSSFADE_SECONDS, equal power, so the level does not dip halfway.
	// The change comes at black (GVars_Fade), so the crossing plays under the fade-in.

	public static final float CROSSFADE_SECONDS = 1f ;

	/** 0 when a crossfade begins, 1 once the new track is alone. */
	private static float crossfadeProgress = 1f ;
	/** How loud the outgoing track was, as a share of musicVolume(), when its fade began. */
	private static float outgoingStartGain = 1f ;

	/** Steps the crossfade. Main_Application calls it every frame, paused or not. */
	public static void update(float delta)
	{
		if(currentlyRunningMusicSecondary == null)
			return ;
		crossfadeProgress = Math.min(1f, crossfadeProgress + delta / CROSSFADE_SECONDS) ;
		if(crossfadeProgress >= 1f)
			disposeOutgoing() ;
		applyCrossfadeVolumes() ;
	}

	/** True while an outgoing track is still fading out under the new one. */
	public static boolean isCrossfading()
	{
		return currentlyRunningMusicSecondary != null ;
	}

	private static float incomingGain()
	{
		return MathUtils.sin(crossfadeProgress * MathUtils.HALF_PI) ;
	}

	private static void applyCrossfadeVolumes()
	{
		if(currentlyRunningMusic != null)
			currentlyRunningMusic.setVolume(musicVolume() * incomingGain()) ;
		if(currentlyRunningMusicSecondary != null)
			currentlyRunningMusicSecondary.setVolume(musicVolume() * outgoingStartGain * MathUtils.cos(crossfadeProgress * MathUtils.HALF_PI)) ;
	}

	private static void disposeOutgoing()
	{
		if(currentlyRunningMusicSecondary != null)
		{
			currentlyRunningMusicSecondary.stop() ;
			currentlyRunningMusicSecondary.dispose() ;
			currentlyRunningMusicSecondary = null ;
		}
	}

	/** One carriage's music in one variant - the sound lab's per-carriage buttons. */
	public static void PlayCarriage(int carriage, Enum_Music.CarriageVariant variant)
	{
		GVars_Audio.carriageVariant = variant ;
		PlayMusic(Enum_Music.forCarriage(carriage)) ;
	}

	/**
	 * Switches what plays under the carriages (r94). If a carriage's track is playing it changes
	 * at once and goes on from the same bar when both are the same song.
	 */
	public static void setCarriageVariant(Enum_Music.CarriageVariant variant)
	{
		if(variant == null || variant == GVars_Audio.carriageVariant)
			return ;
		GVars_Audio.carriageVariant = variant ;
		if(requestedTrack != null && requestedTrack.carriage > 0 && !sameFile(fileFor(requestedTrack), currentlyRunningFile))
			startRequestedTrack() ;
	}

	private static boolean sameFile(FileHandle a, FileHandle b)
	{
		return a != null && b != null && a.path().equals(b.path()) ;
	}

	/** Music was the one thing the volume setting never reached - it only ever fed sound effects. */
	public static float musicVolume()
	{
		return MathUtils.clamp(GVars_Audio.masterVolume * GVars_Audio.musiqueVolume, 0f, 1f) ;
	}

	/** Takes effect on what is playing right now, so a volume slider does something while you drag it. */
	static void applyVolumeChange()
	{
		if(GVars_Audio.muted || musicVolume() <= 0f)
			StopAndDisposeMusic() ;
		else if(currentlyRunningMusic == null)
			startRequestedTrack() ;
		else
			applyCrossfadeVolumes() ;
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
		disposeOutgoing() ;
		crossfadeProgress = 1f ;
	}
}

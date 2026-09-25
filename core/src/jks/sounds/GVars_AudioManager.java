package jks.sounds;

import jks.tools.Utils_Debug;

import java.util.EnumMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;

import jks.debug.GVars_Debug;

public class GVars_AudioManager 
{	
	private static FileHandle musicFile = Gdx.files.internal("musics/intro.mp3");

	private static Music currentlyRunningMusic;
	private static Music currentlyRunningMusicSecondary;
	private static Music currentlyRunningAmbiance;

	public static void init() 
	{
		
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
				return Gdx.files.internal("musics/wagons/wa" + carriage + "_modulated.ogg") ;
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

	/** musics/wagons/wa<n>_ideal.(ogg|mp3|wav), or null while it has not been made. */
	public static FileHandle idealFile(int carriage)
	{
		for(String format : IDEAL_FORMATS)
		{
			FileHandle file = Gdx.files.internal("musics/wagons/wa" + carriage + "_ideal." + format) ;
			if(file.exists())
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
		currentlyRunningTempo = nextTempo ;
		if(songPosition > 0f)
			currentlyRunningMusic.setPosition(songPosition / nextTempo) ;
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

	// --- Sound effects (r39, r45, r87) -------------------------------------------------------
	//
	// A rails bed loops under every level. The departure, a key piece, a level completed and each
	// of Ross's steps play once. All are Sounds, decoded whole into memory, rather than Music: a
	// Sound loops sample-exactly, and the beds were cut to loop without a seam. Which candidate
	// plays for each moment is GVars_Audio.choice(slot), picked in the sound lab.
	//
	// Same rules as the music. The rails are a request that outlives muting, so unmuting
	// brings them back, and volume changes reach what is already playing.

	/** The candidates are levelled alike; this sets how far under the music each moment sits. */
	static float gain(Enum_Effect_Sound.Slot slot)
	{
		switch(slot)
		{
			case RAILS :          return 0.35f ;
			case DEPARTURE :      return 0.8f ;
			case KEY_PIECE :      return 0.7f ;
			case LEVEL_COMPLETE : return 0.8f ;
			case FOOTSTEPS :      return 0.3f ;
			default :             return 1f ;
		}
	}

	private static final EnumMap<Enum_Effect_Sound, Sound> effectSounds = new EnumMap<>(Enum_Effect_Sound.class) ;

	private static Enum_Effect_Sound requestedRails ;
	private static Enum_Effect_Sound playingRails ;
	private static long railsId = -1 ;

	/** One playing instance per one-shot slot: a new one replaces it rather than piling up. */
	private static final EnumMap<Enum_Effect_Sound.Slot, Enum_Effect_Sound> playingOnce = new EnumMap<>(Enum_Effect_Sound.Slot.class) ;
	private static final EnumMap<Enum_Effect_Sound.Slot, Long> playingOnceId = new EnumMap<>(Enum_Effect_Sound.Slot.class) ;

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
		PlayRails(GVars_Audio.choice(Enum_Effect_Sound.Slot.RAILS)) ;
	}

	/** A particular rails bed - the sound lab plays candidates through this. */
	public static void PlayRails(Enum_Effect_Sound which)
	{
		if(which == null || which.slot != Enum_Effect_Sound.Slot.RAILS)
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
		
		Sound sound = effectSound(requestedRails) ;
		if(sound == null)
			return ;
		
		railsId = sound.loop(effectsVolume() * gain(Enum_Effect_Sound.Slot.RAILS)) ;
		playingRails = railsId == -1 ? null : requestedRails ;
	}

	/** The chosen candidate for a one-shot moment: DEPARTURE, KEY_PIECE, LEVEL_COMPLETE or FOOTSTEPS. */
	public static void PlayEffect(Enum_Effect_Sound.Slot slot)
	{
		PlayEffect(GVars_Audio.choice(slot)) ;
	}

	/** A particular one-shot candidate. Silently nothing while muted: a missed chime is not replayed. */
	public static void PlayEffect(Enum_Effect_Sound which)
	{
		PlayEffect(which, 1f) ;
	}

	private static int footsteps ;

	/** How many footsteps have sounded since the game started. Muted or at zero volume, none do. */
	public static int footstepsPlayed()
	{
		return footsteps ;
	}

	/**
	 * One of Ross's steps (r87), the chosen candidate. Each is pitched a little differently, so a
	 * walk across the carriage is not one sample repeated. It replaces the step before it, which
	 * has always rung out by then: they come every 0.42 s and last a quarter of one.
	 */
	public static void PlayFootstep()
	{
		PlayFootstep(GVars_Audio.choice(Enum_Effect_Sound.Slot.FOOTSTEPS)) ;
	}

	/** A particular footstep candidate - the sound lab walks with each through this. */
	public static void PlayFootstep(Enum_Effect_Sound which)
	{
		if(which != null && which.slot == Enum_Effect_Sound.Slot.FOOTSTEPS)
			PlayEffect(which, MathUtils.random(0.93f, 1.07f)) ;
	}

	private static void PlayEffect(Enum_Effect_Sound which, float pitch)
	{
		if(which == null || which.slot == Enum_Effect_Sound.Slot.RAILS)
			return ;
		
		stopOnce(which.slot) ;
		if(effectsSilent())
			return ;
		
		Sound sound = effectSound(which) ;
		if(sound == null)
			return ;
		
		long id = sound.play(effectsVolume() * gain(which.slot), pitch, 0f) ;
		if(id != -1)
		{
			if(which.slot == Enum_Effect_Sound.Slot.FOOTSTEPS)
				footsteps++ ;
			playingOnce.put(which.slot, which) ;
			playingOnceId.put(which.slot, id) ;
		}
	}

	/**
	 * Stops the rails and the departure, and forgets the rails request: leaving the train, not
	 * muting it. A key or level chime is left to finish - the last one sounds as the outro opens.
	 */
	public static void StopTrain()
	{
		requestedRails = null ;
		stopRailsPlayback() ;
		stopOnce(Enum_Effect_Sound.Slot.DEPARTURE) ;
	}

	/** Stops everything the effects are playing, the lab's Stop button. */
	public static void StopEffects()
	{
		StopTrain() ;
		for(Enum_Effect_Sound.Slot slot : Enum_Effect_Sound.Slot.values())
			stopOnce(slot) ;
	}

	/** The rails bed coming out of the speakers, or null. */
	public static Enum_Effect_Sound currentRails()
	{
		return railsId == -1 ? null : playingRails ;
	}

	/** The one-shot last started for this slot, or null. It may have finished since: Sound cannot say. */
	public static Enum_Effect_Sound lastPlayed(Enum_Effect_Sound.Slot slot)
	{
		return playingOnce.get(slot) ;
	}

	private static void applyEffectsVolumeChange()
	{
		if(effectsSilent())
		{
			stopRailsPlayback() ;
			for(Enum_Effect_Sound.Slot slot : Enum_Effect_Sound.Slot.values())
				stopOnce(slot) ;
			return ;
		}
		
		if(railsId == -1)
			startRequestedRails() ;
		else
			effectSounds.get(playingRails).setVolume(railsId, effectsVolume() * gain(Enum_Effect_Sound.Slot.RAILS)) ;
		
		for(Enum_Effect_Sound.Slot slot : playingOnce.keySet())
			effectSounds.get(playingOnce.get(slot)).setVolume(playingOnceId.get(slot), effectsVolume() * gain(slot)) ;
	}

	private static void stopRailsPlayback()
	{
		if(railsId != -1)
			effectSounds.get(playingRails).stop(railsId) ;
		railsId = -1 ;
		playingRails = null ;
	}

	private static void stopOnce(Enum_Effect_Sound.Slot slot)
	{
		Enum_Effect_Sound playing = playingOnce.remove(slot) ;
		Long id = playingOnceId.remove(slot) ;
		if(playing != null && id != null)
			effectSounds.get(playing).stop(id) ;
	}

	/** Loaded on first use and kept: switching candidates in the lab should not decode again. */
	private static Sound effectSound(Enum_Effect_Sound which)
	{
		Sound sound = effectSounds.get(which) ;
		if(sound == null)
		{
			if(Gdx.audio == null)
				return null ;
			sound = Gdx.audio.newSound(Gdx.files.internal(which.path)) ;
			effectSounds.put(which, sound) ;
		}
		return sound ;
	}

	/** Releases every decoded effect and its OpenAL buffer. */
	public static void DisposeEffects()
	{
		StopEffects() ;
		for(Sound sound : effectSounds.values())
			sound.dispose() ;
		effectSounds.clear() ;
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

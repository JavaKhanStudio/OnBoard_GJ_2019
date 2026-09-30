package jks.sounds;

import java.util.EnumMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;

import jks.amain.GVars_Platform;

/**
 * The effects half of GVars_AudioManager (r197): the rails bed, the one-shot slots and Ross's
 * footsteps. Callers go through GVars_AudioManager.
 */
final class GVars_Effects
{
	private GVars_Effects() {}

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
		applyVolumeChange() ;
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

	/** Takes effect on what is playing right now, as the music does. */
	static void applyVolumeChange()
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
			sound = Gdx.audio.newSound(GVars_Platform.current.soundFile(which.path)) ;
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

}

package jks.sounds;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;

/**
 * The only thing that plays anything. Two machines sit behind it (r197): GVars_Music - the one
 * song, its carriage variants and the crossfade - and GVars_Effects - the rails bed, the one-shot
 * slots and the footsteps. The master volume and muting reach both, so they live here.
 */
public class GVars_AudioManager
{
	public static void init()
	{

	}

	// --- Master volume and mute: both halves -------------------------------------------------

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
		GVars_Music.applyVolumeChange() ;
		GVars_Effects.applyVolumeChange() ;
	}

	// --- Music: GVars_Music ------------------------------------------------------------------

	public static final float CROSSFADE_SECONDS = GVars_Music.CROSSFADE_SECONDS ;

	public static FileHandle fileFor(Enum_Music whichOne)                                   { return GVars_Music.fileFor(whichOne) ; }
	public static FileHandle carriageFile(int carriage, Enum_Music.CarriageVariant variant) { return GVars_Music.carriageFile(carriage, variant) ; }
	public static FileHandle idealFile(int carriage)                                        { return GVars_Music.idealFile(carriage) ; }
	public static void PlayMusic(Enum_Music whichOne)                                       { GVars_Music.PlayMusic(whichOne) ; }
	public static void PlayCarriage(int carriage, Enum_Music.CarriageVariant variant)       { GVars_Music.PlayCarriage(carriage, variant) ; }
	public static void setCarriageVariant(Enum_Music.CarriageVariant variant)               { GVars_Music.setCarriageVariant(variant) ; }
	/** Steps the crossfade. Main_Application calls it every frame, paused or not. */
	public static void update(float delta)                                                  { GVars_Music.update(delta) ; }
	public static boolean isCrossfading()                                                   { return GVars_Music.isCrossfading() ; }
	public static float musicVolume()                                                       { return GVars_Music.musicVolume() ; }
	public static boolean isMusicPlaying()                                                  { return GVars_Music.isMusicPlaying() ; }
	public static Enum_Music currentMusic()                                                 { return GVars_Music.currentMusic() ; }
	public static float musicPosition()                                                     { return GVars_Music.musicPosition() ; }
	public static void StopMusic()                                                          { GVars_Music.StopMusic() ; }
	public static void StopAndDisposeMusic()                                                { GVars_Music.StopAndDisposeMusic() ; }

	// --- Sound effects: GVars_Effects --------------------------------------------------------

	public static float effectsVolume()                                                     { return GVars_Effects.effectsVolume() ; }
	public static void setEffectsVolume(float volume)                                       { GVars_Effects.setEffectsVolume(volume) ; }
	public static void StartRails()                                                         { GVars_Effects.StartRails() ; }
	public static void PlayRails(Enum_Effect_Sound which)                                   { GVars_Effects.PlayRails(which) ; }
	public static void PlayEffect(Enum_Effect_Sound.Slot slot)                              { GVars_Effects.PlayEffect(slot) ; }
	public static void PlayEffect(Enum_Effect_Sound which)                                  { GVars_Effects.PlayEffect(which) ; }
	public static int footstepsPlayed()                                                     { return GVars_Effects.footstepsPlayed() ; }
	public static void PlayFootstep()                                                       { GVars_Effects.PlayFootstep() ; }
	public static void PlayFootstep(Enum_Effect_Sound which)                                { GVars_Effects.PlayFootstep(which) ; }
	public static void StopTrain()                                                          { GVars_Effects.StopTrain() ; }
	public static void StopEffects()                                                        { GVars_Effects.StopEffects() ; }
	public static Enum_Effect_Sound currentRails()                                          { return GVars_Effects.currentRails() ; }
	public static Enum_Effect_Sound lastPlayed(Enum_Effect_Sound.Slot slot)                 { return GVars_Effects.lastPlayed(slot) ; }
	public static void DisposeEffects()                                                     { GVars_Effects.DisposeEffects() ; }
}

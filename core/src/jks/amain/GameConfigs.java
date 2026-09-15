package jks.amain;

/**
 * The settings written to, and read back from, the "config" file next to the game.
 *
 * The fields carry defaults now. A fresh install writes this file before anything has
 * been configured, and all-zero defaults meant a 0x0 window, vsync off and silence.
 */
public class GameConfigs 
{
	public int width = 1600, height = 900 ;
	public boolean isFullScreen = false ;
	public boolean useVsynch = true ;
	public int fps = 60 ; 
	public float volume = 1f ; 
	/** The effects slider: the train sounds, scaled again by volume. */
	public float effectsVolume = 1f ; 
	/** Enum_Train_Sound names, as picked in the sound lab. An unknown name falls back to the default. */
	public String railsSound = null ; 
	public String departureSound = null ; 
	/** Off: the UI, carriages and items are filtered Linear, which Simon chose as the default. */
	public boolean useMipmaps = false ; 
	
}

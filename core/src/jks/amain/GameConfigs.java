package jks.amain;

import jks.sounds.Enum_Effect_Sound;

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
	/** The effects slider: the train sounds and every other effect, scaled again by volume. */
	public float effectsVolume = 1f ; 
	/** Enum_Effect_Sound names, as picked in the sound lab. An unknown name falls back to the default. */
	public String railsSound = null ; 
	public String departureSound = null ; 
	public String keyPieceSound = null ; 
	public String levelCompleteSound = null ; 
	public String footstepsSound = null ; 
	/** Off: the UI, carriages and items are filtered Linear, which Simon chose as the default. */
	public boolean useMipmaps = false ; 
	
	/** The saved sound for a slot. Not a getter, so Jackson leaves it out of the file. */
	public String chosenSound(Enum_Effect_Sound.Slot slot)
	{
		switch(slot)
		{
			case RAILS :          return railsSound ; 
			case DEPARTURE :      return departureSound ; 
			case KEY_PIECE :      return keyPieceSound ; 
			case LEVEL_COMPLETE : return levelCompleteSound ; 
			case FOOTSTEPS :      return footstepsSound ; 
			default :             return null ; 
		}
	}
	
	public void chooseSound(Enum_Effect_Sound sound)
	{
		switch(sound.slot)
		{
			case RAILS :          railsSound = sound.name() ; break ; 
			case DEPARTURE :      departureSound = sound.name() ; break ; 
			case KEY_PIECE :      keyPieceSound = sound.name() ; break ; 
			case LEVEL_COMPLETE : levelCompleteSound = sound.name() ; break ; 
			case FOOTSTEPS :      footstepsSound = sound.name() ; break ; 
		}
	}
	
}

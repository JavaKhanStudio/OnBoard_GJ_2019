package jks.sounds;

public class GVars_Audio 
{
	public static boolean muted = true ; 
	public static float masterVolume = 1 ; 
	public static float musiqueVolume = 1 ; 
	/** The effects slider, on top of masterVolume. Until r39 there were no effects for it to reach. */
	public static float effectVolume = 1 ; 
	
	/** The train sounds the game plays, chosen in the sound lab. */
	public static Enum_Train_Sound railsChoice = Enum_Train_Sound.defaultFor(Enum_Train_Sound.Slot.RAILS) ; 
	public static Enum_Train_Sound departureChoice = Enum_Train_Sound.defaultFor(Enum_Train_Sound.Slot.DEPARTURE) ; 
	
	
	public static void init()
	{
		
	}
	
}

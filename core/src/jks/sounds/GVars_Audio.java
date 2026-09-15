package jks.sounds;

import java.util.EnumMap;

import jks.amain.GameConfigs;

public class GVars_Audio 
{
	public static boolean muted = true ; 
	public static float masterVolume = 1 ; 
	public static float musiqueVolume = 1 ; 
	/** The effects slider, on top of masterVolume. Until r39 there were no effects for it to reach. */
	public static float effectVolume = 1 ; 
	
	/** The candidate each moment plays, chosen in the sound lab. Every slot has one from the start. */
	private static final EnumMap<Enum_Effect_Sound.Slot, Enum_Effect_Sound> choices = new EnumMap<>(Enum_Effect_Sound.Slot.class) ; 
	static
	{
		for(Enum_Effect_Sound.Slot slot : Enum_Effect_Sound.Slot.values())
			choices.put(slot, Enum_Effect_Sound.defaultFor(slot)) ; 
	}
	
	public static Enum_Effect_Sound choice(Enum_Effect_Sound.Slot slot)
	{
		return choices.get(slot) ; 
	}
	
	public static void choose(Enum_Effect_Sound sound)
	{
		choices.put(sound.slot, sound) ; 
	}
	
	/** What the config file says, slot by slot, with the default wherever it names nothing usable. */
	public static void loadChoices(GameConfigs config)
	{
		for(Enum_Effect_Sound.Slot slot : Enum_Effect_Sound.Slot.values())
			choices.put(slot, Enum_Effect_Sound.fromConfig(config.chosenSound(slot), slot)) ; 
	}
	
	
	public static void init()
	{
		
	}
	
}

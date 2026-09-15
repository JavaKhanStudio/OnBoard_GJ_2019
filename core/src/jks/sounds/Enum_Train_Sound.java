package jks.sounds;

/**
 * The train sound candidates, from desktop/assets/sounds/train (r39). Where each one comes
 * from, and who must be credited, is in SOURCES.md next to the files.
 *
 * A level plays one RAILS bed on a loop and, when a new game opens level 1, one DEPARTURE. Which
 * ones is chosen in the sound lab, and saved to the config. Once Simon has chosen, the losing
 * candidates can go.
 */
public enum Enum_Train_Sound
{
	RAILS_RIDE_CLACKS        (Slot.RAILS, "sounds/train/rails_ride_clacks.ogg", "Recorded ride + joint clicks"),
	RAILS_RIDE               (Slot.RAILS, "sounds/train/rails_ride.ogg", "Recorded ride"),
	RAILS_NORTHERN           (Slot.RAILS, "sounds/train/rails_northern.ogg", "Recorded modern carriage"),
	RAILS_SYNTH              (Slot.RAILS, "sounds/train/rails_synth.ogg", "Synthesised clickety-clack"),

	DEPARTURE_WHISTLE_GARRATT(Slot.DEPARTURE, "sounds/train/departure_whistle_garratt.ogg", "Whistle, then Garratt pulls away"),
	DEPARTURE_GARRATT        (Slot.DEPARTURE, "sounds/train/departure_garratt.ogg", "Garratt pulls away"),
	DEPARTURE_WHISTLE_P8     (Slot.DEPARTURE, "sounds/train/departure_whistle_p8.ogg", "Whistle, then P-8 pulls away (credit)"),
	DEPARTURE_P8             (Slot.DEPARTURE, "sounds/train/departure_p8.ogg", "P-8 pulls away (credit)"),
	;

	public enum Slot
	{
		/** Loops under every level. */
		RAILS,
		/** Plays once as a new game opens level 1. */
		DEPARTURE
	}

	public final Slot slot ;
	public final String path ;
	public final String label ;

	Enum_Train_Sound(Slot slot, String path, String label)
	{
		this.slot = slot ;
		this.path = path ;
		this.label = label ;
	}

	/** The first candidate of a slot, used until one is chosen. */
	public static Enum_Train_Sound defaultFor(Slot slot)
	{
		for(Enum_Train_Sound sound : values())
			if(sound.slot == slot)
				return sound ;
		throw new IllegalArgumentException("no candidate for " + slot) ;
	}

	/** The saved choice, or the slot's default when the name is missing, unknown or for the other slot. */
	public static Enum_Train_Sound fromConfig(String name, Slot slot)
	{
		if(name != null)
			for(Enum_Train_Sound sound : values())
				if(sound.name().equals(name) && sound.slot == slot)
					return sound ;
		return defaultFor(slot) ;
	}
}

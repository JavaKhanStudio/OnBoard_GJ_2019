package jks.sounds;

/**
 * Every sound effect the game plays, as candidates grouped by the moment they are for. Where
 * each one comes from, and who must be credited, is in SOURCES.md next to the files:
 * desktop/assets/sounds/train (r39), desktop/assets/sounds/game (r45) and desktop/assets/sounds/steps
 * (r87).
 *
 * Each slot plays one candidate, chosen in the sound lab and saved to the config. Once Simon has
 * chosen, the losing candidates can go.
 */
public enum Enum_Effect_Sound
{
	RAILS_RIDE_CLACKS        (Slot.RAILS, "sounds/train/rails_ride_clacks.ogg", "Recorded ride + joint clicks"),
	RAILS_RIDE               (Slot.RAILS, "sounds/train/rails_ride.ogg", "Recorded ride"),
	RAILS_NORTHERN           (Slot.RAILS, "sounds/train/rails_northern.ogg", "Recorded modern carriage"),
	RAILS_SYNTH              (Slot.RAILS, "sounds/train/rails_synth.ogg", "Synthesised clickety-clack"),

	DEPARTURE_WHISTLE_GARRATT(Slot.DEPARTURE, "sounds/train/departure_whistle_garratt.ogg", "Whistle, then Garratt pulls away"),
	DEPARTURE_GARRATT        (Slot.DEPARTURE, "sounds/train/departure_garratt.ogg", "Garratt pulls away"),
	DEPARTURE_WHISTLE_P8     (Slot.DEPARTURE, "sounds/train/departure_whistle_p8.ogg", "Whistle, then P-8 pulls away (credit)"),
	DEPARTURE_P8             (Slot.DEPARTURE, "sounds/train/departure_p8.ogg", "P-8 pulls away (credit)"),

	KEY_CHIME                (Slot.KEY_PIECE, "sounds/game/key_chime.ogg", "Glockenspiel, two notes up"),
	KEY_SPARKLE              (Slot.KEY_PIECE, "sounds/game/key_sparkle.ogg", "Music box, quick four-note run"),
	KEY_CLINK                (Slot.KEY_PIECE, "sounds/game/key_clink.ogg", "Keys clink, then a bell"),
	KEY_WOOD                 (Slot.KEY_PIECE, "sounds/game/key_wood.ogg", "Marimba, two soft notes"),

	LEVEL_GLOCKENSPIEL       (Slot.LEVEL_COMPLETE, "sounds/game/level_glockenspiel.ogg", "Glockenspiel arpeggio"),
	LEVEL_MUSIC_BOX          (Slot.LEVEL_COMPLETE, "sounds/game/level_music_box.ogg", "Music box phrase"),
	LEVEL_SWELL              (Slot.LEVEL_COMPLETE, "sounds/game/level_swell.ogg", "Soft chord swell with a bell"),
	LEVEL_WHISTLE            (Slot.LEVEL_COMPLETE, "sounds/game/level_whistle.ogg", "Train whistle, then two bells"),

	STEPS_SHOE               (Slot.FOOTSTEPS, "sounds/steps/steps_shoe.ogg", "Soft shoe on the wooden floor"),
	STEPS_BOOT               (Slot.FOOTSTEPS, "sounds/steps/steps_boot.ogg", "Boot heel on the wooden floor"),
	STEPS_RUNNER             (Slot.FOOTSTEPS, "sounds/steps/steps_runner.ogg", "Muffled, on a carpet runner"),
	STEPS_THUD               (Slot.FOOTSTEPS, "sounds/steps/steps_thud.ogg", "Recorded dull thud"),
	;

	public enum Slot
	{
		/** Loops under every level. */
		RAILS("Rails - loops under every level"),
		/** Plays once as a new game opens level 1. */
		DEPARTURE("Departure - once, as a new game opens level 1"),
		/** Ross gains a piece of the key, but not the last one. */
		KEY_PIECE("Key piece - a piece of the key is gained"),
		/** The last piece of the key: the level is done. */
		LEVEL_COMPLETE("Level complete - the key is whole"),
		/** One step, each time Ross's heel comes down while he walks. */
		FOOTSTEPS("Footsteps - one per step while Ross walks");

		public final String label ;

		Slot(String label)
		{this.label = label ;}
	}

	public final Slot slot ;
	public final String path ;
	public final String label ;

	Enum_Effect_Sound(Slot slot, String path, String label)
	{
		this.slot = slot ;
		this.path = path ;
		this.label = label ;
	}

	/** The first candidate of a slot, used until one is chosen. */
	public static Enum_Effect_Sound defaultFor(Slot slot)
	{
		for(Enum_Effect_Sound sound : values())
			if(sound.slot == slot)
				return sound ;
		throw new IllegalArgumentException("no candidate for " + slot) ;
	}

	/** The saved choice, or the slot's default when the name is missing, unknown or for another slot. */
	public static Enum_Effect_Sound fromConfig(String name, Slot slot)
	{
		if(name != null)
			for(Enum_Effect_Sound sound : values())
				if(sound.name().equals(name) && sound.slot == slot)
					return sound ;
		return defaultFor(slot) ;
	}
}

package jks.sounds;

public enum Enum_Music
{
	STARTING_SCREEN,
	GAME_INTRO,
	/** The music under each carriage (r94). Which recording that is depends on GVars_Audio.carriageVariant. */
	CARRIAGE_1(1),
	CARRIAGE_2(2),
	CARRIAGE_3(3),
	CARRIAGE_4(4) ;

	/** 1 to 4 for a carriage's track, 0 for the others. */
	public final int carriage ;

	Enum_Music()
	{this(0) ;}

	Enum_Music(int carriage)
	{this.carriage = carriage ;}

	/** The track for carriage 1 to 4; GAME_INTRO for anything else. */
	public static Enum_Music forCarriage(int carriage)
	{
		for(Enum_Music track : values())
			if(track.carriage == carriage && carriage > 0)
				return track ;
		return GAME_INTRO ;
	}

	/**
	 * What plays under the carriages - the choice the r94 lab compares. Not saved: the game plays
	 * GVars_Audio.SHIPPED_CARRIAGE_VARIANT, MODULATED since r97, until a lab changes it.
	 *
	 *   MAIN       musics/intro.mp3 in every carriage, as before r97
	 *   MODULATED  the same song aged once per carriage, musics/wagons/wa<n>_modulated.ogg, made by
	 *              tools/make_wagon_music.py. What ships.
	 *   IDEAL      a track composed for the carriage, musics/wagons/wa<n>_ideal.(ogg|mp3|wav),
	 *              generated outside from musics/PROMPTS.md. Until one is dropped there, the
	 *              carriage falls back to MAIN.
	 */
	public enum CarriageVariant
	{
		MAIN(1f, 1f, 1f, 1f),
		// The tempo ratios tools/make_wagon_music.py rendered with: change one, change both.
		MODULATED(1.04f, 1.00f, 0.94f, 0.86f),
		IDEAL(0f, 0f, 0f, 0f) ;

		/**
		 * How fast each carriage's recording runs against intro.mp3, 0 when it is a different
		 * piece. Two recordings of the same song carry the position across a carriage change,
		 * so the song goes on in its new colour rather than starting over.
		 */
		private final float[] tempo ;

		CarriageVariant(float... tempo)
		{this.tempo = tempo ;}

		public float tempo(int carriage)
		{
			return carriage >= 1 && carriage <= tempo.length ? tempo[carriage - 1] : 1f ;
		}
	}
}

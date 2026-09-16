package jks.vue.models;

/**
 * The words the game ends on (r60). Before this the last page faded out and nothing replaced
 * it, so the ending was a blank screen that never went anywhere.
 *
 * Every carriage offers the same shape of choice, and it is always the same shape: the caged
 * bird can be fed (sac) or let out (clefCage), the father's portrait drawn on (crayon) or cut
 * (couteau), the hole in the wall patched (porteTrou) or finished (grenade), the glass filled
 * from the jug (cruche) or from the bottle (wisky). The second of each pair is the one that
 * counts a point of karma - GVars_Game.applyItem, choiceNumber 2 - and more than
 * GVars_Game.KARMA_TO_LEAVE of them is what sends Ross out of the door instead of onto the
 * floor beside his son. So the card says how often he took the opening, and where that left him.
 *
 * INVENTED on r60's instruction and meant to be replaced: every word is here, in one place,
 * and nothing outside this class decides anything but the order.
 *
 * French, with accents, so whatever draws it must use GeosansLight or OpenSans. Never
 * OptimusPrinceps: it carries no accented glyph and would leave holes (doubt d8).
 */
public final class ClosureMessage
{
	private ClosureMessage() {}

	/** What both endings open on - the four choices, in the order their carriages come. */
	private static final String[] OPENING =
	{
		"La cage, le portrait, le mur, la bouteille :",
		"quatre fois l'occasion d'ouvrir.",
	} ;

	/** Karma at or under KARMA_TO_LEAVE: he tore the ticket up and sat down (outroStay1, outroStay2). */
	private static final String[] STAYED =
	{
		"Cette fois, il a refermé la porte.",
		"Le billet de 9 h 30 est resté en morceaux par terre.",
	} ;

	/** More karma than KARMA_TO_LEAVE: he walked out while the boy watched (outroLeave). */
	private static final String[] LEFT =
	{
		"Il a ouvert la porte, aussi.",
		"Le 9 h 30 est parti à l'heure, et il était dedans.",
	} ;

	/** Drawn on its own, larger, under the message. */
	public static final String END = "FIN" ;

	private static final String[] TIMES = {"zéro", "une", "deux", "trois", "quatre"} ;

	/** How often he took the opening, said plainly at both ends of the range. */
	public static String tally(int karma)
	{
		int taken = Math.max(0, Math.min(TIMES.length - 1, karma)) ;

		if(taken == 0)
			return "Ross ne l'a jamais prise." ;

		if(taken == TIMES.length - 1)
			return "Ross l'a prise à chaque fois." ;

		return "Ross l'a prise " + TIMES[taken] + " fois sur quatre." ;
	}

	/**
	 * The card, top line first. An empty string is a blank line, not a line to draw.
	 *
	 * @param karma    GVars_Game.karma, 0 to LEVEL_COUNT.
	 * @param leaving  GVars_Game.leavingEnding(), the same test the pictures branch on.
	 */
	public static String[] lines(int karma, boolean leaving)
	{
		String[] ending = leaving ? LEFT : STAYED ;

		return new String[]
		{
			OPENING[0],
			OPENING[1],
			tally(karma),
			"",
			ending[0],
			ending[1],
		} ;
	}
}

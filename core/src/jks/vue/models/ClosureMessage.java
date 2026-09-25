package jks.vue.models;

import jks.index.Index_Text;
import jks.vue.models.game.GVars_Game;

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
 * INVENTED on r60's instruction and meant to be replaced. The words themselves are rows of
 * i18n/textes.tsv, under "Ending" (r76); nothing outside this class decides anything but the order.
 *
 * French, with accents, so whatever draws it must use GeosansLight or Mansalva. Never
 * OptimusPrinceps: it carries no accented glyph and would leave holes (doubt d8).
 */
public final class ClosureMessage
{
	private ClosureMessage() {}

	/** Drawn on its own, larger, under the message. */
	public static final String END = Index_Text.get("ending.end") ;

	/** How often he took the opening, out of the four carriages, said plainly at both ends of the range. */
	public static String tally(int karma)
	{
		int taken = Math.max(0, Math.min(GVars_Game.LEVEL_COUNT, karma)) ;

		if(taken == 0)
			return Index_Text.get("ending.tally.never") ;

		if(taken == GVars_Game.LEVEL_COUNT)
			return Index_Text.get("ending.tally.always") ;

		return Index_Text.get("ending.tally.some", Index_Text.get("ending.count." + taken)) ;
	}

	/**
	 * The card, top line first. An empty string is a blank line, not a line to draw.
	 *
	 * @param karma    GVars_Game.karma, 0 to LEVEL_COUNT.
	 * @param leaving  GVars_Game.leavingEnding(), the same test the pictures branch on.
	 */
	public static String[] lines(int karma, boolean leaving)
	{
		// What both endings open on, then he tore the ticket up and sat down (outroStay1,
		// outroStay2) or walked out while the boy watched (outroLeave).
		String ending = leaving ? "ending.left" : "ending.stayed" ;

		return new String[]
		{
			Index_Text.get("ending.opening1"),
			Index_Text.get("ending.opening2"),
			tally(karma),
			"",
			Index_Text.get(ending + "1"),
			Index_Text.get(ending + "2"),
		} ;
	}
}

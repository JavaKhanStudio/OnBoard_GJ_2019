package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jks.vue.models.ClosureMessage;
import jks.vue.models.game.GVars_Game;

/**
 * The words the ending closes on (r60). No GL: the card's text is decided here and nowhere
 * else, so what it says can be checked without drawing it.
 */
class ClosureMessageTest
{
	private static String joined(int karma, boolean leaving)
	{
		return String.join(" | ", ClosureMessage.lines(karma, leaving)) ;
	}

	@Test
	@DisplayName("the two endings do not say the same thing")
	void endingsDiffer()
	{
		assertNotEquals(joined(4, true), joined(4, false),
			"leaving and staying read identically - the card does not branch") ;
	}

	@Test
	@DisplayName("the card says how often the opening was taken")
	void tallyFollowsKarma()
	{
		assertEquals("Ross ne l'a jamais prise.", ClosureMessage.tally(0)) ;
		assertEquals("Ross l'a prise une fois sur quatre.", ClosureMessage.tally(1)) ;
		assertEquals("Ross l'a prise deux fois sur quatre.", ClosureMessage.tally(2)) ;
		assertEquals("Ross l'a prise trois fois sur quatre.", ClosureMessage.tally(3)) ;
		assertEquals("Ross l'a prise à chaque fois.", ClosureMessage.tally(GVars_Game.LEVEL_COUNT)) ;
	}

	@Test
	@DisplayName("karma outside 0..LEVEL_COUNT still reads as a sentence")
	void tallyIsClamped()
	{
		assertEquals(ClosureMessage.tally(0), ClosureMessage.tally(-3)) ;
		assertEquals(ClosureMessage.tally(GVars_Game.LEVEL_COUNT), ClosureMessage.tally(99)) ;
	}

	@Test
	@DisplayName("every karma the game can reach produces a card")
	void everyKarmaHasACard()
	{
		for (int karma = 0 ; karma <= GVars_Game.LEVEL_COUNT ; karma++)
		{
			boolean leaving = karma > GVars_Game.KARMA_TO_LEAVE ;
			String[] lines = ClosureMessage.lines(karma, leaving) ;

			assertTrue(lines.length >= 4, "karma " + karma + " gave " + lines.length + " lines") ;
			assertTrue(Arrays.asList(lines).contains(ClosureMessage.tally(karma)),
				"karma " + karma + " does not say how often: " + joined(karma, leaving)) ;
		}
	}

	/**
	 * The card is French. OptimusPrinceps carries no accented glyph and would leave holes rather
	 * than throw (doubt d8), so this is the reason Vue_Scenematic_Outro asks GVars_Font for
	 * GeosansLight. If the words ever lose their accents, that reason is worth re-reading.
	 */
	@Test
	@DisplayName("the words are accented, which is why the card is not drawn in the title face")
	void theCardNeedsAnAccentedFace()
	{
		String all = joined(0, false) + joined(4, true) ;
		assertTrue(all.chars().anyMatch(c -> c > 127),
			"no accented character in either card - check which font it should be drawn with") ;
		assertFalse(ClosureMessage.END.chars().anyMatch(c -> c > 127),
			"FIN gained an accent; it is drawn with the same face, which is fine, but say so") ;
	}
}

package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jks.vars.GVars_Serialization;
import jks.vue.models.game.GameItem;
import jks.vue.models.game.ItemOutline;
import jks.vue.models.game.WagonLevel;

/**
 * Which line each carriage item is outlined with (r71): gold for a piece of the key, blue and
 * red for the two sides of a choice, yellow for the rest. No GL: it is decided from the .wa data
 * alone, so the whole cast of the four carriages is pinned here - a changed interaction in a
 * level file shows up as a changed colour.
 */
class ItemOutlineKindTest
{
	@Test
	@DisplayName("every item of the four carriages has the line of what it gives")
	void everyItemHasItsKind() throws Exception
	{
		Map<String, ItemOutline.Kind> expected = new TreeMap<>() ;
		for (String key : new String[] {"baluchon", "dossierPolice", "appareil", "cadre"})
			expected.put(key, ItemOutline.Kind.KEY) ;
		for (String good : new String[] {"sac", "crayon", "porteTrou", "cruche"})
			expected.put(good, ItemOutline.Kind.GOOD) ;
		for (String bad : new String[] {"clefCage", "couteauSocle", "grenade", "wisky"})
			expected.put(bad, ItemOutline.Kind.BAD) ;
		// Used with no choice attached, acted on, or not part of any puzzle.
		for (String neutral : new String[] {"cube", "clope", "lettreAmour", "papier",
				"cubeM", "cage", "tableau", "poubelle", "trou", "boite", "hochet", "verre", "chapeau"})
			expected.put(neutral, ItemOutline.Kind.NEUTRAL) ;

		Map<String, ItemOutline.Kind> actual = new TreeMap<>() ;
		for (int n = 1 ; n <= 4 ; n++)
		{
			WagonLevel level = GVars_Serialization.prepareJson()
				.readValue(new File(Assets.DIR, "game/wagon/wa" + n + ".wa"), WagonLevel.class) ;
			for (GameItem item : level.listItems)
				actual.put(item.name.replace(".png", ""), ItemOutline.of(item, level.listItems)) ;
		}
		assertEquals(expected, actual) ;
	}
}

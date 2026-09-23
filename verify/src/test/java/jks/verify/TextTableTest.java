package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import jks.index.Index_Text;
import jks.vars.GVars_Serialization;
import jks.vue.models.game.GameItem;
import jks.vue.models.game.WagonLevel;

/**
 * The text table, i18n/textes.tsv (r76). No GL: every key a level or the code names must be a
 * row, and every row must say something in French - a missing one would otherwise only show
 * up as its key, typed into a bubble, in front of a player.
 */
class TextTableTest
{
	private static final File TABLE = new File(Assets.DIR, Index_Text.TABLE) ;

	/** The literal keys handed to Index_Text.get in the game's sources. */
	private static final Pattern GET = Pattern.compile("Index_Text\\.get\\(\"([^\"]+)\"\\s*[,)]") ;

	@Test
	@DisplayName("the table has French in every row")
	void everyRowHasFrench()
	{
		assertTrue(Index_Text.languages().contains(Index_Text.FALLBACK), "no fr column: " + Index_Text.languages()) ;

		List<String> empty = new ArrayList<>() ;
		for (Map.Entry<String, Map<String, String>> row : Index_Text.rows().entrySet())
		{
			String fr = row.getValue().get(Index_Text.FALLBACK) ;
			if (fr == null || fr.isEmpty())
				empty.add(row.getKey()) ;
		}
		assertTrue(empty.isEmpty(), "rows with no French text: " + empty) ;
	}

	@Test
	@DisplayName("every hint and message a level names, and every item's click line, is a row of the table")
	void levelKeysExist() throws Exception
	{
		ObjectMapper mapper = GVars_Serialization.prepareJson() ;
		List<String> missing = new ArrayList<>() ;
		for (int n = 1 ; n <= 4 ; n++)
		{
			WagonLevel level = mapper.readValue(new File(Assets.DIR, "game/wagon/wa" + n + ".wa"), WagonLevel.class) ;
			List<String> keys = new ArrayList<>(List.of(level.hint1, level.hint2, level.hint3)) ;
			for (GameItem item : level.listItems)
			{
				if (item.message_Crucial_1 != null) keys.add(item.message_Crucial_1) ;
				if (item.message_Crucial_2 != null) keys.add(item.message_Crucial_2) ;
				// What Ross says when it is clicked (r74): silence, not the key, when missing.
				keys.add(GameItem.clickKey(n, item.name)) ;
			}
			for (String key : keys)
				if (!Index_Text.has(key))
					missing.add("wa" + n + ": " + key) ;
		}
		assertTrue(missing.isEmpty(), "keys a level names that " + Index_Text.TABLE + " does not have: " + missing) ;
	}

	@Test
	@DisplayName("every key the code asks for is a row of the table")
	void codeKeysExist() throws Exception
	{
		List<String> missing = new ArrayList<>() ;
		int found = 0 ;
		try (Stream<Path> sources = Files.walk(new File(Assets.ROOT, "core/src").toPath()))
		{
			for (Path source : (Iterable<Path>) sources.filter(p -> p.toString().endsWith(".java"))::iterator)
			{
				Matcher get = GET.matcher(Files.readString(source, StandardCharsets.UTF_8)) ;
				while (get.find())
				{
					found++ ;
					if (!Index_Text.has(get.group(1)))
						missing.add(source.getFileName() + ": " + get.group(1)) ;
				}
			}
		}
		assertTrue(found > 20, "only " + found + " Index_Text.get calls found - is the pattern still right?") ;
		assertTrue(missing.isEmpty(), "keys the code asks for that " + Index_Text.TABLE + " does not have: " + missing) ;
	}

	/**
	 * OptimusPrinceps has no é è à â ç and no usable É: it leaves a bare accent where the letter
	 * should be (doubt d8). The rows it draws say "[title face]" in their "where" column.
	 */
	@Test
	@DisplayName("nothing drawn in the title face has an accent, in any language")
	void titleFaceRowsAreUnaccented() throws Exception
	{
		List<String> accented = new ArrayList<>() ;
		int titles = 0 ;
		for (String line : Files.readAllLines(TABLE.toPath(), StandardCharsets.UTF_8))
		{
			String[] cells = line.split("\t", -1) ;
			if (line.startsWith("#") || cells.length < 3 || !cells[1].contains("[title face"))
				continue ;
			titles++ ;
			for (int c = 2 ; c < cells.length ; c++)
				if (cells[c].chars().anyMatch(ch -> ch > 127))
					accented.add(cells[0] + " = " + cells[c]) ;
		}
		assertTrue(titles >= 7, "only " + titles + " rows marked [title face]") ;
		assertTrue(accented.isEmpty(), "the title face cannot draw these (d8): " + accented) ;
	}

	@Test
	@DisplayName("a cell's \\n is a line break and its markup is kept")
	void cellsUnescape()
	{
		String hint = Index_Text.get("wa2.hint3") ;
		assertTrue(hint.contains("\n") && !hint.contains("\\n"), "wa2.hint3 lost its line breaks: " + hint) ;
		assertTrue(Index_Text.get("wa1.cage.message1").contains("{WAVE}libre{ENDWAVE}"), "markup did not survive") ;
		assertEquals("Ross l'a prise deux fois sur quatre.",
			Index_Text.get("ending.tally.some", Index_Text.get("ending.count.2"))) ;
	}

	@Test
	@DisplayName("a key missing from the table shows as itself, not as nothing")
	void missingKeyShowsItself()
	{
		assertEquals("no.such.key", Index_Text.get("no.such.key")) ;
	}

	@Test
	@DisplayName("the line lab's write changes one cell and leaves every other line of the file alone")
	void writeChangesOneCell() throws Exception
	{
		Path copy = Files.createTempFile("textes", ".tsv") ;
		try
		{
			Files.copy(TABLE.toPath(), copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING) ;
			List<String> before = Files.readAllLines(copy, StandardCharsets.UTF_8) ;
			String original = Index_Text.get("wa2.clope.click") ;

			String edited = "Deux lignes,\nune barre \\ et « des guillemets »" ;
			Index_Text.write(copy.toFile(), "wa2.clope.click", "fr", edited) ;
			assertEquals(edited, Index_Text.get("wa2.clope.click"), "the game does not hand back the new text") ;

			List<String> after = Files.readAllLines(copy, StandardCharsets.UTF_8) ;
			assertEquals(before.size(), after.size(), "the write added or dropped lines") ;
			List<String> changed = new ArrayList<>() ;
			for (int i = 0 ; i < before.size() ; i++)
				if (!before.get(i).equals(after.get(i)))
					changed.add(after.get(i)) ;
			assertEquals(1, changed.size(), "more than one line changed: " + changed) ;
			assertTrue(changed.get(0).startsWith("wa2.clope.click\t"), "the wrong line changed: " + changed) ;
			assertTrue(changed.get(0).endsWith("\tDeux lignes,\\nune barre \\\\ et « des guillemets »"),
				"the cell is not escaped as the table writes it: " + changed.get(0)) ;

			// What a fresh read of that file gives back is what was written.
			Index_Text.reloadFrom(copy.toFile()) ;
			assertEquals(edited, Index_Text.get("wa2.clope.click"), "the escaped cell does not read back") ;

			Index_Text.write(copy.toFile(), "wa2.clope.click", "fr", original) ;
			assertEquals(before, Files.readAllLines(copy, StandardCharsets.UTF_8), "writing the original back did not restore the file") ;
		}
		finally
		{
			Files.deleteIfExists(copy) ;
			Index_Text.reloadFrom(TABLE) ;
		}
	}
}

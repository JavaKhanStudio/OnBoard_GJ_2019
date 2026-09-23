package jks.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.utils.GdxRuntimeException;

import jks.tools.Utils_Debug;

/**
 * Every word the player reads (r76), from the one table i18n/textes.tsv: a row per text, a
 * column per language. The code and the .wa files name a key; this hands back the words.
 *
 * Read from the classpath, not Gdx.files, so it works inside the shipped jar and in the tests
 * that run without GL. Loaded once, on the first get().
 *
 * A text missing in the chosen language falls back to French, the language the game was
 * written in. A key missing from the table comes back as the key itself - visible on screen,
 * rather than a blank bubble nobody notices - and TextTableTest fails on it before that.
 */
public final class Index_Text
{
	private Index_Text() {}

	public static final String TABLE = "i18n/textes.tsv" ;
	/** The language every row has, and the one a missing text falls back to. */
	public static final String FALLBACK = "fr" ;

	/** The column read, by its header name. Only French exists for now. */
	private static String language = FALLBACK ;

	/** key -> (language -> text), in the table's order. */
	private static Map<String, Map<String, String>> rows ;
	private static List<String> languages ;

	public static String get(String key)
	{
		Map<String, String> row = table().get(key) ;
		if(row == null)
		{
			Utils_Debug.warn("No text for key '" + key + "' in " + TABLE) ;
			return key ;
		}

		String text = row.get(language) ;
		if(text == null || text.isEmpty())
			text = row.get(FALLBACK) ;
		return text ;
	}

	/** get(key) with {0}, {1}... replaced by the arguments, in order. */
	public static String get(String key, Object... args)
	{
		String text = get(key) ;
		for(int a = 0 ; a < args.length ; a++)
			text = text.replace("{" + a + "}", String.valueOf(args[a])) ;
		return text ;
	}

	public static boolean has(String key)
	{
		return table().containsKey(key) ;
	}

	/** Must be a column of the table. Takes effect for text fetched from now on. */
	public static void setLanguage(String code)
	{
		table() ;
		if(!languages.contains(code))
			throw new GdxRuntimeException("No '" + code + "' column in " + TABLE + ", which has " + languages) ;
		language = code ;
	}

	public static String getLanguage()
	{
		return language ;
	}

	public static List<String> languages()
	{
		table() ;
		return Collections.unmodifiableList(languages) ;
	}

	public static Map<String, Map<String, String>> rows()
	{
		return Collections.unmodifiableMap(table()) ;
	}

	private static Map<String, Map<String, String>> table()
	{
		if(rows == null)
			load() ;
		return rows ;
	}

	private static void load()
	{
		InputStream in = Index_Text.class.getClassLoader().getResourceAsStream(TABLE) ;
		if(in == null)
			throw new GdxRuntimeException("The text table " + TABLE + " is not on the classpath") ;

		Map<String, Map<String, String>> read = new LinkedHashMap<String, Map<String, String>>() ;
		List<String> header = null ;

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			String line ;
			int number = 0 ;
			while((line = reader.readLine()) != null)
			{
				number++ ;
				if(line.isEmpty() || line.startsWith("#"))
					continue ;

				String[] cells = line.split("\t", -1) ;
				if(header == null)
				{
					header = new ArrayList<String>() ;
					Collections.addAll(header, cells) ;
					if(!"key".equals(cells[0]) || !header.contains(FALLBACK))
						throw new GdxRuntimeException(TABLE + ":" + number + ": the header must start with 'key' and name an '" + FALLBACK + "' column") ;
					continue ;
				}

				if(read.containsKey(cells[0]))
					throw new GdxRuntimeException(TABLE + ":" + number + ": key '" + cells[0] + "' is there twice") ;

				Map<String, String> row = new LinkedHashMap<String, String>() ;
				// Column 1 is "where", for the translator; the languages follow it.
				for(int c = 2 ; c < header.size() && c < cells.length ; c++)
					row.put(header.get(c), unescape(cells[c])) ;
				read.put(cells[0], row) ;
			}
		}
		catch(IOException e)
		{
			throw new GdxRuntimeException("Could not read " + TABLE, e) ;
		}

		if(header == null)
			throw new GdxRuntimeException(TABLE + " has no header row") ;

		languages = header.subList(2, header.size()) ;
		rows = read ;
	}

	/** \n is a line break and \\ a backslash; anything else after a backslash is kept as it is. */
	static String unescape(String cell)
	{
		StringBuilder text = new StringBuilder(cell.length()) ;
		for(int i = 0 ; i < cell.length() ; i++)
		{
			char c = cell.charAt(i) ;
			if(c == '\\' && i + 1 < cell.length())
			{
				char next = cell.charAt(++i) ;
				if(next == 'n') text.append('\n') ;
				else if(next == 't') text.append('\t') ;
				else if(next == '\\') text.append('\\') ;
				else text.append(c).append(next) ;
			}
			else
				text.append(c) ;
		}
		return text.toString() ;
	}
}

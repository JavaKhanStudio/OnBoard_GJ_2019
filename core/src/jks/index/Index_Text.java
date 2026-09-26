package jks.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;

import jks.amain.GVars_Platform;
import jks.tools.Utils_Debug;

/**
 * Every word the player reads (r76), from the one table i18n/textes.tsv: a row per text, a
 * column per language. The code and the .wa files name a key; this hands back the words.
 *
 * Read through Gdx.files.internal, which finds it in desktop/assets, inside the shipped jar and
 * in the browser build alike (r140: the classloader it was read with does not exist in GWT).
 * The tests that run without GL get a headless Gdx.files from verify's DesktopSession. Loaded
 * once, on the first get().
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
		if(Gdx.files == null)
			throw new GdxRuntimeException("The text table " + TABLE + " was asked for before there was a Gdx.files to read it with") ;
		FileHandle table = Gdx.files.internal(TABLE) ;
		if(!table.exists())
			throw new GdxRuntimeException("The text table " + TABLE + " is not among the assets") ;
		load(table) ;
	}

	private static void load(FileHandle table)
	{
		Map<String, Map<String, String>> read = new LinkedHashMap<String, Map<String, String>>() ;
		List<String> header = null ;

		List<String> lines = lines(table.readString("UTF-8")) ;
		for(int number = 1 ; number <= lines.size() ; number++)
		{
			String line = lines.get(number - 1) ;
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

		if(header == null)
			throw new GdxRuntimeException(TABLE + " has no header row") ;

		languages = header.subList(2, header.size()) ;
		rows = read ;
	}

	/**
	 * The table as it sits in the source tree, for the line lab (r74) to write into. The game
	 * reads the copy among the assets, which a build refreshes; this is the one to edit. Where it
	 * is, is the platform's (r135); null where there is no source tree.
	 */
	public static FileHandle sourceTable()
	{
		return GVars_Platform.current.sourceTextTable() ;
	}

	/** Reads the table again from the file, so an edit made by hand shows without a rebuild. */
	public static void reloadFrom(FileHandle table)
	{
		load(table) ;
	}

	/**
	 * Replaces one cell of the table file - a key's text in one language - and the text the
	 * game hands back for it from now on (r74). Every other line, comment and blank is written
	 * back as it was. The key must already be a row: adding one is a job for the file itself,
	 * where its "where" column gets written.
	 */
	public static void write(FileHandle table, String key, String code, String text)
	{
		List<String> lines = lines(table.readString("UTF-8")) ;
		int column = -1 ;
		boolean found = false ;
		for(int i = 0 ; i < lines.size() && !found ; i++)
		{
			String line = lines.get(i) ;
			if(line.isEmpty() || line.startsWith("#"))
				continue ;
			String[] cells = line.split("\t", -1) ;
			if(column < 0)
			{
				column = Arrays.asList(cells).indexOf(code) ;
				if(column < 2)
					throw new GdxRuntimeException("No '" + code + "' column in " + table) ;
				continue ;
			}
			if(!cells[0].equals(key))
				continue ;
			if(cells.length <= column)
				cells = Arrays.copyOf(cells, column + 1) ;
			for(int c = 0 ; c < cells.length ; c++)
				if(cells[c] == null)
					cells[c] = "" ;
			cells[column] = escape(text) ;
			lines.set(i, String.join("\t", cells)) ;
			found = true ;
		}
		if(!found)
			throw new GdxRuntimeException("No row '" + key + "' in " + table + " - add it there first") ;

		table.writeString(String.join("\n", lines) + "\n", false, "UTF-8") ;

		table().get(key).put(code, text) ;
	}

	/** The lines of a text the way Files.readAllLines cut them: \n or \r\n, no empty one after the last. */
	static List<String> lines(String text)
	{
		List<String> lines = new ArrayList<String>(Arrays.asList(text.split("\r?\n", -1))) ;
		if(lines.get(lines.size() - 1).isEmpty())
			lines.remove(lines.size() - 1) ;
		return lines ;
	}

	/** The reverse of {@link #unescape}: what a cell holds for a text. */
	static String escape(String text)
	{
		return text.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t") ;
	}

	/** \n is a line break and \\ a backslash; anything else after a backslash is kept as it is. */
	public static String unescape(String cell)
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

package jks.verify;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Locates the shipped asset tree and answers case-sensitive existence questions about it. */
public final class Assets
{
	private Assets() {}

	public static final File DIR  = new File(System.getProperty("onboard.assets"));
	public static final File ROOT = new File(System.getProperty("onboard.root"));

	/** Extensions we treat as "this string names a file that must exist". */
	private static final String[] ASSET_EXT =
		{".png", ".jpg", ".jpeg", ".atlas", ".fnt", ".json", ".mp3", ".ogg", ".wav", ".ttf", ".plax"};

	public static boolean looksLikeAssetPath(String s)
	{
		if (s == null) return false;
		String lower = s.toLowerCase();
		for (String e : ASSET_EXT)
			if (lower.endsWith(e)) return true;
		return false;
	}

	/**
	 * True only if the path exists with exactly this spelling. {@link File#exists()} alone is
	 * not enough: it is case-insensitive on Windows and macOS, which is precisely how
	 * UI_newGame.png survived in the source for years while the file was UI_NewGame.png.
	 */
	public static boolean existsExactly(String relative)
	{
		File f = new File(DIR, relative);
		if (!f.exists()) return false;
		try
		{
			return f.getCanonicalFile().getPath().endsWith(relative.replace('/', File.separatorChar));
		}
		catch (IOException e)
		{
			return false;
		}
	}

	/** The same path ignoring case, if one exists - used to make failures actionable. */
	public static String findIgnoringCase(String relative)
	{
		File cur = DIR;
		StringBuilder found = new StringBuilder();
		for (String segment : relative.split("/"))
		{
			File[] entries = cur.listFiles();
			if (entries == null) return null;
			File match = null;
			for (File e : entries)
				if (e.getName().equalsIgnoreCase(segment)) { match = e; break; }
			if (match == null) return null;
			if (found.length() > 0) found.append('/');
			found.append(match.getName());
			cur = match;
		}
		return found.toString();
	}

	/** Every .java file in the project, so tests can assert things about the source itself. */
	public static List<Path> javaSources()
	{
		List<Path> out = new ArrayList<>();
		for (String module : new String[]{"core", "desktop", "editor", "test"})
		{
			Path src = ROOT.toPath().resolve(module).resolve("src");
			if (!Files.isDirectory(src)) continue;
			try (Stream<Path> walk = Files.walk(src))
			{
				walk.filter(p -> p.toString().endsWith(".java")).forEach(out::add);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		return out;
	}

	public static String read(Path p)
	{
		try
		{
			return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}

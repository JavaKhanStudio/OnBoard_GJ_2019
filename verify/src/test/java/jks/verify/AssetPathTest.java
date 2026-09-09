package jks.verify;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jks.index.Index_Interface;
import jks.vinterface.font.Index_Fonts;

/**
 * Every asset the code names must exist on disk with exactly that spelling.
 *
 * This is the regression guard for the bug that stopped the game running at all: the source
 * asked for ui/icon/menu/UI_newGame.png while the file was UI_NewGame.png. That is invisible
 * on Windows and fatal on Linux, and an engine upgrade is exactly the sort of change that
 * shuffles asset names around.
 */
class AssetPathTest
{
	/** Asset paths declared as constants in Index_Interface. */
	private static Map<String, String> constantPaths() throws Exception
	{
		Map<String, String> paths = new LinkedHashMap<>();
		for (Field f : Index_Interface.class.getDeclaredFields())
		{
			if (!Modifier.isStatic(f.getModifiers())) continue;
			if (f.getType() != String.class) continue;
			f.setAccessible(true);
			String value = (String) f.get(null);
			if (Assets.looksLikeAssetPath(value)) paths.put(f.getName(), value);
		}
		return paths;
	}

	@Test
	@DisplayName("every Index_Interface asset constant resolves case-sensitively")
	void indexInterfaceConstantsResolve() throws Exception
	{
		Map<String, String> paths = constantPaths();
		assertTrue(paths.size() >= 20, "expected to discover the UI asset constants, found " + paths.size());
		assertAllExist(paths);
	}

	@Test
	@DisplayName("every font declared in Index_Fonts exists")
	void fontsResolve()
	{
		Map<String, String> paths = new LinkedHashMap<>();
		for (Index_Fonts.Enum_Fonts font : Index_Fonts.Enum_Fonts.values())
			paths.put(font.name(), font.path);

		assertTrue(paths.size() > 0, "no fonts declared");
		assertAllExist(paths);
	}

	@Test
	@DisplayName("every Gdx.files.internal(\"...\") literal in the source resolves")
	void internalFileLiteralsResolve()
	{
		Pattern literal = Pattern.compile("Gdx\\.files\\.internal\\(\\s*\"([^\"]+)\"");
		Map<String, String> paths = new LinkedHashMap<>();

		for (Path source : Assets.javaSources())
		{
			String text = stripComments(Assets.read(source));
			Matcher m = literal.matcher(text);
			while (m.find())
			{
				String value = m.group(1);
				// The level loader deliberately prefixes "assets/" because it reads through
				// java.io.File with desktop/ as the working directory; LevelDataTest covers those.
				if (value.startsWith("assets/")) continue;
				if (Assets.looksLikeAssetPath(value))
					paths.put(source.getFileName() + ": " + value, value);
			}
		}

		assertTrue(paths.size() > 0, "found no Gdx.files.internal literals - has the scan broken?");
		assertAllExist(paths);
	}

	private static String stripComments(String java)
	{
		return java.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
	}

	private static void assertAllExist(Map<String, String> paths)
	{
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, String> e : paths.entrySet())
		{
			if (Assets.existsExactly(e.getValue())) continue;

			String nearby = Assets.findIgnoringCase(e.getValue());
			problems.add(nearby != null
				? e.getKey() + "\n      wants : " + e.getValue() + "\n      on disk: " + nearby + "   (case mismatch)"
				: e.getKey() + "\n      wants : " + e.getValue() + "\n      on disk: <missing>");
		}

		if (!problems.isEmpty())
			fail(problems.size() + " unresolved asset path(s):\n  - " + String.join("\n  - ", problems));
	}
}

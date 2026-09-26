package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jks.amain.GameConfigs;
import jks.amain.Utils_Config;
import jks.vars.GVars_Serialization;
import jks.vue.models.game.WagonLevel;

/**
 * What the game makes of its JSON, field by field (r136). The .wa levels and the config were
 * read by Jackson until the browser build needed a reader GWT can translate. Jackson and libGDX
 * Json do not see the same fields - Jackson reads public fields only, libGDX every field that
 * is not transient - so wa1's stray "currentLevel": 1 would have started being read.
 *
 * Every field of the parsed model is dumped, whatever its visibility, and compared with a dump
 * recorded from the Jackson reader at b079414 (resources/serialization/*.golden.txt). A field one reader fills
 * and the other leaves alone shows as a changed line. -PrecordGolden writes the dumps again.
 */
class SerializationDumpTest
{
	private static final File DUMPS = new File(System.getProperty("onboard.verify"), "src/test/resources/serialization");

	@ParameterizedTest(name = "wa{0}.wa parses to the model Jackson made of it")
	@ValueSource(ints = {1, 2, 3, 4})
	void levelParsesAsBefore(int n) throws Exception
	{
		String text = Files.readString(new File(Assets.DIR, "game/wagon/wa" + n + ".wa").toPath(), StandardCharsets.UTF_8);
		WagonLevel level = GVars_Serialization.prepareJson().fromJson(WagonLevel.class, text);
		compare("wa" + n, level);
	}

	/**
	 * config-2019: as the 2019 build wrote it, and players on desktop keep theirs. config-jackson:
	 * every field, as the Jackson builds wrote it. config-mipmaps: one of those with a field set.
	 */
	@ParameterizedTest(name = "{0} parses to the settings Jackson made of it")
	@ValueSource(strings = {"config-2019", "config-jackson", "config-mipmaps"})
	void configParsesAsBefore(String name) throws Exception
	{
		String text = Files.readString(new File(DUMPS, name + ".json").toPath(), StandardCharsets.UTF_8);
		compare(name, Utils_Config.parse(text));
	}

	/** The editor saves with the game's own Json (r136): nothing of a level may change on the way. */
	@ParameterizedTest(name = "wa{0} comes back the same from an editor save")
	@ValueSource(ints = {1, 2, 3, 4})
	void levelSurvivesAnEditorSave(int n) throws Exception
	{
		String text = Files.readString(new File(Assets.DIR, "game/wagon/wa" + n + ".wa").toPath(), StandardCharsets.UTF_8);
		WagonLevel level = GVars_Serialization.prepareJson().fromJson(WagonLevel.class, text);
		String saved = GVars_Serialization.prepareJson().prettyPrint(level);
		assertEquals(dump("wa" + n, level), dump("wa" + n, GVars_Serialization.prepareJson().fromJson(WagonLevel.class, saved)),
			"wa" + n + " read back from what the editor would save is not the level it saved:\n" + saved);
	}

	@ParameterizedTest(name = "{0} comes back the same once the game has saved it")
	@ValueSource(strings = {"config-2019", "config-jackson", "config-mipmaps"})
	void configSurvivesASave(String name) throws Exception
	{
		GameConfigs read = Utils_Config.parse(Files.readString(new File(DUMPS, name + ".json").toPath(), StandardCharsets.UTF_8));
		String saved = Utils_Config.write(read);
		assertEquals(dump(name, read), dump(name, Utils_Config.parse(saved)), name + " did not survive a save:\n" + saved);
		// Every field, names quoted, as Jackson wrote it: a player can still read and edit it.
		assertTrue(saved.contains("\"footstepsSound\"") && saved.contains("\"width\""), name + " was saved without every field, quoted:\n" + saved);
	}

	private static List<String> dump(String name, Object parsed) throws IllegalAccessException
	{
		List<String> lines = new ArrayList<>();
		dump(name, parsed, lines);
		return lines;
	}

	private static void compare(String name, Object parsed) throws Exception
	{
		List<String> lines = dump(name, parsed);
		String actual = String.join("\n", lines) + "\n";
		File golden = new File(DUMPS, name + ".golden.txt");
		if (Boolean.getBoolean("onboard.golden.record") || !golden.isFile())
		{
			Files.writeString(golden.toPath(), actual, StandardCharsets.UTF_8);
			System.out.println("recorded " + golden);
			return;
		}
		assertTrue(lines.size() > 5, name + ": the dump is nearly empty: " + lines);
		assertEquals(Files.readString(golden.toPath(), StandardCharsets.UTF_8), actual,
			name + ": the model parsed from it differs from what Jackson made of it (" + golden + ")");
	}

	/** One line per field, down through the game's own classes and lists; anything else by toString. */
	static void dump(String path, Object value, List<String> out) throws IllegalAccessException
	{
		if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean
			|| value instanceof Character || value instanceof Enum)
		{
			out.add(path + " = " + (value instanceof String ? "\"" + ((String) value).replace("\n", "\\n") + "\"" : value));
			return;
		}
		if (value instanceof List)
		{
			List<?> list = (List<?>) value;
			out.add(path + " = " + value.getClass().getSimpleName() + "(" + list.size() + ")");
			for (int i = 0; i < list.size(); i++)
				dump(path + "[" + i + "]", list.get(i), out);
			return;
		}
		if (!value.getClass().getName().startsWith("jks."))
		{
			out.add(path + " = " + value.getClass().getSimpleName() + " " + value);
			return;
		}
		for (Class<?> c = value.getClass(); c != Object.class; c = c.getSuperclass())
			for (Field field : c.getDeclaredFields())
			{
				if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
				field.setAccessible(true);
				dump(path + "." + field.getName(), field.get(value), out);
			}
	}
}

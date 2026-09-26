package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import jks.vars.GVars_Serialization;
import jks.vue.models.game.GameItem;
import jks.vue.models.game.WagonLevel;

/**
 * The four levels are .wa files written by the in-house editor in 2019, with Jackson; the game
 * and the editor read and write them with libGDX Json since r136. They are the least replaceable
 * thing in the repository - the code can be rewritten, this content cannot - so the reader
 * that reads them is pinned here, and SerializationDumpTest holds it to what Jackson made of them.
 */
class LevelDataTest
{
	private static final int LEVEL_COUNT = 4;

	private static File levelFile(int n)
	{
		return new File(Assets.DIR, "game/wagon/wa" + n + ".wa");
	}

	private static WagonLevel load(int n) throws Exception
	{
		Json json = GVars_Serialization.prepareJson();
		assertNotNull(json, "GVars_Serialization.prepareJson() returned null");
		return json.fromJson(WagonLevel.class, new FileHandle(levelFile(n)));
	}

	@Test
	@DisplayName("all four level files are present")
	void allLevelsPresent()
	{
		for (int n = 1; n <= LEVEL_COUNT; n++)
			assertTrue(levelFile(n).isFile(), "missing level file " + levelFile(n));
	}

	@ParameterizedTest(name = "level wa{0} deserialises with the game''s own mapper config")
	@ValueSource(ints = {1, 2, 3, 4})
	void levelDeserialises(int n) throws Exception
	{
		WagonLevel level = load(n);

		assertNotNull(level, "wa" + n + " deserialised to null");
		assertNotNull(level.rossAge, "wa" + n + ": rossAge missing");
		assertNotNull(level.path_meta, "wa" + n + ": path_meta missing");
		assertNotNull(level.path_parallax, "wa" + n + ": path_parallax missing");
		assertNotNull(level.listItems, "wa" + n + ": listItems missing");
		assertFalse(level.listItems.isEmpty(), "wa" + n + ": no items - the level would be empty");
		assertNotNull(level.hint1, "wa" + n + ": hint1 missing (Clef reads this field directly)");
	}

	@ParameterizedTest(name = "level wa{0} re-saves with the same item fields")
	@ValueSource(ints = {1, 2, 3, 4})
	void itemFieldsRoundTrip(int n) throws Exception
	{
		// GameItem is shared with the editor, which saves with the game's own Json: runtime
		// state such as picked or hovered (r40) must stay out of the files it writes.
		JsonValue onDisk = new JsonReader().parse(new FileHandle(levelFile(n))).get("listItems").get(0);
		JsonValue resaved = new JsonReader().parse(GVars_Serialization.prepareJson().prettyPrint(load(n))).get("listItems").get(0);

		Set<String> expected = new TreeSet<>(), actual = new TreeSet<>();
		for (JsonValue field = onDisk.child; field != null; field = field.next) expected.add(field.name);
		// Data fields GameItem gained after the four files were last saved.
		expected.addAll(List.of("message_Crucial_1", "message_Crucial_2", "path_Inventaire"));
		for (JsonValue field = resaved.child; field != null; field = field.next) actual.add(field.name);
		assertEquals(expected, actual, "wa" + n + ": a re-saved item would not have the fields the file has");
	}

	@Test
	@DisplayName("the knife's inventory image survives an editor re-save")
	void inventoryImageRoundTrips() throws Exception
	{
		// path_Inventaire (r86) is what the bar shows for the knife; the editor has no widget for
		// it, so a save must at least carry it through. The editor writes with the game's Json.
		Json json = GVars_Serialization.prepareJson();
		WagonLevel resaved = json.fromJson(WagonLevel.class, json.prettyPrint(load(2)));
		GameItem knife = resaved.listItems.stream()
			.filter(item -> "couteauSocle.png".equals(item.name)).findFirst().orElseThrow();
		assertEquals("couteau.png", knife.path_Inventaire, "an editor re-save of wa2 lost the knife's inventory image");
		assertEquals("couteauSocle_1.png", knife.path_EtatApres_1, "an editor re-save of wa2 lost the empty mount");
	}

	@ParameterizedTest(name = "level wa{0} keeps a stable item count")
	@ValueSource(ints = {1, 2, 3, 4})
	void itemCountIsStable(int n) throws Exception
	{
		// Recorded from the 2019 data. If a serialisation change silently drops items,
		// the game still starts and the puzzle is quietly unsolvable - so pin the numbers.
		int[] expected = {0, 6, 6, 7, 6};
		assertEquals(expected[n], load(n).listItems.size(), "wa" + n + ": item count changed");
	}

	@Test
	@DisplayName("levels keep their intended character age and season")
	void levelIdentityIsStable() throws Exception
	{
		// Ross ages through the four carriages; each age selects a different sprite atlas
		// and each season a different parallax backdrop. A silent change here would not
		// crash anything, it would just tell the wrong story.
		String[] ages    = {null, "enfant", "ado", "soldier", "marie"};
		String[] plax    = {null, "Printemps.plax", "ete.plax", "automne.plax", "Hiver.plax"};
		for (int n = 1; n <= LEVEL_COUNT; n++)
		{
			WagonLevel level = load(n);
			assertEquals(ages[n], level.rossAge, "wa" + n + ": character age changed");
			assertEquals(plax[n], level.path_parallax, "wa" + n + ": parallax backdrop changed");
		}
	}

	@ParameterizedTest(name = "every texture referenced by level wa{0} exists")
	@ValueSource(ints = {1, 2, 3, 4})
	void levelTexturesResolve(int n) throws Exception
	{
		WagonLevel level = load(n);
		List<String> problems = new ArrayList<>();

		// Paths built at runtime in WagonLevel.init() ...
		String meta = "game/wagon/" + level.path_meta + "/";
		check(meta + "WAGON.png", "wa" + n + " backdrop", problems);
		check(meta + level.path_parallax, "wa" + n + " parallax", problems);

		// ... and in GameItem.init().
		for (GameItem item : level.listItems)
		{
			String base = "game/wagon/" + item.path + "/";
			check(base + item.path_EtatDebut, "wa" + n + " item " + item.name + " [initial]", problems);
			if (notBlank(item.path_EtatApres_1))
				check(base + item.path_EtatApres_1, "wa" + n + " item " + item.name + " [state 1]", problems);
			if (notBlank(item.path_EtatApres_2))
				check(base + item.path_EtatApres_2, "wa" + n + " item " + item.name + " [state 2]", problems);
			if (notBlank(item.path_Inventaire))
				check(base + item.path_Inventaire, "wa" + n + " item " + item.name + " [inventory]", problems);
		}

		if (!problems.isEmpty())
			fail(problems.size() + " unresolved level texture(s):\n  - " + String.join("\n  - ", problems));
	}

	@ParameterizedTest(name = "level wa{0}'s art is the shape the carriage is drawn at")
	@ValueSource(ints = {1, 2, 3, 4})
	void carriageArtIsDrawnAtItsPaintedShape(int n) throws Exception
	{
		// d13 (r70): the art was drawn 5.5% wider than painted and the items placed against the
		// stretch. A WAGON.png of another shape would be stretched again, under items that fit it.
		WagonLevel level = load(n);
		File art = new File(Assets.DIR, "game/wagon/" + level.path_meta + "/WAGON.png");
		java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(art);
		float painted = (float) image.getWidth() / image.getHeight();
		assertEquals(WagonLevel.PAINTED_ASPECT, painted, 0.001f, art + " is " + image.getWidth() + "x"
			+ image.getHeight() + ", but the carriage is drawn at 3840:1080 (WagonLevel.PAINTED_ASPECT)");
	}

	private static boolean notBlank(String s)
	{
		return s != null && !s.trim().isEmpty();
	}

	private static void check(String path, String label, List<String> problems)
	{
		if (Assets.existsExactly(path)) return;
		String nearby = Assets.findIgnoringCase(path);
		problems.add(label + "\n      wants : " + path
			+ "\n      on disk: " + (nearby != null ? nearby + "   (case mismatch)" : "<missing>"));
	}
}

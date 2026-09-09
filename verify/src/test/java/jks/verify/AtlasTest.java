package jks.verify;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;

/**
 * The .atlas files were written by an old gdx-tools TexturePacker. libGDX has revised the
 * atlas format since (extra fields, a different index encoding), and the parser is the part
 * most likely to quietly change meaning across an engine upgrade - a region that fails to
 * parse becomes a missing sprite, not an exception.
 *
 * TextureAtlasData parses without a GL context, so this runs as a plain unit test.
 */
class AtlasTest
{
	static List<File> atlases() throws IOException
	{
		try (Stream<Path> walk = Files.walk(Assets.DIR.toPath()))
		{
			return walk.filter(p -> p.toString().endsWith(".atlas"))
			           .map(Path::toFile)
			           .sorted()
			           .collect(Collectors.toList());
		}
	}

	@ParameterizedTest(name = "{0} parses and its pages exist")
	@MethodSource("atlases")
	@DisplayName("every texture atlas parses and points at real pages")
	void atlasParses(File atlas)
	{
		FileHandle handle = new FileHandle(atlas);
		TextureAtlasData data = new TextureAtlasData(handle, handle.parent(), false);

		assertFalse(data.getPages().isEmpty(), atlas.getName() + ": no pages");
		assertFalse(data.getRegions().isEmpty(), atlas.getName() + ": no regions");

		List<String> missing = new ArrayList<>();
		for (TextureAtlasData.Page page : data.getPages())
			if (!page.textureFile.exists())
				missing.add(page.textureFile.path());

		assertTrue(missing.isEmpty(), atlas.getName() + ": missing page image(s) " + missing);
	}
}

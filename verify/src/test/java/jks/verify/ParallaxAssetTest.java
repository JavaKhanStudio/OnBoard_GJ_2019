package jks.verify;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
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
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import jks.tools2d.parallax.pages.Parallax_Model;
import jks.tools2d.parallax.pages.WholePage_Model;
import jks.tools2d.parallax.heart.GVars_Serialization;

/**
 * The .plax backdrops are Kryo binaries written in 2019 by kryo 5.0.0-RC1, through the old
 * parallaxReader0.7.jar. Kryo's wire format is not stable between releases, and the failure
 * is silent: the file still "loads", the strings just come back as mojibake, and the first
 * sign of trouble is a missing-file exception several frames later.
 *
 * The game now reads them through io.github.javakhanstudio:parallax-background, which runs on
 * kryo 5.6.2 and carries its own reader for that 2019 layout (WholePage_Model_Serializer,
 * references on). These tests read through that same GVars_Serialization, so they check what
 * the game reads - there is no longer a copy of it in this repo.
 *
 * A fresh write-then-read round-trip passes on any Kryo version, so it proves nothing here.
 * These tests read the actual shipped bytes.
 */
class ParallaxAssetTest
{
	static List<File> parallaxFiles() throws IOException
	{
		try (Stream<Path> walk = Files.walk(Assets.DIR.toPath()))
		{
			return walk.filter(p -> p.toString().endsWith(".plax"))
			           .map(Path::toFile)
			           .sorted()
			           .collect(Collectors.toList());
		}
	}

	@ParameterizedTest(name = "{0} still deserialises to readable data")
	@MethodSource("parallaxFiles")
	@DisplayName("the shipped .plax files still deserialise through the parallax library")
	void plaxDeserialises(File plax) throws Exception
	{
		Kryo kryo = GVars_Serialization.prepareKryo();

		WholePage_Model page;
		try (Input input = new Input(new FileInputStream(plax)))
		{
			page = kryo.readObject(input, WholePage_Model.class);
		}

		assertNotNull(page, plax.getName() + ": deserialised to null");
		assertNotNull(page.pageModel, plax.getName() + ": no page model");

		String atlasName = page.pageModel.atlasName;
		assertNotNull(atlasName, plax.getName() + ": atlas name missing");

		// The tell-tale of a Kryo format mismatch: the field decodes, but into rubbish.
		assertTrue(atlasName.chars().allMatch(c -> c >= 0x20 && c < 0x7F), plax.getName()
			+ ": atlas name decoded to non-printable characters -> \"" + atlasName + "\"."
			+ " The Kryo version no longer matches the one that wrote this file.");

		// The library loads the atlas by its bare name, so the game draws the copy at the asset
		// ROOT. Nothing in this repo reads the copy beside the .plax.
		assertTrue(Assets.existsExactly(atlasName), plax.getName() + ": names an atlas the game"
			+ " cannot load -> " + atlasName + " (resolved to " + new File(Assets.DIR, atlasName)
			+ "). The game reads it from the asset root, not from beside the .plax.");

		File atlas = new File(Assets.DIR, atlasName);
		FileHandle handle = new FileHandle(atlas);
		TextureAtlasData data = new TextureAtlasData(handle, handle.parent(), false);
		assertFalse(data.getPages().isEmpty(), atlasName + ": no pages");

		List<String> missing = new ArrayList<>();
		for (TextureAtlasData.Page atlasPage : data.getPages())
			if (!Assets.existsExactly(Assets.DIR.toPath().relativize(atlasPage.textureFile.file().toPath())
			                                 .toString().replace(File.separatorChar, '/')))
				missing.add(atlasPage.textureFile.path());
		assertTrue(missing.isEmpty(), atlasName + ": missing page image(s) " + missing);

		// Each layer is drawn from atlas.findRegions(regionName).get(regionPosition). libGDX reads
		// the atlas in the platform's default charset (UTF-8 since Java 18, cp1252 on Windows
		// before that), so a non-ASCII name only matches on the machine it was written on: ete.plax
		// once said "Ã©tÃ©" - "été" as cp1252 saw it - and level 2 died on "index can't be >= size".
		List<String> unreachable = new ArrayList<>();
		for (Parallax_Model layer : page.pageModel.pageList)
		{
			String name = layer.regionName;
			assertNotNull(name, plax.getName() + ": a layer has no region name");
			assertTrue(name.chars().allMatch(c -> c >= 0x20 && c < 0x7F), plax.getName()
				+ ": region name \"" + name + "\" is not plain ASCII, so whether it matches the atlas"
				+ " depends on the JVM's default charset. Rename the region in the atlas AND the .plax.");
			int found = 0;
			for (TextureAtlasData.Region region : data.getRegions())
				if (name.equals(region.name))
					found++;
			if (layer.regionPosition >= found)
				unreachable.add(name + "#" + layer.regionPosition + " (atlas has " + found + ")");
		}
		assertTrue(unreachable.isEmpty(), plax.getName() + ": layers the game cannot find in "
			+ atlasName + " -> " + unreachable);

		// Two copies that must say the same thing: edit only the one beside the .plax and the
		// change never reaches the game, and nothing else would notice. Removing that copy is fine.
		File besidePlax = new File(plax.getParentFile(), atlasName);
		if (besidePlax.isFile())
			assertArrayEquals(Files.readAllBytes(atlas.toPath()), Files.readAllBytes(besidePlax.toPath()),
				atlasName + ": the copy beside " + plax.getName() + " differs from the one at the asset"
				+ " root, which is the one the game loads. Edit them together.");
	}
}

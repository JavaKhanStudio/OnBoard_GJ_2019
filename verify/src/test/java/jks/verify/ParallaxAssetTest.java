package jks.verify;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import jks.tools2d.parallax.pages.WholePage_Model;
import jks.vars.GVars_Serialization;

/**
 * The .plax backdrops are Kryo binaries written in 2019 by parallaxReader0.7.jar, a library
 * whose source no longer exists. Kryo's wire format is not stable between releases, and the
 * failure is silent: the file still "loads", the strings just come back as mojibake, and the
 * first sign of trouble is a missing-file exception several frames later.
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
	@DisplayName("the shipped .plax files still deserialise with the pinned Kryo")
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

		File atlas = new File(plax.getParentFile(), atlasName);
		assertTrue(atlas.isFile(), plax.getName() + ": names an atlas that does not exist -> "
			+ atlasName + " (resolved to " + atlas + ")");
	}
}

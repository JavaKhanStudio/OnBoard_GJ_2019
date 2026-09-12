package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jks.amain.GameConfigs;
import jks.amain.Utils_Config;

/**
 * Settings persistence. The options screen could always change things and never save them,
 * so a chosen resolution or volume lasted exactly as long as the process did.
 */
class ConfigTest
{
	@TempDir Path tempDir;
	private String previousPath;

	@BeforeEach
	void useATemporaryConfig()
	{
		previousPath = System.getProperty("onboard.config");
		System.setProperty("onboard.config", tempDir.resolve("config").toString());
	}

	@AfterEach
	void restore()
	{
		if (previousPath == null) System.clearProperty("onboard.config");
		else System.setProperty("onboard.config", previousPath);
	}

	@Test
	@DisplayName("a missing config is created with usable defaults")
	void missingConfigIsWritten()
	{
		GameConfigs loaded = Utils_Config.load();

		assertTrue(Utils_Config.configFile().isFile(), "no config file was written");
		assertNotNull(loaded);
		// All-zero defaults meant a 0x0 window with the sound off, which is what an
		// uninitialised GameConfigs used to produce on a first run.
		assertTrue(loaded.width > 0 && loaded.height > 0, "default window size must be usable");
		assertTrue(loaded.volume > 0f, "the game should not install itself silent");
		assertEquals(false, loaded.useMipmaps, "Linear without mipmaps is the chosen default");
		// d5: asked whether to keep the 60 cap or offer an uncapped default, Simon answered
		// "60 is more than enough". A fresh install gets 60, and nothing above 60 is offered.
		assertEquals(60, loaded.fps, "60 is the chosen frame rate default (d5)");
	}

	@Test
	@DisplayName("settings survive a round trip")
	void settingsRoundTrip()
	{
		Utils_Config.load();
		Utils_Config.current.width = 1280;
		Utils_Config.current.height = 720;
		Utils_Config.current.isFullScreen = true;
		Utils_Config.current.useVsynch = false;
		Utils_Config.current.volume = 0.35f;
		Utils_Config.current.useMipmaps = true;
		Utils_Config.save();

		GameConfigs reloaded = Utils_Config.load();
		assertEquals(1280, reloaded.width);
		assertEquals(720, reloaded.height);
		assertTrue(reloaded.isFullScreen);
		assertEquals(false, reloaded.useVsynch);
		assertEquals(0.35f, reloaded.volume, 1e-6);
		assertTrue(reloaded.useMipmaps);
	}

	@Test
	@DisplayName("an unreadable config falls back to defaults instead of refusing to start")
	void brokenConfigFallsBack() throws Exception
	{
		File file = Utils_Config.configFile();
		file.getParentFile().mkdirs();
		Files.write(file.toPath(), "this is not json".getBytes(StandardCharsets.UTF_8));

		GameConfigs loaded = Utils_Config.load();

		assertNotNull(loaded, "a corrupt settings file must not stop the game starting");
		assertTrue(loaded.width > 0, "should have fallen back to a usable default");
	}

	@Test
	@DisplayName("fields the game does not know about are ignored, not fatal")
	void unknownFieldsAreIgnored() throws Exception
	{
		File file = Utils_Config.configFile();
		file.getParentFile().mkdirs();
		Files.write(file.toPath(),
			"{\"width\":800,\"height\":600,\"someFutureSetting\":true}".getBytes(StandardCharsets.UTF_8));

		GameConfigs loaded = Utils_Config.load();
		assertEquals(800, loaded.width, "a newer config should still load what it can");
		assertEquals(600, loaded.height);
	}
}

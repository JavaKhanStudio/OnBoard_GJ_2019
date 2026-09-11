package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisCheckBox;

import jks.amain.Main_Application;
import jks.amain.Utils_Config;
import jks.index.Index_Interface;
import jks.vinterface.Block_Resolution;
import jks.vinterface.GVars_UI;

/**
 * How the textures Index_Interface loads are filtered, and the options switch that changes it.
 *
 * They were all loaded with no parameter, which is libGDX's Nearest - right for pixel art,
 * wrong for large hand-drawn art shown small. Linear is the default now; mipmaps are a switch
 * in the Graphics block that applies at once and is saved.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TextureFilterTest
{
	/** Not loaded by the start screen, only queued - it queues level 1 without finishing it. */
	private static final String LOADED_LATER = "game/wagon/wa1/cube.png";

	private static GameHarness harness;
	private static Path config;
	private static String previousConfig;

	private static final List<String> atStart = new ArrayList<>();
	private static final List<String> switchedOn = new ArrayList<>();
	private static final List<String> switchedOff = new ArrayList<>();
	private static volatile String loadedLater;
	private static volatile int glError = -1;
	private static volatile boolean savedOn;
	private static volatile Throwable error;

	@BeforeAll
	void runTheSwitch() throws Exception
	{
		// The checkbox saves the config, and the tests must not write the real one.
		config = Files.createTempFile("onboard-config", "");
		previousConfig = System.getProperty("onboard.config");
		System.setProperty("onboard.config", config.toString());

		Main_Application.startPoint = Main_Application.StartPoint.START_SCREEN;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - texture filter verification");
		gl.useVsync(false);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 30);
		harness.frameHook = frame ->
		{
			if (frame != 10) return;
			try
			{
				describe(atStart);

				Block_Resolution graphics = new Block_Resolution();
				GVars_UI.mainUi.addActor(graphics);
				VisCheckBox mipmaps = graphics.findActor("mipmaps");

				mipmaps.setChecked(true);
				glError = Gdx.gl.glGetError();
				describe(switchedOn);
				savedOn = Utils_Config.load().useMipmaps;

				Index_Interface.loadTexture(LOADED_LATER);
				Index_Interface.manager.finishLoading();
				loadedLater = filterOf(Index_Interface.manager.get(LOADED_LATER, Texture.class));

				mipmaps.setChecked(false);
				describe(switchedOff);
			}
			catch (Throwable t)
			{
				error = t;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	@AfterAll
	void restore() throws Exception
	{
		if (previousConfig == null) System.clearProperty("onboard.config");
		else System.setProperty("onboard.config", previousConfig);
		Files.deleteIfExists(config);
	}

	private static void describe(List<String> into)
	{
		for (Texture texture : Index_Interface.manager.getAll(Texture.class, new Array<Texture>()))
			into.add(Index_Interface.manager.getAssetFileName(texture) + " " + filterOf(texture));
	}

	private static String filterOf(Texture texture)
	{
		return texture.getMinFilter() + "/" + texture.getMagFilter();
	}

	@Test
	@DisplayName("the start screen and the switch run without throwing")
	void runsCleanly()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "flipping the mipmaps switch threw: " + error);
	}

	@Test
	@DisplayName("every UI texture starts out Linear, not libGDX's Nearest")
	void linearByDefault()
	{
		assertTrue(atStart.size() >= 20, "expected the start screen to have loaded the UI, got " + atStart);
		for (String texture : atStart)
			assertTrue(texture.endsWith(" Linear/Linear"), "not Linear: " + texture);
	}

	@Test
	@DisplayName("the mipmaps switch reaches every loaded texture, and back")
	void switchAppliesAtOnce()
	{
		assertEquals(GL20.GL_NO_ERROR, glError, "building the mip levels raised a GL error");
		assertEquals(atStart.size(), switchedOn.size());
		for (String texture : switchedOn)
			assertTrue(texture.endsWith(" MipMapLinearLinear/Linear"), "not mipmapped: " + texture);
		for (String texture : switchedOff)
			assertTrue(texture.endsWith(" Linear/Linear"), "not back to Linear: " + texture);
	}

	@Test
	@DisplayName("a texture queued before the switch but loaded after it gets mipmaps too")
	void laterLoadsFollowTheSwitch()
	{
		assertEquals("MipMapLinearLinear/Linear", loadedLater);
	}

	@Test
	@DisplayName("the switch is saved to the config file")
	void switchIsSaved()
	{
		assertTrue(savedOn, "ticking mipmaps did not reach the config file");
	}
}

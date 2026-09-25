package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.utils.Array;

import jks.amain.Main_Application;
import jks.vars.GVars_Heart;
import jks.vinterface.Block_Resolution;
import jks.vinterface.GVars_UI;
import jks.vinterface.overlay.OverlayOptions;

/**
 * A resolution change, the way a player makes one (r117): Options, 960x540 applied through the
 * options' own resolution block, then Escape back to the menu. The stage must be the new window,
 * not the old one stretched - every screen lays itself out from Gdx.graphics - and the options
 * board and the menu must be laid out for it. Frames: menu-resize-*.png, for a person to look at.
 *
 * Like WagonResizeRenderTest it is SKIPPED under cage, which will not resize a window; run it
 * through tools/offscreen_resizable.sh.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MenuResizeRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");
	private static final String TO = "960x540";

	private static GameHarness harness;
	private static Path config;
	private static String previousConfig;
	private static volatile String stuck, stage;
	private static volatile float boardRight, boardTop, menuX;
	private static volatile boolean optionsOpened, finished;
	private static volatile Throwable error;

	@BeforeAll
	void changeTheResolution() throws Exception
	{
		// Applying a resolution saves it; keep the real config out of it.
		config = Files.createTempFile("onboard-config", "");
		previousConfig = System.getProperty("onboard.config");
		System.setProperty("onboard.config", config.toString());

		Main_Application.startPoint = Main_Application.StartPoint.START_SCREEN;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - resolution change verification");
		gl.useVsync(false);
		gl.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);

		int[] step = {0};
		double[] at = {4.0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 25;
		harness.frameHook = frame ->
		{
			if (error != null || finished || harness.gameSeconds < at[0]) return;
			try
			{
				switch (step[0])
				{
					case 0:
						Frames.write(GameHarness.grab(), new File(OUTPUT, "menu-resize-1-menu-1280x720.png"));
						press(Keys.DOWN); press(Keys.DOWN); press(Keys.ENTER);
						optionsOpened = GVars_Heart.vue.overlay instanceof OverlayOptions;
						break;
					case 1:
						Frames.write(GameHarness.grab(), new File(OUTPUT, "menu-resize-2-options-1280x720.png"));
						Block_Resolution block = (Block_Resolution) field(GVars_Heart.vue.overlay, "graphicBloc");
						@SuppressWarnings("unchecked")
						SelectBox<String> box = (SelectBox<String>) field(block, "selectBox_Resolution");
						if (!box.getItems().contains(TO, false))
						{
							Array<String> items = new Array<>(box.getItems());
							items.add(TO);
							box.setItems(items);
						}
						box.setSelected(TO);
						block.applyNewResolution();
						at[0] = harness.gameSeconds + 3;   // the deadline for the window to change
						step[0]++;
						return;
					case 2:
						if (Gdx.graphics.getBackBufferWidth() != 960)
						{
							if (harness.gameSeconds < at[0]) return;
							stuck = TO + " was applied, the window stayed "
								+ Gdx.graphics.getBackBufferWidth() + "x" + Gdx.graphics.getBackBufferHeight();
							finished = true;
							Gdx.app.exit();
							return;
						}
						step[0]++;
						at[0] = harness.gameSeconds + 1.0;
						return;
					case 3:
						Frames.write(GameHarness.grab(), new File(OUTPUT, "menu-resize-3-options-960x540.png"));
						Actor board = (Actor) field(GVars_Heart.vue.overlay, "graphicBloc");
						boardRight = board.getX() + board.getWidth();
						boardTop = board.getY() + board.getHeight();
						stage = Math.round(GVars_UI.mainUi.getWidth()) + "x" + Math.round(GVars_UI.mainUi.getHeight());
						press(Keys.ESCAPE);
						at[0] = harness.gameSeconds + 3.0;   // the menu slides back in
						step[0]++;
						return;
					case 4:
						Frames.write(GameHarness.grab(), new File(OUTPUT, "menu-resize-4-menu-960x540.png"));
						Actor jouer = GVars_UI.mainUi.getRoot().findActor("menu.play");
						menuX = jouer == null ? Float.NaN : jouer.getX();
						finished = true;
						Gdx.app.exit();
						return;
				}
				step[0]++;
				at[0] = harness.gameSeconds + 1.5;
			}
			catch (Throwable t)
			{
				if (error == null) error = t;
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

	private static Object field(Object owner, String name) throws Exception
	{
		for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass())
			try
			{
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f.get(owner);
			}
			catch (NoSuchFieldException next) {}
		throw new NoSuchFieldException(name);
	}

	private static void press(int key)
	{
		Gdx.input.getInputProcessor().keyDown(key);
		Gdx.input.getInputProcessor().keyUp(key);
	}

	@Test
	@DisplayName("after a resolution change in the options, the stage is the new window and the options board fits it")
	void optionsFollowTheWindow()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertTrue(optionsOpened, "Options did not open");
		assumeTrue(stuck == null, "the display would not resize the window (" + stuck + "): cage "
			+ "does that. Run this test through tools/offscreen_resizable.sh.");
		System.out.println("stage " + stage + ", options board to " + boardRight + "," + boardTop);
		assertEquals(TO, stage, "the stage is the old window stretched, not the new one");
		assertTrue(boardRight <= 960 && boardTop <= 540, "the options board runs off the 960x540 window: "
			+ boardRight + "," + boardTop);
	}
}

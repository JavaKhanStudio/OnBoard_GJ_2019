package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;
import jks.vars.GVars_Heart;
import jks.vue.AVue_Model;
import jks.vue.models.Vue_Scenematic_Intro;
import jks.vue.models.Vue_Scenematic_Outro;

/**
 * The story pages' fade on a fixed clock (r196): the intro, then the outro, each driven by its
 * own update() in 1/60 s steps from alpha 0 rather than the window's deltas - two seconds up, a click, half a
 * second down. Every step's alpha goes to build/frames/fade-trace.txt, as float bits, and the
 * frame halfway down to fade-intro-mid.png and fade-outro-mid.png. Run it before and after a
 * change to the fade: the trace must not differ by a bit.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CinematicFadeProbeTest
{
	private static final float STEP = 1 / 60f;
	private static final int STEPS_UP = 150, STEPS_DOWN = 30;

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;
	private static final List<String> trace = new ArrayList<>();
	private static final float[] midAlpha = {Float.NaN, Float.NaN};
	private static volatile int done;

	@BeforeAll
	void fadeBoth()
	{
		Main_Application.startPoint = Main_Application.StartPoint.LOGO;

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1280, 720);
		config.setTitle("On Board - cinematic fade probe");
		config.setResizable(false);
		config.useVsync(false);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 60;
		harness.frameHook = frame ->
		{
			if (harness.gameSeconds < 0.5) return;
			try
			{
				if (done == 0 && GVars_Heart.vue instanceof Vue_Scenematic_Intro)
				{
					probe("intro", GVars_Heart.vue, 0);
					done = 1;
					GVars_Heart.changeVue(new Vue_Scenematic_Outro(), true);
				}
				else if (done == 1 && GVars_Heart.vue instanceof Vue_Scenematic_Outro)
				{
					probe("outro", GVars_Heart.vue, 1);
					done = 2;
					Gdx.app.exit();
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		};
		new Lwjgl3Application(harness, config);
	}

	private static void probe(String name, AVue_Model view, int slot) throws Exception
	{
		// From a page just shown: the frames before the probe ran on the window's clock.
		field(view).setFloat(view, 0f);
		GVars_Heart.inCinematic_Click = false;
		for (int i = 0; i < STEPS_UP; i++) step(name, "up", view, i);
		GVars_Heart.inCinematic_Click = true;
		for (int i = 0; i <= STEPS_DOWN; i++) step(name, "down", view, i);

		midAlpha[slot] = alpha(view);
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		view.render();
		Frames.write(GameHarness.grab(), new File(OUTPUT, "fade-" + name + "-mid.png"));
	}

	private static void step(String name, String phase, AVue_Model view, int i) throws Exception
	{
		view.update(STEP);
		trace.add(String.format("%s %s %3d %08x", name, phase, i, Float.floatToIntBits(alpha(view))));
	}

	private static float alpha(AVue_Model view) throws Exception
	{return field(view).getFloat(view);}

	private static Field field(AVue_Model view) throws Exception
	{
		for (Class<?> c = view.getClass(); c != null; c = c.getSuperclass())
		{
			try
			{
				Field f = c.getDeclaredField("currentAlpha");
				f.setAccessible(true);
				return f;
			}
			catch (NoSuchFieldException e) { /* up a class */ }
		}
		throw new NoSuchFieldException("currentAlpha");
	}

	@Test
	@DisplayName("both cinematics fade on a fixed clock, traced and shot halfway down")
	void traced() throws Exception
	{
		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error);
		assertEquals(2, done, "did not reach both cinematics");
		Files.write(new File(OUTPUT, "fade-trace.txt").toPath(), trace, StandardCharsets.UTF_8);
		for (float a : midAlpha)
			assertTrue(a > 0.4f && a < 0.6f, "halfway down should be about 0.5, was " + a);
	}
}

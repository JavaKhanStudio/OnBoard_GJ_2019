package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;

import jks.amain.GameConfigs;
import jks.amain.Main_Application;
import jks.amain.Utils_Config;
import jks.input.GVars_Inputs;
import jks.vars.GVars_Heart;
import jks.vue.GVars_Steam;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * The clips for the itch.io page (r283): the real game on a fixed 1/30 s clock, a scene played
 * by script, every frame piped to ffmpeg. Not a check - a thing to show. Like WalkClipTest it
 * runs only when asked for, caged:
 *
 *   ./gradlew :verify:test -Pclip --tests '*ItchClipTest' -PclipScene=carriage
 *
 * and writes verify/build/clips/itch-<scene>.mp4, or itch-<scene>-<lang>.mp4 with -PclipLang=en.
 * The game runs on default settings in that language, saving to a scratch config (TempConfig),
 * so desktop/config is neither read nor written. tools/itch_gifs.sh records each scene and turns them into GIFs.
 * The scenes:
 *   intro     the story pages, clicked on every 3.5 s, into the steam and the start screen
 *   carriage  Ross walks in carriage 1, picks up the bundle, the key is made whole, and the
 *             steam carries him into carriage 2 (the key's other two pieces are given, not
 *             played: the clip does not show how carriage 1 is solved)
 *   walk<N>   Ross walks right through carriage N, his first thought up
 */
@Tag("clip")
class ItchClipTest
{
	private static final int WIDTH = 1280, HEIGHT = 720, FPS = 30;
	private static final float TICK = 1f / FPS;

	@Test
	@DisplayName("records a scene of the game for the itch.io page")
	void record() throws Exception
	{
		String scene = System.getProperty("onboard.clipScene", "carriage");
		String language = System.getProperty("onboard.clipLang", "fr");
		File dir = new File(new File(System.getProperty("onboard.verify")), "build/clips");
		dir.mkdirs();
		File out = new File(dir, "itch-" + scene + (language.equals("fr") ? "" : "-" + language) + ".mp4");
		// The launcher loads the config, and a test has none: the language is set as a flag sets it.
		TempConfig config = TempConfig.use();
		Utils_Config.current = new GameConfigs();
		Utils_Config.current.language = language;

		Script script;
		if (scene.equals("intro"))
		{
			Main_Application.startPoint = Main_Application.StartPoint.INTRO;
			script = new Intro();
		}
		else if (scene.equals("carriage"))
		{
			Main_Application.startPoint = Main_Application.StartPoint.GAME;
			Main_Application.startLevel = 1;
			script = new Carriage();
		}
		else if (scene.startsWith("walk"))
		{
			Main_Application.startPoint = Main_Application.StartPoint.GAME;
			Main_Application.startLevel = Integer.parseInt(scene.substring(4));
			script = new Walk();
		}
		else throw new IllegalArgumentException("no scene " + scene + ": intro, carriage, walk1-4");

		Process ffmpeg = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error",
			"-f", "rawvideo", "-pix_fmt", "rgba", "-s", WIDTH + "x" + HEIGHT, "-r", "" + FPS, "-i", "-",
			"-vf", "vflip", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "18", out.getPath())
			.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
		OutputStream pipe = ffmpeg.getOutputStream();

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(WIDTH, HEIGHT);
		gl.setResizable(false);
		gl.setTitle("On Board - itch clip");
		gl.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);
		gl.useVsync(false);
		gl.setForegroundFPS(FPS);

		Recorder recorder = new Recorder(new Main_Application(), pipe, script);
		try
		{
			new Lwjgl3Application(recorder, gl);
		}
		finally
		{
			GVars_Inputs.rightPressed = false;
			pipe.close();
			config.restore();
		}
		assertEquals(0, ffmpeg.waitFor(), "ffmpeg failed on " + out);
		if (recorder.error != null) recorder.error.printStackTrace();
		assertNull(recorder.error, "the game threw: " + recorder.error);
		System.out.println(String.format("%s: %.1f s", out, recorder.seconds));
	}

	/** What happens at each tick of a scene; false once the scene is over. */
	private interface Script
	{
		boolean at(double seconds);
	}

	private static class Intro implements Script
	{
		private double lastTurn;
		private double steamOver = -1;

		@Override
		public boolean at(double t)
		{
			boolean onIntro = GVars_Heart.vue != null && GVars_Heart.vue.getClass().getSimpleName().equals("Vue_Scenematic_Intro");
			if (onIntro && !GVars_Steam.isRunning() && t - lastTurn > 3.5)
			{
				lastTurn = t;
				GVars_Heart.inCinematic_Click = true;
			}
			// A moment on the start screen once the steam has lifted off it.
			if (!onIntro && !GVars_Steam.isRunning() && steamOver < 0) steamOver = t;
			return (steamOver < 0 || t < steamOver + 1.5) && t < 60;
		}
	}

	private static class Carriage implements Script
	{
		private static final double WALK_FROM = 2.5, WALK_TO = 5.5, PICK_AT = 6.0, WHOLE_AT = 8.0;
		private int step;
		private double lifted = -1;

		@Override
		public boolean at(double t)
		{
			GVars_Inputs.rightPressed = (t >= WALK_FROM && t < WALK_TO);
			if (step == 0 && t >= PICK_AT)
			{
				step = 1;
				click(item("baluchon.png"));
			}
			else if (step == 1 && t >= WHOLE_AT)
			{
				step = 2;
				GVars_Game.addKey(2);
				GVars_Game.addKey(3);
			}
			else if (step == 2 && GVars_Game.currentLevelInt == 2 && !GVars_Steam.isRunning())
			{
				step = 3;
				lifted = t;
			}
			// In carriage 2, Ross walks on a little once the steam has gone.
			if (step == 3) GVars_Inputs.rightPressed = t >= lifted + 1.0 && t < lifted + 3.5;
			return (step < 3 || t < lifted + 4.0) && t < 60;
		}
	}

	private static class Walk implements Script
	{
		@Override
		public boolean at(double t)
		{
			GVars_Inputs.rightPressed = t >= 3.5 && t < 8.5;
			return t < 9.0;
		}
	}

	private static GameItem item(String name)
	{
		for (GameItem item : GVars_Game.currentLevel.listItems)
			if (name.equals(item.name)) return item;
		throw new AssertionError(name + " is not in carriage " + GVars_Game.currentLevelInt);
	}

	/** The game's own click on an item, with whatever is in hand. */
	private static void click(GameItem item)
	{
		item.tryTouch(new Vector3(item.posX + 2, item.posY + 2, 0), GVars_Game.selectedItem, false);
	}

	/** Runs the game on a fixed clock, plays the script, and pipes each frame. */
	private static class Recorder implements ApplicationListener
	{
		private final ApplicationListener game;
		private final OutputStream pipe;
		private final Script script;
		private final ByteBuffer pixels = BufferUtils.newByteBuffer(WIDTH * HEIGHT * 4);
		private final byte[] bytes = new byte[WIDTH * HEIGHT * 4];
		private Graphics steady;
		double seconds;
		volatile Throwable error;

		Recorder(ApplicationListener game, OutputStream pipe, Script script)
		{
			this.game = game;
			this.pipe = pipe;
			this.script = script;
		}

		@Override
		public void create()
		{
			game.create();
		}

		@Override
		public void render()
		{
			if (error != null) return;
			try
			{
				Graphics real = Gdx.graphics;
				if (steady == null) steady = (Graphics) Proxy.newProxyInstance(Graphics.class.getClassLoader(),
					new Class<?>[] {Graphics.class}, (proxy, method, args) ->
						method.getName().equals("getDeltaTime") ? TICK : method.invoke(real, args));
				Gdx.graphics = steady;
				try
				{
					game.render();
				}
				finally
				{
					Gdx.graphics = real;
				}
				seconds += TICK;

				pixels.clear();
				Gdx.gl.glReadPixels(0, 0, WIDTH, HEIGHT, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);
				pixels.get(bytes);
				pipe.write(bytes);

				if (!script.at(seconds)) Gdx.app.exit();
			}
			catch (IOException | RuntimeException | AssertionError e)
			{
				error = e;
				Gdx.app.exit();
			}
		}

		@Override public void resize(int width, int height) { game.resize(width, height); }
		@Override public void pause()  { game.pause(); }
		@Override public void resume() { game.resume(); }
		@Override public void dispose(){ try { game.dispose(); } catch (Throwable ignored) {} }
	}
}

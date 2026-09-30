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
import com.badlogic.gdx.utils.BufferUtils;

import jks.amain.Main_Application;
import jks.input.GVars_Inputs;
import jks.personnage.model.SIW_Data;
import jks.vue.models.game.GVars_Game;

/**
 * A clip of Ross walking across a carriage, the way it plays (r177): the real game, the right
 * arrow held, every frame piped to ffmpeg. Not a check - a thing to watch, before and after a
 * change to his walk. It runs only when asked for, caged like the GL tests:
 *
 *   ./gradlew :verify:test -Pclip --tests '*WalkClipTest' -PclipLevel=3
 *
 * and writes verify/build/clips/walk-wa3-0.075s.mp4, named after SIW_Data.WALK_FRAME_SECONDS.
 * The game's clock is held to 1/60 s a frame, so the clip plays at the speed the game does
 * however long the framebuffer read takes.
 */
@Tag("clip")
class WalkClipTest
{
	private static final int WIDTH = 1280, HEIGHT = 720, FPS = 60;
	private static final float TICK = 1f / FPS;
	private static final double STAND = 0.8, WALK = 4.0, AFTER = 0.6;

	@Test
	@DisplayName("records Ross walking right across a carriage")
	void record() throws Exception
	{
		int level = Integer.parseInt(System.getProperty("onboard.clipLevel", "1"));
		File dir = new File(new File(System.getProperty("onboard.verify")), "build/clips");
		dir.mkdirs();
		File out = new File(dir, String.format("walk-wa%d-%.3fs.mp4", level, SIW_Data.WALK_FRAME_SECONDS));

		Process ffmpeg = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error",
			"-f", "rawvideo", "-pix_fmt", "rgba", "-s", WIDTH + "x" + HEIGHT, "-r", "" + FPS, "-i", "-",
			"-vf", "vflip", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "20", out.getPath())
			.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
		OutputStream pipe = ffmpeg.getOutputStream();

		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = level;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(WIDTH, HEIGHT);
		gl.setResizable(false);
		gl.setTitle("On Board - walk clip");
		gl.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);
		gl.useVsync(false);
		gl.setForegroundFPS(FPS);

		Recorder recorder = new Recorder(new Main_Application(), pipe);
		try
		{
			new Lwjgl3Application(recorder, gl);
		}
		finally
		{
			GVars_Inputs.rightPressed = false;
			pipe.close();
		}
		assertEquals(0, ffmpeg.waitFor(), "ffmpeg failed on " + out);
		if (recorder.error != null) recorder.error.printStackTrace();
		assertNull(recorder.error, "the game threw: " + recorder.error);
		System.out.println(String.format("%s: walked %.0f world px in %.1f s (%.0f px/s)", out,
			recorder.walkedTo - recorder.walkedFrom, WALK, (recorder.walkedTo - recorder.walkedFrom) / WALK));
	}

	/** Runs the game on a fixed clock with the right arrow held for WALK s, and pipes each frame. */
	private static class Recorder implements ApplicationListener
	{
		private final ApplicationListener game;
		private final OutputStream pipe;
		private final ByteBuffer pixels = BufferUtils.newByteBuffer(WIDTH * HEIGHT * 4);
		private final byte[] bytes = new byte[WIDTH * HEIGHT * 4];
		private Graphics steady;
		private double seconds;
		volatile Throwable error;
		float walkedFrom, walkedTo;

		Recorder(ApplicationListener game, OutputStream pipe)
		{
			this.game = game;
			this.pipe = pipe;
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
				// Everything the game times - his position, his legs' frame - reads this.
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

				boolean walking = seconds >= STAND && seconds < STAND + WALK;
				if (walking && !GVars_Inputs.rightPressed) walkedFrom = GVars_Game.ross.position.x;
				if (!walking && GVars_Inputs.rightPressed) walkedTo = GVars_Game.ross.position.x;
				GVars_Inputs.rightPressed = walking;

				pixels.clear();
				Gdx.gl.glReadPixels(0, 0, WIDTH, HEIGHT, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);
				pixels.get(bytes);
				pipe.write(bytes);

				if (seconds >= STAND + WALK + AFTER) Gdx.app.exit();
			}
			catch (IOException | RuntimeException e)
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

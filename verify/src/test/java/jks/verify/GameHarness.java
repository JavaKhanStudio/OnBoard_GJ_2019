package jks.verify;

import java.awt.image.BufferedImage;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Wraps the real ApplicationListener so a test can drive the actual game loop: run it for a
 * fixed number of frames, grab one of them, then shut down cleanly - recording anything the
 * game threw instead of letting it kill the JVM.
 */
public class GameHarness implements ApplicationListener
{
	private final ApplicationListener delegate;
	private final int captureFrame;
	private final int exitFrame;

	private int frame;

	/** The game's own clock: seconds of simulated time, not frames or wall time. */
	public volatile double gameSeconds;

	/**
	 * When set, capture once this much of the game's own clock has passed rather than at a
	 * frame index. Anything driven by a timer - a fade, a typing effect - is at a different
	 * point at frame N depending on how fast the machine drew those N frames, which makes a
	 * frame-indexed golden of a fade flap between runs.
	 */
	public double captureAfterSeconds = -1;
	public double exitAfterSeconds = -1;

	/** Whatever the game threw, on whichever lifecycle method. Null means a clean run. */
	public volatile Throwable error;
	/** The captured frame, already flipped the right way up. */
	public volatile BufferedImage capture;
	public volatile int framesRendered;

	/** Called after each rendered frame, so a test can drive the running game. */
	public java.util.function.IntConsumer frameHook;

	public GameHarness(ApplicationListener delegate, int captureFrame, int exitFrame)
	{
		this.delegate = delegate;
		this.captureFrame = captureFrame;
		this.exitFrame = exitFrame;
	}

	@Override
	public void create()
	{
		try
		{
			delegate.create();
		}
		catch (Throwable t)
		{
			fail(t);
		}
	}

	@Override
	public void render()
	{
		if (error != null) return;

		try
		{
			delegate.render();
			frame++;
			framesRendered = frame;

			// The same clamp Main_Application applies, so this clock matches the one the
			// game's own animations advance on.
			gameSeconds += Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f);

			if (frameHook != null) frameHook.accept(frame);

			if (captureAfterSeconds > 0)
			{
				if (capture == null && gameSeconds >= captureAfterSeconds) capture = grab();
			}
			else if (frame == captureFrame) capture = grab();

			boolean done = exitAfterSeconds > 0
				? gameSeconds >= exitAfterSeconds
				: frame >= exitFrame;
			if (done && capture != null) Gdx.app.exit();
			else if (frame >= exitFrame) Gdx.app.exit();
		}
		catch (Throwable t)
		{
			fail(t);
		}
	}

	private void fail(Throwable t)
	{
		if (error == null) error = t;
		Gdx.app.exit();
	}

	/** Reads the framebuffer and flips it: GL's origin is bottom-left, an image's is top-left. */
	static BufferedImage grab()
	{
		int w = Gdx.graphics.getBackBufferWidth();
		int h = Gdx.graphics.getBackBufferHeight();

		Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, w, h);
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < h; y++)
		{
			for (int x = 0; x < w; x++)
			{
				int rgba = pixmap.getPixel(x, y);
				int rgb = ((rgba >>> 24) & 0xFF) << 16 | ((rgba >>> 16) & 0xFF) << 8 | ((rgba >>> 8) & 0xFF);
				image.setRGB(x, h - 1 - y, rgb);
			}
		}
		pixmap.dispose();
		return image;
	}

	@Override public void resize(int width, int height) { delegate.resize(width, height); }
	@Override public void pause()  { delegate.pause(); }
	@Override public void resume() { delegate.resume(); }
	@Override public void dispose(){ try { delegate.dispose(); } catch (Throwable ignored) {} }
}

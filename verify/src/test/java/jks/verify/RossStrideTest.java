package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import jks.camera.GVars_Camera;
import jks.personnage.model.AnimationModel;
import jks.personnage.model.Enum_AGE;
import jks.personnage.model.SIW_Data;
import jks.personnage.model.SpriteModel;
import jks.vars.GVars_Heart;

/**
 * Do Ross's feet stay on the floor as he walks (r114)? Walks him right at full speed through
 * SpriteModel's own update and draw, one game frame at a time at the 60 fps cap, on a still
 * camera the size of the carriage's world, and finds his boot on the floor in each frame.
 *
 * A boot that is down should stay where it landed. What it does instead is the slide: printed
 * per age, and drawn to build/frames/ross-stride-*.png with a ruler every 50 world px. Until
 * r114 it was 200-300 px/s, his speed twice what his legs walked; Enum_AGE.stepLength is what
 * this measures, and his speed is derived from it.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RossStrideTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");
	/** Two walk cycles of 'move', in 60 fps game frames. */
	private static final int GAME_FRAMES = (int) Math.ceil(2 * 8 * SIW_Data.WALK_FRAME_SECONDS * 60);
	/**
	 * World px a second a planted boot may slide. Reading a heel off the screen is good to a few
	 * px, which is some 20 px/s over one step; skating was 200-300 px/s until r114.
	 */
	private static final double MAX_SLIDE = 40;
	private static final float START_X = 150;
	/** Rows of the screen kept, counted up from the bottom: his boots and shins. */
	private static final int FEET_ROWS = 200;

	private static final List<List<BufferedImage>> shots = new ArrayList<>();
	private static final List<List<float[]>> states = new ArrayList<>();
	private static volatile Throwable error;

	@BeforeAll
	void walk()
	{
		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode((int) GVars_Camera.WORLD_WIDTH, (int) GVars_Camera.WORLD_HEIGHT);
		gl.setTitle("On Board - Ross stride verification");
		gl.useVsync(false);

		new Lwjgl3Application(new ApplicationAdapter()
		{
			SpriteBatch batch;
			OrthographicCamera camera;
			int age, frame;
			SpriteModel ross;

			@Override
			public void create()
			{
				batch = new SpriteBatch();
				camera = new OrthographicCamera();
				camera.setToOrtho(false, GVars_Camera.WORLD_WIDTH, GVars_Camera.WORLD_HEIGHT);
				camera.update();
				// Paused, draw() leaves the animation clock where the test puts it.
				GVars_Heart.isPaused = true;
			}

			@Override
			public void render()
			{
				try
				{
					if (ross == null)
					{
						if (age == Enum_AGE.values().length) { Gdx.app.exit(); return; }
						ross = new SpriteModel(SIW_Data.getRoss(Enum_AGE.values()[age]));
						ross.position.set(START_X, 20);
						shots.add(new ArrayList<>());
						states.add(new ArrayList<>());
						frame = 0;
					}
					// Full speed from the first frame, clamped by update: what the input path reaches
					// after a tenth of a second.
					ross.velocity.x = Float.MAX_VALUE;
					ross.update(1 / 60f);
					setClock(ross, frame / 60f);
					Gdx.gl.glClearColor(1, 0, 1, 1);
					Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
					batch.setProjectionMatrix(camera.combined);
					batch.begin();
					ross.draw(batch);
					batch.end();
					shots.get(age).add(feet(GameHarness.grab()));
					states.get(age).add(new float[] {ross.position.x, ross.currentState.getKeyFrameIndex(frame / 60f)});
					if (++frame == GAME_FRAMES) { ross = null; age++; }
				}
				catch (Throwable t)
				{
					error = t;
					Gdx.app.exit();
				}
			}
		}, gl);
		GVars_Heart.isPaused = false;
	}

	/** Only the bottom of the screen is kept: two cycles of four ages in full do not fit the heap. */
	private static BufferedImage feet(BufferedImage shot)
	{
		BufferedImage out = new BufferedImage(shot.getWidth(), FEET_ROWS, BufferedImage.TYPE_INT_RGB);
		out.getGraphics().drawImage(shot.getSubimage(0, shot.getHeight() - FEET_ROWS, shot.getWidth(), FEET_ROWS), 0, 0, null);
		return out;
	}

	private static void setClock(SpriteModel ross, float seconds)
	{
		try
		{
			Field clock = AnimationModel.class.getDeclaredField("stateTime");
			clock.setAccessible(true);
			clock.setFloat(ross, seconds);
		}
		catch (ReflectiveOperationException e)
		{
			throw new IllegalStateException(e);
		}
	}

	@Test
	@DisplayName("a boot on the floor stays where it landed as he walks")
	void measureTheSlide() throws Exception
	{
		if (error != null) error.printStackTrace();
		assertNull(error, "walking Ross threw: " + error);
		StringBuilder report = new StringBuilder();
		List<String> slides = new ArrayList<>();
		for (int a = 0; a < shots.size(); a++)
		{
			Enum_AGE age = Enum_AGE.values()[a];
			List<BufferedImage> walk = shots.get(a);
			// The first grab can come before the window reaches its final size: only full-size ones count.
			int width = walk.get(walk.size() - 1).getWidth();
			int ground = groundRow(walk);
			report.append(age.path).append(" - each key frame as it comes up: Ross x (world), and his boots on the floor x..x (screen)\n");
			double steps = 0;
			int strikes = 0;
			int lastKey = -1;
			for (int f = 0; f < walk.size(); f++)
			{
				int key = (int) states.get(a).get(f)[1];
				if (key == lastKey || walk.get(f).getWidth() != width) continue;
				lastKey = key;
				List<int[]> boots = boots(walk.get(f), ground);
				StringBuilder b = new StringBuilder();
				for (int[] boot : boots) b.append(' ').append(boot[0]).append("..").append(boot[1]);
				report.append(String.format("  #%d  %6.1f %s%n", key + 1, states.get(a).get(f)[0], b));
				// Heel strike (SIW_Data.stepFrames): both boots down, the step is heel to heel.
				if ((key == 3 || key == 7) && boots.size() == 2)
				{
					steps += boots.get(1)[0] - boots.get(0)[0];
					strikes++;
				}
			}
			// The window may be given less than the world's 1600 px (cage's display is 1280): back to world px.
			double toWorld = GVars_Camera.WORLD_WIDTH / width;
			double step = toWorld * steps / Math.max(1, strikes);
			double feet = step / (SIW_Data.FRAMES_PER_STEP * SIW_Data.WALK_FRAME_SECONDS);
			double body = age.walkSpeed();
			report.append(String.format("  => a step is %.0f world px; four key frames a step, the feet carry him %.0f px/s;"
				+ " he moves %.0f px/s at 60 fps: a planted boot slides %.0f px/s (%.1f px a frame)%n",
				step, feet, body, body - feet, (body - feet) / 60));
			if (strikes == 0) slides.add(age.path + " has no heel strike with both boots down");
			else if (Math.abs(body - feet) > MAX_SLIDE) slides.add(age.path + String.format(" slides %.0f px/s", body - feet));
			Frames.write(strip(walk, ground), new File(OUTPUT, "ross-stride-" + age.path + ".png"));
		}
		System.out.print(report);
		assertEquals(Enum_AGE.values().length, shots.size(), report.toString());
		assertTrue(slides.isEmpty(), "Ross skates: " + slides + "\n" + report + "See " + OUTPUT + "/ross-stride-*.png");
	}

	/** The floor: the row his soles reach in most frames. */
	private static int groundRow(List<BufferedImage> walk)
	{
		List<Integer> lows = new ArrayList<>();
		for (BufferedImage s : walk) lows.add(lowestRow(s));
		lows.sort(null);
		return lows.get(lows.size() / 2);
	}

	/** Each run of him within 10 px of the floor, left to right: a boot on the floor, or two touching. */
	private static List<int[]> boots(BufferedImage shot, int ground)
	{
		boolean[] down = new boolean[shot.getWidth()];
		for (int y = Math.max(0, ground - 10); y <= ground; y++)
			for (int x = 0; x < shot.getWidth(); x++)
				if (isFigure(shot.getRGB(x, y))) down[x] = true;
		List<int[]> runs = new ArrayList<>();
		for (int x = 0; x < down.length; x++)
		{
			if (!down[x]) continue;
			int end = x;
			// Gaps under 6 px are the outline breaking up, not two boots.
			while (end + 1 < down.length && (down[end + 1] || (end + 6 < down.length && anyDown(down, end + 1, end + 6)))) end++;
			if (end - x > 10) runs.add(new int[] {x, end});
			x = end;
		}
		return runs;
	}

	private static boolean anyDown(boolean[] down, int from, int to)
	{
		for (int x = from; x <= to; x++) if (down[x]) return true;
		return false;
	}

	private static int lowestRow(BufferedImage shot)
	{
		for (int y = shot.getHeight() - 1; y >= 0; y--)
			for (int x = 0; x < shot.getWidth(); x++)
				if (isFigure(shot.getRGB(x, y))) return y;
		return -1;
	}

	private static boolean isFigure(int rgb)
	{
		int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
		return Math.abs(r - 0xFF) + g + Math.abs(b - 0xFF) > 200;
	}

	/** His feet, every third game frame, stacked, over a ruler every 50 world px. */
	private static BufferedImage strip(List<BufferedImage> walk, int ground)
	{
		int top = Math.max(0, ground - 140), h = Math.min(160, FEET_ROWS - top), every = 3, x1 = (int) START_X + 900;
		int rows = (walk.size() + every - 1) / every;
		BufferedImage out = new BufferedImage(x1, rows * h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		for (int r = 0; r < rows; r++)
		{
			g.drawImage(walk.get(r * every).getSubimage(0, top, x1, h), 0, r * h, null);
			g.setColor(Color.BLACK);
			float tick = 50f * walk.get(walk.size() - 1).getWidth() / GVars_Camera.WORLD_WIDTH;
			for (float x = 0; x < x1; x += tick) g.drawLine((int) x, r * h + h - 12, (int) x, r * h + h);
			g.drawLine(0, r * h + 140, x1, r * h + 140);
			g.drawString("game frame " + r * every, 4, r * h + 14);
		}
		g.dispose();
		return out;
	}
}

package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import jks.personnage.model.AnimationModel;
import jks.personnage.model.Enum_AGE;
import jks.personnage.model.Enum_AnimState;
import jks.personnage.model.SIW_Data;
import jks.personnage.model.SpriteModel;
import jks.vars.GVars_Heart;

/**
 * Ross's walk stays on his body (r111). The packer trimmed each frame of 'move' to its own box
 * and wrote where the box sat on the artist's canvas; until r111 SpriteModel drew every box from
 * the same corner, so his head and torso jumped some 40 px side to side on every frame while
 * his legs walked.
 *
 * Draws each walk frame of every age through SpriteModel, both facings, on a magenta screen,
 * and measures the centre of his head. The artist's own canvas moves it at most 10 atlas px.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RossWalkRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");
	private static final int KEY = 0xFF00FF;
	/** Screen px the head may wander across a cycle: the widest authored sway (10 atlas px x 0.75) plus filtering. */
	private static final int MAX_HEAD_SWAY = 12;
	private static final int FRAMES = 8;

	/** age + facing -> the grab of each walk frame, then the idle one. */
	private static final Map<String, List<BufferedImage>> grabs = new LinkedHashMap<>();
	private static volatile Throwable error;

	@BeforeAll
	void drawEveryFrame()
	{
		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - Ross walk verification");
		gl.useVsync(false);

		new Lwjgl3Application(new ApplicationAdapter()
		{
			SpriteBatch batch;
			final List<Runnable> steps = new ArrayList<>();

			@Override
			public void create()
			{
				batch = new SpriteBatch();
				// Paused, draw() leaves the animation clock where the test puts it.
				GVars_Heart.isPaused = true;
				for (Enum_AGE age : Enum_AGE.values())
				{
					SpriteModel ross = new SpriteModel(SIW_Data.getRoss(age));
					ross.position.set(400, 20);
					for (boolean facing : new boolean[] {true, false})
					{
						String key = age.path + (facing ? "-facing-right" : "-facing-left");
						grabs.put(key, new ArrayList<>());
						for (int i = 0; i <= FRAMES; i++)
						{
							int frame = i;
							steps.add(() ->
							{
								ross.reverse(facing);
								ross.changeAnimationState(frame < FRAMES ? Enum_AnimState.WALK : Enum_AnimState.IDLE, true);
								setClock(ross, frame < FRAMES ? frame * 0.106f + 0.05f : 0f);
								Gdx.gl.glClearColor(1, 0, 1, 1);
								Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
								batch.begin();
								ross.draw(batch);
								batch.end();
								grabs.get(key).add(GameHarness.grab());
							});
						}
					}
				}
			}

			@Override
			public void render()
			{
				try
				{
					if (steps.isEmpty() || error != null) Gdx.app.exit();
					else steps.remove(0).run();
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
	@DisplayName("his head stays put through the walk cycle, and the first step starts where he stood")
	void headStaysOnTheBody() throws Exception
	{
		if (error != null) error.printStackTrace();
		assertNull(error, "drawing Ross threw: " + error);
		assertEquals(Enum_AGE.values().length * 2, grabs.size());

		StringBuilder report = new StringBuilder();
		List<String> failures = new ArrayList<>();
		for (Map.Entry<String, List<BufferedImage>> e : grabs.entrySet())
		{
			List<BufferedImage> shots = e.getValue();
			assertEquals(FRAMES + 1, shots.size(), e.getKey());
			Frames.write(strip(shots), new File(OUTPUT, "ross-walk-" + e.getKey() + ".png"));

			double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
			StringBuilder heads = new StringBuilder();
			for (int i = 0; i < FRAMES; i++)
			{
				double head = headCentre(shots.get(i));
				heads.append(String.format(" %.0f", head));
				min = Math.min(min, head);
				max = Math.max(max, head);
			}
			double idle = headCentre(shots.get(FRAMES));
			report.append(e.getKey()).append(": head x").append(heads)
				.append(String.format(" (sway %.0f px), idle %.0f%n", max - min, idle));
			if (max - min > MAX_HEAD_SWAY)
				failures.add(e.getKey() + " head sways " + Math.round(max - min) + " px");
			// Idle and the walk's first frame are the same drawing: starting to walk must not jump.
			if (Math.abs(idle - headCentre(shots.get(0))) > 1)
				failures.add(e.getKey() + " jumps between idle and the first step");
		}
		System.out.print(report);
		assertTrue(failures.isEmpty(), failures + "\n" + report + "See " + OUTPUT + "/ross-walk-*.png");
	}

	/** Centre of the figure's top 15 %: his head, in screen px from the left. */
	private static double headCentre(BufferedImage shot)
	{
		int top = -1, bottom = -1;
		for (int y = 0; y < shot.getHeight() && bottom < 0; y++)
			if (rowHasFigure(shot, y)) top = top < 0 ? y : top;
		for (int y = shot.getHeight() - 1; y >= 0 && bottom < 0; y--)
			if (rowHasFigure(shot, y)) bottom = y;
		assertTrue(top >= 0, "Ross is not on screen");
		int band = top + (bottom - top) * 15 / 100;
		int left = Integer.MAX_VALUE, right = -1;
		for (int y = top; y <= band; y++)
			for (int x = 0; x < shot.getWidth(); x++)
				if (isFigure(shot.getRGB(x, y)))
				{
					left = Math.min(left, x);
					right = Math.max(right, x);
				}
		return (left + right) / 2.0;
	}

	private static boolean rowHasFigure(BufferedImage shot, int y)
	{
		for (int x = 0; x < shot.getWidth(); x++)
			if (isFigure(shot.getRGB(x, y))) return true;
		return false;
	}

	/** Far from the magenta clear colour; the blend at his outline is not him. */
	private static boolean isFigure(int rgb)
	{
		int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
		return Math.abs(r - 0xFF) + g + Math.abs(b - 0xFF) > 200;
	}

	/** The frames side by side, cropped to the part of the screen he uses, for a person to look at. */
	private static BufferedImage strip(List<BufferedImage> shots)
	{
		int x0 = 250, w = 600, h = shots.get(0).getHeight();
		BufferedImage out = new BufferedImage(w * shots.size(), h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		for (int i = 0; i < shots.size(); i++)
		{
			g.drawImage(shots.get(i).getSubimage(x0, 0, w, h), i * w, 0, null);
			g.setColor(java.awt.Color.BLACK);
			g.drawString(i < FRAMES ? "move #" + (i + 1) : "idle", i * w + 8, 16);
		}
		g.dispose();
		return out;
	}
}

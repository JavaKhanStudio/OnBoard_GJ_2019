package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.ui.Button;

import jks.amain.Main_Application;
import jks.vinterface.GVars_UI;
import jks.vue.GVars_Steam;
import jks.vue.GVars_Steam.Motion;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.SteamLab;

/**
 * How the steam leaving a carriage moves (r262, second round): Snap is d16's flip-book, its
 * ring still while the line is read; Breathe, Gather and Drift ease in, keep the ring alive,
 * and close by the same moment. Played from the steam lab, once each, on the shipped ring.
 *
 * Writes every frame of each play, at a third of the screen, to
 * build/frames/steam-motion/<motion>/0000.png, a frame per 1/30 s of game time:
 *   python3 tools/steam_motion_sheet.py verify/build/frames/steam-motion out-dir
 * makes a sheet and a video of each from them.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SteamMotionRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames/steam-motion");

	/** The carriage has loaded and its first thought is up. */
	private static final double FIRST_PLAY_AT = 4.0;
	/** Two moments in the ring's hold, a second apart, where a living ring has moved. */
	private static final double HOLD_A = 2.6, HOLD_B = 3.6;

	private static GameHarness harness;
	/** The biggest change of the screen from one frame to the next while the ring rises. */
	private static final Map<Motion, Double> biggestStep = new EnumMap<>(Motion.class);
	/** How far the screen changed between HOLD_A and HOLD_B. */
	private static final Map<Motion, Double> holdMoved = new EnumMap<>(Motion.class);
	private static final Map<Motion, Boolean> atRing = new EnumMap<>(Motion.class);
	private static final Map<Motion, Double> changeAfter = new EnumMap<>(Motion.class);
	private static final Map<Motion, Double> reading = new EnumMap<>(Motion.class);
	private static final Map<Motion, Double> coveredBeforeChange = new EnumMap<>(Motion.class);
	private static final Map<Motion, Double> liftedAfter = new EnumMap<>(Motion.class);
	private static volatile Throwable error;

	@BeforeAll
	void playEachMotion()
	{
		Main_Application.startPoint = Main_Application.StartPoint.STEAM_LAB;
		// A shorter run than the last would leave its tail frames behind.
		for (Motion motion : Motion.values())
		{
			File[] old = new File(OUTPUT, motion.name().toLowerCase()).listFiles();
			if (old != null) for (File f : old) f.delete();
		}

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - steam motion verification");
		gl.useVsync(false);

		Motion[] motions = Motion.values();
		int[] index = {-1};
		double[] playedAt = {0};
		int[] carriage = {0};
		int[] written = {0};
		BufferedImage[] previous = {null};
		BufferedImage[] holdA = {null};
		BufferedImage[] lastCovered = {null};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 90;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				double t = harness.gameSeconds;
				if (t < FIRST_PLAY_AT) return;
				if (index[0] < 0) index[0] = 0;
				Motion motion = motions[index[0]];

				if (playedAt[0] == 0)
				{
					// Through the lab's own button, so the panel in the frames names the motion.
					((Button) GVars_UI.mainUi.getRoot().findActor("motion-" + motion.name())).setChecked(true);
					assertEquals(motion, GVars_Steam.motion, "the lab's button did not pick " + motion);
					carriage[0] = GVars_Game.currentLevelInt;
					assertTrue(SteamLab.play(), "the lab did not start the steam for " + motion);
					reading.put(motion, (double) GVars_Game.dialogBubble.secondsBusy());
					playedAt[0] = t;
					written[0] = 0;
					previous[0] = null;
					holdA[0] = null;
					lastCovered[0] = null;
					return;
				}

				double in = t - playedAt[0];
				BufferedImage now = GameHarness.grab();
				Frames.write(third(now), new File(OUTPUT, motion.name().toLowerCase() + "/" + String.format("%04d", written[0]++) + ".png"));

				if (in < 2.0 && previous[0] != null)
					biggestStep.merge(motion, Frames.difference(previous[0], now), Math::max);
				previous[0] = now;

				if (holdA[0] == null && in >= HOLD_A)
				{
					holdA[0] = now;
					atRing.put(motion, GVars_Steam.isAtRing());
				}
				if (holdA[0] != null && !holdMoved.containsKey(motion) && in >= HOLD_B)
					holdMoved.put(motion, Frames.difference(holdA[0], now));

				if (GVars_Steam.isCovered()) lastCovered[0] = now;

				if (!changeAfter.containsKey(motion) && GVars_Game.currentLevelInt != carriage[0])
				{
					changeAfter.put(motion, in);
					if (lastCovered[0] != null) coveredBeforeChange.put(motion, steam(lastCovered[0]));
				}
				if (changeAfter.containsKey(motion) && !liftedAfter.containsKey(motion) && !GVars_Steam.isRunning())
					liftedAfter.put(motion, in - changeAfter.get(motion));

				// Once the steam has lifted off the next carriage, and a moment more, the next motion.
				if (liftedAfter.containsKey(motion) && in >= changeAfter.get(motion) + liftedAfter.get(motion) + 1.0)
				{
					playedAt[0] = 0;
					index[0]++;
					if (index[0] == motions.length)
					{
						// A lab pick outlives the run: the next GL test in this JVM plays the game's.
						((Button) GVars_UI.mainUi.getRoot().findActor("motion-" + GVars_Steam.SHIPPED_MOTION.name())).setChecked(true);
						Gdx.app.exit();
					}
				}
			}
			catch (Throwable e)
			{
				if (error == null) error = e;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	private static BufferedImage third(BufferedImage frame)
	{
		int w = frame.getWidth() / 3, h = frame.getHeight() / 3;
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		out.getGraphics().drawImage(frame.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING), 0, 0, null);
		return out;
	}

	/** The share of the screen in the covered frame's dark grey. */
	private static double steam(BufferedImage frame)
	{
		int steam = 0, all = 0;
		for (int x = 0; x < frame.getWidth(); x += 4)
			for (int y = 0; y < frame.getHeight(); y += 4)
			{
				int p = frame.getRGB(x, y);
				int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
				if (Math.abs(r - 70) < 8 && Math.abs(g - 101) < 8 && Math.abs(b - 108) < 8) steam++;
				all++;
			}
		return steam / (double) all;
	}

	private void ran()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertEquals(Motion.values().length, liftedAfter.size(), "not every motion lifted off the next carriage: " + liftedAfter);
		System.out.println("steam motion: biggest step " + biggestStep + ", hold moved " + holdMoved
			+ ", change after " + changeAfter + " (reading " + reading + "), lifted after " + liftedAfter);
	}

	@Test
	@DisplayName("Snap jumps from frame to frame; the easing motions roll in, no step half as big")
	void theRiseRolls()
	{
		ran();
		double snap = biggestStep.get(Motion.SNAP);
		for (Motion motion : new Motion[] {Motion.BREATHE, Motion.GATHER, Motion.DRIFT})
			assertTrue(biggestStep.get(motion) < snap / 2, motion + " stepped " + biggestStep + " while rising");
	}

	@Test
	@DisplayName("Snap's ring stands still while the line is read; the others keep moving")
	void theRingLives()
	{
		ran();
		for (Motion motion : Motion.values())
			assertTrue(atRing.get(motion), motion + " was not at its ring " + HOLD_A + " s in");
		// Ross idles and the bubble types, so even Snap's screen moves a little.
		double still = holdMoved.get(Motion.SNAP);
		for (Motion motion : new Motion[] {Motion.BREATHE, Motion.GATHER, Motion.DRIFT})
			assertTrue(holdMoved.get(motion) > still * 2 + 0.005, motion + " held still: " + holdMoved);
	}

	@Test
	@DisplayName("Every motion covers the screen for the change, which comes when it always did, and lifts slower than Snap")
	void theChangeStays()
	{
		ran();
		for (Motion motion : Motion.values())
		{
			assertTrue(coveredBeforeChange.containsKey(motion), motion + " changed the carriage without covering it");
			assertTrue(coveredBeforeChange.get(motion) > 0.85, motion + " covered only " + coveredBeforeChange.get(motion));
			double expected = Math.max(reading.get(motion), 9 * GVars_Steam.FRAME_SECONDS + GVars_Steam.HOLD_SECONDS);
			assertEquals(expected, changeAfter.get(motion), 0.2, motion + " moved the change");
			if (motion != Motion.SNAP)
				assertEquals(motion.liftSeconds, liftedAfter.get(motion), 0.2, motion + " lifted off at the wrong pace");
		}
	}

	@Test
	@DisplayName("The game ships Breathe until Simon picks (r262)")
	void shipsBreathe()
	{
		assertEquals(Motion.BREATHE, GVars_Steam.SHIPPED_MOTION);
	}
}

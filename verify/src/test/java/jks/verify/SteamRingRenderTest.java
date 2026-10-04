package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import jks.amain.Main_Application;
import jks.vue.GVars_Steam;
import jks.vue.GVars_Steam.Ring;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.SteamLab;

/**
 * The steam that leaves a carriage holds at its first ring while Ross's line is read, the
 * carriage still showing, and closes only for the change, which comes when it always did
 * (r262). Played from the steam lab, once in each shape: Whole is the steam before r262.
 *
 * Writes build/frames/steam-ring-<shape>.png, the screen a second and a half into each, and
 * steam-ring-<shape>-covered.png, the last frame before the change.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SteamRingRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	/** The carriage has loaded and its first thought is up. */
	private static final double FIRST_PLAY_AT = 4.0;
	/** Into the hold: a 12-a-second ring of 3 frames is up long before this. */
	private static final double HOLDING_AFTER = 1.5;

	/** The two greys the fumee frames are painted in, pale puffs and dark body. */
	private static final int[][] STEAM = {{70, 101, 108}, {100, 124, 129}};

	private static GameHarness harness;
	private static final Map<Ring, Double> coveredHolding = new EnumMap<>(Ring.class);
	private static final Map<Ring, Boolean> atRingHolding = new EnumMap<>(Ring.class);
	private static final Map<Ring, Double> coveredBeforeChange = new EnumMap<>(Ring.class);
	/** Seconds from Play to the next carriage, and what the line asked to be read for. */
	private static final Map<Ring, Double> changeAfter = new EnumMap<>(Ring.class);
	private static final Map<Ring, Double> reading = new EnumMap<>(Ring.class);
	private static volatile double coveredBefore = -1;
	private static volatile String settings;
	private static volatile Throwable error;

	@BeforeAll
	void playEachShape()
	{
		Main_Application.startPoint = Main_Application.StartPoint.STEAM_LAB;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - steam ring verification");
		gl.useVsync(false);

		Ring[] shapes = Ring.values();
		int[] shape = {-1};
		double[] playedAt = {0};
		int[] carriage = {0};
		boolean[] grabbedHolding = {false};
		BufferedImage[] lastCovered = {null};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 60;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				double t = harness.gameSeconds;
				if (t < FIRST_PLAY_AT) return;
				if (shape[0] < 0)
				{
					coveredBefore = covered(GameHarness.grab());
					shape[0] = 0;
				}
				Ring ring = shapes[shape[0]];

				if (playedAt[0] == 0)
				{
					GVars_Steam.ring = ring;
					carriage[0] = GVars_Game.currentLevelInt;
					assertTrue(SteamLab.play(), "the lab did not start the steam for " + ring);
					reading.put(ring, (double) GVars_Game.dialogBubble.secondsBusy());
					playedAt[0] = t;
					grabbedHolding[0] = false;
					lastCovered[0] = null;
					return;
				}

				if (!grabbedHolding[0] && t >= playedAt[0] + HOLDING_AFTER)
				{
					grabbedHolding[0] = true;
					BufferedImage holding = GameHarness.grab();
					Frames.write(holding, new File(OUTPUT, "steam-ring-" + ring.name().toLowerCase() + ".png"));
					coveredHolding.put(ring, covered(holding));
					atRingHolding.put(ring, GVars_Steam.isAtRing());
				}

				if (GVars_Steam.isCovered()) lastCovered[0] = GameHarness.grab();

				if (!changeAfter.containsKey(ring) && GVars_Game.currentLevelInt != carriage[0])
				{
					changeAfter.put(ring, t - playedAt[0]);
					if (lastCovered[0] != null)
					{
						Frames.write(lastCovered[0], new File(OUTPUT, "steam-ring-" + ring.name().toLowerCase() + "-covered.png"));
						coveredBeforeChange.put(ring, covered(lastCovered[0]));
					}
				}

				// Once the steam has lifted off the next carriage and its thought is up, the next shape.
				if (changeAfter.containsKey(ring) && !GVars_Steam.isRunning() && t >= playedAt[0] + changeAfter.get(ring) + 3.0)
				{
					playedAt[0] = 0;
					shape[0]++;
					if (shape[0] == shapes.length)
					{
						settings = SteamLab.settings();
						// A lab pick outlives the run: the next GL test in this JVM plays the game's.
						GVars_Steam.ring = GVars_Steam.SHIPPED_RING;
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

	/** The share of the screen painted in the steam's greys. */
	private static double covered(BufferedImage frame)
	{
		int steam = 0, all = 0;
		for (int x = 0; x < frame.getWidth(); x += 4)
			for (int y = 0; y < frame.getHeight(); y += 4)
			{
				int p = frame.getRGB(x, y);
				int r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
				for (int[] grey : STEAM)
					if (Math.abs(r - grey[0]) < 8 && Math.abs(g - grey[1]) < 8 && Math.abs(b - grey[2]) < 8)
					{
						steam++;
						break;
					}
				all++;
			}
		return steam / (double) all;
	}

	private void ran()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertEquals(Ring.values().length, changeAfter.size(), "not every shape reached its change: " + changeAfter);
	}

	@Test
	@DisplayName("Before r262 the covered screen held; now the steam holds at its first ring, the carriage showing")
	void theRingHolds()
	{
		ran();
		assertTrue(coveredBefore < 0.05, "the carriage itself reads as steam: " + coveredBefore);
		assertTrue(coveredHolding.get(Ring.WHOLE) > 0.8, "Whole no longer covers while it holds: " + coveredHolding);
		for (Ring ring : new Ring[] {Ring.CORNER, Ring.FLOOR, Ring.FRAME})
		{
			assertTrue(atRingHolding.get(ring), ring + " was not at its ring " + HOLDING_AFTER + " s in");
			double share = coveredHolding.get(ring);
			assertTrue(share > 0.1 && share < 0.6, ring + " covered " + share + " of the screen while the line was read");
		}
	}

	@Test
	@DisplayName("Every shape closes over the whole screen for the change, and the change comes when it always did")
	void itClosesForTheChange()
	{
		ran();
		for (Ring ring : Ring.values())
		{
			// The bubble stays in front of the steam (r118): it is all that is not steam.
			assertTrue(coveredBeforeChange.containsKey(ring), ring + " changed the carriage without covering it");
			assertTrue(coveredBeforeChange.get(ring) > 0.85, ring + " covered only " + coveredBeforeChange.get(ring) + " before the change");
			// The change waits for the line; the steam's cover and hold make up the rest, as before r262.
			double expected = Math.max(reading.get(ring), 9 * GVars_Steam.FRAME_SECONDS + GVars_Steam.HOLD_SECONDS);
			assertEquals(expected, changeAfter.get(ring), 0.2, ring + " moved the change");
		}
	}

	@Test
	@DisplayName("Copy settings names the shape and each shape's frame, and what moved from the game's")
	void copySettings()
	{
		ran();
		assertTrue(settings.contains("\"ring\": \"FRAME\""), settings);
		assertTrue(settings.contains("\"frameCORNER\": " + Ring.CORNER.shippedFrame), settings);
		assertTrue(settings.contains("\"frameFRAME\": " + Ring.FRAME.shippedFrame), settings);
		assertTrue(settings.contains("\"moved\": [\"ring\"]"), settings);
	}
}

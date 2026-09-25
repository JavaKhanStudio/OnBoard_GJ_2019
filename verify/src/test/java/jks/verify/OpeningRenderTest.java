package jks.verify;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;
import jks.vars.GVars_Heart;

/**
 * The opening - three splash logos, then the story cinematic, then the start screen -
 * was written in 2019 and then commented out of Main_Application, so it has never been
 * exercised by anything. Now that the game starts there again, it needs a guard.
 *
 * The screens hand off to each other on a timer, so this run also records which views it
 * passed through, proving the chain still moves rather than stalling on the first logo.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpeningRenderTest
{
	private static final int CAPTURE_FRAME = 60;

	/**
	 * A frame budget is the wrong unit here. The logo sequence is driven by scene2d actions
	 * on a wall clock - three logos at three seconds each - and the test window renders
	 * unthrottled, so a few hundred frames can pass in well under a second. Run until the
	 * sequence has visibly moved on, with a hard ceiling so a stall still ends the test.
	 * That ceiling is TIMEOUT_SEC, not a frame count: an idle machine draws 100 000 frames
	 * in about 10s of game time, which cut the run off on the story pages (r116).
	 */
	private static final int    EXIT_FRAME  = Integer.MAX_VALUE;
	/**
	 * Generous, because game time can lag real time here: Main_Application clamps delta to
	 * 1/30s, so if a frame takes longer than that the scene2d timers advance slower than
	 * the wall clock. The logo sequence is ~9s of game time.
	 */
	private static final double TIMEOUT_SEC = 60.0;

	private static final double MAX_DIFFERENCE = 0.12;

	private static final File MODULE = new File(System.getProperty("onboard.verify"));
	private static final File GOLDEN = new File(MODULE, "src/test/resources/golden/opening.png");
	private static final File OUTPUT = new File(MODULE, "build/frames");

	private static GameHarness harness;
	private static final Set<String> viewsSeen = new LinkedHashSet<>();
	private static volatile double secondsRun;
	private static volatile double lastPageTurn;

	@BeforeAll
	void runTheOpening()
	{
		Main_Application.startPoint = Main_Application.StartPoint.LOGO;

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1280, 720);   // cage's headless output size; fits any real display too
		config.setTitle("On Board - opening verification");
		config.setResizable(false);
		// Unlike the frame-indexed tests, this one waits on a wall clock. With vsync on, a
		// window the compositor is not scheduling (five test windows open and close in a
		// row, focus elsewhere) renders at a crawl, and since Main_Application clamps delta
		// to 1/30s the game's own clock then falls behind real time and the wait times out.
		config.useVsync(false);

		harness = new GameHarness(new Main_Application(), CAPTURE_FRAME, EXIT_FRAME);
		long startedAt = System.nanoTime();
		harness.frameHook = frame ->
		{
			if (GVars_Heart.vue != null) viewsSeen.add(GVars_Heart.vue.getClass().getSimpleName());

			double elapsed = (System.nanoTime() - startedAt) / 1e9;
			secondsRun = elapsed;

			// The story pages wait for the player before turning. Stand in for them, so the
			// test can follow the opening all the way to the menu rather than stopping at
			// the first hand-off - which is how a start screen that crashed on its first
			// label went unnoticed.
			if (GVars_Heart.vue != null
				&& GVars_Heart.vue.getClass().getSimpleName().equals("Vue_Scenematic_Intro")
				&& harness.gameSeconds - lastPageTurn > 0.4)
			{
				lastPageTurn = harness.gameSeconds;
				GVars_Heart.inCinematic_Click = true;
			}

			boolean reachedMenu = viewsSeen.contains("Vue_StartScreen");
			if (reachedMenu || elapsed > TIMEOUT_SEC)
			{
				com.badlogic.gdx.Gdx.app.exit();
			}
		};

		new Lwjgl3Application(harness, config);
	}

	@Test
	@DisplayName("the opening sequence renders without throwing")
	void openingRuns()
	{
		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null
			: "the opening threw: " + harness.error);
	}

	@Test
	@DisplayName("the opening runs all the way from the logos to the menu")
	void openingAdvances()
	{
		assertTrue(viewsSeen.contains("Vue_Preloading"),
			"never showed the logo screen; saw " + viewsSeen);
		assertTrue(viewsSeen.contains("Vue_Scenematic_Intro"),
			"the logos never handed off to the story pages. Saw " + viewsSeen);
		assertTrue(viewsSeen.contains("Vue_StartScreen"),
			"the opening never reached the menu. Saw " + viewsSeen + " in "
			+ String.format("%.1fs / %d frames / %.1fs of game time", secondsRun,
				harness.framesRendered, harness.gameSeconds) + ". Note this sequence runs on a wall clock while"
			+ " Main_Application clamps delta to 1/30s, so a machine under heavy GPU load can make"
			+ " game time lag real time badly enough to trip the ceiling.");
		System.out.println(String.format("opening passed through %s in %.1fs / %d frames",
			viewsSeen, secondsRun, harness.framesRendered));
	}

	@Test
	@DisplayName("the logo screen still looks the same")
	void matchesGoldenOpening() throws Exception
	{
		BufferedImage frame = harness.capture;
		assertNotNull(frame, "no frame was captured");
		Frames.write(frame, new File(OUTPUT, "opening-actual.png"));

		if (Boolean.getBoolean("onboard.golden.record") || !GOLDEN.isFile())
		{
			Frames.write(frame, GOLDEN);
			System.out.println("Recorded golden opening frame at " + GOLDEN.getAbsolutePath());
			return;
		}

		double difference = Frames.difference(Frames.read(GOLDEN), frame);
		assertTrue(difference <= MAX_DIFFERENCE, String.format(
			"the opening renders differently: mean difference %.4f exceeds %.4f (see %s)",
			difference, MAX_DIFFERENCE, new File(OUTPUT, "opening-actual.png").getAbsolutePath()));
	}
}

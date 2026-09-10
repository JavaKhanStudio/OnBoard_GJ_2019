package jks.verify;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;
import jks.index.Index_Credits;
import jks.vinterface.overlay.OverlayCredits;
import jks.vue.Utils_View;

/**
 * The credits screen. The button existed since 2019 with a stub behind it, so this is the
 * first time anything has drawn it.
 *
 * Worth its own test for one reason beyond "does it appear": the names carry accents, and
 * FreeType only rasterises the characters it is asked for. A missing glyph does not throw,
 * it just leaves a hole where a letter should be - "Bdard" - which no assertion about the
 * screen existing would ever notice.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreditsRenderTest
{
	private static final double CAPTURE_AFTER_SECONDS = 1.5;
	private static final double EXIT_AFTER_SECONDS    = 2.0;
	private static final double MAX_DIFFERENCE        = 0.12;

	private static final File MODULE = new File(System.getProperty("onboard.verify"));
	private static final File GOLDEN = new File(MODULE, "src/test/resources/golden/credits.png");
	private static final File OUTPUT = new File(MODULE, "build/frames");

	private static GameHarness harness;
	private static volatile Throwable creditsError;
	private static volatile boolean creditsShown;

	@BeforeAll
	void showTheCredits()
	{
		Main_Application.startPoint = Main_Application.StartPoint.START_SCREEN;

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1280, 720);
		config.setTitle("On Board - credits verification");
		config.useVsync(false);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 100_000);
		harness.captureAfterSeconds = CAPTURE_AFTER_SECONDS;
		harness.exitAfterSeconds = EXIT_AFTER_SECONDS;
		harness.frameHook = frame ->
		{
			if (creditsShown || harness.gameSeconds < 0.4) return;
			creditsShown = true;
			try
			{
				Utils_View.setOverlay(new OverlayCredits(seconds -> { }));
			}
			catch (Throwable t)
			{
				creditsError = t;
			}
		};

		new Lwjgl3Application(harness, config);
	}

	@Test
	@DisplayName("the credits screen opens without throwing")
	void creditsOpen()
	{
		if (creditsError != null) creditsError.printStackTrace();
		assertNull(creditsError, creditsError == null ? null
			: "opening the credits threw: " + creditsError);
		assertTrue(creditsShown, "the credits were never opened");
		assertNull(harness.error, harness.error == null ? null
			: "the game threw while the credits were up: " + harness.error);
	}

	@Test
	@DisplayName("every name and role is listed exactly once")
	void everyoneIsCredited()
	{
		java.util.List<String> everyone = new java.util.ArrayList<>();
		for (Index_Credits.Section section : Index_Credits.SECTIONS)
			for (String person : section.people)
				everyone.add(person);

		assertTrue(everyone.size() >= 4, "expected the whole team, found " + everyone);
		assertEquals(everyone.size(), new java.util.HashSet<>(everyone).size(),
			"somebody is credited twice: " + everyone);
		for (String person : everyone)
			assertTrue(person != null && !person.trim().isEmpty(), "blank name in the credits");
	}

	@Test
	@DisplayName("the credits render, accents and all")
	void matchesGolden() throws Exception
	{
		BufferedImage frame = harness.capture;
		assertNotNull(frame, "no frame was captured");
		Frames.write(frame, new File(OUTPUT, "credits-actual.png"));

		if (Boolean.getBoolean("onboard.golden.record") || !GOLDEN.isFile())
		{
			Frames.write(frame, GOLDEN);
			System.out.println("Recorded golden credits at " + GOLDEN.getAbsolutePath());
			return;
		}

		double difference = Frames.difference(Frames.read(GOLDEN), frame);
		assertTrue(difference <= MAX_DIFFERENCE, String.format(
			"the credits render differently: mean difference %.4f exceeds %.4f (see %s)",
			difference, MAX_DIFFERENCE, new File(OUTPUT, "credits-actual.png").getAbsolutePath()));
	}

	private static void assertEquals(int expected, int actual, String message)
	{
		org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
	}
}

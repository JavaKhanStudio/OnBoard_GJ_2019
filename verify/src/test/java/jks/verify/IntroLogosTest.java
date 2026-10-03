package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture;

import jks.amain.Main_Application;
import jks.vars.GVars_Heart;
import jks.vue.models.Vue_Scenematic_Intro;

/**
 * The splash logos ride on the intro's pages (r158), where they once played alone before it
 * (Vue_Preloading, and r23's click-per-logo): the team's on page 1, libGDX's on page 2, the
 * jam's on page 3, the order they always came in. The game starts on the first page.
 *
 * Each logo waits for its page to be half up, starts before the page is done (r257), and fades
 * in gently (D4); one click turns the
 * page, logo and all. The clicks go in through the input processor the game installed, as a
 * mouse would. A shot of each page with its logo fully up lands in build/frames/intro-logo-N.png.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntroLogosTest
{
	private static final double TIMEOUT_SEC = 60.0;
	/** A frame may raise a logo by no more than this: a fade, not a flash. */
	private static final float MAX_STEP = 0.1f;

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;
	private static final List<String> firstViews = new ArrayList<>();
	private static final List<String> logosShown = new ArrayList<>();
	private static final List<BufferedImage> shots = new ArrayList<>();
	private static final List<String> faults = new ArrayList<>();
	private static volatile int clicks;
	private static volatile int clicksToMenu = -1;
	private static float lastLogoAlpha;
	/** Logos that started to rise while their page was still coming up (r257). */
	private static final List<String> earlyLogos = new ArrayList<>();
	private static Texture pageShot;

	@BeforeAll
	void clickThroughTheIntro() throws Exception
	{
		Main_Application.startPoint = Main_Application.StartPoint.LOGO;

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1280, 720);
		config.setTitle("On Board - intro logos verification");
		config.setResizable(false);
		config.useVsync(false);

		harness = new GameHarness(new Main_Application(), -1, Integer.MAX_VALUE);
		long startedAt = System.nanoTime();
		harness.frameHook = frame ->
		{
			String view = GVars_Heart.vue == null ? "none" : GVars_Heart.vue.getClass().getSimpleName();
			if (firstViews.isEmpty()) firstViews.add(view);

			if (GVars_Heart.vue instanceof Vue_Scenematic_Intro)
			{
				Vue_Scenematic_Intro intro = (Vue_Scenematic_Intro) GVars_Heart.vue;
				String logo = name(intro, intro.currentLogo);

				if (intro.logoAlpha > lastLogoAlpha && intro.currentAlpha < Vue_Scenematic_Intro.LOGO_FROM)
					faults.add(String.format("%s rose with its page only %.2f up", logo, intro.currentAlpha));
				if (intro.logoAlpha > 0 && intro.currentAlpha < 1 && !earlyLogos.contains(logo))
					earlyLogos.add(logo);
				if (intro.logoAlpha - lastLogoAlpha > MAX_STEP)
					faults.add(String.format("%s jumped %.2f in one frame", logo, intro.logoAlpha - lastLogoAlpha));
				lastLogoAlpha = intro.logoAlpha;

				// Fully up and not yet shot: keep the frame, then turn the page. The page only
				// starts to go on the frame after the click, so it is the page that is counted.
				if (intro.logoAlpha >= 1 && intro.currentpage != pageShot)
				{
					pageShot = intro.currentpage;
					logosShown.add(logo);
					shots.add(GameHarness.grab());
					click();
				}
			}
			else if (view.equals("Vue_StartScreen") && clicksToMenu < 0)
			{
				clicksToMenu = clicks;
				Gdx.app.exit();
			}

			if ((System.nanoTime() - startedAt) / 1e9 > TIMEOUT_SEC) Gdx.app.exit();
		};

		new Lwjgl3Application(harness, config);

		for (int i = 0; i < shots.size(); i++)
			Frames.write(shots.get(i), new File(OUTPUT, "intro-logo-" + (i + 1) + ".png"));
	}

	private static String name(Vue_Scenematic_Intro intro, Texture logo)
	{
		if (logo == intro.logo_Team)   return "pixmen";
		if (logo == intro.logo_Engine) return "libGDX";
		if (logo == intro.logo_Jam)    return "Jamming";
		return "none";
	}

	private static void click()
	{
		clicks++;
		int x = Gdx.graphics.getWidth() / 2;
		int y = Gdx.graphics.getHeight() / 2;
		Gdx.input.getInputProcessor().touchDown(x, y, 0, Buttons.LEFT);
		Gdx.input.getInputProcessor().touchUp(x, y, 0, Buttons.LEFT);
	}

	@Test
	@DisplayName("clicking through the intro throws nothing")
	void runsClean()
	{
		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null : "the intro threw: " + harness.error);
	}

	@Test
	@DisplayName("the game opens on the intro, with no screen of logos before it")
	void opensOnTheIntro()
	{
		assertEquals(List.of("Vue_Scenematic_Intro"), firstViews);
	}

	@Test
	@DisplayName("each page carries one logo, in the old order, and one click turns it")
	void oneLogoPerPage()
	{
		assertEquals(List.of("pixmen", "libGDX", "Jamming"), logosShown);
		assertEquals(3, clicksToMenu, "it took " + clicksToMenu + " clicks to reach the menu, not one per page");
	}

	@Test
	@DisplayName("a logo waits for its page to be half up, then fades in gently")
	void fadesInGently()
	{
		assertTrue(faults.isEmpty(), String.join("; ", faults.subList(0, Math.min(5, faults.size()))));
	}

	@Test
	@DisplayName("each logo starts to rise before its page is done (r257)")
	void startsBeforeThePageIsDone()
	{
		assertEquals(List.of("pixmen", "libGDX", "Jamming"), earlyLogos);
	}
}

package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import jks.amain.Main_Application;
import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vue.models.Vue_Preloading;
import jks.vue.models.Vue_Scenematic_Intro;

/**
 * A click on the splash logos skips ONE logo: pixman to libGDX, libGDX to Jamming, Jamming
 * to the story. Each logo otherwise holds the screen for three seconds of its own timer.
 *
 * The clicks go in through the input processor the game installed, as a mouse would, so
 * this also fails if the logo screen stops listening.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LogoSkipTest
{
	/** Long enough to see a logo has arrived; far short of the three seconds it would stay. */
	private static final double CLICK_AFTER = 0.3;
	/** How long the first story page must hold, unclicked, once the logos hand over. */
	private static final double INTRO_HOLD  = 1.5;
	private static final double TIMEOUT_SEC = 30.0;

	private static GameHarness harness;
	private static final List<String> logosShown = new ArrayList<>();
	private static volatile int clicks;
	private static volatile int clicksAtIntro = -1;
	private static volatile double introAt = -1;
	private static volatile boolean firstPageHeld = true;

	private static double logoSince;

	@BeforeAll
	void clickThroughTheLogos()
	{
		Main_Application.startPoint = Main_Application.StartPoint.LOGO;

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1280, 720);
		config.setTitle("On Board - logo skip verification");
		config.setResizable(false);
		config.useVsync(false);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		long startedAt = System.nanoTime();
		harness.frameHook = frame ->
		{
			if (GVars_Heart.vue instanceof Vue_Preloading)
			{
				String logo = logoOnScreen((Vue_Preloading) GVars_Heart.vue);
				if (logo != null && (logosShown.isEmpty() || !logosShown.get(logosShown.size() - 1).equals(logo)))
				{
					logosShown.add(logo);
					logoSince = harness.gameSeconds;
				}
				if (logo != null && harness.gameSeconds - logoSince >= CLICK_AFTER
					&& clicks < logosShown.size())
				{
					click();
				}
			}
			else if (GVars_Heart.vue instanceof Vue_Scenematic_Intro)
			{
				Vue_Scenematic_Intro intro = (Vue_Scenematic_Intro) GVars_Heart.vue;
				if (introAt < 0)
				{
					introAt = harness.gameSeconds;
					clicksAtIntro = clicks;
				}
				if (intro.currentpage != intro.page_1) firstPageHeld = false;
				if (harness.gameSeconds - introAt >= INTRO_HOLD) Gdx.app.exit();
			}

			if ((System.nanoTime() - startedAt) / 1e9 > TIMEOUT_SEC) Gdx.app.exit();
		};

		new Lwjgl3Application(harness, config);
	}

	private static void click()
	{
		clicks++;
		int x = Gdx.graphics.getWidth() / 2;
		int y = Gdx.graphics.getHeight() / 2;
		Gdx.input.getInputProcessor().touchDown(x, y, 0, Buttons.LEFT);
		Gdx.input.getInputProcessor().touchUp(x, y, 0, Buttons.LEFT);
	}

	/** Which of the three logos the stage is showing, or null between them. */
	private static String logoOnScreen(Vue_Preloading logos)
	{
		for (Actor actor : GVars_UI.mainUi.getActors())
		{
			if (!(actor instanceof Image) || !(((Image) actor).getDrawable() instanceof TextureRegionDrawable))
				continue;
			Texture shown = ((TextureRegionDrawable) ((Image) actor).getDrawable()).getRegion().getTexture();
			if (shown == logos.logo_Team)   return "pixman";
			if (shown == logos.logo_Engine) return "libGDX";
			if (shown == logos.logo_Jam)    return "Jamming";
		}
		return null;
	}

	@Test
	@DisplayName("clicking through the logos throws nothing")
	void runsClean()
	{
		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null
			: "the logo screen threw: " + harness.error);
	}

	@Test
	@DisplayName("each click skips one logo, in order, then hands over to the story")
	void oneLogoPerClick()
	{
		assertEquals(List.of("pixman", "libGDX", "Jamming"), logosShown,
			"a click should move on by exactly one logo");
		assertTrue(introAt >= 0, "the logos never handed over to the story pages");
		assertEquals(3, clicksAtIntro, "it took " + clicksAtIntro + " clicks to reach the story, not one per logo");
		// Left alone the logos take nine seconds; a click a third of a second in on each
		// should arrive in about one.
		assertTrue(introAt < 3.0, String.format(
			"the clicks did not cut the logos short: reached the story after %.1fs of game time", introAt));
	}

	@Test
	@DisplayName("the click that ends the logos does not also turn the first story page")
	void clickIsSpentOnTheLogo()
	{
		assertTrue(introAt >= 0, "the logos never handed over to the story pages");
		assertTrue(firstPageHeld, "the first story page turned by itself: the last logo's click carried over");
	}
}

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
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;
import jks.vars.GVars_Heart;
import jks.vue.GVars_Steam;

/**
 * The click that leaves the intro's last page raises the fumee steam over it, the menu takes
 * over once the steam covers the screen, and the steam lifts off the menu (r85).
 *
 * Runs the shipped path, logos first, clicking the story pages on as a player would. Every
 * steam frame shown is written to build/frames/steam-NN-view-fK.png, which is the film the
 * ticket hands back.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntroSteamTest
{
	private static final double TIMEOUT_SEC = 60.0;
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	/** One line per rendered frame while the steam runs: the view under it and the frame shown. */
	private final List<String> steam = new ArrayList<>();
	private final List<BufferedImage> shots = new ArrayList<>();
	private final List<String> shotNames = new ArrayList<>();
	private GameHarness harness;
	private double lastPageTurn;
	private boolean sawSteam;

	@BeforeAll
	void runTheOpening() throws Exception
	{
		Main_Application.startPoint = Main_Application.StartPoint.LOGO;

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1280, 720);
		config.setTitle("On Board - intro steam verification");
		config.setResizable(false);
		config.useVsync(false);

		harness = new GameHarness(new Main_Application(), -1, 100_000);
		long startedAt = System.nanoTime();
		harness.frameHook = frame ->
		{
			String view = GVars_Heart.vue == null ? "none" : GVars_Heart.vue.getClass().getSimpleName();

			if (view.equals("Vue_Scenematic_Intro") && !GVars_Steam.isRunning()
				&& harness.gameSeconds - lastPageTurn > 2.5)
			{
				lastPageTurn = harness.gameSeconds;
				GVars_Heart.inCinematic_Click = true;
			}

			int index = GVars_Steam.frameIndex();
			if (index >= 0)
			{
				sawSteam = true;
				String line = view + " f" + (index + 1);
				if (steam.isEmpty() || !steam.get(steam.size() - 1).equals(line))
				{
					steam.add(line);
					shots.add(GameHarness.grab());
					shotNames.add(String.format("steam-%02d-%s-f%d.png", shots.size(), view, index + 1));
				}
			}

			boolean over = sawSteam && index < 0;
			if (over || (System.nanoTime() - startedAt) / 1e9 > TIMEOUT_SEC)
				Gdx.app.exit();
		};

		new Lwjgl3Application(harness, config);

		for (int i = 0; i < shots.size(); i++)
			Frames.write(shots.get(i), new File(OUTPUT, shotNames.get(i)));
		System.out.println("steam ran: " + steam);
	}

	@Test
	@DisplayName("the steam runs without throwing")
	void runsClean()
	{
		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null : "the intro threw: " + harness.error);
	}

	@Test
	@DisplayName("the steam rises over the last page, the menu comes at full cover, and the steam lifts off it")
	void coversThenLifts()
	{
		List<String> expected = new ArrayList<>();
		for (int f = 1; f <= 9; f++) expected.add("Vue_Scenematic_Intro f" + f);
		for (int f = 9; f >= 1; f--) expected.add("Vue_StartScreen f" + f);
		assertEquals(expected, steam, "the steam frames, in the order they showed, over which view");
	}
}

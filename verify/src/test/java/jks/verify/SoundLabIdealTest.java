package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.kotcrab.vis.ui.widget.VisTextButton;

import jks.amain.Main_Application;
import jks.sounds.GVars_AudioManager;
import jks.vinterface.GVars_UI;

/**
 * The sound lab's carriage rows (r99): the ideal tracks are lab-only files in lab-assets/, so
 * each row's Ideal button is dark exactly when that carriage's track is not there, and Modulated
 * - which ships in desktop/assets - is always lit. Passes with lab-assets/ empty or filled
 * (-PlabAssets=<dir> points it elsewhere); the frame is kept for a person to look at.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SoundLabIdealTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;
	private static final List<String> seen = new ArrayList<>();
	private static volatile boolean grabbed;
	private static volatile Throwable error;

	@BeforeAll
	void openTheLab()
	{
		Main_Application.startPoint = Main_Application.StartPoint.SOUND_LAB;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - sound lab verification");
		gl.useVsync(false);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 15;
		harness.frameHook = frame ->
		{
			if (error != null || grabbed || harness.gameSeconds < 1.0) return;
			try
			{
				List<VisTextButton> ideal = new ArrayList<>(), modulated = new ArrayList<>();
				collect(GVars_UI.mainUi.getRoot(), ideal, modulated);
				for (int carriage = 1; carriage <= ideal.size(); carriage++)
				{
					boolean here = GVars_AudioManager.idealFile(carriage) != null;
					seen.add((ideal.get(carriage - 1).isDisabled() == !here ? "ok   " : "FAIL ")
						+ "wa" + carriage + " ideal " + (here ? "here, Ideal lit" : "absent, Ideal dark"));
				}
				for (int carriage = 1; carriage <= modulated.size(); carriage++)
					seen.add((modulated.get(carriage - 1).isDisabled() ? "FAIL " : "ok   ") + "wa" + carriage + " Modulated lit (it ships)");
				Frames.write(GameHarness.grab(), new File(OUTPUT, "sound-lab-carriages.png"));
				grabbed = true;
				Gdx.app.exit();
			}
			catch (Throwable e)
			{
				if (error == null) error = e;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	/** The carriage rows' Ideal and Modulated buttons, top to bottom. */
	private static void collect(Group group, List<VisTextButton> ideal, List<VisTextButton> modulated)
	{
		for (Actor child : group.getChildren())
		{
			if (child instanceof VisTextButton)
			{
				String text = ((VisTextButton) child).getText().toString();
				if (text.equals("Ideal")) ideal.add((VisTextButton) child);
				if (text.equals("Modulated")) modulated.add((VisTextButton) child);
			}
			else if (child instanceof Group)
				collect((Group) child, ideal, modulated);
		}
	}

	@Test
	@DisplayName("each carriage's Ideal button is dark exactly when its track is not in lab-assets, and Modulated is always lit")
	void idealIsDarkWhereItsFileIsMissing()
	{
		String report = String.join("\n", seen);
		System.out.println(report);
		assertNull(harness.error, harness.error == null ? null : "the lab threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertTrue(grabbed, "the lab was never looked at");
		assertEquals(8, seen.size(), "four carriage rows, two buttons each:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);
	}
}

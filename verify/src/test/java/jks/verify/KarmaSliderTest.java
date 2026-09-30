package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.kotcrab.vis.ui.widget.VisSlider;

import jks.amain.Main_Application;
import jks.index.Index_Interface;
import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vue.models.Vue_Scenematic_Outro;
import jks.vue.models.game.GVars_Game;

/**
 * Karma on a slider (d11). A run started in carriage 4 has none of the karma carriages 1 to 3
 * could have given, so without the slider it can only reach the staying ending. With
 * -Donboard.lab=true the slider sets it, and the outro follows.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KarmaSliderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;

	private static final Checks seen = new Checks();
	private static Steps steps;
	private static volatile boolean finished;

	@BeforeAll
	void leaveFromCarriageFour()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = 4;
		Main_Application.lab = true;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - karma slider verification");
		gl.useVsync(false);

		steps = new Steps(1.5, 0.6);
		steps.add(() -> {
			seen.record("the slider is under the carriage", panel() != null);
			seen.record("a carriage-4 start has no karma", GVars_Game.karma == 0 && slider().getValue() == 0);
			slider().setValue(3);
			seen.record("the slider sets the karma", GVars_Game.karma == 3 && GVars_Game.leavingEnding());
		});
		steps.add(() -> {
			Frames.write(GameHarness.grab(), new File(OUTPUT, "karma-slider.png"));
			// As interaction 2 on an item would.
			GVars_Game.karma = 1;
		});
		steps.add(() -> {
			seen.record("karma earned in play moves the slider", slider().getValue() == 1);
			slider().setValue(2);
			seen.record("2 is still the staying ending", !GVars_Game.leavingEnding());
			slider().setValue(3);
			GVars_Game.nextLevel();
		});
		// nextLevel fades out for 1 s before the outro takes over.
		steps.add(() -> {});
		steps.add(() -> {});
		steps.add(() -> {
			seen.record("carriage 4 leads to the outro", GVars_Heart.vue instanceof Vue_Scenematic_Outro);
			Texture leave = Index_Interface.manager.get(Index_Interface.outroPage_leave, Texture.class);
			seen.record("and the outro takes the leaving ending", ((Vue_Scenematic_Outro) GVars_Heart.vue).page_2 == leave);
			finished = true;
		});

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = steps.seconds() + 0.4;
		steps.drive(harness);

		new Lwjgl3Application(harness, gl);
	}

	@AfterAll
	void restore()
	{
		Main_Application.startLevel = 1;
		Main_Application.lab = false;
	}

	private static Group panel()
	{
		return GVars_UI.mainUi.getRoot().findActor("karmaSlider");
	}

	private static VisSlider slider()
	{
		for (Actor child : panel().getChildren())
			if (child instanceof VisSlider) return (VisSlider) child;
		throw new AssertionError("no slider in the karma panel");
	}

	@Test
	@DisplayName("the karma slider sets the karma, follows it, and the outro takes the ending it gives")
	void sliderDecidesTheEnding()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (steps.error != null) steps.error.printStackTrace();
		String report = String.join("\n", seen);
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + "\n" + report);
		assertNull(steps.error, steps.error == null ? null : "a step threw: " + steps.error + "\n" + report);
		assertTrue(finished, "not every step ran:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);
		assertEquals(7, seen.size(), report);
	}
}

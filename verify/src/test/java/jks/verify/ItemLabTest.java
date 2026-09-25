package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTextButton;

import jks.amain.Main_Application;
import jks.vinterface.GVars_UI;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.ItemOutline;

/**
 * The item lab (d10): a carriage with the outline's numbers on sliders, to choose how a hovered
 * item lights. Its sliders must reach ItemOutline, and "Every item" must light them all.
 * hover-lab.png is the render handed back to the board.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemLabTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;

	private static final List<String> seen = new ArrayList<>();
	private static volatile Throwable error;
	private static volatile boolean finished;

	@BeforeAll
	void openTheLab()
	{
		// Carriage 4 has the papers, the palest item in the game.
		Main_Application.startPoint = Main_Application.StartPoint.ITEM_LAB;
		Main_Application.startLevel = 4;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - item lab verification");
		gl.useVsync(false);

		boolean[] pressed = {false};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 3.0;
		harness.frameHook = frame ->
		{
			try
			{
				if (!pressed[0] && harness.gameSeconds >= 1.5)
				{
					Actor lab = GVars_UI.mainUi.getRoot().findActor("itemOutlineLab");
					record("the lab's panel is over the carriage", lab != null);
					record("in carriage 4", "wa4".equals(GVars_Game.currentLevel.path_meta));
					record("nothing is outlined before asking", !ItemOutline.lightAll);

					VisTextButton every = find((Group) lab, VisTextButton.class);
					every.toggle();
					record("Every item outlines them all", ItemOutline.lightAll);

					VisSlider width = find((Group) lab, VisSlider.class);
					width.setValue(4f);
					record("the first slider sets the width", ItemOutline.width == 4f);
					pressed[0] = true;
				}
				else if (pressed[0] && !finished && harness.gameSeconds >= 2.5)
				{
					Frames.write(GameHarness.grab(), new File(OUTPUT, "hover-lab.png"));
					finished = true;
				}
			}
			catch (Throwable t)
			{
				if (error == null) error = t;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	@AfterAll
	void restore()
	{
		Main_Application.startLevel = 1;
		Main_Application.startPoint = Main_Application.StartPoint.LOGO;
	}

	private static <T extends Actor> T find(Group group, Class<T> type)
	{
		for (Actor child : group.getChildren())
			if (type.isInstance(child)) return type.cast(child);
		throw new AssertionError("no " + type.getSimpleName() + " in the lab");
	}

	private static void record(String step, boolean ok)
	{
		seen.add((ok ? "ok   " : "FAIL ") + step);
	}

	@Test
	@DisplayName("the item lab opens on a carriage, and its controls move the outline")
	void labMovesTheOutline()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		String report = String.join("\n", seen);
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + "\n" + report);
		assertNull(error, error == null ? null : "a step threw: " + error + "\n" + report);
		assertTrue(finished, "not every step ran:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);
		assertEquals(5, seen.size(), report);
	}
}

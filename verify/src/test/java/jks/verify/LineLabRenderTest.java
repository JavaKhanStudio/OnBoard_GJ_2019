package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;

import jks.amain.Main_Application;
import jks.index.Index_Text;
import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vue.models.Vue_LineLab;

/**
 * The line lab (r74) opens, lists a carriage's items with their lines, and flips to each of
 * the four carriages without throwing. Two frames per carriage are kept for a person to look at,
 * its rows at the top and scrolled to the end; the last has the cage's long line typed whole in
 * the preview bubble. A carriage is taller than the window (r214): its last line's field, given
 * the keyboard as Tab gives it, must scroll whole into the list and the window.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LineLabRenderTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;
	private static volatile BufferedImage first;
	private static volatile int width, height, carriagesShown;
	private static final StringBuilder unreachable = new StringBuilder();
	private static volatile Throwable error;

	@BeforeAll
	void openTheLab()
	{
		Main_Application.startPoint = Main_Application.StartPoint.LINE_LAB;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - line lab verification");
		gl.useVsync(false);

		int[] step = {0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 20;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				// A carriage every two seconds: shown, grabbed at the top, its last field focused, grabbed.
				double t = harness.gameSeconds;
				int carriage = step[0] / 4 + 1;
				if (t < 1.0 + step[0] * 0.5)
					return;
				Vue_LineLab lab = (Vue_LineLab) GVars_Heart.vue;
				switch (step[0] % 4)
				{
					case 0:
						// The longest line, typed from the start: whole in the bubble by the last frame.
						if (step[0] == 0)
							lab.say(Index_Text.get("wa1.cage.message1"));
						lab.showCarriage(carriage);
						break;
					case 1:
						grab("line-lab-carriage-" + carriage + ".png");
						break;
					case 2:
						// As Tab reaches it: focusing the last field must scroll the list to it.
						GVars_UI.mainUi.setKeyboardFocus(lastField());
						break;
					default:
						grab("line-lab-carriage-" + carriage + "-end.png");
						checkLastFieldShows(carriage);
						carriagesShown++;
						if (carriage == 4)
							Gdx.app.exit();
				}
				step[0]++;
			}
			catch (Throwable e)
			{
				if (error == null) error = e;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	@Test
	@DisplayName("the line lab shows each of the four carriages")
	void everyCarriageShows()
	{
		assertNull(harness.error, harness.error == null ? null : "the lab threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertNotNull(first, "no frame was taken");
		assertEquals(1280, width, "the window is not the size the frames assume");
		assertEquals(720, height, "the window is not the size the frames assume");
		assertEquals(4, carriagesShown, "not every carriage was shown");
		assertEquals("", unreachable.toString(), "a carriage's last line cannot be scrolled into view");
		assertTrue(harness.framesRendered > 0);
	}

	private static void grab(String name) throws java.io.IOException
	{
		BufferedImage frameImage = GameHarness.grab();
		if (first == null)
		{
			first = frameImage;
			width = Gdx.graphics.getWidth();
			height = Gdx.graphics.getHeight();
		}
		Frames.write(frameImage, new File(OUTPUT, name));
	}

	/** The last field of the list, scrolled to the end, lies inside the list's view and the window. */
	private static void checkLastFieldShows(int carriage)
	{
		ScrollPane rows = find(GVars_UI.mainUi.getRoot(), ScrollPane.class);
		TextArea last = lastField();
		Vector2 bottom = last.localToStageCoordinates(new Vector2(0, 0));
		Vector2 top = last.localToStageCoordinates(new Vector2(0, last.getHeight()));
		Vector2 viewBottom = rows.localToStageCoordinates(new Vector2(0, 0));
		Vector2 viewTop = rows.localToStageCoordinates(new Vector2(0, rows.getHeight()));
		if (bottom.y < Math.max(viewBottom.y, 0) - 1 || top.y > Math.min(viewTop.y, Gdx.graphics.getHeight()) + 1)
			unreachable.append(String.format("carriage %d: last field at y %.0f..%.0f, list shows %.0f..%.0f; ",
				carriage, bottom.y, top.y, viewBottom.y, viewTop.y));
	}

	private static TextArea lastField()
	{
		TextArea last = null;
		for (Actor actor : ((Group) find(GVars_UI.mainUi.getRoot(), ScrollPane.class).getActor()).getChildren())
			if (actor instanceof TextArea)
				last = (TextArea) actor;
		return last;
	}

	private static <T extends Actor> T find(Actor actor, Class<T> type)
	{
		if (type.isInstance(actor))
			return type.cast(actor);
		if (actor instanceof Group)
			for (Actor child : ((Group) actor).getChildren())
			{
				T found = find(child, type);
				if (found != null)
					return found;
			}
		return null;
	}
}

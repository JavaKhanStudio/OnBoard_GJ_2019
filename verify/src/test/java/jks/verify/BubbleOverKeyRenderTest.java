package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.scenes.scene2d.Actor;

import jks.amain.Main_Application;
import jks.index.Index_Text;
import jks.vinterface.GVars_UI;
import jks.vue.models.game.Clef;
import jks.vue.models.game.GVars_Game;

/**
 * Ross's bubble is drawn in front of the key (r106). With Ross at the right of carriage 2, older
 * and taller, his thought rises into the top right corner where the key sits: the key used to be
 * added to the stage after the bubble at every carriage, and so covered what he was saying.
 * In carriage 1 the boy is too small for his cloud to reach it, so only the order is checked.
 *
 * Checked in carriage 1 and again after a carriage change, which builds a new key: that new key
 * must not come in front either, and the last carriage's key must be gone from the stage.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BubbleOverKeyRenderTest
{
	private static final double SAY_AT = 2.0;
	private static final double LOOK_AT = SAY_AT + 1.5;
	private static final double NEXT_AT = LOOK_AT + 0.2;
	/** The carriage change fades out for a second and the next one back in over two (r44). */
	private static final double SAY_AGAIN_AT = NEXT_AT + 4.0;
	private static final double LOOK_AGAIN_AT = SAY_AGAIN_AT + 1.5;

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;
	private static volatile Throwable error;

	private static volatile int bubbleFirst = -1, keyFirst = -1, bubbleSecond = -1, keySecond = -1, keysSecond = -1;
	private static volatile boolean overlapSecond;

	@BeforeAll
	void sayItUnderTheKey()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = 1;
		GVars_Game.thinkAtEachStep = false;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - bubble over key verification");
		gl.useVsync(false);

		int[] step = {0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 60;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				double t = harness.gameSeconds;
				if (step[0] == 0 && t >= SAY_AT)
				{
					step[0] = 1;
					say();
				}
				else if (step[0] == 1 && t >= LOOK_AT)
				{
					step[0] = 2;
					bubbleFirst = GVars_Game.dialogBubble.getZIndex();
					keyFirst = GVars_Game.clef.getZIndex();
					Frames.write(GameHarness.grab(), new File(OUTPUT, "bubble-over-key.png"));
				}
				else if (step[0] == 2 && t >= NEXT_AT)
				{
					step[0] = 3;
					GVars_Game.nextLevel();
				}
				else if (step[0] == 3 && t >= SAY_AGAIN_AT)
				{
					step[0] = 4;
					say();
				}
				else if (step[0] == 4 && t >= LOOK_AGAIN_AT)
				{
					step[0] = 5;
					bubbleSecond = GVars_Game.dialogBubble.getZIndex();
					keySecond = GVars_Game.clef.getZIndex();
					int keys = 0;
					for (Actor actor : GVars_UI.mainUi.getActors())
						if (actor instanceof Clef) keys++;
					keysSecond = keys;
					overlapSecond = overlap();
					Frames.write(GameHarness.grab(), new File(OUTPUT, "bubble-over-key-carriage2.png"));
					harness.exitAfterSeconds = t + 0.2;
				}
			}
			catch (Throwable e)
			{
				error = e;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	/** Ross to the right of the carriage, facing left, thinking: the cloud rises to the key. */
	private static void say()
	{
		GVars_Game.ross.position.x = 1150;
		GVars_Game.dialogBubble.applyText(Index_Text.get("wa1.hint1"));
	}

	/** The bubble's box and the key's box meet on the stage: the test shows something. */
	private static boolean overlap()
	{
		Actor a = GVars_Game.dialogBubble, b = GVars_Game.clef;
		return a.getX() < b.getX() + b.getWidth() && b.getX() < a.getX() + a.getWidth()
			&& a.getY() < b.getY() + b.getHeight() && b.getY() < a.getY() + a.getHeight();
	}

	@Test
	@DisplayName("the bubble is drawn in front of the key, before and after a carriage change")
	void bubbleInFront()
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);

		assertTrue(overlapSecond, "the bubble never reached the key in carriage 2: the frames prove nothing");
		assertTrue(bubbleFirst > keyFirst, "carriage 1: the key (" + keyFirst + ") is drawn over the bubble (" + bubbleFirst + ")");
		assertTrue(bubbleSecond > keySecond, "carriage 2: the key (" + keySecond + ") is drawn over the bubble (" + bubbleSecond + ")");
		assertEquals(1, keysSecond, "the last carriage's key is still on the stage");
	}
}

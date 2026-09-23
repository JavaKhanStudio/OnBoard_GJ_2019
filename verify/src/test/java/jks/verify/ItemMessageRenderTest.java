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

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.math.Vector3;

import jks.amain.Main_Application;
import jks.index.Index_Text;
import jks.vinterface.tools.DialogBubble;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * What Ross says when something is used on an item (r81). The cage in carriage 1 has a line for
 * each of its two choices; until r81 they sat in wa1.wa and nothing ever showed them.
 *
 * Uses the sac on the cage the way a click does - GameItem.tryTouch with the sac selected - and
 * checks the bubble types message 1, then gives way to the thought of the next step (r73).
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ItemMessageRenderTest
{
	private static final double USE_AT   = 2.0;
	/** Long enough for the bubble to fade in and the line to be typed out. */
	private static final double SHOWN_AT = USE_AT + 5.0;

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;

	private static volatile BufferedImage before, said;
	private static volatile String expected, shownText, afterText;
	private static volatile float shownAlpha;
	private static volatile double goneAt;
	private static volatile Throwable error;

	@BeforeAll
	void useTheSacOnTheCage()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - item message verification");
		gl.useVsync(false);

		boolean[] done = {false, false, false};

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 100_000);
		harness.exitAfterSeconds = 60;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				if (!done[0] && harness.gameSeconds >= USE_AT)
				{
					done[0] = true;
					before = GameHarness.grab();
					Frames.write(before, new File(OUTPUT, "item-message-before.png"));

					GameItem sac = item("sac.png"), cage = item("cage.png");
					expected = Index_Text.get(cage.message_Crucial_1);
					GVars_Game.pickItem(sac);
					GVars_Game.selectedItem = sac;
					Vector3 middle = new Vector3(cage.posX + cage.objectTexture.getWidth() / 2f,
						cage.posY + cage.objectTexture.getHeight() / 2f, 0);
					cage.tryTouch(middle, sac, false);
					// How long it should stay, from the moment it was said.
					// The line has been read and faded, and the next thought has begun.
					goneAt = harness.gameSeconds + expected.length() / 30.0 + DialogBubble.READING_SECONDS + 1.0;
				}
				else if (done[0] && !done[1] && harness.gameSeconds >= SHOWN_AT)
				{
					done[1] = true;
					shownText = GVars_Game.dialogBubble.getText();
					shownAlpha = GVars_Game.dialogBubble.getColor().a;
					said = GameHarness.grab();
					Frames.write(said, new File(OUTPUT, "item-message-said.png"));
				}
				else if (done[1] && !done[2] && harness.gameSeconds >= goneAt)
				{
					done[2] = true;
					afterText = GVars_Game.dialogBubble.getText();
					harness.exitAfterSeconds = harness.gameSeconds + 0.2;
				}
			}
			catch (Throwable t)
			{
				error = t;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	private static GameItem item(String name)
	{
		for (GameItem item : GVars_Game.currentLevel.listItems)
			if (name.equals(item.name)) return item;
		throw new AssertionError(name + " is not in carriage " + GVars_Game.currentLevelInt);
	}

	@Test
	@DisplayName("using the sac on the cage types the cage's first line into the bubble")
	void theCageSpeaks()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertNotNull(said, "no frame was taken after the sac was used");

		assertTrue(expected.startsWith("Voila qui devrait remplir son estomac"),
			"wa1.cage.message1 is not the line it was: " + expected);
		assertEquals(expected, shownText, "the bubble is not saying the cage's line");
		assertEquals(1f, shownAlpha, 0.01f, "the bubble is not fully shown");
		assertTrue(Frames.difference(before, said) > 0.002,
			"the frame did not change when the line was said - the bubble is not drawn");
	}

	/**
	 * The cage gives key piece 3, so once its line has been read Ross thinks of the next step:
	 * the hint of piece 1, the first he still lacks (r73). It replaces the line, not joins it.
	 */
	@Test
	@DisplayName("once the line has been read, Ross thinks of the next step instead")
	void theLineMakesWayForTheNextStep()
	{
		assertEquals(Index_Text.get("wa1.hint1"), afterText,
			"after the cage's line the bubble should hold the thought of the first missing piece");
	}
}

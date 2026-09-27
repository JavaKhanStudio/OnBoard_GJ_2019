package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector3;

import jks.amain.Main_Application;
import jks.index.Index_Interface;
import jks.index.Index_Text;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GameItem;

/**
 * A choice is not taken back (r160). Feeds the bird in carriage 1 - the seeds on the cage, its
 * first use - then holds the cage's key to it, the way a click does. Before r160 that second use
 * went through: the cage opened on a fed bird, karma rose and key piece 3 was won a second time.
 * Now nothing changes, the key stays in hand, and Ross says wa1.cage.after1.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChoiceStaysMadeRenderTest
{
	private static final String KEY = "wa1.cage.after1";

	private static final double FEED_AT  = 2.0;
	/** After the feeding line has been read: the refusal is said over nothing. */
	private static final double TRY_AT   = FEED_AT + 6.0;
	/** Long enough for the line to be typed out, and short of its fading: a short line goes after 4.5 s. */
	private static final double SHOWN_AT = TRY_AT + 2.5;

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;

	private static volatile BufferedImage fed, refused;
	private static volatile String pieceLineFed, pieceLineAfter, shownText;
	private static volatile int karmaFed = -1, karmaAfter = -1;
	private static volatile boolean keyStillHeld;
	private static volatile Texture textureFed, textureAfter, textureOpen;
	private static volatile Throwable error;

	@BeforeAll
	void feedTheBirdThenTryToFreeIt()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = 1;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - choice stays made verification");
		gl.useVsync(false);

		boolean[] done = {false, false, false};

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 60;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				GameItem cage = item("cage.png");
				if (!done[0] && harness.gameSeconds >= FEED_AT)
				{
					done[0] = true;
					GameItem sac = item("sac.png"), clefCage = item("clefCage.png");
					// Both in the bar: the seeds are used, the key is kept for later.
					GVars_Game.pickItem(clefCage);
					GVars_Game.pickItem(sac);
					GVars_Game.selectedItem = sac;
					cage.tryTouch(middle(cage), sac, false);
					textureFed = cage.objectTexture;
					karmaFed = GVars_Game.karma;
					pieceLineFed = GVars_Game.clef.lineOf(3);
					textureOpen = Index_Interface.manager.get("game/wagon/wa1/" + cage.path_EtatApres_2, Texture.class);
				}
				else if (done[0] && !done[1] && harness.gameSeconds >= TRY_AT)
				{
					done[1] = true;
					fed = GameHarness.grab();
					Frames.write(fed, new File(OUTPUT, "choice-stays-fed.png"));
					GameItem clefCage = item("clefCage.png");
					GVars_Game.selectedItem = clefCage;
					cage.tryTouch(middle(cage), clefCage, false);
				}
				else if (done[1] && !done[2] && harness.gameSeconds >= SHOWN_AT)
				{
					done[2] = true;
					shownText = GVars_Game.dialogBubble.getText();
					textureAfter = cage.objectTexture;
					karmaAfter = GVars_Game.karma;
					pieceLineAfter = GVars_Game.clef.lineOf(3);
					keyStillHeld = GVars_Game.playerInventory.contains(item("clefCage.png"));
					refused = GameHarness.grab();
					Frames.write(refused, new File(OUTPUT, "choice-stays-refused.png"));
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

	private static Vector3 middle(GameItem item)
	{
		return new Vector3(item.posX + item.objectTexture.getWidth() / 2f,
			item.posY + item.objectTexture.getHeight() / 2f, 0);
	}

	private static GameItem item(String name)
	{
		for (GameItem item : GVars_Game.currentLevel.listItems)
			if (name.equals(item.name)) return item;
		throw new AssertionError(name + " is not in carriage " + GVars_Game.currentLevelInt);
	}

	@Test
	@DisplayName("the cage's key held to a fed bird changes nothing: same cage, same karma, same key piece")
	void theChoiceStands()
	{
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertNotNull(refused, "no frame was taken after the key was tried");

		assertTrue(textureFed != textureOpen, "feeding the bird already opened the cage");
		assertSame(textureFed, textureAfter, "the cage changed when its key was tried on the fed bird");
		assertEquals(karmaFed, karmaAfter, "trying the other use counted karma");
		assertEquals(pieceLineFed, pieceLineAfter, "trying the other use won key piece 3 again");
		assertTrue(keyStillHeld, "the refused key left the bar");
	}

	@Test
	@DisplayName("and Ross says why, in the bubble")
	void rossSaysWhy()
	{
		assertNotNull(refused, "no frame was taken after the key was tried");
		assertEquals(Index_Text.get(KEY), shownText, "the bubble is not saying " + KEY);
		assertTrue(Frames.difference(fed, refused) > 0.002,
			"the frame did not change when the line was said - the bubble is not drawn");
	}
}

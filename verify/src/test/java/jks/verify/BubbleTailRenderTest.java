package jks.verify;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;
import jks.index.Index_Text;
import jks.vinterface.tools.DialogBubble;
import jks.vue.models.game.GVars_Game;

/**
 * Ross stands in front of his thought's tail (r249). The bubble is on the stage, drawn over the
 * whole level, so when it slid down beside the taller Ross of carriage 2 its puffs crossed his
 * face. The tail is now drawn before him.
 *
 * Ross at the right of carriage 2, thinking: the cloud cannot go further right than the screen's
 * edge, so setBubblePosition slides it left and its puffs land on his face. One frame with the
 * cloud, one once it has faded: a pixel the painted tail covers but the frame did not change at
 * is a pixel Ross drew over it. Behind him, 37% of the tail's samples are; drawn over him, 5%,
 * which is the moving backdrop seen through the window.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BubbleTailRenderTest
{
	private static final double SAY_AT = 2.0;
	private static final double LOOK_AT = SAY_AT + 1.5;
	private static final double FADE_AT = LOOK_AT + 0.1;
	private static final double LOOK_AGAIN_AT = FADE_AT + 0.8;

	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");
	private static final File CLOUD = new File(new File(System.getProperty("onboard.verify")).getParentFile(),
		"desktop/assets/tools/dialog/bubble_think_2.png");

	private static GameHarness harness;
	private static volatile Throwable error;

	private static volatile BufferedImage thinking, faded;
	private static volatile float[] box;
	private static volatile boolean reversed;

	@BeforeAll
	void thinkBesideRoss()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = 2;
		GVars_Game.thinkAtEachStep = false;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - bubble tail verification");
		gl.useVsync(false);

		int[] step = {0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 30;
		harness.frameHook = frame ->
		{
			if (error != null) return;
			try
			{
				double t = harness.gameSeconds;
				DialogBubble bubble = GVars_Game.dialogBubble;
				if (step[0] == 0 && t >= SAY_AT)
				{
					step[0] = 1;
					GVars_Game.ross.position.x = 1300;
					bubble.applyText(Index_Text.get("wa2.hint1"));
				}
				else if (step[0] == 1 && t >= LOOK_AT)
				{
					step[0] = 2;
					box = bubble.cloudBox();
					reversed = bubble.isReversed();
					thinking = GameHarness.grab();
					Frames.write(thinking, new File(OUTPUT, "bubble-tail-behind-ross.png"));
				}
				else if (step[0] == 2 && t >= FADE_AT)
				{
					step[0] = 3;
					bubble.makeDisappear();
				}
				else if (step[0] == 3 && t >= LOOK_AGAIN_AT)
				{
					step[0] = 4;
					faded = GameHarness.grab();
					Frames.write(faded, new File(OUTPUT, "bubble-tail-faded.png"));
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

	@Test
	@DisplayName("Ross hides part of his thought's tail")
	void rossInFrontOfTheTail() throws Exception
	{
		if (harness.error != null) harness.error.printStackTrace();
		if (error != null) error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error);
		assertNull(error, error == null ? null : "a step threw: " + error);
		assertTrue(thinking != null && faded != null, "the frames were not taken");

		// The painting's lower third - its tail, under DialogBubble.TAIL_TOP - laid on the screen
		// where the cloud is: a square box[2] wide, from box[1] up, mirrored when reversed.
		BufferedImage cloud = ImageIO.read(CLOUD);
		int side = cloud.getWidth(), from = Math.round(side * 800f / 1200f);
		float scale = box[2] / side;
		int h = thinking.getHeight(), painted = 0, hidden = 0;
		for (int sy = from; sy < side; sy++)
			for (int sx = 0; sx < side; sx++)
			{
				if ((cloud.getRGB(sx, sy) >>> 24) < 250) continue;
				int x = Math.round(box[0] + (reversed ? side - 1 - sx : sx) * scale);
				int y = Math.round(h - 1 - (box[1] + (side - 1 - sy) * scale));
				if (x < 0 || y < 0 || x >= thinking.getWidth() || y >= h) continue;
				painted++;
				if (same(thinking.getRGB(x, y), faded.getRGB(x, y))) hidden++;
			}
		System.out.println("r249 tail: " + painted + " painted samples on screen, " + hidden + " left unchanged by it");
		assertTrue(painted > 1000, "the tail is not on the screen: the frame proves nothing");
		assertTrue(hidden > painted / 5, "Ross hid " + hidden + " of the tail's " + painted
			+ " samples: it is drawn over him");
	}

	private static boolean same(int a, int b)
	{
		for (int shift = 0; shift <= 16; shift += 8)
			if (Math.abs(((a >> shift) & 0xff) - ((b >> shift) & 0xff)) > 6) return false;
		return true;
	}
}

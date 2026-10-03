package jks.verify;

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
import com.badlogic.gdx.math.Vector3;

import jks.amain.Main_Application;
import jks.camera.GVars_Camera;
import jks.input.GVars_Inputs;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.GVars_Personnage;
import jks.vue.models.game.GameItem;

/**
 * Walks Ross to each end of carriage 1 and lets go of the key the moment he stops (r252). The
 * view used to pan at half his speed whatever he did, so a walk to his last step left the
 * view ~200 world units short of the carriage's end: the block with the O (cube.png, the last
 * item of wa1) was cut in half unless the player kept pushing against the wall. The view must
 * show the whole cube when Ross is at the end, and the carriage's start when he is back.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CameraFollowsRossTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");
	private static final double GIVE_UP_AT = 25;

	private GameHarness harness;
	private volatile Throwable error;
	private volatile BufferedImage atEnd, atStart;
	private volatile String endSeen, startSeen;
	private volatile float cubeLeft, cubeRight, endViewLeft, endViewRight, startViewLeft;

	@BeforeAll
	void walkToTheCube()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = 1;
		GVars_Game.thinkAtEachStep = false;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - camera follows Ross verification");
		gl.useVsync(false);

		int[] step = {0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = GIVE_UP_AT;
		harness.frameHook = frame ->
		{
			if (error != null || step[0] == 4) return;
			try
			{
				float ross = GVars_Game.ross.position.x;
				if (step[0] == 0 && harness.gameSeconds >= 2.5)
				{
					GVars_Inputs.rightPressed = true;
					step[0] = 1;
				}
				// Let go the frame he reaches his last step, as a player does who sees him stop.
				else if (step[0] == 1 && ross >= GVars_Personnage.maxPositionX)
				{
					GVars_Inputs.rightPressed = false;
					GameItem cube = cube();
					cubeLeft = cube.posX;
					cubeRight = cube.posX + cube.objectTexture.getWidth();
					endViewLeft = viewLeft();
					endViewRight = viewRight();
					endSeen = "Ross at " + ross + ", view " + endViewLeft + ".." + endViewRight
						+ ", cube " + cubeLeft + ".." + cubeRight;
					atEnd = GameHarness.grab();
					GVars_Inputs.leftPressed = true;
					step[0] = 2;
				}
				else if (step[0] == 2 && ross <= GVars_Personnage.minPositionX)
				{
					GVars_Inputs.leftPressed = false;
					startViewLeft = viewLeft();
					startSeen = "Ross at " + ross + ", view " + startViewLeft + ".." + viewRight();
					atStart = GameHarness.grab();
					step[0] = 4;
					Gdx.app.exit();
				}
			}
			catch (Throwable t)
			{
				if (error == null) error = t;
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	private static GameItem cube()
	{
		for (GameItem item : GVars_Game.currentLevel.listItems)
			if ("cube.png".equals(item.name)) return item;
		throw new AssertionError("wa1 has no cube.png");
	}

	private static float viewLeft()  { return GVars_Camera.camera.unproject(new Vector3(0, 0, 0)).x; }
	private static float viewRight() { return GVars_Camera.camera.unproject(new Vector3(Gdx.graphics.getWidth(), 0, 0)).x; }

	@Test
	@DisplayName("Ross at the end of carriage 1 has the block with the O in view, and the start when he is back")
	void viewFollowsRoss() throws Exception
	{
		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error);
		assertNull(error, error == null ? null : "the walk threw: " + error);
		assertNotNull(atEnd, "Ross never reached his last step on the right within " + GIVE_UP_AT + " s");
		Frames.write(atEnd, new File(OUTPUT, "camera-follows-end.png"));
		assertTrue(cubeLeft >= endViewLeft && cubeRight <= endViewRight + 0.5f,
			"the block with the O is not all in view when Ross stops at the end: " + endSeen);
		assertNotNull(atStart, "Ross never walked back to the start: " + endSeen);
		Frames.write(atStart, new File(OUTPUT, "camera-follows-start.png"));
		assertTrue(Math.abs(startViewLeft) < 0.5f, "the view is not on the carriage's start when Ross is: " + startSeen);
	}
}

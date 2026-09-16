package jks.verify;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.badlogic.gdx.math.Vector3;

import jks.amain.Main_Application;
import jks.camera.GVars_Camera;
import jks.input.GVars_Inputs;
import jks.vue.models.game.GVars_Game;

/**
 * Walks carriage 3 from one end to the other in a window that is not 1600x900 (r64, notice n14).
 * The carriage art used to be sized from the window while the camera clamps, the items and Ross
 * were in pixels of a 1600x900 one: at 1280x720 the room ended a third of a screen before the
 * camera did, at 1920x1080 its last 448 px could not be reached, Ross's head left the top, and
 * the first Left press jumped the view 160 px.
 *
 * Only a launch size is checked. A window resized under a running carriage (the options screen)
 * was the other half of n14, but cage neither opens a window larger than its 1280x720 output nor
 * resizes one, so no test here can make Gdx.graphics change size mid-game.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CarriageGeometryTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");
	/** Carriage 3 has the tallest Ross: the soldier, whose head used to leave a 720-high world. */
	private static final int CARRIAGE = 3;
	private static final double GIVE_UP_AT = 20;
	/** The world the carriage was laid out in, and the carriage's length in it. Written out rather
	 *  than read from the game, so the same test says what was wrong before the fix. */
	private static final float WORLD_HEIGHT = 900, CARRIAGE_LENGTH = 3000;

	private static final int WIDTH = 1280, HEIGHT = 720;

	private GameHarness harness;
	private final List<String> seen = new ArrayList<>();
	private volatile Throwable error;
	private volatile boolean finished;
	private volatile BufferedImage leftEnd, rightEnd;

	@BeforeAll
	void walkTheCarriage()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		Main_Application.startLevel = CARRIAGE;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(WIDTH, HEIGHT);
		gl.setTitle("On Board - carriage geometry verification");
		gl.useVsync(false);

		int[] step = {0};
		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, 1_000_000);
		harness.exitAfterSeconds = GIVE_UP_AT + 1;
		harness.captureAfterSeconds = GIVE_UP_AT;
		harness.frameHook = frame ->
		{
			if (error != null || finished) return;
			try
			{
				float x = GVars_Camera.camera.position.x;
				if (step[0] == 0 && harness.gameSeconds >= 1.0)
				{
					record("the window is " + WIDTH + "x" + HEIGHT + " (got " + Gdx.graphics.getWidth() + "x"
						+ Gdx.graphics.getHeight() + ")", Gdx.graphics.getWidth() == WIDTH && Gdx.graphics.getHeight() == HEIGHT);
					record("the view opens on the start of the carriage (left edge at x " + viewLeft() + ")", near(viewLeft(), 0));
					record("the view is the whole world's height (top at y " + viewTop() + ")", near(viewTop(), WORLD_HEIGHT));
					float head = GVars_Game.ross.position.y + GVars_Game.ross.getFrameHeight();
					record("Ross's head is inside the view (top at " + head + " of " + viewTop() + ")", head > 0 && head <= viewTop());
					GVars_Inputs.leftPressed = true;
					step[0] = 1;
				}
				else if (step[0] == 1 && harness.gameSeconds >= 1.6)
				{
					GVars_Inputs.leftPressed = false;
					record("Left at the start of the carriage does not move the view (left edge at " + viewLeft() + ")", near(viewLeft(), 0));
					leftEnd = GameHarness.grab();
					GVars_Inputs.rightPressed = true;
					step[0] = 2;
				}
				else if (step[0] == 2 && (x >= CARRIAGE_LENGTH || harness.gameSeconds >= GIVE_UP_AT - 1))
				{
					// Held a little past the clamp, so a frame that overshoots and is pulled back shows.
					if (harness.gameSeconds < GIVE_UP_AT - 1 && !near(viewRight(), CARRIAGE_LENGTH))
						return;
					step[0] = 3;
					due = harness.gameSeconds + 0.5;
				}
				else if (step[0] == 3 && harness.gameSeconds >= due)
				{
					GVars_Inputs.rightPressed = false;
					record("Right stops with the view's right edge on the end of the carriage (at " + viewRight()
						+ " of " + CARRIAGE_LENGTH + ")", near(viewRight(), CARRIAGE_LENGTH));
					rightEnd = GameHarness.grab();
					finished = true;
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

	private double due;

	private static float viewLeft()  { return unproject(0, 0).x; }
	private static float viewRight() { return unproject(Gdx.graphics.getWidth(), 0).x; }
	private static float viewTop()   { return unproject(0, 0).y; }

	/** A screen point (y down, as input reports it) in world units. */
	private static Vector3 unproject(float x, float y)
	{
		return GVars_Camera.camera.unproject(new Vector3(x, y, 0));
	}

	private static boolean near(float a, float b)
	{
		return Math.abs(a - b) < 1f;
	}

	private void record(String what, boolean ok)
	{
		seen.add((ok ? "ok   " : "FAIL ") + what);
	}

	@Test
	@DisplayName("the view pans from one end of the carriage to the other, no further, and Ross fits in it")
	void theCarriageFitsTheView() throws Exception
	{
		String report = String.join("\n", seen);
		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error, harness.error == null ? null : "the game threw: " + harness.error + "\n" + report);
		assertNull(error, error == null ? null : "a step threw: " + error + "\n" + report);
		assertTrue(finished, "the walk did not reach the end of the carriage:\n" + report);
		for (String line : seen)
			assertTrue(line.startsWith("ok"), report);

		assertNotNull(rightEnd, "the far end was never captured");
		Frames.write(leftEnd, new File(OUTPUT, "carriage-left-end.png"));
		File right = new File(OUTPUT, "carriage-right-end.png");
		Frames.write(rightEnd, right);

		// The carriage reaches the right edge of the screen: the last columns above the bar are
		// the art, not the white the game clears to (what a 1280x720 window used to show there).
		int bar = rightEnd.getHeight() / 9, white = 0, total = 0;
		for (int y = 0; y < rightEnd.getHeight() - bar; y++)
			for (int x = rightEnd.getWidth() - 4; x < rightEnd.getWidth(); x++, total++)
				if ((rightEnd.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF) white++;
		assertTrue(white < total / 20, white + " of the " + total + " pixels on the right edge are bare white - "
			+ "the carriage stops short of the screen. See " + right.getAbsolutePath());
	}
}

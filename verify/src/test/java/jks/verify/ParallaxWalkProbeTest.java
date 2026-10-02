package jks.verify;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
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

import jks.amain.Main_Application;
import jks.camera.GVars_Camera;
import jks.input.GVars_Inputs;
import jks.tools2d.parallax.ParallaxLayer;
import jks.tools2d.parallax.heart.Parallax_Heart;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.WagonLevel;

/**
 * A probe, not a gate (r234): walks Ross right, stands, walks left in carriage 1 and writes,
 * frame by frame, where the view is and how far each backdrop layer has scrolled, to
 * build/frames/parallax-walk.csv, with a few grabs beside it. Run it with
 * ./gradlew :verify:test -PwithGl --tests jks.verify.ParallaxWalkProbeTest
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParallaxWalkProbeTest
{
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;
	private static Steps steps;
	private static final List<String> rows = new ArrayList<>();
	private static volatile String phase = "wait";
	private static int grabs;
	private static String ratios;

	@BeforeAll
	void walkCarriage1()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;
		GVars_Game.thinkAtEachStep = false;

		Lwjgl3ApplicationConfiguration gl = new Lwjgl3ApplicationConfiguration();
		gl.setWindowedMode(1280, 720);
		gl.setTitle("On Board - parallax walk probe");
		gl.useVsync(false);
		gl.setForegroundFPS(60);

		steps = new Steps(2.5, 1.0);
		steps.add(1.5, () -> phase = "stand");
		steps.add(2.0, () -> { phase = "right"; GVars_Inputs.rightPressed = true; });
		steps.add(1.5, () -> { phase = "stand2"; GVars_Inputs.rightPressed = false; });
		steps.add(2.0, () -> { phase = "left"; GVars_Inputs.leftPressed = true; });
		steps.add(0.5, () -> { phase = "end"; GVars_Inputs.leftPressed = false; });

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = steps.seconds() + 0.2;
		steps.drive(harness);
		java.util.function.IntConsumer drive = harness.frameHook;
		harness.frameHook = frame ->
		{
			drive.accept(frame);
			if (phase.equals("wait")) return;
			try
			{
				record(frame);
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		};

		new Lwjgl3Application(harness, gl);
	}

	private static void record(int frame) throws Exception
	{
		Field f = WagonLevel.class.getDeclaredField("parallax");
		f.setAccessible(true);
		Parallax_Heart heart = (Parallax_Heart) f.get(GVars_Game.currentLevel);
		if (ratios == null)
		{
			StringBuilder r = new StringBuilder("# speed ratio of each layer:");
			for (ParallaxLayer layer : heart.parallaxReader.layers)
				r.append(' ').append(layer.getParallaxSpeedRatioX());
			ratios = r.toString();
		}
		StringBuilder row = new StringBuilder();
		row.append(frame).append(',').append(String.format("%.4f", harness.gameSeconds)).append(',')
			.append(String.format("%.4f", Gdx.graphics.getDeltaTime())).append(',').append(phase).append(',')
			.append(String.format("%.2f", GVars_Camera.camera.position.x)).append(',')
			.append(String.format("%.2f", GVars_Game.ross.position.x)).append(',')
			.append(String.format("%.3f", heart.getWorldWidth()));
		for (ParallaxLayer layer : heart.parallaxReader.layers)
			row.append(',').append(String.format("%.4f", layer.getScrollX()));
		rows.add(row.toString());
		if (frame % 20 == 0 && grabs < 30)
		{
			grabs++;
			Frames.write(GameHarness.grab(), new File(OUTPUT, String.format("parallax-walk-%04d-%s.png", frame, phase)));
		}
	}

	@Test
	@DisplayName("logs the view and every backdrop layer while Ross walks")
	void logs() throws Exception
	{
		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error);
		assertNull(steps.error);
		OUTPUT.mkdirs();
		try (PrintWriter out = new PrintWriter(new File(OUTPUT, "parallax-walk.csv")))
		{
			out.println(ratios);
			out.println("frame,seconds,delta,phase,cameraX,rossX,plaxWorldWidth,layers...");
			for (String row : rows) out.println(row);
		}
	}
}

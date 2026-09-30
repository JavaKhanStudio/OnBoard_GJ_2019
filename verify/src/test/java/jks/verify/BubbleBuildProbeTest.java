package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import jks.amain.Main_Application;
import jks.vinterface.tools.DialogBubble;
import jks.vinterface.tools.DialogBubble.DialogSize;

/**
 * Both of DialogBubble's constructors on a fixed clock (r205): each bubble is built, given a
 * line with {WAVE} in it, and stepped and drawn 90 times by 1/60 s on a stage of its own, so
 * nothing on the window's clock reaches it. What each built - its colour, its
 * children, their sizes, places and text - goes to build/frames/bubble-probe.txt and the frames
 * to bubble-probe-hint.png (the game's) and bubble-probe-dialog.png (DialogRenderTest's). Run it
 * before and after a change to how the bubble is built: nothing may differ by a byte.
 */
@Tag("gl")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BubbleBuildProbeTest
{
	private static final String LINE = "Le pauvre, seul dans cette {WAVE}cage{ENDWAVE}. Il me regarde comme s'il avait faim.";
	private static final File OUTPUT = new File(new File(System.getProperty("onboard.verify")), "build/frames");

	private static GameHarness harness;
	private static final List<String> dump = new ArrayList<>();
	private static volatile int done;

	@BeforeAll
	void buildBoth()
	{
		Main_Application.startPoint = Main_Application.StartPoint.GAME;

		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1280, 720);
		config.setTitle("On Board - bubble build probe");
		config.setResizable(false);
		config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);
		config.useVsync(false);

		harness = new GameHarness(new Main_Application(), Integer.MAX_VALUE, Integer.MAX_VALUE);
		harness.exitAfterSeconds = 60;
		harness.frameHook = frame ->
		{
			if (done != 0 || harness.gameSeconds < 1.5) return;
			try
			{
				DialogBubble hint = new DialogBubble(DialogSize.BUBBLE_LARGE_TEXT_MEDIUM);
				record("hint, built", hint);
				hint.applyText(LINE);
				shoot("hint", hint);

				DialogBubble dialog = new DialogBubble("{COLOR=black}" + LINE, DialogSize.BUBBLE_LARGE_TEXT_LARGE, true);
				record("dialog, built", dialog);
				shoot("dialog", dialog);
			}
			catch (java.io.IOException e)
			{
				throw new RuntimeException(e);
			}
			done = 1;
			Gdx.app.exit();
		};
		new Lwjgl3Application(harness, config);
	}

	private static void shoot(String name, DialogBubble bubble) throws java.io.IOException
	{
		Stage stage = new Stage(new ScreenViewport());
		stage.addActor(bubble);
		bubble.setBubblePosition(560, 340);
		// Drawn after every step, as the game does: the label lays its line out as it draws,
		// and steps taken before it ever has type nothing.
		for (int i = 0; i < 90; i++)
		{
			stage.act(1 / 60f);
			Gdx.gl.glClearColor(0.2f, 0.3f, 0.2f, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			stage.draw();
		}
		record(name + ", after 1.5 s", bubble);
		Frames.write(GameHarness.grab(), new File(OUTPUT, "bubble-probe-" + name + ".png"));
		bubble.remove();
		stage.dispose();
	}

	private static void record(String what, DialogBubble bubble)
	{
		dump.add(what + ": textWhenVisible=" + bubble.textWhenVisible + " " + describe(bubble));
		for (Actor child : bubble.getChildren())
			dump.add("  " + child.getClass().getSimpleName() + " " + describe(child)
				+ (child instanceof com.github.tommyettinger.textra.TypingLabel
					? " text=" + ((com.github.tommyettinger.textra.TypingLabel) child).getOriginalText()
						+ " typed=" + ((com.github.tommyettinger.textra.TypingLabel) child).length() + " ended=" + ((com.github.tommyettinger.textra.TypingLabel) child).hasEnded() : ""));
	}

	private static String describe(Actor a)
	{
		return String.format("x=%s y=%s w=%s h=%s color=%s visible=%s", a.getX(), a.getY(), a.getWidth(), a.getHeight(),
			a.getColor(), a.isVisible());
	}

	@Test
	@DisplayName("both constructors build, type and draw on a fixed clock")
	void probed() throws Exception
	{
		if (harness.error != null) harness.error.printStackTrace();
		assertNull(harness.error);
		assertEquals(1, done, "the game never got far enough to build a bubble");
		Files.write(new File(OUTPUT, "bubble-probe.txt").toPath(), dump, StandardCharsets.UTF_8);
	}
}

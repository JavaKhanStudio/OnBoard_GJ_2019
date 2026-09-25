package jks.vue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;

import jks.camera.GVars_Camera;
import jks.index.Index_Interface;
import jks.input.GVars_Inputs;

/**
 * The steam wipe out of the intro's last page (r85): the painted fumee frames rise from the
 * bottom right until they cover the screen, the change runs once it is covered, and the same
 * frames play backwards to lift the steam off the next screen.
 *
 * Main_Application updates and draws it over every view, like GVars_Fade, so it lasts across
 * the changeVue.
 *
 * A carriage whose key is whole leaves the same way, but waits for Ross's last line (r91): with
 * {@link #creep} the steam covers at its own speed, then holds for as long as the line still
 * needs to be read, and a click anywhere cuts the hold short. The bubble saying it is drawn over
 * the steam until the steam has lifted, so what he says is never hidden (r118). For the whole of
 * a creep the keyboard and the mouse reach nothing else, as under GVars_Fade.
 */
public class GVars_Steam
{
	/** The frames are painted as a short flip-book: twelve a second, like the 2019 animations. */
	public static final float FRAME_SECONDS = 1f / 12f ;
	/** How long the covered screen holds before the steam lifts. */
	public static final float HOLD_SECONDS = 0.25f ;

	static Array<AtlasRegion> frames ;
	/** Seconds into the current phase. */
	static float time ;
	/** 1 while covering, -1 while lifting, 0 when there is no steam. */
	static int direction ;
	static Runnable atCovered ;
	/** How long the covered screen holds: HOLD_SECONDS, or the reading left for a creep. */
	static float holdFor ;
	/** Waiting on a line to be read: a click cuts it short. */
	static boolean creeping ;
	/** Drawn over the steam while it runs: the bubble of the line being read (r118). */
	static Actor over ;

	/** The view's own input, put back when a creep's steam has lifted. */
	static InputProcessor held ;
	/** A click anywhere hurries a creep; a key going up still reaches the view, as under GVars_Fade. */
	static final InputProcessor blocker = new InputAdapter()
	{
		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button)
		{
			hurry() ;
			return true ;
		}

		@Override
		public boolean keyUp(int keycode)
		{
			return held != null && held.keyUp(keycode) ;
		}
	} ;

	/** No steam, and nothing kept from an earlier GL context. The GL tests start many games in one JVM. */
	public static void reset()
	{
		frames = null ;
		time = 0 ;
		direction = 0 ;
		atCovered = null ;
		holdFor = 0 ;
		creeping = false ;
		over = null ;
		held = null ;
	}

	public static boolean isRunning()
	{
		return direction != 0 ;
	}

	/**
	 * Covers the screen in steam, runs the change once it is covered, and lifts the steam off
	 * what the change put there. Returns false, doing nothing, when steam is already running.
	 */
	public static boolean through(Runnable change)
	{
		if(isRunning())
			return false ;

		frames = Index_Interface.manager.get(Index_Interface.steamFrames, TextureAtlas.class).findRegions("fumee") ;
		atCovered = change ;
		time = 0 ;
		direction = 1 ;
		holdFor = HOLD_SECONDS ;
		return true ;
	}

	/**
	 * {@link #through}, waiting at least these seconds before the change (r91): the pause in
	 * which the last thing said in a carriage is read. The steam covers at its own speed and the
	 * covered screen holds for the rest, with the line's bubble drawn over it until the steam
	 * has lifted (r118). Input is held until then, and a click cuts the hold short.
	 */
	public static boolean creep(float seconds, Runnable change, Actor bubble)
	{
		if(!through(change))
			return false ;

		holdFor = Math.max(HOLD_SECONDS, seconds - coverSeconds()) ;
		creeping = holdFor > HOLD_SECONDS ;
		over = bubble ;
		GVars_Inputs.leftPressed = false ;
		GVars_Inputs.rightPressed = false ;
		holdInput() ;
		return true ;
	}

	public static boolean isCreeping()
	{
		return direction > 0 && creeping ;
	}

	/** A creep goes on without waiting for the line: the plain hold once covered. */
	public static void hurry()
	{
		if(!isCreeping())
			return ;

		holdFor = HOLD_SECONDS ;
		creeping = false ;
	}

	private static void holdInput()
	{
		InputProcessor current = Gdx.input.getInputProcessor() ;
		if(current != blocker)
			held = current ;
		Gdx.input.setInputProcessor(blocker) ;
	}

	private static void giveInputBack()
	{
		if(Gdx.input.getInputProcessor() == blocker)
			Gdx.input.setInputProcessor(held) ;
		held = null ;
	}

	private static float coverSeconds()
	{
		return frames.size * FRAME_SECONDS ;
	}

	public static void update(float delta)
	{
		if(direction == 0)
			return ;

		time += delta ;
		if(direction > 0 && time >= coverSeconds() + holdFor)
		{
			time = 0 ;
			direction = -1 ;
			creeping = false ;
			Runnable change = atCovered ;
			atCovered = null ;
			change.run() ;
			// A new view sets its own input in init(): that is the one to give back.
			if(held != null)
				holdInput() ;
		}
		else if(direction < 0 && time >= coverSeconds())
		{
			if(held != null)
				giveInputBack() ;
			reset() ;
		}
	}

	/** Which frame shows now, 0 being the first wisp and size-1 the covered screen; -1 for none. */
	public static int frameIndex()
	{
		if(direction == 0)
			return -1 ;

		if(direction > 0)
			return Math.min((int)(time / FRAME_SECONDS), frames.size - 1) ;
		int step = Math.min((int)(time / FRAME_SECONDS), frames.size - 1) ;
		return frames.size - 1 - step ;
	}

	private static final Matrix4 screen = new Matrix4() ;

	/** Over everything the view drew, the interface included. */
	public static void draw()
	{
		int index = frameIndex() ;
		if(index < 0)
			return ;

		// In screen pixels, whatever the view left on the batch: a carriage leaves its world
		// camera there, and the steam then covered only part of the screen (r120).
		screen.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) ;
		GVars_Camera.staticBatch.setProjectionMatrix(screen) ;
		GVars_Camera.staticBatch.begin() ;
		GVars_Camera.staticBatch.setColor(1, 1, 1, 1) ;
		GVars_Camera.staticBatch.draw(frames.get(index), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) ;
		GVars_Camera.staticBatch.end() ;

		// The line being read stays in front (r118), and fades out on its own after the change.
		// The stage drew it under the steam already; this draws it again, in the stage's space.
		if(over != null && over.getStage() != null && over.isVisible())
		{
			Batch batch = over.getStage().getBatch() ;
			batch.setProjectionMatrix(over.getStage().getCamera().combined) ;
			batch.begin() ;
			over.draw(batch, 1f) ;
			batch.end() ;
		}
	}
}

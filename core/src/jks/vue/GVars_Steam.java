package jks.vue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
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
 * A carriage whose key is whole leaves the same way, but slowly (r91): {@link #creep} stretches
 * the covering over the time Ross's last line needs to be read, fading each frame into the next
 * rather than flipping, and a click anywhere hurries it back to the plain speed. For the whole of
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
	/** How long the covering takes: the frames' own speed, or longer for a creep. */
	static float coverFor ;
	/** Covering slowly: frames blend into each other, and a click hurries it. */
	static boolean creeping ;

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
		coverFor = 0 ;
		creeping = false ;
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
		coverFor = coverSeconds() ;
		return true ;
	}

	/**
	 * {@link #through}, with the covering stretched over at least these seconds (r91): the
	 * pause in which the last thing said in a carriage is read. Input is held until the steam
	 * has lifted, and a click hurries it.
	 */
	public static boolean creep(float seconds, Runnable change)
	{
		if(!through(change))
			return false ;

		coverFor = Math.max(seconds, coverSeconds()) ;
		creeping = coverFor > coverSeconds() ;
		GVars_Inputs.leftPressed = false ;
		GVars_Inputs.rightPressed = false ;
		holdInput() ;
		return true ;
	}

	public static boolean isCreeping()
	{
		return direction > 0 && creeping ;
	}

	/** The rest of a creep at the frames' own speed, from where it is. Skipping, but not a jump. */
	public static void hurry()
	{
		if(!isCreeping())
			return ;

		time = time / coverFor * coverSeconds() ;
		coverFor = coverSeconds() ;
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

	/** Seconds a frame shows while covering: FRAME_SECONDS, or more while creeping. */
	private static float coverFrameSeconds()
	{
		return coverFor / frames.size ;
	}

	public static void update(float delta)
	{
		if(direction == 0)
			return ;

		time += delta ;
		if(direction > 0 && time >= coverFor + HOLD_SECONDS)
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
			return Math.min((int)(time / coverFrameSeconds()), frames.size - 1) ;
		int step = Math.min((int)(time / FRAME_SECONDS), frames.size - 1) ;
		return frames.size - 1 - step ;
	}

	/** How far a creep is into the next frame, 0 to 1, which is drawn over this one that much. */
	private static float blendIntoNext()
	{
		if(!isCreeping())
			return 0 ;
		float into = time / coverFrameSeconds() ;
		return Math.min(into - (int) into, 1) ;
	}

	/** Over everything the view drew, the interface included. */
	public static void draw()
	{
		int index = frameIndex() ;
		if(index < 0)
			return ;

		GVars_Camera.staticBatch.begin() ;
		GVars_Camera.staticBatch.setColor(1, 1, 1, 1) ;
		GVars_Camera.staticBatch.draw(frames.get(index), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) ;
		// Slowed down, a flip-book at two frames a second jerks: the next one fades up instead.
		float blend = blendIntoNext() ;
		if(blend > 0 && index + 1 < frames.size)
		{
			GVars_Camera.staticBatch.setColor(1, 1, 1, blend) ;
			GVars_Camera.staticBatch.draw(frames.get(index + 1), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) ;
			GVars_Camera.staticBatch.setColor(1, 1, 1, 1) ;
		}
		GVars_Camera.staticBatch.end() ;
	}
}

package jks.vue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Array;

import jks.camera.GVars_Camera;
import jks.index.Index_Interface;

/**
 * The steam wipe out of the intro's last page (r85): the painted fumee frames rise from the
 * bottom right until they cover the screen, the change runs once it is covered, and the same
 * frames play backwards to lift the steam off the next screen.
 *
 * Main_Application updates and draws it over every view, like GVars_Fade, so it lasts across
 * the changeVue.
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

	/** No steam, and nothing kept from an earlier GL context. The GL tests start many games in one JVM. */
	public static void reset()
	{
		frames = null ;
		time = 0 ;
		direction = 0 ;
		atCovered = null ;
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
		return true ;
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
		if(direction > 0 && time >= coverSeconds() + HOLD_SECONDS)
		{
			time = 0 ;
			direction = -1 ;
			Runnable change = atCovered ;
			atCovered = null ;
			change.run() ;
		}
		else if(direction < 0 && time >= coverSeconds())
		{
			reset() ;
		}
	}

	/** Which frame shows now, 0 being the first wisp and size-1 the covered screen; -1 for none. */
	public static int frameIndex()
	{
		if(direction == 0)
			return -1 ;

		int step = Math.min((int)(time / FRAME_SECONDS), frames.size - 1) ;
		return direction > 0 ? step : frames.size - 1 - step ;
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
		GVars_Camera.staticBatch.end() ;
	}
}

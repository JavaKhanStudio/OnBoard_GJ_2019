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
import jks.vinterface.tools.DialogBubble;

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
 *
 * While that line is read, a creep no longer sits on a covered screen (r262): the steam rises to
 * its first ring, {@link Ring#frame}, and holds there with the carriage still showing; it closes
 * the rest of the way just before the change, which comes when it always did. {@link #ring} says
 * where the ring rises from; the steam lab (-Donboard.start=steam_lab) compares them.
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

	/**
	 * Where a creep's first ring rises from (r262). WHOLE is the steam before it: the frames
	 * rise all the way and the covered screen holds. The others hold at their {@link #frame},
	 * the painted frames turned over - never stretched - into the screen's other corners. Each
	 * starts on the frame where it covers about a third of the screen.
	 */
	public enum Ring
	{
		/** Covers the screen, then holds (r118). */
		WHOLE(1, 1, 9),
		/** The painted corner alone, bottom right: frame 3 is the last of only pale puffs. */
		CORNER(1, 1, 3),
		/** Both bottom corners: the steam comes up from under the floor. */
		FLOOR(-1, 1, 2),
		/** All four corners: a ring round the screen. */
		FRAME(-1, -1, 1) ;

		/** -1 when the frames are also drawn turned over left to right, and top to bottom. */
		final int mirrorX, mirrorY ;
		/** The frame the ring holds on in the game, 1 the first wisp. */
		public final int shippedFrame ;
		/** The frame it holds on now, 1 to 8; the steam lab moves it. Not saved. */
		public int frame ;

		Ring(int mirrorX, int mirrorY, int shippedFrame)
		{
			this.mirrorX = mirrorX ;
			this.mirrorY = mirrorY ;
			this.shippedFrame = shippedFrame ;
			this.frame = shippedFrame ;
		}
	}

	/** The game's: Floor, held at its frame 2 - Simon's pick in the steam lab (d16, r270). */
	public static final Ring SHIPPED_RING = Ring.FLOOR ;

	/** Where the next creep's ring rises from; the steam lab moves it. Not saved. */
	public static Ring ring = SHIPPED_RING ;
	/** This run's: WHOLE for {@link #through}, {@link #ring} for a creep. */
	static Ring shape = Ring.WHOLE ;

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
		shape = Ring.WHOLE ;
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
		shape = Ring.WHOLE ;
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
		shape = ring ;
		GVars_Inputs.resetInputs() ;
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
		// From a ring, the frames left close before the change: cut short, it swapped the
		// carriage in plain view (r262).
		if(shape != Ring.WHOLE)
			holdFor = Math.max(HOLD_SECONDS, time - coverSeconds() + (frames.size - holdIndex()) * FRAME_SECONDS) ;
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
		{
			int risen = Math.min((int)(time / FRAME_SECONDS), frames.size - 1) ;
			int hold = holdIndex() ;
			if(risen <= hold)
				return risen ;
			// Held at the ring until the frames left only just have time to close (r262).
			float closing = coverSeconds() + holdFor - (frames.size - 1 - hold) * FRAME_SECONDS ;
			if(time < closing)
				return hold ;
			return Math.min(hold + 1 + (int)((time - closing) / FRAME_SECONDS), frames.size - 1) ;
		}
		int step = Math.min((int)(time / FRAME_SECONDS), frames.size - 1) ;
		return frames.size - 1 - step ;
	}

	/** The frame a creep holds on while the line is read: the covered one for WHOLE. */
	private static int holdIndex()
	{
		if(shape == Ring.WHOLE)
			return frames.size - 1 ;
		return Math.max(0, Math.min(shape.frame, frames.size) - 1) ;
	}

	/** Whether the steam covers the whole screen now, on its way to the change. */
	public static boolean isCovered()
	{
		return direction > 0 && frameIndex() == frames.size - 1 ;
	}

	/** Whether the steam stands at its ring now, the line being read over the carriage (r262). */
	public static boolean isAtRing()
	{
		return direction > 0 && shape != Ring.WHOLE && frameIndex() == holdIndex() ;
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
		int w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight() ;
		AtlasRegion frame = frames.get(index) ;
		GVars_Camera.staticBatch.draw(frame, 0, 0, w, h) ;
		// The same frame turned over into the ring's other corners: a negative size flips it.
		if(shape.mirrorX < 0)
			GVars_Camera.staticBatch.draw(frame, w, 0, -w, h) ;
		if(shape.mirrorY < 0)
		{
			GVars_Camera.staticBatch.draw(frame, 0, h, w, -h) ;
			GVars_Camera.staticBatch.draw(frame, w, h, -w, -h) ;
		}
		GVars_Camera.staticBatch.end() ;

		// The line being read stays in front (r118), and fades out on its own after the change.
		// The stage drew it under the steam already; this draws it again, in the stage's space.
		if(over != null && over.getStage() != null && over.isVisible())
		{
			// Its tail too, once Ross is under the steam: a carriage draws it apart, before
			// him (r249), and the covered screen showed a cloud cut flat at its bottom.
			if(over instanceof DialogBubble && index >= frames.size - 2)
				((DialogBubble) over).drawTail(over.getStage()) ;

			Batch batch = over.getStage().getBatch() ;
			batch.setProjectionMatrix(over.getStage().getCamera().combined) ;
			batch.begin() ;
			over.draw(batch, 1f) ;
			batch.end() ;
		}
	}
}

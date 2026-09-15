package jks.vue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import jks.input.GVars_Inputs;

/**
 * The fade to black between the menu and the game, and between two carriages (r44). The screen
 * darkens, the change happens once it is black, and the new screen comes up out of the black.
 * The timings are the story pages' (Vue_Scenematic_Intro): one second out, two in.
 *
 * Main_Application updates and draws it over every view, so it lasts across a changeVue. For
 * the whole fade the keyboard and the mouse reach nothing but key releases, so a click cannot
 * pick an item in a carriage that is going away and Enter cannot start the game twice.
 */
public class GVars_Fade
{
	public static final float OUT_SECONDS = 1f ;
	public static final float IN_SECONDS = 2f ;

	/** 0 is clear, 1 is black. */
	static float alpha ;
	/** 1 while darkening, -1 while clearing, 0 when there is no fade. */
	static int direction ;
	static Runnable atBlack ;

	/** The view's own input, put back when the fade is over. */
	static InputProcessor held ;
	/** Lets a key go up, so a key held into the fade does not walk Ross forever after it. */
	static final InputProcessor blocker = new InputAdapter()
	{
		@Override
		public boolean keyUp(int keycode)
		{
			return held != null && held.keyUp(keycode) ;
		}
	} ;

	static ShapeRenderer shapes ;

	/** No fade, and nothing kept from an earlier GL context. The GL tests start many games in one JVM. */
	public static void reset()
	{
		alpha = 0 ;
		direction = 0 ;
		atBlack = null ;
		held = null ;
		shapes = null ;
	}

	public static boolean isFading()
	{
		return direction != 0 ;
	}

	/**
	 * Fades out, runs the change at black, and fades back in. Returns false, doing nothing,
	 * when a fade is already under way.
	 */
	public static boolean through(Runnable change)
	{
		if(isFading())
			return false ;

		atBlack = change ;
		direction = 1 ;
		GVars_Inputs.leftPressed = false ;
		GVars_Inputs.rightPressed = false ;
		holdInput() ;
		return true ;
	}

	public static void update(float delta)
	{
		if(direction > 0)
		{
			alpha += delta / OUT_SECONDS ;
			if(alpha >= 1)
			{
				alpha = 1 ;
				direction = -1 ;
				Runnable change = atBlack ;
				atBlack = null ;
				change.run() ;
				// A new view sets its own input in init(): that is the one to give back.
				holdInput() ;
			}
		}
		else if(direction < 0)
		{
			alpha -= delta / IN_SECONDS ;
			if(alpha <= 0)
			{
				alpha = 0 ;
				direction = 0 ;
				// Unless something changed the view without a fade meanwhile, and set its own.
				if(Gdx.input.getInputProcessor() == blocker)
					Gdx.input.setInputProcessor(held) ;
				held = null ;
			}
		}
	}

	private static void holdInput()
	{
		InputProcessor current = Gdx.input.getInputProcessor() ;
		if(current != blocker)
			held = current ;
		Gdx.input.setInputProcessor(blocker) ;
	}

	/** Over everything the view drew, the interface included. */
	public static void draw()
	{
		if(alpha <= 0)
			return ;

		if(shapes == null)
			shapes = new ShapeRenderer() ;

		shapes.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) ;
		Gdx.gl.glEnable(GL20.GL_BLEND) ;
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA) ;
		shapes.begin(ShapeType.Filled) ;
		shapes.setColor(0, 0, 0, alpha) ;
		shapes.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()) ;
		shapes.end() ;
	}
}

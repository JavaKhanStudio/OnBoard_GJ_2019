package jks.vue.models.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;

import jks.camera.GVars_Camera;
import jks.vinterface.Utils_Board;

/**
 * Where a click sent Ross, when the options' click-to-walk is on (r229): a thin ring in the
 * boards' ink at the point clicked, that opens a little and fades out in about half a second.
 * Gentle (D4): no flash, never more than 0.6 opaque, and gone before he gets there. Cream
 * vanished on the drawings strewn over the floor of carriage 1.
 *
 * Drawn in the world, so it stays on the spot clicked while the view pans after him.
 */
public class ClickMarker
{
	/** How long a ring lasts, in seconds. */
	public static final float LIFE = 0.6f ; 
	/** Its radius as it appears and as it goes, and its line's width, in world units. */
	private static final float FROM_RADIUS = 10f, TO_RADIUS = 26f, LINE = 3f, MAX_ALPHA = 0.6f ; 
	private static final int SEGMENTS = 28 ; 
	
	private static float x, y, age = LIFE ; 
	
	public static void show(float worldX, float worldY)
	{
		x = worldX ; 
		y = worldY ; 
		age = 0 ; 
	}
	
	/** Takes it away at once: a level change or a new run. */
	public static void clear()
	{
		age = LIFE ; 
	}
	
	public static boolean isShowing()
	{
		return age < LIFE ; 
	}
	
	public static void update(float delta)
	{
		if(isShowing())
			age = Math.min(LIFE, age + delta) ; 
	}
	
	public static void draw()
	{
		if(!isShowing())
			return ; 
		
		float t = age / LIFE ; 
		// Eased out: it opens quickly and settles, rather than growing at a steady pace.
		float radius = MathUtils.lerp(FROM_RADIUS, TO_RADIUS, 1 - (1 - t) * (1 - t)) ; 
		float alpha = MAX_ALPHA * (1 - t) ; 
		
		Gdx.gl.glEnable(GL20.GL_BLEND) ; 
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA) ; 
		ShapeRenderer shapes = GVars_Camera.shapeRenderer ; 
		shapes.setProjectionMatrix(GVars_Camera.camera.combined) ; 
		shapes.begin(ShapeType.Filled) ; 
		shapes.setColor(Utils_Board.INK.r, Utils_Board.INK.g, Utils_Board.INK.b, alpha) ; 
		// A ring of thick segments rather than a Line circle: GL line width is 1 px on most
		// drivers, which reads as nothing at 4K.
		for(int i = 0 ; i < SEGMENTS ; i++)
		{
			float a0 = MathUtils.PI2 * i / SEGMENTS, a1 = MathUtils.PI2 * (i + 1) / SEGMENTS ; 
			shapes.rectLine(x + radius * MathUtils.cos(a0), y + radius * MathUtils.sin(a0), 
					x + radius * MathUtils.cos(a1), y + radius * MathUtils.sin(a1), LINE) ; 
		}
		shapes.end() ; 
	}
}

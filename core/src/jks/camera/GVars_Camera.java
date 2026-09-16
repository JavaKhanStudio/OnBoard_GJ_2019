package jks.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer; 

public class GVars_Camera 
{
	public static OrthographicCamera camera;
	
	public static ShapeRenderer shapeRenderer ;
	public static Batch staticBatch ;
	
	public static final float sizeCamCaseX = 26; 
	public static final float sizeCamCaseY = 15; 

	public static float computedAddedMomentum_X ; 
	public static float addedMomentum_X ; 
	
	public static float computedAddedMomentum_Y ; 
	public static float addedMomentum_Y ; 
	
	public static boolean isXAmovible = true;
	public static boolean isYAmovible = false;
	public static float screenMovementSpeed = 7.5f;
	
	public static float worldMutiplier = 1f ;
	
	/**
	 * The carriage's world, whatever the window (r64, notice n14). The items, Ross's height and
	 * standing line, and the pan clamps were all laid out in pixels of a 1600x900 window, while the
	 * carriage art was sized from the live window - so anywhere but 1600x900 the two disagreed: the
	 * room ended before the camera did, Ross's head left the top, the first Left press jumped.
	 * The camera now always sees this much, stretched to the window; the resolution list only
	 * offers 16:9, so the stretch is uniform.
	 */
	public static final float WORLD_WIDTH = 1600 ; 
	public static final float WORLD_HEIGHT = 900 ; 
	
	public static void init()
	{
		staticBatch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer() ; 
		camera = new OrthographicCamera();
		camera.setToOrtho(false, WORLD_WIDTH * GVars_Camera.worldMutiplier, WORLD_HEIGHT * GVars_Camera.worldMutiplier);	
		
	}
	
}
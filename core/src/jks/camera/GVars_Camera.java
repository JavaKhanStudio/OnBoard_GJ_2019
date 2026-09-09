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
	
	public static void init()
	{
		staticBatch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer() ; 
		camera = new OrthographicCamera();
		camera.setToOrtho(false, Gdx.graphics.getWidth() * GVars_Camera.worldMutiplier, Gdx.graphics.getHeight() * GVars_Camera.worldMutiplier);	
		
	}
	
}
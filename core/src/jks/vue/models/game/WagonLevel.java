package jks.vue.models.game;

import static jks.camera.GVars_Camera.staticBatch;
import static jks.index.Index_Interface.manager;
import static jks.index.Index_Interface.loadTexture;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jks.camera.GVars_Camera;
import jks.input.IKM_Game_Keyboard;
import jks.tools2d.parallax.heart.Parallax_Heart;

public class WagonLevel 
{

	public String rossAge ; 
	
	public String path_meta ; 
	
	public String path_parallax ; 
	
	public ArrayList<GameItem> listItems = new ArrayList<GameItem>() ; 
	
	public String hint1 ; 
	public String hint2 ; 
	public String hint3 ; 
	
	@JsonIgnore
	public Texture wagon ; 
	@JsonIgnore
	Parallax_Heart parallax ;  
	
	public boolean readyForUse = false ; 
	
	@JsonIgnore
	float currentCamPosition ; 
	
	int currentLevel ; 
	
	public WagonLevel()
	{}

	String myWagonPath,myParallaxPath ; 
	String metaPath ;
	
	public void init()
	{
		try 
		{
			metaPath = "game/wagon/" + path_meta + "/" ; 
			myWagonPath = metaPath + "WAGON.png" ; 
			myParallaxPath = metaPath + path_parallax ;  
			
			parallax = new Parallax_Heart(myParallaxPath) ;
			parallax.screenSpeedConstantX = 800 ; 

			loadTexture(myWagonPath) ;
			
			for(GameItem item : listItems)
			{
				item.init(); 
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		} 
	}
	
	public void setAsGameReady()
	{
		manager.finishLoading(); 
		wagon =  manager.get(myWagonPath, Texture.class) ;
		for(GameItem item : listItems)
		{
			item.setGameReady(); 
		}
		readyForUse = true ; 
		
		GVars_Game.preLoadLevel(currentLevel + 1); 
		resize() ; 
	}
	
	public void update(float delta)
	{
    	parallax.act(delta);	
    	ItemOutline.advance(delta);
	}
	
	public void draw()
	{
		parallax.render();
		
    	// Here rather than in update: the camera has moved by now, and a paused level is still drawn.
    	Vector3 pointer = IKM_Game_Keyboard.pointerInCarriage() ; 
    	for(GameItem gameItem : listItems)
    		gameItem.updateHover(pointer);
    	
    	staticBatch.begin();
    	staticBatch.draw(wagon, 0, BAR, WIDTH, GVars_Camera.WORLD_HEIGHT - BAR);
    	for(GameItem gameItem : listItems)
    	{
    		gameItem.draw(staticBatch);
    	}
    	staticBatch.end();
    	
    	// In the world too, under the view wherever it has panned to, so it always meets the art.
    	GVars_Camera.shapeRenderer.setProjectionMatrix(GVars_Camera.camera.combined);
    	GVars_Camera.shapeRenderer.begin(ShapeType.Filled);
    	GVars_Camera.shapeRenderer.setColor(0, 0, 0, 1);
    	GVars_Camera.shapeRenderer.rect(0, 0, WIDTH, BAR);
    	GVars_Camera.shapeRenderer.end();
	}
	
	/** The black bar under the carriage, in world units: a ninth of the world's height. */
	public static final float BAR = GVars_Camera.WORLD_HEIGHT / 9 ; 
	/** The shape every WAGON.png was painted at: 3840x1080. */
	public static final float PAINTED_ASPECT = 3840f / 1080f ; 
	/**
	 * How long the carriage is drawn, in world units: the height above the bar at the art's
	 * painted shape, 2844.4. It was 3000 until d13 (r70), 5.5% wider than painted, and the
	 * items were placed against that stretch - their x in the .wa files were scaled by 0.948
	 * to follow. Before r64 it followed the window.
	 */
	public static final float WIDTH = (GVars_Camera.WORLD_HEIGHT - BAR) * PAINTED_ASPECT ; 
	
	/** The bar's height in screen pixels, for the stage (the inventory row sits on it). */
	public float decalYBot ;
	
	public void resize() 
	{
		decalYBot = BAR * Gdx.graphics.getHeight() / GVars_Camera.WORLD_HEIGHT ; 
	}
}
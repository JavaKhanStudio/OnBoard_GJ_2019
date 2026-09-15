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
	}
	
	public void draw()
	{
		parallax.render();
		
    	// Here rather than in update: the camera has moved by now, and a paused level is still drawn.
    	Vector3 pointer = IKM_Game_Keyboard.pointerInCarriage() ; 
    	for(GameItem gameItem : listItems)
    		gameItem.updateHover(pointer);
    	
    	staticBatch.begin();
    	staticBatch.draw(wagon,0,decalYBot, (Gdx.graphics.getWidth()  * 2) - proportionMinus, Gdx.graphics.getHeight() - decalYBot - decalYTop);
    	for(GameItem gameItem : listItems)
    	{
    		gameItem.draw(staticBatch);
    	}
    	staticBatch.end();
    	
    	GVars_Camera.shapeRenderer.begin(ShapeType.Filled);
    	GVars_Camera.shapeRenderer.setColor(0, 0, 0, 1);
    	GVars_Camera.shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), decalYBot);
    	GVars_Camera.shapeRenderer.end();
	}
	
	public float decalYBot ;
	float decalYTop ; 
	float proportionMinus ; 
	
	public void resize() 
	{
		decalYBot = Gdx.graphics.getHeight()/9 ; 
//		decalYTop = Gdx.graphics.getHeight()/9 ;
		decalYTop = 0 ; 
		proportionMinus = (decalYTop + decalYBot) * 2 ; 
	}
}
package jks.vue.models;

import static jks.index.Index_Interface.*;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Texture;
import com.kotcrab.vis.ui.widget.VisImage;

import jks.camera.GVars_Camera;
import jks.input.IKM_Game_Keyboard;
import jks.input.IKM_Game_XBoxController;
import jks.sounds.Enum_Music;
import jks.sounds.GVars_AudioManager;
import jks.vars.GVars_Heart;
import jks.vinterface.GVars_UI;
import jks.vue.AVue_Model;
import jks.vue.models.game.GVars_Game;

public class Vue_Scenematic_Outro extends AVue_Model
{
	
	public Texture page_1 ;
	public Texture page_2 ;
	public Texture page_3 ;
	
	public Texture currentpage ;
	
	ArrayList<Texture> imageSequence ;
	
	int currentIndex = 0; 
	
	@Override
	public void init() 
	{
		resize(0,0) ; 
		GVars_Heart.inCinematic = true ; 
		
		Gdx.input.setInputProcessor(new InputMultiplexer(GVars_UI.mainUi, new IKM_Game_Keyboard()));
		Controllers.clearListeners();
		Controllers.addListener(new IKM_Game_XBoxController()) ; 
		
		imageSequence = new ArrayList<Texture>() ; 
		GVars_AudioManager.PlayMusic(Enum_Music.GAME_INTRO);
		
		System.out.println(manager + "manager");
		
		page_1 = manager.get(outroPage1, Texture.class) ;
		currentpage = page_1 ; 
		
		if(GVars_Game.karma > 2) // Leave Ending
		{
			page_2 = manager.get(outroPage_leave, Texture.class) ;
			imageSequence.add(page_2) ; 
		}
		else // stay Ending
		{
			page_2 = manager.get(outroPage_stay_1, Texture.class) ;
			page_3 = manager.get(outroPage_stay_2, Texture.class) ;
			
			imageSequence.add(page_2) ; 
			imageSequence.add(page_3) ; 
		}
	
	}
	

	@Override
	public void destroy() 
	{}

	@Override
	public void restart() 
	{}

	
	
	@Override
	public void update(float delta) 
	{
		GVars_UI.mainUi.act(delta);
		
		if(inDescent)
		{
			if(GVars_Heart.inCinematic_Click)
			{
				GVars_Heart.inCinematic_Click = false ; 
			}
			
			currentAlpha -= (1/fadeOutXSec) * delta ; 
			
			if(currentAlpha < 0)
			{
				if( imageSequence.size() > 0)
				{
					currentIndex ++ ; 
					currentpage = imageSequence.get(0) ;
					
//					if(currentIndex == 2)
//					{smokeScreenOn = true ;}
//					else
//					{smokeScreenOn = false ;}
					
					imageSequence.remove(0) ; 
					inDescent = false ; 
				}
			}
			
		}
		else
		{
			currentAlpha += (1/fadeInXSec) * delta ; 
			
			if(currentAlpha > 1)
			{currentAlpha = 1 ;}
			
			if(GVars_Heart.inCinematic_Click)
			{
				inDescent = true ;
				GVars_Heart.inCinematic_Click = false ; 
			}
		}
			
	}
	
	boolean inDescent = false ; 
	float fadeInXSec = 2; 
	float fadeOutXSec = 1; 
	float smokeSpeed ; 
	float currentAlpha ; 

	@Override
	public void render() 
	{
		drawInterface() ;
		GVars_Camera.staticBatch.begin() ;
		
		GVars_Camera.staticBatch.setColor(1, 1, 1, currentAlpha); 
		GVars_Camera.staticBatch.draw(currentpage, 0, 0,  Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				
		GVars_Camera.staticBatch.end() ;
	}

	private void buildSmokeScreen()
	{
		
	}
	
	
	@Override
	public void resize(int x, int y) 
	{
//		logoSize = Gdx.graphics.getWidth()/5 ; 
	}
	
	
	
}
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
import jks.vue.GVars_Steam;

public class Vue_Scenematic_Intro extends AVue_Model
{
	
	public Texture page_1 ;
	public Texture page_2 ;
	public Texture page_3 ;
	
	public Texture currentpage ;
	
	ArrayList<Texture> imageSequence ;
	
	/**
	 * The splash logos used to play alone on black before the story (Vue_Preloading); since
	 * r158 each page carries one, in the same order - the team, libGDX, the jam - on a part of
	 * the painting with nothing in it. Where, in the page's own 1920x1080 pixels from its
	 * top-left: {left, top}. The size is the texture's, which tools/make_intro_logos.py bakes
	 * at its height on a 1080 page; it scales with the page's height and keeps its shape (D3).
	 */
	private static final float[][] LOGO_PLACES = {
		{1670, 606},   // page 1: the empty purple wagon roof, bottom right, clear of the rails
		{1450, 866},   // page 2: under the ticket, bottom right
		{  90, 700},   // page 3: on the steam, bottom left
	} ;
	private static final float PAGE_WIDTH = 1920, PAGE_HEIGHT = 1080 ;
	
	public Texture logo_Team ;
	public Texture logo_Engine ;
	public Texture logo_Jam ;
	
	ArrayList<Texture> logoSequence ;
	/** The logo on the current page. */
	public Texture currentLogo ;
	/** It rises once its page is fully up, over LOGO_FADE_IN seconds (D4), and leaves with the page. */
	public float logoAlpha ;
	private static final float LOGO_FADE_IN = 1.5f ;
	
	VisImage showingPage ; 
	
	int currentIndex = 0; 
	
	@Override
	public void init() 
	{
		resize(0,0) ; 
		GVars_Heart.inCinematic = true ; 
		GVars_Heart.inCinematic_Click = false ; 
		
		Gdx.input.setInputProcessor(new InputMultiplexer(GVars_UI.mainUi, new IKM_Game_Keyboard()));
		Controllers.clearListeners();
		Controllers.addListener(new IKM_Game_XBoxController()) ; 
		
		imageSequence = new ArrayList<Texture>() ; 
		GVars_AudioManager.PlayMusic(Enum_Music.GAME_INTRO);
		
		page_1 = manager.get(introPage_1, Texture.class) ;
		page_2 = manager.get(introPage_2, Texture.class) ;
		page_3 = manager.get(introPage_3, Texture.class) ;
		
		imageSequence.add(page_2) ; 
		imageSequence.add(page_3) ; 
		
		logo_Team = manager.get(introLogo_Team, Texture.class) ;
		logo_Engine = manager.get(introLogo_LibGDX, Texture.class) ;
		logo_Jam = manager.get(introLogo_Jam, Texture.class) ;
		
		logoSequence = new ArrayList<Texture>() ; 
		logoSequence.add(logo_Engine) ; 
		logoSequence.add(logo_Jam) ; 
		
		currentpage = page_1 ; 
		currentLogo = logo_Team ; 
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
				currentIndex ++ ; 
				currentpage = imageSequence.get(0) ;
				imageSequence.remove(0) ; 
				currentLogo = logoSequence.remove(0) ; 
				logoAlpha = 0 ; 
				inDescent = false ; 
			}
			
		}
		else
		{
			currentAlpha += (1/fadeInXSec) * delta ; 
			
			if(currentAlpha > 1)
			{currentAlpha = 1 ;}
			
			if(currentAlpha == 1)
				logoAlpha = Math.min(1, logoAlpha + delta / LOGO_FADE_IN) ; 
			
			if(GVars_Heart.inCinematic_Click)
			{
				GVars_Heart.inCinematic_Click = false ; 
				// The last page does not fade: steam rises over it, and lifts off the menu, lit already.
				if(imageSequence.isEmpty())
					GVars_Steam.through(() -> GVars_Heart.changeVue(new Vue_StartScreen().alreadyUp(),true)) ;
				else
					inDescent = true ;
			}
		}
	}
	
	boolean inDescent = false ; 
	float fadeInXSec = 2; 
	float fadeOutXSec = 1; 
	public float currentAlpha ; 

	@Override
	public void render() 
	{
		drawInterface() ;
		GVars_Camera.staticBatch.begin() ;
		
		GVars_Camera.staticBatch.setColor(1, 1, 1, currentAlpha); 
		GVars_Camera.staticBatch.draw(currentpage, 0, 0,  Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		if(logoAlpha > 0)
		{
			float[] place = LOGO_PLACES[currentIndex] ; 
			float scale = Gdx.graphics.getHeight() / PAGE_HEIGHT ; 
			float width = currentLogo.getWidth() * scale ; 
			float height = currentLogo.getHeight() * scale ; 
			float x = place[0] / PAGE_WIDTH * Gdx.graphics.getWidth() ; 
			float y = Gdx.graphics.getHeight() - place[1] * scale - height ; 
			GVars_Camera.staticBatch.setColor(1, 1, 1, Math.max(0, currentAlpha) * logoAlpha); 
			GVars_Camera.staticBatch.draw(currentLogo, x, y, width, height);
		}
		
		GVars_Camera.staticBatch.end() ;
	}

	
	@Override
	public void resize(int x, int y) 
	{
	}
	
	
	
}
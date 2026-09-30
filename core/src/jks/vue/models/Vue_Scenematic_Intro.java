package jks.vue.models;

import static jks.index.Index_Interface.*;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.kotcrab.vis.ui.widget.VisImage;

import jks.camera.GVars_Camera;
import jks.vars.GVars_Heart;
import jks.vue.GVars_Steam;

public class Vue_Scenematic_Intro extends AVue_Scenematic
{
	
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
	
	@Override
	public void init() 
	{
		startScenematic() ; 
		GVars_Heart.inCinematic_Click = false ; 
		
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
	protected void whileUp(float delta)
	{
		if(currentAlpha == 1)
			logoAlpha = Math.min(1, logoAlpha + delta / LOGO_FADE_IN) ; 
	}
	
	@Override
	protected void clicked()
	{
		// The last page does not fade: steam rises over it, and lifts off the menu, lit already.
		if(imageSequence.isEmpty())
			GVars_Steam.through(() -> GVars_Heart.changeVue(new Vue_StartScreen().alreadyUp(),true)) ;
		else
			inDescent = true ;
	}
	
	@Override
	protected void fadedOut()
	{
		currentIndex ++ ; 
		currentpage = imageSequence.get(0) ;
		imageSequence.remove(0) ; 
		currentLogo = logoSequence.remove(0) ; 
		logoAlpha = 0 ; 
		inDescent = false ; 
	}

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
	
}

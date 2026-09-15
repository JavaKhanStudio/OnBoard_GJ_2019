package jks.amain;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.graphics.GL20;

import java.util.Locale;

import jks.sounds.GVars_AudioManager;
import jks.tools.Utils_Debug;

import jks.camera.GVars_Camera;
import jks.index.Index_Interface;
import jks.input.GVars_Controller;
import jks.input.Player_Inputs;
import jks.vars.GVars_Heart;
import jks.vars.GVars_Serialization;
import jks.vinterface.GVars_UI;
import jks.vinterface.font.GVars_Font;
import jks.vue.models.Vue_Preloading;
import jks.vue.models.Vue_Scenematic_Intro;
import jks.vue.models.Vue_Scenematic_Outro;
import jks.vue.models.Vue_SoundLab;
import jks.vue.models.Vue_StartScreen;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.Vue_Game;

public class Main_Application extends ApplicationAdapter 
{

	/**
	 * Where the game begins.
	 *
	 * The screens chain into each other by themselves - the logos hand off to the intro,
	 * the intro to the start screen, "New Game" to the first carriage, and finishing the
	 * fourth to the ending - so LOGO plays the whole thing through.
	 *
	 * This used to be five lines with four of them commented out, which meant jumping past
	 * the intro during development left the shipped game starting mid-story. Override it
	 * with -Donboard.start=game when you are working on a level.
	 *
	 * SOUND_LAB is not part of the game - nothing leads there. It is a development screen
	 * listing every track and sound effect the game names; see Vue_SoundLab.
	 *
	 * CREDITS is the start screen with the credits already open, exactly as its Credits
	 * button leaves it, so Retour lands on the menu. It is there so the credits can be looked
	 * at without sitting through the logos.
	 */
	public enum StartPoint
	{
		LOGO, INTRO, START_SCREEN, GAME, OUTRO, SOUND_LAB, CREDITS
	}
	
	public static StartPoint startPoint = fromSystemProperty() ; 
	
	private static StartPoint fromSystemProperty()
	{
		String requested = System.getProperty("onboard.start") ; 
		if(requested == null)
			return StartPoint.LOGO ; 
		
		try
		{
			return StartPoint.valueOf(requested.trim().toUpperCase(Locale.ROOT)) ; 
		}
		catch(IllegalArgumentException unknown)
		{
			Utils_Debug.warn("Unknown onboard.start value '" + requested + "', starting at the logos") ; 
			return StartPoint.LOGO ; 
		}
	}
	
	@Override
	public void create () 
	{
		if(GVars_Heart.isFullScreen)
		{
			DisplayMode mode = Gdx.graphics.getDisplayMode();
			Gdx.graphics.setFullscreenMode(mode);
		}
		mainInit() ;
		
		switch(startPoint)
		{
			case LOGO:         startAtLogo() ;        break ; 
			case INTRO:        startAtIntro() ;       break ; 
			case START_SCREEN: startAtStartScreen() ; break ; 
			case GAME:         startAtGame() ;        break ; 
			case OUTRO:        startAtOutro() ;       break ;
			case SOUND_LAB:    GVars_Heart.changeVue(new Vue_SoundLab(),true) ; break ;
			case CREDITS:      GVars_Heart.changeVue(new Vue_StartScreen(true),true) ; break ;
		}
	}
	
	public void startAtGame() 
	{	
		Vue_Game myGame = new Vue_Game() ; 
		GVars_Heart.changeVue(myGame,true) ; 
		GVars_Game.loadLevel(1);
	}
	
	public void startAtIntro() 
	{	
		GVars_Heart.changeVue(new Vue_Scenematic_Intro(),true) ; 
	}
	
	public void startAtOutro() 
	{
		GVars_Game.karma = 4 ; 
		GVars_Heart.changeVue(new Vue_Scenematic_Outro(),true) ; 	
	}
	
	public void startAtLogo() 
	{
		GVars_Heart.changeVue(new Vue_Preloading(),true) ; 
	}
	
	private void mainInit()
	{
		GVars_Serialization.init(); 
		GVars_UI.init() ; 
		GVars_Camera.init();	
		GVars_Controller.init();
		Index_Interface.init();
	}

	public void startAtStartScreen()
	{
		GVars_Heart.changeVue(new Vue_StartScreen(),true) ;
	}

	@Override
	public void render () 
	{
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    	float delta = Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f);
    
    	if (delta > 0) 
    	{
    		Player_Inputs.updateInput_ControllingInterface() ;
    		
    		if(!GVars_Heart.isPaused)
    			GVars_Heart.vue.update(delta);
        	
    		GVars_Heart.vue.render();
    	}
	}
    
    @Override
	public void resize(int width, int height) 
	{
		GVars_UI.mainUi.getViewport().update(width, height, true);
		GVars_UI.mainUi.getViewport().getCamera().update();
		GVars_Camera.staticBatch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		if(GVars_Heart.vue != null)
		{
			GVars_Heart.vue.resize(width,height) ; 
		}	
	}
    
    @Override
    public void pause()
    {}

    @Override
    public void resume()
    {}

    @Override
	public void dispose() 
	{
		// Music holds a decoder and an OpenAL source. Nothing released them, which was
		// invisible while the game only ever exited by having its process killed.
		GVars_AudioManager.StopAndDisposeMusic() ;
		GVars_AudioManager.DisposeTrainSounds() ;
		GVars_Font.dispose() ;
    }
}
package jks.amain;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.graphics.GL20;

import jks.camera.GVars_Camera;
import jks.index.Index_Interface;
import jks.input.GVars_Controller;
import jks.input.Player_Inputs;
import jks.vars.GVars_Heart;
import jks.vars.GVars_Serialization;
import jks.vinterface.GVars_UI;
import jks.vue.models.Vue_Preloading;
import jks.vue.models.Vue_Scenematic_Intro;
import jks.vue.models.Vue_Scenematic_Outro;
import jks.vue.models.Vue_StartScreen;
import jks.vue.models.game.GVars_Game;
import jks.vue.models.game.Vue_Game;

public class Main_Application extends ApplicationAdapter 
{

	@Override
	public void create () 
	{
		if(GVars_Heart.isFullScreen)
		{
			DisplayMode mode = Gdx.graphics.getDisplayMode();
			Gdx.graphics.setFullscreenMode(mode);
		}
		mainInit() ;
		
//		startAtLogo() ;
//		startAtIntro() ;
//		startAtStartScreen();	
		startAtGame() ; 
//		startAtOutro() ; 
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

	private void startAtStartScreen()
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
    
    }
}
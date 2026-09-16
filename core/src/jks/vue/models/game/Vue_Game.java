package jks.vue.models.game;

import static jks.camera.GVars_Camera.camera;
import static jks.camera.GVars_Camera.staticBatch;
import static jks.vue.models.game.GVars_Game.currentLevel;
import static jks.vue.models.game.GVars_Game.* ; 

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.GL20;

import jks.camera.GVars_Camera;
import jks.input.GVars_Inputs;
import jks.input.IKM_Game_Keyboard;
import jks.input.IKM_Game_XBoxController;
import jks.sounds.Enum_Music;
import jks.sounds.GVars_AudioManager;
import jks.vars.GVars_Heart;
import jks.vars.GVars_Serialization;
import jks.vinterface.GVars_UI;
import jks.vinterface.ToRender;
import jks.vue.AVue_Model;

public class Vue_Game extends AVue_Model
{
    
    boolean ending = false ; 
    boolean ended = false ; 
    float visibleLevel = 1 ; 
    
    PauseButton pauseButton ; 
   
    final float erasingSpeed = 0.5f ; 
    final float timeTonext = 1f/erasingSpeed + erasingSpeed * 3 ; 
    
    @Override
    public void init() 
    {
    	resize(0, 0);
    	GVars_Game.init();
    	GVars_Serialization.init();
    	
    	// The mouse's way to the pause screen (d4). The stage is new for every view and a
    	// level change does not clear it, so one button lasts the whole game.
    	pauseButton = new PauseButton() ;
    	GVars_UI.mainUi.addActor(pauseButton);
    	
    	GVars_Heart.inCinematic = false ; 
    	
    	Gdx.input.setInputProcessor(new InputMultiplexer(GVars_UI.mainUi, new IKM_Game_Keyboard()));
		Controllers.clearListeners();
		Controllers.addListener(new IKM_Game_XBoxController()) ; 
		GVars_AudioManager.PlayMusic(Enum_Music.GAME_INTRO);
		// Under every level: loading the next one does not come back through here, so the loop
		// carries on across the whole game.
		GVars_AudioManager.StartRails();
		restart() ; 
    }
    
    
    @Override
    public void render() 
    {
    	camera.update();
    	Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        staticBatch.setProjectionMatrix(camera.combined);
        staticBatch.setColor(1,1,1,1);
        
        if(currentLevel != null)
		{
			currentLevel.draw();
		}
        
    	for (ToRender rende : toRender) 
			rende.render();
    	
    	staticBatch.begin();
    	
    	if(ending)
    		staticBatch.setColor(1, 1, 1, visibleLevel);
    	
    	GVars_Game.ross.draw(staticBatch);
    	staticBatch.end();
    	
    	drawInterface() ;
    }

	@Override
	public void destroy() 
	{}

	@Override
	public void restart() 
	{
		ending = false ; 
    	ended = false ; 
    	visibleLevel = 1 ; 
	}

	
	float posX ; 
	float posY ; 
	
	@Override
	public void update(float delta) 
	{
		if(dialogBubble != null) 
		{
			if(ross.getIsReserve()) 
			{	
				dialogBubble.reverse(false) ; 
				posX = ross.position.x - GVars_Camera.camera.position.x + 800 
						+ ross.getFrameWidth() * 0.8f ;
			}
			else 
			{
				dialogBubble.reverse(true) ; 
				posX = ross.position.x - GVars_Camera.camera.position.x + 800 
						- ((ross.getFrameWidth() * 0.8f) - ross.getFrameWidth()) 
						;
			}
			
			posY = ross.position.y + ross.getFrameHeight() * 0.8f ; 
			dialogBubble.setPosition(posX, posY);
		}
		
		if(ending)
		{
			visibleLevel -= delta * erasingSpeed ; 
			
			
			if(ended)
			{
				
			}
			return ; 
		}
			
		if(currentLevel != null)
		{
			GVars_Inputs.updateInput_Game(delta) ; 
			currentLevel.update(delta);
		}
		
		GVars_UI.mainUi.act(delta);
		
	}



	@Override
	public void resize(int x, int y) 
	{
		// The pause screen, which the model resizes, is only ever open over this view.
		super.resize(x, y);
		if(pauseButton != null)
			pauseButton.resize();
		// The parallax library recomputes the backdrop's world height here; it used to be fixed
		// when the level was built, so a resolution change left the old aspect ratio behind.
		if(GVars_Game.currentLevel != null && GVars_Game.currentLevel.parallax != null)
			GVars_Game.currentLevel.parallax.resize(x, y);
		// The carried items are centred on the bar, so the middle moving moves them (r59).
		if(GVars_Game.inventory != null)
			GVars_Game.inventory.place();
	}
}
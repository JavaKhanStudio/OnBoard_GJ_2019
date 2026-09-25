package jks.vue.models;

import static jks.index.Index_Interface.maisMenus_Background;
import static jks.index.Index_Interface.manager;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.GL20;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import jks.camera.GVars_Camera;
import jks.input.IKM_Game_Keyboard;
import jks.input.IKM_Game_XBoxController;
import jks.input.Player_Inputs;
import jks.sounds.Enum_Music;
import jks.sounds.GVars_AudioManager;
import jks.vinterface.GVars_UI;
import jks.vinterface.StartScreen_SmoothSideSelect;
import jks.vinterface.font.GVars_Font;
import jks.vinterface.overlay.OverlayCredits;
import jks.vue.AVue_Model;
import jks.vue.Utils_View;
import jks.vue.models.game.GVars_Game; 

public class Vue_StartScreen extends AVue_Model
{
	Texture background ;
	public Texture sourceTexture ;
	TextButton incrementOnce ;
	StartScreen_SmoothSideSelect smoothSideSelect ; 
	
	private float gettingUpPercent = 0 ; 
	private final float gettingUpInXSec = 2 ; 
	
	/** Arrive with the credits open instead of the menu - the CREDITS start point. */
	private final boolean openOnCredits ;

	public Vue_StartScreen()
	{this(false) ;}

	public Vue_StartScreen(boolean openOnCredits)
	{this.openOnCredits = openOnCredits ;}

	@Override
	public void init()
	{
		toRender = new ArrayList<>() ;
		
		background = manager.get(maisMenus_Background, Texture.class) ;
		
		GVars_AudioManager.PlayMusic(Enum_Music.STARTING_SCREEN);
		
		Gdx.input.setInputProcessor(new InputMultiplexer(GVars_UI.mainUi, new IKM_Game_Keyboard()));
		Controllers.clearListeners();
		Controllers.addListener(new IKM_Game_XBoxController()) ; 
		
		smoothSideSelect = new StartScreen_SmoothSideSelect() ; 
		GVars_UI.mainUi.addActor(smoothSideSelect);
		incrementOnce = new TextButton("increment Once +",GVars_UI.baseSkin) ; 
		GVars_Font.resize();

		// The menu entries start parked off the left edge, which is where the Credits
		// button sends them, so skipping enterScene leaves the screen as that button would.
		// Retour calls enterScene and the menu slides in as usual.
		if(openOnCredits)
			Utils_View.setOverlay(new OverlayCredits(smoothSideSelect)) ;
		else
			smoothSideSelect.enterScene(gettingUpInXSec/2);
		GVars_Game.preLoadLevel(1); 
	}

	/** Arrive already lit, without the rise out of black: the intro's steam lifts off it instead (r85). */
	public Vue_StartScreen alreadyUp()
	{
		gettingUpPercent = 1 ;
		return this ;
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
		Player_Inputs.updateInput_ControllingInterface() ;
		GVars_UI.mainUi.act(delta);
		gettingUpPercent += (1/gettingUpInXSec * delta) ; 
		
		if(gettingUpPercent > 1)
			gettingUpPercent = 1 ; 
	}
	
	@Override
	public void render()
	{
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		GVars_Camera.staticBatch.begin() ;
		GVars_Camera.staticBatch.setColor(gettingUpPercent, gettingUpPercent, gettingUpPercent, gettingUpPercent);
		GVars_Camera.staticBatch.draw(background,0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		GVars_Camera.staticBatch.end() ;
		
		renderBeforeInterface() ;
		drawInterface() ;
	}
	
	@Override
	public void resize(int x, int y) 
	{
		super.resize(x,y) ; 
		// A label keeps the glyphs of the font it was given, so the menu fonts GVars_Font
		// rasterised for the new width only reach it through setStyle (r117). The entries are
		// placed again by enterScene, when the options hand the menu back.
		GVars_Font.resize() ;
		GVars_UI.massResize(null) ;
	}
}
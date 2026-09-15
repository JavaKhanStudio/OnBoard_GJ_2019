package jks.vars;

import jks.tools.Utils_Debug;

import java.util.Random;

import com.badlogic.gdx.assets.AssetManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import jks.camera.GVars_Camera;
import jks.input.GVars_Controller;
import jks.sounds.GVars_AudioManager;
import jks.tools.Enum_Timming;
import jks.tools.GlobalTimmer;
import jks.vinterface.GVars_UI;
import jks.vue.AVue_Model;

public class GVars_Heart 
{

	public static boolean isAzerty = true ; 
	
	public static boolean debug = true;
	public static boolean isPaused = false ; 
	public static AVue_Model vue;
	public static final Random random = new Random();
	public static AssetManager assetManager = new AssetManager();
	public static boolean isFullScreen = false;
	
	public static boolean inCinematic = false ;
	public static boolean inCinematic_Click = false ;


	public static void init() 
	{
		loadAssets() ; 
	
		GVars_Camera.init();
		GVars_UI.init();
		GVars_Controller.init();
		GVars_UI.init();
		
		GlobalTimmer.purge() ;
	}
 
	
	public static void loadAssets()
	{
		GlobalTimmer.registerTime(Enum_Timming.ASSETS);
		
//		Index_Sprite.init();
		GlobalTimmer.getElapse(Enum_Timming.ASSETS, "Sprite", true);
		
		GVars_AudioManager.init();
		GlobalTimmer.getElapse(Enum_Timming.ASSETS, "Sounds", true);
	}
	
	public static void changeVue(AVue_Model View,boolean cleanAll) 
	{
		if(cleanAll) 
		{
			// The music is deliberately NOT stopped here. Every view asks for its track in
			// init(), so tearing the music down on each change restarted the same recording
			// from zero at every screen. PlayMusic now decides whether a change is needed,
			// which lets one track carry across the logos, the intro and the menu.
			//
			// The train is the opposite: it belongs to the game view alone. Vue_Game starts the
			// rails again in its own init(), so leaving for the outro or the menu silences them.
			GVars_AudioManager.StopTrain() ;
			GVars_Heart.isPaused = false ; 
			GVars_UI.reset() ; 
		}
		
		if (View != null) 
		{
			vue = View;
			vue.init();
		} else {
			Utils_Debug.warn("Aucune view?");
		}
	}

	public static void togglePauseMenu() 
	{
		GVars_Heart.isPaused = !GVars_Heart.isPaused ; 
		GVars_UI.setPause(GVars_Heart.isPaused) ; 
	}

}

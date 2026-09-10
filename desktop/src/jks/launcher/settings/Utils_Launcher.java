package jks.launcher.settings;

import jks.tools.Utils_Debug;

import static jks.launcher.settings.GVars_Laucher.finalHeight;
import static jks.launcher.settings.GVars_Laucher.finalWidth;
import static jks.launcher.settings.GVars_Laucher.tailleTest;


import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.GameConfigs;
import jks.amain.Utils_Config;
import jks.vars.FVars_Heart;
import jks.sounds.GVars_Audio;
import jks.vars.GVars_Heart;

public class Utils_Launcher 
{

	
	public static void basicConfig(Lwjgl3ApplicationConfiguration config)
	{
		config.setBackBufferConfig(8, 8, 8, 8, 32, 2, 4);
		config.useVsync(true) ;
		config.setResizable(false); ;
		config.setTitle("generic") ; 
	}
	
	public static void setFullScreen(Lwjgl3ApplicationConfiguration config)
	{
		basicConfig(config) ; 
		config.setWindowedMode(finalWidth, finalHeight);
		GVars_Heart.isFullScreen = false ; 
	}
	
	public static void setSideTestScreen(Lwjgl3ApplicationConfiguration config)
	{
		config.setWindowedMode((int) (FVars_Heart.screenXModel * tailleTest), (int) (FVars_Heart.screenYModel * tailleTest));
		config.setWindowPosition(45, 45);
	}
	
	public static void loadConfig(Lwjgl3ApplicationConfiguration config)
	{
		// Reading and writing the file now lives in core (Utils_Config) so that the options
		// screen, which is also in core, can save what the player changes. This method is
		// only the part that applies settings to the window before libGDX starts.
		GameConfigs gameConfig = Utils_Config.load() ;
		
		config.useVsync(gameConfig.useVsynch) ;
		applyVolume(gameConfig.volume) ;
		
		if(gameConfig.isFullScreen)
		{
			GVars_Heart.isFullScreen = true ;
			DisplayMode[] modes = config.getDisplayModes() ;
			DisplayMode display = modes[modes.length - 1] ;
			config.setWindowedMode(display.width, display.height) ;
		}
		else
		{
			config.setWindowedMode(gameConfig.width, gameConfig.height) ;
		}
	}
	
	/**
	 * GVars_Audio.muted defaulted to true, so anything the audio manager was asked to play
	 * would have been silent. The saved volume now decides it.
	 */
	private static void applyVolume(float volume)
	{
		GVars_Audio.masterVolume = volume ;
		GVars_Audio.muted = volume <= 0f ;
	}
	
}

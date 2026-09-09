package jks.launcher.settings;

import static jks.launcher.settings.GVars_Laucher.finalHeight;
import static jks.launcher.settings.GVars_Laucher.finalWidth;
import static jks.launcher.settings.GVars_Laucher.tailleTest;

import java.io.File;

import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jks.amain.GameConfigs;
import jks.vars.FVars_Heart;
import jks.vars.GVars_Heart;

public class Utils_Launcher 
{

	private static String configPath = "config." ;
	
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
		ObjectMapper objectMapper = new ObjectMapper() ; 
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) ; 
		
		FileHandle configFile = new FileHandle(new File(configPath)) ;
		try
		{
			if(configFile.exists())
			{
				GameConfigs gameConfig = objectMapper.readValue(configFile.file(),GameConfigs.class) ;	
				config.useVsync(gameConfig.useVsynch);
				
				if(gameConfig.isFullScreen)
				{
					GVars_Heart.isFullScreen = true ;
					DisplayMode display = config.getDisplayModes()[config.getDisplayModes().length - 1] ; 
					config.setWindowedMode(display.width,display.height);
				}
				else
				{
					System.out.println(gameConfig.width + "/" + gameConfig.height);
					config.setWindowedMode(gameConfig.width, gameConfig.height);
				}
			}
			else
			{
				GameConfigs configsFile = new GameConfigs(); 
				objectMapper.writeValue(new File(configPath),configsFile) ; 
				config.useVsync(true);
			}
		}
		catch(Exception e)
		{
			System.err.println("dont work");
			e.printStackTrace();
			config.setWindowedMode(1280, 720);
		}	
	}
	
}
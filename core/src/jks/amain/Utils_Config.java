package jks.amain;

import java.io.File;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jks.tools.Utils_Debug;

/**
 * Reads and writes the "config" file that sits next to the game.
 *
 * This used to live in the desktop launcher, which meant the options screen - which is in
 * core - had no way to save anything. You could change the resolution and it would apply,
 * and then be gone the next time you started.
 *
 * The file is resolved against the working directory rather than through Gdx.files, because
 * the launcher reads it before there is a libGDX application to ask.
 */
public class Utils_Config 
{
	private static final String DEFAULT_PATH = "config" ;
	
	/** Overridable with -Donboard.config=..., which is how the tests avoid the real file. */
	private static String configPath()
	{
		return System.getProperty("onboard.config", DEFAULT_PATH) ;
	}
	
	/** The settings in force. Loaded once at startup; the options screen edits this. */
	public static GameConfigs current = new GameConfigs() ;
	
	private static ObjectMapper mapper()
	{
		ObjectMapper mapper = new ObjectMapper() ;
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) ;
		return mapper ;
	}
	
	public static File configFile()
	{
		return new File(configPath()) ;
	}
	
	/**
	 * Loads the config, writing one with the defaults if there is none. Never throws: a
	 * settings file that cannot be read is a reason to fall back to defaults, not a reason
	 * to refuse to start the game.
	 */
	public static GameConfigs load()
	{
		File file = configFile() ;
		try
		{
			if(file.isFile())
				current = mapper().readValue(file, GameConfigs.class) ;
			else
			{
				current = new GameConfigs() ;
				save() ;
			}
		}
		catch(Exception e)
		{
			Utils_Debug.warn("Could not read " + file.getAbsolutePath() + ", using defaults: " + e) ;
			current = new GameConfigs() ;
		}
		
		return current ;
	}
	
	/** Writes the settings in force back to disk. Never throws, for the same reason. */
	public static void save()
	{
		try
		{
			mapper().writerWithDefaultPrettyPrinter().writeValue(configFile(), current) ;
		}
		catch(Exception e)
		{
			Utils_Debug.warn("Could not save settings to " + configFile().getAbsolutePath() + ": " + e) ;
		}
	}
	
}

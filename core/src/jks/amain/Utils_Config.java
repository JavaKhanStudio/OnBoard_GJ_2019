package jks.amain;

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
 * Where the text is kept is the platform's (GVars_Platform, r135): the desktop reads a file
 * next to the game before there is a libGDX application to ask, which Gdx.files cannot do.
 */
public class Utils_Config 
{
	/** The settings in force. Loaded once at startup; the options screen edits this. */
	public static GameConfigs current = new GameConfigs() ;
	
	private static ObjectMapper mapper()
	{
		ObjectMapper mapper = new ObjectMapper() ;
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) ;
		return mapper ;
	}
	
	/**
	 * Loads the config, writing one with the defaults if there is none. Never throws: a
	 * settings file that cannot be read is a reason to fall back to defaults, not a reason
	 * to refuse to start the game.
	 */
	public static GameConfigs load()
	{
		Platform platform = GVars_Platform.current ;
		try
		{
			String text = platform.readConfig() ;
			if(text != null)
				current = mapper().readValue(text, GameConfigs.class) ;
			else
			{
				current = new GameConfigs() ;
				save() ;
			}
		}
		catch(Exception e)
		{
			Utils_Debug.warn("Could not read " + platform.configLocation() + ", using defaults: " + e) ;
			current = new GameConfigs() ;
		}
		
		return current ;
	}
	
	/** Writes the settings in force back to disk. Never throws, for the same reason. */
	public static void save()
	{
		try
		{
			GVars_Platform.current.writeConfig(mapper().writerWithDefaultPrettyPrinter().writeValueAsString(current)) ;
		}
		catch(Exception e)
		{
			Utils_Debug.warn("Could not save settings to " + GVars_Platform.current.configLocation() + ": " + e) ;
		}
	}
	
}

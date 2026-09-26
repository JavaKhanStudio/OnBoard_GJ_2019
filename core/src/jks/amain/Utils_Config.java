package jks.amain;

import jks.tools.Utils_Debug;
import jks.vars.GVars_Serialization;

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
	
	/** The settings a config text holds; a field it lacks keeps its default, one it adds is skipped. */
	public static GameConfigs parse(String text)
	{
		GameConfigs parsed = GVars_Serialization.newJson().fromJson(GameConfigs.class, text) ;
		if(parsed == null)
			throw new IllegalArgumentException("no settings in it") ;
		return parsed ;
	}
	
	/** The config text for these settings, one field a line. */
	public static String write(GameConfigs configs)
	{
		return GVars_Serialization.newJson().prettyPrint(configs) ;
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
				current = parse(text) ;
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
			GVars_Platform.current.writeConfig(write(current)) ;
		}
		catch(Exception e)
		{
			Utils_Debug.warn("Could not save settings to " + GVars_Platform.current.configLocation() + ": " + e) ;
		}
	}
	
}

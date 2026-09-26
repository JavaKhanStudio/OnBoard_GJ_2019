package jks.html;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import jks.amain.GVars_Platform;
import jks.amain.Platform;

/**
 * The game in a browser tab (r137). The settings live in the browser's localStorage, through
 * libGDX Preferences, as the same text the desktop writes to its "config" file. There is no
 * window to centre and no lab-assets/. The backdrops come from their .plaxpj, Platform's default.
 */
public class HtmlPlatform implements Platform
{
	/** The Preferences name, and so the localStorage key prefix. */
	static final String PREFERENCES = "onboard" ;
	static final String KEY = "config" ;

	public static void install()
	{
		GVars_Platform.current = new HtmlPlatform() ;
	}

	private static Preferences preferences()
	{
		return Gdx.app.getPreferences(PREFERENCES) ;
	}

	@Override
	public String readConfig()
	{
		Preferences preferences = preferences() ;
		return preferences.contains(KEY) ? preferences.getString(KEY) : null ;
	}

	@Override
	public void writeConfig(String text)
	{
		Preferences preferences = preferences() ;
		preferences.putString(KEY, text) ;
		preferences.flush() ;
	}

	@Override
	public String configLocation()
	{
		return "localStorage (" + PREFERENCES + "." + KEY + ")" ;
	}
}

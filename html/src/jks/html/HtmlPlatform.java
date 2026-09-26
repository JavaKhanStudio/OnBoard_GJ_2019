package jks.html;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.files.FileHandle;

import jks.amain.GVars_Platform;
import jks.amain.Platform;

/**
 * The game in a browser tab (r137). The settings live in the browser's localStorage, through
 * libGDX Preferences, as the same text the desktop writes to its "config" file. There is no
 * window to centre and no lab-assets/. The backdrops come from their .plaxpj, Platform's default.
 *
 * Every .ogg is an .mp3 here (r152): html/build.gradle's webAssets hands the page the assets with
 * each Ogg Vorbis file encoded again as MP3, which every browser decodes, Safari included.
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
	public FileHandle soundFile(String path)
	{
		return Gdx.files.internal(path.endsWith(".ogg") ? path.substring(0, path.length() - 4) + ".mp3" : path) ;
	}

	@Override
	public String configLocation()
	{
		return "localStorage (" + PREFERENCES + "." + KEY + ")" ;
	}
}

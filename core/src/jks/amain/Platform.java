package jks.amain;

import com.badlogic.gdx.files.FileHandle;

/**
 * What the game needs from the machine it runs on and that libGDX does not give every backend
 * (r135). core/ must stay translatable for the browser build, so java.io.File, java.nio.file and
 * the lwjgl3 backend live behind this, in desktop/ (jks.launcher.DesktopPlatform).
 *
 * Every method has a default that does without: that is what a platform without a disk or a
 * window gets, and what core falls back to when no launcher has set one.
 */
public interface Platform
{
	/** The saved settings as text, or null when there are none yet. May throw: the caller falls back to defaults. */
	default String readConfig() throws Exception
	{
		return null ;
	}

	/** Keeps the settings for the next start. Without a disk they last as long as the game. */
	default void writeConfig(String text) throws Exception
	{
	}

	/** Where the settings are kept, for a warning. */
	default String configLocation()
	{
		return "(nowhere)" ;
	}

	/** Puts the window back in the middle of the screen after it changed size. */
	default void centreWindow()
	{
	}

	/** A file under lab-assets/ (r99), or null where there is none. Lab-only: the game never needs one. */
	default FileHandle labAsset(String relative)
	{
		return null ;
	}

	/** The text table in the source tree, for the line lab to write into (r74), or null where there is none. */
	default FileHandle sourceTextTable()
	{
		return null ;
	}
}

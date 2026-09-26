package jks.launcher;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.files.FileHandle;

import jks.amain.GVars_Platform;
import jks.amain.Platform;
import jks.index.Index_Text;

/**
 * The game on a desktop (r135): the settings in a file next to the game, a window to centre,
 * and the checkout's lab-assets/ and source text table for the labs. core/ asks through
 * jks.amain.Platform so that it stays free of java.io.File and the lwjgl3 backend.
 */
public class DesktopPlatform implements Platform
{
	/** Sets it for the game. Every launcher calls this first, before the config is read. */
	public static void install()
	{
		GVars_Platform.current = new DesktopPlatform() ;
	}

	/**
	 * The "config" file, resolved against the working directory rather than through Gdx.files,
	 * because the launcher reads it before there is a libGDX application to ask. Overridable
	 * with -Donboard.config=..., which is how the tests avoid the real file.
	 */
	public static File configFile()
	{
		return new File(System.getProperty("onboard.config", "config")) ;
	}

	/**
	 * Where the labs keep what the game never ships (r99): lab-assets/ at the root of the
	 * checkout, git-ignored and outside desktop/assets, so neither git nor the jar carries it.
	 * Found from the working directory, desktop/, the way runGame and the tests start the game.
	 */
	public static File labAssets()
	{
		return new File(System.getProperty("onboard.labAssets", "../lab-assets")) ;
	}

	@Override
	public String readConfig() throws Exception
	{
		File file = configFile() ;
		if(!file.isFile())
			return null ;
		return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) ;
	}

	@Override
	public void writeConfig(String text) throws Exception
	{
		Files.write(configFile().toPath(), text.getBytes(StandardCharsets.UTF_8)) ;
	}

	@Override
	public String configLocation()
	{
		return configFile().getAbsolutePath() ;
	}

	@Override
	public void centreWindow()
	{
		// libGDX has no cross-platform window position, hence all of this class.
		if(!(Gdx.graphics instanceof Lwjgl3Graphics))
			return ;
		Lwjgl3Graphics g = (Lwjgl3Graphics) Gdx.graphics ;
		DisplayMode mode = g.getDisplayMode() ;
		g.getWindow().setPosition(mode.width / 2 - g.getWidth() / 2, mode.height / 2 - g.getHeight() / 2) ;
	}

	@Override
	public FileHandle labAsset(String relative)
	{
		// Gdx.files.absolute, not local: local() would glue an absolute -Donboard.labAssets onto
		// the working directory.
		if(Gdx.files == null)
			return null ;
		return Gdx.files.absolute(new File(labAssets(), relative).getAbsolutePath()) ;
	}

	@Override
	public FileHandle sourceTextTable()
	{
		// Under -Donboard.assets, or assets/ from desktop/, where runGame starts.
		return new FileHandle(new File(System.getProperty("onboard.assets", "assets"), Index_Text.TABLE)) ;
	}
}

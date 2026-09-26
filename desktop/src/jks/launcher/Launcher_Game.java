package jks.launcher;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;
import jks.debug.GVars_Debug;
import jks.launcher.settings.Utils_Launcher;
import jks.sounds.GVars_Audio;

public class Launcher_Game 
{
	
	public static void main (String[] arg) 
	{
			
		DesktopPlatform.install() ;
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		
		// Reads the "config" file next to the game: window size, fullscreen, vsync, volume.
		// Writes it with defaults on first run. This was commented out, so the settings
		// screen wrote preferences that nothing ever read back.
		Utils_Launcher.loadConfig(config) ;
		
		// Was useOpenGL3(true, 1, 1), which is gone in libGDX 1.14 - and asked for a 1.1
		// context while requesting GL3, so the driver decided what you actually got.
		config.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2);
		config.setTitle("On Board");
		config.setResizable(false);
		// The window's own icon: title bar, taskbar, Alt-Tab. The .ico baked into onboard.exe
		// only reaches Explorer, so without this Windows shows its generic application icon.
		// GLFW picks the closest size for each use. Ignored on macOS, where the .icns does it.
		// Regenerate with tools/make_window_icons.py.
		config.setWindowIcon(
			"ui/icon/window/logo_onboard_16.png",
			"ui/icon/window/logo_onboard_32.png",
			"ui/icon/window/logo_onboard_48.png",
			"ui/icon/window/logo_onboard_128.png");

//		config.setBackBufferConfig(8, 8, 8, 8, 32, 2, 4);
		Lwjgl3Application application = new Lwjgl3Application(new Main_Application(), config);
	
	}
	
	public static void finalModelGame(Lwjgl3ApplicationConfiguration config)
	{
		DesktopPlatform.install() ;
		GVars_Audio.muted = false ;
		GVars_Debug.debugMode = false;
		new Lwjgl3Application(new Main_Application(), config);
	}
	
	/*
	public static void activateGame(LwjglApplicationConfiguration config)
	{
		config.width = (int) (16 * taille) ;
		config.height = (int) (9 * taille) ;
		new LwjglApplication(new Main_Game(), config);
	}
	*/
	
}

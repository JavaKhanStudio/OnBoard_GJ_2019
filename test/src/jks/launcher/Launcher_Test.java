package jks.launcher;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import jks.amain.Main_Application;

public class Launcher_Test 
{
	
	public static void main (String[] arg) 
	{
			
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

		config.setWindowedMode(1600, 900);
//		config.setWindowedMode(3200, 1800);
		config.useOpenGL3(true, 1, 1);
		config.setTitle("Test");
		config.setResizable(false);

		Lwjgl3Application application = new Lwjgl3Application(new Main_Application(), config);
	
	}
	
}

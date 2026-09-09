package jks.launcher;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;

import jks.editor.Editor_Application;
import jks.editor.GVars_Editor;

public class Launcher_Editor 
{

	public static void main (String[] arg) 
	{		
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setWindowedMode(1600, 900);
		config.useOpenGL3(true, 1, 1);
		config.setTitle("GENERIC");
		config.setResizable(false);
		
		config.setWindowListener(new Lwjgl3WindowAdapter() 
		{
            @Override
            public void filesDropped(String[] files) 
            {
            	GVars_Editor.ref.reciveFiles(files) ; 
            }
        });
		
		Lwjgl3Application application = new Lwjgl3Application(new Editor_Application(), config);
	}
	
}

package jks.verify;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import jks.launcher.DesktopPlatform;

/**
 * Runs every test on the desktop platform, as Launcher_Game runs the game (r135): the tests
 * build their own Lwjgl3Application or none at all, so no launcher main installs it for them.
 * Found through META-INF/services, so no test has to remember it.
 */
public class DesktopSession implements LauncherSessionListener
{
	@Override
	public void launcherSessionOpened(LauncherSession session)
	{
		DesktopPlatform.install() ;
	}
}

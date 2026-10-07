package jks.verify;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jks.launcher.LinuxMenuEntry;

/**
 * The Linux package's application menu entry (r276): the files it writes, and its Exec line for
 * the folder a player unzips into ("On Board/", with a space) or a worse one.
 */
class LinuxMenuEntryTest
{
	@TempDir
	Path home ;

	@Test
	@DisplayName("it writes the .desktop entry and the five icons, and leaves them alone the second time")
	void writesTheEntryAndIcons() throws Exception
	{
		Path exe = Paths.get("/home/player/Jeux/On Board/onboard") ;
		LinuxMenuEntry.write(exe, home) ;

		Path desktop = home.resolve("applications/" + LinuxMenuEntry.ID + ".desktop") ;
		String entry = new String(Files.readAllBytes(desktop), StandardCharsets.UTF_8) ;
		assertTrue(entry.contains("\nExec=\"/home/player/Jeux/On Board/onboard\"\n"), entry) ;
		assertTrue(entry.contains("\nPath=/home/player/Jeux/On Board\n"), entry) ;
		assertTrue(entry.contains("\nIcon=" + LinuxMenuEntry.ID + "\n"), entry) ;
		assertTrue(entry.contains("\nStartupWMClass=On Board\n"), entry) ;
		for(int size : new int[] { 16, 32, 48, 128, 256 })
		{
			Path icon = home.resolve("icons/hicolor/" + size + "x" + size + "/apps/" + LinuxMenuEntry.ID + ".png") ;
			assertArrayEquals(Files.readAllBytes(Paths.get(System.getProperty("onboard.assets"), "ui/icon/window/logo_onboard_" + size + ".png")),
				Files.readAllBytes(icon), icon.toString()) ;
		}

		FileTime old = FileTime.fromMillis(0) ;
		Files.setLastModifiedTime(desktop, old) ;
		LinuxMenuEntry.write(exe, home) ;
		assertEquals(old, Files.getLastModifiedTime(desktop), "an unchanged entry is not rewritten") ;

		LinuxMenuEntry.write(Paths.get("/elsewhere/On Board/onboard"), home) ;
		assertTrue(new String(Files.readAllBytes(desktop), StandardCharsets.UTF_8).contains("\nExec=\"/elsewhere/On Board/onboard\"\n"),
			"a moved folder rewrites the entry") ;
	}

	@Test
	@DisplayName("Exec escapes \" ` $ \\ inside the quotes, doubles %, then doubles the backslashes again")
	void execEscapes()
	{
		assertEquals("\"/a b/onboard\"", LinuxMenuEntry.execValue("/a b/onboard")) ;
		assertEquals("\"/100%% \\\\$x \\\\\\\\ \\\\` \\\\\"/onboard\"", LinuxMenuEntry.execValue("/100% $x \\ ` \"/onboard")) ;
	}
}

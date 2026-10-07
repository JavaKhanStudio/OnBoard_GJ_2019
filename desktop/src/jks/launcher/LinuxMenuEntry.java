package jks.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

/**
 * The Linux package puts On Board in the player's application menu, with its logo (r276).
 *
 * The .exe and the .app carry the logo themselves; a Linux executable is an ELF, which carries
 * no icon, so a file manager shows it generic. Linux shows an application's icon where its
 * .desktop entry is: the app grid, the dock, Alt-Tab. So each time the packaged game starts it
 * writes one, pointing at wherever its folder is now:
 *
 *   $XDG_DATA_HOME/applications/com.pixmen.onboard.desktop      (~/.local/share by default)
 *   $XDG_DATA_HOME/icons/hicolor/<n>x<n>/apps/com.pixmen.onboard.png, from ui/icon/window/
 *
 * "com.pixmen.onboard", the macOS bundle id, and not "onboard": GNOME's on-screen keyboard is
 * already onboard.desktop with an icon called onboard. StartupWMClass is the WM_CLASS GLFW gives
 * the window (its title, "On Board"), so the running game is grouped under this entry.
 *
 * Only construo's launcher does it (an "onboard" executable with app/onboard.json beside it):
 * gradle runGame, the tests and the labs run java and leave the menu alone. The entry stays if
 * the player deletes the folder: Simon accepted that cost on r276. Never fatal: a home that
 * cannot be written only loses the menu entry.
 */
public class LinuxMenuEntry
{
	public static final String ID = "com.pixmen.onboard" ;
	static final int[] SIZES = { 16, 32, 48, 128, 256 } ;

	/** Called by the launcher before the window opens. */
	public static void install()
	{
		try
		{
			if(!System.getProperty("os.name", "").startsWith("Linux"))
				return ;
			Optional<String> command = ProcessHandle.current().info().command() ;
			if(!command.isPresent())
				return ;
			Path exe = Paths.get(command.get()).toAbsolutePath() ;
			if(!exe.getFileName().toString().equals("onboard") || !Files.isRegularFile(exe.resolveSibling("app/onboard.json")))
				return ;
			write(exe, dataHome()) ;
		}
		catch(Exception e)
		{
			System.err.println("LinuxMenuEntry: no menu entry: " + e) ;
		}
	}

	/** $XDG_DATA_HOME when it is an absolute path, as the spec says, else ~/.local/share. */
	static Path dataHome()
	{
		String xdg = System.getenv("XDG_DATA_HOME") ;
		if(xdg != null && xdg.startsWith("/"))
			return Paths.get(xdg) ;
		return Paths.get(System.getProperty("user.home"), ".local", "share") ;
	}

	/** Writes the entry and the icons for the launcher at exe; leaves a file alone when it is unchanged. */
	public static void write(Path exe, Path dataHome) throws IOException
	{
		for(int size : SIZES)
		{
			byte[] png ;
			try(InputStream in = LinuxMenuEntry.class.getClassLoader().getResourceAsStream("ui/icon/window/logo_onboard_" + size + ".png"))
			{
				if(in == null)
					throw new IOException("ui/icon/window/logo_onboard_" + size + ".png is not on the classpath") ;
				png = in.readAllBytes() ;
			}
			writeIfChanged(dataHome.resolve("icons/hicolor/" + size + "x" + size + "/apps/" + ID + ".png"), png) ;
		}
		writeIfChanged(dataHome.resolve("applications/" + ID + ".desktop"), entry(exe).getBytes(StandardCharsets.UTF_8)) ;
	}

	static String entry(Path exe)
	{
		// Path= because the game reads its "config" from the working directory.
		return "[Desktop Entry]\n"
			+ "Type=Application\n"
			+ "Name=On Board\n"
			+ "Exec=" + execValue(exe.toString()) + "\n"
			+ "Path=" + stringValue(exe.getParent().toString()) + "\n"
			+ "Icon=" + ID + "\n"
			+ "Terminal=false\n"
			+ "Categories=Game;AdventureGame;\n"
			+ "StartupWMClass=On Board\n" ;
	}

	/**
	 * The Desktop Entry spec's two layers: inside the quoted argument, " ` $ \ take a backslash
	 * and % is doubled; then the whole value is a string, whose own backslashes are doubled again.
	 */
	public static String execValue(String path)
	{
		StringBuilder quoted = new StringBuilder("\"") ;
		for(char c : path.toCharArray())
		{
			if(c == '"' || c == '`' || c == '$' || c == '\\')
				quoted.append('\\') ;
			if(c == '%')
				quoted.append('%') ;
			quoted.append(c) ;
		}
		return stringValue(quoted.append('"').toString()) ;
	}

	static String stringValue(String s)
	{
		return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r") ;
	}

	private static void writeIfChanged(Path file, byte[] content) throws IOException
	{
		if(Files.isRegularFile(file) && Arrays.equals(Files.readAllBytes(file), content))
			return ;
		Files.createDirectories(file.getParent()) ;
		Files.write(file, content) ;
	}
}

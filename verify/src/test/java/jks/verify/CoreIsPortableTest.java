package jks.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The rail for the browser build (r135): GWT translates core/src, and none of these translate.
 * What core needs of them goes through jks.amain.Platform, which desktop/ implements. Jackson
 * joins the list when phase 2 (r136) takes it out.
 */
class CoreIsPortableTest
{
	/** java.io.File and its streams, java.nio.file, and anything of the lwjgl3 backend. */
	static final Pattern DESKTOP_ONLY = Pattern.compile(
		"\\b(com\\.badlogic\\.gdx\\.backends\\.|org\\.lwjgl\\.|java\\.io\\.File\\w*\\b|java\\.nio\\.file\\.)") ;

	static final Path CORE = Paths.get(System.getProperty("onboard.root", ".."), "core", "src") ;

	@Test
	@DisplayName("nothing in core/src names the desktop backend, java.io.File or java.nio.file")
	void coreNamesNoDesktopOnlyApi() throws IOException
	{
		assertTrue(Files.isDirectory(CORE), "no core sources at " + CORE.toAbsolutePath()) ;
		List<String> found = new ArrayList<>() ;
		int files = 0 ;
		try (Stream<Path> walk = Files.walk(CORE))
		{
			for (Path source : (Iterable<Path>) walk.filter(p -> p.toString().endsWith(".java"))::iterator)
			{
				files++ ;
				found.addAll(offences(CORE.relativize(source).toString(), Files.readAllLines(source, StandardCharsets.UTF_8))) ;
			}
		}
		assertTrue(files > 50, "scanned only " + files + " files under " + CORE.toAbsolutePath()) ;
		assertEquals(List.of(), found, "core/src uses what the browser build cannot translate; put it behind jks.amain.Platform") ;
	}

	@Test
	@DisplayName("the scan catches an import, a qualified name and a stream, and passes a comment")
	void scanCatchesWhatItShould()
	{
		List<String> found = offences("Planted.java", List.of(
			"import java.io.File;",
			"import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;",
			"    java.nio.file.Files.write(p, b) ;",
			"import java.io.FileInputStream;",
			"import org.lwjgl.glfw.GLFW;",
			"// java.io.File was here once",
			" * java.nio.file.Files in a javadoc",
			"import java.io.InputStream;")) ;
		assertEquals(5, found.size(), "the scan did not find exactly the five planted uses: " + found) ;
	}

	/** Every line of code, comments aside, that names a desktop-only API, as file:line: text. */
	static List<String> offences(String name, List<String> lines)
	{
		List<String> found = new ArrayList<>() ;
		for (int i = 0 ; i < lines.size() ; i++)
		{
			String line = lines.get(i).trim() ;
			if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*"))
				continue ;
			if (DESKTOP_ONLY.matcher(line).find())
				found.add(name + ":" + (i + 1) + ": " + line) ;
		}
		return found ;
	}
}

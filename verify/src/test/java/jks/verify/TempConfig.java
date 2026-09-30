package jks.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Points onboard.config at an empty temporary file for one test class, so whatever the game
 * saves while a test drives it never reaches the real desktop/config. restore() puts the
 * property back as it was and deletes the file.
 */
final class TempConfig
{
	final Path path;
	private final String previous;

	private TempConfig(Path path, String previous)
	{
		this.path = path;
		this.previous = previous;
	}

	static TempConfig use() throws IOException
	{
		TempConfig config = new TempConfig(Files.createTempFile("onboard-config", ""), System.getProperty("onboard.config"));
		System.setProperty("onboard.config", config.path.toString());
		return config;
	}

	void restore() throws IOException
	{
		if (previous == null) System.clearProperty("onboard.config");
		else System.setProperty("onboard.config", previous);
		Files.deleteIfExists(path);
	}
}

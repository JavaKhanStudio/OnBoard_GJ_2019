package jks.verify;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * No shipped PNG may be greyscale.
 *
 * libGDX keeps a grey PNG's channel count: grey+alpha becomes a LuminanceAlpha pixmap and plain
 * grey an Alpha one, uploaded as GL_LUMINANCE_ALPHA and GL_ALPHA. The launcher asks for a GL 3.2
 * core profile, which has neither, so the texture is incomplete and draws as a solid black
 * rectangle - no exception, no log line. LWJGL3's default context still has both formats, so a
 * render test that does not copy the launcher's context sees nothing wrong.
 *
 * This is how the thought bubble went black (r37): the oxipng pass in bdeeed0 is lossless, and
 * losslessly turned the RGBA bubbles into grey+alpha. Palette PNGs are fine - they are expanded
 * to RGB or RGBA on load. Recompress with oxipng --nc so the colour type is left alone.
 */
class PngColourTypeTest
{
	private static final int GREY = 0;
	private static final int GREY_ALPHA = 4;

	@Test
	@DisplayName("no asset PNG is greyscale, which a GL 3.2 core context draws as black")
	void noGreyscalePngs() throws IOException
	{
		List<String> grey = new ArrayList<>();
		int checked = 0;
		try (Stream<Path> walk = Files.walk(Assets.DIR.toPath()))
		{
			for (Path p : (Iterable<Path>) walk.filter(f -> f.toString().toLowerCase().endsWith(".png"))::iterator)
			{
				checked++;
				int type = colourType(p);
				if (type == GREY || type == GREY_ALPHA)
					grey.add(Assets.DIR.toPath().relativize(p) + (type == GREY ? "  (grey)" : "  (grey+alpha)"));
			}
		}

		assertTrue(checked >= 50, "expected to find the asset PNGs, found " + checked);
		assertTrue(grey.isEmpty(), "greyscale PNGs render as black rectangles in the game's GL 3.2 "
			+ "core context - save them as RGBA:\n  " + String.join("\n  ", grey));
	}

	/** The colour type byte of the IHDR chunk: 8 bytes of signature, 8 of chunk header, then width, height, bit depth. */
	private static int colourType(Path png) throws IOException
	{
		try (InputStream in = Files.newInputStream(png))
		{
			DataInputStream data = new DataInputStream(in);
			data.skipNBytes(8 + 8 + 4 + 4 + 1);
			return data.readUnsignedByte();
		}
	}
}

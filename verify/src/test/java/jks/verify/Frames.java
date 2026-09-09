package jks.verify;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/** Image comparison tuned for "is this still the same scene?" rather than "is this identical?". */
public final class Frames
{
	private Frames() {}

	/**
	 * Comparison happens on a thumbnail. The game animates - parallax scrolls, Ross idles - so
	 * two runs are never pixel-identical at the same frame index. Downscaling throws away that
	 * jitter and keeps composition and palette, which is what actually regresses when an engine
	 * upgrade goes wrong: a black screen, a missing layer, textures that failed to bind.
	 */
	private static final int TW = 64, TH = 36;

	public static BufferedImage thumbnail(BufferedImage src)
	{
		BufferedImage out = new BufferedImage(TW, TH, BufferedImage.TYPE_INT_RGB);
		out.getGraphics().drawImage(src.getScaledInstance(TW, TH, Image.SCALE_AREA_AVERAGING), 0, 0, null);
		return out;
	}

	/** Mean absolute per-channel difference of the thumbnails, as a fraction of full scale. */
	public static double difference(BufferedImage a, BufferedImage b)
	{
		BufferedImage ta = thumbnail(a), tb = thumbnail(b);
		long sum = 0;
		for (int y = 0; y < TH; y++)
		{
			for (int x = 0; x < TW; x++)
			{
				int pa = ta.getRGB(x, y), pb = tb.getRGB(x, y);
				sum += Math.abs(((pa >> 16) & 0xFF) - ((pb >> 16) & 0xFF));
				sum += Math.abs(((pa >> 8) & 0xFF) - ((pb >> 8) & 0xFF));
				sum += Math.abs((pa & 0xFF) - (pb & 0xFF));
			}
		}
		return sum / (double) (TW * TH * 3 * 255);
	}

	/** How much of the frame is not the clear colour - a blank or half-drawn frame scores near zero. */
	public static double distinctColourRatio(BufferedImage image)
	{
		BufferedImage t = thumbnail(image);
		java.util.Set<Integer> colours = new java.util.HashSet<>();
		for (int y = 0; y < TH; y++)
			for (int x = 0; x < TW; x++)
				colours.add(t.getRGB(x, y));
		return colours.size() / (double) (TW * TH);
	}

	public static void write(BufferedImage image, File target) throws IOException
	{
		File parent = target.getParentFile();
		if (parent != null) parent.mkdirs();
		ImageIO.write(image, "png", target);
	}

	public static BufferedImage read(File source) throws IOException
	{
		return ImageIO.read(source);
	}
}

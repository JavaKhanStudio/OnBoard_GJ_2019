package jks.verify;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

/**
 * What the game played, read back from the WAV the GL tests' audio goes to instead of the
 * speakers (gradle/offscreen.gradle, captureAudio). Lets a test check the sound that came out
 * of the audio device, not just what the audio code believes it asked for.
 *
 * The file belongs to whichever test JVM last opened an audio device. forkEvery = 1 runs the
 * classes one after another, so a test reading it after its own game has exited reads its own.
 */
public class AudioCapture
{
	public final File file;
	public final int sampleRate, channels;
	/** Mono, -1 to 1: the channels averaged. */
	private final float[] samples;

	private AudioCapture(File file, int sampleRate, int channels, float[] samples)
	{
		this.file = file;
		this.sampleRate = sampleRate;
		this.channels = channels;
		this.samples = samples;
	}

	/** The capture Gradle set up for this JVM, or null when the sound went to the speakers. */
	public static File configuredFile()
	{
		String path = System.getenv("ONBOARD_AUDIO_CAPTURE");
		return path == null ? null : new File(path);
	}

	/**
	 * Reads a 16-bit PCM WAV. The sizes in the header are only filled in when the device
	 * closes, so the data chunk is taken to run to the end of the file whatever it says.
	 */
	public static AudioCapture read(File file) throws IOException
	{
		ByteBuffer in = ByteBuffer.wrap(Files.readAllBytes(file.toPath())).order(ByteOrder.LITTLE_ENDIAN);
		if (in.remaining() < 12 || in.getInt() != 0x46464952 /* RIFF */) throw new IOException(file + " is not a WAV");
		in.getInt();
		if (in.getInt() != 0x45564157 /* WAVE */) throw new IOException(file + " is not a WAV");

		int format = -1, channels = 0, rate = 0, bits = 0;
		while (in.remaining() >= 8)
		{
			int id = in.getInt();
			int size = in.getInt();
			if (id == 0x20746d66 /* fmt  */)
			{
				int start = in.position();
				format = in.getShort() & 0xFFFF;
				channels = in.getShort();
				rate = in.getInt();
				in.getInt(); in.getShort();
				bits = in.getShort();
				in.position(start + size);
			}
			else if (id == 0x61746164 /* data */)
			{
				// WAVE_FORMAT_EXTENSIBLE (0xFFFE) is what OpenAL Soft writes, PCM inside.
				if ((format != 1 && format != 0xFFFE) || bits != 16)
					throw new IOException(file + ": expected 16-bit PCM, got format " + format + " at " + bits + " bits");
				int frames = in.remaining() / (2 * channels);
				float[] mono = new float[frames];
				for (int i = 0; i < frames; i++)
				{
					float sum = 0;
					for (int c = 0; c < channels; c++) sum += in.getShort() / 32768f;
					mono[i] = sum / channels;
				}
				return new AudioCapture(file, rate, channels, mono);
			}
			else in.position(Math.min(in.limit(), in.position() + size + (size & 1)));
		}
		throw new IOException(file + " has no data chunk");
	}

	public double seconds()
	{
		return samples.length / (double) sampleRate;
	}

	/** Loudness in windows of this length, from the start of the file: root mean square, 0 to 1. */
	public double[] rms(double windowSeconds)
	{
		int window = Math.max(1, (int) (windowSeconds * sampleRate));
		double[] out = new double[samples.length / window];
		for (int w = 0; w < out.length; w++)
		{
			double sum = 0;
			for (int i = w * window; i < (w + 1) * window; i++) sum += samples[i] * samples[i];
			out[w] = Math.sqrt(sum / window);
		}
		return out;
	}

	/** RMS as decibels below full scale, for messages a person can read. */
	public static String db(double rms)
	{
		return rms <= 0 ? "-inf dB" : String.format("%.0f dB", 20 * Math.log10(rms));
	}
}

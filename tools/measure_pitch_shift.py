#!/usr/bin/env python3
"""
How far a carriage's song sits above or below intro.mp3, in semitones, measured from the files
(r123): the long-term spectrum of each on a log-frequency axis, and the shift that lines them up
best. Tempo does not move a spectrum, so this reads the pitch alone.

    tools/measure_pitch_shift.py                       every wa<n>_modulated.ogg
    tools/measure_pitch_shift.py path/to/file.ogg ...  those files

Needs ffmpeg and numpy.
"""
import glob
import os
import subprocess
import sys

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, "desktop", "assets", "musics", "intro.mp3")
RATE = 22050
BINS_PER_SEMITONE = 10
FFT = 8192


def mono(path):
	raw = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-ac", "1", "-ar", str(RATE), "-f", "f32le", "-"],
		check=True, stdout=subprocess.PIPE).stdout
	return np.frombuffer(raw, dtype=np.float32).astype(np.float64)


def log_spectrum(samples):
	"""Mean magnitude spectrum, resampled on a semitone grid from 80 Hz to 5 kHz, in dB."""
	frames = len(samples) // FFT
	window = np.hanning(FFT)
	mag = np.zeros(FFT // 2 + 1)
	for i in range(frames):
		mag += np.abs(np.fft.rfft(samples[i * FFT:(i + 1) * FFT] * window))
	freqs = np.fft.rfftfreq(FFT, 1 / RATE)
	grid = 80 * 2 ** (np.arange(0, 12 * np.log2(5000 / 80) * BINS_PER_SEMITONE) / (12 * BINS_PER_SEMITONE))
	db = 20 * np.log10(np.interp(grid, freqs, mag) + 1e-9)
	# Remove the slow tilt (the EQ), keep the harmonic peaks that carry the pitch.
	kernel = np.ones(12 * BINS_PER_SEMITONE) / (12 * BINS_PER_SEMITONE)
	return db - np.convolve(db, kernel, mode="same")


def shift(reference, other):
	span = 4 * BINS_PER_SEMITONE
	cut = slice(span, len(reference) - span)
	scores = [np.dot(reference[cut], other[span + k:len(other) - span + k]) for k in range(-span, span + 1)]
	return (int(np.argmax(scores)) - span) / BINS_PER_SEMITONE


def main():
	paths = sys.argv[1:] or sorted(glob.glob(os.path.join(ROOT, "desktop", "assets", "musics", "wagons", "wa*_modulated.ogg")))
	reference = log_spectrum(mono(SOURCE))
	for path in paths:
		print("{}  {:+.1f} semitones".format(os.path.relpath(path, ROOT), shift(reference, log_spectrum(mono(path)))))


if __name__ == "__main__":
	main()

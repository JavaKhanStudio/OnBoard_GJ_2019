#!/usr/bin/env python3
"""
Build the candidates for the two game-event sounds the sound lab offers (r45): gaining a piece
of the key, and completing a level.

All but one are synthesised here, so they carry no licence. The exception reuses the public
domain steam whistle from tools/make_train_sounds.py - see desktop/assets/sounds/game/SOURCES.md.

They are tuned to G, D, A and E. musics/intro.mp3 reads as G major or E minor to a chroma
profile, without much confidence, and those four notes sit inside G major, E minor, D major and
D minor alike, so a chime cannot clash with the music under it whichever it is.

    tools/make_jingles.py          writes desktop/assets/sounds/game/*.ogg

Needs ffmpeg and numpy.
"""
import os
import sys

import numpy as np

sys.dont_write_bytecode = True   # no tools/__pycache__ from the import below
import make_train_sounds as train

RATE = train.RATE
train.OUT = os.path.join(train.ROOT, "desktop", "assets", "sounds", "game")

NOTE = {"G3": 196.00, "D4": 293.66, "A4": 440.00, "G4": 392.00,
	"D5": 587.33, "E5": 659.25, "G5": 783.99, "A5": 880.00,
	"D6": 1174.66, "E6": 1318.51, "G6": 1567.98, "A6": 1760.00, "D7": 2349.32, "E7": 2637.02}


def seconds(t):
	return np.arange(int(t * RATE)) / RATE


def partials(freq, length, table):
	"""A struck tone: (ratio, amplitude, decay seconds) per partial, with a 2 ms attack."""
	t = seconds(length)
	out = sum(a * np.sin(2 * np.pi * freq * r * t) * np.exp(-t / d) for r, a, d in table if freq * r < RATE / 2.2)
	attack = int(0.002 * RATE)
	out[:attack] *= np.linspace(0, 1, attack)
	return out


GLOCKENSPIEL = ((1, 1.0, 0.9), (2.76, 0.35, 0.35), (5.40, 0.15, 0.15), (8.93, 0.08, 0.07))
MUSIC_BOX = ((1, 1.0, 0.55), (3.0, 0.25, 0.2), (5.9, 0.12, 0.08))
MARIMBA = ((1, 1.0, 0.45), (3.93, 0.3, 0.08), (10.2, 0.08, 0.02))


def sequence(notes, length):
	"""(start seconds, note, timbre, loudness) laid into one buffer."""
	out = np.zeros(int(length * RATE))
	for start, note, timbre, loud in notes:
		tone = loud * partials(NOTE[note], length - start, timbre)
		i = int(start * RATE)
		out[i:i + len(tone)] += tone[:len(out) - i]
	return out


def keys_clink(rng, length=0.5):
	"""A small bunch of keys shaken once: bright, inharmonic, quickly damped hits."""
	out = np.zeros(int(length * RATE))
	for start in (0.0, 0.055, 0.12, 0.2):
		hit = partials(rng.uniform(2800, 3600), 0.25,
			((1, 1.0, 0.05), (1.47, 0.6, 0.04), (2.09, 0.4, 0.03), (2.93, 0.25, 0.02)))
		hit *= rng.uniform(0.5, 1.0)
		i = int((start + rng.normal(0, 0.004)) * RATE)
		out[i:i + len(hit)] += hit[:len(out) - i]
	return out


def pad(freqs, length, swell=0.35, release=1.2, rng=None):
	"""A soft held chord: detuned sines with a few weak harmonics, swelling in and dying away."""
	t = seconds(length)
	out = np.zeros(len(t))
	for f in freqs:
		for detune in (-0.8, 0.0, 0.8):
			for h, a in ((1, 1.0), (2, 0.3), (3, 0.12)):
				out += a * np.sin(2 * np.pi * (f + detune) * h * t + (rng.uniform(0, 6.28) if rng is not None else 0))
	envelope = np.minimum(1, t / swell) * np.exp(-np.maximum(0, t - swell) / release)
	return out * envelope


def normalise_peak_window(samples, db, window=0.1):
	n = int(window * RATE)
	loudest = max(train.rms(samples[i:i + n]) for i in range(0, len(samples) - n, n // 2))
	return train.limit(samples * (10 ** (db / 20) / loudest))


def tail(samples, fade=0.25):
	return train.fade(samples, 0.0, fade)


KEY_DB = -17.0
LEVEL_DB = -15.0


def main():
	rng = np.random.default_rng(45)

	# --- gaining a piece of the key -------------------------------------------------------
	write = train.write
	write("key_chime", normalise_peak_window(tail(sequence(
		[(0.0, "D6", GLOCKENSPIEL, 0.8), (0.09, "A6", GLOCKENSPIEL, 1.0)], 1.3)), KEY_DB))

	write("key_sparkle", normalise_peak_window(tail(sequence(
		[(0.0, "G6", MUSIC_BOX, 0.7), (0.05, "A6", MUSIC_BOX, 0.75), (0.10, "D7", MUSIC_BOX, 0.85), (0.15, "E7", MUSIC_BOX, 1.0)], 1.0)), KEY_DB))

	clink = keys_clink(rng)
	bell = sequence([(0.22, "D7", GLOCKENSPIEL, 0.6)], 1.2)
	bell[:len(clink)] += clink
	write("key_clink", normalise_peak_window(tail(bell), KEY_DB))

	write("key_wood", normalise_peak_window(tail(sequence(
		[(0.0, "D5", MARIMBA, 0.9), (0.11, "A5", MARIMBA, 1.0)], 0.9)), KEY_DB))

	# --- completing a level ---------------------------------------------------------------
	write("level_glockenspiel", normalise_peak_window(tail(sequence(
		[(0.0, "G5", GLOCKENSPIEL, 0.7), (0.14, "D6", GLOCKENSPIEL, 0.75), (0.28, "G6", GLOCKENSPIEL, 0.85),
		 (0.46, "D7", GLOCKENSPIEL, 0.7), (0.46, "G6", GLOCKENSPIEL, 0.5)], 2.4), 0.6), LEVEL_DB))

	write("level_music_box", normalise_peak_window(tail(sequence(
		[(0.0, "D6", MUSIC_BOX, 0.8), (0.11, "E6", MUSIC_BOX, 0.8), (0.22, "G6", MUSIC_BOX, 0.85),
		 (0.33, "A6", MUSIC_BOX, 0.85), (0.50, "D7", MUSIC_BOX, 1.0), (0.50, "G6", MUSIC_BOX, 0.5)], 2.0), 0.5), LEVEL_DB))

	chord = pad((NOTE["G3"], NOTE["D4"], NOTE["A4"]), 2.8, rng=rng)
	chord = chord / np.abs(chord).max() * 0.35
	chord += sequence([(0.30, "G6", GLOCKENSPIEL, 0.5), (0.30, "D6", GLOCKENSPIEL, 0.35)], 2.8)
	write("level_swell", normalise_peak_window(tail(chord, 0.6), LEVEL_DB))

	whistle = train.fade(train.cut(train.source("whistle"), 0, 1.6), 0.02, 0.5)
	whistle = whistle / np.abs(whistle).max() * 0.6
	out = sequence([(1.1, "G6", GLOCKENSPIEL, 0.55), (1.24, "D7", GLOCKENSPIEL, 0.6)], 2.6)
	out[:len(whistle)] += whistle
	write("level_whistle", normalise_peak_window(tail(out, 0.5), LEVEL_DB))


if __name__ == "__main__":
	sys.exit(main())

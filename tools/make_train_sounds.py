#!/usr/bin/env python3
"""
Build the train sound candidates the sound lab offers: rails beds that loop under a level,
and departures that play once when a new game opens level 1.

Nobody on the team recorded these. The recordings come from Wikimedia Commons, public domain,
CC0 or CC BY only, and every one is named with its author and licence in
desktop/assets/sounds/train/SOURCES.md. Two rails beds are synthesised here instead, so there
is a candidate with the clickety-clack of jointed track that no recording on Commons has.

What it does to them, so the lab compares like with like:
  - cuts the useful stretch (the times below were read off spectrograms)
  - loops a rails bed with an equal-power crossfade, so the seam is not a click
  - normalises: rails to one RMS, departures to one loudest-second RMS, peaks under -1 dBFS
  - mono Vorbis, 44.1 kHz

    tools/make_train_sounds.py            downloads the sources into build/train-sounds-src
                                          and writes desktop/assets/sounds/train/*.ogg

Needs ffmpeg and numpy.
"""
import os
import subprocess
import sys
import urllib.request
import wave

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "build", "train-sounds-src")
OUT = os.path.join(ROOT, "desktop", "assets", "sounds", "train")
RATE = 44100

COMMONS = "https://upload.wikimedia.org/wikipedia/commons/"
SOURCES = {
	"garratt": COMMONS + "2/21/South_Australian_Railways_Garratt_406_starts_off.ogg",
	"p8":      COMMONS + "b/be/WWS_PrussianpassengersteamlocomotiveP-8.ogg",
	"whistle": COMMONS + "c/c6/Parovoz_sound.ogg",
	"ride":    COMMONS + "a/a2/Complete_train_ride_4_minutes.ogg",
	"northern": COMMONS + "0/07/Northern_Trains_323239_DMSO_A%2C_on_the_Crewe_to_Manchester_line%2C_Jan_2022.ogg",
}


def source(name):
	"""The recording as mono float samples at RATE, downloaded once."""
	os.makedirs(CACHE, exist_ok=True)
	ogg = os.path.join(CACHE, name + ".ogg")
	wav = os.path.join(CACHE, name + ".wav")
	if not os.path.isfile(ogg):
		request = urllib.request.Request(SOURCES[name], headers={"User-Agent": "onboard-tools/1.0"})
		with urllib.request.urlopen(request) as response, open(ogg, "wb") as out:
			out.write(response.read())
	if not os.path.isfile(wav):
		subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", ogg, "-ac", "1", "-ar", str(RATE), wav], check=True)
	with wave.open(wav) as w:
		return np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float64) / 32768


def cut(samples, start, end):
	return samples[int(start * RATE):int(end * RATE)].copy()


def rms(samples):
	return np.sqrt(np.mean(samples ** 2) + 1e-12)


def fade(samples, fade_in=0.0, fade_out=0.0):
	out = samples.copy()
	if fade_in > 0:
		n = int(fade_in * RATE)
		out[:n] *= np.sin(np.linspace(0, np.pi / 2, n)) ** 2
	if fade_out > 0:
		n = int(fade_out * RATE)
		out[-n:] *= np.cos(np.linspace(0, np.pi / 2, n)) ** 2
	return out


def loop(samples, length, crossfade=1.5):
	"""length seconds that repeat without a seam: the stretch just past the end fades into the start."""
	n, x = int(length * RATE), int(crossfade * RATE)
	assert len(samples) >= n + x, "not enough recording to loop"
	t = np.linspace(0, np.pi / 2, x)
	out = samples[:n].copy()
	out[:x] = samples[:x] * np.sin(t) + samples[n:n + x] * np.cos(t)
	return out


def normalise_rms(samples, db):
	return limit(samples * (10 ** (db / 20) / rms(samples)))


def normalise_loudest_second(samples, db):
	windows = [rms(samples[i:i + RATE]) for i in range(0, max(1, len(samples) - RATE), RATE // 4)]
	return limit(samples * (10 ** (db / 20) / max(windows)))


def limit(samples, ceiling_db=-1.0):
	ceiling = 10 ** (ceiling_db / 20)
	peak = np.abs(samples).max()
	return samples * (ceiling / peak) if peak > ceiling else samples


def spectrum_noise(length, shape, rng):
	"""Noise with a given spectral shape, built in the frequency domain - so it loops perfectly."""
	n = int(length * RATE)
	freqs = np.fft.rfftfreq(n, 1 / RATE)
	phases = rng.uniform(0, 2 * np.pi, len(freqs))
	spectrum = shape(np.maximum(freqs, 1.0)) * np.exp(1j * phases)
	spectrum[0] = 0
	out = np.fft.irfft(spectrum, n)
	return out / rms(out)


def clacks(length, rng, speed=16.7, joint=18.3, bogie=14.0, axle=2.6):
	"""
	Wheels over rail joints, heard from inside a carriage: each joint is struck by the front
	bogie's two axles, then the rear bogie's - the "da-dum ... da-dum" of old jointed track.
	The loop holds a whole number of joints, so the rhythm carries across the seam.
	"""
	n = int(length * RATE)
	out = np.zeros(n)
	period = joint / speed
	joints = int(round(length / period))
	period = length / joints
	hit_length = int(0.12 * RATE)
	t = np.arange(hit_length) / RATE
	for j in range(joints):
		for offset in (0.0, axle / speed, bogie / speed, (bogie + axle) / speed):
			when = j * period + offset + rng.normal(0, 0.004)
			strength = rng.uniform(0.7, 1.0) * (1.0 if offset in (0.0, axle / speed) else 0.8)
			hit = sum(a * np.sin(2 * np.pi * f * rng.uniform(0.95, 1.05) * t) * np.exp(-t / d)
				for f, a, d in ((150, 1.0, 0.045), (420, 0.7, 0.03), (1150, 0.35, 0.015), (2600, 0.15, 0.008)))
			hit += 0.5 * rng.normal(0, 1, hit_length) * np.exp(-t / 0.01)
			start = int(when * RATE) % n
			idx = (np.arange(hit_length) + start) % n
			out[idx] += strength * hit
	return out / np.abs(out).max()


def write(name, samples):
	os.makedirs(OUT, exist_ok=True)
	wav = os.path.join(CACHE, name + ".out.wav")
	with wave.open(wav, "wb") as w:
		w.setnchannels(1)
		w.setsampwidth(2)
		w.setframerate(RATE)
		w.writeframes((np.clip(samples, -1, 1) * 32767).astype(np.int16).tobytes())
	target = os.path.join(OUT, name + ".ogg")
	subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", "-map_metadata", "-1", target], check=True)
	print("%-28s %5.1f s  %4d KB" % (name + ".ogg", len(samples) / RATE, os.path.getsize(target) // 1024))


RAILS_DB = -20.0
DEPARTURE_DB = -14.0


def main():
	rng = np.random.default_rng(39)

	# --- rails beds ----------------------------------------------------------------------
	ride = source("ride")
	write("rails_ride", normalise_rms(loop(cut(ride, 70, 96), 24), RAILS_DB))

	northern = source("northern")
	write("rails_northern", normalise_rms(loop(cut(northern, 200, 226), 24), RAILS_DB))

	length = 17.52   # 16 joints at the default speed
	# Wheel roar: strong under 300 Hz and rolling off, with a faint hiss of steel on steel above.
	roar = spectrum_noise(length, lambda f: (f / 40) ** -0.5 / (1 + (f / 500) ** 3), rng)
	hiss = spectrum_noise(length, lambda f: np.exp(-((np.log(f) - np.log(2500)) ** 2) / 0.5), rng)
	write("rails_synth", normalise_rms(roar + 0.08 * hiss + 6.0 * clacks(length, rng), RAILS_DB))

	bed = loop(cut(ride, 110, 130), length)
	write("rails_ride_clacks", normalise_rms(bed / rms(bed) + 3.0 * clacks(length, rng), RAILS_DB))

	# --- departures ----------------------------------------------------------------------
	garratt = fade(cut(source("garratt"), 5, 32), 0.3, 6)   # the first 5 s is the engine standing
	p8 = fade(cut(source("p8"), 0, 14.5), 0.2, 2.5)
	whistle = fade(cut(source("whistle"), 0, 3.4), 0.02, 0.8)

	write("departure_garratt", normalise_loudest_second(garratt, DEPARTURE_DB))
	write("departure_p8", normalise_loudest_second(p8, DEPARTURE_DB))

	def after_whistle(chuffs, at=2.6):
		chuffs = chuffs / rms(chuffs[-8 * RATE:]) * rms(whistle) * 0.6
		out = np.zeros(int(at * RATE) + len(chuffs))
		out[:len(whistle)] += whistle
		out[int(at * RATE):] += chuffs
		return out

	write("departure_whistle_garratt", normalise_loudest_second(after_whistle(garratt), DEPARTURE_DB))
	write("departure_whistle_p8", normalise_loudest_second(after_whistle(p8), DEPARTURE_DB))


if __name__ == "__main__":
	sys.exit(main())

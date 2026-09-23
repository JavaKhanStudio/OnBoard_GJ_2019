#!/usr/bin/env python3
"""
Build the footstep candidates the sound lab offers (r87): one step of Ross's on the carriage
floor. The game plays one each time a heel comes down in his walk cycle (move frames 4 and 8,
SpriteModel.playFootsteps), at a slightly different pitch each time, so a file holds ONE step.

Three are synthesised here and carry no licence. The fourth is a public domain recording from
Wikimedia Commons - see desktop/assets/sounds/steps/SOURCES.md.

A step is a heel strike and, 60 to 80 ms later, the ball of the foot: each a burst of shaped
noise over the few low resonances of whatever floor it lands on. Every candidate is levelled to
the same loudest 20 ms, so the lab compares the sound and not the volume.

    tools/make_footsteps.py          writes desktop/assets/sounds/steps/*.ogg

Needs ffmpeg and numpy.
"""
import os
import subprocess
import sys
import urllib.request
import wave

import numpy as np

sys.dont_write_bytecode = True   # no tools/__pycache__ from the import below
import make_train_sounds as train

RATE = train.RATE
CACHE = train.CACHE = os.path.join(train.ROOT, "build", "footstep-src")
train.OUT = os.path.join(train.ROOT, "desktop", "assets", "sounds", "steps")

THUD = "https://upload.wikimedia.org/wikipedia/commons/5/5b/Dull_thud.ogg"
LENGTH = 0.26
STEP_DB = -12.0


def seconds(t):
	return np.arange(int(t * RATE)) / RATE


def band_noise(length, low, high, rng):
	"""White noise kept between low and high Hz, with soft shoulders so it does not ring."""
	n = int(length * RATE)
	freqs = np.fft.rfftfreq(n, 1 / RATE)
	shape = 1 / (1 + (low / np.maximum(freqs, 1)) ** 4) / (1 + (freqs / high) ** 4)
	out = np.fft.irfft(np.fft.rfft(rng.normal(0, 1, n)) * shape, n)
	return out / np.abs(out).max()


def strike(length, noise, decay, modes, rng):
	"""A contact: noise decaying over `decay` s, plus struck floor modes (Hz, amplitude, decay s)."""
	t = seconds(length)
	out = noise * np.exp(-t / decay)
	for freq, amplitude, ring in modes:
		out += amplitude * np.sin(2 * np.pi * freq * rng.uniform(0.97, 1.03) * t) * np.exp(-t / ring)
	attack = int(0.0015 * RATE)
	out[:attack] *= np.linspace(0, 1, attack)
	return out


def step(rng, heel, toe, toe_at, toe_level):
	"""Heel, then the ball of the foot, as one sound of LENGTH seconds."""
	out = np.zeros(int(LENGTH * RATE))
	h = heel(rng)
	out[:len(h)] += h[:len(out)]
	t = toe(rng) * toe_level
	start = int(toe_at * RATE)
	out[start:start + len(t)] += t[:len(out) - start]
	return out


def normalise_peak_window(samples, db, window=0.02):
	n = int(window * RATE)
	loudest = max(train.rms(samples[i:i + n]) for i in range(0, len(samples) - n, n // 4))
	return train.limit(samples * (10 ** (db / 20) / loudest))


def recording(url, name):
	os.makedirs(CACHE, exist_ok=True)
	ogg, wav = os.path.join(CACHE, name + ".ogg"), os.path.join(CACHE, name + ".wav")
	if not os.path.isfile(ogg):
		request = urllib.request.Request(url, headers={"User-Agent": "onboard-tools/1.0"})
		with urllib.request.urlopen(request) as response, open(ogg, "wb") as out:
			out.write(response.read())
	if not os.path.isfile(wav):
		subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", ogg, "-ac", "1", "-ar", str(RATE), wav], check=True)
	with wave.open(wav) as w:
		return np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(np.float64) / 32768


def main():
	rng = np.random.default_rng(87)
	# The carriage floor: planks over a frame, a hollow thump near 110 Hz and a few wood modes.
	floor = ((110, 0.9, 0.045), (240, 0.45, 0.03), (520, 0.2, 0.02), (1050, 0.08, 0.012))

	# A soft leather sole: dull heel, a lighter scuff as the foot rolls.
	shoe = step(rng,
		lambda r: strike(0.2, band_noise(0.2, 120, 1400, r), 0.012, floor, r),
		lambda r: strike(0.15, band_noise(0.15, 300, 3000, r), 0.018, floor[1:], r),
		0.075, 0.45)

	# Boots: a hard heel with a click on top, the floor ringing longer.
	boot_floor = tuple((f, a * 1.2, d * 1.3) for f, a, d in floor)
	boot = step(rng,
		lambda r: strike(0.22, band_noise(0.22, 150, 4500, r), 0.008, boot_floor, r),
		lambda r: strike(0.15, band_noise(0.15, 400, 5000, r), 0.01, boot_floor[1:], r),
		0.065, 0.55)

	# A carpet runner down the aisle: the floor is barely heard, the step is a muffled press.
	runner_floor = ((95, 0.6, 0.035), (200, 0.2, 0.02))
	runner = step(rng,
		lambda r: strike(0.2, band_noise(0.2, 60, 600, r), 0.02, runner_floor, r),
		lambda r: strike(0.15, band_noise(0.15, 100, 900, r), 0.022, runner_floor[1:], r),
		0.08, 0.35)

	# gregoryweir's dull thud, cut short: under 0.2 s it reads as a footfall rather than a knock.
	thud = train.fade(recording(THUD, "thud")[:int(0.2 * RATE)], 0.002, 0.08)

	for name, samples in (("steps_shoe", shoe), ("steps_boot", boot), ("steps_runner", runner), ("steps_thud", thud)):
		train.write(name, normalise_peak_window(train.fade(samples, 0, 0.04), STEP_DB))


if __name__ == "__main__":
	sys.exit(main())

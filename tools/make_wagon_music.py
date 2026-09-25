#!/usr/bin/env python3
"""
The main song, aged once per carriage (r94): musics/intro.mp3 rendered four ways, so the sound
lab and the Carriage labs can hear whether one song that changes colour carries the four ages
as well as four songs would.

The carriages are Ross's four ages, one season each (see musics/PROMPTS.md): wa1 Printemps the
child, wa2 Ete the young man, wa3 Automne the adult, wa4 Hiver the old man. The song gets a
little older with him - brighter and quicker at first, then lower, slower, further away:

    wa1  a semitone up, 4 % quicker, the bass thinned and the top lifted: light, a child's size
    wa2  the song as written, a touch warmer and wider: the only carriage that sounds like it
    wa3  a tone down, 6 % slower, the top dulled, a hall around it: heavier, looking back
    wa4  a minor third down, 14 % slower, band-limited like an old wireless, a long room and a
         slight tape wow: the same song remembered rather than played

"Slightly" is the brief, so none of this rewrites the notes: pitch and tempo move separately
(rubberband), so a carriage is never just the tape sped up. The tempo ratios are repeated in
Enum_Music.CarriageVariant, which carries the playback position from one carriage to the next:
change one, change both.

The reverb is applied circularly - its tail wraps round to the start - because the files loop:
a tail cut off at the end would click at every pass.

    tools/make_wagon_music.py        writes desktop/assets/musics/wagons/wa<n>_modulated.ogg
    tools/make_wagon_music.py 1      only wa1: the others keep their files, and wa1's room is the
                                     same random draw as a full run, so only the recipe changes

wa1 was a whole tone up until r123, when Simon heard it as a bit too high: tools/measure_pitch_shift.py
reads each file's shift back.

Needs ffmpeg (with rubberband and libvorbis) and numpy.
"""
import os
import subprocess
import sys

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, "desktop", "assets", "musics", "intro.mp3")
OUT = os.path.join(ROOT, "desktop", "assets", "musics", "wagons")
RATE = 44100

# semitones, tempo ratio, ffmpeg EQ chain, reverb (seconds, wet), level in dB against the source
RECIPES = {
	1: dict(semitones=+1, tempo=1.04,
		eq="highpass=f=140,treble=g=3:f=5000,bass=g=-2:f=200",
		reverb=(0.8, 0.10), gain=0.0),
	2: dict(semitones=0, tempo=1.00,
		eq="bass=g=1.5:f=180,treble=g=-1:f=7000,extrastereo=m=1.3",
		reverb=(1.2, 0.12), gain=0.0),
	3: dict(semitones=-2, tempo=0.94,
		eq="lowpass=f=6500,treble=g=-3:f=3500,equalizer=f=400:t=q:w=1:g=1.5",
		reverb=(2.4, 0.24), gain=-1.0),
	4: dict(semitones=-3, tempo=0.86,
		eq="highpass=f=180,lowpass=f=3000,equalizer=f=1200:t=q:w=1:g=2,vibrato=f=0.45:d=0.04",
		reverb=(4.0, 0.38), gain=-3.0),
}


def render(recipe):
	"""The source through rubberband and the EQ, as float stereo samples at RATE."""
	chain = "rubberband=pitch={:.6f}:tempo={:.4f}:pitchq=quality:formant=preserved:transients=mixed,{}".format(
		2 ** (recipe["semitones"] / 12), recipe["tempo"], recipe["eq"])
	raw = subprocess.run(["ffmpeg", "-v", "error", "-i", SOURCE, "-af", chain, "-ac", "2", "-ar", str(RATE),
		"-f", "f32le", "-"], check=True, stdout=subprocess.PIPE).stdout
	return np.frombuffer(raw, dtype=np.float32).reshape(-1, 2).astype(np.float64)


def impulse(seconds, rng):
	"""A room: decaying noise, darker as it dies, a little different in each ear."""
	n = int(seconds * RATE)
	t = np.arange(n) / RATE
	decay = np.exp(-6.9 * t / seconds)  # -60 dB at the end
	ir = np.empty((n, 2))
	for ch in range(2):
		noise = rng.standard_normal(n) * decay
		# One-pole low-pass whose cutoff falls with time: high frequencies die first.
		smooth = np.empty(n)
		y = 0.0
		alpha = 0.35 + 0.6 * (t / seconds)
		for i in range(n):
			y = alpha[i] * y + (1 - alpha[i]) * noise[i]
			smooth[i] = y
		ir[:, ch] = smooth / np.sqrt(np.sum(smooth ** 2))
	predelay = int(0.02 * RATE)
	return np.vstack([np.zeros((predelay, 2)), ir])


def circular_reverb(dry, seconds, wet, rng):
	ir = impulse(seconds, rng)
	n = len(dry)
	out = np.empty_like(dry)
	for ch in range(2):
		kernel = np.zeros(n)
		kernel[:len(ir)] = ir[:, ch]
		out[:, ch] = np.fft.irfft(np.fft.rfft(dry[:, ch]) * np.fft.rfft(kernel), n)
	return (1 - wet) * dry + wet * out


def rms(samples):
	return np.sqrt(np.mean(samples ** 2) + 1e-12)


def main():
	os.makedirs(OUT, exist_ok=True)
	source = render(dict(semitones=0, tempo=1.0, eq="anull"))
	rng = np.random.default_rng(94)
	only = {int(a) for a in sys.argv[1:]} or set(RECIPES)
	for carriage, recipe in RECIPES.items():
		if carriage not in only:
			impulse(recipe["reverb"][0], rng)  # draw its room anyway, so the next carriage's is the same
			continue
		samples = render(recipe)
		samples = circular_reverb(samples, *recipe["reverb"], rng)
		# Loudness matched to the source, then the recipe's own offset: an older carriage sits
		# a little further back, not a little louder because its reverb added energy.
		samples *= rms(source) / rms(samples) * 10 ** (recipe["gain"] / 20)
		peak = np.max(np.abs(samples))
		if peak > 0.97:
			samples *= 0.97 / peak
		path = os.path.join(OUT, "wa{}_modulated.ogg".format(carriage))
		subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "f32le", "-ar", str(RATE), "-ac", "2", "-i", "-",
			"-c:a", "libvorbis", "-q:a", "4", path], check=True, input=samples.astype(np.float32).tobytes())
		print("{}  {:.1f} s  {} KB".format(os.path.relpath(path, ROOT), len(samples) / RATE, os.path.getsize(path) // 1024))


if __name__ == "__main__":
	main()

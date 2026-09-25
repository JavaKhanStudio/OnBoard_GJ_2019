#!/usr/bin/env python3
"""How deep the music dips by itself (r113): for each track, the deepest 50 ms window under the
loudest within NEAR windows on both sides. CarriageMusicTest.HOLE_DB must sit above this, or the
song's own phrase endings read as a hole at the carriage change. Needs ffmpeg and numpy.
Usage: tools/music_troughs.py [file ...]  (default: the aged carriage songs and intro.mp3)"""
import glob, subprocess, sys
import numpy as np

NEAR = 10

def levels(path, step=0.05, rate=44100):
    raw = subprocess.run(['ffmpeg', '-v', 'quiet', '-i', path, '-ac', '1', '-f', 'f32le', '-ar', str(rate), '-'],
                         capture_output=True, check=True).stdout
    x = np.frombuffer(raw, dtype=np.float32)
    n = int(rate * step)
    return np.array([np.sqrt(np.mean(x[i:i + n] ** 2)) + 1e-9 for i in range(0, len(x) - n, n)])

files = sys.argv[1:] or sorted(glob.glob('desktop/assets/musics/wagons/wa*_modulated.ogg')) + ['desktop/assets/musics/intro.mp3']
for f in files:
    l = levels(f)
    depth = max(20 * np.log10(min(l[i - NEAR:i].max(), l[i + 1:i + 1 + NEAR].max()) / l[i]) for i in range(NEAR, len(l) - NEAR))
    print(f'{depth:5.1f} dB  {f}')

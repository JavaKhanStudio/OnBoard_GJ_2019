#!/usr/bin/env python3
"""Sheets and videos of the steam leaving a carriage, one per motion (r262).

Reads the frames SteamMotionRenderTest writes - one per 1/30 s of game time, from Play on -
and makes, for each motion:
  <out>/steam-motion-<motion>.mp4   the play at its real speed
and one sheet of all of them, a row per motion, a cell every STEP seconds:
  <out>/steam-motion-sheet.png

    ./gradlew :verify:test -PwithGl --tests jks.verify.SteamMotionRenderTest
    python3 tools/steam_motion_sheet.py verify/build/frames/steam-motion out-dir
"""
import os
import subprocess
import sys

from PIL import Image, ImageDraw

FPS = 30
STEP = 0.5
CELLS = 18
SCALE = 0.5


def main(src, out):
    os.makedirs(out, exist_ok=True)
    motions = sorted(d for d in os.listdir(src) if os.path.isdir(os.path.join(src, d)))
    order = ["snap", "breathe", "gather", "drift"]
    motions.sort(key=lambda m: order.index(m) if m in order else 99)
    rows = []
    for m in motions:
        d = os.path.join(src, m)
        frames = sorted(f for f in os.listdir(d) if f.endswith(".png"))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(FPS),
                        "-i", os.path.join(d, "%04d.png"), "-pix_fmt", "yuv420p",
                        "-vf", "scale=trunc(iw/2)*2:trunc(ih/2)*2",
                        os.path.join(out, "steam-motion-%s.mp4" % m)], check=True)
        cells = []
        for i in range(CELLS):
            n = int(round(i * STEP * FPS))
            if n >= len(frames):
                break
            im = Image.open(os.path.join(d, frames[n])).convert("RGB")
            im = im.resize((int(im.width * SCALE), int(im.height * SCALE)), Image.LANCZOS)
            ImageDraw.Draw(im).text((4, 2), "%.1f s" % (i * STEP), fill=(255, 255, 0))
            cells.append(im)
        rows.append((m, cells))
    cw, ch = rows[0][1][0].size
    label = 70
    sheet = Image.new("RGB", (label + CELLS * (cw + 2), len(rows) * (ch + 2)), (255, 255, 255))
    draw = ImageDraw.Draw(sheet)
    for r, (m, cells) in enumerate(rows):
        y = r * (ch + 2)
        draw.text((4, y + ch // 2), m.upper(), fill=(0, 0, 0))
        for i, im in enumerate(cells):
            sheet.paste(im, (label + i * (cw + 2), y))
    sheet.save(os.path.join(out, "steam-motion-sheet.png"))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])

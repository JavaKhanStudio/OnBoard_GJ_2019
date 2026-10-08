#!/usr/bin/env python3
"""The thought cloud dissolving from its bottom up as the steam closes (r262), close up.

Reads the frames SteamMotionRenderTest writes (a third of 1280x720, one per 1/30 s from Play)
and lays the cloud's corner of the screen, three times bigger, every 0.1 s from FROM to TO:

    ./gradlew :verify:test -PwithGl --tests jks.verify.SteamMotionRenderTest
    python3 tools/steam_bubble_sheet.py verify/build/frames/steam-motion/gather out.png
"""
import sys
from PIL import Image, ImageDraw

FROM, TO, STEP = 4.9, 6.0, 0.1
BOX = (20, 0, 260, 120)


def main(src, out):
    cells = []
    t = FROM
    while t <= TO + 1e-6:
        cell = Image.open('%s/%04d.png' % (src, round(t * 30))).crop(BOX)
        cells.append((t, cell.resize((cell.width * 3, cell.height * 3), Image.LANCZOS)))
        t += STEP
    w, h = cells[0][1].size
    cols = 4
    rows = (len(cells) + cols - 1) // cols
    sheet = Image.new('RGB', (cols * w, rows * (h + 18)), 'white')
    draw = ImageDraw.Draw(sheet)
    for k, (t, cell) in enumerate(cells):
        x, y = (k % cols) * w, (k // cols) * (h + 18)
        sheet.paste(cell, (x, y + 18))
        draw.text((x + 4, y + 3), '%.1f s after Play' % t, fill='black')
    sheet.save(out)


main(sys.argv[1], sys.argv[2])

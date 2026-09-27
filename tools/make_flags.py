#!/usr/bin/env python3
"""
Draw the start menu's language flags (r162): France for fr, the Union Jack for en.

Drawn here rather than fetched, so there is no licence to carry (D5). Drawn big and scaled
down with a box filter, so the diagonals are smooth; an ink outline, the colour of the
lettering on the team's painted boards, keeps them from looking pasted onto the painting.

    tools/make_flags.py desktop/assets/ui/icon/flags
"""
import math
import os
import sys

from PIL import Image, ImageDraw

W, H = 90, 60          # the size shipped, 3:2
SS = 8                 # drawn this many times bigger
INK = (59, 38, 24, 255)
BLUE, WHITE, RED = (0, 35, 149, 255), (255, 255, 255, 255), (237, 41, 57, 255)
UK_BLUE, UK_RED = (1, 33, 105, 255), (200, 16, 46, 255)


def france(d, w, h):
    d.rectangle([0, 0, w // 3, h], fill=BLUE)
    d.rectangle([w // 3, 0, 2 * w // 3, h], fill=WHITE)
    d.rectangle([2 * w // 3, 0, w, h], fill=RED)


def band(d, x0, y0, x1, y1, width, fill):
    """A straight band of the given width between two points."""
    dx, dy = x1 - x0, y1 - y0
    length = math.hypot(dx, dy)
    nx, ny = -dy / length * width / 2, dx / length * width / 2
    d.polygon([(x0 + nx, y0 + ny), (x1 + nx, y1 + ny), (x1 - nx, y1 - ny), (x0 - nx, y0 - ny)], fill=fill)


def union_jack(d, w, h):
    d.rectangle([0, 0, w, h], fill=UK_BLUE)
    unit = h / 30                        # the flag is 60 x 30 units; this one is cropped to 3:2
    band(d, 0, 0, w, h, 6 * unit, WHITE)
    band(d, 0, h, w, 0, 6 * unit, WHITE)
    # The red saltire is offset: counter-changed, each red band on one side of its white one.
    off = unit
    band(d, 0, 0 + off, w / 2, h / 2 + off, 2 * unit, UK_RED)
    band(d, w / 2, h / 2 - off, w, h - off, 2 * unit, UK_RED)
    band(d, 0, h - off, w / 2, h / 2 - off, 2 * unit, UK_RED)
    band(d, w / 2, h / 2 + off, w, 0 + off, 2 * unit, UK_RED)
    d.rectangle([w / 2 - 5 * unit, 0, w / 2 + 5 * unit, h], fill=WHITE)
    d.rectangle([0, h / 2 - 5 * unit, w, h / 2 + 5 * unit], fill=WHITE)
    d.rectangle([w / 2 - 3 * unit, 0, w / 2 + 3 * unit, h], fill=UK_RED)
    d.rectangle([0, h / 2 - 3 * unit, w, h / 2 + 3 * unit], fill=UK_RED)


def flag(paint):
    w, h = W * SS, H * SS
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    inner = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    paint(ImageDraw.Draw(inner), w, h)
    # Rounded a little, and outlined in ink.
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([SS, SS, w - 1 - SS, h - 1 - SS], radius=5 * SS, fill=255)
    img.paste(inner, (0, 0), mask)
    ImageDraw.Draw(img).rounded_rectangle([SS, SS, w - 1 - SS, h - 1 - SS], radius=5 * SS, outline=INK, width=3 * SS)
    return img.resize((W, H), Image.BOX)


def main(target):
    os.makedirs(target, exist_ok=True)
    flag(france).save(os.path.join(target, "fr.png"))
    flag(union_jack).save(os.path.join(target, "en.png"))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "desktop/assets/ui/icon/flags")

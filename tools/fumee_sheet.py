#!/usr/bin/env python3
"""Lay the nine frames of ui/cinematic/fumee.atlas over the last intro page, as a 3x3 sheet.

The steam wipe was drawn in 2019 and never wired up (r79, r85). This shows what it does
without GL: each frame is a full 1920x1080 overlay, composited on introPage3.

    python3 tools/fumee_sheet.py build/r79/fumee_over_intro3.png
"""
import sys
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "desktop" / "assets"
FRAME_W, FRAME_H = 1920, 1080
THUMB_W, THUMB_H = 480, 270


def main(out):
    atlas = Image.open(ASSETS / "ui/cinematic/fumee.png").convert("RGBA")
    page = Image.open(ASSETS / "ui/story/intro/introPage3.png").convert("RGBA").resize((FRAME_W, FRAME_H))
    sheet = Image.new("RGBA", (3 * THUMB_W, 3 * THUMB_H))
    # fumee.atlas packs index 1..9 row by row, three to a row, with no padding.
    for i in range(9):
        x, y = (i % 3) * FRAME_W, (i // 3) * FRAME_H
        frame = atlas.crop((x, y, x + FRAME_W, y + FRAME_H))
        shown = Image.alpha_composite(page, frame).resize((THUMB_W, THUMB_H))
        sheet.paste(shown, ((i % 3) * THUMB_W, (i // 3) * THUMB_H))
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    sheet.convert("RGB").save(out)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "build/fumee_over_intro3.png")

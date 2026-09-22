#!/usr/bin/env python3
"""Draw a whole carriage the way WagonLevel.draw does, without the game: WAGON.png at
WIDTH x (900 - BAR) over the black bar, and every item at its native size at (posX, posY).

Made for d13 (r70), to see whether the items still sit on the art once the art is drawn at
its painted shape. No parallax, no Ross - only the art and the items.

    tools/carriage_composite.py --width 2844.44 out/dir     # one PNG per carriage
"""
import argparse
import json
import os

from PIL import Image

WAGONS = os.path.join(os.path.dirname(__file__), "..", "desktop", "assets", "game", "wagon")
WORLD_HEIGHT, BAR = 900, 100


def composite(level, width):
    with open(os.path.join(WAGONS, "wa%d.wa" % level), encoding="utf-8") as f:
        data = json.load(f)
    art_dir = os.path.join(WAGONS, data["path_meta"])
    w = round(width)
    frame = Image.new("RGB", (w, WORLD_HEIGHT), (255, 255, 255))
    art = Image.open(os.path.join(art_dir, "WAGON.png")).convert("RGBA")
    frame.paste(art.resize((w, WORLD_HEIGHT - BAR), Image.LANCZOS), (0, 0), art.resize((w, WORLD_HEIGHT - BAR), Image.LANCZOS))
    frame.paste((0, 0, 0), (0, WORLD_HEIGHT - BAR, w, WORLD_HEIGHT))
    for item in data["listItems"]:
        tex = Image.open(os.path.join(WAGONS, item["path"], item["path_EtatDebut"])).convert("RGBA")
        # The world's y goes up from the bottom; the image's goes down from the top.
        x, y = round(item["posX"]), round(WORLD_HEIGHT - item["posY"] - tex.height)
        frame.paste(tex, (x, y), tex)
    return frame


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--width", type=float, required=True, help="WagonLevel.WIDTH, in world units")
    p.add_argument("out")
    a = p.parse_args()
    os.makedirs(a.out, exist_ok=True)
    for level in range(1, 5):
        path = os.path.join(a.out, "carriage%d.png" % level)
        composite(level, a.width).save(path)
        print(path)


if __name__ == "__main__":
    main()

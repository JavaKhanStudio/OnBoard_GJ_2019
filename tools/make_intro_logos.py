#!/usr/bin/env python3
"""
Build the logos the intro's story pages carry (r158): the team's three splash logos, each
shrunk to the height it is drawn at on a 1920x1080 page and ringed with a dark ink border,
so it reads on the painted art instead of on black.

The 2019 files in ui/preload/ are the source and are not touched. Each one is scaled with
Lanczos to its draw height (a 2000 px logo drawn 420 px tall shimmers under Linear filtering
without mipmaps), then its alpha is grown by BORDER px and filled with INK under the logo,
with a softer HALO beyond that so the edge does not cut hard into the painting.

    tools/make_intro_logos.py            # writes desktop/assets/ui/story/intro/logo_*.png

The heights must match the INTRO_LOGOS table in Vue_Scenematic_Intro, which places them.
"""
import os

from PIL import Image, ImageChops, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "desktop", "assets", "ui")
SOURCE = os.path.join(ROOT, "preload")
TARGET = os.path.join(ROOT, "story", "intro")

# source file -> (target file, height in pixels of a 1080-high page)
LOGOS = {
    "logo_team.png":   ("logo_team.png",   420),
    "logo_libGdx.png": ("logo_libGdx.png", 172),
    "logo_jam.png":    ("logo_jam.png",    280),
}

INK = (30, 20, 36)   # the near-black the painters outline with
BORDER = 5           # px of solid ink around the logo
HALO = 6             # px of soft ink beyond it
HALO_ALPHA = 0.45


def grow(alpha, radius):
    # MaxFilter takes an odd size; repeat small steps for a round-ish grow.
    for _ in range(radius):
        alpha = alpha.filter(ImageFilter.MaxFilter(3))
    return alpha


def bake(source, target, height):
    logo = Image.open(source).convert("RGBA")
    logo = logo.crop(logo.getchannel("A").getbbox())
    width = round(logo.width * height / logo.height)
    logo = logo.resize((width, height), Image.LANCZOS)

    pad = BORDER + HALO + 2
    canvas = Image.new("RGBA", (width + 2 * pad, height + 2 * pad), (0, 0, 0, 0))
    alpha = Image.new("L", canvas.size, 0)
    alpha.paste(logo.getchannel("A"), (pad, pad))

    solid = grow(alpha, BORDER).filter(ImageFilter.GaussianBlur(0.8))
    halo = grow(solid, HALO).filter(ImageFilter.GaussianBlur(HALO / 2))
    halo = halo.point(lambda a: int(a * HALO_ALPHA))

    under = ImageChops.lighter(solid, halo)
    ink = Image.new("RGBA", canvas.size, INK + (0,))
    ink.putalpha(under)

    canvas.alpha_composite(ink)
    canvas.alpha_composite(logo, (pad, pad))
    canvas.save(target, optimize=True)
    print(f"{target}: {canvas.size[0]}x{canvas.size[1]}")


if __name__ == "__main__":
    for name, (out, height) in LOGOS.items():
        bake(os.path.join(SOURCE, name), os.path.join(TARGET, out), height)

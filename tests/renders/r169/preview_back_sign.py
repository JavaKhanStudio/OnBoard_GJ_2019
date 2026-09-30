#!/usr/bin/env python3
"""r169 preview for Simon's choice: the RETOUR sign with its lettering filled off the plank
(same method as tools/make_blank_panel.py) and BACK lettered in the title face, ink colour taken
from the painted letters. The arrow is kept. Not used by the game.
    python3 tests/renders/r169/preview_back_sign.py <out.png>
"""
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

SRC = "desktop/assets/ui/icon/pause/boutonRetour.png"
BOX = (300, 150, 1215, 375)       # the word RETOUR, clear of the arrow (x < 280)
INK = 95                          # letters ~87 luminance, plank 103-132

art = Image.open(SRC).convert("RGBA")
px = np.array(art).astype(float)
lum = px[..., :3] @ [0.299, 0.587, 0.114]
x0, y0, x1, y1 = BOX
mask = np.zeros(lum.shape, bool)
mask[y0:y1, x0:x1] = (lum[y0:y1, x0:x1] < INK) & (px[y0:y1, x0:x1, 3] > 200)
ink = px[mask][:, :3].mean(axis=0)
mask = np.array(Image.fromarray((mask * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(7))) > 0
out = px.copy()
for y in range(y0 - 7, y1 + 7):
    holes = np.where(mask[y])[0]
    if len(holes):
        wood = np.where(~mask[y])[0]
        for c in range(3):
            out[y, holes, c] = np.interp(holes, wood, px[y, wood, c])
blank = Image.fromarray(out.clip(0, 255).astype(np.uint8))

def letter(word, size):
    img = blank.copy()
    d = ImageDraw.Draw(img)
    f = ImageFont.truetype("desktop/assets/ui/fonts/OptimusPrinceps.ttf", size)
    l, t, r, b = d.textbbox((0, 0), word, font=f)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    d.text((cx - (r + l) / 2, cy - (b + t) / 2), word, font=f, fill=tuple(int(v) for v in ink) + (255,))
    return img

tiles = [Image.open(SRC).convert("RGBA"), letter("Retour", 230), letter("Back", 230)]
w, h = tiles[0].size
sheet = Image.new("RGBA", (w, h * 3 + 40), (90, 90, 90, 255))
for i, t in enumerate(tiles):
    sheet.alpha_composite(t, (0, i * (h + 20)))
sheet.save(sys.argv[1])

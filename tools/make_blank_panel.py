#!/usr/bin/env python3
"""
Build the options screen's sign boards: the pause panel with "~PAUSE~" taken off its plank.

The team painted one board, and painted PAUSE into it. The options screen wants the same
board with its own titles, which the game writes on the plank in the menu font. The lettering
is a flat dark brown on wood whose grain runs sideways, so each row of it is filled by
interpolating between the wood either side of the letters on that row. The leaves and the vine
that cross the plank are green and are left alone.

    tools/make_blank_panel.py desktop/assets/ui/icon/pause/pauseMenu.png \
                              desktop/assets/ui/icon/pause/panneauVide.png
"""
import sys

import numpy as np
from PIL import Image, ImageFilter

# Where the lettering sits, in pauseMenu.png's own pixels (1095x1009).
TEXT_BOX = (320, 85, 790, 200)   # x0, y0, x1, y1
INK_LUMINANCE = 68               # the letters are ~42, the plank 75-100
EDGE = 7                         # grow the mask over the letters' anti-aliased edge


def blank(source, target):
    art = Image.open(source).convert("RGBA")
    pixels = np.array(art).astype(float)
    x0, y0, x1, y1 = TEXT_BOX

    luminance = pixels[..., :3] @ [0.299, 0.587, 0.114]
    box = (slice(y0, y1), slice(x0, x1))
    ink = (luminance[box] < INK_LUMINANCE) \
        & (pixels[box][..., 0] >= pixels[box][..., 1]) \
        & (pixels[box][..., 3] > 200)

    mask = np.zeros(luminance.shape, bool)
    mask[box] = ink
    mask = np.array(Image.fromarray((mask * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(EDGE))) > 0
    mask &= ~(pixels[..., 1] > pixels[..., 0] + 10)   # never paint over a leaf

    out = pixels.copy()
    for y in range(y0 - EDGE, y1 + EDGE):
        holes = np.where(mask[y])[0]
        if len(holes) == 0:
            continue
        wood = np.where(~mask[y])[0]
        for channel in range(3):
            out[y, holes, channel] = np.interp(holes, wood, pixels[y, wood, channel])

    Image.fromarray(out.clip(0, 255).astype(np.uint8)).save(target, format="PNG", optimize=True)
    print(f"{target}: {int(mask.sum())} pixels of lettering filled")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    blank(sys.argv[1], sys.argv[2])

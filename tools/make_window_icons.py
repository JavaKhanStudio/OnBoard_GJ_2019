#!/usr/bin/env python3
"""
Build the running window's icons from a PNG: one square PNG per size.

The .ico and .icns only reach the file manager - Explorer, Finder, shortcuts. The window a
running game opens takes its icon from whatever the program hands GLFW, and without that
Windows shows its generic application icon in the title bar and taskbar. GLFW picks the
closest size for each use, so a handful of small ones beats one large one scaled on the fly.

    tools/make_window_icons.py <source.png> <out dir>
"""
import os
import sys

from PIL import Image

from make_icns import square

SIZES = [16, 32, 48, 128]


def build(source, target_dir):
    art = Image.open(source).convert("RGBA")
    os.makedirs(target_dir, exist_ok=True)
    stem = os.path.splitext(os.path.basename(source))[0]

    written = []
    for size in SIZES:
        path = os.path.join(target_dir, f"{stem}_{size}.png")
        square(art, size).save(path, format="PNG", optimize=True)
        written.append(path)
    return written


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__.strip())
    for path in build(sys.argv[1], sys.argv[2]):
        print(f"wrote {path}")

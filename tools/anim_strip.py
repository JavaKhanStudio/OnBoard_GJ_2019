#!/usr/bin/env python3
"""Lay out Ross's 'move' frames from one age's atlas two ways, to judge the walk (r111).

Row 1, AS DRAWN: what SpriteModel.draw puts on screen - the trimmed region stretched to its
own width and height from the same corner, packing offset ignored.
Row 2, AS AUTHORED: each frame put back on its orig canvas at its packing offset, as the
artist exported it.

A red line marks the same x in every cell, a blue line the same ground. Ghost (right) stacks
all frames of each row at 1/8 opacity: a clean walk-in-place is one figure with moving legs.

    python3 tools/anim_strip.py desktop/assets/game/anim/ado.atlas out.png
"""
import sys
from PIL import Image, ImageDraw


def read_atlas(path):
    """The regions of a single-page libGDX atlas: name, xy, size, orig, offset, index."""
    lines = [l.rstrip("\n") for l in open(path)]
    lines = [l for l in lines if l.strip()]
    page, regions, cur = lines[0], [], None
    for l in lines[1:]:
        if not l.startswith(" ") and ":" not in l:
            cur = {"name": l.strip()}
            regions.append(cur)
        elif cur is not None and ":" in l:
            k, v = [s.strip() for s in l.split(":", 1)]
            cur[k] = [int(x) if x.lstrip("-").isdigit() else x for x in (p.strip() for p in v.split(","))]
    return page, regions


def main(atlas, out, name="move"):
    page, regions = read_atlas(atlas)
    sheet = Image.open(atlas.rsplit("/", 1)[0] + "/" + page).convert("RGBA")
    frames = sorted((r for r in regions if r["name"] == name), key=lambda r: r["index"][0])
    ow, oh = frames[0]["orig"]
    pad = 8
    cells = len(frames) + 1
    img = Image.new("RGBA", (cells * (ow + pad) + pad, 2 * (oh + pad) + pad), (235, 235, 235, 255))
    d = ImageDraw.Draw(img)
    for row in range(2):
        ghost = Image.new("RGBA", (ow, oh), (0, 0, 0, 0))
        for i, r in enumerate(frames):
            x, y = r["xy"]
            w, h = r["size"]
            crop = sheet.crop((x, y, x + w, y + h))
            # As drawn: the frame's own size, bottom-left pinned (unflipped here, so both rows
            # face the same way). As authored: back on the orig canvas at its offset.
            at = (0, oh - h) if row == 0 else (r["offset"][0], oh - r["offset"][1] - h)
            cell = Image.new("RGBA", (ow, oh), (255, 255, 255, 255))
            cell.alpha_composite(crop, at)
            ghost.alpha_composite(_tint(crop, at, ow, oh))
            px, py = pad + i * (ow + pad), pad + row * (oh + pad)
            img.paste(cell, (px, py))
            d.text((px + 4, py + 4), f"{'drawn' if row == 0 else 'authored'} #{r['index'][0]}", fill=(0, 0, 0, 255))
        px, py = pad + len(frames) * (ow + pad), pad + row * (oh + pad)
        bg = Image.new("RGBA", (ow, oh), (255, 255, 255, 255))
        img.paste(Image.alpha_composite(bg, ghost), (px, py))
        d.text((px + 4, py + 4), "ghost of all frames", fill=(0, 0, 0, 255))
    cx = ow // 2
    for i in range(cells):
        px = pad + i * (ow + pad)
        d.line([(px + cx, pad), (px + cx, img.height - pad)], fill=(220, 0, 0, 255), width=1)
    for row in range(2):
        gy = pad + row * (oh + pad) + oh - min(f["offset"][1] for f in frames)
        d.line([(pad, gy), (img.width - pad, gy)], fill=(0, 0, 220, 255), width=1)
    img.save(out)


def _tint(crop, at, ow, oh):
    layer = Image.new("RGBA", (ow, oh), (0, 0, 0, 0))
    a = crop.split()[3].point(lambda v: v // 8)
    solid = Image.new("RGBA", crop.size, (0, 0, 0, 255))
    solid.putalpha(a)
    layer.alpha_composite(solid, at)
    return layer


if __name__ == "__main__":
    main(*sys.argv[1:])

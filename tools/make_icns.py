#!/usr/bin/env python3
"""
Build a macOS .icns from a PNG, without macOS tooling.

The .icns container is simple: the magic "icns", a big-endian total length, then a run of
chunks, each a 4-byte type code, a big-endian length covering the header, and the payload.
Modern type codes take a PNG payload directly, so no Apple-specific encoding is involved.

    tools/make_icns.py <source.png> <out.icns>
"""
import io
import struct
import sys

from PIL import Image

# type code -> pixel size. The retina codes are the same pixels at twice the density,
# which is why 32/64 and 256/512 appear more than once.
SIZES = [
    (b"icp4", 16), (b"icp5", 32), (b"icp6", 64),
    (b"ic07", 128), (b"ic08", 256), (b"ic09", 512), (b"ic10", 1024),
    (b"ic11", 32), (b"ic12", 64), (b"ic13", 256), (b"ic14", 512),
]


def square(image, size):
    """Fit the artwork inside a transparent square, keeping its proportions."""
    fitted = image.copy()
    fitted.thumbnail((size, size), Image.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.paste(fitted, ((size - fitted.width) // 2, (size - fitted.height) // 2))
    return canvas


def build(source, target):
    art = Image.open(source).convert("RGBA")

    chunks = []
    for code, size in SIZES:
        buffer = io.BytesIO()
        square(art, size).save(buffer, format="PNG")
        payload = buffer.getvalue()
        chunks.append(code + struct.pack(">I", len(payload) + 8) + payload)

    body = b"".join(chunks)
    with open(target, "wb") as handle:
        handle.write(b"icns" + struct.pack(">I", len(body) + 8) + body)

    return len(chunks), len(body) + 8


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__.strip())
    count, total = build(sys.argv[1], sys.argv[2])
    print(f"wrote {sys.argv[2]}: {count} sizes, {total/1024:.0f} KB")

#!/usr/bin/env python3
"""
Show, from Linux, the icon each packaged executable presents itself with - and fail if it is
not the On Board logo.

A file manager here cannot tell: Nautilus shows onboard.exe and the .app with generic icons,
because only Explorer reads a PE's icon resources and only Finder reads a bundle's .icns. So
this reads them itself:

  Windows  onboard.exe's RT_GROUP_ICON / RT_ICON resources (construo re-encodes
           desktop/assets/ui/icon/logo_onboard.ico as PNG icons, 16 to 256 px)
  macOS    Contents/Info.plist's CFBundleIconFile, the .icns it names (logo_onboard.icns)
  Linux    nothing to read: an ELF binary carries no icon

Each largest icon is compared with logo_onboard.png (both cropped to their opaque pixels,
64 px on grey, mean RGB difference), and all of them are drawn side by side on grey.

    tools/package_icons.py [dir] [out.png]   defaults: build/release  build/release/package_icons.png

[dir] holds the unpacked OnBoard-*/ folders (r278: build/release/, not dist/).
"""
import io
import os
import plistlib
import struct
import sys

from make_icns import square
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOGO = os.path.join(ROOT, "desktop/assets/ui/icon/logo_onboard.png")
RT_ICON, RT_GROUP_ICON = 3, 14
# Mean per-channel difference (0-255) under which an icon counts as the logo. The exe's
# icon differs from logo_onboard.png by about 1, the .icns by 0; the logo mirrored or
# turned 15 degrees by over 30, a greyscale copy by 20.
MAX_DIFF = 8


def pe_icons(path):
    """Every RT_ICON image in a PE file, largest first, as PIL images."""
    data = open(path, "rb").read()
    pe = struct.unpack_from("<I", data, 0x3C)[0]
    assert data[pe:pe + 4] == b"PE\0\0", f"{path}: not a PE file"
    sections = struct.unpack_from("<H", data, pe + 6)[0]
    opt_size = struct.unpack_from("<H", data, pe + 20)[0]
    opt = pe + 24
    magic = struct.unpack_from("<H", data, opt)[0]
    dirs = opt + (112 if magic == 0x20B else 96)
    rsrc_rva = struct.unpack_from("<I", data, dirs + 2 * 8)[0]
    if not rsrc_rva:
        return []
    table = opt + opt_size
    for i in range(sections):
        s = table + 40 * i
        vsize, va, rawsize, raw = struct.unpack_from("<IIII", data, s + 8)
        if va <= rsrc_rva < va + max(vsize, rawsize):
            base = raw + (rsrc_rva - va)
            delta = va - raw
            break
    else:
        raise SystemExit(f"{path}: no section holds the resources")

    def entries(offset):
        named, ids = struct.unpack_from("<HH", data, base + offset + 12)
        for n in range(named + ids):
            name, target = struct.unpack_from("<II", data, base + offset + 16 + 8 * n)
            yield name, target

    icons = []
    for type_id, type_dir in entries(0):
        if type_id != RT_ICON:
            continue
        for _, name_dir in entries(type_dir & 0x7FFFFFFF):
            for _, leaf in entries(name_dir & 0x7FFFFFFF):
                rva, size = struct.unpack_from("<II", data, base + leaf)
                blob = data[rva - delta:rva - delta + size]
                if blob.startswith(b"\x89PNG"):
                    icons.append(Image.open(io.BytesIO(blob)).convert("RGBA"))
                else:
                    # a BMP icon: wrap it in a one-entry .ico so PIL can read it
                    w, h = struct.unpack_from("<ii", blob, 4)
                    bits = struct.unpack_from("<H", blob, 14)[0]
                    head = struct.pack("<HHHBBBBHHII", 0, 1, 1, w % 256, (h // 2) % 256,
                                       0, 0, 1, bits, size, 22)
                    icons.append(Image.open(io.BytesIO(head + blob)).convert("RGBA"))
    return sorted(icons, key=lambda im: -im.width)


def app_icon(app):
    """The largest image of the .icns a bundle's Info.plist names."""
    plist = plistlib.load(open(os.path.join(app, "Contents/Info.plist"), "rb"))
    name = plist.get("CFBundleIconFile")
    if not name:
        return None, "Info.plist names no CFBundleIconFile"
    path = os.path.join(app, "Contents/Resources", name)
    if not os.path.exists(path):
        return None, f"Info.plist names {name}, which is not in Contents/Resources"
    data = open(path, "rb").read()
    pngs, at = [], 8
    while at < len(data):
        length = struct.unpack_from(">I", data, at + 4)[0]
        if data[at + 8:at + 12] == b"\x89PNG":
            pngs.append(Image.open(io.BytesIO(data[at + 8:at + length])).convert("RGBA"))
        at += length
    if not pngs:
        return None, f"{name} holds no PNG image"
    return max(pngs, key=lambda im: im.width), name


def diff(icon, logo):
    """Each cropped to its opaque pixels and drawn at 64 px on grey: padding (the .icns
    keeps Apple's margin round the artwork), a 251-wide .ico or rounding no longer count."""
    def norm(im):
        im = im.crop(im.getchannel("A").point(lambda a: 255 if a > 32 else 0).getbbox())
        flat = Image.new("RGBA", (64, 64), (136, 136, 136, 255))
        flat.alpha_composite(im.resize((64, 64), Image.LANCZOS))
        return flat.convert("RGB").tobytes()
    a, b = norm(icon), norm(logo)
    return sum(abs(x - y) for x, y in zip(a, b)) / len(a)


def main():
    dist = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "build", "release")
    out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(dist, "package_icons.png")
    logo = Image.open(LOGO).convert("RGBA")
    shown, failed = [], False

    for exe in sorted(p for p in (os.path.join(dist, d, "onboard.exe") for d in os.listdir(dist))
                      if os.path.exists(p)):
        icons = pe_icons(exe)
        if not icons:
            print(f"FAIL {exe}: no icon resources - Explorer shows the generic one")
            failed = True
            continue
        d = diff(icons[0], logo)
        ok = d <= MAX_DIFF
        failed |= not ok
        sizes = " ".join(str(i.width) for i in icons)
        print(f"{'ok  ' if ok else 'FAIL'} {exe}: icons {sizes} px, largest differs from the logo by {d:.1f}")
        shown.append(icons[0])

    for app in sorted(os.path.join(dist, d, a) for d in os.listdir(dist)
                      if os.path.isdir(os.path.join(dist, d))
                      for a in os.listdir(os.path.join(dist, d)) if a.endswith(".app")):
        icon, what = app_icon(app)
        if icon is None:
            print(f"FAIL {app}: {what}")
            failed = True
            continue
        d = diff(icon, logo)
        ok = d <= MAX_DIFF
        failed |= not ok
        print(f"{'ok  ' if ok else 'FAIL'} {app}: {what}, {icon.width} px, differs from the logo by {d:.1f}")
        shown.append(icon)

    if not shown:
        raise SystemExit(f"no onboard.exe or .app under {dist}: build a package first")
    tile = 256
    sheet = Image.new("RGBA", (tile * len(shown), tile), (136, 136, 136, 255))
    for i, icon in enumerate(shown):
        sheet.alpha_composite(icon.resize((tile, tile), Image.LANCZOS), (tile * i, 0))
    sheet.convert("RGB").save(out)
    print(f"icons drawn in {out}")
    print("linux: onboard is an ELF binary, which carries no icon")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()

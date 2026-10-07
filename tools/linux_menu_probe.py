#!/usr/bin/env python3
"""
Show what a Linux desktop finds for On Board's application menu entry (r276), without a desktop.

The packaged game writes $XDG_DATA_HOME/applications/com.pixmen.onboard.desktop and its hicolor
icons when it starts (desktop/src/jks/launcher/LinuxMenuEntry.java). Run it with XDG_DATA_HOME
pointing at a scratch dir, never at Simon's own, then point this at the same dir:

    XDG_DATA_HOME=<dir> tools/linux_menu_probe.py <out.png>

It asks Gio for the entry the way GNOME's app grid does (listed, shown, its name, its
StartupWMClass), asks GTK's icon theme for the icon at the sizes the dock, Alt-Tab and the app
grid use, and draws each resolved file at that size on GNOME's dark grey, the name below.
Fails unless the entry is listed and every size resolves to one of ours.

A real GNOME Shell is not started: gnome-shell --headless registers with GDM and starts gvfs and
evolution's services on whatever session bus it gets, which is too close to Simon's session.
"""
import os
import sys
import warnings

import gi

gi.require_version("Gtk", "3.0")
from gi.repository import Gio, Gtk  # noqa: E402
from PIL import Image, ImageDraw, ImageFont  # noqa: E402

ID = "com.pixmen.onboard"
# dock 32-64, Alt-Tab 96, app grid 96, and the app grid at 2x
SIZES = [16, 32, 48, 64, 96, 192]


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "linux_menu.png"
    data = os.environ.get("XDG_DATA_HOME")
    if not data or data.rstrip("/") == os.path.expanduser("~/.local/share"):
        sys.exit("set XDG_DATA_HOME to the scratch dir the packaged game wrote into")
    warnings.simplefilter("ignore")
    app = Gio.DesktopAppInfo.new(ID + ".desktop")
    if app is None:
        sys.exit(f"FAIL no {ID}.desktop under {data}/applications")
    listed = any(a.get_id() == ID + ".desktop" for a in Gio.AppInfo.get_all())
    print(f"entry: {app.get_filename()}")
    print(f"  name {app.get_name()!r}, listed {listed}, shown {app.should_show()}, "
          f"StartupWMClass {app.get_startup_wm_class()!r}, Exec {app.get_commandline()}")
    theme = Gtk.IconTheme()
    failed = not (listed and app.should_show())

    tiles = []
    for size in SIZES:
        info = theme.lookup_by_gicon(app.get_icon(), size, Gtk.IconLookupFlags.FORCE_SIZE)
        path = info.get_filename() if info else None
        ours = bool(path) and path.startswith(data) and ID in path
        failed |= not ours
        print(f"  {size:3} px  {'ok  ' if ours else 'FAIL'} {path}")
        tiles.append((size, path))

    pad, label = 24, 28
    width = sum(s for s, _ in tiles) + pad * (len(tiles) + 1)
    height = max(SIZES) + pad * 2 + label
    sheet = Image.new("RGB", (width, height), (36, 36, 36))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("DejaVuSans.ttf", 14)
    except OSError:
        font = ImageFont.load_default()
    x = pad
    for size, path in tiles:
        y = pad + max(SIZES) - size
        if path:
            # what a desktop does with the file it found: scaled to the slot
            icon = Image.open(path).convert("RGBA").resize((size, size), Image.LANCZOS)
            sheet.paste(icon, (x, y), icon)
        draw.text((x, pad + max(SIZES) + 6), f"{size}", fill=(200, 200, 200), font=font)
        x += size + pad
    draw.text((width - 160, pad + max(SIZES) + 6), app.get_name(), fill=(255, 255, 255), font=font)
    sheet.save(out)
    print(f"drawn in {out}")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()

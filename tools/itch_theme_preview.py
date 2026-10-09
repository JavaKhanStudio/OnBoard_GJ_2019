#!/usr/bin/env python3
"""Mock an itch.io page dressed with tools/itch_theme.sh's images, to a PNG.

  tools/itch_theme_preview.py <out.png> <banner> <embed> [bg colour]

<banner> and <embed> are file names in docs/itch (theme_banner_ages.png, theme_embed.jpg...).
The layout is itch's default theme roughly: 960-px column over the background image, banner on
top, the 960x540 embed with its Run game button, then the start of the description. A mockup for
choosing the pictures, not a measure of itch's layout. Needs google-chrome.
"""
import os, subprocess, sys, tempfile

d = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "docs/itch"))
out, banner, embed = sys.argv[1:4]
bg = sys.argv[4] if len(sys.argv) > 4 else "#806875"
doc = f"""<!doctype html><meta charset=utf-8><style>
body{{margin:0;background:{bg} url(file://{d}/theme_background.jpg) no-repeat center top;
 font:16px/1.55 Lato,'Helvetica Neue',sans-serif;color:#e9e4dc}}
.col{{width:960px;margin:40px auto 0;background:#2a2530;box-shadow:0 0 12px #0008}}
.banner img{{display:block;width:960px}}
.embed{{position:relative;width:960px;height:540px;background:url(file://{d}/{embed}) center/cover}}
.run{{position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);background:#fa5c5c;
 color:#fff;font-weight:bold;padding:12px 26px;border-radius:3px;font-size:18px}}
.txt{{padding:18px 28px 30px}} b{{color:#fff}}
</style><div class=col><div class=banner><img src="file://{d}/{banner}"></div>
<div class=embed><div class=run>Run game</div></div>
<div class=txt><p><b>The 9:30 is leaving. Will Ross be on board?</b></p>
<p>A ticket, an autumn platform, a man with a suitcase. Ross steps aboard, and the train runs
through his life: one carriage for each age, each one a memory he has to walk through to reach
the door at the far end.</p></div></div>"""
with tempfile.NamedTemporaryFile("w", suffix=".html", delete=False, encoding="utf-8") as f:
    f.write(doc)
subprocess.run(["google-chrome", "--headless=new", "--hide-scrollbars", "--mute-audio",
                "--allow-file-access-from-files", f"--screenshot={os.path.abspath(out)}",
                "--window-size=1920,1300", "file://" + f.name], check=True, capture_output=True)
os.unlink(f.name)
print(out)

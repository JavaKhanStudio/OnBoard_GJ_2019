#!/usr/bin/env python3
"""Render docs/itch/page.md's tagline and Description prose as an itch-like page, to a PNG.

  tools/itch_page_preview.py <out.png> [fr|en|both]

Only headings, **bold**, paragraphs and the tagline: the tables and install notes are left out,
since this is for reading the copy, not checking the layout itch gives it. Needs google-chrome.
"""
import html, os, re, subprocess, sys, tempfile

page = open(os.path.join(os.path.dirname(__file__), "..", "docs/itch/page.md"), encoding="utf-8").read()
out = sys.argv[1]
which = sys.argv[2] if len(sys.argv) > 2 else "both"

tagline = re.search(r"## Short description or tagline\n\n(.+?)\n", page).group(1)
desc = page[page.index("### Français"):page.index("### Commandes / Controls")]
parts = {"fr": desc[:desc.index("### English")], "en": desc[desc.index("### English"):]}
if which != "both":
    parts = {which: parts[which]}

def inline(t):
    t = html.escape(t)
    return re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", t)

cols = []
for lang, text in parts.items():
    blocks = [b.strip() for b in text.split("\n\n") if b.strip()]
    body = []
    for b in blocks:
        if b.startswith("### "):
            body.append(f"<h3>{inline(b[4:])}</h3>")
        else:
            body.append(f"<p>{inline(' '.join(b.splitlines()))}</p>")
    cols.append(f"<div class=col>{''.join(body)}</div>")

doc = f"""<!doctype html><meta charset=utf-8><style>
body{{margin:0;background:#1d1a1f;color:#e9e4dc;font:16px/1.55 Lato,'Helvetica Neue',sans-serif}}
.card{{margin:24px auto;padding:20px 28px;background:#2a2530;max-width:{'1240' if len(cols)>1 else '640'}px}}
.tag{{color:#c9b98f;font-style:italic;margin:0 0 6px}}
h1{{margin:0 0 4px;font-size:30px}} h3{{color:#c9b98f;margin:0 0 8px}}
.cols{{display:flex;gap:40px}} .col{{flex:1}} b{{color:#fff}}
</style><div class=card><h1>On Board</h1><p class=tag>{html.escape(tagline)}</p>
<div class=cols>{''.join(cols)}</div></div>"""

with tempfile.NamedTemporaryFile("w", suffix=".html", delete=False, encoding="utf-8") as f:
    f.write(doc)
subprocess.run(["google-chrome", "--headless=new", "--hide-scrollbars", "--mute-audio",
                f"--screenshot={os.path.abspath(out)}", "--window-size=1320,860",
                "file://" + f.name], check=True, capture_output=True)
os.unlink(f.name)
print(out)

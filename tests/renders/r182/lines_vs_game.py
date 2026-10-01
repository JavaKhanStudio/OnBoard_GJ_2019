#!/usr/bin/env python3
"""The Lines web page's bubble against the game's own (r182).

The game side is BubbleLinesRenderTest: its sheets (verify/build/frames/bubble-lines-
<lang>-<n>.png) and, per line, the rows its label broke the line into ("r182 <lang> <key>
| cloud <px> | row / row", in the test's XML). Run it first:

    ./gradlew :verify:test -PwithGl --tests '*BubbleLinesRenderTest*'

The page side is atelier's /lines?project=onboard, served in-process from the atelier
checkout beside this one (as its tools/render_page.py does) and opened in headless Chrome:
a probe script reads the rows each bubble's text was laid out in, then lays the bubbles of
one carriage out on a grid in the game's order for a screenshot at 309/220 device pixels
a CSS pixel, so a page cloud is the game's 309 px.

    ../atelier/.venv/bin/python tests/renders/r182/lines_vs_game.py [--out DIR]

Writes DIR/side-<lang>-<n>.png (game | page, pair by pair, the rows under each; a line
the two break differently is labelled in red with both breaks) and DIR/wraps.txt, and
prints how many lines break differently.
"""
import argparse
import json
import pathlib
import re
import shutil
import subprocess
import sys
import threading
import html as htmllib

from PIL import Image, ImageDraw, ImageFont

HERE = pathlib.Path(__file__).resolve()
ONBOARD = HERE.parents[3]
ATELIER = ONBOARD.parent / "atelier"
sys.path.insert(0, str(ATELIER))
from atelier import db, server  # noqa: E402

XML = ONBOARD / "verify/build/test-results/test/TEST-jks.verify.BubbleLinesRenderTest.xml"
FRAMES = ONBOARD / "verify/build/frames"
SHOWN, GAME_SIDE = 220, 309          # web/lines.html SHOWN; the game's cloud at 1280 x 720
DSF = GAME_SIDE / SHOWN

PROBE = r"""<script>
(() => {
const P = new URLSearchParams(location.search);
const LANG = P.get("r182lang"), KEYS = (P.get("r182keys") || "").split(",").filter(Boolean);
function rows(text){
  const tops = [];
  const w = document.createTreeWalker(text, NodeFilter.SHOW_TEXT);
  const r = document.createRange();
  for (let n; (n = w.nextNode());){
    for (let i = 0; i < n.data.length; i++){
      r.setStart(n, i); r.setEnd(n, i + 1);
      const b = r.getClientRects()[0];
      if (!b) continue;
      let row = tops.find(t => Math.abs(t.y - b.top) < 4);
      if (!row) tops.push(row = {y: b.top, s: ""});
      row.s += n.data[i];
    }
  }
  return tops.sort((a, b) => a.y - b.y).map(t => t.s);
}
function widths(text){
  const tops = [];
  const w = document.createTreeWalker(text, NodeFilter.SHOW_TEXT);
  const r = document.createRange();
  for (let n; (n = w.nextNode());){
    for (let i = 0; i < n.data.length; i++){
      r.setStart(n, i); r.setEnd(n, i + 1);
      const b = r.getClientRects()[0];
      if (!b || !n.data[i].trim()) continue;
      let row = tops.find(t => Math.abs(t.y - b.top) < 4);
      if (!row) tops.push(row = {y: b.top, l: b.left, r: b.right});
      row.l = Math.min(row.l, b.left); row.r = Math.max(row.r, b.right);
    }
  }
  return tops.sort((a, b) => a.y - b.y).map(t => +(t.r - t.l).toFixed(2));
}
function bub(key, lang){
  const row = document.querySelector('.lnrow[data-key="' + key + '"]');
  if (!row) return null;
  return [...row.querySelectorAll(".lnlang")].find(b =>
    b.querySelector(".lnlangname").textContent === lang)?.querySelector(".lnbub") || null;
}
setTimeout(() => {
  const out = {fonts: [...document.fonts].map(f => [f.family, f.status]), lines: {}};
  document.querySelectorAll(".lnrow.isbubble").forEach(row => {
    row.querySelectorAll(".lnlang").forEach(b => {
      const lang = b.querySelector(".lnlangname").textContent, t = b.querySelector(".lntext");
      const w = b.querySelector(".lnbub"), body = b.querySelector(".lnbody");
      out.lines[lang + " " + row.dataset.key] = {rows: rows(t), widths: widths(t),
        bodyW: body.clientWidth, scale: w.dataset.scale,
        over: w.classList.contains("over"), textH: t.scrollHeight, bodyH: body.clientHeight,
        font: getComputedStyle(t).fontSize, lh: getComputedStyle(t).lineHeight};
    });
  });
  if (LANG){
    const room = document.querySelector(".lnbub").getBoundingClientRect();
    const grid = document.createElement("div");
    grid.style.cssText = "position:absolute;left:0;top:0;display:grid;background:#5a5a5a;"
      + "grid-template-columns:repeat(4," + room.width + "px);grid-auto-rows:" + room.height + "px";
    KEYS.forEach(k => {
      const b = bub(k, LANG), cell = document.createElement("div");
      cell.style.cssText = "display:flex;align-items:center;justify-content:center";
      if (b) cell.append(b.cloneNode(true));
      grid.append(cell);
    });
    document.body.replaceChildren(grid);
    document.body.style.cssText = "margin:0;background:#5a5a5a";
    document.body.dataset.room = room.width + "x" + room.height;
  }
  const pre = document.createElement("pre");
  pre.id = "r182";
  pre.style.display = "none";
  pre.textContent = JSON.stringify(out);
  document.body.append(pre);
}, 4000);
})();
</script></body>"""


def game_lines():
    """{(lang, key): (cloud px, [rows])} and, per (lang, carriage), the keys in sheet order."""
    lines, order = {}, {}
    for m in re.finditer(r"^r182 (\w\w) (wa(\d)\.\S+) \| cloud (\d+) \| (.*)$",
                         htmllib.unescape(XML.read_text()), re.M):
        lang, key, n, cloud, rows = m.groups()
        lines[lang, key] = (int(cloud), [r.rstrip() for r in rows.split(" / ")])
        order.setdefault((lang, int(n)), []).append(key)
    return lines, order


def chrome(exe, url, *args):
    return subprocess.run([exe, "--headless=new", "--disable-gpu", "--no-sandbox",
                           "--hide-scrollbars", "--virtual-time-budget=9000", *args, url],
                          capture_output=True, text=True, timeout=180,
                          stdin=subprocess.DEVNULL).stdout


def page_json(dom):
    m = re.search(r'<pre id="r182"[^>]*>(.*?)</pre>', dom, re.S)
    if not m:
        sys.exit("the probe wrote nothing: did /lines load?")
    return json.loads(htmllib.unescape(m.group(1)))


def label_font(size):
    """A face with French accents for the labels: whatever fontconfig gives for Noto Sans."""
    face = subprocess.run(["fc-match", "-f", "%{file}", "Noto Sans"], capture_output=True,
                          text=True).stdout
    return ImageFont.truetype(face, size) if face else ImageFont.load_default()


def side_sheet(lang, n, keys, game, page, shot, room, out):
    sheet = Image.open(FRAMES / ("bubble-lines-%s-%d.png" % (lang, n))).convert("RGB")
    grows = (len(keys) + 3) // 4
    gw, gh = sheet.width // 4, sheet.height // grows - 18
    cell = max(gw, gh)
    pw, ph = round(room[0] * DSF), round(room[1] * DSF)
    font, small = label_font(15), label_font(12)
    pairs, label = 2, 92
    W = pairs * 2 * cell + (pairs - 1) * 24
    H = ((len(keys) + pairs - 1) // pairs) * (cell + label)
    img = Image.new("RGB", (W, H), (40, 40, 40))
    d = ImageDraw.Draw(img)
    differ = 0
    for i, key in enumerate(keys):
        x = (i % pairs) * (2 * cell + 24)
        y = (i // pairs) * (cell + label)
        cloud, grows_rows = game[lang, key]
        c = cloud + 24                        # BubbleLinesRenderTest's crop: the cloud, 12 a side
        g = sheet.crop(((i % 4) * gw, (i // 4) * (gh + 18),
                        (i % 4) * gw + min(c, gw), (i // 4) * (gh + 18) + min(c, gh)))
        img.paste(g, (x + (cell - g.width) // 2, y + (cell - g.height) // 2))
        px, py = (i % 4) * pw, (i // 4) * ph
        p = shot.crop((px + (pw - cell) // 2, py + (ph - cell) // 2,
                       px + (pw + cell) // 2, py + (ph + cell) // 2))
        img.paste(p, (x + cell, y))
        pl = page.get(lang + " " + key)
        prow = [r.rstrip() for r in pl["rows"]] if pl else []
        same = prow == grows_rows
        differ += not same
        colour = (120, 220, 120) if same else (255, 110, 110)
        d.text((x + 4, y + cell + 4), "%s %s   game %d rows, cloud x%.2f | page %d rows, x%s%s"
               % (lang, key, len(grows_rows), cloud / GAME_SIDE, len(prow),
                  pl["scale"] if pl else "?", ", OVER" if pl and pl["over"] else ""),
               fill=colour, font=font)
        if not same:
            d.text((x + 4, y + cell + 26), "game: " + " / ".join(grows_rows), fill=colour, font=small)
            d.text((x + 4, y + cell + 44), "page: " + " / ".join(prow), fill=colour, font=small)
        d.text((x + 4, y + 4), "game", fill=(255, 255, 255), font=small)
        d.text((x + cell + 4, y + 4), "page", fill=(255, 255, 255), font=small)
    path = out / ("side-%s-%d.png" % (lang, n))
    img.save(path)
    return path, differ


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=str(HERE.parent / "out"))
    ap.add_argument("--css", default="", help="CSS put after the page's own: a fix tried "
                    "on the page before it is raised on atelier")
    a = ap.parse_args()
    exe = next((x for x in ("google-chrome", "chromium", "chromium-browser") if shutil.which(x)), None)
    if not exe:
        sys.exit("no Chrome on this machine")
    out = pathlib.Path(a.out)
    out.mkdir(parents=True, exist_ok=True)
    game, order = game_lines()
    if not game:
        sys.exit("no r182 rows in %s: run BubbleLinesRenderTest first" % XML)

    server.Handler.projects = db.load_all_configs()
    server.read_model.projects = server.Handler.projects
    server.Handler.default = "onboard"
    server.Handler.require_auth = False
    server.Handler._config_stamp = db.config_stamp()
    srv = server.Server(("127.0.0.1", 0), server.Handler)
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    probe = pathlib.Path(server.WEB) / "_probe_r182.html"
    probe.write_text((pathlib.Path(server.WEB) / "lines.html").read_text()
                     .replace("</body>", PROBE, 1)
                     .replace("</head>", "<style>%s</style></head>" % a.css, 1))
    base = "http://127.0.0.1:%d/%s?project=onboard" % (srv.server_address[1], probe.name)
    report, total = [], 0
    try:
        # The rows as the page shows them at 1280, reduced motion so no letter is mid-wave.
        page = page_json(chrome(exe, base, "--window-size=1280,2000",
                                "--force-prefers-reduced-motion", "--dump-dom"))
        report.append("fonts: %s" % page["fonts"])
        for (lang, n), keys in sorted(order.items(), key=lambda x: (x[0][0] != "fr", x[0][1])):
            url = base + "&r182lang=%s&r182keys=%s" % (lang, ",".join(keys))
            dom = chrome(exe, url, "--window-size=1280,2000", "--dump-dom")
            room = tuple(float(v) for v in re.search(r'data-room="([\d.]+)x([\d.]+)"', dom).groups())
            shot_path = out / ("page-%s-%d.png" % (lang, n))
            rows = (len(keys) + 3) // 4
            chrome(exe, url, "--force-device-scale-factor=%f" % DSF,
                   "--window-size=%d,%d" % (4 * room[0] + 1, rows * room[1] + 1),
                   "--screenshot=%s" % shot_path)
            path, differ = side_sheet(lang, n, keys, game, page["lines"],
                                      Image.open(shot_path).convert("RGB"), room, out)
            total += differ
            print(path, "-", differ, "of", len(keys), "lines break differently")
            for key in keys:
                pl = page["lines"].get(lang + " " + key)
                g = game[lang, key][1]
                p = [r.rstrip() for r in pl["rows"]] if pl else None
                report.append("%s %s %s\n  game x%.2f: %s\n  page x%s: %s\n  page widths at game px: %s in %s" % (
                    "SAME" if p == g else "DIFF", lang, key, game[lang, key][0] / GAME_SIDE,
                    " / ".join(g), pl and pl["scale"], p and " / ".join(p),
                    pl and [round(w * DSF, 1) for w in pl["widths"]], pl and round(pl["bodyW"] * DSF, 1)))
    finally:
        probe.unlink(missing_ok=True)
        srv.shutdown()
    (out / "wraps.txt").write_text("\n".join(report) + "\n")
    print(total, "of", len(game), "lines break differently; rows in", out / "wraps.txt")


if __name__ == "__main__":
    main()

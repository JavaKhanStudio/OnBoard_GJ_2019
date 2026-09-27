#!/usr/bin/env bash
# tab_shot.sh <url> <out.png> — a real Chrome window, tab strip and all, shot offscreen (r165).
#
# Headless Chrome has no tab strip, so the tab's icon cannot be seen there. This runs the system
# Chrome (fresh profile: no cached favicon, muted) in gamescope's headless backend, the way
# tools/itch_shots.sh shoots the game, and saves what the top of the window shows. Nothing reaches
# Simon's screen or speakers (D6).
set -euo pipefail
url="$1"; out="$(readlink -f "$2")"
W=${W:-1000}; H=${H:-400}
work=$(mktemp -d); trap 'rm -rf "$work"' EXIT
cat >"$work/inside.sh" <<EOF
#!/usr/bin/env bash
google-chrome --user-data-dir="$work/profile" --no-first-run --no-default-browser-check --mute-audio \
	--ozone-platform=x11 --window-position=0,0 --window-size=$W,$H --new-window "$url" >"$work/chrome.log" 2>&1 &
chrome=\$!
sleep 8
gamescopectl screenshot "$out" >/dev/null 2>&1
sleep 1
kill \$chrome 2>/dev/null; wait \$chrome 2>/dev/null
EOF
chmod +x "$work/inside.sh"
timeout 40 gamescope --backend headless -W $W -H $H -w $W -h $H -- "$work/inside.sh" >/dev/null 2>&1 || true
[ -f "$out" ] || { echo "tab_shot: no screenshot" >&2; cat "$work/chrome.log" >&2; exit 1; }
echo "tab_shot: $out"

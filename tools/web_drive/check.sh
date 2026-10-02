#!/usr/bin/env bash
# check.sh — prove the drive's browser version (dist/onboard-web.zip, r222) plays from where a player
# would start it: unzip it, run the Linux launcher, then serve.ps1 under pwsh (the Windows one's
# server, when pwsh is installed), and play each with the browser gate; then open index.html from
# disk and shoot the notice it should show instead of the game.
#
#   tools/web_drive/check.sh [out-dir]     default tests/renders/r222; PWSH= to point at pwsh
#
# Nothing opens on the screen: ONBOARD_NO_BROWSER keeps the launchers from opening a tab, and the
# gate's Chrome is headless. No Windows PowerShell 5.1 and no Mac here: those two are not run.
set -euo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/../.." && pwd)
OUT=$(readlink -f "${1:-$ROOT/tests/renders/r222}")
PWSH=${PWSH:-$(command -v pwsh || echo "$HOME/.local/opt/pwsh/pwsh")}
UNZIP=$(mktemp -d)
SERVER=
stop() { [[ -n $SERVER ]] && kill "$SERVER" 2>/dev/null; SERVER=; }
trap 'stop; rm -rf "$UNZIP"' EXIT
unzip -q "$ROOT/dist/onboard-web.zip" -d "$UNZIP"
TOP="$UNZIP/On Board (navigateur - browser)"
echo "check: $(cat "$TOP/VERSION.txt")"
ls "$TOP" | sed 's/^/  /'
ss -ltn | grep -q '127.0.0.1:47219 ' && { echo "check: 47219 is already taken" >&2; exit 1; }

play() {  # play <name>: gate the server just started on 47219
	for _ in $(seq 50); do curl -sf -o /dev/null http://127.0.0.1:47219/VERSION.txt && break; sleep 0.2; done
	mkdir -p "$OUT/gate-$1"
	node "$ROOT/tools/browser-gate/gate.mjs" http://127.0.0.1:47219/ "$OUT/gate-$1" | tail -1 | sed "s/^/  $1: /"
	stop; sleep 1
}

ONBOARD_NO_BROWSER=1 setsid "$TOP/Jouer - Play (Linux).sh" >/dev/null & SERVER=$!
play perl
if [[ -x $PWSH ]]; then
	ONBOARD_NO_BROWSER=1 "$PWSH" -NoProfile -ExecutionPolicy Bypass -File "$TOP/fichiers - files/serve.ps1" >/dev/null & SERVER=$!
	play ps1
else
	echo "  ps1: NOT RUN, no pwsh"
fi

url="file://$(python3 -c 'import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))' "$TOP/fichiers - files/index.html")"
node "$ROOT/tools/browser-gate/open_probe.mjs" "$url" "$OUT/web_file_notice.png" >/dev/null 2>&1 || true
echo "  file://: $OUT/web_file_notice.png (should show the notice, not the game)"

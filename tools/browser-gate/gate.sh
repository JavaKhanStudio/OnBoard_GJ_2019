#!/usr/bin/env bash
# gate.sh — the browser gate (r137): build the html module, serve it, and play it in headless Chrome
# past the logos, through the intro to the start screen, then open a carriage. Headless always, and
# Chrome's audio muted (--mute-audio): nothing reaches Simon's screen or speakers (D6).
#
#   tools/browser-gate/gate.sh             an optimized compile (~1 min), then the gate
#   tools/browser-gate/gate.sh --draft     a ~20 s unoptimized compile
#   tools/browser-gate/gate.sh --no-build  gate whatever html/build/war already holds
#
# BROWSER=firefox plays it in Firefox instead (FIREFOX=, default /usr/bin/firefox), in a window
# inside cage and with its volume at zero: GWT gives Firefox its own lines, and this checks them.
# Needs node and a Chrome (CHROME=, default /usr/bin/google-chrome); puppeteer-core is installed
# here on first run and never downloads a browser. Screenshots and gate.json: html/build/gate/.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
here="$root/tools/browser-gate"
war="$root/html/build/war"
out="$root/html/build/gate${BROWSER:+-$BROWSER}"
build=(:html:war)
for arg in "$@"; do
	case "$arg" in
		--draft) build+=(-Pdraft) ;;
		--no-build) build=() ;;
		*) echo "unknown option: $arg" >&2; exit 2 ;;
	esac
done

[ ${#build[@]} -gt 0 ] && "$root/gradlew" -p "$root" "${build[@]}" -q
[ -f "$war/html/html.nocache.js" ] || { echo "no browser build in $war: run without --no-build" >&2; exit 1; }
[ -d "$here/node_modules/puppeteer-core" ] || (cd "$here" && npm ci --no-audit --no-fund --silent)

rm -rf "$out"; mkdir -p "$out"
# What itch's HTML5 upload limits are about: 1000 files, 200 MB a file, 500 MB in all
files=$(find "$war" -type f | wc -l)
bytes=$(du -sb "$war" | cut -f1)
biggest=$(find "$war" -type f -printf '%s %P\n' | sort -n | tail -1)
echo "gate: war $files files, $((bytes / 1048576)) MB, largest $((${biggest%% *} / 1048576)) MB (${biggest#* })"

port=$(python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1", 0)); print(s.getsockname()[1])')
"$(dirname "$(readlink -f "$(command -v java)")")/jwebserver" -b 127.0.0.1 -p "$port" -d "$war" >"$out/server.log" 2>&1 &
server=$!
trap 'kill $server 2>/dev/null' EXIT
for _ in $(seq 50); do curl -sf -o /dev/null "http://127.0.0.1:$port/index.html" && break; sleep 0.1; done

# Firefox has no WebGL headless here: it runs in a window inside cage (tools/offscreen.sh), which
# nobody sees. Without cage it stays headless, and fails for want of WebGL rather than open a window.
runner=()
[ "${BROWSER:-}" = firefox ] && runner=("$root/tools/offscreen.sh" env MOZ_ENABLE_WAYLAND=1)
WAR_FILES=$files WAR_BYTES=$bytes "${runner[@]}" node "$here/gate.mjs" "http://127.0.0.1:$port/index.html" "$out"

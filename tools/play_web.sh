#!/usr/bin/env bash
# play_web.sh — the board's "Play it in the browser" (r156): build the browser build (r137) from the
# checkout as it stands, then serve html/build/war on http://127.0.0.1:8795/ until stopped. The board
# ([viewers.web] in ../atelier/projects/onboard.toml) runs this when nothing answers on the port and
# opens the page in Simon's browser once it does.
#
#   tools/play_web.sh            an optimized compile (~1 min the first time; Gradle skips it after)
#   tools/play_web.sh --draft    a ~20 s unoptimized compile
#
# The port is FIXED on purpose: the game keeps its options in localStorage, which belongs to the
# origin, so a new port each time would forget them. A server already up keeps serving what it was
# given — stop it (the board's Stop, or kill it) to play newer work.
#
# It opens nothing itself: whoever starts it opens the page. The gate that plays it unattended, muted
# and headless, is tools/browser-gate/gate.sh.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
port="${PORT:-8795}"
build=(:html:war)
for arg in "$@"; do
	case "$arg" in
		--draft) build+=(-Pdraft) ;;
		*) echo "unknown option: $arg" >&2; exit 2 ;;
	esac
done

"$root/gradlew" -p "$root" "${build[@]}" -q
war="$root/html/build/war"
[ -f "$war/html/html.nocache.js" ] || { echo "no browser build in $war after :html:war" >&2; exit 1; }
echo "play_web: serving $war on http://127.0.0.1:$port/"
exec "$(dirname "$(readlink -f "$(command -v java)")")/jwebserver" -b 127.0.0.1 -p "$port" -d "$war"

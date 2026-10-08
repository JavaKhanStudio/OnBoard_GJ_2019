#!/usr/bin/env bash
# itch_shots.sh — take the itch.io screenshots from the release zip, run offscreen at 1920x1080.
#
#   tools/itch_shots.sh                   # dist/onboard-linuxX64.zip -> build/itch_shots/
#   tools/itch_shots.sh <zip> <out dir>
#   ONLY="wa1 wa3" tools/itch_shots.sh    # just those scenes
#   LANG_CODE=en tools/itch_shots.sh      # in English: <scene>_en_<seconds>.png (default fr)
#
# Each scene is one launch of the packaged game with its own -Donboard.* start flags
# (README.md "Running it"), added to the launcher's app/onboard.json vmArgs since the
# runtime has no java binary to pass them to. Each is grabbed at a few moments on the wall
# clock: <out>/<scene>_<seconds>.png. There is no xdotool on this machine, so a shot that
# needs a click cannot be taken: every moment has to be reachable from a start flag.
#
# WHY GAMESCOPE, NOT CAGE: cage's headless output is 1280x720 and cannot be changed, so
# smoke_package.sh's nested Xwayland is 1280x720 too and x11grab refuses a 1920x1080 area.
# gamescope's headless backend takes the size (-W/-H) and gamescopectl saves the frame.
# The game's own "config" is written before each run: 1920x1080 windowed, in LANG_CODE. Nothing
# reaches the screen, and ALSOFT_DRIVERS=null keeps the sound off the speakers.
set -uo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)
ZIP=$(readlink -f "${1:-$ROOT/dist/onboard-linuxX64.zip}")
OUT=$(mkdir -p "${2:-$ROOT/build/itch_shots}" && readlink -f "${2:-$ROOT/build/itch_shots}")
W=1920 H=1080
LANG_CODE=${LANG_CODE:-fr}
SUFFIX=$([[ $LANG_CODE == fr ]] || printf %s "_$LANG_CODE")

# scene name | JVM flags | moments (s after launch)
SCENES=(
	"logo|-Donboard.start=logo|3 6"
	"intro|-Donboard.start=intro|6 12 18 24"
	"start|-Donboard.start=start_screen|6 10"
	"wa1|-Donboard.start=game -Donboard.level=1|8 14"
	"wa2|-Donboard.start=game -Donboard.level=2|8 14"
	"wa3|-Donboard.start=game -Donboard.level=3|8 14"
	"wa4|-Donboard.start=game -Donboard.level=4|8 14"
	"outro_leave|-Donboard.start=outro -Donboard.karma=4|6 12 18 24"
	"outro_stay|-Donboard.start=outro -Donboard.karma=0|6 12 18 24 30"
	"credits|-Donboard.start=credits|6"
)

for tool in gamescope gamescopectl python3 unzip; do
	command -v $tool >/dev/null || { echo "itch_shots: needs $tool" >&2; exit 2; }
done

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
unzip -q "$ZIP" -d "$WORK"
GAME="$WORK/On Board"
cp "$GAME/app/onboard.json" "$WORK/onboard.json"

for scene in "${SCENES[@]}"; do
	IFS='|' read -r name flags moments <<<"$scene"
	read -ra ts <<<"$moments"
	read -ra flagv <<<"$flags"
	[[ -n ${ONLY:-} && " $ONLY " != *" $name "* ]] && continue
	rm -f "$OUT/${name}${SUFFIX}"_[0-9]*.png
	# Written whole, not edited from desktop/config: a sed on its layout broke when that changed.
	printf '{"width": %d, "height": %d, "isFullScreen": false, "language": "%s"}\n' $W $H "$LANG_CODE" >"$GAME/config"
	python3 - "$GAME/app/onboard.json" "$WORK/onboard.json" "${flagv[@]}" <<'PY'
import json, sys
dst, src, flags = sys.argv[1], sys.argv[2], sys.argv[3:]
cfg = json.load(open(src))
cfg["vmArgs"] = cfg["vmArgs"] + flags
json.dump(cfg, open(dst, "w"))
PY
	cat >"$WORK/inside.sh" <<EOF
#!/usr/bin/env bash
cd "$GAME"
# XDG_DATA_HOME: the packaged game adds itself to the application menu (r276); not to Simon's.
XDG_DATA_HOME="$WORK/xdg" ./onboard >"$OUT/$name$SUFFIX.log" 2>&1 &
game=\$!
prev=0
for t in ${ts[*]}; do
	sleep \$((t - prev)); prev=\$t
	gamescopectl screenshot "$OUT/${name}${SUFFIX}_\$t.png" >/dev/null 2>&1
done
sleep 2
kill \$game 2>/dev/null; wait \$game 2>/dev/null
EOF
	chmod +x "$WORK/inside.sh"
	# Headless needs no display, and connects to WAYLAND_DISPLAY when it is set: an agent's
	# session sets it to a dead socket on purpose (D6), and gamescope then exits at once.
	env -u WAYLAND_DISPLAY -u DISPLAY ALSOFT_DRIVERS=null timeout $((ts[-1] + 40)) \
		gamescope --backend headless -W $W -H $H -w $W -h $H -- "$WORK/inside.sh" >/dev/null 2>&1
	echo "  $name$SUFFIX: $(find "$OUT" -maxdepth 1 -name "${name}${SUFFIX}_[0-9]*.png" | wc -l) of ${#ts[@]} shots"
done

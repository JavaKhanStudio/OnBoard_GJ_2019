#!/usr/bin/env bash
# smoke_package.sh — unzip the Linux release zip, run it offscreen as a player would, and
# save what it showed at a few moments.
#
#   tools/smoke_package.sh                          # dist/onboard-linuxX64.zip, 5 12 20 s
#   tools/smoke_package.sh <zip> <out dir> 4 10 30  # other zip, dir, moments
#
# Out: <out dir>/shot_<seconds>.png, and game.log. Exit 1 if the game was gone before the
# last shot, or a shot came out black.
#
# WHY A SECOND XWAYLAND: cage's own XWayland is rootless, and a grab of its root window
# (import, ffmpeg x11grab) comes out black: the GL window is never drawn into it. Started
# by hand, Xwayland is not rootless - it is one full-size window inside cage whose root the
# game draws onto - so x11grab of :9 sees the game. Nothing reaches the screen or speakers.
#
# Only the Linux zip runs on this machine. The Windows and macOS zips are the same jar
# built by the same configuration; running this proves nothing about their launchers.
set -uo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)
ZIP=$(readlink -f "${1:-$ROOT/dist/onboard-linuxX64.zip}")
OUT=$(mkdir -p "${2:-$ROOT/build/smoke}" && readlink -f "${2:-$ROOT/build/smoke}")
shift 2 2>/dev/null || shift $#
MOMENTS=("${@:-5 12 20}")
read -ra MOMENTS <<<"${MOMENTS[*]}"

for tool in cage Xwayland ffmpeg magick unzip; do
	command -v $tool >/dev/null || { echo "smoke_package: needs $tool" >&2; exit 2; }
done

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
unzip -q "$ZIP" -d "$WORK"
[[ -x "$WORK/On Board/onboard" ]] || { echo "smoke_package: no executable 'On Board/onboard' in $ZIP" >&2; exit 1; }
rm -f "$OUT"/shot_*.png "$OUT/game.log"

cat >"$WORK/inside.sh" <<EOF
#!/usr/bin/env bash
Xwayland :9 -geometry 1600x900 -noreset >"$OUT/xwayland.log" 2>&1 &
xw=\$!
sleep 2
cd "$WORK/On Board"
DISPLAY=:9 WAYLAND_DISPLAY= ./onboard >"$OUT/game.log" 2>&1 &
game=\$!
prev=0
for t in ${MOMENTS[*]}; do
	sleep \$((t - prev)); prev=\$t
	ffmpeg -loglevel error -y -f x11grab -i :9 -frames:v 1 "$OUT/shot_\$t.png"
done
kill -0 \$game 2>/dev/null && echo "smoke_package: alive at \${prev}s" >>"$OUT/game.log"
kill \$game 2>/dev/null; wait \$game 2>/dev/null
kill \$xw
EOF
chmod +x "$WORK/inside.sh"
last=${MOMENTS[-1]}
WLR_BACKENDS=headless ALSOFT_DRIVERS=null timeout $((last + 40)) cage -- "$WORK/inside.sh" >/dev/null 2>&1

status=0
grep -q "alive at" "$OUT/game.log" || { echo "smoke_package: the game was gone before ${last}s:" >&2; tail -20 "$OUT/game.log" >&2; status=1; }
for t in "${MOMENTS[@]}"; do
	shot="$OUT/shot_$t.png"
	if [[ ! -f $shot ]]; then echo "  ${t}s  no shot" >&2; status=1; continue; fi
	mean=$(magick "$shot" -format '%[fx:mean]' info:)
	echo "  ${t}s  $shot  mean=$mean"
	# Black at the last moment means nothing was drawn; an early black shot is the logo fading in.
	[[ $t == "$last" ]] && awk "BEGIN{exit !($mean < 0.02)}" && { echo "smoke_package: last shot is black" >&2; status=1; }
done
exit $status

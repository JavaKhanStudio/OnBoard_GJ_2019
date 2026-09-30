#!/usr/bin/env bash
# lab_shots.sh — open every surface on the board's Labs, offscreen, and grab what it shows.
#
#   tools/lab_shots.sh                     # every surface -> build/lab_shots/<Name>_<s>.png
#   ONLY="Credits SoundLab" tools/lab_shots.sh
#   AT="12 20" tools/lab_shots.sh          # the moments to grab, in seconds after the game starts
#   W=1280 H=720 tools/lab_shots.sh        # another window size (default 1600x900)
#
# WHY: a surface that no longer opens is a Labs button that lies, and a Gradle task that
# ended 0 is not a screen that rendered. The labs upkeep (r188) needs to SEE each one.
#
# The surfaces come from `atelier lab list --json`, so a surface added to the board is shot
# without editing this. Each viewer's flags are the ones the board's projects/onboard.toml
# gives it ([viewers.level], [viewers.start], [viewers.item_lab]); a viewer not listed here
# is skipped with a line saying so.
#
# Headless as itch_shots.sh is, and for its reason (cage's output is fixed at 1280x720):
# gamescope --backend headless at the game's 1600x900, gamescopectl saves the frame.
# ONBOARD_INSIDE_CAGE=1 keeps gradle/offscreen.gradle from starting a cage of its own, and
# the sound goes to build/audio/runGame.wav as it does for any agent run. Moments are timed
# from Gradle's "> Task :desktop:runGame" line, so a slow build does not eat them.
set -uo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)
OUT=$(mkdir -p "$ROOT/build/lab_shots" && readlink -f "$ROOT/build/lab_shots")
W=${W:-1600} H=${H:-900}
read -ra MOMENTS <<<"${AT:-10 18}"

for tool in gamescope gamescopectl atelier python3; do
	command -v $tool >/dev/null || { echo "lab_shots: needs $tool" >&2; exit 2; }
done

# name|flags, one line per surface
surfaces=$(cd "$ROOT" && atelier lab list --json | python3 -c '
import json, sys
for lab in json.load(sys.stdin):
    v, a = lab["viewer"], lab["args"]
    if v == "level":
        flags = "-Donboard.start=game -Donboard.level=%s -Donboard.lab=true" % a["level"]
    elif v == "item_lab":
        flags = "-Donboard.start=item_lab -Donboard.level=%s" % a["level"]
    elif v == "start":
        flags = "-Donboard.start=%s" % a["point"]
    else:
        print("lab_shots: %s uses viewer %s, not shot here" % (lab["name"], v), file=sys.stderr)
        continue
    print("%s|%s" % (lab["name"], flags))
')

while IFS='|' read -r name flags; do
	[[ -z $name ]] && continue
	[[ -n ${ONLY:-} && " $ONLY " != *" $name "* ]] && continue
	rm -f "$OUT/${name}"_*.png
	log="$OUT/$name.log"
	# The game opens its window at the config's size, not gamescope's: give it one of W x H,
	# a copy, so nothing a lab saves reaches desktop/config either.
	config=$(mktemp)
	python3 - "$ROOT/desktop/config" "$config" "$W" "$H" <<'PY'
import json, os, sys
src, dst, w, h = sys.argv[1:]
cfg = json.load(open(src)) if os.path.exists(src) else {}
cfg.update(width=int(w), height=int(h), isFullScreen=False)
json.dump(cfg, open(dst, "w"), indent=2)
PY
	inside=$(mktemp)
	cat >"$inside" <<EOF
#!/usr/bin/env bash
cd "$ROOT"
ONBOARD_INSIDE_CAGE=1 ONBOARD_OFFSCREEN=1 ./gradlew --console=plain :desktop:runGame $flags -Donboard.config=$config >"$log" 2>&1 &
game=\$!
for _ in \$(seq 300); do grep -q '> Task :desktop:runGame' "$log" 2>/dev/null && break; kill -0 \$game 2>/dev/null || break; sleep 1; done
prev=0
for t in ${MOMENTS[*]}; do
	sleep \$((t - prev)); prev=\$t
	gamescopectl screenshot "$OUT/${name}_\$t.png" >/dev/null 2>&1
done
sleep 2
kill \$game 2>/dev/null; wait \$game 2>/dev/null
EOF
	chmod +x "$inside"
	timeout $((MOMENTS[-1] + 360)) gamescope --backend headless -W $W -H $H -w $W -h $H -- "$inside" >/dev/null 2>&1
	rm -f "$inside" "$config"
	shots=$(ls "$OUT/${name}"_*.png 2>/dev/null | wc -l)
	echo "$name: $shots of ${#MOMENTS[@]} frames ($flags)"
done <<<"$surfaces"

#!/usr/bin/env bash
# check_web_mp3.sh — the browser build's .mp3 (html/build/web-mp3, r152) against the .ogg it was
# encoded from: same number of samples once decoded, so a looping track or rails loop gains no gap.
# Run after ./gradlew :html:webMp3 (or :html:war). Fails on the first file that differs.
set -euo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)
cd "$ROOT/desktop/assets"
samples() { ffmpeg -v error -i "$1" -f s16le -ac 1 - | wc -c; }
bad=0
while IFS= read -r ogg; do
	mp3="$ROOT/html/build/web-mp3/${ogg%.ogg}.mp3"
	[[ -f $mp3 ]] || { echo "missing: $mp3" >&2; bad=1; continue; }
	diff=$(( ($(samples "$mp3") - $(samples "$ogg")) / 2 ))
	printf '%+6d samples  %s\n' "$diff" "$ogg"
	(( diff == 0 )) || bad=1
done < <(find . -name '*.ogg' | sort)
exit $bad

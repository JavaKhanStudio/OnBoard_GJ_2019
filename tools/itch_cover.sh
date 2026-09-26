#!/usr/bin/env bash
# itch_cover.sh — make the itch.io cover (630x500) from the team's own art.
#
#   tools/itch_cover.sh            # -> docs/itch/cover.png, cover_platform.png
#
# A crop of a 1920x1080 intro page to itch's 630:500, scaled down as a whole, with the game's
# stamp logo (ui/icon/logo_onboard.png) laid over a corner. Both are resized proportionally
# and never stretched (doctrine D3). Two candidates, so Simon can pick:
#   cover.png           introPage3, Ross stepping aboard in the steam
#   cover_platform.png  introPage1, the autumn platform and the rails
set -euo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)
A="$ROOT/desktop/assets"
OUT="$ROOT/docs/itch"
mkdir -p "$OUT"
LOGO="$A/ui/icon/logo_onboard.png"

# source | crop x offset in the 1920x1080 page (the crop is 1361x1080) | logo corner | out
cover() {
	magick "$A/ui/story/intro/$1" -crop 1361x1080+"$2"+0 +repage -resize 630x500 \
		\( "$LOGO" -resize x200 \) -gravity "$3" -geometry +14+10 -composite \
		-strip "$OUT/$4"
}
cover introPage3.png 250 NorthWest cover.png
cover introPage1.png 0 NorthEast cover_platform.png
magick identify "$OUT"/cover*.png

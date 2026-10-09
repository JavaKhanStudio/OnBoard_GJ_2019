#!/usr/bin/env bash
# itch_theme.sh — make the itch.io page theme images (Edit theme: banner, background, embed BG)
# from the team's own art, cropped and scaled whole, never stretched (doctrine D3).
#
#   tools/itch_theme.sh [shots dir]    # -> docs/itch/theme_*.{png,jpg}
#
#   theme_banner_ages.png      960 wide: Ross in the four carriages, child to groom, side by side.
#                              From itch_shots.sh's <wa>_14.png (the hint bubble is gone by 14 s):
#                              ONLY="wa1 wa2 wa3 wa4" tools/itch_shots.sh makes them.
#   theme_banner_train.png     960x320: introPage3's red carriage in the steam, the stamp logo left.
#   theme_background.jpg       1920 wide: introPage1, the autumn platform, fading at the bottom into
#                              the colour printed last — set it as the theme's background colour.
#   theme_embed.jpg            1280x720 (the embed's viewport): introPage3, Ross stepping aboard.
#   theme_embed_ticket.jpg     1280x720: introPage2, the 9:30 ticket in his hand.
#   theme_embed_steam_*.jpg    1280x720: the intro's painted steam (ui/cinematic/fumee.png, the
#                              frames GVars_Steam plays) half over the train (_train) or the platform
#                              (_platform), or closed with the stamp logo above the button (_logo).
# JPEG where a PNG would pass push_check's 1 MB.
set -euo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)
A="$ROOT/desktop/assets/ui/story/intro"
SHOTS=${1:-$ROOT/build/itch_shots}
OUT="$ROOT/docs/itch"
LOGO="$ROOT/desktop/assets/ui/icon/logo_onboard.png"
GUTTER='#1d1a1f'
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT

# Banner: the four ages. 480x850 from x=100,y=105 holds Ross in every carriage, head to boots,
# under the pause button and above the black band at the bottom of the shot.
for w in wa1 wa2 wa3 wa4; do
	[[ -f $SHOTS/${w}_14.png ]] || { echo "itch_theme: no $SHOTS/${w}_14.png — run ONLY=\"wa1 wa2 wa3 wa4\" tools/itch_shots.sh" >&2; exit 1; }
	magick "$SHOTS/${w}_14.png" -crop 480x850+100+105 +repage "$T/$w.png"
done
magick "$T"/wa{1,2,3,4}.png -background "$GUTTER" -splice 8x0 +append -chop 8x0 \
	-resize 960x -strip "$OUT/theme_banner_ages.png"

# Banner: the train. A 1920x640 band of introPage3 (the man in the door, head to boots, and the
# steam), the stamp logo on the left.
magick "$A/introPage3.png" -crop 1920x640+0+160 +repage -resize 960x320 \
	\( "$LOGO" -resize x290 \) -gravity West -geometry +16+0 -composite \
	-strip "$OUT/theme_banner_train.png"

# Background: introPage1 whole, then 600 px fading into its own bottom-edge colour.
BG=$(magick "$A/introPage1.png" -crop 1920x40+0+1040 +repage -scale 1x1! -format '#%[hex:p{0,0}]' info:)
magick "$A/introPage1.png" \( -size 1920x600 gradient:"rgba(0,0,0,0)-$BG" \) -gravity South \
	-compose Over -composite -strip -quality 88 "$OUT/theme_background.jpg"

# Embed BG: the viewport is 1280x720 (page.md, Embed options), the pages are 16:9: scale whole.
magick "$A/introPage3.png" -resize 1280x720 -strip -quality 90 "$OUT/theme_embed.jpg"
magick "$A/introPage2.png" -resize 1280x720 -strip -quality 90 "$OUT/theme_embed_ticket.jpg"

# The steam: fumee.atlas packs nine 1920x1080 frames, three a row. Frame 6 has risen past the
# middle, the man left of it (the Run game button lands on steam, not on a face); frame 9 covers.
fumee() { magick "$ROOT/desktop/assets/ui/cinematic/fumee.png" -crop 1920x1080+$(( ($1-1)%3*1920 ))+$(( ($1-1)/3*1080 )) +repage "$T/fumee$1.png"; }
fumee 6; fumee 9
magick "$A/introPage3.png" "$T/fumee6.png" -composite -resize 1280x720 -strip -quality 90 "$OUT/theme_embed_steam_train.jpg"
magick "$A/introPage1.png" "$T/fumee6.png" -composite -resize 1280x720 -strip -quality 90 "$OUT/theme_embed_steam_platform.jpg"
magick "$T/fumee9.png" -resize 1280x720 \( "$LOGO" -resize x300 \) -gravity Center -geometry +0-150 \
	-composite -strip -quality 90 "$OUT/theme_embed_steam_logo.jpg"

magick identify "$OUT"/theme_*
echo "background colour: $BG"

#!/usr/bin/env bash
# itch_gifs.sh — record the itch.io clips from the real game and turn them into GIFs (r283).
#
#   tools/itch_gifs.sh                         # every scene -> docs/itch/clip_<scene>.gif
#   ONLY="carriage" tools/itch_gifs.sh         # just those scenes
#   LANGS=en tools/itch_gifs.sh                # English only (default: fr en)
#   NO_RECORD=1 tools/itch_gifs.sh             # reuse verify/build/clips/itch-*.mp4
#
# French clips are clip_<scene>.gif, the others clip_<scene>_<lang>.gif.
#
# Each scene is one run of verify's ItchClipTest (-Pclip, caged offscreen like the GL tests,
# no sound out): the game on a fixed 1/30 s clock, played by script, piped to ffmpeg as
# verify/build/clips/itch-<scene>.mp4 at 1280x720. The scenes are listed in ItchClipTest.
# The GIF is that clip at GIF_WIDTH wide and GIF_FPS, on one palette made for the whole clip
# (scaled whole, never stretched: D3). 640 px, 12 fps, 128 colours and an ordered dither keep
# a 16 s clip near 5 MB: the default error-diffusion dither made the same clip 13 MB, its
# noise changing every frame where the parallax moves. GIF_WIDTH=480 GIF_FPS=10 halves it.
# The mp4 is kept beside it as docs/itch/clip_<scene>.mp4, a source for a trailer.
set -uo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)
CLIPS="$ROOT/verify/build/clips"
OUT="$ROOT/docs/itch"
SCENES=${ONLY:-carriage intro walk3 walk4}
LANGS=${LANGS:-fr en}
GIF_WIDTH=${GIF_WIDTH:-640} GIF_FPS=${GIF_FPS:-12}

command -v ffmpeg >/dev/null || { echo "itch_gifs: needs ffmpeg" >&2; exit 2; }
mkdir -p "$OUT"
for lang in $LANGS; do
for scene in $SCENES; do
	tag=$scene$([[ $lang == fr ]] || printf %s "-$lang")
	name=clip_$scene$([[ $lang == fr ]] || printf %s "_$lang")
	if [[ -z ${NO_RECORD:-} ]]; then
		"$ROOT/gradlew" -p "$ROOT" -q :verify:test -Pclip --tests '*ItchClipTest' -PclipScene="$scene" -PclipLang="$lang" \
			>"$CLIPS/itch-$tag.log" 2>&1 || { echo "itch_gifs: $tag failed, see $CLIPS/itch-$tag.log" >&2; exit 1; }
	fi
	mp4="$CLIPS/itch-$tag.mp4"
	[[ -f $mp4 ]] || { echo "itch_gifs: no $mp4" >&2; exit 1; }
	ffmpeg -y -loglevel error -i "$mp4" -filter_complex \
		"fps=$GIF_FPS,scale=$GIF_WIDTH:-1:flags=lanczos,split[a][b];[a]palettegen=stats_mode=diff:max_colors=128[p];[b][p]paletteuse=dither=bayer:bayer_scale=5:diff_mode=rectangle" \
		"$OUT/$name.gif" || exit 1
	cp "$mp4" "$OUT/$name.mp4"
	echo "  $tag: $OUT/$name.gif ($(du -h "$OUT/$name.gif" | cut -f1)), $(ffprobe -v error -show_entries format=duration -of csv=p=0 "$mp4") s"
done
done

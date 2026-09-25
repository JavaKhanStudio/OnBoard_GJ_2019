#!/usr/bin/env bash
# Candidate faces for Ross's thought bubbles (r103), into lab-assets/fonts/bubble/, where
# BubbleFontLabTest draws the real bubble once in each. All come from github.com/google/fonts
# under the SIL Open Font License (ofl/) or Apache 2.0 (apache/): free to ship in a game, sold
# or not, as long as the licence file goes with the face. Every one has the French the text
# table writes (checked with fontTools: accents, « », ’, …, œ).
# ./tools/fetch_bubble_fonts.sh [dir]
set -euo pipefail
dir="${1:-$(dirname "$0")/../lab-assets/fonts/bubble}"
mkdir -p "$dir"
for f in \
	ofl/comicneue/ComicNeue-Bold ofl/comicrelief/ComicRelief-Regular ofl/patrickhand/PatrickHand-Regular \
	ofl/patrickhandsc/PatrickHandSC-Regular ofl/gochihand/GochiHand-Regular ofl/kalam/Kalam-Regular \
	ofl/bangers/Bangers-Regular ofl/bubblegumsans/BubblegumSans-Regular ofl/delius/Delius-Regular \
	ofl/caveatbrush/CaveatBrush-Regular ofl/fuzzybubbles/FuzzyBubbles-Regular ofl/mansalva/Mansalva-Regular \
	apache/chewy/Chewy-Regular apache/schoolbell/Schoolbell-Regular apache/walterturncoat/WalterTurncoat-Regular \
	apache/permanentmarker/PermanentMarker-Regular apache/comingsoon/ComingSoon-Regular
do
	curl -sfL -o "$dir/${f##*/}.ttf" "https://github.com/google/fonts/raw/main/$f.ttf"
done
ls "$dir"

#!/usr/bin/env bash
# package_icons.sh — prove tools/package_icons.py tells the On Board logo from another icon (r274).
#
#   tests/package_icons.sh
#
# In a scratch dist/ it builds two fake .app bundles with tools/make_icns.py: one from
# logo_onboard.png, which must pass, one from the logo mirrored, which must fail, and one whose
# Info.plist names an .icns that is not there, which must fail too. No package is built.
# (A real onboard.exe was checked by hand on r274: the built one passes at 1.2, the JDK's javaw.exe,
# Duke at 48 px, fails at 83.)
set -euo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
LOGO="$ROOT/desktop/assets/ui/icon/logo_onboard.png"

app() { # <dist> <icns source png or "">
	local res="$1/OnBoard-mac/On Board.app/Contents/Resources"
	mkdir -p "$res"
	cat >"$1/OnBoard-mac/On Board.app/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<plist version="1.0"><dict><key>CFBundleIconFile</key><string>logo_onboard.icns</string></dict></plist>
PLIST
	[[ -z $2 ]] || python3 "$ROOT/tools/make_icns.py" "$2" "$res/logo_onboard.icns" >/dev/null
}

python3 -c "from PIL import Image; Image.open('$LOGO').transpose(Image.FLIP_LEFT_RIGHT).save('$WORK/mirrored.png')"
app "$WORK/good" "$LOGO"
app "$WORK/mirrored" "$WORK/mirrored.png"
app "$WORK/missing" ""

fails=0
run() { # <name> <dist> <expected exit>
	local code=0
	python3 "$ROOT/tools/package_icons.py" "$WORK/$2" "$WORK/$2.png" >"$WORK/$2.out" 2>&1 || code=$?
	if [[ $code == "$3" ]]; then echo "  ok   $1"; else echo "  FAIL $1 (exit $code)"; cat "$WORK/$2.out"; fails=1; fi
}
run "the logo passes" good 0
run "the logo mirrored fails" mirrored 1
run "an .icns that is not there fails" missing 1
((fails)) && exit 1
echo "package_icons: passed"

#!/usr/bin/env bash
# package_all.sh — build the four release zips (what goes on itch.io) from a commit, not
# from whatever the checkout happens to hold.
#
#   tools/package_all.sh            # package HEAD
#   tools/package_all.sh <commit>   # package that commit
#
# WHY A WORKTREE: this checkout is shared by several sessions, and at any moment it holds
# their unfinished edits. A release built here would ship them. The build runs in a
# detached worktree of the commit (../onboard-release by default, RELEASE_TREE= to move
# it), kept between runs so construo's four downloaded JDKs are not fetched again.
#
# ONE GRADLE INVOCATION PER TARGET: the fat jar carries one platform's natives, and
# desktop/build.gradle refuses two package tasks in one invocation.
#
# Each zip is then repacked so that it unzips into one "On Board/" folder (construo puts
# app/, runtime/ and the launcher at the root of the zip, which spills them over whatever
# folder a player extracts into), with VERSION.txt beside the launcher. Unix modes, the
# launcher's exec bit among them, are carried over.
#
# THE BROWSER BUILD (r138) is built in the same worktree: ./gradlew :html:war, and
# html/build/war zipped as it is, index.html at the ROOT of the zip (itch's HTML5 uploads
# want it there, not in a folder), with VERSION.txt beside it.
#
# THE DRIVE'S BROWSER VERSION (r222) is dist/onboard-web.zip, the same build in another shape: a
# browser will not run it from a disk (file://), so it unzips into "On Board (navigateur - browser)/"
# holding a "Jouer - Play" launcher for Windows, Mac and Linux, each starting a small local web
# server (tools/web_drive/) and opening the page. The site sits in "fichiers - files/" below them,
# so index.html is not the first thing anyone clicks. onboard-html.zip is for itch, not for a drive.
#
# THE PACKAGES ARE NOT SIGNED: Simon chose that on r222 (2026-10-02) and wants it said at every
# release, which is why the run ends by saying it. docs/signing.md is what signing would take.
#
# THE ICONS (r274): tools/package_icons.py reads the icon onboard.exe and each .app carry and fails
# the run unless it is the On Board logo; dist/package_icons.png shows them side by side.
#
# Out: dist/onboard-{winX64,linuxX64,macArm64,macX64,html,web}.zip, dist/VERSION, the drive's
# "dist/LISEZMOI - README.txt" (from tools/drive_README.txt), and the unpacked
# dist/OnBoard-*/ folders, all replaced. The zip names do not change between versions;
# dist/VERSION says which commit they are.
#
# IT IS THE BOARD'S "MAKE A BUILD" COMMAND (r254): the Tools screen (atelier r1420) runs it with
# nobody watching. So everything it says, and every line Gradle prints, is also kept in
# dist/package_all.log; a good run ends with the absolute path of each zip and of that log, and
# any failure ends with one "package_all: FAILED ..." line naming the step and the log. Two runs
# at once are refused (they would share ../onboard-release). Nothing in a build opens a window or
# plays a sound, and the script makes sure of it rather than trusting a caller's flag (D6): no
# DISPLAY or WAYLAND_DISPLAY reaches Gradle, and OpenAL has only its null driver.
set -Eeuo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)
mkdir -p "$ROOT/dist"
LOG="$ROOT/dist/package_all.log"
: >"$LOG"
exec > >(tee -a "$LOG") 2> >(tee -a "$LOG" >&2)
STEP="setup"
fail() { echo "package_all: FAILED at $STEP: $*. Log: $LOG" >&2; exit 1; }
trap 'fail "line $LINENO exited $?"' ERR

unset DISPLAY WAYLAND_DISPLAY
export ALSOFT_DRIVERS=null
for tool in git zip unzip rsync ffmpeg flock; do
	command -v $tool >/dev/null || fail "needs $tool on this machine"
done
python3 -c 'import PIL' 2>/dev/null || fail "needs python3 with Pillow (tools/package_icons.py)"

COMMIT=$(git -C "$ROOT" rev-parse --verify --quiet "${1:-HEAD}^{commit}") || fail "no commit '${1:-HEAD}'"
TREE=${RELEASE_TREE:-$(dirname "$ROOT")/onboard-release}
exec 9>"$TREE.lock"
flock -n 9 || fail "another package_all is already building in $TREE"

# 2026.09.26-bb2b2a9: the commit's date, so versions sort, and its hash, so it can be found.
VERSION="$(git -C "$ROOT" log -1 --format=%cd --date=format:%Y.%m.%d "$COMMIT")-$(git -C "$ROOT" rev-parse --short "$COMMIT")"
echo "package_all: $VERSION in $TREE"

if [[ -e "$TREE/.git" ]]; then
	git -C "$TREE" checkout --quiet --detach --force "$COMMIT"
	git -C "$TREE" clean -fdq -e dist/ -e desktop/build/ -e .gradle/
else
	git -C "$ROOT" worktree add --detach "$TREE" "$COMMIT"
fi

# gradle task          construo zip name       unpacked folder
TARGETS=(
	"packageWindows         onboard-winX64.zip     OnBoard-windows"
	"packageLinux           onboard-linuxX64.zip   OnBoard-linux"
	"packageMacAppleSilicon onboard-macArm64.zip   OnBoard-macapplesilicon"
	"packageMacIntel        onboard-macX64.zip     OnBoard-macintel"
)

STAGE=$(mktemp -d)
trap 'rm -rf "$STAGE"' EXIT

# One Gradle invocation, its whole output kept in the log; on failure its last 30 lines on stderr.
gradle() {
	echo "---- ./gradlew $* ----" >>"$LOG"
	if ! (cd "$TREE" && ./gradlew --console=plain "$@") >"$STAGE/gradle.log" 2>&1; then
		cat "$STAGE/gradle.log" >>"$LOG"
		tail -30 "$STAGE/gradle.log" >&2
		fail "./gradlew $* did not build"
	fi
	cat "$STAGE/gradle.log" >>"$LOG"
}

for line in "${TARGETS[@]}"; do
	read -r task zip folder <<<"$line"
	STEP=$task
	rm -f "$TREE/dist/$zip"
	echo "  $task"
	gradle ":desktop:$task"
	grep '^checkDistNatives:' "$STAGE/gradle.log" | sed 's/^/    /' \
		|| fail "it never ran checkDistNatives"
	[[ -f "$TREE/dist/$zip" ]] || fail "it made no dist/$zip"

	rm -rf "${STAGE:?}/On Board" "${STAGE:?}/$zip"
	mkdir "$STAGE/On Board"
	unzip -q "$TREE/dist/$zip" -d "$STAGE/On Board"
	echo "On Board $VERSION" >"$STAGE/On Board/VERSION.txt"
	# -X: no extra timestamps/uid fields; -y: keep symlinks as symlinks. zip stores modes.
	(cd "$STAGE" && zip -qrXy "$zip" "On Board")
	mv "$STAGE/$zip" "$TREE/dist/$zip"
done

# r274: the .exe and both .app must show the On Board logo, which no file manager here can show.
STEP="icons"
echo "  icons"
python3 "$TREE/tools/package_icons.py" "$TREE/dist" "$TREE/dist/package_icons.png" | sed 's/^/    /' \
	|| fail "an executable does not present the logo (tools/package_icons.py)"

STEP="html:war"
echo "  html:war"
rm -f "$TREE/dist/onboard-html.zip"
gradle :html:war
echo "On Board $VERSION" >"$TREE/html/build/war/VERSION.txt"
(cd "$TREE/html/build/war" && zip -qrX "$TREE/dist/onboard-html.zip" .)

STEP="web (drive)"
echo "  web (drive)"
WEB="$STAGE/On Board (navigateur - browser)"
rm -rf "$WEB" "$TREE/dist/onboard-web.zip"
mkdir -p "$WEB"
cp -a "$TREE/html/build/war" "$WEB/fichiers - files"
cp "$TREE/tools/web_drive/serve.pl" "$TREE/tools/web_drive/serve.ps1" "$WEB/fichiers - files/"
cp "$TREE/tools/web_drive/Jouer - Play"* "$WEB/"
chmod +x "$WEB/fichiers - files/serve.pl" "$WEB/"*.command "$WEB/"*.sh
echo "On Board $VERSION" >"$WEB/VERSION.txt"
(cd "$STAGE" && zip -qrX "$TREE/dist/onboard-web.zip" "On Board (navigateur - browser)")

STEP="copy to $ROOT/dist"
for line in "${TARGETS[@]}"; do
	read -r task zip folder <<<"$line"
	cp "$TREE/dist/$zip" "$ROOT/dist/$zip"
	rsync -a --delete "$TREE/dist/$folder/" "$ROOT/dist/$folder/"
done
cp "$TREE/dist/onboard-html.zip" "$TREE/dist/onboard-web.zip" "$TREE/dist/package_icons.png" "$ROOT/dist/"
echo "$VERSION" >"$ROOT/dist/VERSION"
# What goes on a drive with the zips (r222): which zip for which computer, past the two warnings.
sed "s/@VERSION@/$VERSION/" "$TREE/tools/drive_README.txt" >"$ROOT/dist/LISEZMOI - README.txt"

# The last lines are what the Tools screen shows: each file, absolute, then the log.
echo "package_all: built $VERSION"
for line in "${TARGETS[@]}" "- onboard-html.zip" "- onboard-web.zip"; do
	read -r task zip folder <<<"$line"
	printf '  %-6s %s\n' "$(du -h "$ROOT/dist/$zip" | cut -f1)" "$ROOT/dist/$zip"
done
echo "package_all: NOT SIGNED. Windows warns \"Windows protected your PC\", macOS says \"damaged\"."
echo "  Simon chose unsigned (r222); say so in the release note. What signing takes: docs/signing.md"
echo "  log: $LOG"

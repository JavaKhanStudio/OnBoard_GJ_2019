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
# Out: dist/onboard-{winX64,linuxX64,macArm64,macX64,html,web}.zip, dist/VERSION, the drive's
# "dist/LISEZMOI - README.txt" (from tools/drive_README.txt), and the unpacked
# dist/OnBoard-*/ folders, all replaced. The zip names do not change between versions;
# dist/VERSION says which commit they are.
set -euo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)
COMMIT=$(git -C "$ROOT" rev-parse --verify "${1:-HEAD}^{commit}")
TREE=${RELEASE_TREE:-$(dirname "$ROOT")/onboard-release}

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

for line in "${TARGETS[@]}"; do
	read -r task zip folder <<<"$line"
	rm -f "$TREE/dist/$zip"
	echo "  $task"
	if ! (cd "$TREE" && ./gradlew --console=plain ":desktop:$task") >"$STAGE/gradle.log" 2>&1; then
		tail -30 "$STAGE/gradle.log" >&2
		echo "package_all: $task FAILED" >&2
		exit 1
	fi
	grep '^checkDistNatives:' "$STAGE/gradle.log" | sed 's/^/    /' \
		|| { echo "package_all: $task never ran checkDistNatives" >&2; exit 1; }
	[[ -f "$TREE/dist/$zip" ]] || { echo "package_all: $task made no dist/$zip" >&2; exit 1; }

	rm -rf "${STAGE:?}/On Board" "${STAGE:?}/$zip"
	mkdir "$STAGE/On Board"
	unzip -q "$TREE/dist/$zip" -d "$STAGE/On Board"
	echo "On Board $VERSION" >"$STAGE/On Board/VERSION.txt"
	# -X: no extra timestamps/uid fields; -y: keep symlinks as symlinks. zip stores modes.
	(cd "$STAGE" && zip -qrXy "$zip" "On Board")
	mv "$STAGE/$zip" "$TREE/dist/$zip"
done

echo "  html:war"
rm -f "$TREE/dist/onboard-html.zip"
if ! (cd "$TREE" && ./gradlew --console=plain :html:war) >"$STAGE/gradle.log" 2>&1; then
	tail -30 "$STAGE/gradle.log" >&2
	echo "package_all: html:war FAILED" >&2
	exit 1
fi
echo "On Board $VERSION" >"$TREE/html/build/war/VERSION.txt"
(cd "$TREE/html/build/war" && zip -qrX "$TREE/dist/onboard-html.zip" .)

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

mkdir -p "$ROOT/dist"
for line in "${TARGETS[@]}"; do
	read -r task zip folder <<<"$line"
	cp "$TREE/dist/$zip" "$ROOT/dist/$zip"
	rsync -a --delete "$TREE/dist/$folder/" "$ROOT/dist/$folder/"
done
cp "$TREE/dist/onboard-html.zip" "$TREE/dist/onboard-web.zip" "$ROOT/dist/"
echo "$VERSION" >"$ROOT/dist/VERSION"
# What goes on a drive with the zips (r222): which zip for which computer, past the two warnings.
sed "s/@VERSION@/$VERSION/" "$TREE/tools/drive_README.txt" >"$ROOT/dist/LISEZMOI - README.txt"

echo "package_all: $VERSION"
for line in "${TARGETS[@]}"; do
	read -r task zip folder <<<"$line"
	printf '  %-24s %s\n' "$zip" "$(du -h "$ROOT/dist/$zip" | cut -f1)"
done
for zip in onboard-html.zip onboard-web.zip; do
	printf '  %-24s %s\n' "$zip" "$(du -h "$ROOT/dist/$zip" | cut -f1)"
done
echo "package_all: NOT SIGNED. Windows warns \"Windows protected your PC\", macOS says \"damaged\"."
echo "  Simon chose unsigned (r222); say so in the release note. What signing takes: docs/signing.md"

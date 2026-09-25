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
# Out: dist/onboard-{winX64,linuxX64,macArm64,macX64}.zip, dist/VERSION, and the unpacked
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

	rm -rf "$STAGE/On Board" "$STAGE/$zip"
	mkdir "$STAGE/On Board"
	unzip -q "$TREE/dist/$zip" -d "$STAGE/On Board"
	echo "On Board $VERSION" >"$STAGE/On Board/VERSION.txt"
	# -X: no extra timestamps/uid fields; -y: keep symlinks as symlinks. zip stores modes.
	(cd "$STAGE" && zip -qrXy "$zip" "On Board")
	mv "$STAGE/$zip" "$TREE/dist/$zip"
done

mkdir -p "$ROOT/dist"
for line in "${TARGETS[@]}"; do
	read -r task zip folder <<<"$line"
	cp "$TREE/dist/$zip" "$ROOT/dist/$zip"
	rsync -a --delete "$TREE/dist/$folder/" "$ROOT/dist/$folder/"
done
echo "$VERSION" >"$ROOT/dist/VERSION"

echo "package_all: $VERSION"
for line in "${TARGETS[@]}"; do
	read -r task zip folder <<<"$line"
	printf '  %-24s %s\n' "$zip" "$(du -h "$ROOT/dist/$zip" | cut -f1)"
done

#!/usr/bin/env bash
# itch_push.sh — send the four release zips to itch.io, one butler channel per platform.
#
#   tools/itch_push.sh --dry-run <user>/<game>   # what would go where, no network, no key
#   tools/itch_push.sh <user>/<game>             # push (needs Simon's credentials)
#
# The zips are tools/package_all.sh's: dist/onboard-*.zip, and dist/VERSION names the commit
# they were built from. It becomes each build's version on itch (--userversion-file).
#
# REFUSES to push:
#   - a zip that is missing, or whose "On Board/VERSION.txt" is not dist/VERSION (the four
#     must be one build);
#   - a build older than the game: if any commit since dist/VERSION's touched what goes in
#     a package (core/, desktop/, the Gradle files), rebuild first. Commits to docs, tools
#     or tests do not make it stale;
#   - without credentials: BUTLER_API_KEY in the environment, or ~/.config/itch/butler_creds
#     from a `butler login`. Only Simon has those.
#
# butler is fetched into tools/.butler/ (gitignored) the first time it is needed.
set -euo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)
BUTLER_DIR="$ROOT/tools/.butler"
BUTLER_URL=https://broth.itch.zone/butler/linux-amd64/LATEST/archive/default
CREDS=${BUTLER_CREDS:-$HOME/.config/itch/butler_creds}

DRY=0
if [[ ${1:-} == --dry-run ]]; then DRY=1; shift; fi
TARGET=${1:-}
if [[ ! $TARGET =~ ^[A-Za-z0-9_-]+/[A-Za-z0-9_-]+$ ]]; then
	echo "usage: tools/itch_push.sh [--dry-run] <itch user>/<game slug>   e.g. simon/on-board" >&2
	exit 2
fi

# zip                    channel    (itch reads the platform from the channel's name)
CHANNELS=(
	"onboard-winX64.zip   windows"
	"onboard-linuxX64.zip linux"
	"onboard-macArm64.zip mac-arm64"
	"onboard-macX64.zip   mac-intel"
)
# What goes into a package. A commit touching none of these leaves the zips current.
PACKAGED=(core desktop build.gradle settings.gradle gradle.properties gradle)

fail() { echo "itch_push: $*" >&2; exit 1; }

[[ -f $ROOT/dist/VERSION ]] || fail "no dist/VERSION: build with tools/package_all.sh first"
VERSION=$(<"$ROOT/dist/VERSION")
BUILT=${VERSION##*-}
git -C "$ROOT" cat-file -e "$BUILT^{commit}" 2>/dev/null || fail "dist/VERSION names $BUILT, which is not a commit here"

STALE=$(git -C "$ROOT" log --format='%h %s' "$BUILT..HEAD" -- "${PACKAGED[@]}")
[[ -z $STALE ]] || fail "the zips are $VERSION, and these commits since change what is packaged:
$STALE
Rebuild: tools/package_all.sh"

for line in "${CHANNELS[@]}"; do
	read -r zip channel <<<"$line"
	[[ -f $ROOT/dist/$zip ]] || fail "dist/$zip is missing"
	inside=$(unzip -p "$ROOT/dist/$zip" "On Board/VERSION.txt" 2>/dev/null) \
		|| fail "dist/$zip has no On Board/VERSION.txt: not built by tools/package_all.sh"
	[[ $inside == "On Board $VERSION" ]] || fail "dist/$zip is '$inside', dist/VERSION is $VERSION"
done

echo "itch_push: On Board $VERSION -> $TARGET"
for line in "${CHANNELS[@]}"; do
	read -r zip channel <<<"$line"
	printf '  %-22s %5s  ->  %s:%s\n' "$zip" "$(du -h "$ROOT/dist/$zip" | cut -f1)" "$TARGET" "$channel"
done

if (( DRY )); then
	echo "itch_push: dry run, nothing sent"
	exit 0
fi

# Fetched before the credentials check, so the `butler login` it suggests exists.
if [[ ! -x $BUTLER_DIR/butler ]]; then
	echo "itch_push: fetching butler into tools/.butler/"
	mkdir -p "$BUTLER_DIR"
	curl -fsSL -o "$BUTLER_DIR/butler.zip" "$BUTLER_URL"
	unzip -qo "$BUTLER_DIR/butler.zip" -d "$BUTLER_DIR"
	rm "$BUTLER_DIR/butler.zip"
	chmod +x "$BUTLER_DIR/butler"
fi
"$BUTLER_DIR/butler" --version

if [[ -z ${BUTLER_API_KEY:-} && ! -s $CREDS ]]; then
	cat >&2 <<EOF
itch_push: no itch.io credentials, nothing sent. Simon, one of:
  - run once:  $BUTLER_DIR/butler login     (opens the browser, saves $CREDS)
  - or make a key at https://itch.io/user/settings/api-keys and
    BUTLER_API_KEY=<key> tools/itch_push.sh $TARGET
EOF
	exit 1
fi

for line in "${CHANNELS[@]}"; do
	read -r zip channel <<<"$line"
	"$BUTLER_DIR/butler" push --userversion-file="$ROOT/dist/VERSION" \
		"$ROOT/dist/$zip" "$TARGET:$channel"
done
echo "itch_push: $VERSION pushed. Each channel appears as an upload on the game's edit page."

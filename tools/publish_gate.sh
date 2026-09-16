#!/usr/bin/env bash
# This board's publish gate: `[coordinator] gate` in the atelier checkout's
# projects/onboard.toml.
#
# The worker runs it on a detached worktree of the commit it is about to push,
# with ATELIER_CHECKOUT naming the real checkout. Exit 0 and that commit goes
# out; anything else and a Publish pass is queued with this script's last lines.
#
# It is the check every Publish pass ran by hand, the gate the #build, #flow and
# #level packs name: `./gradlew build` — compile every module and run the fast
# tests, no display needed. The GL tests (-PwithGl) boot the real game in cage
# and stay a session's job.
#
# Nothing heavy is borrowed from $ATELIER_CHECKOUT: the dependencies and the JDK
# live in ~/.gradle, shared by every checkout, and borrowing its build/ dirs
# would let this tree pass on the checkout's outputs instead of its own.
#
# Then no tracked file may have changed: a check that writes the tree is not a
# check (the game's own config file, desktop/config, is the one to watch).
#
#   ATELIER_CHECKOUT=$PWD tools/publish_gate.sh      # by hand, from a worktree
set -uo pipefail
cd "$(dirname "$(readlink -f "$0")")/.."
: "${ATELIER_CHECKOUT:?the real checkout this worktree was made from}"

STEPS=(
  "./gradlew build --console=plain"
)

echo "gate: $(git rev-parse --short HEAD), ${#STEPS[@]} steps"
LOG=$(mktemp)
trap 'rm -f "$LOG"' EXIT
for step in "${STEPS[@]}"; do
  start=$SECONDS
  if $step > "$LOG" 2>&1; then
    echo "  ok   $step  ($((SECONDS - start))s, $(grep -c ' PASSED$' "$LOG") tests passed)"
  else
    echo "  FAIL $step  ($((SECONDS - start))s)"
    # The worker quotes only the last lines: end on what failed, not Gradle's advice.
    grep -A3 -E '^\* What went wrong' "$LOG"
    grep -E 'error:|warning: \[' "$LOG" | head -10
    grep -E ' FAILED$' "$LOG" | grep -v '^> Task' | head -20
    exit 1
  fi
done

changed=$(git status --porcelain --untracked-files=no)
if [ -n "$changed" ]; then
  echo "gate: the checks changed tracked files:"
  echo "$changed"
  exit 1
fi
echo "gate: passed in ${SECONDS}s"

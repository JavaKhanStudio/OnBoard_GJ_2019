#!/usr/bin/env bash
# package_all_failure.sh — prove tools/package_all.sh fails cleanly when a Gradle task fails (r265).
#
#   tests/package_all_failure.sh
#
# It copies tools/package_all.sh into a scratch git repository whose ./gradlew prints a few lines
# and exits 1, and runs it there with RELEASE_TREE in the scratch dir. The real checkout's dist/,
# build/release/ and ../onboard-release are never touched, and nothing is built, shown or played.
#
# Passes when the run exits 1, its last stderr line is
#   package_all: FAILED at packageWindows: ./gradlew :desktop:packageWindows did not build. Log: <log>
# stderr carries the stub's last lines, and <log> holds the stub's whole output.
set -euo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

REPO="$WORK/repo"
mkdir -p "$REPO/tools"
cp "$ROOT/tools/package_all.sh" "$REPO/tools/"
cat >"$REPO/gradlew" <<'EOF'
#!/usr/bin/env bash
for i in $(seq 1 40); do echo "stub gradle line $i"; done
echo "STUB GRADLE FAILURE for $*"
exit 1
EOF
chmod +x "$REPO/gradlew"
git -C "$REPO" init -q
git -C "$REPO" add -A
git -C "$REPO" -c user.name=test -c user.email=test@invalid commit -qm stub --no-verify

set +e
RELEASE_TREE="$WORK/release" "$REPO/tools/package_all.sh" >"$WORK/out" 2>"$WORK/err"
code=$?
set -e
sleep 0.2 # tee in package_all.sh may still be flushing

LOG="$REPO/build/release/package_all.log"
fails=0
check() { if eval "$2"; then echo "  ok   $1"; else echo "  FAIL $1"; fails=1; fi; }
check "exit code 1 (was $code)" '[[ $code == 1 ]]'
check "last stderr line names the step and the log" \
	'[[ $(tail -1 "$WORK/err") == "package_all: FAILED at packageWindows: ./gradlew :desktop:packageWindows did not build. Log: $LOG" ]]'
check "stderr carries Gradle's last lines" 'grep -q "STUB GRADLE FAILURE for --console=plain :desktop:packageWindows" "$WORK/err"'
check "stderr carries only the last 30" '! grep -q "stub gradle line 10$" "$WORK/err"'
check "log holds Gradle's whole output" 'grep -q "stub gradle line 1$" "$LOG" && grep -q "STUB GRADLE FAILURE" "$LOG"'
check "log ends with the FAILED line" 'grep -q "^package_all: FAILED at packageWindows" "$LOG"'
check "no later step ran" '! grep -q "html:war" "$WORK/out"'
if ((fails)); then
	echo "--- stdout"; cat "$WORK/out"; echo "--- stderr"; cat "$WORK/err"
	exit 1
fi
echo "package_all_failure: passed"

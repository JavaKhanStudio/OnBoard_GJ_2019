#!/usr/bin/env bash
# gl_test_under_load.sh — run one GL test class N times while every core is kept busy, and
# print each run's verdict. For a test that "failed once under load" (r145): ten runs here
# either all agree or they do not, which a rerun on an idle machine cannot tell you.
#
#   tools/gl_test_under_load.sh KeyHintRenderTest            # 10 runs, 8 busy loops, as `stress -c 8`
#   RUNS=5 HOGS=40 tools/gl_test_under_load.sh KeyHintRenderTest
#   HOGS=0 tools/gl_test_under_load.sh KeyHintRenderTest     # the same, idle, for comparison
#
# There is no stress(1) on this machine, so the load is plain shell busy loops, niced 0 like
# the test. Each run's report (the test's System.out) is kept in build/under_load/<class>-<n>.txt.
# Run it in a worktree: :verify:test holds verify/build/test.lock for the whole of each run.
set -uo pipefail
ROOT=$(cd "$(dirname "$(readlink -f "$0")")/.." && pwd)
CLASS=${1:?usage: gl_test_under_load.sh <TestClass>}
RUNS=${RUNS:-10}
HOGS=${HOGS:-8}
OUT="$ROOT/build/under_load"
mkdir -p "$OUT"

hogs=()
stop_hogs() { ((${#hogs[@]})) && kill "${hogs[@]}" 2>/dev/null; wait 2>/dev/null; }
trap stop_hogs EXIT
for ((i = 0; i < HOGS; i++)); do
	sh -c 'while :; do :; done' &
	hogs+=($!)
done
echo "gl_test_under_load: $CLASS x$RUNS, $HOGS busy loops, load $(cut -d' ' -f1 /proc/loadavg)"

passed=0
for ((n = 1; n <= RUNS; n++)); do
	log="$OUT/$CLASS-$n.txt"
	(cd "$ROOT" && ./gradlew -q --console=plain :verify:test -PwithGl --tests "*.$CLASS" --rerun) >"$log" 2>&1
	rc=$?
	xml="$ROOT/verify/build/test-results/test/TEST-jks.verify.$CLASS.xml"
	[[ -f "$xml" ]] && sed -n '/<system-out><!\[CDATA\[/,/\]\]><\/system-out>/p' "$xml" >>"$log"
	if ((rc == 0)); then passed=$((passed + 1)); verdict=PASS; else verdict=FAIL; fi
	echo "  run $n: $verdict  load $(cut -d' ' -f1 /proc/loadavg)  ($log)"
done
echo "gl_test_under_load: $passed of $RUNS passed"
((passed == RUNS))

#!/bin/sh
# Probe for r132: start two :verify:test runs in this checkout at the same moment and report
# each one's verdict. Before gradle/testlock.gradle, one of them could end in
# NoSuchFileException on in-progress-results-generic.bin although its tests passed.
#
#   tools/probe_concurrent_tests.sh [gradle args...]    default: -PwithGl --tests jks.verify.SettingsTest
#
# Logs go to build/probe-concurrent/run{1,2}.log. Exit 0 when both builds succeed.
cd "$(dirname "$0")/.." || exit 2
[ $# -eq 0 ] && set -- -PwithGl --tests jks.verify.SettingsTest
out=build/probe-concurrent
mkdir -p "$out"
./gradlew :verify:test --rerun "$@" >"$out/run1.log" 2>&1 & p1=$!
./gradlew :verify:test --rerun "$@" >"$out/run2.log" 2>&1 & p2=$!
wait $p1; r1=$?
wait $p2; r2=$?
for n in 1 2; do
	printf 'run%s: ' "$n"
	grep -E 'BUILD (SUCCESSFUL|FAILED)' "$out/run$n.log" | tail -1
	grep -E 'exclusiveRun:|NoSuchFileException|EOFException' "$out/run$n.log" | sed 's/^/    /'
done
[ $r1 -eq 0 ] && [ $r2 -eq 0 ]

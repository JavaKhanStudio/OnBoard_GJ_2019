#!/usr/bin/env bash
# offscreen_resizable.sh — like offscreen.sh, but the window can change size (r109).
#
# cage keeps every window at the size of its output, so Gdx.graphics.setWindowedMode() does
# nothing inside it and a test that resizes the window mid-carriage never sees the new size.
# This starts cage, then a rootful Xwayland of its own inside it: an X server with no window
# manager, where a window is whatever size it asks to be. The command runs on that X display,
# still off your screen and off your speakers.
#
# Usage:
#   tools/offscreen_resizable.sh ./gradlew --no-daemon :verify:test -PwithGl --tests jks.verify.WagonResizeRenderTest
# --no-daemon: a Gradle daemon started elsewhere would open its windows on its own display.
set -uo pipefail

if [[ $# -eq 0 ]]; then
	echo "usage: tools/offscreen_resizable.sh <command...>" >&2
	exit 2
fi

if [[ "${ONBOARD_RESIZABLE_INNER:-0}" != "1" ]]; then
	exec env ONBOARD_RESIZABLE_INNER=1 "$(dirname "$0")/offscreen.sh" "$0" "$@"
fi

display=":$(( 40 + RANDOM % 50 ))"
Xwayland "$display" -geometry 1600x900 >/dev/null 2>&1 &
xwayland=$!
for _ in $(seq 50); do [[ -e "/tmp/.X11-unix/X${display#:}" ]] && break; sleep 0.1; done
env -u WAYLAND_DISPLAY DISPLAY="$display" ONBOARD_INSIDE_CAGE=1 "$@"
status=$?
kill "$xwayland" 2>/dev/null
exit $status

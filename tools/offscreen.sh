#!/usr/bin/env bash
# offscreen.sh — run a command so its render windows never appear or steal focus.
#
# WHY: the GL tests each open a real 1600x900 window to get a GPU surface. Five of them
# open and close in a row, on your desktop, in front of whatever you were doing. Running
# them inside `cage` (a headless wlroots compositor) gives them their own invisible
# display — GPU rendering is preserved, you just never see the windows.
#
# The sound goes nowhere too (ALSOFT_DRIVERS=null), so the game's music stays off your
# speakers.
#
# Falls back to running normally if cage is missing, or if you set ONBOARD_NO_OFFSCREEN=1
# because you actually want to watch.
#
# The Gradle tasks do this by themselves now (gradle/offscreen.gradle): the GL tests always,
# runGame and runEditor when a board agent runs them. This wrapper is for everything else
# that opens a window - a packaged build, a fat jar, a scratch program.
#
# Usage:
#   tools/offscreen.sh dist/OnBoard-linux/onboard
#   tools/offscreen.sh java -jar desktop/build/dist/OnBoard-1.0-linuxX64.jar
#
# Borrowed from the same trick in ~/Shadow/tools/grun.sh.
set -uo pipefail

if [[ $# -eq 0 ]]; then
	echo "usage: tools/offscreen.sh <command...>" >&2
	exit 2
fi

if [[ "${ONBOARD_NO_OFFSCREEN:-0}" != "1" ]] && command -v cage >/dev/null 2>&1; then
	# WLR_BACKENDS=headless: no DRM lease, no physical output, nothing on screen.
	# cage brings up its own XWayland, which is what LWJGL's GLFW connects to.
	# ONBOARD_INSIDE_CAGE tells gradle/offscreen.gradle not to start a second one inside.
	# ALSOFT_DRIVERS=null: nobody is listening either. OpenAL Soft, which LWJGL ships, plays
	# into a device that discards it; a Gradle task inside still writes its own WAV.
	exec env WLR_BACKENDS=headless ONBOARD_INSIDE_CAGE=1 ALSOFT_DRIVERS=null cage -- "$@"
fi

if [[ "${ONBOARD_NO_OFFSCREEN:-0}" != "1" ]]; then
	echo "offscreen.sh: cage not found, running on your display instead" >&2
fi
exec "$@"

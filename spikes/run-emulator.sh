#!/usr/bin/env bash
# Relaunch the slimmed nocturne emulator cold.
# Boot settings live in ~/.android/avd/Medium_Phone.avd/config.ini
set -euo pipefail
SDK="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
ADB="$SDK/platform-tools/adb"
EMU="$SDK/emulator/emulator"

# 1. Stop any running instance
if "$ADB" devices 2>/dev/null | grep -q 'emulator-5554'; then
    "$ADB" kill-server >/dev/null 2>&1 || true
fi
pkill -f "qemu-system-x86_64.*Medium_Phone" 2>/dev/null || true
pkill -f "netsim" 2>/dev/null || true
sleep 2

# 2. Fresh adb server
"$ADB" kill-server >/dev/null 2>&1 || true
"$ADB" start-server >/dev/null 2>&1

# 3. Cold boot, no snapshot, no audio, GPU auto
"$EMU" -avd Medium_Phone_API_37.0 -no-window -no-snapshot-load -wipe-data -gpu auto -no-audio -memory 4096
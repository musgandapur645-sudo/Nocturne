#!/usr/bin/env bash
# Capture fresh Nocturne Android UI screenshots from a connected device.
# Usage: scripts/capture-android-ui.sh [--wait]
set -euo pipefail

ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
PKG="com.never.nocturne"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK="$ROOT/androidApp/build/outputs/apk/debug/androidApp-debug.apk"
OUT="$ROOT/screenshots"

if [[ "${1:-}" == "--wait" ]]; then
  echo "Waiting for a device (plug in phone, enable USB debugging, unlock)..."
  for _ in $(seq 1 240); do
    if [[ -n "$("$ADB" devices | awk 'NR>1 && $2=="device"')" ]]; then break; fi
    sleep 5
  done
fi

SERIAL="$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1; exit}')"
if [[ -z "$SERIAL" ]]; then
  echo "No authorized device. Unlock the phone and accept the USB debugging prompt." >&2
  exit 1
fi
echo "Device: $SERIAL"

"$ADB" -s "$SERIAL" install -r "$APK"
"$ADB" -s "$SERIAL" shell settings put system screen_off_timeout 600000
"$ADB" -s "$SERIAL" shell input keyevent KEYCODE_WAKEUP
"$ADB" -s "$SERIAL" shell wm dismiss-keyguard
"$ADB" -s "$SERIAL" shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 4

mkdir -p "$OUT"
shot() {
  "$ADB" -s "$SERIAL" exec-out screencap -p > "$OUT/$1.png"
  echo "  captured $1.png"
}

echo "Capturing initial state..."
shot "ui_00_launch"

echo "Done. Take manual actions (scan folder, play a track, open tabs), then re-run with --shot <name>."

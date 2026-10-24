#!/bin/bash
# Phase 0 helper: start the Android emulator detached from the calling process.
cd "$HOME/Android/Sdk/emulator" || exit 1
exec ./emulator -avd Medium_Phone_API_37.0 -no-snapshot-save -audio pulse -no-boot-anim

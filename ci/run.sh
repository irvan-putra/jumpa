#!/usr/bin/env bash
set -euo pipefail
PKG=com.vanipy.jumpa
mkdir -p ci/shots
VER=$(sed -n 's/^val appVersionName = "\(.*\)"/\1/p' app/build.gradle.kts)
APK="app/build/outputs/apk/debug/jumpa-v${VER}-debug.apk"
test -f "$APK"
adb install -r "$APK"
python3 ci/smoke.py

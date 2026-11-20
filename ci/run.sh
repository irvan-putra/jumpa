#!/usr/bin/env bash
set -euo pipefail
PKG=com.vanipy.jumpa
mkdir -p ci/shots
VER=$(sed -n 's/^val appVersionName = "\(.*\)"/\1/p' app/build.gradle.kts)
APK="app/build/outputs/apk/debug/jumpa-v${VER}-debug.apk"
test -f "$APK"
adb install -r "$APK"
# seed one fictional contact so the system picker has something to pick
RAW_URI=$(adb shell content insert --uri content://com.android.contacts/raw_contacts --bind account_type:s:jumpa-test --bind account_name:s:jumpa-test | tr -d '\r')
RAW_ID=$(echo "$RAW_URI" | grep -o '[0-9]*$')
adb shell content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:"$RAW_ID" --bind mimetype:s:vnd.android.cursor.item/name --bind data1:s:Uji
python3 ci/smoke.py

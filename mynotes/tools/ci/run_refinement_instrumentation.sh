#!/usr/bin/env bash
# Preserve original instrumentation exit status; capture bounded diagnostics on a hang.
set -uo pipefail
set +e
timeout 1500s adb shell am instrument -w -r "$@" app.space.notes.debug.test/androidx.test.runner.AndroidJUnitRunner > refinement-native.txt
code=$?
set -e
{
    echo "Instrumentation exit $code; synthetic debug host only. Assertions are validated separately."
    timeout 15s adb shell dumpsys activity top || true
    timeout 15s adb shell dumpsys window windows | tail -n 200 || true
    timeout 15s adb shell dumpsys power | tail -n 100 || true
    timeout 15s adb shell dumpsys input | tail -n 100 || true
    timeout 15s adb logcat -d -t 500 || true
} > refinement-native-diagnostics.txt 2>&1
exit "$code"

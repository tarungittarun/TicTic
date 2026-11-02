#!/usr/bin/env bash
# Preserve original instrumentation exit status; capture bounded diagnostics on a hang.
set -uo pipefail
set +e
python3 "$(dirname "$0")/instrumentation_watchdog.py" timeout 1500s adb shell am instrument -w -r -e listener app.space.notes.HangDiagnostics "$@" app.space.notes.debug.test/androidx.test.runner.AndroidJUnitRunner
code=$?
set -e
# Independent diagnostic thread writes these even if the app main thread cannot answer dumpsys.
for report in native-test-progress native-stall-threads; do
    timeout 15s adb exec-out run-as app.space.notes.debug cat "files/$report.txt" > "refinement-native-${report#native-}.txt" 2>/dev/null || true
done
{
    echo "Instrumentation exit $code; synthetic debug host only. Assertions are validated separately."
    timeout 15s adb shell dumpsys activity top || true
    timeout 15s adb shell dumpsys window windows | tail -n 200 || true
    timeout 15s adb shell dumpsys power | tail -n 100 || true
    timeout 15s adb shell dumpsys input | tail -n 100 || true
    timeout 15s adb logcat -d -t 500 || true
} > refinement-native-diagnostics.txt 2>&1
exit "$code"

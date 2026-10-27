#!/usr/bin/env bash
set -Eeuo pipefail
API="$1";OUT=evidence/soak;mkdir -p "$OUT"
collect(){ adb logcat -d -t 500 > "$OUT/logcat.txt" 2>&1 || true;adb shell dumpsys meminfo app.space.notes.debug > "$OUT/memory.txt" 2>&1 || true; }
trap collect EXIT
# Same API26 root-only radio fixture, restored to ordinary shell before testing.
if (( API==26 )); then adb root;adb wait-for-device;fi
adb shell settings put global airplane_mode_on 1
adb shell svc wifi disable
adb shell svc data disable
if (( API==26 )); then adb unroot;adb wait-for-device;fi
adb shell id > "$OUT/shell.txt";test "$(adb shell id -u | tr -d '\r')" = 2000
sha256sum test-apks/*.apk > "$OUT/apk-sha256.txt"
adb install -r test-apks/notes-debug.apk
adb install -r test-apks/notes-debug-androidTest.apk
adb shell am instrument -r -w -e disableAnalytics true -e soakMinutes 75 -e class app.space.notes.NativeSoakTest app.space.notes.debug.test/androidx.test.runner.AndroidJUnitRunner | tee "$OUT/instrumentation.txt"
python3 - <<'PY'
from pathlib import Path
import json
from mynotes.tools.ci.results import android
expected=['app.space.notes.NativeSoakTest#repeatedNativeInferenceAndDurableDocumentCyclesStayBounded']
r=android(Path('evidence/soak/instrumentation.txt').read_text(),expected)
Path('evidence/soak/results.json').write_text(json.dumps(r,indent=2))
assert r['ok'],r
PY
adb shell run-as app.space.notes.debug cat files/soak-report.json > "$OUT/report.json"
python3 - <<'PY'
import json
r=json.load(open('evidence/soak/report.json'))
assert r['requested_minutes']==75 and r['elapsed_ms']>=75*60000 and r['iterations']>=4,r
print(json.dumps(r,indent=2))
PY

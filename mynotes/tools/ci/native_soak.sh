#!/usr/bin/env bash
set -Eeuo pipefail
API="$1";OUT=evidence/soak;mkdir -p "$OUT"
exec > >(tee "$OUT/lane.log") 2>&1
trap 'python3 -m mynotes.tools.ci.results build --path "$OUT/lane.log"' ERR
collect(){ adb logcat -d -t 500 > "$OUT/logcat.txt" 2>&1 || true;adb shell dumpsys meminfo app.space.notes.debug > "$OUT/memory.txt" 2>&1 || true; }
trap collect EXIT
# Same API26 root-only radio fixture, restored to ordinary shell before testing.
if (( API==26 )); then adb root;adb wait-for-device;fi
adb shell settings put global airplane_mode_on 1
adb shell svc wifi disable
adb shell svc data disable
if (( API==26 )); then adb unroot;adb wait-for-device;fi
adb shell id > "$OUT/shell.txt";test "$(adb shell id -u | tr -d '\r')" = 2000
DEBUG=$(find test-apks -name notes-debug.apk);TEST=$(find test-apks -name notes-debug-androidTest.apk)
test -f "$DEBUG" && test -f "$TEST"
sha256sum "$DEBUG" "$TEST" > "$OUT/apk-sha256.txt"
adb install -r "$DEBUG"
adb install -r "$TEST"
adb shell am instrument -r -w -e disableAnalytics true -e soakMinutes 70 -e class app.space.notes.NativeSoakTest app.space.notes.debug.test/androidx.test.runner.AndroidJUnitRunner | tee "$OUT/instrumentation.txt"
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
assert r['requested_minutes']==70 and r['elapsed_ms']>=70*60000 and r['iterations']>=4,r
print(json.dumps(r,indent=2))
metrics={k:v for k,v in r.items() if k!='samples'}
print('::notice title=Native soak completed::'+json.dumps(metrics).replace('%','%25').replace('\n','%0A'))
PY

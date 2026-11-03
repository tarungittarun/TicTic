#!/usr/bin/env bash
# One native execution; preserve real results even when the process fails.
set -uo pipefail
night="$1"; orientation="$2"; font="${3:-1.0}"
sha256sum refinement-apks/debug/notes-debug.apk refinement-apks/androidTest/debug/notes-debug-androidTest.apk > actual-apk-sha256.txt
python3 mynotes/tools/ci/instrumentation_watchdog.py timeout 300s adb shell am instrument -w -r -e timeout_msec 240000 -e expectedNight "$night" -e expectedOrientation "$orientation" -e expectedFont "$font" -e class 'app.space.notes.EnvironmentQualificationTest,app.space.notes.CoreJourneysTest#canonicalPdfOptionsAreDiscoverableAndCancelLeavesDocumentUnchanged' app.space.notes.debug.test/androidx.test.runner.AndroidJUnitRunner
code=$?
adb exec-out run-as app.space.notes.debug tar -C files -cf - ux-review > refinement-captures.tar
python3 - <<'PY'
from pathlib import Path
import json
from mynotes.tools.ci.results import android
expected=['app.space.notes.EnvironmentQualificationTest#'+m for m in ['fontScaleMatchesRequestedLane','orientationMatchesRequestedLane','nightResourcesMatchRequestedLane']]+['app.space.notes.CoreJourneysTest#canonicalPdfOptionsAreDiscoverableAndCancelLeavesDocumentUnchanged']
result=android(Path('refinement-native.txt').read_text(errors='replace'),expected)
Path('pdf-regression.json').write_text(json.dumps(result,indent=2)+'\n')
print('::notice title=Original PDF regression actual native result::'+json.dumps(result))
raise SystemExit(0 if result['ok'] else 1)
PY
verified=$?
if [ "$code" -ne 0 ]; then exit "$code"; fi
exit "$verified"

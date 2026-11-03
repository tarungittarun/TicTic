#!/usr/bin/env bash
# Disposable public fixture only. Screenshots do not connect another UiAutomation
# service (which could perturb the tested accessibility connection).
set -uo pipefail
mkdir -p pdf-option-probe
python3 mynotes/tools/ci/instrumentation_watchdog.py timeout 120s adb shell am instrument -w -r -e listener app.space.notes.HangDiagnostics -e timeout_msec 90000 -e class app.space.notes.CoreJourneysTest#canonicalPdfOptionsAreDiscoverableAndCancelLeavesDocumentUnchanged app.space.notes.debug.test/androidx.test.runner.AndroidJUnitRunner &
pid=$!
for frame in $(seq 1 18); do
  adb exec-out screencap -p > "pdf-option-probe/frame-$frame.png"
  adb shell dumpsys window windows > "pdf-option-probe/windows-$frame.txt"
  sleep 0.5
done
wait "$pid"
python3 - <<'PY'
import json
from pathlib import Path
from mynotes.tools.ci.results import android
case='app.space.notes.CoreJourneysTest#canonicalPdfOptionsAreDiscoverableAndCancelLeavesDocumentUnchanged'
r=android(Path('refinement-native.txt').read_text(errors='replace'),[case]);Path('pdf-option-probe/result.json').write_text(json.dumps(r,indent=2)+'\n');print('::notice title=Isolated original PDF options probe::'+json.dumps(r))
PY

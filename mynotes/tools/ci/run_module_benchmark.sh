#!/usr/bin/env bash
# One bounded execution; no assertion retries. Always retain partial measurements.
set -uo pipefail
workload="$1"; night="$2"; orientation="$3"
method=variedImageCorpusMeasuresGroundTruthAndElapsedTime
shard=0
if [[ "$workload" == voice* ]]; then method=publicVoiceCorpusMeasuresTranscriptsDurationsAndPreservation; shard="${workload#voice}"; fi
python3 mynotes/tools/ci/instrumentation_watchdog.py timeout 3300s adb shell am instrument -w -r -e timeout_msec 3200000 -e expectedNight "$night" -e expectedOrientation "$orientation" -e corpusShard "$shard" -e class "app.space.notes.EnvironmentQualificationTest,app.space.notes.ModuleCorpusBenchmark#$method" app.space.notes.debug.test/androidx.test.runner.AndroidJUnitRunner
code=$?
adb exec-out run-as app.space.notes.debug tar -C files -cf - module-benchmark > module-benchmark.tar
python3 - "$method" <<'PY'
import json,sys
from pathlib import Path
from mynotes.tools.ci.results import android
expected=['app.space.notes.EnvironmentQualificationTest#'+m for m in ['fontScaleMatchesRequestedLane','orientationMatchesRequestedLane','nightResourcesMatchRequestedLane']]+['app.space.notes.ModuleCorpusBenchmark#'+sys.argv[1]]
report=android(Path('refinement-native.txt').read_text(errors='replace'),expected)
Path('module-execution.json').write_text(json.dumps(report,indent=2)+'\n')
if not report['ok']:print('::error title=Module corpus execution incomplete::'+json.dumps(report)[:3000])
PY
exit "$code"

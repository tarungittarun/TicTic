#!/usr/bin/env bash
# New public inputs only; do not repeat the already retained149-observation study.
set -uo pipefail
set +e
bash mynotes/tools/ci/run_refinement_instrumentation.sh -e class app.space.notes.AdditionalPublicSceneStudy
code=$?
timeout 20s adb exec-out run-as app.space.notes.debug cat files/additional-public-scenes.json > additional-public-scenes.json
python3 - <<'PY'
import json
from pathlib import Path
from mynotes.tools.ci.results import android
from mynotes.tools.ci.smallfont_scene_report import independent_row
name='app.space.notes.AdditionalPublicSceneStudy#originalPosterAndObliqueUnsupportedLatinSignsRetainAllOutputs'
report=android(Path('refinement-native.txt').read_text(),[name]);Path('additional-public-native-verified.json').write_text(json.dumps(report,indent=2)+'\n');assert report['ok'],report
study=json.loads(Path('additional-public-scenes.json').read_text());assert study['expected']==len(study['cases'])==2 and not study['production_changed'] and not study['physical_acceptance']
frozen=json.loads(Path('mynotes/report-assets/public-ocr-scenes-v3/manifest.json').read_text());expected={r['id']:r for r in frozen['records']};assert {r['id'] for r in study['cases']}==set(expected)
for row in study['cases']:
    source=expected[row['id']];assert row['source_sha256']==source['sha256'] and row['reference']==source['reference'];independent_row(row)
PY
verified=$?
if [ "$code" != 0 ]; then exit "$code"; fi
exit "$verified"

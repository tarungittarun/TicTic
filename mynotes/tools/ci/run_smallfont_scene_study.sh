#!/usr/bin/env bash
# Evaluation only; retain raw hypotheses and failures before returning original status.
set -uo pipefail
set +e
bash mynotes/tools/ci/run_refinement_instrumentation.sh -e class app.space.notes.SmallFontSceneStudy
code=$?
for name in small-font-paired-study public-scene-study; do
  timeout 20s adb exec-out run-as app.space.notes.debug cat "files/$name.json" > "$name.json"
done
python3 - <<'PY'
import json
from pathlib import Path
from mynotes.tools.ci.results import android
names=['app.space.notes.SmallFontSceneStudy#frozenPhotoDialogAndNegativeSceneInputsRetainFailures','app.space.notes.SmallFontSceneStudy#pairedSmallFontCropEnlargementRecordsActualOutputsBeforeAdoption']
report=android(Path('refinement-native.txt').read_text(),names)
Path('smallfont-native-verified.json').write_text(json.dumps(report,indent=2)+'\n')
if not report['ok']:raise SystemExit('Study methods incomplete: '+str(report))
for name,count in [('small-font-paired-study',144),('public-scene-study',5)]:
    data=json.loads(Path(name+'.json').read_text());assert data['expected']==count and len(data['cases'])==count
    assert not data['production_changed'] and not data['physical_acceptance']
    assert all(row['status']=='OK' and row['source_pixels_unchanged'] for row in data['cases'])
PY
verified=$?
if [ "$code" != 0 ]; then exit "$code"; fi
exit "$verified"

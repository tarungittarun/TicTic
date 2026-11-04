#!/usr/bin/env bash
# Public disposable guest, exact F16 weights, balanced ordered phases; no retries or data clearing.
set -euo pipefail
shard="$1"; first="$2"
adb root
python3 -m mynotes.tools.ci.adb_state --uid 0
adb shell settings put global airplane_mode_on 1
adb shell svc wifi disable
adb shell svc data disable
adb unroot
python3 -m mynotes.tools.ci.adb_state --uid 2000
if [[ "$first" == standard ]]; then phases='standard flash'; else phases='flash standard'; fi
failed=0
for phase in $phases; do
  adb install -r "$phase-kernel.apk"
  adb install -r test-kernel.apk
  adb shell am start -W -n app.space.notes.debug/app.space.notes.NotesActivity
  if [[ "$phase" == "$first" ]]; then
    adb shell "run-as app.space.notes.debug sh -c 'mkdir -p files; printf public-install-over > files/kernel-marker'"
  else
    [[ "$(adb exec-out run-as app.space.notes.debug cat files/kernel-marker)" == public-install-over ]]
  fi
  NOTES_NATIVE_OUTPUT="$phase-native.txt" bash mynotes/tools/ci/run_refinement_instrumentation.sh -e timeout_msec 1800000 -e speechShard "$shard" -e speechKernel "$phase" -e class app.space.notes.TrainedWhisperTest,app.space.notes.CompactSpeechBenchmark || failed=1
  adb exec-out run-as app.space.notes.debug cat "files/speech-kernel-study/$phase-$shard.json" > "$phase-$shard.json" || failed=1
  PHASE="$phase" python3 - <<'PY' || failed=1
import json,os,pathlib
from mynotes.tools.ci.results import android
phase=os.environ['PHASE']
expected=['app.space.notes.TrainedWhisperTest#'+m for m in ['actualTrainedCpuModelRunsOfflineAndPreservesPublicAudio','privateWorkerDeathReturnsErrorWithoutKillingNotesOrLosingAudio','cancelledExperimentalRecognitionRetainsAudioWithoutInstallingModel']]+['app.space.notes.CompactSpeechBenchmark#fixedPublicClipsRetainEveryHypothesisAndTime']
r=android(pathlib.Path(phase+'-native.txt').read_text(errors='replace'),expected)
pathlib.Path(phase+'-execution.json').write_text(json.dumps(r,indent=2)+'\n')
print('::notice title=Original native speech phase::'+json.dumps({k:v for k,v in r.items() if k!='cases'}))
if not r['ok']:raise SystemExit(1)
PY
 done
python3 -m mynotes.tools.ci.audit_speech_kernel "standard-$shard.json" "flash-$shard.json" speech-kernel-comparison.json || failed=1
exit "$failed"

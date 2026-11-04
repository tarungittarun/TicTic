#!/usr/bin/env bash
# Disposable synthetic debug guest only. Each model runs once; no clear-data/uninstall.
set -euo pipefail
adb shell settings put global airplane_mode_on 1
adb shell svc wifi disable
adb shell svc data disable
adb shell settings put system font_scale 1.0
adb shell cmd uimode night "$1"
adb install -r f16-smoke.apk
adb install -r test-smoke.apk
python3 -m mynotes.tools.ci.native_environment --stop-pixel-launcher --settle-home
adb shell am start -W -n app.space.notes.debug/app.space.notes.NotesActivity
python3 -m mynotes.tools.ci.native_environment --orientation "$2"
adb shell svc power stayon true
adb shell settings put system screen_off_timeout 3600000
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell "run-as app.space.notes.debug sh -c 'mkdir -p files; printf public-q8-install-over > files/q8-smoke-marker'"
failed=0
for model in f16 q8; do
  if [[ "$model" == q8 ]]; then
    adb install -r q8-smoke.apk
    [[ "$(adb exec-out run-as app.space.notes.debug cat files/q8-smoke-marker)" == public-q8-install-over ]]
    adb shell am start -W -n app.space.notes.debug/app.space.notes.NotesActivity
  fi
  NOTES_NATIVE_OUTPUT="$model-native.txt" bash mynotes/tools/ci/run_refinement_instrumentation.sh -e timeout_msec 180000 -e class app.space.notes.TrainedWhisperTest || failed=1
  python3 -m mynotes.tools.ci.verify_recognition_pilot --classes app.space.notes.TrainedWhisperTest --log "$model-native.txt" --output "$model-verified.json" || failed=1
  # Retain the known old-model-cache growth risk; this is NOT peak RAM measurement.
  adb shell run-as app.space.notes.debug du -sk files/trained-whisper-v1 > "$model-storage.txt"
  for file in refinement-native-*.txt; do
    [[ ! -f "$file" ]] || cp "$file" "$model-$file"
  done
 done
exit "$failed"

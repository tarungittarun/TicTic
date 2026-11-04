#!/usr/bin/env bash
# Disposable synthetic debug guest only. Each model runs once; no clear-data/uninstall.
set -euo pipefail
adb shell settings put global airplane_mode_on 1
adb shell svc wifi disable
adb shell svc data disable
adb shell settings put system font_scale 1.0
adb shell cmd uimode night no
adb install -r fast-smoke.apk
adb install -r test-smoke.apk
python3 -m mynotes.tools.ci.native_environment --stop-pixel-launcher --settle-home
adb shell am start -W -n app.space.notes.debug/app.space.notes.NotesActivity
python3 -m mynotes.tools.ci.native_environment --orientation portrait
adb shell svc power stayon true
adb shell settings put system screen_off_timeout 3600000
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell "run-as app.space.notes.debug sh -c 'mkdir -p files; printf public-kan-best-install-over > files/kan-best-marker'"
failed=0
for model in fast best; do
  if [[ "$model" == best ]]; then
    adb install -r best-smoke.apk
    [[ "$(adb exec-out run-as app.space.notes.debug cat files/kan-best-marker)" == public-kan-best-install-over ]]
    adb shell am start -W -n app.space.notes.debug/app.space.notes.NotesActivity
  fi
  NOTES_NATIVE_OUTPUT="$model-native.txt" bash mynotes/tools/ci/run_refinement_instrumentation.sh -e timeout_msec 180000 -e class app.space.notes.OfflineOcrTest || failed=1
  python3 -m mynotes.tools.ci.verify_recognition_pilot --classes app.space.notes.OfflineOcrTest --log "$model-native.txt" --output "$model-verified.json" || failed=1
  # Measure installed OCR model directory bytes; this is NOT peak RAM or battery measurement.
  adb shell run-as app.space.notes.debug du -sk files/offline-ocr-v1 > "$model-storage.txt"
  for file in refinement-native-*.txt; do
    [[ ! -f "$file" ]] || cp "$file" "$model-$file"
  done
 done
exit "$failed"

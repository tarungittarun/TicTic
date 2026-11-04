#!/usr/bin/env bash
# Own-authored public fixture only. Never retry the original assertion.
set -euxo pipefail
trap 'code=$?; { echo "Setup/command failed: $BASH_COMMAND (exit $code)"; adb shell getprop sys.boot_completed; adb logcat -d -t 200; } > diagnostic-bootstrap.txt 2>&1; exit "$code"' ERR
# Settle actual launcher/provider readiness before changing settings on API26.
python3 -m mynotes.tools.ci.native_environment --stop-pixel-launcher --settle-home
adb shell settings put global airplane_mode_on 1
adb shell svc wifi disable
adb shell svc data disable
adb install -r benchmark-app.apk
adb install -r benchmark-test.apk
adb shell settings put system font_scale 1.0
adb shell am start -W -n app.space.notes.debug/app.space.notes.NotesActivity
set +e
bash mynotes/tools/ci/run_refinement_instrumentation.sh -e timeout_msec 180000 -e class app.space.notes.OfflineOcrTest#automaticBundledRecognitionNeedsNoLanguageChoiceAndKeepsSymbols
code=$?
for name in public-native-ocr-probe.json public-native-ocr-input.png; do
  adb exec-out run-as app.space.notes.debug cat "files/$name" > "$name" || rm -f "$name"
done
exit "$code"

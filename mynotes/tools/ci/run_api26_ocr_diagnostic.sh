#!/usr/bin/env bash
# Own-authored public fixture only. Never retry the original assertion.
set -euxo pipefail
trap 'code=$?; { echo "Setup/command failed: $BASH_COMMAND (exit $code)"; adb shell getprop sys.boot_completed; adb logcat -d -t 200; } > diagnostic-bootstrap.txt 2>&1; exit "$code"' ERR
# API26 radio provisioning requires the same bounded root/unroot handoff as the
# existing refinement workflow. Never execute app fixtures as root.
adb root
python3 -m mynotes.tools.ci.adb_state --uid 0
adb shell settings put global airplane_mode_on 1
adb shell svc wifi disable
adb shell svc data disable
adb unroot
python3 -m mynotes.tools.ci.adb_state --uid 2000
adb install -r benchmark-app.apk
adb install -r benchmark-test.apk
adb shell settings put system font_scale 1.0
adb shell am start -W -n app.space.notes.debug/app.space.notes.NotesActivity
set +e
bash mynotes/tools/ci/run_refinement_instrumentation.sh -e timeout_msec "${OCR_TIMEOUT_MS:-180000}" -e class "${OCR_TEST_CLASS:-app.space.notes.OfflineOcrTest#automaticBundledRecognitionNeedsNoLanguageChoiceAndKeepsSymbols}"
code=$?
for name in public-native-ocr-probe.json public-native-ocr-input.png ocr-common-text-report.json ocr-corpus-report.json; do
  adb exec-out run-as app.space.notes.debug cat "files/$name" > "$name" || rm -f "$name"
done
for name in ocr ocr-holdout; do
  adb exec-out run-as app.space.notes.debug cat "files/module-benchmark/$name.json" > "$name.json" || rm -f "$name.json"
done
exit "$code"

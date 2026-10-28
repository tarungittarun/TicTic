#!/usr/bin/env bash
# One emulator, one explicit aspect/API lane. No retries and no independent APK rebuilds.
set -Eeuo pipefail
: "${LANE_ID:?}" "${LANE_GROUP:?}" "${LANE_API:?}"
OUT="evidence/$LANE_ID"
mkdir -p "$OUT"
exec > >(tee "$OUT/lane.log") 2>&1
set -x
collect() {
  for scene in search-keyboard numeric-h1 numeric-h2 home editor editor-keyboard format format-keyboard capture palette labels diagram checklist settings storage; do
    timeout 5s adb exec-out run-as app.space.notes.debug cat "files/ux-review/$scene.png" > "$OUT/ux-$scene.png" 2>/dev/null || true
  done
  timeout 15s adb logcat -d -t 500 > "$OUT/logcat.txt" 2>&1 || true
  timeout 15s adb shell dumpsys activity activities > "$OUT/activities.txt" 2>&1 || true
  timeout 15s adb exec-out screencap -p > "$OUT/final-state.png" 2>/dev/null || true
}
trap collect EXIT
failure() {
  local status="$1" command="$2" line="$3"
  set +x
  printf '::error title=Android lane failed::%s at script line %s (exit %s)\n' "$command" "$line" "$status"
  python3 -m mynotes.tools.ci.results build --path "$OUT/lane.log" || true
  collect
  python3 -m mynotes.tools.ci.results build --path "$OUT/logcat.txt" || true
  exit "$status"
}
trap 'failure "$?" "$BASH_COMMAND" "$LINENO"' ERR
python3 -m mynotes.tools.ci.test_plan --output "$OUT/test-plan.json"
CLASSES=$(python3 -m mynotes.tools.ci.test_plan --group "$LANE_GROUP" --classes)
RELEASE=$(python3 -c 'import json;print("accepted-release/"+json.load(open("accepted-release/audit.json"))["filename"])')
DEBUG=$(find test-apks -name 'notes-debug.apk')
TEST=$(find test-apks -name 'notes-debug-androidTest.apk')
test -f "$DEBUG" && test -f "$TEST" && test -f "$RELEASE"
# API 26 AOSP denies shell CHANGE_WIFI_STATE. Root only the fixture setup,
# then restore the ordinary shell before installing/testing either app.
if (( LANE_API == 26 )); then
  adb root || echo 'Root transition command lost transport; verifying required root state.'
  python3 -m mynotes.tools.ci.adb_state --uid 0
  test "$(adb shell id -u | tr -d '\r')" = 0
fi
# Flight-mode settings plus disabled radios; APK audit independently rejects INTERNET.
adb shell settings put global airplane_mode_on 1
adb shell svc wifi disable
adb shell svc data disable
if (( LANE_API == 26 )); then
  adb unroot || echo 'Unroot transition command lost transport; verifying required ordinary-shell state.'
  python3 -m mynotes.tools.ci.adb_state --uid 2000
fi
# Configure real resource variants before either app starts; do not fake theme assertions.
adb shell settings put system font_scale "${LANE_FONT:-1.0}"
adb shell settings put system accelerometer_rotation 0
if [[ "${LANE_ORIENTATION:-portrait}" == landscape ]]; then
  adb shell settings put system user_rotation 1
  if (( LANE_API >= 31 )); then
    adb shell wm set-ignore-orientation-request true
    adb shell wm fixed-to-user-rotation enabled
    adb shell wm user-rotation lock 1
  fi
elif [[ "${LANE_ORIENTATION:-portrait}" == portrait ]]; then
  adb shell settings put system user_rotation 0
fi
if (( LANE_API >= 29 )); then adb shell cmd uimode night "${LANE_NIGHT:-no}"; fi
adb shell dumpsys window displays > "$OUT/display-policy.txt"
adb shell wm size > "$OUT/display-size.txt"
adb shell wm density > "$OUT/display-density.txt"
adb shell settings get system font_scale > "$OUT/font-scale.txt"
adb shell id > "$OUT/test-shell-identity.txt"
test "$(adb shell id -u | tr -d '\r')" = 2000
adb shell settings get global airplane_mode_on > "$OUT/airplane-mode.txt"
grep -qx '1' <(tr -d '\r' < "$OUT/airplane-mode.txt")
adb shell dumpsys connectivity > "$OUT/connectivity.txt"
python3 - <<'PYBASE'
import hashlib,json
from pathlib import Path
metadata=json.loads(Path('upgrade-baseline/identity.json').read_text())
assert hashlib.sha256(Path('upgrade-baseline/baseline.apk').read_bytes()).hexdigest()==metadata['sha256']
PYBASE
cp upgrade-baseline/identity.json "$OUT/upgrade-baseline-identity.json"
adb install -r upgrade-baseline/baseline.apk | tee "$OUT/install-baseline.txt"
adb shell am start -W -n app.space.notes/.NotesActivity
find_release_text() {
  local target="$1"
  for attempt in {1..8}; do
    adb shell rm -f /sdcard/notes-upgrade.xml
    if adb shell uiautomator dump /sdcard/notes-upgrade.xml >/dev/null && adb pull /sdcard/notes-upgrade.xml "$OUT/upgrade-window.xml" >/dev/null; then
      if python3 -m mynotes.tools.ci.ui --xml "$OUT/upgrade-window.xml" --text "$target" > "$OUT/coordinates.txt"; then return 0; fi
    fi
    # A null root during an Android taskbar/window transition is not a completed UI lookup.
    adb shell dumpsys window windows > "$OUT/selector-attempt-$attempt-window.txt" 2>&1 || true
  done
  return 1
}
find_release_text '+ Text'
read -r X Y < "$OUT/coordinates.txt";adb shell input tap "$X" "$Y"
find_release_text 'Title'
read -r X Y < "$OUT/coordinates.txt";adb shell input tap "$X" "$Y"
SEED="Upgrade_${LANE_ID}"
adb shell input text "$SEED"
adb shell input keyevent KEYCODE_BACK
adb shell input keyevent KEYCODE_BACK
adb shell am start -W -n app.space.notes/.NotesActivity
find_release_text "$SEED"
cp "$OUT/upgrade-window.xml" "$OUT/seed-before-upgrade.xml"
adb install -r "$RELEASE" | tee "$OUT/install-update.txt"
adb shell dumpsys package app.space.notes > "$OUT/package.txt"
adb shell am start -W -n app.space.notes/.NotesActivity | tee "$OUT/launch.txt"
grep -q 'Status: ok' "$OUT/launch.txt"
find_release_text "$SEED"
cp "$OUT/upgrade-window.xml" "$OUT/seed-after-upgrade.xml"
printf '%s\n' "$SEED" > "$OUT/preserved-note-title.txt"
adb shell pidof app.space.notes > "$OUT/pid.txt"
test -s "$OUT/pid.txt"
adb shell uiautomator dump /sdcard/notes-release-window.xml > "$OUT/window-dump.txt"
adb pull /sdcard/notes-release-window.xml "$OUT/release-window.xml"
adb exec-out screencap -p > "$OUT/release-home.png"
BASE=$(adb shell pm path app.space.notes | tr -d '\r' | sed -n 's/^package://p')
test -n "$BASE"
adb pull "$BASE" "$OUT/installed-base.apk"
python3 - "$RELEASE" "$OUT" <<'PYHASH'
import hashlib,json,os,sys
from pathlib import Path
release=Path(sys.argv[1]);out=Path(sys.argv[2]);audit=json.loads(Path('accepted-release/audit.json').read_text())
installed=hashlib.sha256((out/'installed-base.apk').read_bytes()).hexdigest()
download=hashlib.sha256(release.read_bytes()).hexdigest()
assert installed==download==audit['accepted_sha256'],'Installed/downloaded/audited APK bytes differ'
metadata={'lane':os.environ['LANE_ID'],'group':os.environ['LANE_GROUP'],'api':int(os.environ['LANE_API']),
          'source':os.environ['GITHUB_SHA'],'installed_sha256':installed,'download_sha256':download,
          'audited_sha256':audit['accepted_sha256'],'accepted_from':audit['accepted_from'],
          'release_versionCode':audit['versionCode'],'network':'airplane setting enabled, radios disabled; release has no INTERNET permission',
          'test_shell_uid':2000,'radio_setup_root':int(os.environ['LANE_API'])==26,
          'font':float(os.environ.get('LANE_FONT','1.0')),'night':os.environ.get('LANE_NIGHT','no'),
          'orientation':os.environ.get('LANE_ORIENTATION','portrait'),'profile':os.environ.get('LANE_PROFILE','Nexus 5'),'mic':os.environ.get('LANE_MIC','denied'),'notifications':os.environ.get('LANE_NOTIFICATIONS') or 'denied'}
metadata['installed_bytes']=(out/'installed-base.apk').stat().st_size
(out/'identity.json').write_text(json.dumps(metadata,indent=2)+'\n')
# Do not upload one full large native APK per lane: keep the hash receipt; the shared audited artifact preserves bytes.
(out/'installed-base.apk').unlink()
PYHASH
adb install -r "$DEBUG"
adb install -r "$TEST"
if [[ "${LANE_MIC:-denied}" == granted ]]; then
  adb shell pm grant app.space.notes.debug android.permission.RECORD_AUDIO
fi
if (( LANE_API >= 33 )) && [[ "${LANE_NOTIFICATIONS:-denied}" == granted ]]; then
  adb shell pm grant app.space.notes.debug android.permission.POST_NOTIFICATIONS
fi
if [[ "${LANE_ORIENTATION:-portrait}" == landscape ]] && (( LANE_API >= 31 )); then
  # Installation/system windows can request NOSENSOR/LOCKED; the full lane must remain in real landscape.
  adb shell wm fixed-to-user-rotation enabled
  adb shell wm user-rotation lock 1
  adb shell wm user-rotation > "$OUT/rotation-before.txt"
  adb shell wm fixed-to-user-rotation > "$OUT/fixed-rotation-before.txt"
fi
adb shell dumpsys display > "$OUT/display-before-instrumentation.txt"
adb shell am instrument -r -w -e disableAnalytics true -e expectedMic "${LANE_MIC:-denied}" -e expectedFont "${LANE_FONT:-1.0}" -e expectedNight "${LANE_NIGHT:-no}" -e expectedOrientation "${LANE_ORIENTATION:-portrait}" -e class "$CLASSES" \
  app.space.notes.debug.test/androidx.test.runner.AndroidJUnitRunner | tee "$OUT/instrumentation.txt"
adb shell dumpsys display > "$OUT/display-after-instrumentation.txt"
adb shell settings get system user_rotation > "$OUT/user-rotation-after.txt"
if [[ "$LANE_GROUP" == all || "$LANE_GROUP" == ocr ]]; then
  adb shell run-as app.space.notes.debug cat files/ocr-corpus-report.json > "$OUT/ocr-corpus-report.json"
  for language in hin kan; do adb shell run-as app.space.notes.debug cat "files/ocr-fixture-$language.png" > "$OUT/ocr-fixture-$language.png"; done
  python3 - "$OUT/ocr-corpus-report.json" <<'PYOCR'
import json,sys
report=json.load(open(sys.argv[1]));assert len(report['cases'])==2
for case in report['cases']:
    message=json.dumps(case,ensure_ascii=False).replace('%','%25').replace('\r','%0D').replace('\n','%0A')
    print('::notice title=Controlled OCR diagnostic::'+message)
    if not case['token_match']:print('::warning title=Controlled OCR recognition gap::'+case['language']+' did not recover its expected token. Inspect the saved image/text; model initialization is not an accuracy guarantee.')
PYOCR
fi
python3 -m mynotes.tools.ci.results android --group "$LANE_GROUP" \
  --path "$OUT/instrumentation.txt" --output "$OUT/results.json"

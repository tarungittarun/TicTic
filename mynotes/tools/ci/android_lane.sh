#!/usr/bin/env bash
# One emulator, one explicit aspect/API lane. No retries and no independent APK rebuilds.
set -Eeuo pipefail
: "${LANE_ID:?}" "${LANE_GROUP:?}" "${LANE_API:?}"
OUT="evidence/$LANE_ID"
mkdir -p "$OUT"
exec > >(tee "$OUT/lane.log") 2>&1
set -x
collect() {
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
# Flight-mode settings plus disabled radios; APK audit independently rejects INTERNET.
adb shell settings put global airplane_mode_on 1
adb shell svc wifi disable
adb shell svc data disable
adb shell settings get global airplane_mode_on > "$OUT/airplane-mode.txt"
grep -qx '1' <(tr -d '\r' < "$OUT/airplane-mode.txt")
adb shell dumpsys connectivity > "$OUT/connectivity.txt"
adb install -r mynotes/releases/MyNotes-v0.1.0-1.apk | tee "$OUT/install-baseline.txt"
adb install -r "$RELEASE" | tee "$OUT/install-update.txt"
adb shell dumpsys package app.space.notes > "$OUT/package.txt"
adb shell am start -W -n app.space.notes/.NotesActivity | tee "$OUT/launch.txt"
grep -q 'Status: ok' "$OUT/launch.txt"
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
          'release_versionCode':audit['versionCode'],'network':'airplane setting enabled, radios disabled; release has no INTERNET permission'}
(out/'identity.json').write_text(json.dumps(metadata,indent=2)+'\n')
PYHASH
adb install -r "$DEBUG"
adb install -r "$TEST"
adb shell am instrument -r -w -e disableAnalytics true -e class "$CLASSES" \
  app.space.notes.debug.test/androidx.test.runner.AndroidJUnitRunner | tee "$OUT/instrumentation.txt"
python3 -m mynotes.tools.ci.results android --group "$LANE_GROUP" \
  --path "$OUT/instrumentation.txt" --output "$OUT/results.json"

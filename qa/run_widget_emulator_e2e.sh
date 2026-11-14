#!/usr/bin/env bash
# Invoked as one command by android-emulator-runner. Its script parser runs each
# non-empty line in a fresh `sh`, so keep the workflow entrypoint to one line.
set -Eeuo pipefail

readonly APP_PACKAGE="app.space.one"
readonly TEST_PACKAGE="app.space.one.test"
readonly SCREEN_DIR="/tmp/widget-e2e-screenshots"
readonly INSTRUMENTATION_LOG="/tmp/widget-e2e-instrumentation.log"
phase="initializing"

on_error() {
    local status=$1 failed_line=$2 failed_command=$3
    local detail="Phase: $phase · line $failed_line · exit $status · command: $failed_command"
    printf 'Widget E2E script failed: %s\n' "$detail" >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
    local escaped
    escaped=$(printf '%s' "$detail" | sed 's/%/%25/g; s/\r//g; s/::/%3A%3A/g')
    echo "::error title=Widget emulator setup failed::$escaped"
    exit "$status"
}
trap 'on_error "$?" "$LINENO" "$BASH_COMMAND"' ERR

phase="locate APKs"
main_apk=$(find app/build/outputs/apk -path '*/lite/debug/*.apk' \
    ! -path '*/androidTest/*' -print -quit)
test_apk=$(find app/build/outputs/apk -path '*/androidTest/lite/debug/*.apk' -print -quit)
test -n "$main_apk" && test -s "$main_apk"
test -n "$test_apk" && test -s "$test_apk"
printf 'App APK: %s\nInstrumentation APK: %s\n' "$main_apk" "$test_apk"

phase="wait for emulator"
adb wait-for-device
adb shell getprop sys.boot_completed
phase="install target APK"
adb install -r "$main_apk"
phase="install instrumentation APK"
adb install -r "$test_apk"
phase="grant emulator notification access"
adb shell pm grant "$APP_PACKAGE" android.permission.POST_NOTIFICATIONS 2>/dev/null || true
phase="prepare a clean emulator screenshot directory"
adb shell run-as "$APP_PACKAGE" rm -rf files/widget-e2e
adb shell run-as "$APP_PACKAGE" mkdir -p files/widget-e2e

phase="run Android instrumentation suite"
set +e
adb shell am instrument -w -r "$TEST_PACKAGE/androidx.test.runner.AndroidJUnitRunner" \
    > "$INSTRUMENTATION_LOG" 2>&1
test_status=$?
set -e
phase="print instrumentation results"
cat "$INSTRUMENTATION_LOG"

phase="prepare screenshot export"
rm -rf "$SCREEN_DIR"
mkdir -p "$SCREEN_DIR"
phase="list emulator screenshots"
mapfile -t images < <(
    adb exec-out run-as "$APP_PACKAGE" ls -1 files/widget-e2e 2>/dev/null \
        | tr -d '\r' | grep '\.jpg$' || true
)
for image in "${images[@]}"; do
    phase="copy emulator screenshot $image"
    adb exec-out run-as "$APP_PACKAGE" cat "files/widget-e2e/$image" \
        > "$SCREEN_DIR/$image"
done

count=$(find "$SCREEN_DIR" -maxdepth 1 -type f -name '*.jpg' | wc -l | tr -d ' ')
{
    printf '### API 34 emulator evidence\n\n'
    printf 'Screenshot count: **%s** (expected 24)\n\n' "$count"
    printf 'Instrumentation exit code: `%s`\n\n' "$test_status"
    if [ -f "$INSTRUMENTATION_LOG" ]; then
        printf '<details><summary>Instrumentation result</summary>\n\n```text\n'
        tail -n 45 "$INSTRUMENTATION_LOG"
        printf '\n```\n</details>\n'
    fi
} >> "${GITHUB_STEP_SUMMARY:-/dev/null}"

if [ "$count" -ne 24 ]; then
    echo "::error title=Widget screenshot count::Expected 24 emulator captures; got $count"
    printf 'Captured filenames: %s\n' "${images[*]:-none}"
    test_status=1
fi

# The workflow emits the exported captures in three follow-up groups. One JPEG per
# notice keeps each image intact and stays under GitHub's per-step annotation limit;
# no artifact upload or publishing step is used.
printf 'Screenshots are surfaced in the three E2E screenshot evidence steps below.\n' \
    >> "${GITHUB_STEP_SUMMARY:-/dev/null}"

if [ "$test_status" -ne 0 ] || ! grep -q 'OK (' "$INSTRUMENTATION_LOG"; then
    excerpt=$(grep -E 'INSTRUMENTATION_STATUS:|FAILURES!!!|AssertionError|Exception|Error in |Tests run:|OK \(' \
        "$INSTRUMENTATION_LOG" | tail -n 35 || true)
    if [ -z "$excerpt" ]; then excerpt=$(tail -n 30 "$INSTRUMENTATION_LOG"); fi
    escaped=$(printf '%s\n' "$excerpt" | sed 's/%/%25/g; s/\r//g; s/::/%3A%3A/g' \
        | awk 'BEGIN { ORS = "%0A" } { print }')
    echo "::error title=Widget instrumentation failure::$escaped"
    exit 1
fi

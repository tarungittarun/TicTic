#!/usr/bin/env bash
# Invoked as one command by android-emulator-runner. Its script parser runs each
# non-empty line in a fresh `sh`, so keep the workflow entrypoint to one line.
set -Eeuo pipefail

readonly APP_PACKAGE="app.space.one"
readonly TEST_PACKAGE="app.space.one.test"
readonly SCREEN_DIR="/tmp/widget-e2e-screenshots"
readonly INSTRUMENTATION_LOG="/tmp/widget-e2e-instrumentation.log"

on_error() {
    local status=$?
    printf 'Widget E2E script failed at line %s (exit %s): %s\n' \
        "$LINENO" "$status" "$BASH_COMMAND" >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
    echo "::error title=Widget emulator setup failed::Script line $LINENO exited with status $status"
    exit "$status"
}
trap on_error ERR

main_apk=$(find app/build/outputs/apk -path '*/lite/debug/*.apk' \
    ! -path '*/androidTest/*' -print -quit)
test_apk=$(find app/build/outputs/apk -path '*/androidTest/lite/debug/*.apk' -print -quit)
test -n "$main_apk" && test -s "$main_apk"
test -n "$test_apk" && test -s "$test_apk"
printf 'App APK: %s\nInstrumentation APK: %s\n' "$main_apk" "$test_apk"

adb wait-for-device
adb shell getprop sys.boot_completed
adb install -r "$main_apk"
adb install -r "$test_apk"
adb shell pm grant "$APP_PACKAGE" android.permission.POST_NOTIFICATIONS 2>/dev/null || true

set +e
adb shell am instrument -w -r "$TEST_PACKAGE/androidx.test.runner.AndroidJUnitRunner" \
    > "$INSTRUMENTATION_LOG" 2>&1
test_status=$?
set -e
cat "$INSTRUMENTATION_LOG"

rm -rf "$SCREEN_DIR"
mkdir -p "$SCREEN_DIR"
mapfile -t images < <(
    adb exec-out run-as "$APP_PACKAGE" ls files/widget-e2e 2>/dev/null \
        | tr -d '\r' | grep '\.jpg$' || true
)
for image in "${images[@]}"; do
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

# Emit each compact JPEG as a notice so evidence is viewable from the workflow run
# without an artifact upload or a publishing step.
for image in "$SCREEN_DIR"/*.jpg; do
    [ -f "$image" ] || continue
    payload=$(base64 -w0 "$image")
    printf '::notice title=E2E screenshot %s::%s\n' "${image##*/}" "$payload"
done

if [ "$test_status" -ne 0 ] || ! grep -q 'OK (' "$INSTRUMENTATION_LOG"; then
    excerpt=$(grep -E 'INSTRUMENTATION_STATUS:|FAILURES!!!|AssertionError|Exception|Error in |Tests run:|OK \(' \
        "$INSTRUMENTATION_LOG" | tail -n 35 || true)
    if [ -z "$excerpt" ]; then excerpt=$(tail -n 30 "$INSTRUMENTATION_LOG"); fi
    escaped=$(printf '%s\n' "$excerpt" | sed 's/%/%25/g; s/\r//g; s/::/%3A%3A/g' \
        | awk 'BEGIN { ORS = "%0A" } { print }')
    echo "::error title=Widget instrumentation failure::$escaped"
    exit 1
fi

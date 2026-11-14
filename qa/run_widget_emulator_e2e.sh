#!/usr/bin/env bash
# Invoked as one command by android-emulator-runner. Its script parser runs each
# non-empty line in a fresh `sh`, so keep the workflow entrypoint to one line.
set -Eeuo pipefail

readonly APP_PACKAGE="app.space.one"
readonly TEST_PACKAGE="app.space.one.test"
readonly SCREEN_DIR="/tmp/widget-e2e-screenshots"
readonly INSTRUMENTATION_LOG="/tmp/widget-e2e-instrumentation.log"
readonly TARGET_INSTALL_LOG="/tmp/widget-e2e-target-install.log"
readonly TEST_INSTALL_LOG="/tmp/widget-e2e-test-install.log"
phase="initializing"

on_error() {
    local status=$1 failed_line=$2 failed_command=$3
    local command_label=$failed_command
    if [[ "$phase" == "install target APK" ]]; then
        command_label="adb install -r \"$main_apk\" | tee \"$TARGET_INSTALL_LOG\""
    elif [[ "$phase" == "install instrumentation APK" ]]; then
        command_label="adb install -r \"$test_apk\" | tee \"$TEST_INSTALL_LOG\""
    elif [[ "$phase" == "recover emulator after instrumentation APK install failure" ]]; then
        command_label="adb reboot, wait for sys.boot_completed, and reinstall both APKs"
    fi
    local detail="Phase: $phase · line $failed_line · exit $status · command: $command_label"
    if [[ "$phase" == "wait for emulator" || "$phase" == "install target APK" || "$phase" == "install instrumentation APK" || "$phase" == "recover emulator after instrumentation APK install failure" ]]; then
        local setup_log="/tmp/widget-e2e-setup-diagnostics.log"
        local install_log=""
        if [[ "$phase" == "install target APK" ]]; then install_log=$TARGET_INSTALL_LOG; fi
        if [[ "$phase" == "install instrumentation APK" ]]; then install_log=$TEST_INSTALL_LOG; fi
        : > "$setup_log"
        {
            printf 'Failed setup phase: %s\n' "$phase"
            if [[ -n "$install_log" && -f "$install_log" ]]; then
                printf '\nAPK install output:\n'
                tail -n 40 "$install_log"
            fi
            if [[ -f /tmp/widget-e2e-install-recovery.log ]]; then
                printf '\nInstrumentation install recovery attempt:\n'
                tail -n 60 /tmp/widget-e2e-install-recovery.log
            fi
            printf '\nHost memory:\n'
            free -h 2>&1 || true
            printf '\nHost emulator processes:\n'
            ps -eo pid,rss,etime,comm,args \
                | awk '$4 == "emulator" || $4 ~ /^qemu-system-/ { print }' \
                | tail -n 12 || true
            printf '\nADB devices:\n'
            timeout 10s adb devices -l 2>&1 || true
            printf '\nGuest boot state:\n'
            timeout 10s adb shell getprop sys.boot_completed 2>&1 || true
            printf '\nGuest storage:\n'
            timeout 10s adb shell df -h /data 2>&1 || true
            printf '\nGuest memory:\n'
            timeout 10s adb shell cat /proc/meminfo 2>&1 | grep -E 'MemTotal|MemFree|MemAvailable|SwapTotal|SwapFree' || true
            printf '\nRecent package/install/system signals:\n'
            timeout 15s adb logcat -d -b all -v threadtime -t 1000 2>&1 \
                | grep -Ei 'PackageManager|PackageInstaller|INSTALL_FAILED|installd|dex2oat|app[.]space[.]one|system_server|lmkd|lowmemorykiller|watchdog' \
                | tail -n 60 || true
        } >> "$setup_log"
        {
            printf '\n#### Emulator setup diagnostics\n\n```text\n'
            tail -n 80 "$setup_log"
            printf '\n```\n'
        } >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
        local state_escaped signals_escaped
        state_escaped=$(head -n 30 "$setup_log" | cut -c1-100 \
            | sed 's/%/%25/g; s/::/%3A%3A/g' | awk 'BEGIN { ORS = "%0A" } { print }')
        signals_escaped=$(tail -n 12 "$setup_log" | cut -c1-120 \
            | sed 's/%/%25/g; s/::/%3A%3A/g' | awk 'BEGIN { ORS = "%0A" } { print }')
        echo "::error title=Emulator setup state::$state_escaped"
        echo "::error title=Emulator package and system signals::$signals_escaped"
    fi
    printf 'Widget E2E script failed: %s\n' "$detail" >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
    local escaped
    escaped=$(printf '%s' "$detail" | sed 's/%/%25/g; s/\r//g; s/::/%3A%3A/g')
    echo "::error title=Widget emulator setup failed::$escaped"
    exit "$status"
}
trap 'on_error "$?" "$LINENO" "$BASH_COMMAND"' ERR

install_instrumentation_apk_with_recovery() {
    local first_status=0
    adb install -r "$test_apk" 2>&1 | tee "$TEST_INSTALL_LOG" || first_status=$?
    if [ "$first_status" -eq 0 ]; then return 0; fi

    cp "$TEST_INSTALL_LOG" /tmp/widget-e2e-test-install-initial.log 2>/dev/null || true
    phase="recover emulator after instrumentation APK install failure"
    local recovery_log="/tmp/widget-e2e-install-recovery.log"
    : > "$recovery_log"
    {
        printf 'Instrumentation APK install failed with exit %s; collecting pre-reboot state.\n' "$first_status"
        printf '\nInitial instrumentation APK install output:\n'
        tail -n 40 /tmp/widget-e2e-test-install-initial.log 2>/dev/null || true
        printf '\nSystem crash buffer:\n'
        timeout 15s adb logcat -d -b crash -v threadtime -t 200 2>&1 || true
        printf '\nRecent system-server and package-manager signals:\n'
        timeout 15s adb logcat -d -b all -v threadtime -t 1200 2>&1 \
            | grep -Ei 'system_server|Lost network stack|PackageManager|PackageInstaller|INSTALL_FAILED|installd|lmkd|watchdog' \
            | tail -n 60 || true
    } >> "$recovery_log"
    {
        printf '\n#### Instrumentation APK install recovery\n\n```text\n'
        tail -n 60 "$recovery_log"
        printf '\n```\n'
    } >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
    if ! grep -Eiq 'am_crash: .*system_server|Lost network stack|Zygote failed to write to system_server FD|ANR in system_server|system_server.*(died|fatal|crash|not responding)' "$recovery_log"; then
        printf 'No recoverable system_server failure was found; leaving the original install failure intact.\n' \
            | tee -a "$recovery_log"
        phase="install instrumentation APK"
        return "$first_status"
    fi
    echo "::warning title=Emulator install recovery::Instrumentation APK installation failed after a system_server crash; rebooting the emulator and retrying the two APK installs once."

    local reboot_status=0
    timeout 30s adb reboot || reboot_status=$?
    if [ "$reboot_status" -ne 0 ]; then
        printf 'adb reboot returned exit %s; checking whether the device recovers anyway.\n' "$reboot_status" \
            | tee -a "$recovery_log"
    fi
    if ! timeout 240s adb wait-for-device; then
        printf 'ADB device did not return after the recovery reboot.\n' | tee -a "$recovery_log"
        return 1
    fi
    local deadline=$((SECONDS + 240))
    local boot_state=""
    while (( SECONDS < deadline )); do
        boot_state=$(timeout 10s adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)
        if [[ "$boot_state" == "1" ]]; then break; fi
        sleep 2
    done
    if [[ "$boot_state" != "1" ]]; then
        printf 'Android did not report sys.boot_completed=1 after recovery reboot.\n' | tee -a "$recovery_log"
        return 1
    fi

    phase="install target APK"
    adb install -r "$main_apk" 2>&1 | tee "$TARGET_INSTALL_LOG"
    phase="install instrumentation APK"
    adb install -r "$test_apk" 2>&1 | tee "$TEST_INSTALL_LOG"
}

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
adb install -r "$main_apk" 2>&1 | tee "$TARGET_INSTALL_LOG"
phase="install instrumentation APK"
install_instrumentation_apk_with_recovery
phase="grant emulator notification access"
adb shell pm grant "$APP_PACKAGE" android.permission.POST_NOTIFICATIONS 2>/dev/null || true
phase="prepare a clean emulator screenshot directory"
adb shell run-as "$APP_PACKAGE" rm -rf files/widget-e2e
adb shell run-as "$APP_PACKAGE" mkdir -p files/widget-e2e

phase="run Android instrumentation suite"
if adb shell am instrument -w -r "$TEST_PACKAGE/androidx.test.runner.AndroidJUnitRunner" \
        > "$INSTRUMENTATION_LOG" 2>&1; then
    test_status=0
else
    test_status=$?
fi
if [ "$test_status" -ne 0 ] || ! grep -q 'OK (' "$INSTRUMENTATION_LOG"; then
    phase="capture emulator failure diagnostics"
    readonly DEVICE_DIAGNOSTICS_LOG="/tmp/widget-e2e-device-diagnostics.log"
    : > "$DEVICE_DIAGNOSTICS_LOG"
    {
        printf 'Host emulator processes:\n'
        ps -eo pid,rss,etime,comm,args \
            | awk '$4 == "emulator" || $4 ~ /^qemu-system-/ { print }' \
            | tail -n 20 || true
        printf '\nHost memory:\n'
        free -h 2>&1 || true
        printf '\nADB devices:\n'
        timeout 10s adb devices -l 2>&1 || true
        printf '\nGuest boot state and memory:\n'
        timeout 10s adb shell getprop sys.boot_completed 2>&1 || true
        timeout 10s adb shell cat /proc/meminfo 2>&1 | grep -E 'MemTotal|MemFree|MemAvailable|SwapTotal|SwapFree' || true
        printf '\nAndroid crash buffer:\n'
        timeout 15s adb logcat -d -b crash -v threadtime -t 300 2>&1 || true
        printf '\nRecent system/test failure signals:\n'
        timeout 15s adb logcat -d -b all -v threadtime -t 4000 2>&1 | grep -Ei 'FATAL EXCEPTION|Fatal signal|System server|system_server|INSTRUMENTATION_ABORTED|ANR in|lmkd|lowmemorykiller|AndroidRuntime|app\.space\.one|WidgetConceptE2ETest|WidgetActionReceiver|TimerReceiver|START_FOCUS|watchdog' | tail -n 100 || true
    } >> "$DEVICE_DIAGNOSTICS_LOG"
    {
        printf '\n#### Emulator failure diagnostics\n\n```text\n'
        tail -n 100 "$DEVICE_DIAGNOSTICS_LOG"
        printf '\n```\n'
    } >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
    escape_annotation() {
        sed 's/%/%25/g; s/\r//g; s/::/%3A%3A/g' | awk 'BEGIN { ORS = "%0A" } { print }'
    }
    host_resources=$(
        {
            sed -n '/^Host emulator processes:/,/^ADB devices:/p' "$DEVICE_DIAGNOSTICS_LOG"
            sed -n '/^Host memory:/,/^ADB devices:/p' "$DEVICE_DIAGNOSTICS_LOG"
        } | grep -v '^ADB devices:' | tail -n 20 | cut -c1-180 | escape_annotation
    )
    guest_resources=$(sed -n '/^Guest boot state and memory:/,/^Android crash buffer:/p' \
        "$DEVICE_DIAGNOSTICS_LOG" | grep -v '^Android crash buffer:' | cut -c1-180 | escape_annotation)
    system_signals=$(grep -Ei 'FATAL EXCEPTION|Fatal signal|system_server|SystemUI|System UI|ANR in|Long monitor contention|dvm_lock_sample|lmkd|lowmemorykiller|watchdog' \
        "$DEVICE_DIAGNOSTICS_LOG" | tail -n 12 | cut -c1-200 | escape_annotation || true)
    if [ -n "$host_resources" ]; then echo "::error title=Emulator host resources::$host_resources"; fi
    if [ -n "$guest_resources" ]; then echo "::error title=Emulator guest resources::$guest_resources"; fi
    if [ -n "$system_signals" ]; then echo "::error title=Emulator system responsiveness signals::$system_signals"; fi
fi

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
        tail -n 100 "$INSTRUMENTATION_LOG"
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
        "$INSTRUMENTATION_LOG" | tail -n 100 || true)
    if [ -z "$excerpt" ]; then excerpt=$(tail -n 30 "$INSTRUMENTATION_LOG"); fi
    escaped=$(printf '%s\n' "$excerpt" | sed 's/%/%25/g; s/\r//g; s/::/%3A%3A/g' \
        | awk 'BEGIN { ORS = "%0A" } { print }')
    echo "::error title=Widget instrumentation failure::$escaped"
    exit 1
fi

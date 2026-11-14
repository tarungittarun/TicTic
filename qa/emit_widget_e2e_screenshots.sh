#!/usr/bin/env bash
# Surface intact emulator JPEGs as GitHub check annotations. Filenames carry the
# stable 01..24 test ordinal; never renumber a partial capture set by array position.
set -Eeuo pipefail

readonly SCREEN_DIR="${WIDGET_E2E_SCREEN_DIR:-/tmp/widget-e2e-screenshots}"
start=${1:?Usage: emit_widget_e2e_screenshots.sh ZERO_BASED_START COUNT}
count=${2:?Usage: emit_widget_e2e_screenshots.sh ZERO_BASED_START COUNT}
if [[ ! "$start" =~ ^[0-9]+$ || ! "$count" =~ ^[1-9][0-9]*$ ]]; then
    echo "START must be zero-based and COUNT must be positive."
    exit 2
fi

readonly expected=(
    01_alarm_countdown.jpg 02_alarm_stack.jpg 03_wake_switch.jpg 04_quick_alarm.jpg
    05_world_grid.jpg 06_meeting_window.jpg 07_week_strip.jpg 08_month_grid.jpg
    09_year_pulse.jpg 10_day_balance.jpg 11_routine_stack.jpg 12_routine_countdown.jpg
    13_focus_launch.jpg 14_timer_live.jpg 15_lap_insights.jpg 16_month_grid_compact_week.jpg
    17_wake_switch_disabled_after_tap.jpg 18_timer_live_running_after_focus_launch.jpg
    19_timer_live_paused_after_tap.jpg 20_quick_alarm_save_preflight.jpg
    21_widget_catalog_new_concepts.jpg 22_quick_alarm_pin_confirmation.jpg
    23_quick_alarm_widget_studio.jpg 24_quick_alarm_widget_placed.jpg
)
if (( start >= ${#expected[@]} )); then
    echo "START is outside the 24-capture evidence set."
    exit 2
fi
if [[ ! -d "$SCREEN_DIR" ]]; then
    echo "No emulator screenshot directory was exported; nothing to surface."
    exit 0
fi

# Reject unexpected names rather than publishing an annotation whose screenshot
# cannot be mapped back to a specific expected journey/capture.
for path in "$SCREEN_DIR"/*.jpg; do
    [[ -e "$path" ]] || continue
    name=${path##*/}
    known=false
    for expected_name in "${expected[@]}"; do
        if [[ "$name" == "$expected_name" ]]; then known=true; break; fi
    done
    if [[ "$known" != true ]]; then
        echo "Unexpected emulator screenshot filename: $name" >&2
        exit 1
    fi
done

end=$((start + count))
if (( end > ${#expected[@]} )); then end=${#expected[@]}; fi
emitted=0
for ((index = start; index < end; index++)); do
    name=${expected[$index]}
    path="$SCREEN_DIR/$name"
    [[ -f "$path" ]] || continue
    if [[ ! -s "$path" ]]; then
        echo "Emulator screenshot is empty: $name" >&2
        exit 1
    fi
    payload=$(base64 -w0 "$path")
    payload_length=${#payload}
    if (( payload_length > 3800 )); then
        echo "Screenshot $name is too large for an intact GitHub check annotation ($payload_length base64 characters)." >&2
        exit 1
    fi
    ordinal=${name%%_*}
    printf '::notice title=E2E screenshot %s/24 %s::%s\n' \
        "$ordinal" "$name" "$payload"
    emitted=$((emitted + 1))
done
if (( emitted == 0 )); then
    echo "No captures in requested expected range $((start + 1))..$end; filenames retain their 01..24 ordinals."
fi

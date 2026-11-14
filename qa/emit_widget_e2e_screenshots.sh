#!/usr/bin/env bash
# Surface a slice of emulator JPEGs as intact GitHub check annotations. The caller
# divides the 24 expected captures into three groups of eight to respect per-step caps.
set -Eeuo pipefail

readonly SCREEN_DIR="${WIDGET_E2E_SCREEN_DIR:-/tmp/widget-e2e-screenshots}"
start=${1:?Usage: emit_widget_e2e_screenshots.sh START COUNT}
count=${2:?Usage: emit_widget_e2e_screenshots.sh START COUNT}
if [[ ! "$start" =~ ^[0-9]+$ || ! "$count" =~ ^[1-9][0-9]*$ ]]; then
    echo "START must be zero-based and COUNT must be positive."
    exit 2
fi
if [[ ! -d "$SCREEN_DIR" ]]; then
    echo "No emulator screenshot directory was exported; nothing to surface."
    exit 0
fi

mapfile -t images < <(find "$SCREEN_DIR" -maxdepth 1 -type f -name '*.jpg' -printf '%f\n' | sort -V)
total=${#images[@]}
if (( start >= total )); then
    echo "No screenshots in requested range $start..$((start + count - 1)); exported total: $total."
    exit 0
fi

end=$((start + count))
if (( end > total )); then end=$total; fi
for ((index = start; index < end; index++)); do
    name=${images[$index]}
    payload=$(base64 -w0 "$SCREEN_DIR/$name")
    payload_length=${#payload}
    if (( payload_length > 3800 )); then
        echo "Screenshot $name is too large for an intact GitHub check annotation ($payload_length base64 characters)."
        exit 1
    fi
    printf '::notice title=E2E screenshot %02d/%02d %s::%s\n' \
        "$((index + 1))" "$total" "$name" "$payload"
done

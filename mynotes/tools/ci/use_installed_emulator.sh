#!/usr/bin/env bash
# Provisioning only. No test retries, no emulator state restoration, no assertion changes.
set -euo pipefail
sdk="${ANDROID_HOME:?ANDROID_HOME required}"
manager="$sdk/cmdline-tools/latest/bin/sdkmanager"
emulator="$sdk/emulator/emulator"
if [[ ! -x "$manager" || ! -x "$emulator" ]]; then
  echo 'No preinstalled emulator; retain normal SDK installation.'
  exit 0
fi
if ! timeout 20s "$emulator" -version > emulator-preflight.txt 2>&1; then
  echo 'Preinstalled emulator failed preflight; retain normal SDK installation.'
  cat emulator-preflight.txt
  exit 0
fi
sha256sum "$emulator" >> emulator-preflight.txt
cat emulator-preflight.txt
# Keep sdkmanager in its own directory so its relative library lookup still works.
[[ ! -e "$manager.recognition-original" ]] || { echo 'sdkmanager already wrapped' >&2; exit 1; }
mv "$manager" "$manager.recognition-original"
cat > "$manager" <<'WRAPPER'
#!/usr/bin/env bash
set -euo pipefail
sdk="${ANDROID_HOME:?}"
if [[ "$#" == 3 && "$1" == '--install' && "$2" == 'emulator' && "$3" == '--channel=0' ]]; then
  # Revalidate immediately before using the preinstalled binary. Do not suppress
  # a failed preflight: in that case use the normal SDK installer instead.
  if timeout 20s "$sdk/emulator/emulator" -version; then
    echo 'Using runner-provided emulator verified by preflight; skipping latest upgrade.'
    exit 0
  fi
fi
exec "$sdk/cmdline-tools/latest/bin/sdkmanager.recognition-original" "$@"
WRAPPER
chmod +x "$manager"

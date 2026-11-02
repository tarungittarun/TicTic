"""Normalize real emulator rotation and retain system-window/ANR diagnostics.

Nexus 9's natural landscape orientation must not be confused with a phone's.
Only the external Pixel Launcher may be stopped; never suppress app ANR dialogs.
"""
from __future__ import annotations
import argparse
import json
import re
import subprocess
import time
from pathlib import Path


def physical_size(output: str) -> tuple[int, int]:
    match = re.search(r"Physical size:\s*(\d+)x(\d+)", output)
    if not match or min(map(int, match.groups())) < 1:
        raise ValueError("Cannot establish natural physical display size")
    return tuple(map(int, match.groups()))


def rotation_for(size: tuple[int, int], orientation: str) -> int:
    if orientation not in ("portrait", "landscape"):
        raise ValueError("Explicit portrait or landscape is required")
    return int((size[0] > size[1]) != (orientation == "landscape"))


def adb(*args: str, required: bool = True) -> str:
    result = subprocess.run(["adb", "shell", *args], text=True, capture_output=True, timeout=30)
    if required and result.returncode:
        raise RuntimeError("adb " + " ".join(args) + ": " + result.stderr)
    return result.stdout + result.stderr


def snapshot(stage: str) -> None:
    # Artifact only: synthetic test device, not a product diagnostics feature.
    sections = {name: adb(*command, required=False) for name, command in {
        "size": ("wm", "size"), "density": ("wm", "density"),
        "windows": ("dumpsys", "window"),
        "last_anr": ("dumpsys", "activity", "lastanr"),
        "activity": ("dumpsys", "activity", "activities"),
    }.items()}
    Path("refinement-environment-" + stage + ".txt").write_text(
        "\n\n".join(name + "\n" + value for name, value in sections.items()))
    summary = {"stage": stage, "physical_size": sections["size"].strip(),
               "density": sections["density"].strip(),
               "focused_windows": [line.strip() for line in sections["windows"].splitlines()
                                   if "mCurrentFocus=" in line or "mFocusedApp=" in line],
               "anr_header": sections["last_anr"][:2000]}
    Path("refinement-environment-" + stage + ".json").write_text(json.dumps(summary, indent=2))


def settle_home() -> None:
    """Resolve the external launcher's stopped/transitioning window before app tests.

    Do not launch/force-stop the app, dismiss application errors or ignore failed focus.
    Starting HOME is normal emulator preparation; source fixtures still require app focus.
    """
    adb("am", "start", "-W", "-n", "com.google.android.apps.nexuslauncher/.NexusLauncherActivity")
    stable = 0
    for attempt in range(60):
        windows = adb("dumpsys", "window", "windows")
        focused = [line for line in windows.splitlines() if "mCurrentFocus=Window{" in line]
        ready = any("com.google.android.apps.nexuslauncher/" in line and "NexusLauncherActivity" in line for line in focused)
        stable = stable + 1 if ready else 0
        if stable >= 3:
            return
        time.sleep(.25)
    snapshot("home-failed")
    raise RuntimeError("External emulator HOME never obtained a stable focused window; do not run app fixtures against a black transitioning launcher")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--orientation", choices=("portrait", "landscape"))
    parser.add_argument("--stop-pixel-launcher", action="store_true")
    parser.add_argument("--settle-home", action="store_true")
    parser.add_argument("--snapshot", choices=("before", "configured", "after"))
    args = parser.parse_args()
    if args.snapshot:
        snapshot(args.snapshot)
    if args.orientation:
        size = physical_size(adb("wm", "size"))
        rotation = rotation_for(size, args.orientation)
        api = int(adb("getprop", "ro.build.version.sdk").strip())
        adb("settings", "put", "system", "accelerometer_rotation", "0")
        adb("settings", "put", "system", "user_rotation", str(rotation))
        if api >= 31:
            adb("wm", "set-ignore-orientation-request", "true")
            adb("wm", "fixed-to-user-rotation", "enabled")
            adb("wm", "user-rotation", "lock", str(rotation))
        print(f"Natural display {size}; requested {args.orientation}; rotation={rotation}")
    if args.stop_pixel_launcher:
        print("Stopping external Pixel Launcher after retaining preflight ANR/window diagnostics; app ANRs remain visible.")
        adb("am", "force-stop", "com.google.android.apps.nexuslauncher")
    if args.settle_home:
        settle_home()


if __name__ == "__main__":
    main()

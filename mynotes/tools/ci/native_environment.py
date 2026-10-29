"""Normalize real emulator rotation and retain system-window/ANR diagnostics.

Nexus 9's natural landscape orientation must not be confused with a phone's.
Only the external Pixel Launcher may be stopped; never suppress app ANR dialogs.
"""
from __future__ import annotations
import argparse
import json
import re
import subprocess
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


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--orientation", choices=("portrait", "landscape"))
    parser.add_argument("--stop-pixel-launcher", action="store_true")
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


if __name__ == "__main__":
    main()

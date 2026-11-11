"""Bounded post-comparison cold-start replay for the observed emulator-loss window.

This is a host/emulator reliability diagnostic, not an app acceptance test. It
repeats the existing X33 three-launch sequence on the same guest, first for
MyNotes and then for Obsidian, and checks both QEMU PID and Android boot-ID
continuity. It does not edit or delete fixture content.
"""
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time

import paired_ux_session as h
from obsidian_cdp import Device

OUT = Path("comparison37-evidence")
CAPTURE_OUT = OUT / "guest-loss-replay-captures"
APPS = [("mynotes", "app.space.notes"), ("obsidian", "md.obsidian")]
MAX_CYCLES = 30
START_RE = re.compile(r"TotalTime:\s*(\d+)")


def now():
    return datetime.now(timezone.utc).isoformat()


def qemu_pids():
    result = subprocess.run(["ps", "-eo", "pid=,uid=,comm="], capture_output=True,
                            text=True, timeout=5)
    if result.returncode:
        return []
    return sorted(int(line.split()[0]) for line in result.stdout.splitlines()
                  if len(line.split()) == 3 and line.split()[0].isdigit()
                  and int(line.split()[1]) == os.getuid()
                  and line.split()[2].startswith("qemu-system"))


def guest_identity_change(initial_qemu_pids, current_qemu_pids, initial_boot_id, current_boot_id):
    if initial_qemu_pids != current_qemu_pids:
        return "qemu_pid_change"
    if initial_boot_id != current_boot_id:
        return "android_guest_reboot"
    return None


def resolve_activity(package):
    output = h.adb("shell", "cmd", "package", "resolve-activity", "--brief", "-c",
                    "android.intent.category.LAUNCHER", package, timeout=15).decode(errors="replace")
    components = [line.strip() for line in output.splitlines() if "/" in line]
    if not components:
        raise RuntimeError("No launcher activity resolved for pinned app package")
    return components[-1]


def launch_group(app, package, cycle, events):
    component = resolve_activity(package)
    samples = []
    for repeat in range(1, 4):
        event = {"app": app, "cycle": cycle, "repeat": repeat,
                 "started_at": now(), "stage": "force_stop"}
        events.append(event)
        h.adb("shell", "am", "force-stop", package, timeout=15)
        event["stage"] = "cold_start"
        report = h.adb("shell", "am", "start", "-W", "-n", component,
                       timeout=35).decode(errors="replace")
        match = START_RE.search(report)
        event["android_total_time_ms"] = int(match.group(1)) if match else None
        event["launch_state"] = "cold" if "LaunchState: COLD" in report else "reported"
        event["finished_at"] = now()
        samples.append({key: event[key] for key in ("repeat", "android_total_time_ms", "launch_state")})
        time.sleep(4)
    # Keep replay captures separate from the paired-module session files; a
    # focused replay must never overwrite the original comparison transcript.
    h.COMMANDS.clear()
    h.CASES.clear()
    h.SNAPSHOTS.clear()
    h.capture(app, f"REPLAY-{cycle:02d}-{app}")
    capture = h.SNAPSHOTS[-1]
    capture_prefix = CAPTURE_OUT.relative_to(OUT)
    return {"app": app, "cycle": cycle, "samples": samples,
            "capture": {"id": capture["id"],
                        "image": str(capture_prefix / capture["image"]),
                        "accessibility_tree": str(capture_prefix / capture["tree"])},
            "requested_task_acceptance": False}


def main():
    # Keep replay artifacts and helper transcripts separate from the paired
    # module comparison; no source evidence file should be overwritten.
    OUT.mkdir(parents=True, exist_ok=True)
    CAPTURE_OUT.mkdir(parents=True, exist_ok=True)
    h.OUT = CAPTURE_OUT
    h.COMMANDS.clear()
    h.CASES.clear()
    h.SNAPSHOTS.clear()
    try:
        cycles = int(os.environ.get("COMPARISON_GUEST_LOSS_REPLAY_CYCLES", "20"))
    except ValueError as error:
        raise ValueError("Replay cycle count must be an integer") from error
    if not 1 <= cycles <= MAX_CYCLES:
        raise ValueError(f"Replay cycle count must be in [1, {MAX_CYCLES}]")

    started = time.monotonic()
    result = {
        "schema": 1,
        "scope": "Bounded QEMU/ADB cold-start reliability replay of the prior X33 sequence; not app acceptance.",
        "source_run": "42617793054",
        "selected_range": "31-37",
        "cycle_count_requested": cycles,
        "cold_starts_per_app": cycles * 3,
        "app_order_per_cycle": [app for app, _ in APPS],
        "whole_module_acceptance": False,
        "requested_task_acceptance": False,
        "physical_acceptance": False,
        "qemu_pid_change_observed": False,
        "guest_reboot_observed": False,
        "guest_boot_id_stable": None,
        "samples": [],
        "events": [],
        "qemu_pids_at_start": qemu_pids(),
        "started_at": now(),
    }
    observer = None
    try:
        state = h.adb("get-state", timeout=10).decode(errors="replace").strip()
        if state != "device":
            raise RuntimeError("Android guest not ready before replay")
        if len(result["qemu_pids_at_start"]) != 1:
            raise RuntimeError("Could not establish exactly one owned QEMU process at replay start")
        guest_boot_id = h.adb("shell", "cat", "/proc/sys/kernel/random/boot_id",
                              timeout=10).decode(errors="replace").strip()
        if not re.fullmatch(r"[0-9a-f-]{36}", guest_boot_id):
            raise RuntimeError("Could not establish a valid Android guest boot ID")
        result["guest_boot_id_sha256"] = hashlib.sha256(guest_boot_id.encode()).hexdigest()
        # Match X33's observer lifecycle: keep Obsidian's official WebView
        # observer available through the MyNotes control, close it before each
        # Obsidian force-stop/restart group, then reacquire after startup.
        observer = Device()
        if not observer.js("!!window.app?.vault"):
            raise RuntimeError("Official Obsidian vault/WebView observer not established")

        for cycle in range(1, cycles + 1):
            for app, package in APPS:
                if app == "obsidian" and observer is not None:
                    observer.ws.close()
                    observer = None
                sample = launch_group(app, package, cycle, result["events"])
                result["samples"].append(sample)
                result["completed_groups"] = len(result["samples"])
                if app == "obsidian":
                    observer = Device()
                    if not observer.js("!!window.app?.vault"):
                        raise RuntimeError("Obsidian observer did not reconnect after cold starts")
                result["current_qemu_pids"] = qemu_pids()
                if result["current_qemu_pids"] != result["qemu_pids_at_start"]:
                    identity_change = "qemu_pid_change"
                else:
                    current_boot_id = h.adb("shell", "cat", "/proc/sys/kernel/random/boot_id",
                                            timeout=10).decode(errors="replace").strip()
                    identity_change = guest_identity_change(
                        result["qemu_pids_at_start"], result["current_qemu_pids"],
                        guest_boot_id, current_boot_id)
                if identity_change == "qemu_pid_change":
                    result["qemu_pid_change_observed"] = True
                    raise RuntimeError("Owned QEMU PID set changed during the replay")
                result["guest_boot_id_stable"] = identity_change is None
                if identity_change == "android_guest_reboot":
                    result["guest_reboot_observed"] = True
                    raise RuntimeError("Android guest boot ID changed during the replay")
                (OUT / "guest-loss-lifecycle-replay.json").write_text(json.dumps(result, indent=2))

        result["status"] = "completed_no_guest_loss_observed"
    except Exception as error:
        result["status"] = "replay_interrupted"
        result["error_type"] = type(error).__name__
        result["error_excerpt"] = str(error)[:500]
        result["current_qemu_pids"] = qemu_pids()
        (OUT / "guest-loss-lifecycle-replay.json").write_text(json.dumps(result, indent=2))
        raise
    finally:
        if observer is not None:
            try:
                observer.ws.close()
            except Exception:
                pass
        result["finished_at"] = now()
        result["wall_elapsed_seconds"] = round(time.monotonic() - started, 2)
        result["raw_memory_or_environment_collected"] = False
        (OUT / "guest-loss-lifecycle-replay.json").write_text(json.dumps(result, indent=2))

    print("GUEST-LOSS REPLAY", json.dumps({
        "status": result["status"],
        "cycles": result["cycle_count_requested"],
        "completed_groups": result.get("completed_groups", 0),
        "cold_starts_per_app": result["cold_starts_per_app"],
        "wall_elapsed_seconds": result["wall_elapsed_seconds"],
        "qemu_pid_change_observed": result["qemu_pid_change_observed"],
        "guest_reboot_observed": result["guest_reboot_observed"],
        "guest_boot_id_stable": result["guest_boot_id_stable"],
        "whole_module_acceptance": False,
    }, sort_keys=True), flush=True)


if __name__ == "__main__":
    main()

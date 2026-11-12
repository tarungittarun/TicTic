"""Replay evidence stays separate and does not imply requested-task acceptance."""
import importlib.util
from pathlib import Path
import subprocess
import sys
import types
import unittest
from unittest.mock import Mock, patch


MODULE_PATH = Path("mynotes/tools/comparisons/guest_loss_lifecycle_replay.py")


def load_module(helper):
    cdp = types.ModuleType("obsidian_cdp")
    cdp.Device = object
    spec = importlib.util.spec_from_file_location("guest_loss_replay_test", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    with patch.dict(sys.modules, {"paired_ux_session": helper, "obsidian_cdp": cdp}):
        spec.loader.exec_module(module)
    return module


class GuestLossReplayTests(unittest.TestCase):
    def test_guest_identity_detects_host_process_replacement_and_android_reboot(self):
        helper = types.ModuleType("paired_ux_session")
        module = load_module(helper)
        self.assertIsNone(module.guest_identity_change([123], [123], "boot-a", "boot-a"))
        self.assertEqual(module.guest_identity_change([123], [124], "boot-a", "boot-a"),
                         "qemu_pid_change")
        self.assertEqual(module.guest_identity_change([123], [123], "boot-a", "boot-b"),
                         "android_guest_reboot")

    def test_qemu_pid_scan_excludes_zombie_and_dead_processes(self):
        helper = types.ModuleType("paired_ux_session")
        module = load_module(helper)
        ps = subprocess.CompletedProcess([], 0, """41 1001 qemu-system-x86_64 Ssl
42 1001 qemu-system-x86_64 Zl
43 1001 qemu-system-x86_64 X
44 1002 qemu-system-x86_64 S
45 1001 adb S
""", "")
        with patch.object(module.os, "getuid", return_value=1001), \
                patch.object(module.subprocess, "run", return_value=ps):
            self.assertEqual(module.qemu_pids(), [41])

    def test_qemu_process_scan_failure_is_unknown_not_a_guest_loss(self):
        helper = types.ModuleType("paired_ux_session")
        module = load_module(helper)
        result = {"qemu_pids_at_start": [123], "qemu_pid_change_observed": False,
                  "guest_boot_id_stable": True, "guest_loss_observed": False}
        with patch.object(module, "qemu_pids", return_value=None):
            change = module.record_guest_identity(result, "boot-a")
        self.assertEqual(change, "qemu_process_scan_failed")
        self.assertIsNone(result["qemu_pid_change_observed"])
        self.assertIsNone(result["guest_boot_id_stable"])
        self.assertFalse(result["guest_loss_observed"])

    def test_missing_or_zombie_qemu_is_recorded_as_guest_loss_without_stale_boot_claim(self):
        helper = types.ModuleType("paired_ux_session")
        helper.adb = Mock(side_effect=AssertionError("ADB must not be queried after QEMU is gone"))
        module = load_module(helper)
        result = {"qemu_pids_at_start": [3777], "qemu_pid_change_observed": False,
                  "guest_boot_id_stable": True, "guest_loss_observed": False}

        change = module.record_guest_identity(result, "boot-a", [])

        self.assertEqual(change, "qemu_pid_change")
        self.assertEqual(result["current_qemu_pids"], [])
        self.assertTrue(result["qemu_pid_change_observed"])
        self.assertTrue(result["guest_loss_observed"])
        self.assertIsNone(result["guest_boot_id_stable"])
        helper.adb.assert_not_called()

    def test_unreachable_guest_does_not_claim_the_previous_boot_id_is_still_stable(self):
        helper = types.ModuleType("paired_ux_session")
        helper.adb = Mock(side_effect=RuntimeError("device offline"))
        module = load_module(helper)
        result = {"qemu_pids_at_start": [123], "qemu_pid_change_observed": False,
                  "guest_boot_id_stable": True, "guest_loss_observed": False}

        change = module.record_guest_identity(result, "boot-a", [123])

        self.assertEqual(change, "guest_identity_unavailable")
        self.assertEqual(result["guest_identity_check_error_type"], "RuntimeError")
        self.assertIsNone(result["guest_boot_id_stable"])
        self.assertFalse(result["guest_loss_observed"])

    def test_cycle_capture_is_addressable_and_isolated_from_paired_session(self):
        helper = types.ModuleType("paired_ux_session")
        helper.COMMANDS = ["prior comparison command"]
        helper.CASES = ["prior comparison case"]
        helper.SNAPSHOTS = [{"id": "prior comparison image"}]

        def adb(*args, **_kwargs):
            if args[:4] == ("shell", "cmd", "package", "resolve-activity"):
                return b"com.example/.MainActivity\n"
            if args[:3] == ("shell", "am", "start"):
                return b"LaunchState: COLD\nTotalTime: 123\n"
            return b""

        def capture(app, label):
            helper.SNAPSHOTS.append({
                "id": f"{app}-{label}",
                "image": f"{app}-{label}.png",
                "tree": f"{app}-{label}.xml",
            })

        helper.adb = adb
        helper.capture = capture
        module = load_module(helper)
        with patch.object(module.time, "sleep"):
            result = module.launch_group("mynotes", "app.space.notes", 1, [])

        self.assertEqual(len(result["samples"]), 3)
        self.assertEqual([item["android_total_time_ms"] for item in result["samples"]], [123, 123, 123])
        self.assertEqual(result["capture"]["id"], "mynotes-REPLAY-01-mynotes")
        self.assertTrue(result["capture"]["image"].startswith("guest-loss-replay-captures/"))
        self.assertTrue(result["capture"]["accessibility_tree"].endswith(".xml"))
        self.assertEqual(helper.COMMANDS, [])
        self.assertEqual(helper.CASES, [])
        self.assertEqual(len(helper.SNAPSHOTS), 1)
        self.assertFalse(result["requested_task_acceptance"])


if __name__ == "__main__":
    unittest.main()

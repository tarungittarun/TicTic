"""Replay evidence stays separate and does not imply requested-task acceptance."""
import importlib.util
from pathlib import Path
import sys
import types
import unittest
from unittest.mock import patch


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

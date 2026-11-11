"""Apport triage reads only bounded QEMU text metadata, never core contents."""
import importlib.util
from pathlib import Path
import tempfile
import unittest


MODULE_PATH = Path("mynotes/tools/comparisons/apport_crash_metadata.py")


def load_module():
    spec = importlib.util.spec_from_file_location("apport_crash_metadata", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class ApportCrashMetadataTests(unittest.TestCase):
    def test_extracts_qemu_signal_and_sanitized_stack_before_core_attachment(self):
        module = load_module()
        report = (
            b"ProblemType: Crash\n"
            b"ExecutablePath: /usr/local/lib/android/sdk/emulator/qemu/linux-x86_64/qemu-system-x86_64\n"
            b"Signal: 11\n"
            b"StacktraceTop:\n"
            b" #0 0x7fff12345678 in gfxstream_fault () from /home/runner/private/libgfxstream.so\n"
            b" #1 0x7fff23456789 in renderer_shutdown ()\n"
            b"CoreDump: base64\n"
            b"TOKEN_SENTINEL_MUST_NOT_BE_READ\n"
        )
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "qemu.crash"
            path.write_bytes(report)
            result = module.parse_report(path, root)
        self.assertEqual(result["executable_basename"], "qemu-system-x86_64")
        self.assertEqual(result["signal_number"], "11")
        self.assertEqual(len(result["stacktrace_top"]), 2)
        self.assertIn("libgfxstream.so", result["stacktrace_top"][0])
        self.assertNotIn("/home/runner", str(result))
        joined_stack = " ".join(result["stacktrace_top"])
        self.assertIn("<address>", joined_stack)
        self.assertNotRegex(joined_stack, r"0x[0-9a-fA-F]{4,}")
        self.assertNotIn("TOKEN_SENTINEL", str(result))
        self.assertFalse(result["raw_core_read"])
        self.assertFalse(result["raw_report_retained"])

    def test_ignores_non_qemu_reports_and_does_not_follow_symlinks(self):
        module = load_module()
        with tempfile.TemporaryDirectory() as directory, tempfile.TemporaryDirectory() as outside:
            root = Path(directory)
            other = root / "other.crash"
            other.write_text("ExecutablePath: /usr/bin/notes\nSignal: 11\n")
            outside_report = Path(outside) / "qemu.crash"
            outside_report.write_text("ExecutablePath: /usr/bin/qemu-system-x86_64\nSignal: 11\n")
            link = root / "linked.crash"
            link.symlink_to(outside_report)
            self.assertIsNone(module.parse_report(other, root))
            self.assertIsNone(module.parse_report(link, root))
            result = module.collect(root)
            self.assertEqual(result["qemu_report_count"], 0)
            self.assertFalse(result["raw_reports_retained"])

    def test_wait_is_bounded_and_returns_no_crash_when_no_report_exists(self):
        module = load_module()
        with tempfile.TemporaryDirectory() as directory:
            result = module.wait_for_reports(Path(directory), timeout_seconds=0)
        self.assertFalse(result["wait_attempted"])
        self.assertEqual(result["waited_seconds"], 0)
        self.assertEqual(result["qemu_report_count"], 0)
        self.assertFalse(result["raw_cores_read"])

    def test_rejects_an_unbounded_wait_limit(self):
        from unittest.mock import patch
        module = load_module()
        with tempfile.TemporaryDirectory() as directory, \
                patch.object(module.time, "monotonic", side_effect=[0, 181, 181]), \
                patch.object(module.time, "sleep") as sleep:
            result = module.wait_for_reports(Path(directory), timeout_seconds=999, poll_seconds=99)
        self.assertEqual(result["wait_limit_seconds"], 180)
        self.assertEqual(result["waited_seconds"], 180)
        sleep.assert_not_called()

    def test_wait_reads_only_a_stable_bounded_qemu_report(self):
        from unittest.mock import patch
        module = load_module()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "qemu.crash"
            path.write_bytes(
                b"ProblemType: Crash\n"
                b"ExecutablePath: /usr/bin/qemu-system-x86_64\n"
                b"Signal: 11\n"
                b"StacktraceTop:\n"
                b" #0 renderer_shutdown ()\n"
                b"CoreDump: never-read\n"
            )
            with patch.object(module.time, "monotonic", side_effect=[0, 0, 0.1, 0.2]), \
                    patch.object(module.time, "sleep") as sleep:
                result = module.wait_for_reports(root, timeout_seconds=1, poll_seconds=0.1)
        self.assertEqual(result["qemu_report_count"], 1)
        self.assertEqual(result["candidate_report_count"], 1)
        self.assertEqual(result["reports"][0]["signal_number"], "11")
        self.assertEqual(sleep.call_count, 2)
        self.assertFalse(result["raw_cores_read"])


if __name__ == "__main__":
    unittest.main()

"""Host diagnostics distinguish QEMU exit from zombie and live processes."""
import importlib.util
from pathlib import Path
import tempfile
import unittest


MODULE_PATH = Path("mynotes/tools/comparisons/run_with_host_diagnostics.py")


def load_module():
    spec = importlib.util.spec_from_file_location("host_diagnostics", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class QemuProcessStatusTests(unittest.TestCase):
    def test_identifies_running_and_zombie_qemu_even_when_comm_has_parentheses(self):
        module = load_module()
        with tempfile.TemporaryDirectory() as directory:
            process = Path(directory) / "123"
            process.mkdir()
            (process / "comm").write_text("qemu-system-x86_64\n")
            stat = process / "stat"
            stat.write_text("123 (qemu-system-x86_64 (worker)) R 1 2 3\n")
            self.assertEqual(module.qemu_process_status(123, directory), {
                "state": "running", "running": True, "terminated": False
            })
            stat.write_text("123 (qemu-system-x86_64 (worker)) Z 1 2 3\n")
            self.assertEqual(module.qemu_process_status(123, directory), {
                "state": "zombie", "running": False, "terminated": True
            })

    def test_missing_or_reused_pid_is_not_reported_as_live_qemu(self):
        module = load_module()
        with tempfile.TemporaryDirectory() as directory:
            self.assertTrue(module.qemu_process_status(321, directory)["terminated"])
            process = Path(directory) / "123"
            process.mkdir()
            (process / "comm").write_text("python\n")
            (process / "stat").write_text("123 (python) S 1 2 3\n")
            status = module.qemu_process_status(123, directory)
        self.assertEqual(status["state"], "different_process")
        self.assertTrue(status["terminated"])

    def test_rejects_invalid_pid_values(self):
        module = load_module()
        for pid in (None, 0, -1, True, "123"):
            with self.subTest(pid=pid):
                status = module.qemu_process_status(pid)
                self.assertFalse(status["running"])
                self.assertFalse(status["terminated"])


if __name__ == "__main__":
    unittest.main()

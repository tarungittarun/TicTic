"""Emulator-runner lines are separate shells; diagnostics must share a process."""
import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch
import yaml

class HostFinallyTest(unittest.TestCase):
    def module(self):
        spec=importlib.util.spec_from_file_location('host_finally',Path('mynotes/tools/comparisons/run_with_host_diagnostics.py'))
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module

    def test_original_failure_is_not_masked_and_host_collection_still_runs(self):
        module=self.module()
        for error in [RuntimeError('dummy guest unavailable'),SystemExit(7)]:
            with patch.object(module.runpy,'run_path',side_effect=error),patch.object(module,'collect_host') as collect,patch.object(module.traceback,'print_exc'):
                self.assertEqual(module.main(),7 if isinstance(error,SystemExit) else 1)
                collect.assert_called_once_with(Path('comparison37-evidence'))

    def test_success_collects_host_without_changing_original_result(self):
        module=self.module()
        with patch.object(module.runpy,'run_path'),patch.object(module,'collect_host') as collect:
            self.assertEqual(module.main(),0);collect.assert_called_once()

    def test_emulator_script_does_not_depend_on_cross_shell_variables_or_heredoc(self):
        workflow=yaml.safe_load(Path('.github/workflows/notes-comparison37-capture-isolation.yml').read_text())
        step=next(s for s in workflow['jobs']['compare']['steps'] if s.get('uses','').startswith('ReactiveCircus/'))
        script=step['with']['script']
        self.assertIn('python mynotes/tools/comparisons/run_with_host_diagnostics.py',script)
        self.assertNotIn('<<',script);self.assertNotIn('result=',script)

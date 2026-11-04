import unittest
from pathlib import Path
import yaml

class LandscapeRecoveryPythonContext(unittest.TestCase):
    def test_shell_python_has_workspace_import_context_before_any_native_run(self):
        data=yaml.safe_load(Path('.github/workflows/notes-candidate67-landscape-setup-supplement.yml').read_text())
        steps=data['jobs']['missing_native']['steps']
        step=next(s for s in steps if s.get('shell')=='python')
        self.assertEqual(step['env']['PYTHONPATH'],'${{ github.workspace }}')
        self.assertIn("ModuleNotFoundError: No module named 'mynotes'",step['run'])
        self.assertIn("b'MODULE 01/' not in prior",step['run'])

    def test_device_setup_does_not_use_the_stalled_power_service_command(self):
        data=yaml.safe_load(Path('.github/workflows/notes-candidate67-landscape-setup-supplement.yml').read_text())
        script=next(s['with']['script'] for s in data['jobs']['missing_native']['steps'] if s.get('uses','').startswith('ReactiveCircus/'))
        self.assertNotIn('svc power stayon',script)
        self.assertIn('timeout 30s adb shell settings put global stay_on_while_plugged_in 3',script)
        for line in script.splitlines():
            if 'adb ' in line:self.assertTrue(line.strip().startswith('timeout '),line)

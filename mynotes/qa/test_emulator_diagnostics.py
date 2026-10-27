from pathlib import Path
from types import SimpleNamespace
import subprocess
import tempfile
import unittest
from mynotes.tools.ci.adb_state import wait_uid
ROOT=Path(__file__).resolve().parents[2]
class EmulatorDiagnostics(unittest.TestCase):
    def test_restart_transport_is_not_mistaken_for_test_failure_or_root_success(self):
        replies=iter([SimpleNamespace(returncode=1,stdout='',stderr='closed'),SimpleNamespace(returncode=0,stdout='0\n',stderr=''),SimpleNamespace(returncode=0,stdout='2000\n',stderr='')]);ticks=iter(range(20))
        self.assertEqual(2000,wait_uid(2000,seconds=10,run=lambda *a,**k:next(replies),clock=lambda:next(ticks),sleep=lambda seconds:None))
    def test_root_never_accepted_when_ordinary_shell_required(self):
        ticks=iter(range(20))
        with self.assertRaisesRegex(RuntimeError,'required UID 2000'):
            wait_uid(2000,seconds=3,run=lambda *a,**k:SimpleNamespace(returncode=0,stdout='0\n',stderr=''),clock=lambda:next(ticks),sleep=lambda seconds:None)
    def test_bootstrap_observer_preserves_both_output_channels(self):
        observer=ROOT/'mynotes/tools/ci/emulator_observer.cjs'
        with tempfile.TemporaryDirectory() as d:
            completed=subprocess.run(['node','--require',str(observer),'-e','console.log("bootstrap probe");console.error("error probe")'],cwd=d,capture_output=True,text=True,check=True)
            self.assertIn('bootstrap probe',completed.stdout);self.assertIn('error probe',completed.stderr)
            observed=(Path(d)/'evidence/emulator-bootstrap.log').read_text();self.assertIn('bootstrap probe',observed);self.assertIn('error probe',observed)

    def test_landscape_uses_fixed_display_policy_and_actual_activity_assertions(self):
        script=(ROOT/'mynotes/tools/ci/android_lane.sh').read_text();self.assertIn('wm fixed-to-user-rotation enabled',script);self.assertIn('display-after-instrumentation.txt',script)
        test=(ROOT/'mynotes/app/src/androidTest/java/app/space/notes/EnvironmentQualificationTest.java').read_text();self.assertIn('getRealMetrics(metrics)',test);self.assertIn('wanted,actual.get()',test)

    def test_bootstrap_diagnostic_keeps_uppercase_panic_and_shutdown_context(self):
        from mynotes.tools.ci.results import build_diagnostic
        with tempfile.TemporaryDirectory() as d:
            log=Path(d)/'boot.log'
            log.write_text('runner setup\n'+('ordinary progress\n'*30)+'PANIC: Broken AVD\n'+('ordinary progress\n'*60)+'shutdown outcome\n')
            result=build_diagnostic(log)
            self.assertIn('runner setup',result);self.assertIn('PANIC: Broken AVD',result);self.assertIn('shutdown outcome',result)

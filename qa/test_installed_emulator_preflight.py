import os
import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT=Path(__file__).resolve().parents[1]/'mynotes/tools/ci/use_installed_emulator.sh'


class EmulatorPreflightTest(unittest.TestCase):
    def test_only_emulator_upgrade_is_skipped_after_successful_preflight(self):
        with tempfile.TemporaryDirectory() as folder:
            sdk=Path(folder)
            manager=sdk/'cmdline-tools/latest/bin/sdkmanager'
            emulator=sdk/'emulator/emulator'
            manager.parent.mkdir(parents=True);emulator.parent.mkdir()
            manager.write_text('#!/bin/sh\necho ORIGINAL "$@"\n');manager.chmod(0o755)
            emulator.write_text('#!/bin/sh\necho EMULATOR_VERSION\n');emulator.chmod(0o755)
            env={**os.environ,'ANDROID_HOME':folder}
            subprocess.run(['bash',str(SCRIPT)],cwd=folder,env=env,check=True,capture_output=True)
            for args,expected in [(['--install','emulator','--channel=0'],'Using runner-provided'),(['--install','platform-tools'],'ORIGINAL')]:
                result=subprocess.run([str(manager),*args],env=env,check=True,capture_output=True,text=True)
                self.assertIn(expected,result.stdout)
            emulator.write_text('#!/bin/sh\nexit 1\n')
            result=subprocess.run([str(manager),'--install','emulator','--channel=0'],env=env,check=True,capture_output=True,text=True)
            self.assertIn('ORIGINAL',result.stdout)

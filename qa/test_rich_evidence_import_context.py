import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import yaml

class RichEvidenceImportContext(unittest.TestCase):
    def test_temporary_python_shell_can_import_the_repository_probe_parser(self):
        doc=yaml.safe_load(Path('.github/workflows/notes-candidate66-rich-evidence.yml').read_text())
        step=next(x for x in doc['jobs']['retain']['steps'] if x.get('shell')=='python')
        self.assertEqual('.',step['env']['PYTHONPATH'])
        with tempfile.TemporaryDirectory() as directory:
            script=Path(directory)/'runner-step.py';script.write_text('from mynotes.tools.ci.release_rich_update_probe import METHOD\nprint(METHOD)\n')
            result=subprocess.run([sys.executable,str(script)],cwd=Path.cwd(),env={**os.environ,'PYTHONPATH':str(Path.cwd())},capture_output=True,text=True)
            self.assertEqual(0,result.returncode,result.stderr)
            self.assertIn('ReleaseRichMigrationProbe#',result.stdout)

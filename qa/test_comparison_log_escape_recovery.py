"""Recover original console bytes without replaying terminal control sequences."""
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

class LogEscapeRecoveryTest(unittest.TestCase):
    def test_retries_only_explicit_gh_escape_refusal_and_keeps_original_bytes(self):
        spec=importlib.util.spec_from_file_location('comparison_logs',Path('mynotes/tools/comparisons/collect_comparison_logs.py'))
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
        metadata={'databaseId':1,'headSha':'abc','status':'completed','url':'https://github.com/example','jobs':[{'databaseId':2,'name':'public dummy guest'}]}
        data=b'\x1b[31mKilled process emulator\x1b[0m\n'
        responses=[subprocess.CompletedProcess([],1,b'',b'the response contains terminal escape sequences; pass --allow-escape-sequences to output it anyway'),subprocess.CompletedProcess([],0,data,b'')]
        with tempfile.TemporaryDirectory() as directory,patch.object(module.subprocess,'check_output',return_value=json.dumps(metadata).encode()),patch.object(module.subprocess,'run',side_effect=responses) as run,patch('builtins.print') as out:
            records=module.collect(1,directory)
            self.assertEqual((Path(directory)/'2.txt').read_bytes(),data)
            self.assertTrue(records[0]['raw_escape_preserving_retry'])
            self.assertEqual(run.call_args_list[1].args[0][-1],'--allow-escape-sequences')
            self.assertNotIn('\x1b',str(out.call_args_list))

"""Native harness deadlines never turn missing, duplicate, or failed tests green."""
import ast
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from mynotes.tools.ci import run_ordered_modules as runner


class OrderedNativeDeadlines(unittest.TestCase):
    def preserved(self, rows):
        with tempfile.TemporaryDirectory() as directory:
            target=Path(directory)/'refinement-native.json'
            with patch.object(runner, 'Path', return_value=target):
                result=runner.preserve_partial(['A#one','B#two'], rows, {'first':['A#one'],'second':['B#two']})
            self.assertEqual(result,json.loads(target.read_text()))
            return result

    def row(self, module, cases, code=0, failures=None):
        return dict(module=module,cases=cases,process_exit=code,failures=failures or [],ok=not failures and code==0)

    def test_empty_inventory_execution_is_preserved_but_not_approved(self):
        self.assertFalse(self.preserved([])['ok'])

    def test_partial_assertions_survive_next_adb_stall_without_approval(self):
        result=self.preserved([self.row('first',['A#one'])])
        self.assertFalse(result['ok'])
        self.assertEqual(result['cases'],['A#one'])
        self.assertEqual(result['instrumentation_invocations'],1)

    def test_complete_once_can_pass(self):
        self.assertTrue(self.preserved([self.row('first',['A#one']),self.row('second',['B#two'])])['ok'])

    def test_nonzero_launcher_cannot_pass_even_with_complete_assertions(self):
        result=self.preserved([self.row('first',['A#one']),self.row('second',['B#two'],124)])
        self.assertFalse(result['ok'])
        self.assertIn('Module launcher failed: second',result['failures'])

    def test_asserted_failure_is_not_hidden(self):
        self.assertFalse(self.preserved([self.row('first',['A#one']),self.row('second',['B#two'],failures=['actual assertion'])])['ok'])

    def test_duplicate_assertion_is_not_complete_execution(self):
        self.assertFalse(self.preserved([self.row('first',['A#one']),self.row('second',['A#one'])])['ok'])

    def test_every_external_command_has_a_positive_bounded_deadline(self):
        tree=ast.parse(Path(runner.__file__).read_text())
        calls=[n for n in ast.walk(tree) if isinstance(n,ast.Call) and isinstance(n.func,ast.Attribute) and isinstance(n.func.value,ast.Name) and n.func.value.id=='subprocess' and n.func.attr=='run']
        self.assertEqual(len(calls),4)
        for call in calls:
            values=[k.value for k in call.keywords if k.arg=='timeout']
            self.assertEqual(len(values),1)
            self.assertTrue(0<ast.literal_eval(values[0])<=1650)

    def test_partial_failure_does_not_claim_full_instrumentation_count(self):
        self.assertEqual(self.preserved([])['instrumentation_invocations'],0)

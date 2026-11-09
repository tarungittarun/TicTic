"""CDP cleanup must not hide the original restart-sampling failure."""
import ast
import json
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import Mock

SOURCE = Path(__file__).resolve().parents[1] / 'mynotes/tools/comparisons/paired_modules37.py'


class DiagnosticReconnectTests(unittest.TestCase):
    def load(self, out):
        node = next(n for n in ast.parse(SOURCE.read_text()).body
                    if isinstance(n, ast.FunctionDef) and n.name == 'obsidian_launch_samples_reconnecting')
        old = SimpleNamespace(ws=Mock())
        fresh = object()
        sample = Mock(return_value={'diagnostics': 'not user task latency'})
        factory = Mock(return_value=fresh)
        namespace = {'D': old, 'Device': factory, 'OUT': out, 'json': json,
                     'h': SimpleNamespace(launch_samples=sample)}
        exec(compile(ast.Module(body=[node], type_ignores=[]), str(SOURCE), 'exec'), namespace)
        return namespace, old, fresh, sample, factory

    def test_success_reconnects_without_replacing_diagnostics(self):
        with tempfile.TemporaryDirectory() as tmp:
            ns, old, fresh, sample, factory = self.load(Path(tmp))
            self.assertIs(ns['obsidian_launch_samples_reconnecting'](), sample.return_value)
            old.ws.close.assert_called_once()
            factory.assert_called_once_with()
            self.assertIs(ns['D'], fresh)

    def test_failed_sample_still_reconnects_and_preserves_original_exception(self):
        with tempfile.TemporaryDirectory() as tmp:
            ns, _, fresh, sample, _ = self.load(Path(tmp))
            original = RuntimeError('UI automation diagnostic failed')
            sample.side_effect = original
            with self.assertRaises(RuntimeError) as raised:
                ns['obsidian_launch_samples_reconnecting']()
            self.assertIs(raised.exception, original)
            self.assertIs(ns['D'], fresh)

    def test_reconnect_failure_records_type_only_without_masking_sample_error(self):
        with tempfile.TemporaryDirectory() as tmp:
            ns, _, _, sample, factory = self.load(Path(tmp))
            original = ValueError('sample failed')
            sample.side_effect = original
            factory.side_effect = RuntimeError('private endpoint must not be copied')
            with self.assertRaises(ValueError) as raised:
                ns['obsidian_launch_samples_reconnecting']()
            self.assertIs(raised.exception, original)
            receipt = (Path(tmp) / 'obsidian-reconnect-after-diagnostic-error.json').read_text()
            self.assertEqual(json.loads(receipt), {'reconnected': False, 'error_type': 'RuntimeError'})
            self.assertNotIn('private endpoint', receipt)

    def test_receipt_write_failure_cannot_replace_original_exception(self):
        # A minimal path-like object that fails writing the receipt.
        class Unwritable:
            def __truediv__(self, name):
                return self
            def write_text(self, text):
                raise OSError('disk full')
        ns, _, _, sample, factory = self.load(Unwritable())
        original = ValueError('original sample error')
        sample.side_effect = original
        factory.side_effect = RuntimeError('reconnect failed')
        with self.assertRaises(ValueError) as raised:
            ns['obsidian_launch_samples_reconnecting']()
        self.assertIs(raised.exception, original)

    def test_close_failure_also_reconnects_without_starting_sample(self):
        with tempfile.TemporaryDirectory() as tmp:
            ns, old, fresh, sample, _ = self.load(Path(tmp))
            original = RuntimeError('close failed')
            old.ws.close.side_effect = original
            with self.assertRaises(RuntimeError) as raised:
                ns['obsidian_launch_samples_reconnecting']()
            self.assertIs(raised.exception, original)
            self.assertIs(ns['D'], fresh)
            sample.assert_not_called()

    def test_reconnect_failure_after_good_sample_is_not_success(self):
        with tempfile.TemporaryDirectory() as tmp:
            ns, _, _, sample, factory = self.load(Path(tmp))
            failure = RuntimeError('observer reconnect failed')
            factory.side_effect = failure
            with self.assertRaises(RuntimeError) as raised:
                ns['obsidian_launch_samples_reconnecting']()
            self.assertIs(raised.exception, failure)
            sample.assert_called_once_with('obsidian', 'md.obsidian')

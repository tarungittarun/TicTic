import unittest
from mynotes.tools.ci.audit_ocr61_history import FIELDS, pair


class HistoricalOcr61Test(unittest.TestCase):
    def row(self, recognized, cer):
        return {**{k: 'same' for k in FIELDS}, 'reference': 'hello', 'status': 'OK',
                'recognized': recognized, 'cer': cer, 'strict_wer': float(recognized != 'hello'),
                'accuracy_target_met': cer <= .2, 'elapsed_ms': 10}

    def test_preserves_improvement_regression_and_original_hypotheses(self):
        bad, good = self.row('hell', .2), self.row('hello', 0.)
        improved = pair(bad, good)
        self.assertTrue(improved['improved'])
        self.assertFalse(improved['regressed'])
        self.assertEqual('hell', improved['baseline']['recognized'])
        self.assertTrue(pair(good, bad)['regressed'])
        self.assertFalse(pair(good, good)['improved'])

    def test_changed_inputs_and_false_native_scores_fail_instead_of_being_omitted(self):
        old = self.row('hello', 0.)
        for field in ('input_sha256', 'reference', 'font_px'):
            with self.assertRaises(ValueError):
                pair(old, {**old, field: 'changed'})
        with self.assertRaises(ValueError):
            pair(old, {**old, 'recognized': 'bad', 'cer': 0.})

    def test_supplemental_audits_identify_actual_source_runs(self):
        from unittest.mock import patch
        from pathlib import Path
        from mynotes.tools.ci.audit_ocr61_history import summarize
        row=self.row('hello',0.)
        with patch('mynotes.tools.ci.audit_ocr61_history.load',return_value={'one':row}):
            report=summarize(Path('old'),Path('new'),41767304608,41771337080)
        self.assertEqual({'baseline':41767304608,'candidate':41771337080},report['source_runs'])
        self.assertIn('41771337080',report['scope'])
        self.assertFalse(report['adopted'])

import hashlib
import json
import unittest
from pathlib import Path
from mynotes.tools.ci.audit_quantized_speech import audit

FOLDER=Path(__file__).resolve().parents[1]/'mynotes/docs/recognition/compact-speech/41765677553'


class RetainedCompactSpeechEvidence(unittest.TestCase):
    def test_retained_receipt_and_independent_scores_match_raw_predictions(self):
        source=FOLDER/'paired-public-predictions.json'
        receipt=json.loads((FOLDER/'receipt.json').read_text())
        self.assertEqual(receipt['report_sha256'],hashlib.sha256(source.read_bytes()).hexdigest())
        self.assertEqual(audit(json.loads(source.read_text())),json.loads((FOLDER/'independent-audit.json').read_text()))
        self.assertFalse(receipt['adopted'])

    def test_smaller_model_regressions_are_not_hidden_by_average(self):
        report=json.loads((FOLDER/'independent-audit.json').read_text())
        self.assertEqual(['slt-arctic_b0040','slt-arctic_b0045'],report['summary']['q5_1']['regressed_ids'])
        self.assertEqual([],report['summary']['q8_0']['changed_hypothesis_ids'])
        self.assertEqual(64,report['summary']['q8_0']['cases'])
        self.assertFalse(report['adopted'])

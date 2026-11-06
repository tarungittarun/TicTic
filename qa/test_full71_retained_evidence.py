"""Preserve actual broad-run failures; never replace them with selected or rerun passes."""
import hashlib
import json
from pathlib import Path
import unittest
from mynotes.tools.ci.results import android
from mynotes.tools.ci.test_plan import build_plan
from mynotes.tools.ci.refinement_plan import selected_cases

EVIDENCE = Path('mynotes/docs/full-qualification/42277337133')


class FullOriginalEvidenceTest(unittest.TestCase):
    def test_summary_agrees_with_twelve_original_native_logs(self):
        summary = json.loads((EVIDENCE / 'summary.json').read_text())
        self.assertEqual(len(summary['native_lanes']), 12)
        passed, failures = 0, []
        for name, row in summary['native_lanes'].items():
            lane = EVIDENCE / name
            expected = json.loads((lane / 'refinement-expected.json').read_text())
            shard = int(name.rsplit('-s', 1)[1])
            self.assertEqual(expected, selected_cases(build_plan(), 'all', shard))
            result = android((lane / 'refinement-native.txt').read_text(), expected)
            self.assertEqual(result, json.loads((lane / 'refinement-native.json').read_text()))
            self.assertEqual(row, {k: v for k, v in result.items() if k != 'cases'})
            passed += result['passed']
            failures.extend(result['failures'])
        self.assertEqual(passed, summary['native_executions_passed'])
        self.assertEqual(passed, 2671)
        self.assertEqual(len(failures), 5)
        self.assertEqual(summary['workflow_conclusion'], 'failure')
        self.assertFalse(summary['all_six_profiles_exact_native_pass'])
        self.assertFalse(summary['whole_app_ux_acceptance'])

    def test_original_evidence_hashes_unchanged(self):
        manifest = json.loads((EVIDENCE / 'retention.json').read_text())
        for row in manifest['files']:
            self.assertFalse(row['source_bytes_modified'])
            self.assertEqual(hashlib.sha256((EVIDENCE / row['path']).read_bytes()).hexdigest(), row['sha256'])

    def test_both_signed_upgrade_proofs_are_bounded_and_preserve_data(self):
        proofs = list(EVIDENCE.glob('full71-release69-to71-*/release-rich-update-proof.json'))
        self.assertEqual(len(proofs), 2)
        for path in proofs:
            proof = json.loads(path.read_text())
            self.assertTrue(proof['ok'])
            self.assertFalse(proof['uninstall_or_clear_data'])
            self.assertFalse(proof['physical_acceptance'])
            self.assertFalse(proof['release_approval'])
            self.assertEqual(proof['from']['version_code'], 69)
            self.assertEqual(proof['to']['version_code'], 71)

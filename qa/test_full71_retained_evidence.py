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


class FullRecheckEvidenceTest(unittest.TestCase):
    def test_final_rechecks_are_actual_outcomes_not_a_replacement_full_score(self):
        root = Path('mynotes/docs/ux-improvement-ledger/implementations/42282354356')
        expected_counts = {'improvement71-api26-portrait': (218, 218),
                           'improvement71-api34-large': (0, 2),
                           'improvement71-api36-portrait': (0, 1)}
        for name, counts in expected_counts.items():
            lane = root / name
            expected = json.loads((lane / 'refinement-expected.json').read_text())
            actual = android((lane / 'refinement-native.txt').read_text(), expected)
            receipt = json.loads((lane / 'refinement-native-verified.json').read_text())
            self.assertEqual(actual, receipt)
            self.assertEqual((actual['passed'], actual['expected']), counts)
        original = json.loads((EVIDENCE / 'summary.json').read_text())
        self.assertEqual(original['native_executions_passed'], 2671)
        self.assertEqual(original['workflow_conclusion'], 'failure')

    def test_findings_match_actual_original_failed_methods(self):
        data = json.loads((EVIDENCE.parent / 'findings.json').read_text())
        failures = json.loads((EVIDENCE / 'summary.json').read_text())['native_lanes']
        methods = {failure.split(': status ', 1)[0] for row in failures.values() for failure in row.get('failures', [])}
        self.assertEqual({row['method'] for row in data['findings']}, methods)
        self.assertEqual(len(methods), 5)
        for row in data['findings']:
            self.assertTrue((EVIDENCE.parent / row['source']).is_file())
            self.assertTrue(row['next_test'])
            self.assertNotEqual(row['status'], 'fixed_verified')


class RecheckBinaryProvenanceTest(unittest.TestCase):
    def test_rebuild_is_never_misrepresented_as_byte_identical(self):
        proof = json.loads((EVIDENCE.parent / 'recheck-apk-identity.json').read_text())
        self.assertTrue(proof['app_test_same_certificate'])
        self.assertFalse(proof['unsigned_payload_equal'])
        self.assertNotEqual(proof['original_app']['sha256'], proof['used_app']['sha256'])
        self.assertNotEqual(proof['original_app']['identity'][3], proof['used_app']['identity'][3])
        self.assertEqual(set(proof['changed_unsigned_entries']), {
            'lib/arm64-v8a/libnotes_whisper.so',
            'lib/armeabi-v7a/libnotes_whisper.so',
            'lib/x86_64/libnotes_whisper.so'})

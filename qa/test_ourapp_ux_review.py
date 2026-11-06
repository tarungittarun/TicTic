"""Integrity of the comparison document, not a claim of new app/device tests."""
import json
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
FOLDER=ROOT/'mynotes/docs/comparisons/ourapp-vs-obsidian'
class UXReviewIntegrity(unittest.TestCase):
 def setUp(self):self.data=json.loads((FOLDER/'review-data.json').read_text());self.manifest=json.loads((FOLDER/'evidence-manifest.json').read_text())
 def test_two_modules_and_twenty_unique_findings(self):
  rows=self.data['findings'];self.assertEqual(len(rows),20);self.assertEqual(len({r['id'] for r in rows}),20);self.assertEqual({r['module'] for r in rows},{'Find & organise','Edit & read'})
 def test_every_finding_has_evidence_limits_and_acceptance(self):
  for row in self.data['findings']:
   for key in ['task','observed','comparison','impact','direction','acceptance','source','limit','kind']:self.assertTrue(row[key],(row['id'],key))
   self.assertTrue(row['evidence']);self.assertTrue(all(ref in self.data['assets'] for ref in row['evidence']))
 def test_frames_are_real_retained_repository_assets(self):
  import hashlib
  for key,item in self.manifest['assets'].items():
   path=ROOT/item['path'];self.assertTrue(path.is_file());self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(),item['sha256']);self.assertFalse(item['source_pixels_modified'])
 def test_no_fabricated_live_measurements(self):
  self.assertEqual(self.manifest['new_live_sessions'],0);self.assertEqual(self.manifest['new_runtime_bugs_reproduced'],0);self.assertIsNone(self.manifest['matched_timing_results'])
 def test_source_scopes_are_not_mistaken_for_observed_executions(self):
  by={r['id']:r for r in self.data['findings']}
  self.assertIn('Source-supported',by['N09']['kind']);self.assertIn('Reproduce',by['N09']['limit']);self.assertIn('not a measured bug',by['N07']['observed'])

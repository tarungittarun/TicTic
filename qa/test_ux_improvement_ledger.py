"""Ledger integrity; no device/user-study acceptance is implied by these checks."""
import hashlib,json
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'mynotes/docs/ux-improvement-ledger'
class ImprovementLedgerTests(unittest.TestCase):
 def setUp(self):self.data=json.loads((OUT/'ledger.json').read_text())
 def test_37_existing_module_plans_remain_individually_scoped(self):
  self.assertEqual({p['id'] for p in self.data['plan']},{'PLAN-X%02d'%n for n in range(1,38)})
  for p in self.data['plan']:
   self.assertTrue(p['task']);self.assertTrue(p['acceptance']);self.assertTrue(p['source_audit_is_not_execution'])
 def test_aspect_coverage_is_not_a_blanket_pass(self):
  for p in self.data['plan']:
   self.assertEqual(p['aspects']['animation'],'not_tested');self.assertEqual(p['aspects']['smoothness'],'not_tested')
 def test_no_fix_claim_without_implementation_and_retest(self):
  self.assertFalse(self.data['fixes_implemented'])
  for item in self.data['work_items']:
   self.assertEqual(item['status'],'proposed_not_implemented')
   self.assertTrue(item['next_test']);self.assertTrue(item['alternatives'])
 def test_literal_source_inventory_is_not_runtime_execution(self):
  definitions=json.loads((OUT/'source-surfaces.json').read_text())['controls']
  self.assertEqual(len({r['id'] for r in definitions}),len(definitions))
  for item in definitions:
   self.assertEqual(item['runtime_status'],'not_tested')
   self.assertEqual(hashlib.sha256((ROOT/item['path']).read_bytes()).hexdigest(),item['source_sha256'])
 def test_promoted_items_require_both_apps_and_real_retained_files(self):
  for item in self.data['work_items']:
   self.assertTrue(item['ours']);self.assertTrue(item['obsidian']);self.assertTrue(item['reproduction']);self.assertTrue(item['acceptance'])
   self.assertTrue(any('mynotes' in p for p in item['evidence']))
   self.assertTrue(any('obsidian' in p for p in item['evidence']))
   for ref in item['evidence']:self.assertTrue((OUT/ref).is_file(),ref)
 def test_causes_are_not_automatically_declared_confirmed(self):
  for item in self.data['work_items']:
   self.assertIn(item['cause_confidence'],['hypothesis','source_supported_mechanism_not_exclusive_cause','confirmed_by_isolating_experiment'])
   if item['cause_confidence']=='confirmed_by_isolating_experiment':self.assertTrue(item.get('causal_experiment'))
   for source in item['source_refs']:
    self.assertEqual(hashlib.sha256((ROOT/source['path']).read_bytes()).hexdigest(),source['sha256'])
 def test_measurements_define_units_and_limits(self):
  for m in self.data['measurements']:
   self.assertTrue(m['definition']);self.assertTrue(m['unit']);self.assertTrue(m['samples']);self.assertTrue(m['limit']);self.assertTrue((OUT/m['source']).is_file())
 def test_retained_files_have_unmodified_source_hashes(self):
  for manifest in (OUT/'sessions').glob('*/retention.json'):
   for f in json.loads(manifest.read_text())['files']:
    p=manifest.parent/f['path'];self.assertEqual(hashlib.sha256(p.read_bytes()).hexdigest(),f['sha256']);self.assertFalse(f['source_bytes_modified'])

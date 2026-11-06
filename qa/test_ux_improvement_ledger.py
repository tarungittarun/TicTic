"""Ledger integrity; no device/user-study acceptance is implied by these checks."""
import hashlib,json
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'mynotes/docs/ux-improvement-ledger'
def pinned_source(path,digest):
 current=(ROOT/path).read_bytes()
 if hashlib.sha256(current).hexdigest()==digest:return current
 # Historical audit pins do not silently change when an authorised fix lands.
 return (OUT/'source-baselines'/(digest+'.java')).read_bytes()

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
  self.assertEqual(self.data['fixes_implemented'],any(i['status']!='proposed_not_implemented' for i in self.data['work_items']))
  for item in self.data['work_items']:
   self.assertIn(item['status'],['proposed_not_implemented','implemented_pending_native_retest','fixed_verified'])
   if item['status']!='proposed_not_implemented':
    self.assertRegex(item['implementation_commit'],r'^[0-9a-f]{40}$')
   if item['status']=='fixed_verified':self.assertTrue(item['retest_evidence'])
   self.assertTrue(item['next_test']);self.assertTrue(item['alternatives'])
 def test_literal_source_inventory_is_not_runtime_execution(self):
  definitions=json.loads((OUT/'source-surfaces.json').read_text())['controls']
  self.assertEqual(len({r['id'] for r in definitions}),len(definitions))
  for item in definitions:
   self.assertEqual(item['runtime_status'],'not_tested')
   self.assertEqual(hashlib.sha256(pinned_source(item['path'],item['source_sha256'])).hexdigest(),item['source_sha256'])
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
    self.assertEqual(hashlib.sha256(pinned_source(source['path'],source['sha256'])).hexdigest(),source['sha256'])
 def test_measurements_define_units_and_limits(self):
  for m in self.data['measurements']:
   self.assertTrue(m['definition']);self.assertTrue(m['unit']);self.assertTrue(m['samples']);self.assertTrue(m['limit']);self.assertTrue((OUT/m['source']).is_file())
 def test_retained_files_have_unmodified_source_hashes(self):
  for manifest in (OUT/'sessions').glob('*/retention.json'):
   for f in json.loads(manifest.read_text())['files']:
    p=manifest.parent/f['path'];self.assertEqual(hashlib.sha256(p.read_bytes()).hexdigest(),f['sha256']);self.assertFalse(f['source_bytes_modified'])

class PairedHarnessEvidenceTests(unittest.TestCase):
 def search_function(self,matches,titles):
  import ast,types
  tree=ast.parse((ROOT/'mynotes/tools/comparisons/paired_ux_session.py').read_text())
  obsidian=next(n for n in tree.body if isinstance(n,ast.FunctionDef) and n.name=='obsidian')
  search=next(n for n in obsidian.body if isinstance(n,ast.FunctionDef) and n.name=='search')
  class Device:
   def tap(self,*args):return True
   def type(self,*args):pass
   def js(self,expression):
    if '.search-result-file-match' in expression:return matches
    if '.search-result-file-title' in expression:return titles
    if expression=='document.body.innerText':return '0 results\nNo matches found.\nUX Meeting'
  namespace={'d':Device(),'time':types.SimpleNamespace(sleep=lambda _:None),'capture':lambda *args:None,'TITLE':'UX Meeting','MARKER':'cobalt42'}
  exec(compile(ast.Module(body=[search],type_ignores=[]),'<bounded-search-check>','exec'),namespace)
  return namespace['search']
 def test_background_editor_title_cannot_pass_search(self):
  with self.assertRaisesRegex(AssertionError,'No visible result-passage'):
   self.search_function([],[])()
 def test_hidden_match_cannot_pass_search(self):
  with self.assertRaises(AssertionError):self.search_function([{'text':'cobalt42','visible':False}],['UX Meeting'])()
 def test_result_title_without_marker_cannot_pass(self):
  with self.assertRaises(AssertionError):self.search_function([{'text':'other body','visible':True}],['UX Meeting'])()
 def test_visible_marker_without_result_title_cannot_pass(self):
  with self.assertRaises(AssertionError):self.search_function([{'text':'cobalt42','visible':True}],[])()
 def test_explicit_result_passage_and_title_can_pass(self):
  result=self.search_function([{'text':'Decision marker cobalt42 belongs','visible':True}],['UX Meeting'])()
  self.assertEqual(result['status'],'visible_outcome_verified')
 def test_prior_raw_false_positive_is_preserved_and_overridden(self):
  out=ROOT/'mynotes/docs/ux-improvement-ledger'
  raw=json.loads((out/'sessions/42218900341/session.json').read_text())
  case=next(c for c in raw['cases'] if c['app']=='obsidian' and c['case']=='S01')
  self.assertEqual(case['status'],'visible_outcome_verified');self.assertIn('No matches found.',case['result_labels'])
  ledger=json.loads((out/'ledger.json').read_text())
  review=next(r for r in ledger['evidence_reviews'] if r['id']=='REVIEW-WAVE2-S01')
  self.assertIn('invalidated_harness_false_positive',review['conclusion'])
 def test_offline_bootstrap_precedes_apk_installation(self):
  workflow=(ROOT/'.github/workflows/notes-paired-ux-study.yml').read_text()
  self.assertLess(workflow.index('python -m mynotes.tools.ci.offline_guest'),workflow.index('adb install'))
 def test_third_creation_check_precedes_independent_fixture_setup(self):
  out=ROOT/'mynotes/docs/ux-improvement-ledger/sessions/42220127426'
  data=json.loads((out/'session.json').read_text())
  create=next(c for c in data['cases'] if c['app']=='obsidian' and c['case']=='W01')
  fixture=next(c for c in data['cases'] if c['app']=='obsidian' and c['case']=='FIXTURE')
  self.assertEqual(create['status'],'visible_outcome_verified')
  self.assertEqual(create['saved_text'],data['fixtures']['body'])
  self.assertLess(create['finished'],fixture['started'])
  self.assertEqual(fixture['status'],'fixture_prepared_not_a_test_pass')
 def test_third_offline_receipts_have_explicit_bounded_scope(self):
  out=ROOT/'mynotes/docs/ux-improvement-ledger/sessions/42220127426'
  proof=json.loads((out/'offline-guest-proof.json').read_text())
  self.assertFalse(proof['notes_installed']);self.assertEqual(proof['consecutive_checks'],3)
  self.assertEqual(proof['final_shell_uid'],2000);self.assertFalse(proof['active_default_network'])
  self.assertIn('Active default network: none',json.loads((out/'environment.json').read_text())['connectivity'])
  self.assertIn('Active default network: none',(out/'connectivity-after-session.txt').read_text())

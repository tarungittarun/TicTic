import unittest
from mynotes.tools.build_e2e_report import case_status, methods, source_surfaces


class ExecutedE2EReport(unittest.TestCase):
    def test_failure_is_never_hidden_by_another_profile_pass(self):
        evidence={'profiles':{'a':{'cases':['A#m']},'b':{'failures':['A#m: status -2 assertion']}}}
        self.assertEqual('FAIL',case_status('A#m',evidence)['status'])
        self.assertEqual(['a'],case_status('A#m',evidence)['passed'])
        self.assertEqual(['b'],case_status('A#m',evidence)['failed'])

    def test_missing_is_not_a_failed_assertion_or_a_pass(self):
        evidence={'profiles':{'a':{'missing':['A#m']}}}
        self.assertEqual('UNEXECUTED',case_status('A#m',evidence)['status'])
        self.assertEqual(['a'],case_status('A#m',evidence)['missing'])
        self.assertEqual([],case_status('A#m',evidence)['failed'])

    def test_method_inventory_covers_exact_native_plan(self):
        from mynotes.tools.ci.test_plan import build_plan,cases
        self.assertEqual(set(cases(build_plan()['android'])),set(methods()))

    def test_real_traversal_owners_and_literal_surface_inventory_exist(self):
        inventory=methods()
        self.assertIn('e2e-home-notes',inventory['app.space.notes.CoreJourneysTest#wholeAppLibraryTraversalRecordsEachVisitedPage']['scenes'])
        self.assertIn('e2e-settings',inventory['app.space.notes.CoreJourneysTest#wholeAppSettingsTraversalRecordsEachVisitedPage']['scenes'])
        self.assertTrue(all(row['line']>0 for row in source_surfaces()))
        self.assertIn('Viewed files',{row['title'] for row in source_surfaces()})

    def test_gate_rejects_self_reported_green_with_missing_or_duplicate_methods(self):
        from mynotes.tools.build_e2e_report import qualification_gate,PROFILES
        evidence={'profiles':{lane:{'ok':True,'expected':1,'passed':1,'cases':['A#m']} for lane in PROFILES}}
        self.assertTrue(qualification_gate(evidence,{'A#m'}))
        self.assertFalse(qualification_gate(evidence,{'A#m','A#notrun'}))
        evidence['profiles'][PROFILES[0]]['cases']=['A#m','A#m']
        self.assertFalse(qualification_gate(evidence,{'A#m'}))

    def test_preserved_evidence_cannot_be_relabelled_as_another_source(self):
        import tempfile,json
        from pathlib import Path
        from mynotes.tools.build_e2e_report import from_preserved
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);(root/'provenance.json').write_text(json.dumps({'capture_run':'1','source_sha':'a'*40}))
            with self.assertRaisesRegex(ValueError,'mismatch'):
                from_preserved(root,root/'out','2','a'*40)
            with self.assertRaisesRegex(ValueError,'mismatch'):
                from_preserved(root,root/'out','1','b'*40)

    def test_an_executed_failure_is_not_relabelled_as_unexecuted(self):
        evidence={'profiles':{'a':{'cases':[],'missing':['A#m','A#neverran'],'failures':['A#m: status -2 assertion']}}}
        failed=case_status('A#m',evidence)
        self.assertEqual('FAIL',failed['status'])
        self.assertEqual([],failed['missing'])
        self.assertEqual('UNEXECUTED',case_status('A#neverran',evidence)['status'])

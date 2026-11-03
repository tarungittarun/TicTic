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

    def test_full_bundle_needs_real_diagnostic_save_and_all_system_pages(self):
        from mynotes.tools.build_e2e_report import supplemental_gate,SYSTEM_SCENES
        supplement={'updates':[{'report':{'from':{'version_code':code},'ok':True,'notes_and_doc_retained':True,'uninstall_or_clear_data':False}} for code in [33,35,40,52,55]],'reader_recovery':[{'path':'process-recovery/123/reader-process-recovery.json','report':{'ok':True,'diagnostic_file_save':{'ok':True},'records':[{'original_unchanged':True,'query_restored':True,'before_pid':10,'after_pid':11} for _ in range(3)]}}]}
        screens=[{'scene':scene,'proof_kind':'signed-release-system','capture_run':'123'} for scene in SYSTEM_SCENES]
        self.assertTrue(supplemental_gate(supplement,screens))
        self.assertFalse(supplemental_gate(supplement,screens[1:]))
        supplement['reader_recovery'][0]['report']['diagnostic_file_save']['ok']=False
        self.assertFalse(supplemental_gate(supplement,screens))
        supplement['reader_recovery'][0]['report']['diagnostic_file_save']['ok']=True
        supplement['updates'].pop()
        self.assertFalse(supplemental_gate(supplement,screens))

    def test_system_capture_hash_and_run_are_checked_separately_from_native_counts(self):
        import tempfile,json,hashlib
        from pathlib import Path
        from unittest.mock import patch
        from mynotes.tools import build_e2e_report as report
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);folder=root/'process-recovery/123';folder.mkdir(parents=True)
            raw=b'unit-test-only-not-a-real-emulator-image';scene='reader-pdf-opened'
            (folder/(scene+'.jpg')).write_bytes(raw)
            capture={'name':scene,'capture_run':'123','width':720,'height':1280,'sha256':hashlib.sha256(raw).hexdigest()}
            manifest=folder/'screenshot-previews.json';manifest.write_text(json.dumps({'captures':[capture]}))
            supplements={'reader_recovery':[{'path':'process-recovery/123/reader-process-recovery.json','report':{'ok':True}}]};evidence={'screens':[],'profiles':{'native':{'passed':1}}}
            with patch.object(report,'ROOT',root):
                report.append_system_captures(evidence,root,supplements)
                self.assertEqual(1,evidence['profiles']['native']['passed'])
                self.assertEqual('signed-release-system',evidence['screens'][0]['proof_kind'])
                (folder/(scene+'.jpg')).write_bytes(b'tampered')
                with self.assertRaisesRegex(ValueError,'hash mismatch'):report.append_system_captures(evidence,root,supplements)
                (folder/(scene+'.jpg')).write_bytes(raw);capture['capture_run']='124';manifest.write_text(json.dumps({'captures':[capture]}))
                with self.assertRaisesRegex(ValueError,'run mismatch'):report.append_system_captures(evidence,root,supplements)

    def test_conditional_rail_screens_have_source_owners(self):
        inventory=methods();owner=inventory['app.space.notes.CoreJourneysTest#adaptiveEditorRailsKeepWritingBetweenActualToolColumns']
        self.assertTrue({'adaptive-doc-rails','adaptive-notes-rails'}.issubset(owner['scenes']))

    def test_native_preview_uses_global_run_not_system_capture_schema(self):
        import tempfile,json,hashlib
        from pathlib import Path
        from mynotes.tools.build_e2e_report import from_preserved,PROFILES
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);(root/'provenance.json').write_text(json.dumps({'capture_run':'123','source_sha':'a'*40}))
            folder=root/PROFILES[0];folder.mkdir();raw=b'unit-test-only-native-preview'
            (folder/'unit.jpg').write_bytes(raw)
            (folder/'manifest.json').write_text(json.dumps({'previews':[{'name':'unit','width':20,'height':30,'sha256':hashlib.sha256(raw).hexdigest(),'original_png_sha256':'b'*64}]}))
            evidence=from_preserved(root,root/'out','123','a'*40)
            self.assertEqual(1,len(evidence['screens']))
            self.assertNotIn('proof_kind',evidence['screens'][0])

    def test_leaf_diagnostic_picker_target_remains_truthy(self):
        import xml.etree.ElementTree as ET
        from mynotes.tools.ci.reader_process_recovery_smoke import picker_target,wait_until
        leaf=ET.fromstring('<node text="" content-desc="Show roots" enabled="true" bounds="[0,72][168,216]"/>')
        self.assertEqual(0,len(leaf))
        target=wait_until(lambda:picker_target([leaf],'Show roots'),seconds=.02)
        self.assertEqual('Show roots',target['content-desc'])
        self.assertIsNone(picker_target([leaf],'Save'))
        leaf.set('text','SAVE');self.assertTrue(picker_target([leaf],'Save'))

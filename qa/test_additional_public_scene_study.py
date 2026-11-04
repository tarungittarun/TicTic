import hashlib,json,unittest
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
class AdditionalPublicSceneStudy(unittest.TestCase):
    def test_exact_public_originals_license_and_manual_references_are_frozen(self):
        root=ROOT/'mynotes/report-assets/public-ocr-scenes-v3';manifest=json.loads((root/'manifest.json').read_text())
        self.assertFalse(manifest['production_packaged']);self.assertEqual(0,manifest['training_steps']);self.assertEqual(2,len(manifest['records']))
        self.assertEqual(manifest['license_sha256'],hashlib.sha256((root/'LICENSE').read_bytes()).hexdigest())
        for row in manifest['records']:
            data=(root/row['file']).read_bytes();self.assertEqual(row['sha256'],hashlib.sha256(data).hexdigest());self.assertEqual(row['git_blob'],hashlib.sha1(('blob '+str(len(data))+'\0').encode()+data).hexdigest())
            self.assertEqual((row['width'],row['height']),Image.open(root/row['file']).size);self.assertTrue(row['reference']);self.assertLess(len(data),2000000)
    def test_real_sign_scene_is_not_misrepresented_as_supported_french(self):
        text=(ROOT/'mynotes/report-assets/public-ocr-scenes-v3/README.md').read_text()
        self.assertIn('French is not a supported OCR language',text);self.assertIn('not current medical advice',text)
    def test_gate_verifies_only_actual_new_native_identity_and_independent_scores(self):
        source=(ROOT/'mynotes/tools/ci/run_additional_public_scene_study.sh').read_text()
        self.assertIn('originalPosterAndObliqueUnsupportedLatinSignsRetainAllOutputs',source);self.assertIn('independent_row(row)',source);self.assertNotIn('verify_recognition_pilot',source)
    def test_missing_study_is_incomplete_not_eight_expected_successes(self):
        import tempfile
        from mynotes.tools.ci.additional_scene_results import collect
        with tempfile.TemporaryDirectory() as folder:
            result=collect(Path(folder)/'empty',Path(folder)/'out',ROOT/'mynotes/report-assets/public-ocr-scenes-v3/manifest.json')
        self.assertEqual(0,result['observed']);self.assertFalse(result['complete']);self.assertEqual(8,result['expected']);self.assertEqual(4,len(result['profiles']))
    def test_extra_study_does_not_overwrite_main_corpus_native_log(self):
        source=(ROOT/'mynotes/tools/ci/run_additional_public_scene_study.sh').read_text()
        self.assertIn('NOTES_NATIVE_OUTPUT=additional-public-native.txt',source)
        self.assertIn("Path('additional-public-native.txt').read_text()",source)
        self.assertIn('expectedNight "$1"',source);self.assertIn('expectedOrientation "$2"',source)
    def test_full_qualification_keeps_additional_cohort_separate_and_runs_all_profiles(self):
        import yaml
        full=yaml.safe_load((ROOT/'.github/workflows/notes-module-e2e.yml').read_text())
        job=full['jobs']['benchmark'];profiles=job['strategy']['matrix']['include']
        self.assertEqual(4,len([r for r in profiles if r['workload']=='ocr']))
        emulator=next(s for s in job['steps'] if str(s.get('uses','')).startswith('ReactiveCircus/'))
        self.assertIn('run_additional_public_scene_study.sh ${{ matrix.night }} ${{ matrix.orientation }}',emulator['with']['script'])
        report=next(s for s in full['jobs']['report']['steps'] if s.get('name','').startswith('Generate module-ordered'))
        self.assertIn('--additional-scenes-required',report['run'])
        self.assertTrue(any('additional-public-*' in str(s.get('with',{}).get('path','')) for s in job['steps']))
    def write_local_parser_fixture(self,root):
        # Deliberately fabricated unit-parser inputs, never published as device evidence.
        from mynotes.tools.ci.additional_scene_results import NAMES,PROFILES
        from mynotes.tools.ci.smallfont_scene_report import normalize
        manifest=json.loads((ROOT/'mynotes/report-assets/public-ocr-scenes-v3/manifest.json').read_text())
        for profile in PROFILES:
            folder=root/('module-benchmark-'+profile+'-ocr');folder.mkdir(parents=True)
            rows=[dict(id=r['id'],reference=r['reference'],recognized=r['reference'],cer=0,character_edits=0,reference_characters=len(normalize(r['reference'])),elapsed_ms=1,status='OK',source_pixels_unchanged=True,source_sha256=r['sha256']) for r in manifest['records']]
            (folder/'additional-public-scenes.json').write_text(json.dumps(dict(expected=2,cases=rows,production_changed=False,physical_acceptance=False)))
            raw=''
            for name in NAMES:
                cls,method=name.split('#');raw+=f'INSTRUMENTATION_STATUS: class={cls}\nINSTRUMENTATION_STATUS: test={method}\nINSTRUMENTATION_STATUS_CODE: 0\n'
            (folder/'additional-public-native.txt').write_text(raw+'INSTRUMENTATION_CODE: -1\n')
    def test_local_complete_parser_fixture_requires_all_actual_identities_and_pairs(self):
        import tempfile
        from mynotes.tools.ci.additional_scene_results import collect
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);self.write_local_parser_fixture(root/'artifacts');result=collect(root/'artifacts',root/'out',ROOT/'mynotes/report-assets/public-ocr-scenes-v3/manifest.json')
        self.assertTrue(result['complete']);self.assertEqual(8,result['observed']);self.assertFalse(result['physical_acceptance'])
    def test_claimed_success_json_cannot_replace_missing_native_execution(self):
        import tempfile
        from mynotes.tools.ci.additional_scene_results import collect
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);self.write_local_parser_fixture(root/'artifacts');lane=root/'artifacts/module-benchmark-light-portrait-ocr'
            (lane/'additional-public-native.txt').write_text('');(lane/'additional-public-native-verified.json').write_text('{"ok":true}')
            result=collect(root/'artifacts',root/'out',ROOT/'mynotes/report-assets/public-ocr-scenes-v3/manifest.json')
        self.assertFalse(result['complete']);self.assertEqual(8,result['observed'])
    def test_expensive_full_corpus_waits_for_new_selection_reader_and_navigation_gates(self):
        import yaml
        full=yaml.safe_load((ROOT/'.github/workflows/notes-module-e2e.yml').read_text())
        self.assertIn('recognition_focus',full['jobs']['benchmark']['needs'])
        focus=full['jobs']['recognition_focus'];emulator=next(s for s in focus['steps'] if str(s.get('uses','')).startswith('ReactiveCircus/'))
        for name in ['nativeDocSelectionSurvivesUnselectedAreaScroll','nativeNotesSelectionSurvivesUnselectedAreaScroll','wholeAppLibraryTraversalRecordsEachVisitedPage','app.space.notes.runtime.ViewedFileJourneysTest']:
            self.assertIn(name,emulator['with']['script'])

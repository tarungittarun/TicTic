import json,unittest
from pathlib import Path
from mynotes.tools.build_module_e2e_report import MODULES,PROFILES,module_for,scene_module,independent_score
ROOT=Path(__file__).resolve().parents[2]

class SeparateModuleE2E(unittest.TestCase):
    def test_module_assignment_covers_known_feature_groups(self):
        self.assertEqual('Read image / OCR',module_for('OfflineOcrTest#corpus'))
        self.assertEqual('Voice inbox and transcripts',module_for('VoiceRecoveryJourneysTest#restore'))
        self.assertEqual('Diagrams',module_for('CoreJourneysTest#diagramShapes'))
        self.assertEqual('Notes and checklists',module_for('CoreJourneysTest#numberedWriting'))
        self.assertEqual('Reminders',scene_module('reminder-date','CoreJourneysTest#nativeSignatureShape'))
        self.assertEqual(len(MODULES),len(set(MODULES)))
    def test_independent_ground_truth_scoring_rejects_forged_scores(self):
        row={'id':'unit','status':'OK','reference':'abc','recognized':'axc','cer':1/3,'strict_wer':1.0,'elapsed_ms':1,'accuracy_target_met':False}
        independent_score(row,'ocr')
        row['cer']=0
        with self.assertRaisesRegex(ValueError,'score mismatch'):independent_score(row,'ocr')
    def test_independent_scoring_rejects_false_accuracy_pass(self):
        row={'id':'unit','status':'OK','reference':'abc','recognized':'axc','cer':1/3,'strict_wer':1.0,'elapsed_ms':1,'accuracy_target_met':True}
        with self.assertRaisesRegex(ValueError,'False accuracy'):independent_score(row,'ocr')
    def test_theme_and_orientation_are_a_full_cross_product(self):
        self.assertEqual({'light-portrait','dark-portrait','light-landscape','dark-landscape'},set(PROFILES))
        import yaml
        workflow=yaml.safe_load((ROOT/'.github/workflows/notes-module-e2e.yml').read_text())
        native=workflow['jobs']['native']['strategy']['matrix']['include'];bench=workflow['jobs']['benchmark']['strategy']['matrix']['include']
        self.assertEqual(4,len(native));self.assertEqual(12,len(bench))
        self.assertEqual(set(PROFILES),{row['id'] for row in native})
        self.assertEqual(34,workflow['jobs']['native']['steps'][-2]['with']['api-level'])
        self.assertIn('run_ordered_modules',workflow['jobs']['native']['steps'][-2]['with']['script'])
        self.assertEqual({(night,orientation) for night in ['no','yes'] for orientation in ['portrait','landscape']},{(row['night'],row['orientation']) for row in native})
        self.assertIn('always()',workflow['jobs']['report']['if'])
        self.assertFalse(workflow['jobs']['native']['strategy']['fail-fast'])
    def test_harness_is_opt_in_test_only_and_corpus_is_not_vendored(self):
        gradle=(ROOT/'mynotes/app/build.gradle').read_text()
        self.assertIn("project.hasProperty('moduleE2E')",gradle)
        self.assertIn('android.sourceSets.androidTest',gradle)
        self.assertFalse((ROOT/'mynotes/benchmarks/assets/module-corpus/manifest.json').exists())
        java=(ROOT/'mynotes/app/benchmarks/java/app/space/notes/ModuleCorpusBenchmark.java').read_text()
        self.assertIn('assertEquals(384,rows.length())',java)
        self.assertIn('int expected=402',java)
        self.assertIn('VoiceTranscripts.apply',java)
        self.assertIn('audio_unchanged',java)
        self.assertIn('reference',java)
        self.assertNotIn('assumeTrue',java)
    def test_absent_archive_copying_requires_hash_bound_primary_grant(self):
        from mynotes.tools.ci.module_corpus import retained_grant,ARCHIVES
        for speaker,digest in ARCHIVES.items():
            grant,url=retained_grant(speaker,digest)
            self.assertIn('free for use for any purpose',grant.lower())
            self.assertIn(speaker,url)
            with self.assertRaisesRegex(ValueError,'provenance mismatch'):retained_grant(speaker,'0'*64)
    def test_benchmark_script_fails_on_instrumentation_outcome_failure(self):
        script=(ROOT/'mynotes/tools/ci/run_module_benchmark.sh').read_text()
        self.assertIn('sys.exit(1)',script)
        self.assertIn('exit "$verification"',script)
    def test_every_existing_native_scene_has_one_explicit_module_and_workflow_order(self):
        from mynotes.tools.build_module_e2e_report import SCENE_ORDER,SCENE_MODULE
        scenes=[scene for values in SCENE_ORDER.values() for scene in values]
        self.assertEqual(len(scenes),len(set(scenes)))
        evidence=json.loads((ROOT/'mynotes/docs/e2e/41591079488/provenance.json').read_text())
        known={row['scene'] for row in evidence['screens'] if not row.get('proof_kind')}
        self.assertEqual(known,set(scenes))
        self.assertEqual('Diagrams',SCENE_MODULE['diagram'])
        self.assertEqual('Voice inbox and transcripts',SCENE_MODULE['voice-inbox'])
        self.assertEqual('Images and drawing',SCENE_MODULE['drawing'])
    def test_performance_summary_uses_actual_sizes_and_durations(self):
        from mynotes.tools.build_module_e2e_report import performance_summary
        rows=[{'id':'unit','profile':'light-portrait','module':'voice','kind':'clean','status':'OK','reference':'one two','recognized':'one','normalized_wer':.5,'elapsed_ms':2000,'input_bytes':64044,'duration_seconds':2.0,'accuracy_target_met':False}]
        result=performance_summary(rows)[0]
        self.assertEqual(1.0,result['aggregate_real_time_factor'])
        self.assertEqual(.5,result['micro_error_rate_excluding_empty_reference'])
        self.assertEqual(64044,result['input_bytes_min'])
        self.assertEqual(0,result['accuracy_target_passes'])
    def test_report_indic_fonts_are_hash_pinned_and_report_only(self):
        import hashlib
        folder=ROOT/'mynotes/report-assets/fonts'
        for entry in json.loads((folder/'provenance.json').read_text()):
            font=folder/('NotoSans'+entry['script']+'-Regular.ttf')
            self.assertEqual(entry['static_sha256'],hashlib.sha256(font.read_bytes()).hexdigest())
            self.assertIn('SIL OPEN FONT LICENSE',(folder/('NotoSans'+entry['script']+'-OFL.txt')).read_text())
        from mynotes.tools.build_module_e2e_report import report_markup
        self.assertIn('name="Devanagari"',report_markup('भारत'))
        self.assertIn('name="Kannada"',report_markup('ಕನ್ನಡ'))
    def test_native_execution_has_strict_matching_module_barriers(self):
        from mynotes.tools.ci.run_ordered_modules import plan
        from mynotes.tools.build_module_e2e_report import MODULES
        ordered=plan();self.assertEqual(MODULES,list(ordered))
        self.assertEqual(419,sum(map(len,ordered.values())))
        script=(ROOT/'mynotes/tools/ci/run_ordered_modules.py').read_text()
        self.assertIn('for index,(module,selected) in enumerate(ordered.items())',script)
        self.assertIn('subprocess.run(command',script)
        self.assertNotIn('Popen',script)
    def test_strict_java_word_boundary_does_not_split_nbsp(self):
        from mynotes.tools.build_module_e2e_report import strict_words,normalize
        self.assertEqual(['a\u00a0b'],strict_words(normalize('a\u00a0b')))
        self.assertEqual('one',normalize('\x1cone\x1c'))
    def test_setup_recheck_cannot_swap_build_or_inputs(self):
        from mynotes.tools.build_module_e2e_report import apply_recheck
        data={'source':'original','run':'1'}
        with self.assertRaises(ValueError):apply_recheck(data,[],{'artifact_source_sha':'changed','artifact_source_run':'1'},ROOT)
    def test_report_crop_preserves_ink_and_does_not_mutate_source(self):
        from PIL import Image,ImageDraw
        from mynotes.tools.build_module_e2e_report import specimen_crop
        image=Image.new('RGB',(100,80),'white');ImageDraw.Draw(image).rectangle((20,25,40,45),fill='black')
        before=image.tobytes();crop,changed=specimen_crop(image)
        self.assertTrue(changed);self.assertLess(crop.width,100)
        self.assertEqual(before,image.tobytes())
        self.assertEqual(21*21,sum(p==(0,0,0) for p in crop.getdata()))
    def test_accepted_setup_correction_keeps_rejected_history_and_cannot_pass_partial_corpus(self):
        import copy,tempfile
        from mynotes.tools.build_module_e2e_report import apply_recheck
        lane='dark-landscape-ocr'
        row={'id':'ocr-0000','lane':lane,'profile':'dark-landscape','module':'ocr','kind':'unit-layout-not-execution','reference':'abc','recognized':'axc','input_sha256':'unit-sha','input_bytes':1,'status':'OK','cer':1/3,'strict_wer':1.0,'elapsed_ms':1,'accuracy_target_met':False}
        data={'source':'unit-source','run':'unit-parent','benchmark_executions':{lane:{'ok':False,'failures':['app.space.notes.EnvironmentQualificationTest#orientationMatchesRequestedLane: unit orientation mismatch']}}}
        proof={'artifact_source_sha':'unit-source','artifact_source_run':'unit-parent','profile':'dark-landscape','run':'unit-correction','execution':{'ok':True,'passed':4,'failures':[]},'measurements':[copy.deepcopy(row)]}
        with tempfile.TemporaryDirectory() as folder:
            rows=[copy.deepcopy(row)];output=Path(folder);apply_recheck(data,rows,proof,output)
            self.assertTrue((output/'original-provenance.json').exists())
            self.assertEqual('unit-parent',json.loads((output/(lane+'-rejected-original-observations.json')).read_text())['run'])
            self.assertFalse(data['benchmark_complete']);self.assertFalse(data['all_accuracy_targets_met'])
            self.assertEqual('unit-correction',rows[0]['capture_run'])
    def test_ordered_driver_reads_watchdog_file_and_keeps_single_invocation_validation(self):
        script=(ROOT/'mynotes/tools/ci/run_ordered_modules.py').read_text()
        watchdog=(ROOT/'mynotes/tools/ci/instrumentation_watchdog.py').read_text()
        self.assertIn("os.environ.get('NOTES_NATIVE_OUTPUT','refinement-native.txt')",watchdog)
        self.assertIn('actual=raw.read_text',script)
        self.assertIn('observed=android(actual,selected)',script)
        self.assertIn('report=compare(expected,passed,failures)',script)
        self.assertNotIn('android(combined,expected)',script)
    def test_ordered_driver_real_file_channel_and_multi_invocation_aggregate(self):
        import os,tempfile,subprocess
        from unittest.mock import patch
        from mynotes.tools.ci import run_ordered_modules as driver
        from mynotes.tools.build_module_e2e_report import MODULES
        selected={name:['unit.Driver#case'+str(i)] for i,name in enumerate(MODULES)}
        def fake_run(command,**kwargs):
            if command[0]=='bash':
                case=command[-1].split('#')[1]
                Path(kwargs['env']['NOTES_NATIVE_OUTPUT']).write_text('INSTRUMENTATION_STATUS: class=unit.Driver\nINSTRUMENTATION_STATUS: test='+case+'\nINSTRUMENTATION_STATUS_CODE: 0\nINSTRUMENTATION_CODE: -1\n')
            return subprocess.CompletedProcess(command,0,stdout='')
        previous=os.getcwd()
        with tempfile.TemporaryDirectory() as folder:
            try:
                os.chdir(folder)
                with patch.object(driver,'plan',return_value=selected),patch.object(driver.subprocess,'run',side_effect=fake_run),patch('sys.argv',['unit-driver','--night','no','--orientation','portrait']):driver.main()
                report=json.loads(Path('refinement-native.json').read_text())
                self.assertTrue(report['ok']);self.assertEqual(12,report['passed']);self.assertEqual(12,report['instrumentation_invocations'])
                self.assertEqual(12,len(list(Path('.').glob('ordered-module-[0-9][0-9].txt'))))
            finally:os.chdir(previous)
    def test_pdf_export_control_is_native_named_and_cancel_is_transactional(self):
        activity=(ROOT/'mynotes/app/src/main/java/app/space/notes/NotesActivity.java').read_text()
        block=activity.split('@Override public void exportPdf()',1)[1].split('private void choosePdfDestination',1)[0]
        self.assertIn('new android.widget.CheckBox(options.getContext())',block)
        self.assertIn('guides.setMinHeight',block)
        self.assertNotIn('setMultiChoiceItems',block)
        self.assertIn('pdfPrintGuides=guides.isChecked();choosePdfDestination()',block)
        self.assertNotIn('setOnCheckedChangeListener',block)
        native=(ROOT/'mynotes/app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
        self.assertIn('Named PDF control must own its click action',native)
        self.assertIn('Cancel must discard the draft export preference',native)
    def test_segmentation_probe_is_public_test_only_and_separately_timed(self):
        harness=(ROOT/'mynotes/app/benchmarks/java/app/space/notes/ModuleCorpusBenchmark.java').read_text()
        production=(ROOT/'mynotes/runtime/src/main/java/app/space/notes/runtime/OfflineOcr.java').read_text()
        self.assertIn('public_segmentation_probe',harness);self.assertIn('probe_elapsed_ms',harness)
        self.assertNotIn('public_segmentation_probe',production)
    def test_public_probe_independent_scores_reject_wrong_reference_and_score(self):
        from mynotes.tools.build_module_e2e_report import independent_public_probes
        row={'id':'diagnostic','accuracy_target_met':False,'reference':'abc','probe_elapsed_ms':1,'public_segmentation_probe':[{'page_segmentation_mode':mode,'confidence':90,'reference':'abc','recognized':'axc','cer':1/3,'strict_wer':1.0} for mode in [3,11,6]]}
        independent_public_probes(row)
        row['public_segmentation_probe'][1]['cer']=0
        with self.assertRaisesRegex(ValueError,'score mismatch'):independent_public_probes(row)
        row['public_segmentation_probe'][1]['cer']=1/3;row['public_segmentation_probe'][1]['reference']='abd'
        with self.assertRaisesRegex(ValueError,'reference changed'):independent_public_probes(row)
    def test_holdout_is_frozen_and_separately_invoked(self):
        import hashlib
        path=ROOT/'mynotes/docs/regression/ocr-heldout-v1.json'
        self.assertEqual('c0e2294134b54c57b1587663041b2431fcc2a773e565f4be39f378d54b21f188',hashlib.sha256(path.read_bytes()).hexdigest())
        data=json.loads(path.read_text());self.assertEqual(32,len(data['records']));self.assertEqual(4,len(data['conditions']))
        self.assertEqual(32,len({tuple(r['lines']) for r in data['records']}))
        java=(ROOT/'mynotes/app/benchmarks/java/app/space/notes/ModuleCorpusBenchmark.java').read_text()
        self.assertIn('heldOutOcrReferencesMeasureIndependentRecognition',java)
        self.assertIn('assertEquals(128,rows.length())',java)

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
        self.assertEqual(8,len(native));self.assertEqual(12,len(bench))
        self.assertTrue(all(row['api']==34 and row['group']=='all' for row in native))
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

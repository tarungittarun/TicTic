import copy
import unittest
from pathlib import Path
from mynotes.tools.ci.audit_speech_kernel import compare, validate

ROOT=Path(__file__).resolve().parents[1]


def report(variant):
    return dict(variant=variant,shard=0,expected=1,training_steps=0,adopted=False,warmup={'status':'OK'},cases=[dict(id='public-1',reference='Hello world',recognized='hello world',status='OK',audio_unchanged=True,input_sha256='a'*64,input_bytes=32044,seconds=1,kind='clean',normalized_wer=0,elapsed_ms=100)])


class SpeechKernelStudy(unittest.TestCase):
    def test_exact_hypotheses_are_independently_scored(self):
        r=compare(report('standard'),report('flash'),expected=1)
        self.assertEqual((0,0,1),(r['regressed'],r['improved'],r['observations']))
        self.assertFalse(r['adopted'])

    def test_regression_retained_even_if_candidate_is_faster(self):
        r=report('flash');r['cases'][0].update(recognized='hello',normalized_wer=.5,elapsed_ms=1)
        self.assertEqual(1,compare(report('standard'),r,expected=1)['regressed'])

    def test_false_native_score_rejected(self):
        r=report('flash');r['cases'][0]['recognized']='wrong words'
        with self.assertRaises(ValueError):validate(r,expected=1)

    def test_reference_and_input_mismatch_rejected(self):
        for field,value in [('reference','different words'),('input_sha256','b'*64),('seconds',2)]:
            r=report('flash');r['cases'][0][field]=value
            if field=='reference':r['cases'][0]['normalized_wer']=1
            with self.assertRaises(ValueError):compare(report('standard'),r,expected=1)

    def test_missing_duplicate_and_failed_observations_rejected(self):
        for change in ('missing','duplicate','failed','changed-source'):
            r=report('flash')
            if change=='missing':r['cases']=[]
            elif change=='duplicate':r['cases']*=2
            elif change=='failed':r['cases'][0]['status']='ERROR'
            else:r['cases'][0]['audio_unchanged']=False
            with self.assertRaises(ValueError):validate(r,expected=1)

    def test_invalid_and_zero_time_handled_without_fake_speedup(self):
        r=report('flash');r['cases'][0]['elapsed_ms']=0
        self.assertIsNone(compare(report('standard'),r,expected=1)['observed_median_ratio_standard_over_flash'])
        r['cases'][0]['elapsed_ms']=-1
        with self.assertRaises(ValueError):validate(r,expected=1)

    def test_default_and_release_attention_stay_off(self):
        cmake=(ROOT/'mynotes/runtime/src/main/cpp/CMakeLists.txt').read_text()
        self.assertIn('option(NOTES_FLASH_ATTENTION_RESEARCH',cmake)
        self.assertIn('adoption" OFF)',cmake)
        gradle=(ROOT/'mynotes/runtime/build.gradle').read_text()
        self.assertIn("release { externalNativeBuild { cmake { arguments '-DNOTES_FLASH_ATTENTION_RESEARCH=OFF'",gradle)
        self.assertIn("project.hasProperty('flashAttentionResearch')",gradle)
        cpp=(ROOT/'mynotes/runtime/src/main/cpp/notes_whisper.cpp').read_text()
        self.assertIn('params.use_gpu=false;params.flash_attn=false;',cpp)
        self.assertIn('#if NOTES_WHISPER_FLASH_ATTENTION',cpp)

    def test_benchmark_is_opt_in_not_production_source(self):
        self.assertTrue((ROOT/'mynotes/app/benchmarks/java/app/space/notes/CompactSpeechBenchmark.java').is_file())
        self.assertFalse((ROOT/'mynotes/app/src/main/java/app/space/notes/CompactSpeechBenchmark.java').exists())

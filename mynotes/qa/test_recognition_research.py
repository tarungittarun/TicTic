"""Independent correctness checks for nonshipping research scoring/provenance boundaries."""
import unittest
from pathlib import Path
from mynotes.tools.ci.recognition_research import wer

class RecognitionResearchTests(unittest.TestCase):
    def test_lexical_scoring_exposes_substitution_and_insertion(self):
        result=wer('one two','one three four')
        self.assertEqual(2,result['edits'])
        self.assertEqual(1.0,result['lexical_wer'])

    def test_normalization_is_lexical_not_a_punctuation_score(self):
        self.assertEqual(0,wer('Hello, WORLD!','hello world')['edits'])
        self.assertEqual(1,wer('one','ONE two')['edits'])

    def test_silence_reports_hallucinated_word_count(self):
        self.assertEqual(3,wer('','thank you all')['edits'])
        self.assertEqual(0,wer('','')['edits'])

    def test_research_cannot_be_confused_with_app_dependencies_or_training(self):
        root=Path(__file__).resolve().parents[1]
        code=(root/'tools/ci/recognition_research.py').read_text()
        self.assertIn('not Android performance',code)
        self.assertIn('no publisher digest',code)
        self.assertIn('fixture_git_blob',code)
        self.assertNotIn('extractall(',code)
        self.assertNotIn('sherpa-onnx',(root/'app/build.gradle').read_text())

    def test_emulator_report_collection_is_a_complete_single_shell_command(self):
        import subprocess
        root=Path(__file__).resolve().parents[2]
        workflow=(root/'.github/workflows/notes-refinement-native.yml').read_text()
        line=next(line.strip() for line in workflow.splitlines() if 'for report in ocr-common-text-report' in line)
        checked=subprocess.run(['sh','-n'],input=line,text=True,capture_output=True)
        self.assertEqual(0,checked.returncode,checked.stderr)

    def test_ocr_research_preserves_symbols_and_unicode_errors(self):
        from mynotes.tools.ci.ocr_weight_research import cer
        self.assertEqual(1,cer('25 + 5','25 - 5')['character_edits'])
        self.assertEqual(1,cer('😀','🙂')['character_edits'])
        self.assertEqual(1,cer('😀','🙂')['reference_codepoints'])

    def test_ocr_research_normalizes_only_canonical_forms_and_whitespace(self):
        from mynotes.tools.ci.ocr_weight_research import cer
        self.assertEqual(0,cer('é  NEXT','e\u0301\nNEXT')['character_edits'])
        self.assertEqual(1,cer('A','a')['character_edits'])

    def test_ocr_annotation_groups_fit_observed_github_limits(self):
        import json
        from mynotes.tools.ci.ocr_weight_research import TEXT
        messages=[]
        for family in ['fast','best']:
            for language,phrases in TEXT.items():
                rows=[dict(family=family,language=language,case=i,variant=v,reference=t,recognized=t,process_seconds=1.234567890123,image_sha256='a'*64,font_sha256='b'*64,cer=0.0,character_edits=0,reference_codepoints=len(t)) for i,t in enumerate(phrases) for v in ['clean','low-contrast']]
                messages.append(json.dumps(dict(family=family,language=language,cases=rows),ensure_ascii=False))
        self.assertEqual(6,len(messages))
        self.assertTrue(all(len(message.encode())<=3500 for message in messages))

    def test_quality_report_recomputed_score_cannot_be_forged(self):
        from mynotes.tools.ci.quality_reports import validate
        report={'source_sha256':'a'*64,'cases':[dict(variant=v,baseline='one two',recognized='one two',relative_wer=0,elapsed_ms=1) for v in ['one-third-amplitude','seeded-plus-minus-200-pcm16']]}
        self.assertTrue(validate(report,'speech'))
        report['cases'][0]['recognized']='three four'
        with self.assertRaises(ValueError):validate(report,'speech')

    def test_quantitative_report_requires_all_variants_and_hash(self):
        from mynotes.tools.ci.quality_reports import validate
        with self.assertRaises(ValueError):validate({'cases':[]},'speech')
        report={'cases':[dict(variant=v,baseline='one',recognized='one',relative_wer=0,elapsed_ms=1) for v in ['one-third-amplitude','seeded-plus-minus-200-pcm16']]}
        with self.assertRaises(ValueError):validate(report,'speech')

    def test_api26_collection_does_not_require_external_test_binary(self):
        root=Path(__file__).resolve().parents[2]
        workflow=(root/'.github/workflows/notes-refinement-native.yml').read_text()
        self.assertNotIn('run-as app.space.notes.debug test -f',workflow)
        self.assertIn('run-as app.space.notes.debug ls "files/$report"',workflow)

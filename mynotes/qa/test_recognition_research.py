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

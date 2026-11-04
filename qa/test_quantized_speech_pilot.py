import unittest
from mynotes.tools.ci.quantized_speech_pilot import regression, tokens


class QuantizedSpeechTest(unittest.TestCase):
    def test_regressions_and_operational_failures_retained(self):
        base=dict(id='one',input_sha256='same',reference='Text',status='OK',wer=.1)
        self.assertTrue(regression(base,{**base,'wer':.2}))
        self.assertTrue(regression(base,{**base,'status':'ERROR','wer':None}))
        self.assertFalse(regression(base,{**base,'wer':.1}))
        with self.assertRaises(ValueError):
            regression(base,{**base,'input_sha256':'changed'})

    def test_lexical_scoring_is_not_punctuation_accuracy(self):
        self.assertEqual(['hello',"don't",'25'],tokens("Hello! Don't: 25."))

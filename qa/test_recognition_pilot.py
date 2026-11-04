import unittest
from mynotes.tools.ci.verify_recognition_pilot import verify


class RecognitionPilotTest(unittest.TestCase):
    def test_empty_output_cannot_pass(self):
        report = verify('', ['app.space.notes.OfflineOcrTest'])
        self.assertFalse(report['ok'])
        self.assertGreater(report['expected'], 0)
        self.assertEqual(0, report['passed'])

    def test_unknown_class_cannot_pass(self):
        with self.assertRaises(ValueError):
            verify('', ['not.a.Test'])

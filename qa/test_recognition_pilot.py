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

    def test_exact_method_selector_is_verified_without_silently_expanding_class(self):
        name='app.space.notes.CoreJourneysTest#nativeHomeLongHoldDragPersistsUserOrderWithoutChangingDocuments'
        report=verify('',[name])
        self.assertEqual(1,report['expected'])
        self.assertFalse(report['ok'])
        with self.assertRaises(ValueError):verify('',[name+'Unknown'])

import unittest
from mynotes.tools.ci.compare_ocr_historical60 import pair_rows, summarize


class Historical60Test(unittest.TestCase):
    def test_fixture_identity_rejected_before_scoring(self):
        old = dict(id='one', input_sha256='hash', reference='text', kind='plain', condition='clean')
        for key in old:
            with self.subTest(key=key), self.assertRaises(ValueError):
                pair_rows(old, {**old, key: 'different'})

    def test_missing_or_duplicate_history_rejected(self):
        for pairs in ([], [{'profile': 'light-portrait', 'id': 'same'}] * 512):
            with self.assertRaises(ValueError):
                summarize({'pairs': pairs}, None)

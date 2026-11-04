import unittest
from mynotes.tools.ci.smallfont_scene_report import normalize,independent_row
class SmallFontSceneReport(unittest.TestCase):
    def row(self):return dict(status='OK',source_pixels_unchanged=True,reference='abc',recognized='axc',character_edits=1,reference_characters=3,cer=1/3,elapsed_ms=1)
    def test_independent_score_checks_actual_hypothesis(self):self.assertAlmostEqual(1/3,independent_row(self.row()))
    def test_forged_perfect_score_is_rejected(self):
        row=self.row();row['cer']=0
        with self.assertRaises(ValueError):independent_row(row)
    def test_changed_pixels_are_not_qualified(self):
        row=self.row();row['source_pixels_unchanged']=False
        with self.assertRaises(ValueError):independent_row(row)
    def test_java_ascii_whitespace_and_unicode_nfc_are_matched(self):
        self.assertEqual('é a',normalize(' e\u0301\n a '));self.assertEqual('\u00a0a\u00a0',normalize('\u00a0a\u00a0'))

import unittest
from mynotes.tools.ci.audit_public_script_inventions import review

class PublicScriptInventionReview(unittest.TestCase):
    def test_ascii_reference_detects_indic_digit_inventions(self):
        result=review('5:32 100%','5:32 ೫60 ९ 100%')
        self.assertFalse(result['no_unexpected_script_letters_or_digits'])
        self.assertEqual(['೫','९'],[r['character'] for r in result['unexpected']])
    def test_actual_ascii_status_values_are_not_deleted_or_flagged(self):
        self.assertTrue(review('5:32 100%','5:32 100%')['no_unexpected_script_letters_or_digits'])
    def test_never_apply_ascii_rule_to_legitimate_indic_or_mixed_reference(self):
        for reference in ('ಕನ್ನಡ ೫','Meeting भारत ९'):
            self.assertFalse(review(reference,reference)['applicable'])
    def test_nonletter_symbols_do_not_get_mislabelled_as_indic_text(self):
        self.assertTrue(review('100%','100% →')['no_unexpected_script_letters_or_digits'])

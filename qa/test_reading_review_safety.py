import unittest
from pathlib import Path
ROOT=Path('mynotes/runtime/src/main/java/app/space/notes/runtime')
class ReadingReviewSafety(unittest.TestCase):
 def test_score_is_not_presented_as_an_accuracy_percentage(self):
  source=(ROOT/'NotesController.java').read_text()
  self.assertIn('Check before saving',source)
  self.assertIn('not an accuracy percentage',source)
  self.assertNotIn(' · confidence "+result.confidence',source)
 def test_copy_is_explicit_and_does_not_commit_or_dismiss(self):
  source=(ROOT/'ImageTextSelectionDialog.java').read_text()
  action=source.split('Button copy=d.icon(',1)[1].split('tools.addView(copy)',1)[0]
  self.assertIn('clipboard.setPrimaryClip',action)
  self.assertNotIn('useText.accept',action)
  self.assertNotIn('dialog.dismiss()',action)
  self.assertIn('copy.setEnabled(n>0)',source)

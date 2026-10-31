"""Safety source contracts; actual behavior is covered by JVM and native journeys."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
class FindAndNames(unittest.TestCase):
 def test_find_is_bounded_off_thread_and_never_adds_persisted_marks(self):
  source=(ROOT/'runtime/src/main/java/app/space/notes/runtime/NoteFindDialog.java').read_text()
  for token in ['worker.submit','task[0].cancel(true)','request!=generation[0]','LengthFilter(256)','setSelection(match.start,match.end)']:
   self.assertIn(token,source)
  self.assertNotIn('setSpan(',source)
 def test_names_are_not_a_change_to_drawing_type(self):
  source=(ROOT/'runtime/src/main/java/app/space/notes/runtime/NotesController.java').read_text()
  self.assertIn('boolean drawing=image.name.equals("Drawing")',source)
  self.assertIn('n.contentName("image:"+image.id,kind)',source)
  self.assertIn('captureUndo(Arrays.asList(current),true);replace(next)',source)
 def test_named_documents_use_new_schema_without_changing_unnamed_output(self):
  source=(ROOT/'runtime/src/main/java/app/space/notes/runtime/DocumentCodec.java').read_text()
  for token in ['schema=6','schema=8','version!=8','version>=7 && n.has("content")','if(version<8','instanceof String']:
   self.assertIn(token,source)
 def test_doc_reverse_conversion_preserves_rich_rows(self):
  source=(ROOT/'domain/src/main/java/app/space/notes/domain/DocumentContent.java').read_text().split('public DocumentContent checklistToText',1)[1].split('public DocumentContent moveChecklistIntoText',1)[0]
  self.assertIn('offset+mark.start',source)
  self.assertNotIn('[x]',source)
  self.assertNotIn('[ ]',source)

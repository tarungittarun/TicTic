"""Source boundaries complement—not replace—native interaction and visual review."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'
class EditorCoherence(unittest.TestCase):
 def test_find_stays_inside_editor_and_keeps_cancellable_work(self):
  s=(JAVA/'NoteFindDialog.java').read_text()
  for token in ['owner.addView(bar','Previous match','Next match','Close find','task.cancel(true)','bar.addOnAttachStateChangeListener(this)','snapshot.set(i,targets.get(i).getText().toString())']:
   self.assertIn(token,s)
  self.assertNotIn('AlertDialog',s)
  self.assertNotIn('setSpan(',s)
 def test_tools_have_visible_labels_and_shared_outline_symbols(self):
  s=(JAVA/'NotesDesign.java').read_text()
  for token in ['editorTool(String text,String name,String symbol','b.setText(text)','b.setTextSize(11)','b.setMinHeight(dp(60))','actionRow(String name','EditorActions.symbol(name)']:
   self.assertIn(token,s)
 def test_document_tools_use_anchored_trays_not_centered_list_screens(self):
  s=(JAVA/'DocumentEditorView.java').read_text()
  self.assertIn('design.showEditorSheet(menu)',s)
  self.assertIn('design.actionRow(labels[i]',s)
  self.assertNotIn('.setItems(labels',s)
  self.assertNotIn('button("⋮⋮"',s)
 def test_system_date_and_time_pickers_are_not_replaced(self):
  s=(JAVA/'NotesController.java').read_text()
  self.assertIn('new android.app.DatePickerDialog',s)
  self.assertIn('new android.app.TimePickerDialog',s)
  self.assertIn('design.show(date)',s)
 def test_back_closes_find_before_leaving_document(self):
  s=(JAVA/'NotesController.java').read_text().split('public boolean back()',1)[1]
  self.assertLess(s.index('NoteFindDialog.close'),s.index('persistDraft(); renderHome()'))

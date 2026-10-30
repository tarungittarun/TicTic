"""Local source contracts; Java and native execution are separate qualification gates."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'
class DocumentEditorUpgradeContracts(unittest.TestCase):
 def setUp(self):
  self.controller=(JAVA/'NotesController.java').read_text()
  self.history=(JAVA/'TextEditHistory.java').read_text()
 def test_typing_history_is_bounded_private_and_keeps_app_formatting(self):
  for token in ['MAX_STEPS=50','MAX_CHARACTERS=500000','TextStyles.read','TextStyles.write','removeComposingSpans','redo.clear()']:
   self.assertIn(token,self.history)
  for forbidden in ['Diagnostics.','Log.','getSharedPreferences','FileOutputStream','http']:
   self.assertNotIn(forbidden,self.history)
 def test_history_application_suppresses_watchers_and_updates_checklist_data(self):
  method=self.controller.split('private boolean stepTextHistory(',1)[1].split('private void syncUndoControl()',1)[0]
  for token in ['binding=true','finally{binding=previous;}','item.change(field.getText().toString(),item.checked,item.depth)','scheduleDraft()','restartInput(field)']:
   self.assertIn(token,method)
 def test_redundant_checklist_commands_are_absent_from_document_and_item_menus(self):
  item=self.controller.split('private void listItemOptions(',1)[1].split('private void listAction(',1)[0]
  editor=self.controller.split('private void renderEditor()',1)[1].split('private void buildFormatting(',1)[0]
  document=editor.split('if(trashed)menuRows(actions',1)[1].split('TextView dates=',1)[0]
  for label in ['Add item below','Move item up','Move item down','Add item']:
   self.assertNotIn('"'+label+'"',item)
   self.assertNotIn('"'+label+'"',document)
  self.assertIn('icon("Checklist options"',editor)
  self.assertIn('button("+ List item"',editor)
 def test_reordering_accepts_local_state_only_and_preserves_item_payload(self):
  method=self.controller.split('private static final class ChecklistDrag',1)[1].split('private void listItemOptions(',1)[0]
  self.assertIn('event.getLocalState() instanceof ChecklistDrag',method)
  self.assertNotIn('DRAG_FLAG_GLOBAL',method)
  self.assertNotIn('getClipData()',method)
  self.assertIn('noteId.equals(editing)',method)
  self.assertIn('ChecklistOrder.move(note.items,itemId,insertion)',method)
  self.assertIn('if(moved==note.items)return',method)
  self.assertIn('"Position "+(i+1)',method)

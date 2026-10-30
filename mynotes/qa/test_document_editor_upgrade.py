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
  self.assertIn('application/vnd.mynotes.checklist-item',method)
  self.assertNotIn('ClipData.newPlainText',method)
  self.assertNotIn('getClipData()',method)
  self.assertIn('noteId.equals(editing)',method)
  self.assertIn('ChecklistOrder.move(note.items,itemId,insertion)',method)
  self.assertIn('if(moved==note.items){checklistDragState="no-op";return;}',method)
  self.assertLess(method.index('if(moved==note.items)'),method.index('captureUndo('))
  self.assertIn('"Position "+(i+1)',method)

 def test_open_with_and_drop_are_local_read_only_reviewed_intake(self):
  activity=(ROOT/'app/src/main/java/app/space/notes/NotesActivity.java').read_text()
  for token in ['Intent.ACTION_VIEW','intent.getData()!=null','installDocumentDrop','getItemCount()!=1','LocalBackupFiles.requireLocal(uri)','requestDragAndDropPermissions(event)','permissions::release','Intent.EXTRA_LOCAL_ONLY,true']:
   self.assertIn(token,activity)
  grant=self.controller.split('public void openNoteDocument(',1)[1].split('public void importBackup(',1)[0]
  self.assertIn('busy || prepared!=null',grant)
  for mode in ['drawingPad!=null','imageEditor!=null','diagramOpen','ocrPanel','capture!=null']:
   self.assertIn(mode,grant)
  self.assertIn('deferredResults.size()>=8',grant)
  self.assertIn('finally {main.post(release);}',self.controller)
  self.assertIn('for(DocumentGrant grant:new ArrayList<>(documentGrants))grant.run()',self.controller)
 def test_file_location_lookup_never_materializes_or_fabricates_a_file(self):
  files=(JAVA/'LocalNotebookFolders.java').read_text()
  method=files.split('public String location(',1)[1].split('/** Call only after journalling',1)[0]
  for forbidden in ['createDocument(','renameDocument(','deleteDocument(','timestampName(']:
   self.assertNotIn(forbidden,method)
  self.assertIn('No verified individual note file found',method)
  self.assertIn('not a filesystem path',method)
  repository=(JAVA/'NotesRepository.java').read_text()
  method=repository.split('public String documentLocation(',1)[1].split('private boolean exists()',1)[0]
  self.assertIn('not an individual note file',method)
  self.assertNotIn('Diagnostics.',method)

 def test_document_drop_is_intercepted_before_native_text_uri_coercion(self):
  shell=(JAVA/'LibraryGestureLayout.java').read_text()
  method=shell.split('public boolean dispatchDragEvent(',1)[1].split('LibraryGestureLayout(Context',1)[0]
  self.assertIn('event.getLocalState()==null',method)
  self.assertLess(method.index('documentDrop.onDrag(this,event)'),method.index('super.dispatchDragEvent(event)'))
  self.assertIn('if(event.getLocalState() instanceof ChecklistDrag)return false',self.controller)
 def test_ruling_uses_existing_saved_background_and_native_line_geometry(self):
  self.assertIn('((WritingField)body).ruling(n.background==3',self.controller)
  self.assertIn('getLineBounds(line,bounds)',self.controller)
  self.assertIn('textRuling', (JAVA/'NoteBackground.java').read_text())
  self.assertIn('if(repository.appOnlyStorage())',self.controller)

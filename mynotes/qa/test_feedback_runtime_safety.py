"""Read-only performance and async navigation boundaries; native validation is separate."""
from pathlib import Path
import unittest
JAVA=Path(__file__).resolve().parents[1]/'runtime/src/main/java/app/space/notes/runtime'
class FeedbackRuntimeSafety(unittest.TestCase):
 def test_selection_avoids_whole_board_and_image_rebuild(self):
  text=(JAVA/'NotesBoard.java').read_text();self.assertIn('if(rows.equals(previous))',text);self.assertIn('adapter.notifyItemChanged(i,"selection")',text);self.assertIn('if(h.previewImage!=image)',text)
 def test_late_export_and_storage_transactions_cannot_reopen_old_settings(self):
  text=(JAVA/'NotesController.java').read_text()
  for start,end in [('public void exportBackup(', 'public void exportFolderCopy('),('private void moveStorage(', 'private void chooseAppFolderCategories(')]:
   part=text.split(start,1)[1].split(end,1)[0];self.assertIn('callerGeneration=fileViewGeneration',part);self.assertIn('callerGeneration==fileViewGeneration',part);self.assertIn('pageBack=callerBack',part)
 def test_selection_is_geometric_and_limits_are_explicit(self):
  text=(JAVA/'LocalPdfPreview.java').read_text();self.assertIn('PdfTextLayer selection(int page)',text);self.assertIn('source.getRotation()%360!=0',text);self.assertIn('p.getDir()!=0',text);self.assertIn('glyphs.size()>=20000',text);self.assertIn('token!=current.token',text)
  text=(JAVA/'PdfSelectionImage.java').read_text();self.assertIn('imageToView().mapRect',text);self.assertIn('ActionMode.TYPE_FLOATING',text);self.assertIn('setPrimaryClip',text);self.assertIn('handle==0',text)

 def test_header_back_bypasses_only_mutation_guard_and_keeps_safe_back_dispatch(self):
  text=(JAVA/'NotesController.java').read_text()
  header=text.split('private void headerIn(',1)[1].split('private void decorateLibrary(',1)[0]
  self.assertIn('design.icon("Back","back"',header)
  self.assertIn('back();',header)
  self.assertIn('private void act(String label,Runnable action){if(busy)',text)
  back=text.split('public boolean back()',1)[1]
  self.assertLess(back.index('if(pageBack!=null)'),back.index('if(busy)'))
 def test_document_keyboard_request_is_bounded_current_field_and_detach_scoped(self):
  text=(JAVA/'DocumentEditorView.java').read_text()
  for fragment in ['attempts++>=20','uiGeneration!=generation','fields.containsValue(focus)','focus.hasWindowFocus()','keyboard.isActive(focus)','acknowledged && visible','servedFrames>=2','controller.show(WindowInsets.Type.ime())','onDetachedFromWindow(){stopKeyboardStartup()','uiGeneration++;stopKeyboardStartup()']:
   self.assertIn(fragment,text)

 def test_accepted_document_keyboard_request_never_reissues_against_a_later_hide(self):
  text=(JAVA/'DocumentEditorView.java').read_text()
  self.assertIn('if(!acknowledged && focus.hasFocus() && focus.hasWindowFocus()',text)
  self.assertIn('acknowledged=keyboard.showSoftInput',text)

 def test_home_hover_latches_target_until_card_center_leaves_the_original_bounds(self):
  text=(JAVA/'NotesBoard.java').read_text()
  for fragment in ['dragTargetBounds.contains(Math.round(cx),Math.round(cy))','!repeated && canDropOver','dragLastTarget=((Note)rows.get(to)).id','dragTargetBounds.set(b.itemView.getLeft()','dragLastTarget=null;dragTargetBounds.setEmpty()']:
   self.assertIn(fragment,text)
 def test_document_focus_recovery_never_steals_another_views_focus(self):
  text=(JAVA/'DocumentEditorView.java').read_text()
  self.assertIn('actual!=null && actual!=focus',text)
  self.assertIn('focus.isShown() && focus.getWidth()>0',text)
  self.assertIn('draggingContent || readOnly()',text)

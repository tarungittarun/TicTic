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

 def test_conversion_is_in_insert_not_note_management(self):
  s=(JAVA/'NotesController.java').read_text();editor=s.split('private void renderEditor()',1)[1].split('private void buildFormatting',1)[0]
  self.assertIn('captureGrid(capture,new String[]{"Convert to checklist","Reminder"}',editor)
  management=editor.split('LinearLayout actions=menu("Note actions"',1)[1]
  self.assertNotIn('new String[]{"Convert to checklist"}',management)
 def test_find_compacts_without_shrinking_touch_controls(self):
  s=(JAVA/'NoteFindDialog.java').read_text()
  self.assertIn('owner.addOnLayoutChangeListener(resizeListener)',s)
  self.assertIn('statusRow.setVisibility(View.GONE)',s)
  self.assertIn('owner.removeOnLayoutChangeListener(resizeListener)',s)
 def test_basic_document_formatting_is_nonmodal_and_docked(self):
  s=(JAVA/'DocumentEditorView.java').read_text();part=s.split('private void formatMenu()',1)[1].split('private String fontLabel',1)[0]
  self.assertIn('InlineFormatTray dialog',part)
  self.assertNotIn('AlertDialog',part)
  self.assertIn('tray.setTag("document-formatting")',s)

 def test_content_removal_is_last_and_rename_edit_are_direct(self):
  s=(JAVA/'DocumentEditorView.java').read_text().split('private void blockMenu',1)[1].split('private void moveMenu',1)[0]
  self.assertLess(s.index('options.add("Edit")'),s.index('options.add("Rename")'))
  self.assertLess(s.index('options.add("Rename")'),s.index('options.add("Remove section")'))
  self.assertIn('choose(title,options.toArray',s)

 def test_properties_keep_editor_and_have_fixed_actions_outside_scroll(self):
  s=(JAVA/'NotesController.java').read_text()
  for name,end in [('renderLabels','manageLabels'),('renderColours','drawingDraft'),('renderBackgrounds','renderFolders')]:
   part=s.split('private void '+name+'()',1)[1].split('private ',1)[0]
   self.assertNotIn('clearPage()',part)
   self.assertIn('sheets.panel(',part)
  sheets=(JAVA/'NotesSheets.java').read_text()
  self.assertIn('footers.get(panel)',sheets)
  self.assertIn('surface.addView(currentFooter',sheets)
  self.assertIn('footers.clear()',sheets)
 def test_basic_styles_can_be_combined_in_one_open_ribbon(self):
  s=(JAVA/'DocumentEditorView.java').read_text()
  part=s.split('String[] types={"bold"',1)[1].split('panel.addView(design.label("Paragraph"',1)[0]
  self.assertNotIn('dialog.dismiss()',part)
  self.assertIn('dialog.toggles.put(type,control)',part)
  self.assertIn('refreshFormatRibbon()',s)

 def test_footer_reflows_by_measured_scaled_words_not_smaller_font(self):
  s=(JAVA/'NotesController.java').read_text()
  self.assertIn('button.getPaint().measureText(button.getText().toString())',s)
  self.assertIn('reflowEditorDock(dock)',s)
  design=(JAVA/'NotesDesign.java').read_text().split('public Button editorTool',1)[1].split('public Button actionRow',1)[0]
  self.assertIn('b.setMaxLines(1)',design)
  self.assertNotIn('setTextScaleX',design)

 def test_folder_move_is_anchored_and_late_disk_results_cannot_reopen_it(self):
  s=(JAVA/'NotesController.java').read_text()
  part=s.split('private void renderMoveFolderSheet()',1)[1].split('private void renderFolders(boolean moving,Runnable parent)',1)[0]
  self.assertNotIn('clearPage()',part)
  self.assertNotIn('title=null',part)
  self.assertIn('sheets.footer(panel,button("Cancel",()->sheets.close()))',part)
  self.assertIn('token!=folderPageSequence || !panel.isShown()',part)
  self.assertIn('loadMoveFolders(panel,content,chosen)',part)

 def test_colour_targets_fit_the_short_landscape_scroll_view_without_smaller_text(self):
  s=(JAVA/'NotesController.java').read_text().split('private void renderColours()',1)[1].split('private java.io.File drawingDraft',1)[0]
  self.assertIn('pick.setMinHeight(dp(72))',s)
  self.assertIn('pick.setPadding(dp(4),dp(4),dp(4),dp(4))',s)
  self.assertIn('pick.setTextSize(13)',s)
  native=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  self.assertIn('assertEquals(text("Reminder").getHeight(),visible.height())',native)

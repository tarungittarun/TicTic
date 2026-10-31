"""New interaction contracts; native and domain tests separately verify actual behaviour."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'
class WordFlowRevision(unittest.TestCase):
 def test_insert_has_no_text_action(self):
  part=(JAVA/'DocumentEditorView.java').read_text().split('private void insertMenu(boolean',1)[1].split('private static DocumentContent.Block',1)[0]
  self.assertNotIn('"Text"',part)
  self.assertIn('"Paste table"',part)
 def test_table_handle_only_has_four_scoped_actions_and_cut_is_deferred(self):
  s=(JAVA/'DocumentEditorView.java').read_text()
  self.assertIn('new String[]{"Rename","Copy","Cut","Delete"}',s)
  cut=s.split('private void tableHandleMenu',1)[1].split('private void pasteTable',1)[0]
  self.assertIn('cutTable=choice==2?id:null',cut)
  self.assertNotIn('all.remove(cutTable)',cut)
  self.assertIn('DocumentContent next=all.insertAtText',s)
 def test_table_structure_and_width_are_in_overflow(self):
  s=(JAVA/'DocumentEditorView.java').read_text()
  for token in ['"Table options","more"','"Edit table","edit"','"Rows and columns"','"Fit to content"','"Fixed column width"','cell.setBackground(null)']:
   self.assertIn(token,s)
  self.assertNotIn('"Table rows and columns"',s)
 def test_table_sizing_is_lossless_and_versioned(self):
  codec=(JAVA/'ContentCodec.java').read_text();root=(JAVA/'DocumentCodec.java').read_text()
  self.assertIn('out.put("columnWidth",block.table.columnWidth)',codec)
  self.assertIn('integer(in,"columnWidth")',codec)
  self.assertIn('schema=9',root)
  self.assertIn('version<9',root)
 def test_hyperlink_is_one_form_with_no_partial_invalid_write(self):
  s=(JAVA/'DocumentEditorView.java').read_text().split('private void linkMenu()',1)[1].split('private void convertSelection',1)[0]
  for token in ['"Text to display"','"Web address"','"Save link"','RichLinks.replace','original.same','field.setText(linked)']:
   self.assertIn(token,s)
  self.assertNotIn('prompt(',s)
 def test_format_has_close_cross_and_real_selectors(self):
  s=(JAVA/'DocumentEditorView.java').read_text()
  self.assertIn('"Close formatting","close"',s)
  self.assertNotIn('scopeView.setText(range?',s)
  for token in ['dropdown("Font"','dropdown("Size (8–72 sp)"','border.setStroke(dp(1),design.outline)','Format subsequent typing','Format selected text']:
   self.assertIn(token,s)
 def test_zoom_is_bounded_and_view_only(self):
  s=(JAVA/'VisualZoomDialog.java').read_text()
  for token in ['drawing?5:3','design),5,"Diagram"','Math.max(1,Math.min(maximum,value))','ScaleGestureDetector','Zoom out','Reset','Close zoom']:
   self.assertIn(token,s)
  self.assertNotIn('host.change',s)
 def test_table_ruling_is_excluded_but_text_guides_remain(self):
  s=(JAVA/'DocumentEditorView.java').read_text()
  self.assertIn('!key.startsWith("cell:") && host.current().content.writingLines',s)
  pdf=(JAVA/'PdfExporter.java').read_text()
  self.assertNotIn('guides(canvas,cell,',pdf)
  self.assertIn('if(rules)guides(canvas,layout,',pdf)
 def test_default_writing_looks_like_an_edit_field_not_an_insert_command(self):
  s=(JAVA/'DocumentEditorView.java').read_text().split('private EditText writingPlaceholder',1)[1].split('private void continueWriting',1)[0]
  self.assertIn('new EditText',s)
  self.assertIn('setHint("Write text…")',s)
  self.assertIn('!writing.isAttachedToWindow()',s)
  self.assertNotIn('host.change',s)
 def test_stale_dialogs_cannot_mutate_a_replaced_editor(self):
  s=(JAVA/'DocumentEditorView.java').read_text()
  for token in ['uiGeneration++','requireCurrentPage(offered)','uiGeneration!=linkGeneration','linkSnapshot.equals(host.current().content.fingerprint())','fields.get(field.getTag().toString())!=field']:
   self.assertIn(token,s)
 def test_explicit_diagnostic_deletion_is_confirmed(self):
  s=(JAVA/'NotesController.java').read_text().split('settingRow(content,"Delete diagnostic logs"',1)[1].split('content.addView(text(',1)[0]
  self.assertLess(s.index('confirmSheet('),s.index('Diagnostics.clear()'))
  self.assertIn('cannot be undone',s)

class NativeFailureRegressions(unittest.TestCase):
 def test_notes_dock_measures_capacity_instead_of_fixed_three_tool_row(self):
  source=(JAVA/'NotesController.java').read_text().split('private void reflowEditorDock',1)[1].split('private void showNoteFind',1)[0]
  self.assertIn('width/Math.max(dp(48),needed)',source)
  self.assertIn('i%columns==0',source)
 def test_drag_keeps_keyboard_and_caret_reveal_out_of_native_drag(self):
  source=(JAVA/'DocumentEditorView.java').read_text()
  self.assertIn('draggingContent=true',source)
  self.assertIn('private void revealCaret(){if(draggingContent)return;',source)
  self.assertIn('hideSoftInputFromWindow(getWindowToken(),0)',source)
 def test_link_actions_notify_native_menu_and_are_prioritized(self):
  source=(JAVA/'DocumentEditorView.java').read_text()
  self.assertNotIn('addLinkActions(input,menu);return false;',source)
  self.assertIn('"Open link").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)',source)
  self.assertIn('"Copy link").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)',source)

 def test_link_long_press_uses_touched_character_not_a_floating_toolbar_assumption(self):
  source=(JAVA/'DocumentEditorView.java').read_text()
  self.assertIn('input.setOnLongClickListener',source)
  self.assertIn('input.getOffsetForPosition(event.getX(),event.getY())',source)
  self.assertIn('choose("Link",new String[]{"Open link","Copy link"}',source)

 def test_compact_threshold_does_not_feed_back_its_own_padding_change(self):
  source=(JAVA/'NotesController.java').read_text()
  self.assertIn('writingDock.getMeasuredHeight()-writingDock.getPaddingTop()-writingDock.getPaddingBottom()+dp(12)',source)

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
 def test_explicit_diagnostic_deletion_is_confirmed(self):
  s=(JAVA/'NotesController.java').read_text().split('settingRow(content,"Delete diagnostic logs"',1)[1].split('content.addView(text(',1)[0]
  self.assertLess(s.index('confirmSheet('),s.index('Diagnostics.clear()'))
  self.assertIn('cannot be undone',s)

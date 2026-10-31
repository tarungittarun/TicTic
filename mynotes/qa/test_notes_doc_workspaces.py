"""Source contracts supplement, not replace, model/native/user qualification."""
import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class NotesDocWorkspaces(unittest.TestCase):
 def test_simple_creation_has_no_global_opt_in(self):
  controller=(ROOT/'runtime/src/main/java/app/space/notes/runtime/NotesController.java').read_text()
  self.assertNotIn('canonical-default',controller)
  for token in ['documentsWorkspace','Workspace.includes(n,documents)','Copy to Doc','Workspace.copyToDocument','candidate.add(upgraded)','"New Doc"','"New document"']:
   self.assertIn(token,controller)
 def test_header_has_strict_codec_and_fingerprint(self):
  codec=(ROOT/'runtime/src/main/java/app/space/notes/runtime/ContentCodec.java').read_text()
  self.assertIn('bool(in,"header")',codec)
  self.assertIn('if(block.table.hasHeader)out.put("header",true)',codec)
  model=(ROOT/'domain/src/main/java/app/space/notes/domain/DocumentContent.java').read_text()
  self.assertIn('if(block.table.hasHeader)part(out,"header row")',model)
 def test_inline_preview_and_contextual_moves(self):
  editor=(ROOT/'runtime/src/main/java/app/space/notes/runtime/DocumentEditorView.java').read_text()
  for token in ['new DiagramPreview','"Cell actions"','"Move content"','moveIntoText','"Header row · On"']:
   self.assertIn(token,editor)

 def test_doc_tools_are_adaptive_not_horizontally_hidden(self):
  editor=(ROOT/'runtime/src/main/java/app/space/notes/runtime/DocumentEditorView.java').read_text()
  self.assertNotIn('horizontally(this',editor)
  for token in ['compactButton','if(large && i==3)','Continue writing…','Move · choose destination','BlockBoundaryLine','Text to display','Remove link']:
   self.assertIn(token,editor)

 def test_home_hint_cannot_push_the_dock_out_of_view(self):
  controller=(ROOT/'runtime/src/main/java/app/space/notes/runtime/NotesController.java').read_text()
  self.assertIn('libraryArea.addView(emptyHint,new FrameLayout.LayoutParams(-1,-1))',controller)
  self.assertNotIn('root.addView(emptyHint)',controller)
  self.assertIn('viewportVisibility(homeWorkspaceChrome,keyboardVisible()',controller)
 def test_empty_doc_has_a_writing_recovery_path(self):
  editor=(ROOT/'runtime/src/main/java/app/space/notes/runtime/DocumentEditorView.java').read_text()
  self.assertIn('note.content.blocks.isEmpty()',editor)
  self.assertIn('"Start writing"',editor)

 def test_cell_context_requires_exact_identity(self):
  editor=(ROOT/'runtime/src/main/java/app/space/notes/runtime/DocumentEditorView.java').read_text()
  self.assertIn('!id.equals(selectedBlock)',editor)

 def test_table_cells_have_legible_borders_and_uniform_rows(self):
  editor=(ROOT/'runtime/src/main/java/app/space/notes/runtime/DocumentEditorView.java').read_text()
  self.assertIn('cellBorder.setStroke(dp(1),design.outline)',editor)
  self.assertIn('cell.setPadding(dp(6),dp(8),dp(6),dp(8))',editor)
  self.assertIn('row.addView(cell,new LayoutParams(dp(160),-1))',editor)

 def test_workspace_journey_waits_for_new_editor_ime_before_back(self):
  source=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  journey=source.split('void notesAndDocCreationRemainSeparateEvenAfterAnOldOptIn()',1)[1].split('@Test',1)[0]
  self.assertIn('documentImeReady();tap("Back")',journey)
  self.assertIn('editorImeReady();tap("Back")',journey)
  self.assertIn('keyboardVisibleForTest() && controller.view().hasWindowFocus()',source)

"""Scope/data safety source guards; native/JVM qualification is separate."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
class WritingStage(unittest.TestCase):
 def test_aa_has_visible_controls_and_explicit_scope(self):
  s=(ROOT/'runtime/src/main/java/app/space/notes/runtime/DocumentEditorView.java').read_text()
  panel=s.split('private void formatMenu(){')[1].split('private void applyFormat(')[0]
  for token in ['Selected text','New typing','Bold','Italic','Underline','Heading 1','Heading 2','Font','Size (8–72 sp)','design.choice(on)','setStateDescription','ScrollView','fontScale>=1.5f','Decrease text size','Increase text size']:
   self.assertIn(token,panel)
 def test_checklist_names_survive_all_editor_mutations_and_codec(self):
  editor=(ROOT/'runtime/src/main/java/app/space/notes/runtime/DocumentEditorView.java').read_text()
  self.assertNotIn('Block.checklist(block,rows',editor)
  self.assertNotIn('Block.checklist(block.id,rows',editor)
  codec=(ROOT/'runtime/src/main/java/app/space/notes/runtime/ContentCodec.java').read_text()
  self.assertIn('Block.checklist(id,name,items',codec)
  self.assertIn('description=label(list)+", list item "+position',editor)
  self.assertIn('Block.checklist(id,block.name,items',(ROOT/'domain/src/main/java/app/space/notes/domain/NoteOperations.java').read_text())
 def test_checklist_drag_is_local_and_has_preview_and_alternative(self):
  editor=(ROOT/'runtime/src/main/java/app/space/notes/runtime/DocumentEditorView.java').read_text()
  for token in ['ChecklistInsertionLine','getLineStart','moveIntoText','Cancel move','Move checklist','event.getLocalState() instanceof BlockDrag','input.setForeground(null)','scroll.getHeight()/5']:
   self.assertIn(token,editor)
  self.assertNotIn('DRAG_FLAG_GLOBAL',editor)
 def test_no_plain_text_management_header_or_schema_bump(self):
  editor=(ROOT/'runtime/src/main/java/app/space/notes/runtime/DocumentEditorView.java').read_text()
  self.assertIn('if(block.kind==DocumentContent.Kind.TEXT){header.setVisibility(GONE)',editor)
  self.assertIn('Text section options',editor)
  self.assertIn('Show move handle',editor)
  codec=(ROOT/'runtime/src/main/java/app/space/notes/runtime/DocumentCodec.java').read_text()
  self.assertIn('schema=7',codec)

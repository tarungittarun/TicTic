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

 def test_all_mynotes_junit_helpers_have_imports(self):
  import re
  from mynotes.tools.ci.test_plan import mask_comments_and_strings
  helpers={'assertEquals','assertNull','assertNotNull','assertTrue','assertFalse','assertSame','assertNotSame','assertArrayEquals','assertThat','assertThrows','assertNotEquals','fail'}
  paths=list((ROOT/'domain/src/test/java').rglob('*.java'))+list((ROOT/'app/src/androidTest/java').rglob('*.java'))
  self.assertGreater(len(paths),20)
  for path in paths:
   source=mask_comments_and_strings(path.read_text())
   imports=set(re.findall(r'import static org\.junit\.Assert\.(\w+|\*);',source))
   if '*' in imports:continue
   used={name for name in helpers if re.search(r'(?<![\w.])'+name+r'\s*\(',source)}
   self.assertFalse(used-imports,str(path)+': '+str(used-imports))

 def test_canonical_metadata_validation_does_not_clone_embedded_pixels(self):
  source=(ROOT/'runtime/src/main/java/app/space/notes/runtime/DocumentCodec.java').read_text()
  self.assertIn('ContentCodec.metadata(n)',source)
  self.assertNotIn('new JSONObject(n.toString())',source)

 def test_android16_is_full_and_all_four_profiles_are_required(self):
  workflow=(ROOT.parent/'.github/workflows/notes-refinement-native.yml').read_text()
  self.assertIn("id: current-api36, api: 36",workflow)
  self.assertIn("group: all}",workflow.split('id: current-api36',1)[1])
  self.assertIn('if len(peers)==3',workflow)
  self.assertIn("'refinement-native-current-api36'",workflow)
  self.assertIn("'landscape-api34','current-api36'}-set(results)",workflow)
  self.assertNotIn("if: always() && matrix.group == 'all'",workflow)

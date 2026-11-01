"""New revision boundaries plus independent exhaustive CI partition verification."""
from pathlib import Path
import unittest
from mynotes.tools.ci.test_plan import build_plan,cases
from mynotes.tools.ci.refinement_plan import selected_cases
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'
class AdaptiveEditorRevision(unittest.TestCase):
 def test_full_inventory_is_disjoint_exhaustive_and_splits_core_journeys(self):
  plan=build_plan();a=set(selected_cases(plan,'all',0));b=set(selected_cases(plan,'all',1))
  self.assertFalse(a&b);self.assertEqual(set(cases(plan['android'])),a|b)
  for shard in [a,b]:self.assertTrue(any(x.startswith('app.space.notes.CoreJourneysTest#') for x in shard))
  self.assertLess(abs(len(a)-len(b)),15)
 def test_focused_profile_shards_are_also_disjoint_and_exhaustive(self):
  plan=build_plan();a=set(selected_cases(plan,'focused',0));b=set(selected_cases(plan,'focused',1));self.assertFalse(a&b);self.assertEqual(set(selected_cases(plan,'focused')),a|b)
 def test_shared_format_and_grouped_insert_used_by_both_editors(self):
  for file in ['DocumentEditorView.java','NotesController.java']:
   source=(JAVA/file).read_text();self.assertIn('new FormatControls(',source);self.assertIn('EditorRails.install(',source)
  self.assertIn('InsertPalette.show(', (JAVA/'DocumentEditorView.java').read_text())
  self.assertIn('InsertPalette.grid(', (JAVA/'NotesController.java').read_text())
 def test_hyperlink_insert_only_and_multiline(self):
  source=(JAVA/'DocumentEditorView.java').read_text();dock=source.split('String[] glyphs=',1)[1].split('compactDocument=',1)[0]
  self.assertNotIn('Hyperlink',dock);self.assertNotIn('linkMenu',dock)
  for file in ['DocumentEditorView.java','NotesController.java']:
   source=(JAVA/file).read_text();self.assertIn('"Save hyperlink"',source);self.assertNotIn('label.setSingleLine',source);self.assertIn('RichLinks.replace',source)
 def test_full_stored_zoom_and_vector_redraw(self):
  source=(JAVA/'VisualZoomDialog.java').read_text();self.assertIn('Math.max(image.width,image.height)',source);self.assertIn('getChildAt(0).draw(canvas)',source)
  self.assertIn('this(c,image,768)',(JAVA/'NoteImageView.java').read_text())
 def test_real_rails_are_scrollable_and_actions_not_duplicated(self):
  source=(JAVA/'EditorRails.java').read_text()
  for token in ['root.removeView(top)','root.removeView(writing)','root.removeView(dock)','new ScrollView','Gravity.BOTTOM','Collections.reverse(actions)','showPanel','hidePanel']:self.assertIn(token,source)
 def test_overflow_is_top_right_and_no_doc_writing_prompt(self):
  source=(JAVA/'NotesController.java').read_text().split('private void renderEditor()',1)[1].split('private void reflowEditorDock',1)[0];self.assertIn('top.addView(more,',source);self.assertNotIn('dock.addView(more,',source)
  self.assertNotIn('Write text…',(JAVA/'DocumentEditorView.java').read_text())

 def test_keyboard_preparation_hides_ime_without_back_navigation(self):
  source=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text().split('private void closeKeyboard(){',1)[1].split('private String debugMarks',1)[0]
  self.assertIn('hideSoftInputFromWindow',source);self.assertNotIn('KEYCODE_BACK',source)
  for token in ['WindowInsetsAnimation.Callback','imeAnimation.get()','stable[0]>=12','setWindowInsetsAnimationCallback(null)']:self.assertIn(token,source)
 def test_find_can_reveal_title_but_short_rail_viewport_prioritizes_writing(self):
  source=(JAVA/'NotesController.java').read_text().split('private void adaptEditorViewport()',1)[1].split('private boolean viewportVisibility',1)[0]
  self.assertIn('shell.getHeight()<dp(240) && !title.hasFocus()',source)
  find=(JAVA/'NoteFindDialog.java').read_text();self.assertIn('"editor-landscape-rails".equals(owner.getTag())',find);self.assertIn('field.setVisibility(View.VISIBLE);field.requestFocus()',find)

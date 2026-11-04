"""Source guardrails complement (do not replace) native interaction/data assertions."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'

class ContentControlsRevision(unittest.TestCase):
 def editor(self):return (JAVA/'DocumentEditorView.java').read_text()
 def test_collapsed_list_inserts_at_caret_before_range_conversion(self):
  source=self.editor().split('private void convertSelection()',1)[1].split('private InlineFormatTray',1)[0]
  self.assertLess(source.index('if(start==end)'),source.index('selectionToChecklist'))
  self.assertIn('insertionUsesCaret=true',source)
  self.assertIn('insert(list);return;',source)
  self.assertIn('start<0 || end<start',source)
 def test_grips_are_not_content_management_launchers(self):
  source=self.editor().split('private void renderBlock',1)[1].split('private void changeRow',1)[0]
  self.assertIn('Hold the grip, then drag',source)
  self.assertIn('contentActions(block.id)',source)
  self.assertIn('contentOptions(block.id)',source)
  self.assertNotIn('()->blockMenu(block.id)',source)
  self.assertNotIn('"Move item"',source)
  self.assertIn('addAccessibilityAction(handle,"Move item earlier"',source)
  self.assertIn('addAccessibilityAction(handle,"Move item later"',source)
 def test_item_hold_and_feedback_respect_system_preferences(self):
  source=self.editor().split('private void dragHandle',1)[1].split('private Button compactButton',1)[0]
  self.assertIn('postDelayed(begin,android.view.ViewConfiguration.getLongPressTimeout())',source)
  self.assertIn('source instanceof BlockDrag',source)
  self.assertIn('NotesMotion.lift(lifted)',source)
  motion=(JAVA/'NotesMotion.java').read_text()
  for token in ['ValueAnimator.areAnimatorsEnabled()','HapticFeedbackConstants.LONG_PRESS','HapticFeedbackConstants.CLOCK_TICK','if(!enabled())return']:
   self.assertIn(token,motion)
  self.assertNotIn('FLAG_IGNORE_GLOBAL_SETTING',motion)
 def test_distinct_item_grip_has_four_dots(self):
  source=(JAVA/'NotesIcon.java').read_text().split('case "drag-item":',1)[1].split('case "drag":',1)[0]
  self.assertIn('y=9;y<=15;y+=6',source)
  self.assertIn('"drag-item"',self.editor())
  self.assertIn('"drag-item"',(JAVA/'NotesController.java').read_text())
 def test_single_raised_format_panel_and_fifth_clear(self):
  source=(JAVA/'FormatControls.java').read_text()
  for editor in ['DocumentEditorView.java','NotesController.java']:self.assertIn('new FormatControls(', (JAVA/editor).read_text())
  self.assertIn('"bold","italic","underline","strike","clear"',source)
  self.assertNotIn('More text options',source)
  self.assertNotIn('"Style"',source)
  self.assertNotIn('"Hyperlink"',source)
  self.assertIn('tray.setElevation(dp(12))',self.editor())
  self.assertIn('raised.setStroke(dp(1),design.outline)',self.editor())
 def test_size_dropdown_and_both_steppers_share_row(self):
  source=(JAVA/'FormatControls.java').read_text().split('panel.addView(design.label("Size"',1)[1].split('panel.addView(design.divider())',1)[0]
  for token in ['sizes.addView(sizeChoice','sizes.addView(smaller','sizes.addView(bigger']:
   self.assertIn(token,source)
 def test_clipboard_copy_reidentifies_and_cut_is_deferred(self):
  source=self.editor().split('private void contentActions',1)[1].split('private void blockMenu',1)[0]
  actions,paste=source.split('private void pasteTable',1)
  self.assertNotIn('all.remove(cutTable)',actions)
  self.assertIn('value=ContentCopies.copy(value)',paste)
  self.assertIn('Cut, paste within the original document',paste)
  self.assertLess(paste.index('DocumentContent next=all.insertAtText'),paste.index('cutNote=null;cutTable=null'))
  copies=(ROOT/'domain/src/main/java/app/space/notes/domain/ContentCopies.java').read_text()
  for token in ['row.checked,row.depth,row.marks','image.width,image.height,image.data','source.diagram','source.table']:
   self.assertIn(token,copies)
 def test_numbering_works_in_both_editors_and_does_not_interpret_paste(self):
  for name in ['DocumentEditorView.java','NotesController.java']:
   source=(JAVA/name).read_text()
   self.assertIn('AutoListContinuation.afterEnter',source)
   self.assertIn('AutoListContinuation.beforeBackspace',source)
  self.assertIn('editCount[0]==1',self.editor())
  source=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  for name in ['listToolbarAtCollapsedCaretInsertsWithoutCrash','wholeChecklistCopyAndDeferredCut','contentPenHasManagement','typedNumberingContinuesAndSingleBackspace','typedNumberingAndEmptyEntryEscape']:
   self.assertIn(name,source)

 def test_apk_name_matches_incremented_manifest_versions(self):
  import re
  gradle=(ROOT/'app/build.gradle').read_text()
  version=re.search(r"versionName '([^']+)'",gradle).group(1)
  code=re.search(r'versionCode (\d+)',gradle).group(1)
  workflow=(ROOT.parent/'.github/workflows/notes-candidate64-apk.yml').read_text()
  names=set(re.findall(r'MyNotes-v[0-9.]+-\d+-development\.apk',workflow))
  self.assertEqual({f'MyNotes-v{version}-{code}-development.apk'},names)

 def test_motion_and_input_protocol_are_qualified_not_only_static_screenshots(self):
  workflow=(ROOT.parent/'.github/workflows/notes-refinement-native.yml').read_text()
  self.assertIn('motion: 1, shard: 0, group: all}',workflow)
  self.assertIn('animator_duration_scale ${{ matrix.motion }}',workflow)
  self.assertIn('-e expectedMotion ${{ matrix.motion }}',workflow)
  self.assertIn('app.space.notes.runtime.NotesMotionTest',(ROOT/'tools/ci/refinement_plan.py').read_text())
  native=(ROOT/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
  self.assertIn('connection.deleteSurroundingText(1,0)',native)
  self.assertIn('Caret follows generated prefix',native)

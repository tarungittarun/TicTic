"""Local source guardrails; these do not establish Android runtime acceptance."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'runtime/src/main/java/app/space/notes/runtime'

class RefinementHierarchyContracts(unittest.TestCase):
    def setUp(self):
        self.source = (JAVA / 'NotesController.java').read_text()

    def test_trash_never_constructs_creation_controls_outside_its_guard(self):
        home = self.source.split('private void renderHome(){', 1)[1].split('private void saveViewPreferences()', 1)[0]
        dock = home.split('LinearLayout dock=tools(root);', 1)[1].split('homeChrome.clear();', 1)[0]
        self.assertNotIn('top.addView(library', home)
        self.assertLess(dock.index('dock.addView(library'), dock.index('if(!page.equals("Trash"))'))
        creation = dock.split('if(!page.equals("Trash")){', 1)[1]
        for token in ['"+ Checklist"', '"Record voice"', '"New text note"', 'dock.addView(create);']:
            self.assertIn(token, creation)
        self.assertTrue(creation.rstrip().endswith('}'))

    def test_confirmation_primary_matches_its_scope(self):
        confirm = self.source.split('private void confirmSheet(', 1)[1].split('private void drawingHelp()', 1)[0]
        self.assertEqual(1, confirm.count('design.primary(action)'))
        self.assertIn('positive.equals("Clear canvas")?"eraser":"trash"', confirm)
        self.assertIn('design.primaryInk', confirm)
        self.assertIn('button("Cancel",()->sheets.close())', confirm)

    def test_group_requires_two_shapes_without_dismissing_for_validation(self):
        group = self.source.split('private void groupShapes(){', 1)[1].split('private void chooseArrow()', 1)[0]
        for token in ['ids.size()>=2', 'group.setEnabled(ready)', 'validate.run()', 'ungroup.setEnabled(grouped)']:
            self.assertIn(token, group)
        self.assertNotIn('host.showMessage(', group.split('.setPositiveButton("Group"', 1)[1].split('.setNeutralButton(', 1)[0])

    def test_ime_focus_waits_for_layout_and_rejects_a_replaced_field(self):
        note = self.source.split('private void newNote() {', 1)[1].split('private void newChecklist()', 1)[0]
        for token in ['target.addOnLayoutChangeListener', 'if(r<=l || b<=t)return', 'body!=target',
                      '!target.isAttachedToWindow()', 'target.requestFocus()', 'showSoftInput(target,']:
            self.assertIn(token, note)

    def test_native_injection_is_touchscreen_and_has_no_click_fallback(self):
        source = (ROOT / 'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
        tap = source.split('private void tap(String name)', 1)[1].split('private void longPressBodyAt', 1)[0]
        self.assertEqual(2, tap.count('SOURCE_TOUCHSCREEN'))
        self.assertIn('injectInputEvent(down,true)', tap)
        self.assertIn('injectInputEvent(up,true)', tap)
        self.assertIn('frame.contains(bounds) && controller.view().hasWindowFocus()', tap)
        self.assertNotIn('performClick(', tap)

    def test_full_width_default_and_fit_overview_are_distinct(self):
        pad = (JAVA / 'DrawingPad.java').read_text()
        self.assertIn('fitToPaper?Math.min(getWidth()/1024f,getHeight()/(float)paperHeight):getWidth()/1024f', pad)
        self.assertIn('fitToPaper?(getHeight()-paperHeight*scale())/2:0', pad)
        self.assertIn('void fit(){fitToPaper=true;', pad)
        source = (ROOT / 'app/src/androidTest/java/app/space/notes/runtime/DrawingToolsTest.java').read_text()
        self.assertIn('draw(p,1100,100,1100,300)', source)
        self.assertIn('b.getPixel(939,171)', source)
        self.assertIn('b.sameAs(after)', source)

    def test_core_junit_assertion_helpers_are_explicitly_imported(self):
        import re
        source = (ROOT / 'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
        for helper in set(re.findall(r'\b(assert(?:Equals|Null|NotNull|True|False|Same|NotSame|NotEquals|ArrayEquals))\s*\(', source)):
            self.assertIn('import static org.junit.Assert.' + helper + ';', source)

    def test_pin_journey_does_not_issue_two_competing_ime_dismissals(self):
        source = (ROOT / 'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
        case = source.split('void backgroundAndPinStateAreVisibleAndPersistent()', 1)[1].split('@Test', 1)[0]
        self.assertNotIn('hideSoftInputFromWindow', case)
        self.assertIn('tap("Pin")', case)
        self.assertIn('text("Unpin")', case)

    def test_grouping_prerequisites_offer_a_real_next_action(self):
        group = self.source.split('private void groupShapes(){', 1)[1].split('private void chooseArrow()', 1)[0]
        self.assertIn('currentDiagram(n).nodes.size()<2', group)
        self.assertIn('selectedBefore.group.isEmpty()', group)
        self.assertIn('Add at least two shapes before grouping. Your diagram is unchanged.', group)
        self.assertIn('button("Add shape"', group)
        self.assertIn('root.findViewWithTag("diagram-add-shape")', group)
        self.assertIn('add.setTag("diagram-add-shape")', self.source)

    def test_empty_ocr_result_has_persistent_help_and_live_action_availability(self):
        review = self.source.split('private void renderOcrReview(', 1)[1].split('private void saveOcrText(', 1)[0]
        for token in ['No text to save.', 'English', 'Hindi / Devanagari', 'Kannada',
                      '!input.getText().toString().trim().isEmpty()',
                      'emptyResult.setVisibility(hasText?View.GONE:View.VISIBLE)',
                      'controlAvailable(actions.getChildAt(i),hasText)',
                      'input.addTextChangedListener(watcher(availability))', 'availability.run()']:
            self.assertIn(token, review)
        save = self.source.split('private void saveOcrText(', 1)[1].split('public void startVoiceCapture()', 1)[0]
        self.assertIn('if(value.trim().isEmpty())', save)
        self.assertIn('Original note is unavailable or in Trash.', save)

    def test_rare_scene_inventory_remains_bounded_and_collectable(self):
        from mynotes.tools.ci.persist_ux import SCENES
        from mynotes.tools.ci.render_review import MAX_IMAGES
        self.assertTrue({'group-prerequisites', 'shape-picker', 'read-image-language',
                         'ocr-review-empty', 'ocr-review', 'backups', 'restore-preview'} <= SCENES)
        self.assertLessEqual(len(SCENES), MAX_IMAGES)
        self.assertLessEqual(MAX_IMAGES, 65)
        self.assertTrue({'document-mixed','document-table','document-format','document-saved-styles','document-typing-style','document-pdf-options','document-keyboard','document-visuals','document-dragged'} <= SCENES)

    def test_pin_fault_capture_does_not_wait_on_a_lost_fixture_window(self):
        source = (ROOT / 'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
        fault = source.split('catch(AssertionError clipped)', 1)[1].split('instrumentation.waitForIdleSync();main(', 1)[0]
        for token in ['target_attached=', 'activity_finishing=', 'activity_destroyed=', 'decor_focus=',
                      'storeScreenshot("control-failure",instrumentation.getUiAutomation().takeScreenshot())',
                      'throw new AssertionError("Control did not settle unclipped:']:
            self.assertIn(token, fault)
        self.assertNotIn('postOnAnimation', fault)

    def test_expensive_workflows_do_not_trigger_from_incidental_app_edits(self):
        import re
        workflow_root = ROOT.parent / '.github/workflows'
        for filename, expected in [
            ('notes-soak.yml', {'.github/workflows/notes-soak.yml'}),
            ('notes-ci.yml', {'.github/workflows/notes-ci.yml', '.github/actions/setup-mynotes/**'}),
        ]:
            source = (workflow_root / filename).read_text()
            block = source.split('paths:', 1)[1].split('workflow_dispatch:', 1)[0]
            paths = set(re.findall(r"(?m)^\s+- '([^']+)'", block))
            self.assertEqual(expected, paths, filename)

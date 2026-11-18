"""Source-only guardrails, not execution of Java geometry or Android pixels."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'runtime/src/main/java/app/space/notes/runtime'

class ImageEditingContracts(unittest.TestCase):
    def setUp(self):
        self.controller = (JAVA / 'NotesController.java').read_text()
        self.view = (JAVA / 'ImageEditView.java').read_text()

    def test_imported_images_have_a_separate_edit_route(self):
        self.assertIn('drawing?"Edit drawing":"Edit image"', self.controller)
        self.assertIn('else renderImageEditor(n.id,image.id,null)', self.controller)
        self.assertIn('if(!trashed)itemHeader.addView', self.controller)

    def test_save_preserves_attachment_identity_and_validates_original(self):
        save = self.controller.split('private void saveImageEdit(', 1)[1].split('public void attachImage(', 1)[0]
        self.assertIn('disk.execute(', save)
        self.assertIn('new app.space.notes.domain.NoteImage(original.id,original.name,', save)
        self.assertIn('},original.data)', save)
        apply = self.controller.split('private void addImage(String id,app.space.notes.domain.NoteImage image,int attempt,Runnable accepted,Runnable rejected,String originalData)', 1)[1].split('private void renderBackgrounds()', 1)[0]
        self.assertIn('!images.get(index).data.equals(originalData)', apply)
        self.assertLess(apply.index('BackupCodec.encode(candidate)'), apply.index('captureUndo('))
        self.assertIn('revision!=candidateRevision', apply)

    def test_back_cancellation_keeps_the_discard_guard(self):
        leave = self.controller.split('private void leaveImageEditor()', 1)[1].split('private Button imageTool(', 1)[0]
        self.assertIn('pageBack=this::leaveImageEditor', leave)
        self.assertIn('view.saving()', leave)
        self.assertIn('view.dirty()', leave)
        self.assertIn('"Discard image edits?"', leave)

    def test_rotation_state_is_bounded_and_tied_to_the_original(self):
        for key in ['imageEditPlan','imageEditUndo','imageEditRedo','imageEditCrop','imageEditDigest']:
            self.assertGreaterEqual(self.controller.count('"'+key+'"'), 2)
        self.assertIn('digest.equals(restored.getString("imageEditDigest"))', self.controller)
        self.assertIn('values.length>400', self.view)
        self.assertIn('MAX_HISTORY=16', self.view)
        self.assertIn('while(undo.size()>MAX_HISTORY)undo.removeFirst()', self.view)
        self.assertIn('while(values.size()>MAX_HISTORY)values.remove(0)', self.view)

    def test_crop_has_non_dragging_controls_and_recognisable_icons(self):
        for label in ['Crop presets','Rotate left','Rotate right','Mirror','Undo','Redo','Save image edits','Reset all edits']:
            self.assertIn('"'+label+'"', self.controller)
        self.assertIn('control.setMinHeight(dp(64))', self.controller)
        self.assertIn('control.setCompoundDrawables(null,symbol,null,null)', self.controller)
        self.assertIn('controlAvailable(save,available && view.dirty())', self.controller)

    def test_preview_does_not_rasterize_on_touch_and_cleans_up(self):
        touch = self.view.split('public boolean onTouchEvent(', 1)[1]
        self.assertNotIn('createBitmap(', touch)
        self.assertIn('finishGesture(false)', touch)
        self.assertIn('source.recycle()', touch)
        self.assertIn('MotionEvent.ACTION_CANCEL', touch)
        self.assertIn('event.getPointerCount()!=1', touch)

    def test_image_editor_supports_pinch_pan_and_fit_while_browsing(self):
        self.assertIn('ScaleGestureDetector', self.view)
        self.assertIn('zoomAt(detector.getFocusX(),detector.getFocusY()', self.view)
        self.assertIn('ACTION_POINTER_DOWN', self.view)
        self.assertIn('multiFingerNavigation', self.view)
        self.assertIn('void fitToScreen()', self.view)
        self.assertIn('imageTool(row,"Fit","fit",view::fitToScreen)', self.controller)
        self.assertIn('Pinch to zoom around your fingers', self.controller)

    def test_short_two_finger_spans_fall_back_when_platform_scale_slop_is_not_crossed(self):
        self.assertIn('scaledByDetector=false;zoomDetector.onTouchEvent(event)', self.view)
        self.assertIn('if(!scaledByDetector && lastNavSpan>0 && span>0)', self.view)
        self.assertIn('private static float pointerSpan(MotionEvent event)', self.view)
        self.assertIn('lastNavSpan=pointerSpan(event)', self.view)

    def test_text_redaction_and_line_tools_are_saved_in_versioned_annotation_state(self):
        self.assertIn('REDACT, TEXT', self.view)
        self.assertIn('void addText(String value,float sourceX,float sourceY)', self.view)
        self.assertIn('STATE_VERSION=3', self.view)
        self.assertIn('(values[1]<1 || values[1]>STATE_VERSION)', self.view)
        self.assertIn('Tool.ARROW.ordinal()-1', self.view)
        self.assertIn('Tool.REDACT', self.controller)
        self.assertIn('Tool.TEXT', self.controller)
        self.assertIn('"Arrow","Line"', self.controller)
        self.assertIn('view.addText(input.getText().toString(),x,y)', self.controller)
        self.assertIn('ink.setColor(Color.BLACK);canvas.drawRect', self.view)
        self.assertIn('Button cover=imageTool(row,"Cover"', self.controller)
        self.assertNotIn('imageTool(row,"Redact"', self.controller)
        self.assertNotIn('Use Text, Redact', self.view)
        self.assertIn('Use Text, Cover', self.view)
        self.assertIn('textTypeface(mark.font)', self.view)
        self.assertIn('void drawingFont(int value)', self.view)
        self.assertIn('void drawingWidthDp(float value)', self.view)
        self.assertIn('void drawingColor(int color)', self.view)
        self.assertIn('textSelected()?"Text box selected. Drag it to move', self.view)
        self.assertIn('drawTextSelection(canvas,annotations.get(selectedText))', self.view)
        self.assertIn('finishTextMove(true)', self.view)
        self.assertIn('private boolean updateSelectedTextStyleWidth', self.view)
        self.assertIn('private void syncTextToolStyle()', self.view)

    def test_image_editor_has_accessible_color_size_and_font_controls(self):
        self.assertIn('imageTool(row,"Color","palette"', self.controller)
        self.assertIn('imageTool(row,"Thickness","stroke"', self.controller)
        self.assertIn('imageTool(row,"Font","text"', self.controller)
        self.assertIn('"Thickness / text size"', self.controller)
        self.assertIn('"Text font"', self.controller)
        self.assertIn('"imageEditWidth"', self.controller)
        self.assertIn('"imageEditFont"', self.controller)
        self.assertIn('private static final int STATE_MAGIC=0x4d4e4931,STATE_VERSION=3;', self.view)
        self.assertIn('if(version>=3)', self.view)

    def test_image_editor_topbar_orders_undo_redo_immediately_before_save(self):
        editor = self.controller.split('private void renderImageEditor(', 1)[1].split('private void saveImageEdit(', 1)[0]
        undo = editor.index('undo=icon("Undo"')
        redo = editor.index('redo=icon("Redo"')
        self.assertIn('undo.setTooltipText("Undo image edits")', editor)
        self.assertIn('redo.setTooltipText("Redo image edits")', editor)
        save = editor.index('Button save=icon("Save image edits"')
        more = editor.index('Button more=icon("Image options"')
        self.assertLess(undo, redo)
        self.assertLess(redo, save)
        self.assertLess(save, more)
        self.assertNotIn('imageTool(row,"Undo"', editor)
        self.assertNotIn('imageTool(row,"Redo"', editor)

    def test_checklist_actions_target_the_row_and_disable_boundaries(self):
        self.assertIn('"Actions for list item "+(n.items.indexOf(item)+1)', self.controller)
        options = self.controller.split('private void listItemOptions(', 1)[1].split('private void listAction(', 1)[0]
        for token in ['focusedItem=itemId','item.depth<3','item.depth>0','controlAvailable(option,allowed[i])']:
            self.assertIn(token, options)
        for removed in ['Add item below','Move item up','Move item down']:
            self.assertNotIn(removed, options)
        self.assertIn('checklistHandle(n.id,item.id', self.controller)

    def test_invalid_checklist_moves_do_not_replace_undo(self):
        action = self.controller.split('private void listAction(', 1)[1].split('private void convertNote()', 1)[0]
        self.assertLess(action.index('index<0){host.showMessage('), action.index('captureUndo('))
        self.assertLess(action.index('rows.get(index).depth==0)))return;'), action.index('captureUndo('))
        self.assertIn('action.equals("add-after") && index>=0?index+1:rows.size()', action)

    def test_review_action_rows_do_not_baseline_shift_buttons_outside_the_row(self):
        row = self.controller.split('private LinearLayout rowIn(', 1)[1].split('private void header(', 1)[0]
        self.assertIn('actionsRow.setBaselineAligned(false)', row)
        self.assertIn('actionsRow.setGravity(android.view.Gravity.CENTER_VERTICAL)', row)

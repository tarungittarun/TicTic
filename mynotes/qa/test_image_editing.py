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
        self.assertIn('while(undo.size()>50)', self.view)

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

    def test_checklist_actions_target_the_row_and_disable_boundaries(self):
        self.assertIn('"Actions for list item "+(n.items.indexOf(item)+1)', self.controller)
        options = self.controller.split('private void listItemOptions(', 1)[1].split('private void listAction(', 1)[0]
        for token in ['focusedItem=itemId','index>0','index+1<note.items.size()','item.depth<3','item.depth>0','controlAvailable(option,allowed[i])']:
            self.assertIn(token, options)
        self.assertIn('"Add item below"', options)

    def test_invalid_checklist_moves_do_not_replace_undo(self):
        action = self.controller.split('private void listAction(', 1)[1].split('private void convertNote()', 1)[0]
        self.assertLess(action.index('index<0){host.showMessage('), action.index('captureUndo('))
        self.assertLess(action.index('rows.get(index).depth==0)))return;'), action.index('captureUndo('))
        self.assertIn('action.equals("add-after") && index>=0?index+1:rows.size()', action)

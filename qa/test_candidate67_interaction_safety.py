"""Structural guardrails only. Android touch, geometry and file-I/O acceptance is separate."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'mynotes/runtime/src/main/java/app/space/notes/runtime'
class Candidate67Safety(unittest.TestCase):
    def test_create_actions_overlay_the_board_not_a_root_band(self):
        source=(JAVA/'NotesController.java').read_text()
        home=source.split('private void renderHome(){',1)[1].split('private void reorderHome',1)[0]
        self.assertIn('libraryArea.addView(createRow,floating)',home)
        self.assertNotIn('createRow=tools(root)',home)
        self.assertIn('board.setClipToPadding(false)',home)
        self.assertIn('dp(80)',home)
    def test_settings_and_folders_follow_other_library_actions(self):
        source=(JAVA/'NotesController.java').read_text()
        home=source.split('LinearLayout navigation=menu("Library",library);',1)[1].split('LinearLayout workspaceBar',1)[0]
        self.assertIn('new String[]{"Folders","Settings"}',home)
        self.assertLess(home.index('this::newChecklist'),home.index('new String[]{"Folders","Settings"}'))
    def test_selection_preserving_scroll_keeps_real_taps_and_long_hold(self):
        source=(JAVA/'SelectionScrollView.java').read_text()
        for token in ['nearEndpoint','scrollBy','fling','super.dispatchTouchEvent(down)','postDelayed(passHold','MotionEvent.ACTION_CANCEL','onDetachedFromWindow']:
            self.assertIn(token,source)
        self.assertIn('new SelectionScrollView(context)',(JAVA/'NotesController.java').read_text())
        self.assertIn('new SelectionScrollView(getContext())',(JAVA/'DocumentEditorView.java').read_text())
    def test_large_pdf_bounds_and_index_work_are_finite(self):
        source=(JAVA/'LocalPdfPreview.java').read_text()
        self.assertIn('pdf.getPageCount()>10000',source)
        self.assertIn('>128L*1024*1024',source)
        self.assertIn('if(page>=64){partial=true;break;}',source)
        self.assertIn('Math.sqrt(2000000.0',source)
    def test_word_selection_uses_drawn_layout_and_bounded_unicode_clusters(self):
        source=(JAVA/'WordPageFile.java').read_text()
        for token in ['layout.getSelectionPath(at,end,path)','BreakIterator.getCharacterInstance','selectionGlyphs>=50000','glyphs.size()>=5000','pdf.sourceLayers(layers)']:
            self.assertIn(token,source)
    def test_zoom_scope_is_user_chosen_and_does_not_allocate_zoom_rasters(self):
        source=(JAVA/'LocalPdfPreview.java').read_text()
        for token in ['Zoom scope','Each page · independent zoom','Whole file · same zoom on every page','viewer-file-zoom','pageZooms','sharedZoom']:
            self.assertIn(token,source)
        self.assertNotIn('createBitmap',(JAVA/'ZoomablePageImage.java').read_text())

    def test_history_indexing_yields_to_first_raster_and_foreground_work(self):
        source=(JAVA/'LocalPdfPreview.java').read_text()
        self.assertIn('holder.image.setImageBitmap(result);deferTextIndex();',source)
        self.assertIn('if(!closed && !indexingStopped)indexText(consumer)',source)
        self.assertNotIn('if(index!=null)indexText(index)',source)

    def test_image_rotation_reflows_before_measuring_children(self):
        source=(JAVA/'ImageTextSelectionDialog.java').read_text()
        measure=source.split('@Override protected void onMeasure',1)[1].split('private static void showChild',1)[0]
        self.assertLess(measure.index('reflow.accept'),measure.index('super.onMeasure'))
        self.assertNotIn('root.addOnLayoutChangeListener',source)

class LibraryPortraitDiscoverability(unittest.TestCase):
    def test_portrait_library_expands_without_changing_half_height_filters(self):
        source=(ROOT/'mynotes/runtime/src/main/java/app/space/notes/runtime/NotesSheets.java').read_text()
        self.assertIn('"Library".contentEquals(caption.getText())',source)
        self.assertIn('shell.getHeight()>shell.getWidth()',source)
        self.assertIn('float fraction=library?.88f:.72f',source)
        self.assertIn('filter?Math.max(1,shell.getHeight()/2)',source)
    def test_native_traversal_checks_final_settings_row_not_just_child_order(self):
        source=(ROOT/'mynotes/app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
        self.assertIn('settings.getGlobalVisibleRect(visible)',source)
        self.assertIn('settings.getHeight(),visible.height()',source)

class ImageActionRailSafety(unittest.TestCase):
    def test_compact_actions_keep_real_touch_targets_and_do_not_wrap(self):
        source=(JAVA/'ImageTextSelectionDialog.java').read_text()
        self.assertIn('compactAction(extract,d);compactAction(keep,d)',source)
        self.assertIn('button.setMinimumHeight(d.dp(48))',source)
        self.assertIn('button.setSingleLine(true)',source)
        self.assertIn('new LinearLayout.LayoutParams(wide?-1:0,d.dp(48),wide?0:1)',source)
    def test_capability_help_warns_about_observed_no_text_hallucinations(self):
        source=(JAVA/'ImageTextSelectionDialog.java').read_text()
        self.assertIn('Photo textures and status icons can be mistaken for letters',source)
        self.assertIn('even when no writing is present',source)

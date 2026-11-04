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

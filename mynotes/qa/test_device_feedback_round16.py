"""Source safety contracts; these do not replace the Actions Android regressions."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'

class DeviceFeedbackRound16(unittest.TestCase):
    def test_rotation_retains_activity_but_not_font_or_appearance_changes(self):
        manifest=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
        key='{http://schemas.android.com/apk/res/android}'
        activity=next(x for x in manifest.iter('activity') if x.get(key+'name')=='.NotesActivity')
        handled=set(activity.get(key+'configChanges').split('|'))
        self.assertTrue({'orientation','screenSize'}<=handled)
        self.assertNotIn('fontScale',handled)
        self.assertNotIn('uiMode',handled)

    def test_file_back_target_is_captured_before_viewer_clears_page(self):
        source=(JAVA/'NotesController.java').read_text()
        opening=source.split('private void openFile(android.net.Uri uri,boolean importWord)',1)[1].split('public void openNoteDocument',1)[0]
        self.assertLess(opening.index('returnToCaller=fileReturnTarget()'),opening.index('scrollContent('))
        self.assertNotIn('pageBack=this::renderStorage',opening)
        self.assertIn('pageBack=returnToCaller',opening)
        self.assertIn('search.setHint("Search all content & settings")',source)

    def test_word_oversized_line_fits_without_losing_source_text(self):
        source=(JAVA/'WordPageFile.java').read_text()
        self.assertIn('canvas.scale(fit,fit)',source)
        self.assertIn('text.getLineEnd(line)',source)
        self.assertIn('Oversized line/image scaled',source)
        self.assertNotIn('line/image cannot fit a page safely',source)

    def test_pdf_gestures_are_bounded_without_allocating_zoom_sized_rasters(self):
        zoom=(JAVA/'ZoomablePageImage.java').read_text()
        self.assertIn('ScaleGestureDetector',zoom)
        self.assertIn('Math.min(5,zoom*detector.getScaleFactor())',zoom)
        self.assertIn('requestDisallowInterceptTouchEvent',zoom)
        self.assertNotIn('createBitmap',zoom)
        native=(ROOT/'app/src/androidTest/java/app/space/notes/runtime/ViewedFileJourneysTest.java').read_text()
        gesture=native.split('@Test public void pageDoubleTapZoom',1)[1].split('@Test',1)[0]
        self.assertIn('getDisplayMetrics().widthPixels',gesture)
        self.assertNotIn('image.layout(0,0,400,600)',gesture)
        self.assertIn('NotesAppearance.dark(activity,host)',native)
        self.assertIn('parent.setBackgroundColor(design.background)',native)
        pdf=(JAVA/'LocalPdfPreview.java').read_text()
        for action in ['Select page text','Share original file','new PdfSelectionImage']:
            self.assertIn(action,pdf)

    def test_share_grants_read_only_and_selection_opens_without_insertion(self):
        activity=(ROOT/'app/src/main/java/app/space/notes/NotesActivity.java').read_text()
        share=activity.split('void shareViewedFile(',1)[1].split('void openOriginal(',1)[0]
        self.assertIn('Intent.ACTION_SEND',share)
        self.assertIn('FLAG_GRANT_READ_URI_PERMISSION',share)
        self.assertNotIn('FLAG_GRANT_WRITE_URI_PERMISSION',share)
        source=(JAVA/'NotesController.java').read_text()
        selection=source.split('private void addOcrImageSelection(',1)[1].split('private void keepOcrImage(',1)[0]
        self.assertIn('select.performClick();',selection)
        self.assertNotIn('saveOcrText(',selection)

    def test_retained_image_dialog_reflows_existing_view_not_new_selection(self):
        source=(JAVA/'ImageTextSelectionDialog.java').read_text()
        self.assertIn('root.addOnLayoutChangeListener',source)
        reflow=source.split('root.addOnLayoutChangeListener',1)[1].split('view.onChanged',1)[0]
        self.assertIn('body.setOrientation',reflow)
        self.assertIn('side.setLayoutParams',reflow)
        self.assertNotIn('new ImageTextSelection(',reflow)
        self.assertNotIn('new ImageTextSelectionView(',reflow)
        native=(ROOT/'app/src/androidTest/java/app/space/notes/runtime/ImageSelectionTest.java').read_text()
        self.assertIn('retainedDialogReflowsBothWaysWithoutLosingSelectionOrBitmap',native)
        self.assertIn('assertSame(original[0],image)',native)

    def test_spreadsheet_capture_stops_startup_and_requires_attached_fixture(self):
        source=(ROOT/'app/src/androidTest/java/app/space/notes/runtime/ViewedFileJourneysTest.java').read_text()
        fixture=source.split('spreadsheetControlsLeaveAFullDataRowVisible',1)[1].split('@Test',1)[0]
        self.assertIn('startup.close()',fixture)
        self.assertIn('shown.get().isAttachedToWindow()',fixture)
        self.assertIn('shown.get().isShown()',fixture)
        self.assertLess(fixture.index('startup.close()'),fixture.index('activity.setContentView(parent)'))

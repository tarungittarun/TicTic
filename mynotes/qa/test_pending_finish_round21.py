"""Policy/source guards only. Native execution and physical acceptance are separate."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'

class PendingFinishRound21(unittest.TestCase):
    def test_pdf_ocr_is_explicit_bounded_and_not_a_file_writer(self):
        source=(JAVA/'PdfPageOcr.java').read_text()
        for guard in ['getMainLooper()', 'cancellation.check()', '2400.0/', '4000000.0/', 'finally{if(bitmap!=null)bitmap.recycle();}']:
            self.assertIn(guard,source)
        for forbidden in ['FileOutputStream', 'NotesRepository', 'HttpURLConnection']:
            self.assertNotIn(forbidden,source)
        self.assertLess(source.index('page.render('),source.index('OfflineOcr.recognizeBitmap'))

    def test_ocr_uses_renderer_serial_queue_and_cancels_on_close(self):
        source=(JAVA/'LocalPdfPreview.java').read_text()
        section=source.split('private void recognizePage(int page)',1)[1].split('private void find()',1)[0]
        self.assertIn('worker.execute(',section)
        self.assertNotIn('textWorker.execute(',section)
        self.assertIn('"auto",cancellation',section)
        close=source.split('@Override public void close()',1)[1]
        self.assertIn('ocrCancellation.cancel()',close)
        self.assertIn('pageOcrDialog.dismiss()',close)
        self.assertIn('pageTextDialog.dismiss()',close)

    def test_ocr_is_reviewed_not_automatically_imported_or_indexed(self):
        source=(JAVA/'LocalPdfPreview.java').read_text()
        self.assertIn('Recognize page offline',source)
        self.assertIn('Recognized page text — review before copying',source)
        self.assertIn('setTextIsSelectable(true)',source)
        self.assertIn('setEnabled(!blank)',source)
        section=source.split('private void recognizePage(int page)',1)[1].split('private void find()',1)[0]
        self.assertNotIn('consumer.accept',section)
        self.assertNotIn('saveOcrText',section)

    def test_restored_viewers_use_only_local_sources_and_bounded_state(self):
        source=(JAVA/'NotesController.java').read_text()
        session=source.split('private Bundle fileSession(',1)[1].split('private Runnable fileReturnTarget()',1)[0]
        self.assertIn('value.length()>4096',session)
        self.assertIn('LocalDeviceDocuments.requireReadable(uri)',session)
        self.assertIn('name.length()>512',session)
        self.assertIn('"pdf","sheet","word"',session)
        self.assertNotIn('getByteArray',session)
        self.assertIn('state.putBundle("viewedFileSession",session)',source)
        for kind in ['word','sheet','pdf']:
            self.assertIn('shownViewer(fileState,"'+kind+'",name)',source)

    def test_late_file_results_do_not_override_new_navigation(self):
        source=(JAVA/'NotesController.java').read_text()
        self.assertIn('private void clearPage(){fileViewGeneration++',source)
        self.assertIn('fileTicket!=fileViewGeneration',source)
        self.assertIn('disk.execute(pdf::close);return;',source)

    def test_window_capture_is_not_a_different_foreground_activity(self):
        source=(ROOT/'app/src/androidTest/java/app/space/notes/runtime/ViewedFileJourneysTest.java').read_text()
        fixture=source.split('spreadsheetControlsLeaveAFullDataRowVisible',1)[1].split('@Test',1)[0]
        self.assertIn('PixelCopy.request(activity.getWindow()',fixture)
        self.assertIn('android.view.PixelCopy.SUCCESS',fixture)
        self.assertNotIn('getUiAutomation().takeScreenshot()',fixture)

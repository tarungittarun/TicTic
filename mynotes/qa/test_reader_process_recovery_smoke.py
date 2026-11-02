"""Host fixture/parser checks, not emulator recovery acceptance."""
import io
from pathlib import Path
import unittest
import zipfile
from mynotes.tools.ci.reader_process_recovery_smoke import pdf_fixture,word_fixture,stopped_record,visible_rows


class ReaderProcessRecoverySmokeTest(unittest.TestCase):
    def test_public_pdf_has_valid_offsets_and_two_pages(self):
        data=pdf_fixture()
        self.assertEqual(2,data.count(b'/Type /Page /Parent'))
        xref=int(data.split(b'startxref\n')[1].splitlines()[0])
        self.assertEqual(b'xref',data[xref:xref+4])
        entries=data[xref:].splitlines()[3:10]
        for index,entry in enumerate(entries,1):
            offset=int(entry[:10]);self.assertTrue(data[offset:].startswith(str(index).encode()+b' 0 obj\n'))

    def test_word_fixture_is_public_minimal_docx_with_one_explicit_page_break(self):
        with zipfile.ZipFile(io.BytesIO(word_fixture())) as source:
            self.assertIn('[Content_Types].xml',source.namelist())
            xml=source.read('word/document.xml')
            self.assertEqual(1,xml.count(b'w:type="page"'))
            self.assertIn(b'PublicWordPageTwo',xml)
            self.assertNotIn(b'http://private',xml)

    def test_stopped_state_is_scoped_to_target_activity_not_next_task(self):
        target='      * Hist #0: ActivityRecord{x app.space.notes/.NotesActivity t7}\n'
        self.assertTrue(stopped_record(target+'        mState=STOPPED mHaveState=true\n'))
        self.assertFalse(stopped_record(target+'        mState=RESUMED\n    * Task{x}\n      * Hist #0: ActivityRecord{x other/.Home t9}\n        mState=STOPPED\n'))
        self.assertFalse(stopped_record('* Hist #0: ActivityRecord{x other/.Home t9}\n state=STOPPED'))

    def test_visible_rows_and_actual_kill_are_not_force_stop_shortcuts(self):
        self.assertEqual([41,42],visible_rows([{'content-desc':'Spreadsheet row 42; tap a cell'},{'content-desc':'other'},{'content-desc':'Spreadsheet row 41; tap a cell'}]))
        source=(Path(__file__).resolve().parents[1]/'tools/ci/reader_process_recovery_smoke.py').read_text()
        self.assertIn("device.adb('shell','am','kill',PACKAGE)",source)
        self.assertIn('wait_until(lambda:pid() is None)',source)
        self.assertNotIn('device.launch()',source)
        self.assertNotIn("'force-stop'",source)
        self.assertNotIn("'uninstall'",source)

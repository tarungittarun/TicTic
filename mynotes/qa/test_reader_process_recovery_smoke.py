"""Host fixture/parser checks, not emulator recovery acceptance."""
import io
from pathlib import Path
import unittest
import zipfile
from mynotes.tools.ci.reader_process_recovery_smoke import pdf_fixture,word_fixture,stopped_record,visible_rows,picker_nodes


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
        self.assertTrue(stopped_record(target.replace('Hist #','Hist  #')+'        mState=STOPPED mHaveState=true\n'))
        self.assertTrue(stopped_record(target.replace('Hist #','Hist\t#')+'        mState=STOPPED mHaveState=true\n'))
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
        self.assertIn("os.environ.get('GITHUB_ACTIONS')!='true'",source)
        self.assertIn("'ro.kernel.qemu'",source)
        self.assertIn("device.adb('shell','mkdir','/sdcard/Download/'+folder)",source)


    def test_picker_nodes_exclude_other_apps_and_disabled_actions(self):
        xml='<hierarchy><node package="com.google.android.documentsui" enabled="true" text="Downloads"/><node package="private.app" enabled="true" text="Downloads"/><node package="android" enabled="false" text="Denied"/></hierarchy>'
        self.assertEqual(['Downloads'],[node.get('text') for node in picker_nodes(xml)])


    def test_scrollable_app_menu_is_navigated_without_skipping_the_file_action(self):
        from unittest.mock import Mock
        from mynotes.tools.ci.reader_process_recovery_smoke import reachable_click
        from mynotes.tools.ci.release_update_smoke import nodes
        device=Mock()
        device.ui.side_effect=[nodes('<hierarchy><node package="app.space.notes" enabled="true" scrollable="true" bounds="[0,100][500,900]"/></hierarchy>'),nodes('<hierarchy><node package="app.space.notes" enabled="true" text="Open file" clickable="true" bounds="[10,200][490,300]"/></hierarchy>')]
        reachable_click(device,'Open file')
        self.assertEqual('swipe',device.adb.call_args_list[0].args[2])
        self.assertEqual(('shell','input','tap',250,250),device.adb.call_args_list[1].args)

    def test_invalid_version_guard_precedes_any_device_operation(self):
        from unittest.mock import patch
        from mynotes.tools.ci.reader_process_recovery_smoke import verify
        for code in (0,-1,True):
            with patch('mynotes.tools.ci.reader_process_recovery_smoke.Device') as factory:
                with self.assertRaises(ValueError):verify('unused',code)
                factory.assert_not_called()

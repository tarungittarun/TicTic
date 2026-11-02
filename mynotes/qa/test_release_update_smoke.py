"""Host harness self-checks only; emulator success must be measured separately."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import Mock,patch
from mynotes.tools.ci import release_update_smoke as smoke

class ReleaseUpdateSmokeHarness(unittest.TestCase):
    def test_bounds_and_package_identity_fail_closed(self):
        self.assertEqual((20,30),smoke.center('[10,20][30,40]'))
        for bad in ['', '[-1,2][3,4]', '[1,2][1,4]']:
            with self.assertRaises(ValueError):smoke.center(bad)
        self.assertEqual({'version_code':34,'first_install_time':'fixed'},smoke.identity('versionCode=34 minSdk=26\nfirstInstallTime=fixed\n'))
        with self.assertRaises(ValueError):smoke.identity('versionCode=34')

    def test_xml_filters_other_apps_and_prefers_clickable_workspace_tab(self):
        items=smoke.nodes('<hierarchy><node package="app.space.notes" enabled="true" text="Notes"/><node package="keyboard" enabled="true" text="Notes"/><node package="app.space.notes" enabled="true" text="Notes" clickable="true"/></hierarchy>')
        self.assertEqual(2,len(items));self.assertIs(items[1],smoke.named(items,'Notes'))
        self.assertIsNone(smoke.named(items,'Note'))

    def device(self,after=None):
        device=Mock();device.state.side_effect=[{'version_code':33,'first_install_time':'fixed'},after or {'version_code':34,'first_install_time':'fixed'}];device.adb.return_value='34\n';return device

    def test_sequence_cold_checks_both_formats_before_and_after_real_update(self):
        device=self.device()
        with tempfile.TemporaryDirectory() as directory,patch.object(smoke,'Device',return_value=device):
            output=Path(directory)/'report.json';smoke.verify('old.apk','candidate.apk',output)
            report=json.loads(output.read_text());self.assertTrue(report['notes_and_doc_retained']);self.assertFalse(report['release_approval'])
        self.assertEqual(3,device.launch.call_count);self.assertEqual(4,device.check.call_count)
        commands=[call.args for call in device.adb.call_args_list]
        self.assertIn(('install','old.apk'),commands);self.assertIn(('install','-r','candidate.apk'),commands)
        self.assertFalse(any('uninstall' in args or 'clear' in args for args in commands))

    def test_version_or_install_identity_change_never_produces_passing_report(self):
        for after in [{'version_code':33,'first_install_time':'fixed'},{'version_code':34,'first_install_time':'new'}]:
            with tempfile.TemporaryDirectory() as directory,patch.object(smoke,'Device',return_value=self.device(after)):
                output=Path(directory)/'report.json'
                with self.assertRaises(RuntimeError):smoke.verify('old.apk','candidate.apk',output)
                self.assertFalse(output.exists())

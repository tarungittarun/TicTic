import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

from mynotes.tools.ci import release_update_note_smoke as smoke


ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT.parent / '.github/workflows/notes-improvement70.yml'


class Candidate72ReleaseUpdateTests(unittest.TestCase):
    def test_package_install_identity_and_saved_revision_parsers_fail_closed(self):
        self.assertEqual(
            {'version_code': 13, 'first_install_time': '2026-10-01 12:00:00'},
            smoke.identity('versionCode=13 minSdk=26 targetSdk=34\n'
                           'firstInstallTime=2026-10-01 12:00:00\n'))
        with self.assertRaises(ValueError):
            smoke.identity('versionCode=13\n')
        self.assertEqual([4, 5], smoke.saved_revisions(
            'I/MyNotes: save outcome=ok revision=4 count=1\n'
            'I/MyNotes: save outcome=failed revision=5 count=1\n'
            'I/MyNotes: save outcome=ok revision=5 count=1\n'))

    def test_visible_bounds_are_checked_before_tapping(self):
        self.assertEqual((50, 60), smoke.center('[10,20][90,100]'))
        for bounds in ('', '[10,20][10,100]', '[10,20][90,0]'):
            with self.subTest(bounds=bounds), self.assertRaises(ValueError):
                smoke.center(bounds)

    def device(self, after=None):
        device = Mock()
        device.state.side_effect = [
            {'version_code': 13, 'first_install_time': 'fixed'},
            after or {'version_code': 72, 'first_install_time': 'fixed'}]
        device.adb.return_value = '34\n'
        return device

    def test_smoke_checks_legacy_cold_restart_and_signed_install_over_without_data_reset(self):
        device = self.device()
        with tempfile.TemporaryDirectory() as directory, \
                patch.object(smoke, 'Device', return_value=device), \
                patch('builtins.print'):
            output = Path(directory) / 'report.json'
            smoke.verify('public-0.12.apk', 'candidate-0.72.apk', output)
            report = json.loads(output.read_text())

        self.assertTrue(report['legacy_note_retained_after_cold_restart_and_update'])
        self.assertTrue(report['same_package_install_over'])
        self.assertFalse(report['uninstall_or_clear_data'])
        self.assertFalse(report['release_approval'])
        self.assertEqual(2, device.verify_note.call_count)
        self.assertEqual(3, device.launch.call_count)
        commands = [call.args for call in device.adb.call_args_list]
        self.assertIn(('install', 'public-0.12.apk'), commands)
        self.assertIn(('install', '-r', 'candidate-0.72.apk'), commands)
        self.assertFalse(any('uninstall' in args or
                             ('pm' in args and 'clear' in args)
                             for args in commands))

    def test_no_success_report_for_wrong_version_or_changed_install_time(self):
        for after in (
                {'version_code': 71, 'first_install_time': 'fixed'},
                {'version_code': 72, 'first_install_time': 'changed'}):
            with self.subTest(after=after), tempfile.TemporaryDirectory() as directory, \
                    patch.object(smoke, 'Device', return_value=self.device(after)), \
                    patch('builtins.print'):
                output = Path(directory) / 'report.json'
                with self.assertRaises(RuntimeError):
                    smoke.verify('public-0.12.apk', 'candidate-0.72.apk', output)
                self.assertFalse(output.exists())

    def test_version_downgrade_is_rejected_before_device_access(self):
        with patch.object(smoke, 'Device') as factory:
            with self.assertRaises(ValueError):
                smoke.verify('old.apk', 'candidate.apk', 'unused.json', 72, 72)
            factory.assert_not_called()

    def test_workflow_builds_and_uploads_only_a_verified_release_as_update(self):
        workflow = WORKFLOW.read_text()
        self.assertIn('MyNotes-v0.72.0-72-update.apk', workflow)
        self.assertIn('name: MyNotes-v0.72.0-72-update.apk', workflow)
        self.assertIn('archive: false', workflow)
        self.assertIn('candidate72-update-apk-verification', workflow)
        self.assertIn('candidate72-emulator-test-apks-do-not-install', workflow)
        self.assertIn('08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca', workflow)
        self.assertIn('mynotes/releases/MyNotes-v0.12.0-13.apk.sha256', workflow)
        self.assertIn('mynotes/tools/ci/release_update_note_smoke.py', workflow)
        self.assertIn('release_update_note_smoke.py', workflow)
        self.assertIn('actions/download-artifact@v8.0.2', workflow)
        self.assertIn('api: [26, 34]', workflow)
        self.assertNotIn('softprops/action-gh-release', workflow)
        self.assertNotIn('gh release create', workflow)
        smoke_source = (ROOT / 'tools/ci/release_update_note_smoke.py').read_text()
        self.assertIn("'uninstall_or_clear_data': False", smoke_source)
        self.assertNotIn("'shell', 'pm', 'clear'", smoke_source)
        self.assertNotIn("'uninstall', PACKAGE", smoke_source)

    def test_legacy_release_receipt_matches_pinned_checksum(self):
        receipt = json.loads((ROOT / 'releases/MyNotes-v0.12.0-13.apk.release.json').read_text())
        checksum = (ROOT / 'releases/MyNotes-v0.12.0-13.apk.sha256').read_text().split()
        self.assertEqual('app.space.notes', receipt['applicationId'])
        self.assertEqual(13, receipt['versionCode'])
        self.assertEqual('0.12.0', receipt['versionName'])
        self.assertEqual('08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca',
                         receipt['certificate'])
        self.assertEqual(receipt['sha256'], checksum[0])
        self.assertEqual(receipt['filename'], checksum[1])


if __name__ == '__main__':
    unittest.main()

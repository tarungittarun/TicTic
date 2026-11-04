"""Workflow/fixture plan only, not executed install-over evidence."""
import unittest
from pathlib import Path
import yaml
ROOT=Path(__file__).resolve().parents[1]
class Candidate67MigrationPlan(unittest.TestCase):
    def test_exact_new_candidate_old_original_and_two_supported_apis(self):
        path=ROOT/'.github/workflows/notes-candidate67-rich-update.yml';text=path.read_text();data=yaml.safe_load(text)
        self.assertEqual([26,34],[row['api'] for row in data['jobs']['update']['strategy']['matrix']['include']])
        self.assertIn('run-id: 41842482778',text);self.assertIn('run-id: 41807474878',text)
        self.assertIn('MyNotes-v0.67.0-67-development.apk',text);self.assertIn('MyNotes-v0.66.0-66-development.apk',text)
        self.assertIn('candidate[\'digest\'].removeprefix(\'sha256:\')',text)
        self.assertIn('"--old-code","66","--candidate-code","67"',text)
    def test_real_guest_is_offline_same_signer_and_not_cleared(self):
        text=(ROOT/'.github/workflows/notes-candidate67-rich-update.yml').read_text()
        self.assertIn('python3 -m mynotes.tools.ci.offline_guest',text)
        self.assertIn('08cd7d693078ef665b7acd427d71fa3aa477cc6af2cab2a00c310f9d66198fca',text)
        self.assertIn("'android.permission.INTERNET' not in manifest",text)
        self.assertNotIn('adb uninstall',text);self.assertNotIn('pm clear',text);self.assertNotIn('install -d',text)
    def test_native_probe_checks_explicit_pair_instead_of_stale_64_to_66_assertions(self):
        text=(ROOT/'mynotes/app/releaseMigration/java/app/space/notes/ReleaseRichMigrationProbe.java').read_text()
        self.assertIn('oldVersion>0 && candidateVersion>oldVersion',text)
        self.assertIn('assertEquals(oldVersion,version)',text);self.assertIn('assertEquals(candidateVersion,version)',text)
        self.assertNotIn('assertEquals(64,version)',text);self.assertNotIn('assertEquals(66,version)',text)
        runner=(ROOT/'mynotes/tools/ci/release_rich_update_probe.py').read_text()
        self.assertIn("'oldReleaseCode',old_code",runner);self.assertIn("'candidateReleaseCode',candidate_code",runner)

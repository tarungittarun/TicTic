"""Structural guardrails, not runtime pass evidence."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'

class BackupContracts(unittest.TestCase):
    def test_preview_requires_explicit_confirmation(self):
        s=(JAVA/'NotesController.java').read_text()
        self.assertIn('"Confirm merge","Cancel"',s)
        self.assertIn('if(!notes.equals(plan.base))',s)
        self.assertIn('NoteMerge.preview(base,incoming)',s)

    def test_backup_has_checksum_format_and_explicit_bound(self):
        s=(JAVA/'BackupCodec.java').read_text()
        for token in ['MyNotesBackup','MAX_BYTES=8*1024*1024','Backup checksum mismatch','Backup note count mismatch']:
            self.assertIn(token,s)

    def test_export_checks_readback_and_refuses_cloud(self):
        s=(JAVA/'LocalBackupFiles.java').read_text()
        for token in ['com.android.externalstorage.documents','Arrays.equals(bytes,reopened)','BackupCodec.decode(reopened)']:
            self.assertIn(token,s)

    def test_prior_snapshot_before_restore(self):
        s=(JAVA/'NotesRepository.java').read_text()
        method=s.split('public boolean applyRestore',1)[1].split('public void openExisting',1)[0]
        self.assertLess(method.index('atomicWrite(beforeRestore()'),method.index('save(merged)'))

    def test_release_version_is_higher(self):
        s=(ROOT/'app/build.gradle').read_text()
        self.assertIn('versionCode 14',s)
        self.assertIn("versionName '0.13.0'",s)

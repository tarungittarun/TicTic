"""Build and architectural smoke checks, not Android runtime evidence."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]

class NotesStandaloneContracts(unittest.TestCase):
    def test_standalone_modules_registered(self):
        s=(ROOT/'settings.gradle').read_text()
        for module in ['notes-domain','notes-runtime','notes']:
            self.assertIn("include ':"+module+"'",s)

    def test_old_hosts_do_not_consume_new_runtime(self):
        for name in ['app','feature-notes','calc']:
            s=(ROOT/name/'build.gradle').read_text()
            self.assertNotIn("project(':notes-runtime')",s)

    def test_runtime_only_consumes_own_domain(self):
        s=(ROOT/'notes-runtime/build.gradle').read_text()
        self.assertIn("project(':notes-domain')",s)
        for banned in [':app',':shell',':feature-notes',':feature-calculator']:
            self.assertNotIn("project('"+banned+"')",s)

    def test_offline_text_milestone_declares_no_permissions(self):
        r=ET.parse(ROOT/'notes/src/main/AndroidManifest.xml').getroot()
        self.assertEqual([],r.findall('uses-permission'))
        app=r.find('application')
        self.assertEqual('false',app.attrib['{http://schemas.android.com/apk/res/android}allowBackup'])

    def test_domain_has_no_android_dependencies(self):
        for p in (ROOT/'notes-domain/src').rglob('*.java'):
            self.assertNotIn('import android.',p.read_text())

    def test_search_and_disk_work_have_separate_queues(self):
        s=(ROOT/'notes-runtime/src/main/java/app/space/notes/runtime/NotesController.java').read_text()
        for pin in ['searchWorker.execute','disk.execute','ticket!=querySequence','postDelayed(filter,150)',
                    'private static final ExecutorService disk','!dirty && ticket==revision']:
            self.assertIn(pin,s)

    def test_workflow_builds_only_notes(self):
        s=(ROOT/'.github/workflows/notes-ci.yml').read_text()
        self.assertIn(':notes-domain:test :notes:assembleRelease',s)
        self.assertNotIn('tests.yml',s)
        self.assertNotIn(':app:assemble',s)

    def test_repository_is_versioned_and_has_no_silent_cap(self):
        s=(ROOT/'notes-runtime/src/main/java/app/space/notes/runtime/NotesRepository.java').read_text()
        self.assertIn('AtomicFile',s)
        self.assertIn('file.readFully()',s)
        self.assertNotIn('CAP',s)
        self.assertIn('Duplicate note ID',s)

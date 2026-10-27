"""Build and architectural smoke checks, not Android runtime evidence."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
REPO=Path(__file__).resolve().parents[2]
ROOT=REPO/"mynotes"

class NotesStandaloneContracts(unittest.TestCase):
    def test_standalone_modules_registered(self):
        s=(ROOT/'settings.gradle').read_text()
        for module in ['notes-domain','notes-runtime','notes']:
            self.assertIn("':"+module+"'",s)

    def test_old_hosts_do_not_consume_new_runtime(self):
        for name in ['app','feature-notes','calc']:
            s=(REPO/name/'build.gradle').read_text()
            self.assertNotIn("project(':notes-runtime')",s)

    def test_runtime_only_consumes_own_domain(self):
        s=(ROOT/'runtime/build.gradle').read_text()
        self.assertIn("project(':notes-domain')",s)
        for banned in [':app',':shell',':feature-notes',':feature-calculator']:
            self.assertNotIn("project('"+banned+"')",s)

    def test_offline_engines_declare_only_optional_contextual_microphone(self):
        r=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
        self.assertEqual(['android.permission.RECORD_AUDIO','android.permission.POST_NOTIFICATIONS','android.permission.RECEIVE_BOOT_COMPLETED'],[p.attrib['{http://schemas.android.com/apk/res/android}name'] for p in r.findall('uses-permission')])
        self.assertEqual('false',r.find('uses-feature').attrib['{http://schemas.android.com/apk/res/android}required'])
        app=r.find('application')
        self.assertEqual('false',app.attrib['{http://schemas.android.com/apk/res/android}allowBackup'])

    def test_domain_has_no_android_dependencies(self):
        for p in (ROOT/'domain/src').rglob('*.java'):
            self.assertNotIn('import android.',p.read_text())

    def test_search_and_disk_work_have_separate_queues(self):
        s=(ROOT/'runtime/src/main/java/app/space/notes/runtime/NotesController.java').read_text()
        for pin in ['searchWorker.execute','disk.execute','ticket!=querySequence','postDelayed(filter,150)',
                    'private static final ExecutorService disk','!dirty && ticket==revision']:
            self.assertIn(pin,s)

    def test_workflow_builds_only_notes(self):
        s=(REPO/'.github/workflows/notes-ci.yml').read_text()

        for task in [':notes-domain:test',':notes:assembleRelease',':notes:assembleDebug',':notes:assembleDebugAndroidTest']:
            self.assertIn(task,s)
        self.assertNotIn('tests.yml',s)
        self.assertNotIn(':app:assemble',s)

    def test_repository_is_versioned_and_has_no_silent_cap(self):
        s=(ROOT/'runtime/src/main/java/app/space/notes/runtime/NotesRepository.java').read_text()
        self.assertIn('AtomicFile',s)
        self.assertIn('atomicRead(file)',s);self.assertIn('target.readFully()',s)
        self.assertNotIn('CAP',s)
        self.assertIn('Duplicate note ID',(ROOT/'runtime/src/main/java/app/space/notes/runtime/DocumentCodec.java').read_text())

    def test_parent_build_does_not_register_notes(self):
        s=(REPO/'settings.gradle').read_text()
        for module in ['notes','notes-runtime','notes-domain']:
            self.assertNotIn("include ':"+module+"'",s)

    def test_gradle_build_has_no_parent_file_dependencies(self):
        for p in ROOT.rglob('*.gradle'):
            s=p.read_text()
            self.assertNotIn('../',s,str(p))
            self.assertNotIn('includeBuild',s,str(p))
        self.assertTrue((ROOT/'gradle/wrapper/gradle-wrapper.jar').exists())
        self.assertTrue((ROOT/'signing/notes.p12').exists())

    def test_java_imports_stay_within_notes_namespace(self):
        import re
        for directory in ['app','runtime','domain']:
            for p in (ROOT/directory).rglob('*.java'):
                for namespace in re.findall(r'^import (app\.space\.[\w.]+);',p.read_text(),re.M):
                    self.assertTrue(namespace.startswith('app.space.notes.'),str(p)+': '+namespace)

    def test_legacy_workflows_are_paused(self):
        import re
        for name in ['android-ci.yml','tests.yml']:
            s=(REPO/'.github/workflows'/name).read_text()
            events=s.split('\non:\n',1)[1].split('\n\n',1)[0]
            self.assertNotIn('push:',events)
            self.assertNotIn('pull_request:',events)
            jobs=s.split('\njobs:\n',1)[1]
            ids=re.findall(r'^  [\w-]+:\n',jobs,re.M)
            gates=re.findall(r'^    if: \$\{\{ false \}\}',jobs,re.M)
            self.assertGreater(len(ids),0)
            self.assertEqual(len(ids),len(gates),name)

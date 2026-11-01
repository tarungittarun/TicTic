"""Storage safety contracts; Android behavioral evidence is required separately."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'runtime/src/main/java/app/space/notes/runtime'


class AppFolderArchitecture(unittest.TestCase):
    def test_initialization_refuses_nonempty_before_creating_any_namespace(self):
        source = (JAVA / 'AppFolder.java').read_text()
        method = source.split('public void initialize()', 1)[1].split('Uri library()', 1)[0]
        self.assertLess(method.index('fs.list(fs.root).isEmpty()'), method.index('fs.path(path, true)'))
        self.assertLess(method.index('fs.path(path, true)'), method.index('fs.immutable(fs.root, MARKER, ID)'))

    def test_layout_and_registry_exclude_models_cache_and_grant_preferences(self):
        source = (JAVA / 'AppFolder.java').read_text()
        for path in ['Content/Notes', 'Content/Voice', 'Recovery/Library', 'Recovery/Objects', 'Recovery/Checkpoints']:
            self.assertIn(path, source)
        registry = source.split('static boolean owned(', 1)[1].split('private File local(', 1)[0]
        self.assertNotIn('offline-speech', registry)
        self.assertNotIn('offline-ocr', registry)
        prefs = source.split('String[] PREFS =', 1)[1].split(';', 1)[0]
        self.assertNotIn('mynotes-storage-v1', prefs)
        self.assertNotIn('notes-diagnostics', prefs)  # Restore must not enable diagnostic consent.

    def test_recovery_stages_and_verifies_before_publishing_ready(self):
        source = (JAVA / 'AppFolder.java').read_text()
        method = source.split('private void recoverVerified(', 1)[1].split('public static void finishRecovery(', 1)[0]
        for guard in ['prepareRecovery()', 'checkpoint.id.equals(expected)', 'expectedBytes', 'hash(target)', 'PcmWave.inspect', 'DocumentCodec.decode']:
            self.assertIn(guard, method)
        self.assertLess(method.index('hash(target)'), method.index('"ready.json"'))
        self.assertLess(method.index('DocumentCodec.decode'), method.index('"ready.json"'))
        self.assertLess(method.index('ready = true'), method.index('finishRecovery(context)'))

    def test_internal_copy_is_not_disabled_by_selected_folder(self):
        source = (JAVA / 'NotesRepository.java').read_text()
        save = source.split('public void save(', 1)[1].split('public void selectFolder(', 1)[0]
        self.assertLess(save.index('writeLocal(payload,true,head)'), save.index('protocol(selected).commit'))
        self.assertIn('private recovery', save)

    def test_worker_coalesces_and_observes_non_note_state_without_ui_provider_io(self):
        source = (JAVA / 'AppFolderTasks.java').read_text()
        for guard in ['newSingleThreadScheduledExecutor', 'pending.cancel(false)', 'FileObserver', 'state.revision == revision', 'registerOnSharedPreferenceChangeListener']:
            self.assertIn(guard, source)
        controller = (JAVA / 'NotesController.java').read_text()
        self.assertIn('AppFolder.finishRecovery', controller)
        self.assertIn('host::recoverAppFolder', controller)
        self.assertIn('AppFolderTasks.request(context);persisted.run()', controller)

    def test_reads_accept_known_local_files_but_writes_keep_strict_saf_gate(self):
        reads = (JAVA / 'LocalDeviceDocuments.java').read_text()
        for name in ['com.google.android.apps.nbu.files.provider', 'com.android.providers.media.documents', 'com.android.providers.downloads.documents', 'com.android.providers.media.photopicker']:
            self.assertIn(name, reads)
        source = (JAVA / 'LocalBackupFiles.java').read_text()
        read = source.split('public static List<Note> read(', 1)[1].split('decodeImport(', 1)[0]
        self.assertIn('LocalDeviceDocuments.requireReadable', read)
        self.assertIn('requireLocal(uri);byte[] bytes=BackupCodec.encode(notes)', source)

    def test_txt_and_word_lead_to_real_editor_without_mutating_sources(self):
        controller = (JAVA / 'NotesController.java').read_text()
        self.assertIn('"Open editable copy"', controller)
        self.assertIn('withContent(result.content', controller)
        self.assertIn('if(plan.openAfter!=null)', controller)
        word = (JAVA / 'StructuredWord.java').read_text()
        for token in ['TextTable.rich', 'new Note.Mark', 'LocalOpenFile.docx(archive)', 'Nested Word tables', 'Merged Word cells', 'MAX_BLOCKS']:
            self.assertIn(token, word)
        self.assertNotIn('openConnection', word)
        self.assertNotIn('FileOutputStream', word)

    def test_recovery_controller_updates_memory_before_host_recreation_flush(self):
        source = (JAVA / 'NotesController.java').read_text()
        method = source.split('public void recoverAppFolder(', 1)[1].split('private LinearLayout scrollContent(', 1)[0]
        self.assertLess(method.index('notes.addAll(recoveredNotes)'), method.index('host.appearanceChanged()'))
        self.assertLess(method.index('savedHistories.putAll(recoveredHistories)'), method.index('host.appearanceChanged()'))
        self.assertIn('Nothing imported yet: finish the current storage/recovery operation', source)

    def test_note_only_import_cannot_replace_a_complete_source_app_folder(self):
        source = (JAVA / 'NotesController.java').read_text()
        self.assertIn('plan.notesOnlyCopy=new AppFolder(context,source).recognized()', source)
        self.assertIn('if(plan.folder!=null && !plan.notesOnlyCopy)repository.openExisting', source)
        self.assertIn('Complete App folders are recovery sources', (JAVA / 'NotesRepository.java').read_text())

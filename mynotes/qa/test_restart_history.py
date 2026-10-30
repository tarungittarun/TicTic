"""Source safety contracts complement (never replace) actual native restart qualification."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'
class RestartHistoryContracts(unittest.TestCase):
 def test_sidecar_is_private_bounded_and_checksummed(self):
  s=(JAVA/'PersistentHistoryStore.java').read_text()
  for token in ['getFilesDir()','AtomicFile','MAX_BYTES=16*1024*1024','MAX_NOTES=64','History checksum mismatch','History object checksum mismatch','blocked=true','if(blocked)throw','file.failWrite(out)']:
   self.assertIn(token,s)
  self.assertNotIn('Diagnostics.',s)
  self.assertNotIn('ContentResolver',s)
 def test_history_can_never_replace_notes_on_load(self):
  s=(JAVA/'PersistentHistoryStore.java').read_text().split('public Map<String,DocumentHistory> load',1)[1].split('private static List<Note> states',1)[0]
  for token in ['note==null','record.getString("anchor")','DocumentHistory.restore(id','result.clear()']:
   self.assertIn(token,s)
  for token in ['repository.save','file.delete','startWrite','new NotesRepository']:
   self.assertNotIn(token,s)
 def test_controller_preserves_notes_before_optional_history_and_bounds_memory(self):
  s=(JAVA/'NotesController.java').read_text()
  self.assertLess(s.index('repository.save(snapshot);if(historyStore'),s.index('historyStore.save(historySnapshot)'))
  for token in ['trimSavedHistories()','retainedCharacterCount()','MAX_CHARACTERS','historyStepping','historyFor(before.id).align(before,target,redo)','savedHistories.keySet().removeIf(id->find(id)==null)']:
   self.assertIn(token,s)
 def test_search_has_accessible_fallback_and_cancelled_holds(self):
  s=(JAVA/'SearchGestures.java').read_text()
  for token in ['onSingleTapConfirmed','onDoubleTapEvent','getLongPressTimeout()','setOnClickListener','setOnLongClickListener','ACTION_CANCEL','ACTION_MOVE','getScaledTouchSlop','onViewDetachedFromWindow','removeCallbacks(hold)']:
   self.assertIn(token,s)
 def test_folders_do_not_force_private_notes_into_external_storage(self):
  s=(JAVA/'NotesController.java').read_text()
  self.assertIn('repository.listFolders(folderSnapshot)',s)
  self.assertIn('repository.createFolder(name,folderSnapshot)',s)
  self.assertIn('Folders organise notes on this device.',s)
  storage=s.split('private void renderStorage()',1)[1].split('public void selectFolder',1)[0]
  self.assertEqual(1,storage.count('settingRow(content,"Backups"'))
  self.assertIn('History is not part of exported backups.',storage)
 def test_backup_picker_and_export_boundary_both_enforce_local_only(self):
  s=(ROOT/'app/src/main/java/app/space/notes/NotesActivity.java').read_text()
  for method,end in [('public void exportBackup()', 'public void chooseNoteDocument'),('public void importBackup()', 'private void openPicker'),('private void chooseTree(', '@Override protected void onActivityResult')]:
   self.assertIn('Intent.EXTRA_LOCAL_ONLY,true',s.split(method,1)[1].split(end,1)[0])
  boundary=(JAVA/'LocalBackupFiles.java').read_text()
  self.assertIn('requireLocal(uri);byte[] bytes=BackupCodec.encode(notes)',boundary)
  self.assertIn('Backup readback failed',boundary)

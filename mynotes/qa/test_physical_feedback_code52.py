"""Safety/source wiring only. Android execution and Motorola acceptance are separate."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'
class PhysicalFeedback52Contracts(unittest.TestCase):
 def test_drop_completion_is_posted_and_fenced_against_new_data_and_gestures(self):
  source=(JAVA/'NotesBoard.java').read_text().split('void clearView(',1)[1].split('});dragHelper',1)[0]
  self.assertLess(source.index('post(new Runnable'),source.index('adapter.notifyDataSetChanged()'))
  for token in ['isComputingLayout()','postOnAnimation(this)','gesture!=dragEpoch','data!=dataEpoch','listener.reorder(ids)']:
   self.assertIn(token,source)
 def test_folder_pending_outcome_requires_verified_private_save_and_keeps_head_fence(self):
  source=(JAVA/'NotesRepository.java').read_text().split('private void saveVerified(',1)[1].split('public void selectFolder(',1)[0]
  self.assertLess(source.index('writeLocal(payload,true,head)'),source.index('pendingFolderFailure!=null'))
  self.assertIn('System.nanoTime()',source);self.assertIn('protocol(selected).commit(payload,head)',source)
  self.assertIn('throw new FolderCopyPendingException(e)',source)
  self.assertIn('!Objects.equals(current.head,expected)',(JAVA/'GenerationProtocol.java').read_text())
 def test_folder_repair_is_a_reviewed_merge_not_an_unconditional_head_reset(self):
  source=(JAVA/'NotesController.java').read_text()
  for token in ['Review selected folder difference','openExistingFolder(selected)','!source.equals(repository.selectedFolder())','Confirm merge']:
   self.assertIn(token,source)
 def test_private_save_warning_is_not_emitted_on_every_edit(self):
  source=(JAVA/'NotesController.java').read_text()
  self.assertIn('copyPending && !folderPendingNoticeShown',source)
  self.assertIn('if(!copyPending)folderPendingNoticeShown=false',source)
  self.assertIn('persistedRevision=Math.max(persistedRevision,ticket)',source)
 def test_read_only_file_work_does_not_wait_in_library_writer_queue(self):
  source=(JAVA/'NotesController.java').read_text()
  for marker in ['private void openWordPages(', 'private void openFile(android.net.Uri uri,boolean importWord)']:
   section=source.split(marker,1)[1].split('busy=true;',1)[1]
   self.assertTrue(section.startswith('fileReader.execute('))
  self.assertIn('fileReader.shutdownNow()',source)
 def test_viewing_is_default_and_copy_is_explicit_with_bounded_native_pages(self):
  source=(JAVA/'NotesController.java').read_text()
  self.assertIn('if(!importWord){renderFileTextPreview(uri,result.name,result.body);return;}',source)
  self.assertIn('Create editable copy',source);self.assertIn('pages.page(index[0])',source)
  self.assertNotIn('value.trim().split',source.split('private void renderFileTextPreview(',1)[1].split('private Bundle fileSession',1)[0])
  self.assertIn('bytes=bounded(in,8*1024*1024)',(JAVA/'LocalOpenFile.java').read_text())
 def test_filter_uses_tallest_tab_and_half_height_cap(self):
  self.assertIn('maxPageHeight=Math.max(maxPageHeight', (JAVA/'LibraryFilterView.java').read_text())
  source=(JAVA/'NotesSheets.java').read_text()
  self.assertIn('shell.getHeight()/2',source);self.assertIn('instanceof LibraryFilterView',source)
 def test_colourless_reuses_valid_zero_without_schema_change_and_skips_fill(self):
  self.assertIn('node.color!=0',(JAVA/'DiagramRenderer.java').read_text())
  self.assertIn('colourNames[0]="Colourless"',(JAVA/'NotesController.java').read_text())
 def test_native_regressions_exist_for_layout_and_durable_conflict_recovery(self):
  native=ROOT/'app/src/androidTest/java/app/space/notes'
  self.assertIn('board.isComputingLayout()', (native/'runtime/NotesMotionTest.java').read_text())
  self.assertIn('reviewedSelectedFolderMergePreservesBothVersionsAndCanSaveAgain',(native/'RepositoryTransferTest.java').read_text())
  self.assertIn('largerTxtUsesLosslessPagesAndNeverCreatesAHomeNote',(native/'RealFolderStorageTest.java').read_text())

 def test_half_height_filter_reclaims_decoration_not_native_touch_targets(self):
  source=(JAVA/'NotesSheets.java').read_text()
  self.assertIn('filter?0:design.dp(4)',source);self.assertIn('filter?0:design.dp(16)',source)
  self.assertIn('filter?0:design.dp(12)',source)
  self.assertIn('button.setMinHeight(d.dp(48))',(JAVA/'LibraryFilterView.java').read_text())
 def test_pending_folder_state_is_verified_and_remains_visible_after_navigation(self):
  repository=(JAVA/'NotesRepository.java').read_text();controller=(JAVA/'NotesController.java').read_text()
  self.assertIn('atomicWrite(file,bytes);folderCopyPending=pending;',repository)
  self.assertIn('folderCopyPending=local.optBoolean("pending",false)',repository)
  self.assertGreaterEqual(controller.count('showPendingFolderStatus();'),2)

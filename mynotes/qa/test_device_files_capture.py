"""Revision source contracts; behavioral acceptance additionally requires JVM/native results."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'
class DeviceFilesCapture(unittest.TestCase):
 def test_search_bar_is_below_creation_and_icon_removed(self):
  home=(JAVA/'NotesController.java').read_text().split('private void renderHome(){',1)[1].split('private void saveViewPreferences()',1)[0]
  self.assertNotIn('icon("Search"',home);self.assertGreater(home.index('root.addView(searchBar'),home.index('dock.addView(create)'))
 def test_folders_tab_preserves_apply_and_access_to_real_device_picker(self):
  s=(JAVA/'LibraryFilterView.java').read_text()
  for token in ['"Folders"','"Browse folders"','"Choose device folder"','"Apply filters"','fontScale>1.3f']:self.assertIn(token,s)
 def test_doc_insert_routes_both_capture_actions(self):
  for token in ['"Read image"','"Record voice"']:self.assertIn(token,(JAVA/'InsertPalette.java').read_text());self.assertIn('host.action('+token+')',(JAVA/'DocumentEditorView.java').read_text())
 def test_word_archive_is_bounded_and_does_not_execute_external_xml(self):
  s=(JAVA/'LocalOpenFile.java').read_text()
  for token in ['DOCTYPE','ENTITY','total>16*1024*1024','++count>1024','depth>128','CodingErrorAction.REPORT','OfflineOcr.requireLocal']:self.assertIn(token,s)
  self.assertNotIn('URL(',s);self.assertNotIn('FileOutputStream',s)
 def test_mixed_notes_are_not_silently_moved_into_doc_workspace(self):
  self.assertIn('!note.content.notesWorkspace',(ROOT/'domain/src/main/java/app/space/notes/domain/Workspace.java').read_text())
  codec=(JAVA/'DocumentCodec.java').read_text();self.assertIn('schema=10',codec);self.assertIn('version<10 && canonical.content.notesWorkspace',codec)
 def test_speech_keeps_raw_and_uses_one_absolute_word_timeline(self):
  s=(JAVA/'OfflineSpeech.java').read_text();self.assertIn('timeline.addAll(words)',s);self.assertIn('new Result(transcript.toString()',s);self.assertIn('TranscriptReadability.format(timeline)',s)
 def test_ocr_is_still_cancelable_and_no_emoji_fabrication_claim(self):
  s=(JAVA/'OfflineOcr.java').read_text()
  for token in ['PSM_SPARSE_TEXT','prepareBitmap','cancellation.check()','prepared.recycle()']:self.assertIn(token,s)
  self.assertIn('Emoji and decorative lettering may need manual correction',(JAVA/'NotesController.java').read_text())
 def test_recording_meter_and_optional_effect_do_not_drop_permission_safety(self):
  s=(JAVA/'VoiceCapture.java').read_text()
  for token in ['NoiseSuppressor.isAvailable()','listener.level(peak)','checkSelfPermission','suppression.release()','recorder.release()','output.getFD().sync()']:self.assertIn(token,s)

 def test_copy_picker_result_is_not_discarded_by_host_request_guard(self):
  s=(ROOT/'app/src/main/java/app/space/notes/NotesActivity.java').read_text()
  import re
  highest=int(re.search(r'if\(request<94 \|\| request>(\d+)\)',s).group(1))
  self.assertGreaterEqual(highest,104)
  self.assertIn('if(request==104){controller.exportFolderCopy(uri);return;}',s)
  self.assertNotIn('Open note file',s)

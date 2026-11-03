from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
class StartupPrivateFastPathTests(unittest.TestCase):
 def test_keeps_serial_order_and_recovery_before_private_read(self):
  source=(ROOT/'runtime/src/main/java/app/space/notes/runtime/NotesController.java').read_text()
  startup=source.split('final long startupStarted=',1)[1].split('private void deliverDeferred()',1)[0]
  self.assertLess(startup.index('disk.execute('),startup.index('AppFolder.finishRecovery('))
  self.assertLess(startup.index('AppFolder.finishRecovery('),startup.index('repository.loadForStartup()'))
  self.assertIn('Diagnostics.record("startup.phase"',startup);self.assertIn('startupReport.setVisibility(View.VISIBLE)',startup)
 def test_private_path_never_writes_or_hides_corruption(self):
  source=(ROOT/'runtime/src/main/java/app/space/notes/runtime/NotesRepository.java').read_text().split('public List<Note> loadForStartup()',1)[1].split('public List<Note> load()',1)[0]
  self.assertIn('recovery.exists()',source);self.assertIn('if(!exists())return load();',source)
  self.assertIn('DocumentCodec.decode(bytes)',source);self.assertIn('head=storedHead(local)',source)
  repository=(ROOT/'runtime/src/main/java/app/space/notes/runtime/NotesRepository.java').read_text()
  self.assertIn('local.isNull("folderHead")?null:',repository)
  self.assertNotIn('writeLocal(',source);self.assertNotIn('new ArrayList',source)
 def test_filter_choices_cover_full_rows(self):
  source=(ROOT/'runtime/src/main/java/app/space/notes/runtime/LibraryFilterView.java').read_text()
  self.assertIn('new RadioGroup.LayoutParams(LayoutParams.MATCH_PARENT',source)
  self.assertIn('box.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT',source)

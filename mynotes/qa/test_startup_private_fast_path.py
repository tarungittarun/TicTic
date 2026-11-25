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
 def test_startup_measures_each_blocking_phase_and_defers_repair_saves(self):
  source=(ROOT/'runtime/src/main/java/app/space/notes/runtime/NotesController.java').read_text()
  startup=source.split('final long startupStarted=',1)[1].split('private void deliverDeferred()',1)[0]
  for step in ('recovery','repository_init','private load completed','audio_archive_recovery','voice_note_index','voice_catalog_list','voice_transcript_recovery','voice_recovery_failed','expired_note_purge','history_load','home_render','os_surface_refresh','os_surface_refresh_failed'):
   self.assertIn('recordStartupStep("'+step+'"',startup)
  self.assertIn('step=disk_queue',startup);self.assertIn('step=main_queue',startup)
  self.assertIn('startup.ready',startup)
  self.assertLess(startup.index('repository.loadForStartup()'),startup.index('recordStartupStep("audio_archive_recovery"'))
  self.assertNotIn('repository.save(loaded)',startup)
  self.assertIn('Map<String,Integer> voiceNoteIndices=new HashMap<>()',startup)
  self.assertIn('voiceNoteIndices.get(voiceId)',startup)
  self.assertIn('final boolean startupRepairNeeded=recovered||purged',startup)
  deferred=startup.split('if(startupRepairNeeded)main.post',1)[1].split('if(restored!=null)',1)[0]
  self.assertLess(startup.index('ready=true'),startup.index('if(startupRepairNeeded)main.post'))
  self.assertIn('step=deferred_repair_save',deferred)
  self.assertLess(deferred.index('step=deferred_repair_save'),deferred.index('queueSave()'))
  self.assertLess(startup.index('recordStartupStep("home_render"'),startup.index('recordStartupReady(startupStarted,"home")'))
  for surface in ("viewed_file","drawing","image_editor","editor","voice_inbox","auxiliary","home"):
   self.assertIn('recordStartupReady(startupStarted,"'+surface+'")',startup)
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

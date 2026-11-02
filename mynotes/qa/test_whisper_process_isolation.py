from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
class WhisperProcessIsolationTests(unittest.TestCase):
 def test_private_process_contains_native_faults(self):
  manifest=(ROOT/'runtime/src/main/AndroidManifest.xml').read_text()
  self.assertIn('android:process=":offline_whisper"',manifest)
  self.assertIn('WhisperWorkerService" android:exported="false"',manifest)
  client=(ROOT/'runtime/src/main/java/app/space/notes/runtime/WhisperProcessClient.java').read_text()
  self.assertIn('onServiceDisconnected',client);self.assertIn('done.countDown()',client)
  self.assertNotIn('System.loadLibrary',client);self.assertNotIn('decode(',client)
  self.assertIn('context.unbindService(connection)',client)
 def test_worker_only_accepts_own_uid_and_private_paths(self):
  worker=(ROOT/'runtime/src/main/java/app/space/notes/runtime/WhisperWorkerService.java').read_text()
  self.assertIn('message.sendingUid!=android.os.Process.myUid()',worker)
  self.assertIn('getCanonicalFile()',worker);self.assertIn('getFilesDir().getCanonicalPath()',worker)
  self.assertIn('active.compareAndSet(false,true)',worker)
  self.assertIn('cancelled.set(true);worker.shutdown()',worker)

 def test_cpu_debug_is_optimized_without_disabling_assertions(self):
  cmake=(ROOT/'runtime/src/main/cpp/CMakeLists.txt').read_text()
  self.assertIn('CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -O3"',cmake)
  self.assertIn('CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -O3"',cmake)
  self.assertNotIn('-DNDEBUG',cmake)
 def test_content_free_phase_progress_does_not_complete_transcription(self):
  client=(ROOT/'runtime/src/main/java/app/space/notes/runtime/WhisperProcessClient.java').read_text()
  self.assertIn('WhisperWorkerService.PROGRESS',client)
  self.assertIn('FLAG_DEBUGGABLE',client)
  engine=(ROOT/'runtime/src/main/java/app/space/notes/runtime/TrainedWhisper.java').read_text()
  for phase in ('model-verification','model-load','model-ready','decode-window','decode-complete'):self.assertIn('progress.accept("'+phase+'")',engine)

 def test_cancellation_is_scoped_to_request_and_late_binding_is_ignored(self):
  client=(ROOT/'runtime/src/main/java/app/space/notes/runtime/WhisperProcessClient.java').read_text()
  worker=(ROOT/'runtime/src/main/java/app/space/notes/runtime/WhisperWorkerService.java').read_text()
  self.assertEqual(2,client.count('data.putString("request",requestId)'))
  self.assertIn('if(finished.get())return',client)
  self.assertIn('request.equals(activeRequest)',worker)
  self.assertIn('PROCESS_ACTIVE.compareAndSet(false,true)',worker)
  self.assertIn('if(destroyed ||',worker)

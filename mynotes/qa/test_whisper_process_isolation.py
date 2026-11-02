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

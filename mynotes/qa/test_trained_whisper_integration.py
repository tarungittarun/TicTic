import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from mynotes.tools.models.bundle_trained_whisper import bundle
ROOT=Path(__file__).resolve().parents[1]

class TrainedWhisperIntegrationTests(unittest.TestCase):
 def fixture(self,root):
  source=root/'source';source.mkdir();(source/'trained-whisper.bin').write_bytes(__import__('mynotes.qa.test_whisper_ggml',fromlist=['fixture']).fixture())
  data=dict(training_run=41393453222,optimizer_steps=593,default_engine=False,base_sha256='d3dd57d32accea0b295c96e26691aa14d8822fac7d9d27d5dc00b4ca2826dd03',checkpoint_sha256='84dcc55f0c1bcdc53e62d90a50c4dbe284669222f13ecbcbf458d62af42bc338',sha256=hashlib.sha256((source/'trained-whisper.bin').read_bytes()).hexdigest(),bytes=24)
  data['bytes']=(source/'trained-whisper.bin').stat().st_size
  (source/'trained-whisper.json').write_text(json.dumps(data));(source/'licenses').mkdir();(source/'licenses/COPYING').write_text('test notice');(source/'experimental-acoustic-decoder-blocks.pt').write_text('never bundle training checkpoint')
  return source,data
 def test_bundle_checks_hash_and_excludes_pt(self):
  with tempfile.TemporaryDirectory() as directory:
   root=Path(directory);source,data=self.fixture(root)
   import contextlib,io
   with contextlib.redirect_stdout(io.StringIO()):bundle(source,root/'out')
   self.assertEqual((source/'trained-whisper.bin').read_bytes(),(root/'out/voice/trained-whisper.bin').read_bytes())
   self.assertFalse(list((root/'out').rglob('*.pt')))
   self.assertTrue((root/'out/licenses/trained-whisper-COPYING').is_file())
 def test_wrong_checkpoint_or_default_or_payload_is_rejected(self):
  for kind in ('checkpoint','default','bytes'):
   with tempfile.TemporaryDirectory() as directory:
    root=Path(directory);source,data=self.fixture(root)
    if kind=='checkpoint':data['checkpoint_sha256']='0'*64
    elif kind=='default':data['default_engine']=True
    else:(source/'trained-whisper.bin').write_bytes(b'tampered')
    (source/'trained-whisper.json').write_text(json.dumps(data))
    with self.assertRaises(ValueError):bundle(source,root/'out')
 def test_runtime_cpu_cancel_and_review_are_real_not_model_substitution(self):
  java=(ROOT/'runtime/src/main/java/app/space/notes/runtime/TrainedWhisper.java').read_text()
  cpp=(ROOT/'runtime/src/main/cpp/notes_whisper.cpp').read_text()
  for token in ('System.loadLibrary("notes_whisper")','decode(session[0]','Double.NaN','getBoolean("trained-whisper",false)','output.getFD().sync()','if(cancelled.get())'):self.assertIn(token,java)
  for token in ('whisper_init_from_file_with_params','whisper_full(session->context','params.use_gpu=false','p.abort_callback=abort_decode','count>480000'):self.assertIn(token,cpp)
  self.assertNotIn('http',java)
  self.assertNotIn('whisper_full_get_segment_text',java)
 def test_owned_native_tests_are_in_full_inventory(self):
  from mynotes.tools.ci.test_plan import build_plan
  self.assertEqual(2,len(build_plan()['android']['app.space.notes.TrainedWhisperTest']))
 def test_model_export_pins_actual_base_and_trained_tensors(self):
  source=(ROOT/'tools/ci/export_trained_whisper.py').read_text()
  self.assertIn('digest(a.checkpoint)==BLOCK_SHA',source);self.assertIn('set(blocks)==expected',source)
  self.assertIn("state['model_state_dict'][name]=value",source)

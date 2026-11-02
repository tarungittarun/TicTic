import struct,tempfile,unittest
from pathlib import Path
from mynotes.tools.models.whisper_ggml import validate

def fixture(mislabeled=False):
 data=bytearray(struct.pack('<12i',0x67676d6c,51864,1500,384,6,4,448,384,6,4,80,1))
 data+=struct.pack('<2i',80,201)+bytes(80*201*4)
 data+=struct.pack('<i',50000)+bytes(50000*4)
 for i in range(167):
  name=f'encoder.synthetic_{i}'.encode();data+=struct.pack('<4i',1,len(name),1,1)+name
  data+=struct.pack('<f',1.2345) if mislabeled and i==0 else struct.pack('<e',1.2345)
 return data
class WhisperGGMLTests(unittest.TestCase):
 def test_complete_stream_and_f32_mislabeled_as_f16(self):
  with tempfile.TemporaryDirectory() as directory:
   path=Path(directory)/'model.bin';path.write_bytes(fixture());self.assertEqual(167,validate(path))
   path.write_bytes(fixture(True))
   with self.assertRaises(ValueError):validate(path)
 def test_truncated_stream_fails_closed(self):
  with tempfile.TemporaryDirectory() as directory:
   path=Path(directory)/'model.bin';path.write_bytes(fixture()[:-1])
   with self.assertRaises(ValueError):validate(path)

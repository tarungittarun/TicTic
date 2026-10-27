import struct
import tempfile
from pathlib import Path
import unittest
import zipfile
from mynotes.tools.ci.native_audit import elf,inventory,ABIS

def fixture(bits=64,machine=62,alignment=16384,flags=5):
    ident=b'\x7fELF'+bytes([2 if bits==64 else 1,1,1])+b'\0'*9
    if bits==64:
        header=struct.pack('<16sHHIQQQIHHHHHH',ident,3,machine,1,0,64,0,0,64,56,1,0,0,0)
        ph=struct.pack('<IIQQQQQQ',1,flags,0,0,0,120,120,alignment)
    else:
        header=struct.pack('<16sHHIIIIIHHHHHH',ident,3,machine,1,0,52,0,0,52,32,1,0,0,0)
        ph=struct.pack('<IIIIIIII',1,0,0,0,84,84,flags,alignment)
    return header+ph

class NativeAudit(unittest.TestCase):
    def test_elf32_and_64_native_architectures_and_alignment(self):
        for bits,machine in ABIS.values():
            data=elf(fixture(bits,machine));self.assertEqual((bits,machine),(data['bits'],data['machine']));self.assertTrue(data['elf_16k_segment_alignment'])
    def test_four_k_alignment_and_writable_executable_are_reported_not_certified(self):
        data=elf(fixture(alignment=4096,flags=7));self.assertFalse(data['elf_16k_segment_alignment']);self.assertTrue(data['load_segments'][0]['writable_executable'])
    def test_truncated_invalid_and_non_power_two_headers_refused(self):
        for value in [b'not ELF',fixture()[:80],fixture(alignment=12345)]:
            with self.assertRaises(ValueError):elf(value)
    def test_apk_checks_all_three_actual_elf_architectures(self):
        with tempfile.TemporaryDirectory() as d:
            path=Path(d)/'app.apk'
            with zipfile.ZipFile(path,'w') as out:
                for abi,(bits,machine) in ABIS.items():out.writestr('lib/'+abi+'/libexample.so',fixture(bits,machine))
            self.assertEqual(set(ABIS),set(inventory(path,ABIS)['abis']))
    def test_zip_abi_cannot_disguise_other_machine(self):
        with tempfile.TemporaryDirectory() as d:
            path=Path(d)/'app.apk'
            with zipfile.ZipFile(path,'w') as out:out.writestr('lib/arm64-v8a/libexample.so',fixture(64,62))
            with self.assertRaisesRegex(ValueError,'disagrees'):inventory(path)

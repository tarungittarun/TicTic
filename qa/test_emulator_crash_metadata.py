"""Synthetic minidumps only: test scalar extraction, bounds and memory omission."""
import importlib.util
import json
from pathlib import Path
import struct
import tempfile
import unittest


def module():
    spec=importlib.util.spec_from_file_location('crash_metadata',Path('mynotes/tools/comparisons/emulator_crash_metadata.py'))
    loaded=importlib.util.module_from_spec(spec);spec.loader.exec_module(loaded);return loaded


def dummy_dump():
    data=bytearray(1024)
    struct.pack_into('<4sIIIIIQ',data,0,b'MDMP',42899,3,32,0,0,0)
    struct.pack_into('<III',data,32,6,168,80)
    struct.pack_into('<III',data,44,4,112,256)
    struct.pack_into('<III',data,56,5,64,700)  # Memory stream must never be read/output.
    struct.pack_into('<I',data,88,11)
    struct.pack_into('<Q',data,104,0x1030)
    struct.pack_into('<I',data,256,1)
    struct.pack_into('<QI',data,260,0x1000,0x200)
    struct.pack_into('<I',data,280,400)
    name='/private/home/runner/libgfxstream_backend.so'.encode('utf-16-le')
    struct.pack_into('<I',data,400,len(name));data[404:404+len(name)]=name
    data[700:734]=b'TOKEN_SENTINEL_DO_NOT_EMIT_IN_REPORT'
    return data


class CrashMetadataTest(unittest.TestCase):
    def test_extracts_exception_and_basename_but_not_paths_memory_or_context(self):
        loaded=module()
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'dummy.dmp';path.write_bytes(dummy_dump())
            result=loaded.parse_dump(path)
            self.assertEqual(result['exception']['code_hex'],'0xb')
            self.assertEqual(result['exception']['containing_module']['basename'],'libgfxstream_backend.so')
            self.assertEqual(result['exception']['containing_module']['exception_offset_hex'],'0x30')
            self.assertFalse(result['raw_memory_retained']);self.assertFalse(result['thread_context_read'])
            encoded=json.dumps(result)
            self.assertNotIn('TOKEN_SENTINEL',encoded);self.assertNotIn('/private/',encoded)

    def test_invalid_metadata_is_bounded_and_rejected(self):
        loaded=module()
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'dummy.dmp'
            path.write_bytes(b'not a dump')
            with self.assertRaises(loaded.InvalidDump):loaded.parse_dump(path)
            data=dummy_dump();struct.pack_into('<I',data,8,100000)
            path.write_bytes(data)
            with self.assertRaisesRegex(loaded.InvalidDump,'Stream count'):loaded.parse_dump(path)
            data=dummy_dump();struct.pack_into('<I',data,64,9000)  # ignored memory-stream RVA
            path.write_bytes(data);self.assertIsNotNone(loaded.parse_dump(path)['exception'])
            data=dummy_dump();struct.pack_into('<I',data,280,999999)
            path.write_bytes(data)
            with self.assertRaisesRegex(loaded.InvalidDump,'bounds'):loaded.parse_dump(path)

    def test_collects_only_named_emulator_databases_without_copying_dumps(self):
        loaded=module()
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);folder=root/'android-runner/emu-crash-unit.db/pending';folder.mkdir(parents=True)
            raw=folder/'unit.dmp';raw.write_bytes(dummy_dump())
            result=loaded.collect(root)
            self.assertEqual(result['candidate_count'],1);self.assertFalse(result['raw_dumps_uploaded'])
            self.assertEqual(raw.read_bytes(),dummy_dump())
            self.assertNotIn('TOKEN_SENTINEL',json.dumps(result))
            self.assertEqual(list(folder.iterdir()),[raw])

    def test_does_not_follow_database_parent_symlink(self):
        loaded=module()
        with tempfile.TemporaryDirectory() as directory,tempfile.TemporaryDirectory() as outside:
            target=Path(outside)/'emu-crash-unit.db/pending';target.mkdir(parents=True)
            (target/'unit.dmp').write_bytes(dummy_dump())
            (Path(directory)/'android-runner').symlink_to(outside,target_is_directory=True)
            self.assertEqual(loaded.collect(Path(directory))['candidate_count'],0)

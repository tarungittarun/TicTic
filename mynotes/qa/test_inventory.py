import hashlib
from pathlib import Path
import tempfile
import unittest
from mynotes.tools.ci.inventory import inventory

class InventoryTests(unittest.TestCase):
    def fixture(self,d):
        root=Path(d);(root/'releases').mkdir();apk=root/'releases/Test.apk';apk.write_bytes(b'fixture, not a real APK')
        Path(str(apk)+'.sha256').write_text(hashlib.sha256(apk.read_bytes()).hexdigest()+'  Test.apk\n')
        Path(str(apk)+'.source.txt').write_text('a'*40+'\n');return root,apk
    def test_inventory_reports_bytes_and_source_without_claiming_sdk_signature_check(self):
        with tempfile.TemporaryDirectory() as d:
            root,apk=self.fixture(d);result=inventory(root);self.assertEqual(1,len(result['releases']));self.assertEqual(apk.stat().st_size,result['releases'][0]['bytes'])
    def test_modified_immutable_apk_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            root,apk=self.fixture(d);apk.write_bytes(b'changed')
            with self.assertRaisesRegex(ValueError,'checksum'):inventory(root)
    def test_missing_source_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            root,apk=self.fixture(d);Path(str(apk)+'.source.txt').write_text('not-a-sha')
            with self.assertRaisesRegex(ValueError,'source'):inventory(root)
    def test_snapshot_and_local_repository_rejected(self):
        for command in ["implementation 'org:lib:1-SNAPSHOT'",'repositories { mavenLocal() }']:
            with tempfile.TemporaryDirectory() as d:
                root,apk=self.fixture(d);(root/'build.gradle').write_text(command)
                with self.assertRaises(ValueError):inventory(root)

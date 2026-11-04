import io,tarfile,tempfile,unittest
from pathlib import Path
from mynotes.tools.ci.retain_public_captures import retain
class RetainPublicCaptures(unittest.TestCase):
    def test_empty_archive_keeps_native_retention_possible_without_claiming_screenshots(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'empty.tar';path.touch();result=retain(path,Path(directory)/'out')
            self.assertFalse(result['available']);self.assertIn('empty archive',result['reason']);self.assertEqual(0,result['archive_bytes'])
    def test_corrupt_archive_is_explicitly_unavailable(self):
        with tempfile.TemporaryDirectory() as directory:
            path=Path(directory)/'bad.tar';path.write_bytes(b'bad');self.assertFalse(retain(path,Path(directory)/'out')['available'])
    def test_only_named_public_fixture_is_retained(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);path=root/'public.tar'
            with tarfile.open(path,'w') as bundle:
                for name in ['ux-review/image-text-selection.png','ux-review/not-selected.png']:
                    member=tarfile.TarInfo(name);member.size=3;bundle.addfile(member,io.BytesIO(b'PNG'))
            result=retain(path,root/'out');self.assertTrue(result['available']);self.assertEqual(['image-text-selection.png'],[r['file'] for r in result['retained']]);self.assertFalse((root/'out/not-selected.png').exists())
    def test_traversal_is_not_accepted_as_a_public_fixture(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);path=root/'unsafe.tar'
            with tarfile.open(path,'w') as bundle:
                member=tarfile.TarInfo('../image-text-selection.png');member.size=1;bundle.addfile(member,io.BytesIO(b'x'))
            self.assertFalse(retain(path,root/'out')['available']);self.assertFalse((root/'image-text-selection.png').exists())

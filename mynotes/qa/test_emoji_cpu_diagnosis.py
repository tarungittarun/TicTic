"""Host safety/provenance checks; these do not execute/approve Torch inference."""
import ast
from pathlib import Path
import tempfile
import unittest
from mynotes.tools.ci.emoji_cpu_diagnosis import require_identity, WEIGHT_SHA, archive_members, COMMIT

ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = ROOT/'mynotes/docs/emoji-pilot/41457619334'
SCRIPT = ROOT/'mynotes/tools/ci/emoji_cpu_diagnosis.py'


class EmojiCpuDiagnosisSafetyTest(unittest.TestCase):
    def test_wrong_metadata_rejected_before_any_checkpoint_load(self):
        with tempfile.TemporaryDirectory() as directory:
            (Path(directory)/'report.json').write_text('{}')
            with self.assertRaisesRegex(ValueError,'metadata'):
                require_identity(directory)

    def test_wrong_checkpoint_bytes_rejected_before_any_torch_load(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            for name in ('report.json','class-aliases.json'):
                (root/name).write_bytes((EVIDENCE/name).read_bytes())
            (root/'EXPERIMENTAL-NOT-RUNTIME-emoji-cpu.pt').write_bytes(b'not a trusted checkpoint')
            with self.assertRaisesRegex(ValueError,'checkpoint bytes'):
                require_identity(root)

    def test_expected_checkpoint_is_exact_archived_digest(self):
        import json
        self.assertEqual(WEIGHT_SHA,json.loads((EVIDENCE/'report.json').read_text())['weight_file_sha256'])

    def test_sanity_is_not_misrepresented_or_saved_as_runtime_weights(self):
        source=SCRIPT.read_text()
        ast.parse(source)
        self.assertIn('weights_only=True',source)
        self.assertIn('NOT held-out accuracy',source)
        self.assertIn("'new_weights_saved':False",source)
        self.assertIn("'openmoji_pixels_read':False",source)
        self.assertNotIn('torch.save(',source)
        self.assertNotIn('app/src/',source)


class EmojiArchiveLookupTest(unittest.TestCase):
    def members(self,names):
        import io,zipfile
        memory=io.BytesIO()
        with zipfile.ZipFile(memory,'w') as archive:
            for name in names:
                archive.writestr(name,b'not read by the filename-only helper')
        with zipfile.ZipFile(io.BytesIO(memory.getvalue())) as archive:
            return archive_members(archive)

    def test_retained_fe0f_is_resolved_without_inventing_archive_path(self):
        name='twemoji-'+COMMIT+'/assets/72x72/1f3f3-fe0f-200d-1f308.png'
        self.assertEqual(name,self.members([name])[(0x1f3f3,0x200d,0x1f308)].filename)

    def test_duplicate_normalized_names_fail_closed(self):
        prefix='twemoji-'+COMMIT+'/assets/72x72/'
        with self.assertRaisesRegex(ValueError,'Ambiguous'):
            self.members([prefix+'2764.png',prefix+'2764-fe0f.png'])

    def test_unrelated_paths_are_not_asset_candidates(self):
        self.assertEqual({},self.members(['other/assets/72x72/2764.png','twemoji-'+COMMIT+'/assets/svg/2764.svg']))

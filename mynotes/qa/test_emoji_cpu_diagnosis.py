"""Host safety/provenance checks; these do not execute/approve Torch inference."""
import ast
from pathlib import Path
import tempfile
import unittest
from mynotes.tools.ci.emoji_cpu_diagnosis import require_identity, WEIGHT_SHA

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

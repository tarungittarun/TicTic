import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile
from mynotes.tools.ci import q8_native_derivative as q8
from mynotes.tools.ci.resign_public_benchmark_apks import payload


class Q8DerivativeTest(unittest.TestCase):
    def fixture(self, root):
        baseline, compact = b'public-f16-fixture', b'public-q8-fixture'
        identities = {name: (len(data), hashlib.sha256(data).hexdigest()) for name, data in [('f16', baseline), ('q8_0', compact)]}
        app, model, out = root/'base.apk', root/'q8.bin', root/'candidate.apk'
        model.write_bytes(compact)
        manifest = {'bytes': len(baseline), 'sha256': identities['f16'][1], 'training_run': 41393453222, 'default_engine': False, 'optimizer_steps': 593}
        with zipfile.ZipFile(app, 'w') as z:
            for name, data in {q8.MODEL: baseline, q8.MANIFEST: json.dumps(manifest), 'classes.dex': b'unchanged-code', 'lib/x86_64/libnotes_whisper.so': b'unchanged-native', 'AndroidManifest.xml': b'unchanged-manifest'}.items():
                z.writestr(name, data)
        return identities, app, model, out, manifest

    def test_changes_only_model_integrity_and_preserves_code_and_provenance(self):
        with tempfile.TemporaryDirectory() as tmp:
            identities, app, model, out, manifest = self.fixture(Path(tmp))
            original = app.read_bytes()
            with patch.object(q8, 'IDENTITIES', identities):
                proof = q8.derive(app, model, out)
                q8.extract(app, Path(tmp)/'extracted.bin')
            self.assertEqual(original, app.read_bytes())
            self.assertFalse(proof['adopted'])
            self.assertEqual([q8.MODEL, q8.MANIFEST], proof['changed_payload_entries'])
            with zipfile.ZipFile(out) as z:
                changed = json.loads(z.read(q8.MANIFEST))
            self.assertEqual({k:v for k,v in manifest.items() if k not in ('bytes','sha256')}, {k:v for k,v in changed.items() if k not in ('bytes','sha256')})
            for name in ('classes.dex', 'AndroidManifest.xml', 'lib/x86_64/libnotes_whisper.so'):
                self.assertEqual(payload(app)[name], payload(out)[name])

    def test_wrong_quantization_or_source_and_overwrite_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            identities, app, model, out, _ = self.fixture(Path(tmp))
            with patch.object(q8, 'IDENTITIES', identities):
                with self.assertRaises(ValueError):
                    q8.derive(app, model, app)
                model.write_bytes(b'wrong Q5 or corrupt weights')
                with self.assertRaises(ValueError):
                    q8.derive(app, model, out)
                self.assertFalse(out.exists())
                with self.assertRaises(ValueError):
                    q8.checked(b'wrong baseline', 'f16')

    def test_duplicate_unsigned_payload_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            identities, app, model, out, _ = self.fixture(Path(tmp))
            import warnings
            with warnings.catch_warnings():
                warnings.simplefilter('ignore', UserWarning)
                with zipfile.ZipFile(app, 'a') as z:
                    z.writestr('classes.dex', 'injected duplicate')
            with patch.object(q8, 'IDENTITIES', identities), self.assertRaises(ValueError):
                q8.derive(app, model, out)
            self.assertFalse(out.exists())

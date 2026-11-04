import tempfile
import unittest
import zipfile
from pathlib import Path
from mynotes.tools.ci.apk_size_audit import audit


class ApkSizeAuditTest(unittest.TestCase):
    def test_byte_accounting_separates_models_and_abis(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'sample.apk'
            with zipfile.ZipFile(path,'w',compression=zipfile.ZIP_DEFLATED) as archive:
                for name in ['lib/arm64-v8a/engine.so','assets/voice/model.bin','assets/ocr/eng.traineddata','classes.dex']:
                    archive.writestr(name,b'A'*1000)
            result=audit(path)
            self.assertEqual(4,len(result['categories']))
            self.assertEqual(1000,result['categories']['models/speech']['uncompressed_bytes'])
            self.assertIn('native/arm64-v8a',result['categories'])
            self.assertEqual(path.stat().st_size,result['zip_headers_signing_and_other_bytes']+sum(r['compressed_bytes'] for r in result['categories'].values()))

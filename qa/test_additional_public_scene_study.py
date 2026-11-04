import hashlib,json,unittest
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
class AdditionalPublicSceneStudy(unittest.TestCase):
    def test_exact_public_originals_license_and_manual_references_are_frozen(self):
        root=ROOT/'mynotes/report-assets/public-ocr-scenes-v3';manifest=json.loads((root/'manifest.json').read_text())
        self.assertFalse(manifest['production_packaged']);self.assertEqual(0,manifest['training_steps']);self.assertEqual(2,len(manifest['records']))
        self.assertEqual(manifest['license_sha256'],hashlib.sha256((root/'LICENSE').read_bytes()).hexdigest())
        for row in manifest['records']:
            data=(root/row['file']).read_bytes();self.assertEqual(row['sha256'],hashlib.sha256(data).hexdigest());self.assertEqual(row['git_blob'],hashlib.sha1(('blob '+str(len(data))+'\0').encode()+data).hexdigest())
            self.assertEqual((row['width'],row['height']),Image.open(root/row['file']).size);self.assertTrue(row['reference']);self.assertLess(len(data),2000000)
    def test_real_sign_scene_is_not_misrepresented_as_supported_french(self):
        text=(ROOT/'mynotes/report-assets/public-ocr-scenes-v3/README.md').read_text()
        self.assertIn('French is not a supported OCR language',text);self.assertIn('not current medical advice',text)
    def test_gate_verifies_only_actual_new_native_identity_and_independent_scores(self):
        source=(ROOT/'mynotes/tools/ci/run_additional_public_scene_study.sh').read_text()
        self.assertIn('originalPosterAndObliqueUnsupportedLatinSignsRetainAllOutputs',source);self.assertIn('independent_row(row)',source);self.assertNotIn('verify_recognition_pilot',source)

"""Native behaviour belongs in emulator tests; these validate provenance/privacy/build recipes."""
import hashlib
import json
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
RUNTIME=ROOT/'runtime/src/main'

class OfflineModelContracts(unittest.TestCase):
    def test_ocr_model_files_are_exact_pinned_bytes(self):
        manifest=json.loads((RUNTIME/'assets/ocr/models.json').read_text())
        self.assertEqual('Apache-2.0',manifest['license'])
        self.assertEqual({'eng.traineddata','hin.traineddata','kan.traineddata'},set(manifest['files']))
        for name,expected in manifest['files'].items():
            data=(RUNTIME/'assets/ocr'/name).read_bytes();self.assertEqual(expected['bytes'],len(data));self.assertEqual(expected['sha256'],hashlib.sha256(data).hexdigest())
    def test_speech_archive_and_all_members_are_pinned_not_git_vendored(self):
        manifest=json.loads((RUNTIME/'assets/voice/model.json').read_text())
        self.assertEqual('30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498',manifest['sha256'])
        self.assertGreater(len(manifest['files']),10)
        self.assertFalse((RUNTIME/'assets/voice/speech-model.zip').exists())
        for name,entry in manifest['files'].items():self.assertNotIn('..',Path(name).parts);self.assertEqual(64,len(entry['sha256']))
    def test_native_engines_never_use_platform_or_cloud_recognizers(self):
        for name in ['OfflineOcr.java','OfflineSpeech.java','VoiceCapture.java']:
            source=(RUNTIME/'java/app/space/notes/runtime'/name).read_text()
            for banned in ['import android.speech.','https://','HttpURLConnection','SpeechRecognizer.create']:
                self.assertNotIn(banned,source)
    def test_build_only_recipe_is_protected_from_same_version_reuse(self):
        from mynotes.tools.ci.audit import release_input
        self.assertTrue(release_input('mynotes/tools/models/prepare_voice.py'))
        self.assertIn("preBuild.dependsOn prepareOfflineSpeechModel",(ROOT/'runtime/build.gradle').read_text())
    def test_public_speech_sample_has_fixed_hash_and_is_test_only(self):
        sample=(ROOT/'app/src/androidTest/assets/voice/test.wav').read_bytes()
        self.assertEqual('dcfea5712c43a43ba7ae8083afb39d36993e5a69c46e88b68aaa72b65cb615bb',hashlib.sha256(sample).hexdigest())
        self.assertFalse((RUNTIME/'assets/voice/test.wav').exists())
    def test_licenses_ship_in_app_assets(self):
        directory=RUNTIME/'assets/licenses'
        for name in ['Tesseract4Android-Apache-2.0.txt','tessdata-Apache-2.0.txt','LICENSE-vosk.txt','LICENSE-jna.txt','leptonica.txt','jpeg.txt','png.txt']:
            self.assertGreater((directory/name).stat().st_size,500)

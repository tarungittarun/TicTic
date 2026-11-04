from pathlib import Path
import unittest
import yaml


class RecognitionDiskTest(unittest.TestCase):
    def test_affected_emulator_jobs_bound_userdata_without_test_retries(self):
        root = Path(__file__).resolve().parents[1]/'.github/workflows'
        for name in ('notes-module-e2e.yml', 'notes-refinement-native.yml', 'notes-realworld-ocr-pilot.yml', 'notes-q8-native-smoke.yml', 'notes-candidate61-native.yml', 'notes-candidate61-voice-repair.yml', 'notes-kan-best-native.yml'):
            workflow = yaml.safe_load((root/name).read_text())
            emulators = [step for job in workflow['jobs'].values() for step in job.get('steps', []) if 'android-emulator-runner@' in step.get('uses', '').lower()]
            self.assertTrue(emulators, name)
            for step in emulators:
                self.assertEqual('4G', step['with']['disk-size'], name)
        replay = (root/'notes-candidate61-native.yml').read_text()
        self.assertIn('run-id: 41767304608', replay)
        self.assertIn('3012a4bbdab4d3e39200aba174b704f304b81b0cdf128b5fa1ec088e54de7b44', replay)
        self.assertNotIn('gradlew', replay)
        self.assertIn('if: always()', replay)

"""Planning/isolation checks only; no APK or Android runtime verification."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]


class NotesM0Contracts(unittest.TestCase):
    def test_inventory_has_exactly_one_of_every_original_id(self):
        text = (ROOT/'docs/notes/REQUIREMENTS.md').read_text()
        self.assertEqual([f'K{i:03}' for i in range(1,101)],
                         re.findall(r'^### (K\d+)$',text,re.M))

    def test_added_requirements_are_unique_and_present(self):
        text = (ROOT/'docs/notes/REQUIREMENTS.md').read_text()
        ids = re.findall(r'^\| ((?:DG|ST|ML|HX|NF)-\d+) \|',text,re.M)
        self.assertEqual(len(ids),len(set(ids)))
        self.assertEqual(21,len(ids))

    def test_reference_protocol_has_no_android_or_network_dependencies(self):
        text = (ROOT/'tools/notes_storage_probe.py').read_text()
        for dependency in ['requests','urllib','android','subprocess']:
            self.assertNotRegex(text,rf'^(?:import|from) {dependency}',dependency)

    def test_current_app_does_not_consume_planned_runtime(self):
        for filename in ['app/build.gradle','feature-notes/build.gradle','calc/build.gradle']:
            text=(ROOT/filename).read_text()
            self.assertNotIn("project(':notes-runtime')",text)
            self.assertNotIn("project(':notes-domain')",text)

    def test_baseline_does_not_drop_offline_transcription(self):
        text=(ROOT/'NOTES-DEVELOPMENT-BASELINE.md').read_text()
        self.assertIn('Automatic speech transcription — required, not deferred',text)
        self.assertIn('No INTERNET permission',text)
        self.assertIn('Playback and transcription of existing/imported audio',text)

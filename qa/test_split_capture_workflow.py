import unittest
from pathlib import Path

class SplitCaptureWorkflow(unittest.TestCase):
    def test_report_only_fetches_pinned_source_before_builder(self):
        workflow=Path('.github/workflows/notes-candidate63-composite-provenance.yml').read_text()
        sha='3aa67629e1f1748531f348308aa53e833b2becc7'
        fetch='git fetch --no-tags --depth=1 origin '+sha
        self.assertIn(fetch,workflow)
        self.assertIn('git cat-file -e '+sha+'^{tree}',workflow)
        self.assertLess(workflow.index(fetch),workflow.index('python3 -m mynotes.tools.ci.build_boot_supplement_report'))
        self.assertNotIn('android-emulator-runner',workflow)
        self.assertNotIn('gradlew',workflow)
        self.assertIn("captures={'light-portrait-ocr':41788513984,'dark-landscape-ocr':41787131044}",workflow)
        self.assertIn('run-id: 41788513984',workflow)
        self.assertIn('run-id: 41787131044',workflow)

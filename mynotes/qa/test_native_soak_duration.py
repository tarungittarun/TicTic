"""Guard the dedicated long-duration acceptance against accidental shortening."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]

class NativeSoakDurationContracts(unittest.TestCase):
    def test_dedicated_workflow_requires_120_minutes_with_timeout_headroom(self):
        workflow=(ROOT.parent/'.github/workflows/notes-soak.yml').read_text()
        script=(ROOT/'tools/ci/native_soak.sh').read_text()
        java=(ROOT/'app/src/androidTest/java/app/space/notes/NativeSoakTest.java').read_text()
        self.assertIn('120-minute native/durable cycles',workflow)
        self.assertIn('timeout-minutes: 150',workflow)
        self.assertIn('api: [26, 35]',workflow)
        self.assertIn('-e soakMinutes 120',script)
        self.assertIn("requested_minutes']==120",script)
        self.assertIn("elapsed_ms']>=120*60000",script)
        self.assertIn('minutes<=120',java)

    def test_endurance_report_preserves_physical_interaction_limit(self):
        java=(ROOT/'app/src/androidTest/java/app/space/notes/NativeSoakTest.java').read_text()
        self.assertIn('programmatic editor commands are not touch/navigation, physical battery/thermal',java)

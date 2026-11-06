"""Guard exhaustive, nonpublishing qualification; emulator results are not physical acceptance."""
from pathlib import Path
import unittest
import yaml
from mynotes.tools.ci.test_plan import build_plan
from mynotes.tools.ci.refinement_plan import selected_cases


class FullCandidateQualificationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = Path('.github/workflows/notes-full71.yml').read_text()
        cls.workflow = yaml.safe_load(cls.text)

    def test_all_profiles_have_disjoint_exhaustive_shards(self):
        profiles = {}
        for row in self.workflow['jobs']['native']['strategy']['matrix']['include']:
            self.assertEqual(row['group'], 'all')
            key = tuple(row[k] for k in ('api', 'font', 'night', 'orientation', 'motion'))
            profiles.setdefault(key, []).append(row['shard'])
        self.assertEqual(len(profiles), 6)
        for shards in profiles.values():
            self.assertEqual(sorted(shards), [0, 1])
        plan = build_plan()
        a, b = [set(selected_cases(plan, 'all', shard)) for shard in (0, 1)]
        self.assertFalse(a & b)
        self.assertEqual(a | b, set(selected_cases(plan, 'all')))

    def test_checks_not_just_adb_success(self):
        for needle in ('results jvm', 'quality_reports --expected', 'verify_pdf_features',
                       "android(path.read_text", "if not result['ok']", 'offline_guest',
                       'assert_manifest(badging,xml)', '156213948', 'lintDebug :notes:lintRelease'):
            self.assertIn(needle, self.text)
        self.assertFalse(self.workflow['jobs']['native']['strategy']['fail-fast'])

    def test_migration_preserves_data_and_uses_explicit_increasing_versions(self):
        self.assertIn('"--old-code","69","--candidate-code","71"', self.text)
        self.assertIn('03d839d58b828ac72e211b69a1904e00e5f44f9f5c2b37704d4312501431d167', self.text)
        self.assertNotIn('adb uninstall', self.text)
        self.assertNotIn('pm clear', self.text)
        self.assertNotIn('releases upload', self.text)
        self.assertNotIn('contents: write', self.text)


if __name__ == '__main__':
    unittest.main()

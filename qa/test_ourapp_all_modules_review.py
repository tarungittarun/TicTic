"""Document/source integrity only: these tests do not run or certify either app UX."""
import hashlib
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / 'mynotes/docs/comparisons/ourapp-vs-obsidian'
OBSIDIAN = ROOT / 'mynotes/docs/comparisons/obsidian/audit/42182162974'

class AllModuleReviewIntegrity(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = json.loads((REPORT / 'all-modules-review.json').read_text())
        cls.manifest = json.loads((REPORT / 'all-modules-evidence-manifest.json').read_text())

    def test_all_chapters_have_scoped_evidence_and_unrun_manual_task(self):
        self.assertEqual([m['id'] for m in self.data['modules']], ['X%02d' % n for n in range(1,38)])
        for m in self.data['modules']:
            for field in ['title','scope','evidence','observed','comparison','judgment','direction','task','acceptance','sources','kind','priority']:
                self.assertTrue(m[field], (m['id'],field))
            self.assertEqual(m['live_status'], 'NOT RUN')
            self.assertIsNone(m['timing'])
            self.assertTrue(all(ref in self.data['assets'] for ref in m['evidence']))

    def test_exact_pinned_core_registry_no_omissions_or_invented_modules(self):
        captured = json.loads((OBSIDIAN / 'walkthrough-feature-inventory.json').read_text())['plugins']
        rows = self.data['obsidian_module_registry']
        self.assertEqual(len(rows),29)
        self.assertEqual({r['id']:r['enabled'] for r in rows}, {r['id']:r['enabled'] for r in captured})
        self.assertEqual(sum(r['exposed_control'] for r in rows),28)
        module_ids = {m['id'] for m in self.data['modules']}
        self.assertTrue(all(r['module'] in module_ids for r in rows))
        for r in rows:
            owners = [m['id'] for m in self.data['modules'] if r['id'] in m['plugins']]
            self.assertEqual(owners,[r['module']])

    def test_all_205_command_receipts_retain_original_status(self):
        original = json.loads((OBSIDIAN / 'command-coverage.json').read_text())
        rows = self.data['obsidian_command_routing']
        self.assertEqual(len(rows),205)
        self.assertEqual({r['id']:(r['name'],r['status']) for r in rows}, {r['id']:(r['name'],r['status']) for r in original})
        self.assertTrue(all(r['module'] in {m['id'] for m in self.data['modules']} for r in rows))

    def test_all_retained_settings_overviews_are_routed(self):
        original = json.loads((OBSIDIAN / 'settings-feature-results.json').read_text())
        sections = [r['feature'][10:] for r in original if r['feature'].startswith('Settings:')]
        self.assertEqual([r['section'] for r in self.data['obsidian_settings_sections']], sections)
        self.assertEqual(len(sections),20)

    def test_original_native_frames_exist_and_hashes_match(self):
        self.assertEqual(set(self.data['assets']),set(self.manifest['assets']))
        self.assertEqual(len(self.manifest['assets']),104)
        for asset in self.manifest['assets'].values():
            path = ROOT / asset['path']
            self.assertTrue(path.is_file())
            self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(),asset['sha256'])
            self.assertFalse(asset['source_pixels_modified'])

    def test_source_anchors_and_reviewed_bytes_match(self):
        for path,digest in self.manifest['referenced_source_file_hashes'].items():
            self.assertEqual(hashlib.sha256((ROOT / path).read_bytes()).hexdigest(),digest)
        for module in self.data['modules']:
            for source in module['sources']:
                text = (ROOT / source['path']).read_text()
                for anchor in source['anchor'].split('/'):
                    if anchor:self.assertIn(anchor,text)

    def test_no_fabricated_live_measurements_or_bug_reproduction(self):
        for document in [self.data,self.manifest]:
            self.assertEqual(document['new_live_sessions'],0)
            self.assertEqual(document['new_runtime_bugs_reproduced'],0)
        self.assertIsNone(self.data['matched_task_timings'])
        self.assertIsNone(self.manifest['matched_timing_results'])
        chapters = {m['id']:m for m in self.data['modules']}
        self.assertIn('8 MiB',chapters['X24']['observed'])
        self.assertIn('not all',chapters['X19']['observed'])
        self.assertIn('No equivalent',chapters['X18']['comparison'])

    def test_original_phase_one_publication_is_preserved(self):
        receipt = json.loads((REPORT / 'phase1-report-integrity.json').read_text())
        self.assertEqual(hashlib.sha256((REPORT / 'OurApp_vs_Obsidian_Phase1.pdf').read_bytes()).hexdigest(),receipt['pdf_sha256'])
        self.assertEqual(receipt['pdf_pages'],39)

    def test_main_pdf_integrity_receipt_and_module_pages(self):
        receipt = json.loads((REPORT / 'report-integrity.json').read_text())
        self.assertEqual(receipt['pages'],95)
        self.assertEqual(receipt['phase1_appendix_pages'],39)
        self.assertEqual(set(receipt['module_pages']),{m['id'] for m in self.data['modules']})
        self.assertTrue(receipt['one_complete_page_per_module_chapter'])
        self.assertTrue(receipt['all_text_within_page_bounds'])
        self.assertEqual(hashlib.sha256((REPORT / 'OurApp_vs_Obsidian.pdf').read_bytes()).hexdigest(),receipt['sha256'])

"""Exact guide distribution checks, not Android visual/interaction acceptance."""
import hashlib,json,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
class HandbookContract(unittest.TestCase):
 def test_hundred_plus_page_handbook_asset_matches_deliverable(self):
  guide=ROOT/'docs/user-guide';book=(guide/'MyNotes-Illustrated-User-Guide.pdf').read_bytes();manifest=json.loads((guide/'guide-manifest.json').read_text());self.assertEqual(116,manifest['pages']);self.assertEqual(92,manifest['screen_frames']);self.assertEqual(len(book),manifest['bytes']);self.assertEqual(hashlib.sha256(book).hexdigest(),manifest['sha256']);self.assertLess(len(book),32*1024*1024);self.assertTrue(book.startswith(b'%PDF-'));self.assertEqual(book,(ROOT/'app/src/androidTest/assets/guide/handbook.pdf').read_bytes());self.assertEqual(manifest,json.loads((ROOT/'app/src/androidTest/assets/guide/manifest.json').read_text()))
 def test_every_source_screen_has_a_described_route_and_warning(self):
  lines=[line.split('|') for line in (ROOT/'docs/user-guide/screen-guide.tsv').read_text().splitlines() if line and not line.startswith('#')];self.assertEqual(92,len(lines));self.assertEqual(92,len({line[0] for line in lines}));self.assertTrue(all(len(line)==4 and all(line) for line in lines))

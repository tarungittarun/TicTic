"""Frozen public reference/source checks; not proof of OCR accuracy."""
from pathlib import Path
import hashlib,json,unittest
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
class PublicSceneInputs(unittest.TestCase):
    def test_original_blobs_and_derived_crops_are_bound(self):
        root=ROOT/'mynotes/report-assets/public-ocr-scenes-v2';data=json.loads((root/'manifest.json').read_text())
        self.assertEqual(5,len(data['records']));self.assertFalse(data['production_packaged'])
        for row in data['records']:
            raw=(root/row['file']).read_bytes()
            with Image.open(root/row['file']) as image:w,h=image.size
            x,y,r,b=row['crop'];self.assertTrue(0<=x<r<=w and 0<=y<b<=h)
            if 'sha256' in row:
                self.assertEqual(row['sha256'],hashlib.sha256(raw).hexdigest())
                self.assertEqual(row['git_blob'],hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest())
        self.assertEqual(data['license_sha256'],hashlib.sha256((root/'LICENSE').read_bytes()).hexdigest())
    def test_negative_scene_controls_are_not_credited_as_positive_text_photos(self):
        data=json.loads((ROOT/'mynotes/report-assets/public-ocr-scenes-v2/manifest.json').read_text())
        self.assertEqual(2,sum(row['reference']=='' for row in data['records']))
        self.assertIn('Not five independent source photos',data['scope'])
    def test_font_comparison_cannot_change_production_output(self):
        source=(ROOT/'mynotes/app/benchmarks/java/app/space/notes/SmallFontSceneStudy.java').read_text()
        for token in ['Counterbalanced order','"production_changed",false','"source_pixels_unchanged",true','persist("small-font-paired-study",rows,144)']:
            self.assertIn(token,source)
        self.assertFalse((ROOT/'mynotes/app/src/main/java/app/space/notes/SmallFontSceneStudy.java').exists())

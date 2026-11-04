import hashlib
import json
import unittest
from mynotes.tools.ci.verify_public_screenshots import score,edits,ROOT

class PublicScreenshotBaseline(unittest.TestCase):
    def setUp(self):self.manifest=json.loads((ROOT/'manifest.json').read_text())
    def report(self):
        rows=[]
        for record in self.manifest['records']:
            for variant,condition in enumerate(('original','half-size-derived','jpeg50-derived')):
                divisor=2 if variant==1 else 1
                rows.append(dict(id=f'{record["id"]}-{variant}',condition=condition,source_sha256=record['sha256'],input_sha256=record['sha256'],width=record['width']//divisor,height=record['height']//divisor,elapsed_ms=100,status='OK',reference=record['reference'],recognized=record['reference'],source_pixels_unchanged=True,cer=0,accuracy_target_met=True))
        return {'cases':rows}
    def test_pinned_original_git_objects_and_license_bytes(self):
        self.assertFalse(self.manifest['production_packaged']);self.assertEqual('Apache-2.0',self.manifest['license'])
        from PIL import Image
        for record in self.manifest['records']:
            data=(ROOT/record['file']).read_bytes()
            self.assertEqual(record['git_blob'],hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest())
            self.assertEqual(record['sha256'],hashlib.sha256(data).hexdigest())
            with Image.open(ROOT/record['file']) as image:self.assertEqual((record['width'],record['height']),image.size)
        data=(ROOT/'LICENSE').read_bytes()
        self.assertEqual(self.manifest['license_git_blob'],hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest())
        self.assertEqual(self.manifest['license_sha256'],hashlib.sha256(data).hexdigest())
    def test_complete_independently_scored_cases(self):self.assertTrue(score(self.report(),self.manifest)['all_accuracy_targets_met'])
    def test_duplicate_missing_and_unexpected_records_rejected(self):
        for change in ('duplicate','missing','unknown'):
            report=self.report()
            if change=='duplicate':report['cases'][-1]=report['cases'][0]
            if change=='missing':report['cases'].pop()
            if change=='unknown':report['cases'][-1]['id']='unknown'
            with self.assertRaises(ValueError):score(report,self.manifest)
    def test_engine_score_cannot_override_incorrect_recognition(self):
        report=self.report();row=report['cases'][0];row['recognized']='';row['cer']=1;row['accuracy_target_met']=False
        self.assertFalse(score(report,self.manifest)['all_accuracy_targets_met'])
        row['accuracy_target_met']=True
        with self.assertRaises(ValueError):score(report,self.manifest)
    def test_reference_source_dimensions_and_pixels_cannot_change(self):
        for key,value in [('reference','rewritten oracle'),('source_sha256','0'*64),('width',1),('source_pixels_unchanged',False),('elapsed_ms',float('nan'))]:
            report=self.report();report['cases'][0][key]=value
            with self.assertRaises(ValueError):score(report,self.manifest)
    def test_operational_error_is_never_accuracy_pass(self):
        report=self.report();report['cases'][0]['status']='ERROR'
        self.assertFalse(score(report,self.manifest)['all_accuracy_targets_met'])
    def test_distance_keeps_case_punctuation_and_digits(self):
        self.assertEqual(1,edits('Hello','hello'));self.assertEqual(1,edits('100%','100'));self.assertEqual(1,edits('5:32','5:33'))

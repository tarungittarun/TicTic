import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from mynotes.tools.ci.published import validate_receipt,resolve,BASE

class PublicationReceipts(unittest.TestCase):
    def receipt(self,payload=b'apk fixture'):
        return {'schema':1,'storage':'github-release','filename':'MyNotes-v0.6.0-7.apk','url':BASE+'mynotes-v0.6.0-7/MyNotes-v0.6.0-7.apk','applicationId':'app.space.notes','versionCode':7,'versionName':'0.6.0','source':'a'*40,'certificate':'b'*64,'sha256':hashlib.sha256(payload).hexdigest(),'bytes':len(payload)}
    def test_receipt_requires_exact_reviewed_asset_origin_and_identity(self):
        self.assertEqual(7,validate_receipt(self.receipt())['versionCode'])
        for key,value in [('url','https://example.invalid/app.apk'),('filename','../../app.apk'),('source','bad'),('applicationId','other'),('versionCode',0),('sha256','bad')]:
            data=self.receipt();data[key]=value
            with self.assertRaises(ValueError,msg=key):validate_receipt(data)
    def test_cached_asset_bytes_are_still_verified(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)/'releases';root.mkdir();cache=Path(d)/'cache';cache.mkdir();receipt=self.receipt();(root/'file.release.json').write_text(json.dumps(receipt));asset=cache/(receipt['sha256']+'-'+receipt['filename']);asset.write_bytes(b'apk fixture')
            with patch('urllib.request.urlopen',side_effect=AssertionError('must use verified cached bytes')):
                paths=resolve(root,cache);self.assertEqual([asset],paths);self.assertEqual('a'*40,Path(str(asset)+'.source.txt').read_text().strip())
    def test_corrupt_cache_never_silently_accepted(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)/'releases';root.mkdir();cache=Path(d)/'cache';cache.mkdir();receipt=self.receipt();(root/'file.release.json').write_text(json.dumps(receipt));asset=cache/(receipt['sha256']+'-'+receipt['filename']);asset.write_bytes(b'changed')
            with patch('urllib.request.urlopen',side_effect=OSError('no network')):
                with self.assertRaises(OSError):resolve(root,cache)
    def test_local_and_remote_same_filename_cannot_conflict(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);receipt=self.receipt();(root/receipt['filename']).write_bytes(b'apk fixture');(root/'file.release.json').write_text(json.dumps(receipt))
            with self.assertRaisesRegex(ValueError,'Duplicate'):resolve(root,root/'cache')

    def test_large_publisher_creates_draft_then_exposes_without_clobber(self):
        from mynotes.tools.ci import publish
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);accepted=root/'accepted';accepted.mkdir();notes=root/'mynotes';(notes/'releases').mkdir(parents=True)
            receipt=self.receipt();name=receipt['filename'];apk=accepted/name
            with apk.open('wb') as stream:stream.truncate(8*1024*1024+1)
            audit={**receipt,'publish_eligible':True,'accepted_sha256':publish.sha(apk)}
            (accepted/'audit.json').write_text(json.dumps(audit));(accepted/(name+'.sha256')).write_text(audit['accepted_sha256']+'  '+name+'\n')
            with patch.object(publish,'NOTES',notes),patch.object(publish,'run') as run:
                publish.publish(accepted)
                self.assertEqual(2,run.call_count);create,edit=[c.args for c in run.call_args_list]
                self.assertEqual(('gh','release','create'),create[:3]);self.assertIn('--draft',create);self.assertNotIn('--clobber',create)
                self.assertEqual(('gh','release','edit'),edit[:3]);self.assertIn('--draft=false',edit)
                self.assertFalse((notes/'releases'/name).exists());remote=json.loads((notes/'releases'/(name+'.release.json')).read_text());self.assertEqual(audit['accepted_sha256'],remote['sha256'])
                with self.assertRaisesRegex(ValueError,'never overwrite'):publish.publish(accepted)
                self.assertEqual(2,run.call_count)
    def test_publisher_rejects_changed_accepted_bytes_before_external_calls(self):
        from mynotes.tools.ci import publish
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);name=self.receipt()['filename'];(root/name).write_bytes(b'changed');(root/'audit.json').write_text(json.dumps({'filename':name,'publish_eligible':True,'accepted_sha256':'0'*64}))
            with patch.object(publish,'run') as run:
                with self.assertRaisesRegex(ValueError,'changed'):publish.publish(root)
                run.assert_not_called()

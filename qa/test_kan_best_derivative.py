import hashlib
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile
from mynotes.tools.ci import kan_best_derivative as kan
from mynotes.tools.ci.resign_public_benchmark_apks import payload


def blob(data):
    return hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest(),len(data)


class KannadaWeightDerivativeTest(unittest.TestCase):
    def fixture(self, root):
        old, model, notice=b'public-fast-fixture',b'public-best-fixture',b'public-license-fixture'
        fast=hashlib.sha256(old).hexdigest()
        app,out=root/'base.apk',root/'research.apk'
        manifest={'repository':'fast','files':{'kan.traineddata':{'sha256':fast,'bytes':len(old)},'eng.traineddata':{'unchanged':True}}}
        with zipfile.ZipFile(app,'w') as z:
            for name,data in {kan.MODEL:old,kan.MANIFEST:json.dumps(manifest),'classes.dex':b'fixed-code','lib/x86_64/libtess.so':b'fixed-native','AndroidManifest.xml':b'fixed-package-permissions'}.items():z.writestr(name,data)
        return app,out,model,notice,patch.multiple(kan,FAST=fast,BEST=blob(model),LICENSE=blob(notice))

    def test_only_kannada_integrity_and_provenance_change_with_exact_license_added(self):
        with tempfile.TemporaryDirectory() as tmp:
            app,out,model,notice,identities=self.fixture(Path(tmp));original=app.read_bytes()
            with identities:proof=kan.derive(app,out,model,notice)
            self.assertEqual(original,app.read_bytes())
            self.assertFalse(proof['adopted']);self.assertFalse(proof['user_apk_changed'])
            self.assertEqual({kan.MODEL,kan.MANIFEST},set(proof['changed_payload_entries']))
            before,after=payload(app),payload(out)
            self.assertEqual({kan.NOTICE},set(after)-set(before))
            for key in ('classes.dex','lib/x86_64/libtess.so','AndroidManifest.xml'):self.assertEqual(before[key],after[key])
            with zipfile.ZipFile(out) as z:
                self.assertEqual(notice,z.read(kan.NOTICE))
                manifest=json.loads(z.read(kan.MANIFEST))
                self.assertEqual({'unchanged':True},manifest['files']['eng.traineddata'])
                self.assertEqual(kan.REV,manifest['files']['kan.traineddata']['commit'])

    def test_corrupt_weights_license_and_in_place_overwrite_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            app,out,model,notice,identities=self.fixture(Path(tmp))
            with identities:
                for bad_model,bad_notice in [(model+b'corrupt',notice),(model,notice+b'corrupt')]:
                    with self.assertRaises(ValueError):kan.derive(app,out,bad_model,bad_notice)
                with self.assertRaises(ValueError):kan.derive(app,app,model,notice)
            self.assertFalse(out.exists())

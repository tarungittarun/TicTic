import unittest
from mynotes.tools.verify_update import verify

class ReleaseUpdatePolicy(unittest.TestCase):
    def test_new_version_same_identity_passes(self):
        verify(('app.space.notes',2,'0.2.0','cert'),[('app.space.notes',1,'0.1.0','cert')])
    def test_signing_change_rejected(self):
        with self.assertRaises(ValueError):
            verify(('app.space.notes',2,'0.2.0','new'),[('app.space.notes',1,'0.1.0','old')])
    def test_same_code_rejected(self):
        with self.assertRaises(ValueError):
            verify(('app.space.notes',1,'0.2.0','cert'),[('app.space.notes',1,'0.1.0','cert')])
    def test_package_change_rejected(self):
        with self.assertRaises(ValueError):verify(('different.id',2,'0.2.0','cert'),[])
    def test_downgrade_rejected(self):
        with self.assertRaises(ValueError):
            verify(('app.space.notes',2,'0.2.0','cert'),[('app.space.notes',3,'0.3.0','cert')])

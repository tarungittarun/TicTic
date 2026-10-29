import unittest
from mynotes.tools.ci.native_environment import physical_size, rotation_for


class NativeEnvironmentTests(unittest.TestCase):
    def test_phone_portrait_is_natural(self):
        self.assertEqual(0, rotation_for((1080, 1920), 'portrait'))

    def test_phone_landscape_rotates(self):
        self.assertEqual(1, rotation_for((1080, 1920), 'landscape'))

    def test_tablet_natural_landscape_must_rotate_for_portrait(self):
        self.assertEqual(1, rotation_for((2048, 1536), 'portrait'))
        self.assertEqual(0, rotation_for((2048, 1536), 'landscape'))

    def test_override_must_not_replace_natural_dimensions(self):
        self.assertEqual((2048, 1536), physical_size('Physical size: 2048x1536\nOverride size: 800x1200'))

    def test_invalid_display_is_rejected(self):
        for output in ('', 'Override size: 800x1200', 'Physical size: 0x1200'):
            with self.assertRaises(ValueError):
                physical_size(output)

    def test_orientation_is_not_silently_assumed(self):
        with self.assertRaises(ValueError):
            rotation_for((1080, 1920), 'any')

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

    def test_home_preflight_requires_three_stable_focus_samples_without_touching_app(self):
        from unittest.mock import patch
        from mynotes.tools.ci.native_environment import settle_home
        focused='mCurrentFocus=Window{abc u0 com.google.android.apps.nexuslauncher/.NexusLauncherActivity}'
        with patch('mynotes.tools.ci.native_environment.adb',return_value=focused) as adb, patch('mynotes.tools.ci.native_environment.time.sleep'):
            settle_home()
        self.assertEqual(4,adb.call_count)
        self.assertIn('com.google.android.apps.nexuslauncher/.NexusLauncherActivity',adb.call_args_list[0].args)
        self.assertFalse(any('app.space.notes' in str(call) for call in adb.call_args_list))

    def test_unfocused_home_is_a_hard_failure_not_a_suppressed_app_failure(self):
        from unittest.mock import patch
        from mynotes.tools.ci.native_environment import settle_home
        with patch('mynotes.tools.ci.native_environment.adb',return_value='mCurrentFocus=null'),patch('mynotes.tools.ci.native_environment.time.sleep'),patch('mynotes.tools.ci.native_environment.snapshot') as snapshot:
            with self.assertRaisesRegex(RuntimeError,'never obtained a stable focused window'):
                settle_home()
            snapshot.assert_called_once_with('home-failed')

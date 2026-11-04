import unittest
from unittest.mock import patch
from mynotes.tools.ci import native_environment as env

READY='mCurrentFocus=Window{abc u0 app.space.notes.debug/app.space.notes.NotesActivity}'

class NotesWindowPreflight(unittest.TestCase):
    def test_launch_notes_and_require_three_actual_focused_windows(self):
        with patch.object(env,'adb',side_effect=['',READY,READY,READY]) as adb,patch.object(env.time,'sleep'):
            env.settle_app()
        self.assertEqual(('am','start','-W','-n','app.space.notes.debug/app.space.notes.NotesActivity'),adb.call_args_list[0].args)
        self.assertFalse(any('force-stop' in call.args for call in adb.call_args_list))

    def test_system_error_dialog_cannot_count_as_notes_focus(self):
        error='mCurrentFocus=Window{abc u0 Application Not Responding: app.space.notes.debug}'
        with patch.object(env,'adb',return_value=error),patch.object(env.time,'sleep'),patch.object(env,'snapshot') as snapshot:
            with self.assertRaisesRegex(RuntimeError,'ANR dialogs remain visible'):env.settle_app()
            snapshot.assert_called_once_with('app-failed')

    def test_transition_resets_stability_without_hiding_errors(self):
        with patch.object(env,'adb',side_effect=['',READY,READY,'mCurrentFocus=null',READY,READY,READY]) as adb,patch.object(env.time,'sleep'):
            env.settle_app()
        self.assertEqual(7,adb.call_count)

    def test_current_display_query_not_historical_last_anr_dump(self):
        with patch.object(env,'adb',side_effect=['',READY,READY,READY]) as adb,patch.object(env.time,'sleep'):
            env.settle_app()
        self.assertTrue(all(call.args==('dumpsys','window','displays') for call in adb.call_args_list[1:]))

    def test_only_exact_external_launcher_anr_can_be_recovered_once(self):
        error='mCurrentFocus=Window{abc u0 Application Not Responding: com.google.android.apps.nexuslauncher}'
        with patch.object(env,'adb',side_effect=['',error,'','',READY,READY,READY]) as adb,patch.object(env.time,'sleep'),patch.object(env,'snapshot') as snapshot:
            env.settle_app()
        snapshot.assert_called_once_with('external-launcher-anr')
        stops=[c.args for c in adb.call_args_list if 'force-stop' in c.args]
        self.assertEqual([('am','force-stop','com.google.android.apps.nexuslauncher')],stops)

    def test_app_focus_alongside_error_overlay_cannot_pass(self):
        error=READY+'\n'+'mCurrentFocus=Window{err u0 Application Not Responding: app.space.notes.debug}'
        with patch.object(env,'adb',return_value=error) as adb,patch.object(env.time,'sleep'),patch.object(env,'snapshot'):
            with self.assertRaisesRegex(RuntimeError,'ANR dialogs remain visible'):env.settle_app()
        self.assertFalse(any('force-stop' in c.args for c in adb.call_args_list))

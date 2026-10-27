from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'
class LocalSurfaceContracts(unittest.TestCase):
    def test_no_exact_alarm_or_network_permission_is_added(self):
        manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
        for forbidden in ['SCHEDULE_EXACT_ALARM','USE_EXACT_ALARM','INTERNET','ACCESS_NETWORK_STATE']:
            self.assertNotIn('android.permission.'+forbidden,manifest)
        self.assertIn('POST_NOTIFICATIONS',manifest);self.assertIn('RECEIVE_BOOT_COMPLETED',manifest)
    def test_receiver_uses_private_snapshot_off_main_and_finishes_broadcast(self):
        for name in ['ReminderReceiver','NotesWidget']:
            text=(JAVA/(name+'.java')).read_text()
            for expected in ['LocalSystemSurfaces.','goAsync()','pending.finish()','new Thread']:
                self.assertIn(expected,text)
    def test_pending_intents_immutable_and_distinct_by_note_uri(self):
        text=(JAVA/'LocalReminders.java').read_text()
        self.assertIn('FLAG_IMMUTABLE',text);self.assertIn('setData(Uri.parse',text);self.assertIn('Uri.encode(id)',text)
        self.assertIn('setAndAllowWhileIdle',text);self.assertNotIn('setExact',text)
        self.assertIn('note.reminderAt!=expected',text)
    def test_widget_is_resizable_replaceable_and_not_a_remote_service(self):
        text=(ROOT/'runtime/src/main/res/xml/notes_widget_info.xml').read_text()
        self.assertIn('horizontal|vertical',text);self.assertIn('home_screen',text)
        self.assertNotIn('RemoteViewsService',(JAVA/'NotesWidget.java').read_text())
    def test_reminder_is_part_of_portable_document_not_only_preferences(self):
        text=(JAVA/'DocumentCodec.java').read_text()
        self.assertIn('put("reminderAt",n.reminderAt)',text);self.assertIn('version>=5?n.getLong("reminderAt"):0',text)

    def test_atomic_snapshots_and_projection_delivery_have_shared_locks(self):
        text=(JAVA/'NotesRepository.java').read_text();self.assertIn('synchronized(SNAPSHOT_LOCK)',text);self.assertIn('atomicRead(file)',text)
        text=(JAVA/'LocalSystemSurfaces.java').read_text();self.assertIn('static synchronized void refresh',text);self.assertIn('static synchronized void deliver',text);self.assertIn('localSnapshot()',text)
        self.assertIn('synchronized(TRANSCRIPT_LOCK)',(JAVA/'VoiceFiles.java').read_text())

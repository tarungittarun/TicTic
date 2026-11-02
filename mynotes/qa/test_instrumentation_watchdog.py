import sys
import tempfile
import unittest
from pathlib import Path
from mynotes.tools.ci.instrumentation_watchdog import run


class InstrumentationWatchdogTests(unittest.TestCase):
    def test_retains_failure_and_output(self):
        with tempfile.TemporaryDirectory() as directory:
            output=Path(directory)/'output.txt'
            code=run([sys.executable,'-c','print("evidence",flush=True);raise SystemExit(42)'],output,poll_seconds=.01)
            self.assertEqual(42,code)
            self.assertIn('evidence',output.read_text())
            self.assertFalse((output.parent/'refinement-native-watchdog.txt').exists())

    def test_stall_is_failure_not_success(self):
        with tempfile.TemporaryDirectory() as directory:
            output=Path(directory)/'output.txt'
            code=run([sys.executable,'-c','import time;time.sleep(60)'],output,stall_seconds=.05,poll_seconds=.01)
            self.assertEqual(124,code)
            self.assertIn('FAIL:',(output.parent/'refinement-native-watchdog.txt').read_text())

    def test_progress_resets_stall_deadline(self):
        with tempfile.TemporaryDirectory() as directory:
            output=Path(directory)/'output.txt'
            code=run([sys.executable,'-c','import time\nfor i in range(8):\n print(i,flush=True);time.sleep(.04)'],output,stall_seconds=.2,poll_seconds=.01)
            self.assertEqual(0,code)
            self.assertEqual(8,len(output.read_text().splitlines()))

    def test_debug_observer_does_not_depend_on_main_thread(self):
        source=(Path(__file__).resolve().parents[1]/'app/src/androidTest/java/app/space/notes/HangDiagnostics.java').read_text()
        self.assertIn('Thread.getAllStackTraces()',source)
        self.assertIn('200000',source)
        self.assertNotIn('runOnMainSync',source)
        self.assertNotIn('runOnUiThread',source)
        wrapper=(Path(__file__).resolve().parents[1]/'tools/ci/run_refinement_instrumentation.sh').read_text()
        self.assertIn('-e listener app.space.notes.HangDiagnostics',wrapper)
        self.assertIn('run-as app.space.notes.debug cat',wrapper)

    def test_stack_artifacts_reach_existing_evidence_collector(self):
        root=Path(__file__).resolve().parents[2]
        workflow=(root/'.github/workflows/notes-refinement-native.yml').read_text()
        self.assertIn('            refinement-native*.txt',workflow)
        self.assertIn("artifact.glob('refinement-native*.txt')",workflow)

    def test_new_note_does_not_trust_previous_editor_ime(self):
        root=Path(__file__).resolve().parents[1]
        source=(root/'runtime/src/main/java/app/space/notes/runtime/NotesController.java').read_text()
        method=source.split('private void newNote() {',1)[1].split('private void newDocument()',1)[0]
        self.assertIn('boolean requestedForTarget',method)
        self.assertIn('if(requestedForTarget && keyboardVisible())return;',method)
        self.assertLess(method.index('.showSoftInput(target,'),method.index('requestedForTarget=true;'))
        self.assertNotIn('|| keyboardVisible() ||',method)
        journey=(root/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text().split('public void notesAndDocCreationRemainSeparateEvenAfterAnOldOptIn()',1)[1].split('@Test',1)[0]
        self.assertIn('cycle<3',journey)
        self.assertIn('documentImeReady()',journey)
        self.assertIn('editorImeReady()',journey)

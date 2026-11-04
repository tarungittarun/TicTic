import contextlib
import io
import tempfile
import unittest
from pathlib import Path
from mynotes.tools.ci.instrumentation_watchdog import Progress


class NativeProgressPrivacy(unittest.TestCase):
    def test_progress_emits_identity_not_private_text_or_failure_stack(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'output.txt'
            path.write_text('INSTRUMENTATION_STATUS: class=app.space.notes.Test\nINSTRUMENTATION_STATUS: test=example\nINSTRUMENTATION_STATUS: stream=SECRET TRANSCRIPT\nINSTRUMENTATION_STATUS: stack=SECRET STACK\nINSTRUMENTATION_STATUS_CODE: -2\n')
            text=io.StringIO()
            with contextlib.redirect_stdout(text):Progress(path).emit()
            self.assertEqual('Native progress: app.space.notes.Test#example status=-2\n',text.getvalue())

    def test_partial_lines_are_retained_without_repeating_outcomes(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'output.txt';path.write_text('INSTRUMENTATION_STATUS: class=app.space.notes.Test\nINSTRUMENTATION_STATUS: test=exa')
            progress=Progress(path);text=io.StringIO()
            with contextlib.redirect_stdout(text):
                progress.emit()
                with path.open('a') as out:out.write('mple\nINSTRUMENTATION_STATUS_CODE: 0\n')
                progress.emit();progress.emit()
            self.assertEqual(1,text.getvalue().count('status=0'))

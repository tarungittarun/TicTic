"""Source fence only; actual IME behavior still requires unchanged native assertions."""
from pathlib import Path
import unittest


class NewNoteImeRequestTest(unittest.TestCase):
    def test_only_accepted_request_and_current_served_target_can_finish_startup(self):
        root=Path(__file__).resolve().parents[1]
        source=(root/'runtime/src/main/java/app/space/notes/runtime/NotesController.java').read_text()
        section=source.split('private void newNote()',1)[1].split('private void newDocument()',1)[0]
        self.assertIn('requestedForTarget=keyboard.showSoftInput(target,',section)
        self.assertIn('requestedForTarget && keyboardVisible() && keyboard.isActive(target)',section)
        self.assertNotIn('requestedForTarget=true',section)
        self.assertIn('body!=target',section)
        self.assertIn('attempts++>=10',section)
        self.assertIn('target.postDelayed(this,100)',section)

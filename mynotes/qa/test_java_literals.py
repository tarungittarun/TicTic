from pathlib import Path
import unittest
from mynotes.tools.java_source_guard import check_literals
ROOT=Path(__file__).resolve().parents[1]

class JavaLiteralGuard(unittest.TestCase):
    def test_all_notes_java_literals_are_well_formed(self):
        for directory in ['app','runtime','domain']:
            for path in (ROOT/directory).rglob('*.java'):
                with self.subTest(path=str(path)):check_literals(path.read_text())
    def test_raw_newline_in_string_rejected(self):
        with self.assertRaises(ValueError):check_literals('String value="one\ntwo";')
    def test_raw_newline_in_character_rejected(self):
        with self.assertRaises(ValueError):check_literals("char c='\n';")
    def test_comment_quotes_do_not_count(self):
        check_literals('/* "\n\' */ // "\nclass Sample {}')
    def test_escapes_are_accepted(self):
        check_literals(r'''String value="a\n\"quoted\""; char c='\n';''')

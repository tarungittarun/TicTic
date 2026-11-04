import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]


class RegionDiscoverabilityTest(unittest.TestCase):
    def test_reread_is_first_tool_not_hidden_after_four_text_buttons(self):
        source=(ROOT/'mynotes/runtime/src/main/java/app/space/notes/runtime/ImageTextSelectionDialog.java').read_text()
        self.assertIn('tools.addView(reread[0],0)',source)
        self.assertIn('Drag a box, then tap Read this area.',source)
        self.assertIn('Selected text preview',source)

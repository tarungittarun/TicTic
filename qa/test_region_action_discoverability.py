import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]


class RegionDiscoverabilityTest(unittest.TestCase):
    def test_reread_is_full_width_primary_action_not_hidden_in_scrolling_tools(self):
        source=(ROOT/'mynotes/runtime/src/main/java/app/space/notes/runtime/ImageTextSelectionDialog.java').read_text()
        self.assertIn('sidePanel.addView(reread[0],1,new LinearLayout.LayoutParams(-1,d.dp(48)))',source)
        self.assertIn('Drag a box, then tap Read this area.',source)
        self.assertIn('Selected text preview',source)

    def test_secondary_tools_are_compact_and_keep_accessible_names(self):
        source=(ROOT/'mynotes/runtime/src/main/java/app/space/notes/runtime/ImageTextSelectionDialog.java').read_text()
        for label,icon in [('Select all','check'),('Clear','close'),('Choose words','text'),('Reset zoom','fit')]:
            self.assertIn('d.icon("'+label+'","'+icon+'"',source)
        self.assertIn('preview.setMinHeight(d.dp(48))',source)

    def test_primary_actions_are_pinned_outside_secondary_scrolling_controls(self):
        source=(ROOT/'mynotes/runtime/src/main/java/app/space/notes/runtime/ImageTextSelectionDialog.java').read_text()
        self.assertIn('sidePanel.addView(actionContainer,0',source)
        self.assertIn('sidePanel.addView(reread[0],1',source)
        self.assertIn('side.setLayoutParams(wide?new LinearLayout.LayoutParams(-1,0,1)',source)

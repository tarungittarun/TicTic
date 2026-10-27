"""Structural guardrails. Runtime behaviour is covered separately in Android journeys."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'

class CoreNotesContracts(unittest.TestCase):
    def test_schema_writes_v3_and_reads_old_v1_v2(self):
        s=(JAVA/'DocumentCodec.java').read_text()
        self.assertIn('put("version",3)',s)
        self.assertIn('version!=1 && version!=2',s)
        for field in ['items','marks','labels','hideChecked']:
            self.assertIn('"'+field+'"',s)

    def test_board_is_virtualised_masonry(self):
        s=(JAVA/'NotesBoard.java').read_text()
        self.assertIn('extends RecyclerView',s)
        self.assertIn('new StaggeredGridLayoutManager(2',s)
        self.assertIn('setFullSpan',s)
        self.assertIn('setMaxLines(7)',s)

    def test_editor_uses_native_spans_and_caret(self):
        s=(JAVA/'TextStyles.java').read_text()
        for token in ['StyleSpan','UnderlineSpan','StrikethroughSpan','getSpanStart','SPAN_EXCLUSIVE_EXCLUSIVE']:
            self.assertIn(token,s)
        c=(JAVA/'NotesController.java').read_text()
        self.assertIn('body.getSelectionStart()',c)
        self.assertIn('body.setSelection(start,end)',c)

    def test_rotation_snapshot_is_bounded(self):
        self.assertIn('draft.length<=32768',(JAVA/'NotesController.java').read_text())

    def test_all_twelve_colours_and_no_extra_app_imports(self):
        s=(JAVA/'NotesBoard.java').read_text()
        for name in ['Default','Red','Orange','Yellow','Green','Teal','Blue','Dark blue','Purple','Pink','Brown','Gray']:
            self.assertIn('"'+name+'"',s)
        self.assertNotIn('app.space.feature.',s)

"""Read-only performance and async navigation boundaries; native validation is separate."""
from pathlib import Path
import unittest
JAVA=Path(__file__).resolve().parents[1]/'runtime/src/main/java/app/space/notes/runtime'
class FeedbackRuntimeSafety(unittest.TestCase):
 def test_selection_avoids_whole_board_and_image_rebuild(self):
  text=(JAVA/'NotesBoard.java').read_text();self.assertIn('if(rows.equals(previous))',text);self.assertIn('adapter.notifyItemChanged(i,"selection")',text);self.assertIn('if(h.previewImage!=image)',text)
 def test_late_export_and_storage_transactions_cannot_reopen_old_settings(self):
  text=(JAVA/'NotesController.java').read_text()
  for start,end in [('public void exportBackup(', 'public void exportFolderCopy('),('private void moveStorage(', 'private void chooseAppFolderCategories(')]:
   part=text.split(start,1)[1].split(end,1)[0];self.assertIn('callerGeneration=fileViewGeneration',part);self.assertIn('callerGeneration==fileViewGeneration',part);self.assertIn('pageBack=callerBack',part)
 def test_selection_is_geometric_and_limits_are_explicit(self):
  text=(JAVA/'LocalPdfPreview.java').read_text();self.assertIn('PdfTextLayer selection(int page)',text);self.assertIn('source.getRotation()%360!=0',text);self.assertIn('p.getDir()!=0',text);self.assertIn('glyphs.size()>=20000',text);self.assertIn('token!=current.token',text)
  text=(JAVA/'PdfSelectionImage.java').read_text();self.assertIn('imageToView().mapRect',text);self.assertIn('ActionMode.TYPE_FLOATING',text);self.assertIn('setPrimaryClip',text);self.assertIn('handle==0',text)

"""Design and source-safety guards supplement (never replace) attached Android viewer tests."""
from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]
JAVA=R/'mynotes/runtime/src/main/java/app/space/notes/runtime'
class Candidate68ViewerContracts(unittest.TestCase):
 def test_one_filename_toolbar_not_a_repeated_page_position_band(self):
  s=(JAVA/'LocalPdfPreview.java').read_text()
  for text in ['filename.setSingleLine(true)','"File name: "+name','"File options"','pageRail.setVisibility(INVISIBLE)']:self.assertIn(text,s)
  self.assertNotIn('"Go to page"',s)
  self.assertNotIn('controls.addView(position',s)
 def test_file_zoom_changes_page_geometry_not_shared_image_crop(self):
  s=(JAVA/'LocalPdfPreview.java').read_text()
  for text in ['viewport.getWidth()*(fileZoom?fileScale:1)','pages.getLayoutParams().width=width','holder.image.getLayoutParams().height','fileScaleChanged=fileZoom?']:self.assertIn(text,s)
  self.assertIn('Math.sqrt(2000000.0/',s)
 def test_tap_release_is_distinct_from_scrolling_and_handle_drag(self):
  self.assertIn('onPageTap(){clearSelection();}',(JAVA/'PdfSelectionImage.java').read_text())
  self.assertIn('onPageTap();performClick();',(JAVA/'ZoomablePageImage.java').read_text())
  self.assertIn('if(holder.image!=touched)',(JAVA/'LocalPdfPreview.java').read_text())
 def test_search_has_ime_action_progress_first_result_and_highlight(self):
  s=(JAVA/'LocalPdfPreview.java').read_text()
  for text in ['IME_ACTION_SEARCH','searchProgress.setVisibility(VISIBLE)','first && !matches.isEmpty()','textWorker.execute(()->searchPage(page+1','highlightSearch(layer,needle,selected)','design.dp(24)-Math.round(image.searchTop()']:self.assertIn(text,s)
 def test_page_open_is_lazy_and_recognition_has_actual_spinner(self):
  s=(JAVA/'LocalPdfPreview.java').read_text()
  self.assertIn('pdf.openPage(0)',s)
  self.assertNotIn('for(int i=0;i<ratios.length;i++)',s)
  self.assertIn('progressBody.addView(new ProgressBar',s)
  self.assertIn('setTitle((recognized?"Recognized text":"Page text")',s)
 def test_drafts_are_bounded_atomic_and_export_keeps_source_separate(self):
  s=(JAVA/'PdfMarkup.java').read_text()
  for text in ['MAX_STROKES=1000,MAX_POINTS=30000','pdf-annotation-drafts','atomic.finishWrite','PDPageContentStream.AppendMode.APPEND','File.createTempFile("marked-pdf-"','pdf.save(destination)']:self.assertIn(text,s)
  self.assertNotIn('pdf.save(original)',s)
 def test_marked_export_is_empty_destination_only_and_hash_verified(self):
  s=(JAVA/'PdfMarkedCopy.java').read_text()
  for text in ['b.st_size!=0','a.st_dev==b.st_dev && a.st_ino==b.st_ino','Os.fsync','MessageDigest.isEqual','source.getParentFile().equals(context.getCacheDir().getCanonicalFile())']:self.assertIn(text,s)
  activity=(R/'mynotes/app/src/main/java/app/space/notes/NotesActivity.java').read_text()
  for text in ['request>107','if(request==107)','state.putString("markedPdfSource"','PdfMarkedCopy.write']:self.assertIn(text,activity)
 def test_new_native_cases_are_registered_without_dropping_previous_inventory(self):
  from mynotes.tools.build_e2e_report import methods
  from mynotes.tools.ci.run_ordered_modules import plan
  native=methods();self.assertEqual(444,len(native));self.assertEqual(set(native),{n for names in plan().values() for n in names})
  self.assertEqual(7,sum(n.startswith('app.space.notes.runtime.PdfViewerInteractionTest#') for n in native))

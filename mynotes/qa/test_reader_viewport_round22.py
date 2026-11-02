"""Source/ownership safety gates, not Android or physical acceptance evidence."""
from pathlib import Path
import unittest
from mynotes.tools.ci.test_plan import build_plan
from mynotes.tools.ci.refinement_plan import selected_cases

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'runtime/src/main/java/app/space/notes/runtime'


class ReaderViewportRound22Test(unittest.TestCase):
    def test_zoom_snapshot_is_finite_copied_and_does_not_allocate_rasters(self):
        source=(JAVA/'ZoomablePageImage.java').read_text()
        for value in ['pendingZoom.clone()','state.clone()','state.length!=5','Float.isFinite(value)','onSizeChanged']:
            self.assertIn(value,source)
        self.assertNotIn('createBitmap',source)
        self.assertNotIn('writeTo',source)

    def test_reader_state_uses_five_float_zoom_and_normalized_page_offset(self):
        source=(JAVA/'LocalPdfPreview.java').read_text()
        for value in ['state.putFloatArray("zoom",zoom)','state.putFloat("pageTop",savedPageTop(page))','zoom.length==5?zoom.clone():null','scrollToPageViewport(page,top)','Math.round(top*height)']:
            self.assertIn(value,source)
        snapshot=source.split('android.os.Bundle savedViewport()',1)[1].split('void restoreViewport',1)[0]
        self.assertNotIn('putParcelable',snapshot)
        self.assertNotIn('putByteArray',snapshot)
        self.assertIn('if(pages.getWidth()<1)',source)
        self.assertIn('pages.addOnLayoutChangeListener',source)
        self.assertIn('view.removeOnLayoutChangeListener(this)',source)

    def test_new_native_cases_remain_in_each_focused_profile_inventory(self):
        plan=build_plan()
        selected=set(selected_cases(plan,'focused',0)+selected_cases(plan,'focused',1))
        for method in ['pageZoomRestoresBeforeLayoutAndRetainsNormalizedPanAcrossResize','pageZoomRejectsNonFiniteMalformedStateAndClampsFiniteExtremes']:
            self.assertIn('app.space.notes.runtime.ViewedFileJourneysTest#'+method,selected)

    def test_bootstrap_retry_does_not_retry_instrumentation_or_drop_profiles(self):
        workflow=(ROOT.parent/'.github/workflows/notes-refinement-native.yml').read_text()
        bootstrap=workflow.split('- name: Bootstrap emulator package',1)[1].split('- uses: ReactiveCircus/',1)[0]
        self.assertIn('for attempt in 1 2 3',bootstrap)
        self.assertIn('timeout 180',bootstrap)
        self.assertNotIn('run_refinement_instrumentation',bootstrap)
        self.assertIn('fail-fast: false',workflow)
        self.assertIn('Require every selected method once',workflow)

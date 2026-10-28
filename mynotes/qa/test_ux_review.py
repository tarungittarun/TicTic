import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from mynotes.tools.ci.ux_review import approved
from mynotes.tools.ci.ui import coordinates,known_launcher_anr

class RenderedUxPublicationContracts(unittest.TestCase):
    def review(self,**changes):
        value={'reviewed_source':'a'*40,'review_run':'12345','inspected_variants':['full-api34','night-api34','large-font-api34','landscape-api34','tablet-api34'],'findings':'Actual screenshots inspected, no heading overlap.'};value.update(changes);return value
    def check(self,value):
        with tempfile.TemporaryDirectory() as d:
            path=Path(d,'approval.json');path.write_text(json.dumps(value));return approved('b'*40,path)
    @patch('mynotes.tools.ci.ux_review.assert_source_equivalent')
    def test_pending_review_cannot_publish(self,equivalent):
        self.assertFalse(self.check(self.review(reviewed_source=None)));equivalent.assert_not_called()
    @patch('mynotes.tools.ci.ux_review.assert_source_equivalent')
    def test_equivalent_app_inputs_and_all_variants_required(self,equivalent):
        self.assertTrue(self.check(self.review()));equivalent.assert_called_once_with('a'*40,'b'*40)
        self.assertFalse(self.check(self.review(inspected_variants=['full-api34'])))
    @patch('mynotes.tools.ci.ux_review.assert_source_equivalent',side_effect=ValueError('App changed'))
    def test_app_change_invalidates_old_visual_approval(self,equivalent):
        self.assertFalse(self.check(self.review()))
    def test_old_and_new_release_creation_names_have_bounded_coordinates(self):
        for attribute in ['text="+ Text"','content-desc="New text note"']:
            self.assertEqual((25,30),coordinates(f'<hierarchy><node {attribute} enabled="true" bounds="[10,20][40,40]"/></hierarchy>','+ Text'))
    def test_launcher_anr_guard_is_exact_and_never_waives_notes(self):
        xml="""<hierarchy><node package="android" resource-id="android:id/alertTitle" text="Quickstep isn't responding"/><node package="android" resource-id="android:id/aerr_close" text="Close app" enabled="true" clickable="true" bounds="[10,20][50,60]"/></hierarchy>"""
        self.assertEqual((30,40),known_launcher_anr(xml))
        for changed in [xml.replace('Quickstep', 'MyNotes'),xml.replace('package="android"','package="app.space.notes"'),xml.replace('aerr_close','other'),xml.replace('clickable="true"','clickable="false"'),xml.replace('[10,20][50,60]','[10,20][10,20]')]:
            with self.subTest(xml=changed):
                with self.assertRaises(ValueError):known_launcher_anr(changed)
    def test_mixed_filter_tab_row_uses_visual_not_text_baselines(self):
        source=(Path(__file__).resolve().parents[1]/'runtime/src/main/java/app/space/notes/runtime/LibraryFilterView.java').read_text()
        self.assertIn('row.setBaselineAligned(false)',source)
        self.assertIn('row.setGravity(Gravity.CENTER_VERTICAL)',source)
    def test_every_synthetic_scene_is_collected_and_persisted(self):
        import re
        from mynotes.tools.ci.persist_ux import SCENES
        root=Path(__file__).resolve().parents[1]
        script=(root/'tools/ci/android_lane.sh').read_text()
        collected=set(re.search(r'for scene in ([^;]+); do',script).group(1).split())
        captured=set(re.findall(r'screenshot\("([a-z0-9-]+)"',(root/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()))
        self.assertEqual(SCENES,collected)
        self.assertTrue(captured <= collected,captured-collected)
        self.assertTrue({'voice-inbox','drawing','filters-labels','backgrounds'} <= captured)
    def test_renderer_contains_real_raster_and_input_dispatch_checks(self):
        source=(Path(__file__).resolve().parents[1]/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
        for evidence in ['instrumentation.sendPointerSync','layout.draw(canvas)','pixels.getPixel','getLineTop(line)','getLineBottom(line)','takeScreenshot()','format-keyboard','HeadingTypingExtendsAndEnterReturnsToNormalText']:
            self.assertIn(evidence,source)


class SignaturePaletteReadability(unittest.TestCase):
    """A numeric token check complements, but cannot replace, rendered/native review."""
    def contrast(self, foreground, background):
        def luminance(colour):
            values=[int(colour[i:i+2],16)/255 for i in (1,3,5)]
            linear=[v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in values]
            return sum(v*w for v,w in zip(linear,(.2126,.7152,.0722)))
        a,b=sorted((luminance(foreground),luminance(background)))
        return (b+.05)/(a+.05)
    def test_signature_text_and_hints_meet_normal_text_contrast(self):
        import re
        source=(Path(__file__).resolve().parents[1]/'runtime/src/main/java/app/space/notes/runtime/NotesDesign.java').read_text()
        pairs=dict((name,(night,light)) for name,night,light in re.findall(r'(background|surface|ink|muted|accent)=Color.parseColor\(dark\?"(#[A-Fa-f0-9]{6})":"(#[A-Fa-f0-9]{6})"\)',source))
        self.assertEqual(set(pairs),{'background','surface','ink','muted','accent'})
        for mode in (0,1):
            for foreground in ('ink','muted','accent'):
                for background in ('background','surface'):
                    with self.subTest(mode=mode,foreground=foreground,background=background):
                        self.assertGreaterEqual(self.contrast(pairs[foreground][mode],pairs[background][mode]),4.5)
        self.assertGreaterEqual(self.contrast('#241A0B','#E8C58E'),4.5)
    def test_note_card_text_remains_readable_on_every_note_colour(self):
        import re
        source=(Path(__file__).resolve().parents[1]/'runtime/src/main/java/app/space/notes/runtime/NotesBoard.java').read_text()
        for mode,ink in [('DARK','#F5F5F5'),('LIGHT','#202124')]:
            colours=re.findall(r'#[A-Fa-f0-9]{6}',re.search(r'String\[\] '+mode+r'=\{([^}]+)\}',source).group(1))
            self.assertEqual(len(colours),12)
            for colour in colours:
                with self.subTest(mode=mode,colour=colour):self.assertGreaterEqual(self.contrast(ink,colour),4.5)

import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from mynotes.tools.ci.ux_review import approved
from mynotes.tools.ci.ui import coordinates

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
    def test_renderer_contains_real_raster_and_input_dispatch_checks(self):
        source=(Path(__file__).resolve().parents[1]/'app/src/androidTest/java/app/space/notes/CoreJourneysTest.java').read_text()
        for evidence in ['instrumentation.sendPointerSync','layout.draw(canvas)','pixels.getPixel','getLineTop(line)','getLineBottom(line)','takeScreenshot()','format-keyboard','HeadingTypingExtendsAndEnterReturnsToNormalText']:
            self.assertIn(evidence,source)

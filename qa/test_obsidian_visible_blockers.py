"""Conservative labels for captured error states; no fabricated device tests."""
import importlib.util
from pathlib import Path
import unittest
p=Path(__file__).resolve().parents[1]/'mynotes/tools/comparisons/obsidian_coverage.py';spec=importlib.util.spec_from_file_location('visible_blockers',p);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class VisibleBlockersTest(unittest.TestCase):
 def row(self,feature,text,observed=''):return {'feature':feature,'visible_text':text,'observed':observed,'status':'screen_inspected','screenshot':'actual.png'}
 def test_template_error_not_feature_success(self):
  a=self.row('Templates','Failed to list templates. No template folder configured.');b=m.normalize_visible_blockers([a])[0];self.assertEqual(b['status'],'blocked');self.assertEqual(a['status'],'screen_inspected');self.assertEqual(b['screenshot'],'actual.png')
 def test_permission_denial_not_recording_pass(self):
  a=self.row('Command entry: Start recording','Permission denied','Command=audio-recorder:start; invoked=True');self.assertEqual(m.normalize_visible_blockers([a])[0]['status'],'blocked')
 def test_obscured_properties_target_is_explicit(self):
  a=self.row('Properties view','Playground\n2 tabs\nDone');self.assertEqual(m.normalize_visible_blockers([a])[0]['status'],'blocked')
 def test_expected_overview_not_falsely_blocked(self):
  a=self.row('Tab overview','2 tabs\nDone');self.assertEqual(m.normalize_visible_blockers([a])[0]['status'],'screen_inspected')

 def test_paired_permission_denial_blocks_start_in_same_native_segment(self):
  start=self.row('Command entry: Start recording','Allow recording?','Command=audio-recorder:start; invoked=True');stop=self.row('Command entry: Stop recording','Permission denied','Command=audio-recorder:stop; unavailable');start['capture_run']=stop['capture_run']='same'
  self.assertEqual(m.normalize_visible_blockers([start,stop])[0]['status'],'blocked')
 def test_permission_denial_does_not_leak_between_native_segments(self):
  start=self.row('Command entry: Start recording','Recording','Command=audio-recorder:start; invoked=True');stop=self.row('Command entry: Stop recording','Permission denied','Command=audio-recorder:stop; unavailable');start['capture_run']='one';stop['capture_run']='two'
  self.assertEqual(m.normalize_visible_blockers([start,stop])[0]['status'],'screen_inspected')

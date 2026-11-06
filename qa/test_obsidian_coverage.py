import unittest
from mynotes.tools.comparisons.obsidian_coverage import command_coverage
class ObsidianCoverageTruth(unittest.TestCase):
 def test_inventory_is_not_a_pass(self):
  self.assertEqual('inventoried_only',command_coverage([{'id':'a','name':'A'}],[])[0]['status'])
 def test_screenshot_is_not_functional_acceptance(self):
  x=command_coverage([{'id':'graph:open','name':'Graph'}],[{'feature':'Graph','observed':'graph:open; invoked=True','status':'screen_inspected'}])
  self.assertEqual('invoked_ui_inspected',x[0]['status'])
 def test_exact_id_not_prefix(self):
  x=command_coverage([{'id':'editor:toggle-highlight','name':'Default'},{'id':'editor:toggle-highlight-red','name':'Red'}],[{'feature':'Red','observed':'Command=editor:toggle-highlight-red; resulting Markdown=red','status':'functional_pass'}])
  self.assertEqual(['inventoried_only','functional_readback_pass'],[r['status'] for r in x])
 def test_blocked_remains_unverified(self):
  x=command_coverage([{'id':'sync:setup','name':'Sync'}],[{'feature':'Sync','observed':'sync:setup; invoked=True','status':'blocked'}])
  self.assertEqual('blocked_or_unverified',x[0]['status'])
 def test_feature_brief_does_not_certify_color(self):
  from mynotes.tools.comparisons.obsidian_coverage import command_brief
  self.assertIn('does not by itself certify',command_brief('editor:toggle-highlight-red','Red highlight'))
 def test_account_service_limit_stays_in_description(self):
  from mynotes.tools.comparisons.obsidian_coverage import command_brief
  self.assertIn('not end-to-end verified',command_brief('sync:setup','Set up Sync'))
 def test_portrait_image_cannot_be_counted_as_landscape(self):
  from mynotes.tools.comparisons.obsidian_coverage import normalize_requested_variants
  rows=[{'feature':'Appearance variant: light landscape','status':'blocked','observed':'landscape=False'}, {'feature':'Canvas variant: light landscape','status':'screen_inspected','screenshot':'real.jpg'}]
  actual=normalize_requested_variants(rows)
  self.assertEqual('blocked',actual[1]['status']);self.assertEqual('real.jpg',actual[1]['screenshot']);self.assertIn('not achieved',actual[1]['feature'])
  self.assertEqual('screen_inspected',rows[1]['status'])
 def test_achieved_portrait_variant_is_not_reclassified(self):
  from mynotes.tools.comparisons.obsidian_coverage import normalize_requested_variants
  x={'feature':'Canvas variant: light portrait','status':'screen_inspected','screenshot':'real.jpg'}
  self.assertEqual([x],normalize_requested_variants([x]))

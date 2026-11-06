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

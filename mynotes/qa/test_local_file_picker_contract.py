"""Inventory wiring, not a replacement for real platform/SAF qualification."""
import unittest
from mynotes.tools.ci.test_plan import build_plan
from mynotes.tools.ci.refinement_plan import selected_cases


class LocalFilePickerContractTest(unittest.TestCase):
    def test_actual_picker_contract_is_in_every_full_and_focused_inventory(self):
        plan=build_plan()
        case='app.space.notes.LocalFilePickerTest#localPickerAcceptsThePlatformsCsvMimeAndRetainsExistingFormats'
        for group in ('all','focused'):
            whole=selected_cases(plan,group)
            self.assertIn(case,whole)
            shards=[selected_cases(plan,group,index) for index in (0,1)]
            self.assertEqual(1,sum(case in shard for shard in shards))

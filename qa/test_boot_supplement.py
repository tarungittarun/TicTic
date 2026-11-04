import copy
import unittest
from mynotes.tools.ci.build_boot_supplement_report import validate_receipt


def receipt():
    return dict(original_conclusion='failure',source='a'*40,original_source='a'*40,missing_lanes=['light-portrait-ocr'],lane_capture_runs={'light-portrait-ocr':999},original_artifact_names=['module-benchmark-dark-portrait-ocr'],original_failed_jobs=[dict(lane='light-portrait-ocr',conclusion='failure',boot_timeout_verified=True,tests_executed=0)])


class BootSupplement(unittest.TestCase):
    def test_only_zero_test_verified_missing_boot_lane_accepted(self):
        self.assertEqual({'light-portrait-ocr'},validate_receipt(receipt()))

    def test_never_overwrite_original_measurements(self):
        r=receipt();r['original_artifact_names'].append('module-benchmark-light-portrait-ocr')
        with self.assertRaises(ValueError):validate_receipt(r)

    def test_assertion_failure_cannot_be_relabelled_boot(self):
        for key,value in [('tests_executed',1),('boot_timeout_verified',False),('boot_timeout_verified','false')]:
            r=receipt();r['original_failed_jobs'][0][key]=value
            with self.assertRaises(ValueError):validate_receipt(r)

    def test_other_failure_must_block_composite(self):
        r=receipt();r['original_failed_jobs'].append(dict(lane='dark-portrait-ocr',conclusion='failure',boot_timeout_verified=True,tests_executed=0))
        with self.assertRaises(ValueError):validate_receipt(r)

    def test_source_or_original_outcome_cannot_change(self):
        for key,value in [('original_source','b'*40),('original_conclusion','success')]:
            r=receipt();r[key]=value
            with self.assertRaises(ValueError):validate_receipt(r)

    def test_duplicate_unknown_and_nonocr_lane_rejected(self):
        for lanes in ([],['light-portrait-ocr']*2,['unknown-ocr'],['light-portrait-voice0']):
            r=receipt();r['missing_lanes']=lanes
            with self.assertRaises(ValueError):validate_receipt(r)

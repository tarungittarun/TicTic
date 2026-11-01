"""A passing discovery inventory must not silently shrink old regression coverage."""
import json
import unittest
from pathlib import Path
from mynotes.tools.ci.test_plan import discover

ROOT=Path(__file__).resolve().parents[1]

class NativeBaselinePreservation(unittest.TestCase):
    def test_every_qualified_027_regression_remains_authored(self):
        receipt=json.loads((ROOT/'tools/ci/native_baseline_027.json').read_text())
        current=discover(ROOT/'app/src/androidTest/java')
        total=0
        for name,methods in receipt['methods'].items():
            path=ROOT/name
            self.assertTrue(path.is_file(),name)
            source=path.read_text()
            import re
            package=re.search(r'package\s+([\w.]+)',source).group(1)
            key=package+'.'+path.stem
            self.assertFalse(set(methods)-set(current.get(key,[])),name+': '+str(set(methods)-set(current.get(key,[]))))
            total+=len(methods)
        self.assertGreaterEqual(total,327)

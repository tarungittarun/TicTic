"""Catalogue-composition unit tests, not native feature test results."""
import importlib.util
from pathlib import Path
import unittest
p=Path(__file__).resolve().parents[1]/'mynotes/tools/comparisons/obsidian_command_segments.py'
spec=importlib.util.spec_from_file_location('command_segments',p);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class CommandSegmentsTest(unittest.TestCase):
 def setUp(self):
  self.inv={'commands':[{'id':'a'},{'id':'b'}]};self.identity={'apk_sha256':m.PIN};self.rows=[{'observed':'Command=a; invoked=True'},{'observed':'Command=b; unavailable'}]
 def check(self,rows=None,identity=None,complete=True):return m.validate_segments([self.inv,self.inv],[self.identity,identity or self.identity],self.rows if rows is None else rows,complete)
 def test_complete_exact_census(self):self.assertEqual(self.check(),2)
 def test_missing_entry_rejected(self):
  with self.assertRaises(ValueError):self.check(self.rows[:1])
 def test_duplicate_entry_rejected(self):
  with self.assertRaises(ValueError):self.check(self.rows+self.rows[:1])
 def test_wrong_apk_rejected(self):
  with self.assertRaises(ValueError):self.check(identity={'apk_sha256':'different'})
 def test_unfinished_continuation_rejected(self):
  with self.assertRaises(ValueError):self.check(complete=False)

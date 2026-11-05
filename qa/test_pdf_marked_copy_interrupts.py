"""Supplement native SAF/rotation checks; these source guards are not Android execution."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
class MarkedCopyInterruptionContracts(unittest.TestCase):
 def test_cancellation_is_checked_before_open_and_during_copy_and_readback(self):
  source=(ROOT/'mynotes/runtime/src/main/java/app/space/notes/runtime/PdfMarkedCopy.java').read_text()
  self.assertIn('Thread.currentThread().isInterrupted()',source)
  self.assertNotIn('Thread.interrupted()',source)
  self.assertLess(source.index('checkInterrupted();File source='),source.index('openFileDescriptor(destination'))
  self.assertIn('for(int n;(n=input.read(buffer))!=-1;){checkInterrupted();',source)
  self.assertIn('for(int n;(n=check.read(buffer))!=-1;){checkInterrupted();',source)
  self.assertIn('throw new InterruptedIOException',source)
 def test_native_cases_check_rendered_rotations_crop_and_real_empty_saf_destination(self):
  source=(ROOT/'mynotes/app/src/androidTest/java/app/space/notes/runtime/PdfViewerInteractionTest.java').read_text()
  for text in ['for(int rotation:new int[]{0,90,180,270})','for(boolean cropped:new boolean[]{false,true})','red ink at normalized visual coordinate','translucent yellow at normalized visual coordinate','original bytes retained','rotatedAndCroppedMarkedCopiesKeepVisibleCoordinates();']:self.assertIn(text,source)
  source=(ROOT/'mynotes/app/src/androidTest/java/app/space/notes/RealFolderStorageTest.java').read_text()
  for text in ['interruptedFailure.get() instanceof java.io.InterruptedIOException','interruptRetained.get()','Pre-cancelled copy leaves empty destination untouched']:self.assertIn(text,source)

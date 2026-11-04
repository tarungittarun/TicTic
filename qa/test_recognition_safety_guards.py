import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]


class RecognitionSafetyGuards(unittest.TestCase):
    def test_adaptive_threshold_is_empty_result_only(self):
        source=(ROOT/'mynotes/runtime/src/main/java/app/space/notes/runtime/OfflineOcr.java').read_text()
        self.assertIn('if(text.trim().isEmpty() && engine.setVariable("thresholding_method","2"))',source)
        self.assertIn('OcrAlternatives.prefer(text,confidence,adaptive,score)',source)

    def test_crop_cancellation_retains_source_and_suppresses_late_result(self):
        source=(ROOT/'mynotes/runtime/src/main/java/app/space/notes/runtime/ImageRegionText.java').read_text()
        for guard in ('new ArrayBlockingQueue<>(1)','if(!scan.retain())','finally{scan.release();}','if(!cancel.cancelled())listener.complete'):
            self.assertIn(guard,source)
        dialog=(ROOT/'mynotes/runtime/src/main/java/app/space/notes/runtime/ImageTextSelectionDialog.java').read_text()
        self.assertIn('reading[0].cancel()',dialog)

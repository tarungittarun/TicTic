"""Local safety/experiment gates; native behavior is qualified separately in Actions."""
import unittest
from pathlib import Path
from mynotes.tools.ci import indian_speech_pilot as pilot
ROOT=Path(__file__).resolve().parents[1]
class ImageSelectionAndPilot(unittest.TestCase):
 def test_training_and_evaluation_speakers_and_prompts_are_disjoint(self):
  split=pilot.splits();self.assertEqual(32,len(split['train']));self.assertEqual(16,len(split['evaluation']))
  self.assertFalse({s for s,_ in split['train']}&{s for s,_ in split['evaluation']})
  self.assertFalse({i for _,i in split['train']}&{i for _,i in split['evaluation']})
 def test_metric_counts_substitutions_deletions_and_insertions(self):
  self.assertEqual(1,pilot.distance(pilot.normalize('One, two.'),pilot.normalize('one three')))
  self.assertEqual(2,pilot.distance(['one'],['one','two','three']))
  self.assertEqual(2,pilot.distance(['one','two'],[]))
 def test_data_hashes_are_pinned_and_complete(self):
  self.assertEqual({'ksp','slt'},set(pilot.ARCHIVES));self.assertTrue(all(len(value)==64 for value in pilot.ARCHIVES.values()))
 def test_no_extraction_of_untrusted_archive_paths(self):
  source=(ROOT/'tools/ci/indian_speech_pilot.py').read_text();self.assertNotIn('.extractall(',source);self.assertNotIn('shell=True',source)
  self.assertIn('Corpus archive checksum mismatch',source);self.assertIn('Corpus archive',source)
 def test_pilot_does_not_claim_model_adoption(self):
  source=(ROOT/'tools/ci/indian_speech_pilot.py').read_text();self.assertIn("'adopted':False",source);self.assertIn('modified_parameters',source);self.assertIn('loss.backward()',source);self.assertIn('optimizer.step()',source)
 def test_runtime_selection_is_bounded_and_source_owned(self):
  source=(ROOT/'runtime/src/main/java/app/space/notes/runtime/OfflineOcr.java').read_text();self.assertIn('hocr.length()>2000000',source);self.assertIn('words.size()==4096',source);self.assertIn('synchronized boolean retain()',source);self.assertIn('if(--uses==0)image.recycle()',source)
 def test_preview_queue_is_drained_when_controller_closes(self):
  source=(ROOT/'runtime/src/main/java/app/space/notes/runtime/NotesController.java').read_text();self.assertIn('pendingOcrScans.remove(scan)',source);self.assertIn('closeOcrPreview();for(OfflineOcr.Scan',source)
 def test_playback_boost_is_optional_and_does_not_rewrite_audio(self):
  source=(ROOT/'runtime/src/main/java/app/space/notes/runtime/VoicePlaybackCard.java').read_text();self.assertIn('setTargetGain(600)',source);self.assertIn('releaseBoost()',source);self.assertNotIn('FileOutputStream',source);self.assertNotIn('RECORD_AUDIO',source)

 def test_crop_does_not_render_views_from_persisted_worker_callback(self):
  source=(ROOT/'runtime/src/main/java/app/space/notes/runtime/NotesController.java').read_text();self.assertIn('else addImage(target,image);',source);self.assertNotIn('addImage(target,image,0,()->{ocrPanel=false;',source)

 def test_pilot_teacher_forcing_matches_no_timestamp_inference_prefix(self):
  source=(ROOT/'tools/ci/indian_speech_pilot.py').read_text();self.assertIn('tokenizer.sot_sequence_including_notimestamps',source);self.assertIn('targets[:,:len(prefix)-1]=-100',source);self.assertIn("name.startswith('decoder.blocks.3.')",source)

 def test_review_input_has_bounded_height_inside_a_scrollable_column(self):
  source=(ROOT/'runtime/src/main/java/app/space/notes/runtime/NotesController.java').read_text();self.assertIn('reviewScroll.setFillViewport(true)',source);self.assertIn('content.addView(input,new LinearLayout.LayoutParams(-1,dp(240)))',source)

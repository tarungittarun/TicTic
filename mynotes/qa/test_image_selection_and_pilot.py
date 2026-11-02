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

 def test_extra_voice_curation_never_treats_availability_as_permission(self):
  from mynotes.tools.ci.indian_cmu_candidates import permitted
  self.assertFalse(permitted('Publicly available speech. Copyright reserved.'))
  self.assertTrue(permitted('This voice is free for use for any purpose (commercial or otherwise). Modifications must be clearly marked; original authors retained.'))

 def test_extra_voice_archive_digests_are_pinned(self):
  from mynotes.tools.ci.indian_cmu_candidates import EXPECTED
  self.assertEqual({'aup','axb','gka','slp'},set(EXPECTED));self.assertTrue(all(len(value)==64 for value in EXPECTED.values()))

 def test_expanded_training_is_prompt_heldout_not_claimed_speaker_heldout(self):
  from mynotes.tools.ci.indian_acoustic_extension import splits
  split=splits();self.assertEqual(593,len(split['train']));self.assertEqual(64,len(split['evaluation']))
  self.assertEqual({'ksp'}, {speaker for speaker,_ in split['train']}&{speaker for speaker,_ in split['evaluation']})
  self.assertFalse({text for _,text in split['train']}&{text for _,text in split['evaluation']})

 def test_extended_trainable_scope_includes_acoustics_but_preserves_other_blocks(self):
  from mynotes.tools.ci.indian_acoustic_extension import selected_parameter
  self.assertTrue(selected_parameter('encoder.blocks.3.attn.query.weight'));self.assertTrue(selected_parameter('decoder.blocks.3.attn.query.weight'))
  for name in ['encoder.blocks.2.attn.query.weight','decoder.blocks.2.attn.query.weight','decoder.token_embedding.weight','encoder.ln_post.weight']:self.assertFalse(selected_parameter(name))

 def test_large_curation_reads_compressed_archive_forward_without_changing_split_membership(self):
  from types import SimpleNamespace
  requests={'train':[('ksp','one'),('ksp','two')],'evaluation':[('slt','three'),('ksp','four')]}
  members={'wav/'+name+'.wav':SimpleNamespace(offset_data=offset) for name,offset in [('one',30),('two',10),('four',20)]}
  old=pilot.archive_requests('ksp',requests,members);stream=pilot.archive_requests('ksp',requests,members,True)
  self.assertEqual([('train','one'),('train','two'),('evaluation','four')],old)
  self.assertEqual([('train','two'),('evaluation','four'),('train','one')],stream)
  self.assertEqual(set(old),set(stream))

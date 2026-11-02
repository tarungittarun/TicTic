"""Host self-checks, not training/inference or app accuracy evidence."""
import unittest
from mynotes.tools.ci import emoji_cpu_pilot as pilot
from pathlib import Path

class EmojiCpuPilotPolicy(unittest.TestCase):
    def test_git_blob_identity_matches_git_empty_blob(self):
        self.assertEqual('e69de29bb2d1d6434b8b29ae775ad8c2e48c5391',pilot.blob_sha(b''))

    def test_metadata_is_complete_and_zwj_skin_tone_remain_distinct(self):
        with self.assertRaises(ValueError):pilot.png_entries({'truncated':True,'tree':[]},'assets/72x72/')
        tree={'truncated':False,'tree':[{'path':'assets/72x72/1f469-1f3fd-200d-1f4bb.png','type':'blob','sha':'fixed'}]}
        self.assertIn((0x1f469,0x1f3fd,0x200d,0x1f4bb),pilot.png_entries(tree,'assets/72x72/'))

    def test_duplicate_normalized_asset_names_are_not_silently_chosen(self):
        tree={'truncated':False,'tree':[{'path':'assets/72x72/'+name+'.png','type':'blob','sha':'fixed'} for name in ['2764','2764-fe0f']]}
        with self.assertRaises(ValueError):pilot.png_entries(tree,'assets/72x72/')

    def test_experiment_cannot_be_misread_as_a_runtime_model(self):
        source=Path(pilot.__file__).read_text()
        for guard in ['torch.set_num_threads(2)', 'torch.use_deterministic_algorithms(True)', 'EXPERIMENTAL-NOT-RUNTIME', "'runtime_models_changed':False", "'release_approval':False", 'separate_art_style_64_only']:
            self.assertIn(guard,source)
        self.assertNotIn('.cuda()',source)
        self.assertNotIn('saveOcrText',source)


class TrainingVariantSafetyTest(unittest.TestCase):
    def test_fixed_configuration_and_bounded_epochs(self):
        from mynotes.tools.ci.emoji_cpu_pilot import training_config
        self.assertEqual({'stabilized':False,'learning_rate':.002},training_config('baseline',3))
        self.assertEqual({'stabilized':True,'learning_rate':.0005},training_config('centered-leaky-layernorm',20))
        for variant,epochs in [('unknown',3),('baseline',0),('baseline',31),('baseline',True)]:
            with self.assertRaises(ValueError):training_config(variant,epochs)

    def test_reused_art_style_sample_is_not_claimed_as_a_final_untouched_test(self):
        source=(Path(__file__).resolve().parents[1]/'tools/ci/emoji_cpu_pilot.py').read_text()
        self.assertIn('separate_art_style_64_reused_development_only',source)
        self.assertIn('NOT a final untouched test',source)
        self.assertIn("'release_approval':False",source)

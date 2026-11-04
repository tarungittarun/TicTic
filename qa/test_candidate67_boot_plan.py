"""Only original zero-test bootstrap failure can be supplemented; not device acceptance."""
import unittest
from pathlib import Path
import yaml
ROOT=Path(__file__).resolve().parents[1]
class Candidate67BootPlan(unittest.TestCase):
 def test_exact_original_apks_only_missing_profile_and_no_build_or_inference_repeat(self):
  path=ROOT/'.github/workflows/notes-candidate67-native-boot-supplement.yml';s=path.read_text();data=yaml.safe_load(s)
  self.assertEqual([{'id':'light-portrait','night':'no','orientation':'portrait'}],data['jobs']['missing_native']['strategy']['matrix']['include'])
  for required in ['13347602297','65a1312363456a8ccf087e9f7df60e9abf5e9358768f4e070cea15c17c0a49a1','acfd671d4afa4fceb09475cc6e70d1dabb77aefe','126746123138','tests_executed\':0','Timeout waiting for emulator to boot.','mynotes.tools.ci.run_ordered_modules','--additional-scenes-required','assert {j[\'id\'] for j in failed}=={126746123138}']:
   self.assertIn(required,s)
  for forbidden in ['sh gradlew','run_module_benchmark.sh','continue-on-error: true','gh pr merge','git checkout -B']:self.assertNotIn(forbidden,s)
 def test_animation_requalification_explicitly_sets_and_checks_scale_not_null_default(self):
  s=(ROOT/'.github/workflows/notes-candidate67-animated-reorder.yml').read_text()
  for key in ['window_animation_scale','transition_animation_scale','animator_duration_scale']:self.assertIn('settings put global '+key+' 1.0',s)
  self.assertIn('assert float(Path("animation-scale.txt").read_text().strip())==1.0',s)

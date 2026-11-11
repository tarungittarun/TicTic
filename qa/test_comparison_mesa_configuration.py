"""Guard the environment promoted from the bounded Mesa capture control.

These assertions protect configuration consistency, not emulator stability or
functional/human acceptance (which require independently retained runtime data).
"""
import unittest
from pathlib import Path
import yaml

ROOT = Path(__file__).resolve().parents[1]
WORKFLOWS = ['notes-comparison37-capture-isolation.yml', 'notes-comparison37-shards.yml']


class MesaConfigurationTests(unittest.TestCase):
    def workflows(self):
        for name in WORKFLOWS:
            text = (ROOT / '.github/workflows' / name).read_text()
            yield name, text, yaml.safe_load(text)['jobs']['compare']

    def test_matching_renderer_capture_policy_and_binary_pin(self):
        for name, _, job in self.workflows():
            with self.subTest(workflow=name):
                self.assertEqual(job['env']['LIBGL_ALWAYS_SOFTWARE'], '1')
                self.assertEqual(job['env']['COMPARISON_CAPTURE_POLICY'], 'adb_only')
                self.assertNotEqual(job['env'].get('COMPARISON_TRACE_EMULATOR_EXIT'), 'yes')
                step = next(s for s in job['steps'] if s.get('uses', '').startswith('ReactiveCircus/android-emulator-runner@'))
                self.assertEqual(step['with']['emulator-build'], 16428233)
                self.assertIn('-gpu host ', step['with']['emulator-options'])
                self.assertIn('run_with_host_diagnostics.py', step['with']['script'])

    def test_host_renderer_preflight_and_acceptance_fence_remain(self):
        for name, text, job in self.workflows():
            with self.subTest(workflow=name):
                step = next(s for s in job['steps'] if s.get('name') == 'Prepare and identify host software OpenGL')
                self.assertIn('DISPLAY=:99 glxinfo -B', step['run'])
                self.assertIn("echo 'DISPLAY=:99'", step['run'])
                self.assertIn("assert not d['whole_module_acceptance'] and not d['physical_acceptance']", text)

    def test_guest_loss_diagnostic_is_single_dark_landscape_shard_with_safe_trace(self):
        text = (ROOT / '.github/workflows/notes-comparison37-guest-loss-diagnostic.yml').read_text()
        job = yaml.safe_load(text)['jobs']['diagnose']
        env = job['env']
        self.assertEqual(env['COMPARISON_PROFILE'], 'dark-landscape')
        self.assertEqual(env['COMPARISON_RANGE'], '31-37')
        self.assertEqual(env['COMPARISON_ORIENTATION'], 'landscape')
        self.assertEqual(env['LIBGL_ALWAYS_SOFTWARE'], '1')
        self.assertEqual(env['COMPARISON_CAPTURE_POLICY'], 'adb_only')
        self.assertEqual(env['COMPARISON_TRACE_EMULATOR_EXIT'], 'yes')
        self.assertEqual(env['COMPARISON_TRACE_CODE_STACKS'], 'yes')
        emulator = next(step for step in job['steps'] if step.get('uses', '').startswith('ReactiveCircus/android-emulator-runner@'))
        self.assertEqual(emulator['with']['emulator-build'], 16428233)
        self.assertIn('-gpu host ', emulator['with']['emulator-options'])
        self.assertIn('run_with_host_diagnostics.py', emulator['with']['script'])
        self.assertTrue(any(step.get('if') == 'always()' and step.get('uses', '').startswith('actions/upload-artifact@') for step in job['steps']))

    def test_broad_grid_covers_each_pair_exactly_once_per_profile(self):
        _, _, job = next(item for item in self.workflows() if item[0].endswith('shards.yml'))
        pairs = []
        for item in job['strategy']['matrix']['include']:
            first, last = map(int, item['range'].split('-'))
            pairs.extend((item['id'], number) for number in range(first, last + 1))
        self.assertEqual(len(pairs), 4 * 37)
        self.assertEqual(len(set(pairs)), len(pairs))
        self.assertEqual({number for _, number in pairs}, set(range(1, 38)))

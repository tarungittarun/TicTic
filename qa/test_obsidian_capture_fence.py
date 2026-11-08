"""Unit checks of synchronization only; these are not device-test evidence."""
import importlib.util
from pathlib import Path
import sys
import types
import unittest
from unittest.mock import Mock, patch

SOURCE = Path(__file__).resolve().parents[1] / 'mynotes/tools/comparisons/obsidian_cdp.py'

class ObsidianCaptureFenceTest(unittest.TestCase):
    def device(self):
        spec = importlib.util.spec_from_file_location('capture_fence_under_test', SOURCE)
        module = importlib.util.module_from_spec(spec)
        with patch.dict(sys.modules, {'websocket': types.SimpleNamespace()}):
            spec.loader.exec_module(module)
        device = object.__new__(module.Device)
        device.js = Mock(return_value='two-frames')
        device.call = Mock(return_value={'data': 'unit-test-only-discarded-bitmap'})
        return module, device

    def test_webview_snapshot_is_only_a_fence_not_evidence(self):
        module, device = self.device()
        with patch.object(module.time, 'sleep'):
            result = device.settle()
        self.assertTrue(result['compositor_fence'])
        self.assertNotIn('data', result)
        self.assertIn('adb exec-out screencap', result['evidence_source'])
        self.assertEqual(device.call.call_args.args[0], 'Page.captureScreenshot')

    def test_unsupported_compositor_fence_is_explicit(self):
        module, device = self.device()
        device.call.side_effect = RuntimeError('unsupported')
        with patch.object(module.time, 'sleep'):
            result = device.settle()
        self.assertFalse(result['compositor_fence'])

    def test_animation_frame_wait_is_bounded(self):
        module, device = self.device()
        device.js.return_value = 'timeout'
        with patch.object(module.time, 'sleep'):
            result = device.settle()
        self.assertEqual(result['animation_frames'], 'timeout')
        self.assertIn('setTimeout', device.js.call_args.args[0])
        self.assertIn('2000', device.js.call_args.args[0])


    def test_observer_isolation_keeps_adb_evidence_without_extra_gpu_capture(self):
        module, device = self.device()
        with patch.dict(module.os.environ, {'COMPARISON_CAPTURE_POLICY':'adb_only'}), patch.object(module.time,'sleep'):
            result=device.settle()
        device.call.assert_not_called()
        self.assertFalse(result['compositor_fence'])
        self.assertIn('not a proven guest-loss fix',result['policy'])
        self.assertIn('adb exec-out screencap',result['evidence_source'])

"""Emulator-runner lines are separate shells; diagnostics must share a process."""
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import types
import unittest
from unittest.mock import Mock, patch
import yaml


class HostFinallyTest(unittest.TestCase):
    def module(self):
        spec=importlib.util.spec_from_file_location('host_finally',Path('mynotes/tools/comparisons/run_with_host_diagnostics.py'))
        module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module

    def test_original_failure_is_not_masked_and_host_collection_still_runs(self):
        module=self.module()
        for error in [RuntimeError('dummy guest unavailable'),SystemExit(7)]:
            with patch.object(module,'discover_qemu_pid',return_value=123),patch.object(module.runpy,'run_path',side_effect=error),patch.object(module,'collect_host') as collect,patch.object(module.traceback,'print_exc'):
                self.assertEqual(module.main(),7 if isinstance(error,SystemExit) else 1)
                collect.assert_called_once_with(Path('comparison37-evidence'),qemu_pid_hint=123)

    def test_success_collects_host_without_changing_original_result(self):
        module=self.module()
        with patch.object(module,'discover_qemu_pid',return_value=None),patch.object(module.runpy,'run_path'),patch.object(module,'collect_host') as collect:
            self.assertEqual(module.main(),0)
            collect.assert_called_once_with(Path('comparison37-evidence'),qemu_pid_hint=None)

    def test_discovers_only_one_owned_qemu_pid_without_reading_arguments(self):
        module=self.module()
        output='41 1001 qemu-system-x86_64\n42 1001 python\n43 0 qemu-system-x86_64\n'
        with patch.object(module.os,'getuid',return_value=1001),patch.object(module.subprocess,'run',return_value=subprocess.CompletedProcess([],0,output,'')) as run:
            self.assertEqual(module.discover_qemu_pid(),41)
            self.assertEqual(run.call_args.args[0],['ps','-eo','pid=,uid=,comm='])
            self.assertEqual(set(run.call_args.kwargs),{'capture_output','text','timeout'})
        with patch.object(module.os,'getuid',return_value=1001),patch.object(module.subprocess,'run',return_value=subprocess.CompletedProcess([],0,'41 1001 qemu-system-x86_64\n44 1001 qemu-system-x86_64\n','')):
            self.assertIsNone(module.discover_qemu_pid())

    def test_apport_wait_runs_when_proc_confirms_qemu_exit_even_if_tracer_cleanup_was_harness_owned(self):
        module=self.module()
        crash = types.ModuleType('emulator_crash_metadata')
        crash.collect = Mock(return_value={'candidate_count': 0, 'reports': []})
        apport = types.ModuleType('apport_crash_metadata')
        apport.wait_for_reports = Mock(return_value={
            'wait_attempted': True, 'waited_seconds': 3, 'qemu_report_count': 0,
            'reports': [], 'raw_reports_retained': False, 'raw_cores_read': False,
        })
        with tempfile.TemporaryDirectory() as directory:
            output=Path(directory)
            (output/'emulator-exit-trace-receipt.json').write_text(json.dumps({
                'pid': 123, 'stopped_by_harness': True,
                'returncode_before_harness_stop': None,
            }))
            with patch.dict(sys.modules, {
                    'emulator_crash_metadata': crash,
                    'apport_crash_metadata': apport,
                }), patch.dict(module.os.environ, {
                    'COMPARISON_WAIT_FOR_CRASH_REPORT': 'yes',
                    'COMPARISON_CRASH_REPORT_WAIT_SECONDS': '7',
                }), patch.object(module, 'qemu_process_status', return_value={
                    'state': 'zombie', 'running': False, 'terminated': True,
                }), patch.object(module.subprocess, 'run', return_value=subprocess.CompletedProcess([],0,'','')):
                module.collect_host(output)
            host=json.loads((output/'host-after.json').read_text())
        apport.wait_for_reports.assert_called_once()
        self.assertEqual(apport.wait_for_reports.call_args.kwargs['timeout_seconds'],7)
        self.assertFalse(host['qemu_process']['trace_ended_before_harness'])
        self.assertTrue(host['qemu_process']['exit_confirmed_for_apport_wait'])
        self.assertEqual(host['apport_crash_metadata']['wait_reason'],'bounded_post_exit_wait')

    def test_apport_wait_uses_preflight_qemu_identity_when_exit_trace_is_disabled(self):
        module=self.module()
        crash=types.ModuleType('emulator_crash_metadata');crash.collect=Mock(return_value={'candidate_count':0,'reports':[]})
        apport=types.ModuleType('apport_crash_metadata')
        apport.wait_for_reports=Mock(return_value={
            'wait_attempted':True,'waited_seconds':2,'qemu_report_count':0,'reports':[],
            'raw_reports_retained':False,'raw_cores_read':False,
        })
        with tempfile.TemporaryDirectory() as directory:
            output=Path(directory)
            with patch.dict(sys.modules,{'emulator_crash_metadata':crash,'apport_crash_metadata':apport}),patch.dict(module.os.environ,{
                'COMPARISON_WAIT_FOR_CRASH_REPORT':'yes','COMPARISON_CRASH_REPORT_WAIT_SECONDS':'4',
            }),patch.object(module,'qemu_process_status',return_value={
                'state':'not_present','running':False,'terminated':True,
            }),patch.object(module.subprocess,'run',return_value=subprocess.CompletedProcess([],0,'','')):
                module.collect_host(output,qemu_pid_hint=123)
            host=json.loads((output/'host-after.json').read_text())
        apport.wait_for_reports.assert_called_once()
        self.assertEqual(apport.wait_for_reports.call_args.kwargs['timeout_seconds'],4)
        self.assertEqual(host['qemu_process']['identity_source'],'preflight_owned_qemu_scan')
        self.assertIsNone(host['qemu_process']['trace_ended_before_harness'])
        self.assertTrue(host['qemu_process']['exit_confirmed_for_apport_wait'])

    def test_emulator_script_does_not_depend_on_cross_shell_variables_or_heredoc(self):
        workflow=yaml.safe_load(Path('.github/workflows/notes-comparison37-capture-isolation.yml').read_text())
        step=next(s for s in workflow['jobs']['compare']['steps'] if s.get('uses','').startswith('ReactiveCircus/'))
        script=step['with']['script']
        self.assertIn('python mynotes/tools/comparisons/run_with_host_diagnostics.py',script)
        self.assertNotIn('<<',script);self.assertNotIn('result=',script)

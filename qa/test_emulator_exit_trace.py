"""Unit-test host tracer ownership, filtering and separate process-group cleanup."""
import importlib.util
import json
from pathlib import Path
import signal
import subprocess
import tempfile
import unittest
from unittest.mock import Mock,patch


def load():
    spec=importlib.util.spec_from_file_location('exit_trace',Path('mynotes/tools/comparisons/emulator_exit_trace.py'))
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module


class EmulatorExitTraceTest(unittest.TestCase):
    def test_discovery_ignores_other_users_and_non_qemu_processes(self):
        module=load()
        self.assertEqual(module.owned_qemu_pids('41 1001 qemu-system-x86\n42 0 qemu-system-x86\n43 1001 python\n',1001),[41])

    def test_trace_filters_arguments_and_stops_only_its_own_process_group(self):
        module=load()
        with tempfile.TemporaryDirectory() as directory,patch.dict(module.os.environ,{'COMPARISON_TRACE_EMULATOR_EXIT':'yes'}),patch.object(module.os,'getuid',return_value=1001),patch.object(module.time,'sleep'):
            process=Mock(pid=99);process.poll.side_effect=[None,None,0]
            with patch.object(module.subprocess,'run',return_value=subprocess.CompletedProcess([],0,'41 1001 qemu-system-x86\n','')),patch.object(module.subprocess,'Popen',return_value=process) as spawn,patch.object(module.os,'killpg') as kill:
                tracer=module.ExitTracer(directory);tracer.start();tracer.stop()
            arguments=spawn.call_args.args[0]
            self.assertIn('trace=exit,exit_group,kill,tgkill',arguments)
            self.assertNotIn('execve',str(arguments));self.assertTrue(spawn.call_args.kwargs['start_new_session'])
            self.assertEqual(arguments[-2:],['-p','41'])
            kill.assert_called_once_with(99,signal.SIGTERM)
            receipt=json.loads((Path(directory)/'emulator-exit-trace-receipt.json').read_text())
            self.assertFalse(receipt['raw_memory_dumped']);self.assertFalse(receipt['environment_collected'])
            self.assertTrue(receipt['stopped_by_harness'])

    def test_does_not_stop_a_tracer_that_already_observed_process_exit(self):
        module=load()
        with tempfile.TemporaryDirectory() as directory,patch.object(module.os,'killpg') as kill:
            tracer=module.ExitTracer(directory);tracer.process=Mock(pid=99);tracer.process.poll.return_value=0
            tracer.stop();kill.assert_not_called()
            receipt=json.loads((Path(directory)/'emulator-exit-trace-receipt.json').read_text())
            self.assertFalse(receipt['stopped_by_harness'])

    def test_disabled_trace_starts_no_process(self):
        module=load()
        with tempfile.TemporaryDirectory() as directory,patch.dict(module.os.environ,{'COMPARISON_TRACE_EMULATOR_EXIT':'no'}),patch.object(module.subprocess,'Popen') as spawn:
            tracer=module.ExitTracer(directory);tracer.start();tracer.stop();spawn.assert_not_called()

    def test_code_stack_mode_excludes_frequent_kick_syscalls_and_value_dumps(self):
        module=load()
        with tempfile.TemporaryDirectory() as directory,patch.dict(module.os.environ,{'COMPARISON_TRACE_EMULATOR_EXIT':'yes','COMPARISON_TRACE_CODE_STACKS':'yes'}),patch.object(module.os,'getuid',return_value=1001),patch.object(module.time,'sleep'):
            process=Mock(pid=99);process.poll.return_value=0
            with patch.object(module.subprocess,'run',return_value=subprocess.CompletedProcess([],0,'41 1001 qemu-system-x86\n','')),patch.object(module.subprocess,'Popen',return_value=process) as spawn:
                tracer=module.ExitTracer(directory);tracer.start();tracer.stop()
            arguments=spawn.call_args.args[0]
            self.assertIn('-k',arguments);self.assertIn('trace=exit,exit_group',arguments)
            self.assertNotIn('trace=all',arguments);self.assertNotIn('tgkill',str(arguments))
            self.assertFalse(tracer.receipt['raw_memory_dumped'])

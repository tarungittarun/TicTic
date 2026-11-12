"""Unit-test host tracer ownership, filtering and separate process-group cleanup."""
import importlib.util
import json
from pathlib import Path
import signal
import subprocess
import tempfile
import threading
import unittest
from unittest.mock import Mock,patch


def load():
    spec=importlib.util.spec_from_file_location('exit_trace',Path('mynotes/tools/comparisons/emulator_exit_trace.py'))
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module


class EmulatorExitTraceTest(unittest.TestCase):
    def test_fatal_signal_is_matched_to_sanitized_owned_process_identity(self):
        module=load()
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);process=root/'3843';process.mkdir()
            (process/'comm').write_text('qemu-system-x86_64\n')
            (process/'stat').write_text('3843 (qemu-system-x86_64 (worker)) t 3296 2 3\n')
            (process/'status').write_text('Name:\tqemu-system-x86\nTgid:\t3296\nPPid:\t3296\nUid:\t1001\t1001\t1001\t1001\n')
            executable=root/'qemu-system-x86_64';executable.write_text('')
            (process/'exe').symlink_to(executable)
            line='3843  1794224487.379534 --- SIGSEGV {si_signo=SIGSEGV, si_code=SEGV_MAPERR, si_addr=0x563d0b504600} ---'
            event=module.signal_identity_from_trace_line(line,3296,root,1001)
        self.assertEqual(event['signal'],'SIGSEGV')
        self.assertEqual(event['signal_code'],'SEGV_MAPERR')
        self.assertEqual(event['process_identity']['thread_group_pid'],3296)
        self.assertTrue(event['process_identity']['in_qemu_thread_group'])
        self.assertEqual(event['process_identity']['executable_basename'],'qemu-system-x86_64')
        self.assertNotIn('si_addr',str(event))
        self.assertNotIn(directory,str(event))
        self.assertFalse(event['process_identity']['command_line_read'])

    def test_signal_parser_ignores_nonfatal_or_unrecognized_trace_lines(self):
        module=load()
        self.assertIsNone(module.signal_identity_from_trace_line(
            '123 1.0 --- SIGUSR1 {si_signo=SIGUSR1, si_code=SI_TKILL} ---',123))
        self.assertIsNone(module.signal_identity_from_trace_line('123 exit_group(0) = ?',123))

    def test_qemu_termination_check_distinguishes_live_zombie_and_missing_processes(self):
        module=load()
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);process=root/'41';process.mkdir()
            (process/'comm').write_text('qemu-system-x86_64\n')
            (process/'stat').write_text('41 (qemu-system-x86_64) Z 1 2 3\n')
            self.assertTrue(module.qemu_process_terminated(41,root))
            (process/'stat').write_text('41 (qemu-system-x86_64) S 1 2 3\n')
            self.assertFalse(module.qemu_process_terminated(41,root))
            self.assertTrue(module.qemu_process_terminated(42,root))
        self.assertIsNone(module.qemu_process_terminated(None))

    def test_naturally_drains_tracer_after_confirmed_qemu_exit_before_killing_it(self):
        module=load()
        with tempfile.TemporaryDirectory() as directory,patch.object(module.os,'killpg') as kill,patch.object(module,'qemu_process_terminated',return_value=True):
            tracer=module.ExitTracer(directory);tracer.receipt['pid']=41
            tracer.process=Mock(pid=99);tracer.process.poll.side_effect=[None,0,0]
            tracer.stop()
            tracer.process.wait.assert_called_once_with(timeout=2)
            kill.assert_not_called()
            receipt=json.loads((Path(directory)/'emulator-exit-trace-receipt.json').read_text())
            self.assertFalse(receipt['stopped_by_harness'])
            self.assertEqual(receipt['returncode_before_harness_stop'],0)
            self.assertTrue(receipt['target_qemu_terminated_at_harness_stop'])

    def test_live_trace_watcher_records_only_sanitized_fatal_signal_events(self):
        module=load()
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            (root/'emulator-exit-trace.txt').write_text('123 1.5 --- SIGSEGV {si_code=SEGV_MAPERR} ---\n')
            tracer=module.ExitTracer(root);tracer.receipt['pid']=123
            observed=threading.Event()
            def parse(line,target_pid):
                observed.set()
                return {'trace_pid':123,'signal':'SIGSEGV','target_pid':target_pid}
            with patch.object(module,'signal_identity_from_trace_line',side_effect=parse):
                watcher=threading.Thread(target=tracer._watch_signal_identities)
                watcher.start()
                self.assertTrue(observed.wait(1))
                tracer.signal_stop.set();watcher.join(timeout=1)
            self.assertFalse(watcher.is_alive())
            self.assertEqual(tracer.signal_events,[{'trace_pid':123,'signal':'SIGSEGV',
                                                    'target_pid':123}])
            self.assertNotIn('address',str(tracer.signal_events))

    def test_discovery_ignores_other_users_and_non_qemu_processes(self):
        module=load()
        self.assertEqual(module.owned_qemu_pids('41 1001 qemu-system-x86\n42 0 qemu-system-x86\n43 1001 python\n',1001),[41])

    def test_trace_filters_arguments_and_stops_only_its_own_process_group(self):
        module=load()
        with tempfile.TemporaryDirectory() as directory,patch.dict(module.os.environ,{'COMPARISON_TRACE_EMULATOR_EXIT':'yes'}),patch.object(module.os,'getuid',return_value=1001),patch.object(module.time,'sleep'):
            process=Mock(pid=99);process.poll.side_effect=[None,None,0]
            with patch.object(module.subprocess,'run',return_value=subprocess.CompletedProcess([],0,'41 1001 qemu-system-x86\n','')),patch.object(module.subprocess,'Popen',return_value=process) as spawn,patch.object(module.os,'killpg') as kill,patch.object(module,'qemu_process_terminated',return_value=False):
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

"""Opt-in host exit/signal tracing, not Android app performance instrumentation.

Attach only to one QEMU process owned by this CI user. Syscall filtering excludes
file/network I/O, environment, exec arguments and memory dumps. The tracer is a
separate process group: stopping it must never signal the emulator process.
"""
import json
import os
from pathlib import Path
import signal
import subprocess
import time


def owned_qemu_pids(text, uid):
    found = []
    for line in text.splitlines():
        fields = line.split()
        if len(fields) == 3 and fields[0].isdigit() and fields[1].isdigit():
            if int(fields[1]) == uid and fields[2].startswith('qemu-system'):
                found.append(int(fields[0]))
    return found


class ExitTracer:
    def __init__(self, output):
        self.output = Path(output)
        self.process = None
        self.stderr = None
        self.receipt = {'scope': 'Host emulator exit/signal diagnostics only; not app fault attribution',
                        'enabled': os.environ.get('COMPARISON_TRACE_EMULATOR_EXIT') == 'yes',
                        'syscalls': ['exit', 'exit_group', 'kill', 'tgkill'],
                        'symbolic_code_stacks_only': os.environ.get('COMPARISON_TRACE_CODE_STACKS') == 'yes',
                        'raw_memory_dumped': False, 'environment_collected': False}

    def start(self):
        self.output.mkdir(parents=True, exist_ok=True)
        if not self.receipt['enabled']:
            return
        try:
            ps = subprocess.run(['ps', '-eo', 'pid=,uid=,comm='], capture_output=True, text=True, timeout=5)
            if ps.returncode:
                raise RuntimeError('Process discovery failed')
            pids = owned_qemu_pids(ps.stdout, os.getuid())
            self.receipt['matching_process_count'] = len(pids)
            if len(pids) != 1:
                self.receipt['not_started'] = 'Requires exactly one current-user QEMU process'
                return
            self.receipt['pid'] = pids[0]
            self.stderr = (self.output / 'emulator-exit-tracer-status.txt').open('w')
            command=['sudo', '-n', 'strace', '-f', '-ttt']
            if self.receipt['symbolic_code_stacks_only']:
                # strace -k emits module/function/code offsets, not arguments or
                # local variables. Avoid frequent SIGUSR1/tgkill kick stacks.
                command+=['-k','-e','trace=exit,exit_group','-e',
                          'signal=SIGSEGV,SIGABRT,SIGBUS,SIGILL,SIGFPE,SIGKILL,SIGTERM,SIGINT']
                self.receipt['syscalls']=['exit','exit_group']
            else:
                command+=['-e','trace=exit,exit_group,kill,tgkill','-e','signal=all']
            command+=['-o',str(self.output/'emulator-exit-trace.txt'),'-p',str(pids[0])]
            self.process = subprocess.Popen(command,
                stdin=subprocess.DEVNULL, stdout=self.stderr, stderr=self.stderr, start_new_session=True)
            time.sleep(.3)
            self.receipt['tracer_early_returncode'] = self.process.poll()
        except (OSError, RuntimeError, subprocess.SubprocessError) as error:
            self.receipt['not_started'] = type(error).__name__

    def _signal_group(self, sig):
        try:
            os.killpg(self.process.pid, sig)
        except PermissionError:
            # sudo may run the tracer as root. Target only the dedicated group
            # created by this Popen, never QEMU's process or process group.
            result = subprocess.run(['sudo', '-n', 'kill', '-'+sig.name.removeprefix('SIG'),
                                     '--', '-'+str(self.process.pid)], capture_output=True, timeout=5)
            if result.returncode:
                self.receipt['tracer_stop_signal_failed'] = True

    def stop(self):
        try:
            if self.process is not None:
                before = self.process.poll()
                self.receipt['returncode_before_harness_stop'] = before
                self.receipt['stopped_by_harness'] = before is None
                if before is None:
                    # Popen created a dedicated tracer group. QEMU is NOT in it.
                    try:
                        self._signal_group(signal.SIGTERM)
                    except ProcessLookupError:
                        pass
                    try:
                        self.process.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        try:
                            self._signal_group(signal.SIGKILL)
                        except ProcessLookupError:
                            pass
                        self.process.wait(timeout=5)
                self.receipt['returncode_after_harness_stop'] = self.process.poll()
        finally:
            if self.stderr is not None:
                self.stderr.close()
            (self.output / 'emulator-exit-trace-receipt.json').write_text(json.dumps(self.receipt, indent=2))

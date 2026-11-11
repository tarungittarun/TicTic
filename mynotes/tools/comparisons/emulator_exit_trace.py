"""Opt-in host exit/signal tracing, not Android app performance instrumentation.

Attach only to one QEMU process owned by this CI user. Syscall filtering excludes
file/network I/O, environment, exec arguments and memory dumps. The tracer is a
separate process group: stopping it must never signal the emulator process.
"""
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import threading
import time


_FATAL_SIGNAL_RE = re.compile(
    r"^\s*(\d+)\s+(\d+(?:\.\d+)?)\s+--- "
    r"(SIG(?:SEGV|ABRT|BUS|ILL|FPE)) \{([^}]*)\} ---"
)
_SAFE_PROCESS_NAME_RE = re.compile(r"[A-Za-z0-9_.+\-]{1,128}\Z")
_SAFE_SIGNAL_CODE_RE = re.compile(r"[A-Za-z0-9_]{1,48}\Z")


def process_identity(pid, target_pid, proc_root=Path('/proc'), owner_uid=None):
    """Read only owned /proc identity fields; never arguments, env, or memory."""
    if isinstance(pid, bool) or not isinstance(pid, int) or pid <= 0:
        return None
    owner_uid = os.getuid() if owner_uid is None else owner_uid
    process = Path(proc_root) / str(pid)
    try:
        comm = (process / 'comm').read_text().strip()
        stat = (process / 'stat').read_text()
        status = (process / 'status').read_text()
        executable = os.readlink(process / 'exe')
    except OSError:
        return None
    values = {}
    for line in status.splitlines():
        key, separator, value = line.partition(':')
        if separator and key in {'Tgid', 'PPid', 'Uid'}:
            values[key] = value.strip().split()
    try:
        thread_group = int(values['Tgid'][0])
        parent_pid = int(values['PPid'][0])
        process_uid = int(values['Uid'][0])
    except (KeyError, IndexError, ValueError):
        return None
    if process_uid != owner_uid:
        return None
    closing_paren = stat.rfind(')')
    stat_fields = stat[closing_paren + 2:].split() if closing_paren >= 0 else []
    if not stat_fields:
        return None
    process_name = comm if _SAFE_PROCESS_NAME_RE.fullmatch(comm) else '[name suppressed]'
    executable_name = Path(executable).name
    if not _SAFE_PROCESS_NAME_RE.fullmatch(executable_name):
        executable_name = '[name suppressed]'
    return {
        'pid': pid,
        'thread_group_pid': thread_group,
        'parent_pid': parent_pid,
        'state': stat_fields[0] if stat_fields[0] in {'R', 'S', 'D', 'T', 't', 'Z', 'X', 'I', 'P'} else 'other',
        'comm': process_name,
        'executable_basename': executable_name,
        'in_qemu_thread_group': thread_group == target_pid,
        'command_line_read': False,
        'environment_read': False,
        'memory_read': False,
    }


def signal_identity_from_trace_line(line, target_pid, proc_root=Path('/proc'), owner_uid=None):
    """Correlate one fatal strace signal with sanitized /proc identity, if live."""
    match = _FATAL_SIGNAL_RE.match(line)
    if not match:
        return None
    code_match = re.search(r"\bsi_code=([^, }]+)", match.group(4))
    code = code_match.group(1) if code_match else None
    if code is not None and not _SAFE_SIGNAL_CODE_RE.fullmatch(code):
        code = None
    return {
        'trace_pid': int(match.group(1)),
        'trace_timestamp': match.group(2),
        'signal': match.group(3),
        'signal_code': code,
        # Do not retain si_addr or any other raw siginfo field.
        'process_identity': process_identity(int(match.group(1)), target_pid, proc_root, owner_uid),
    }


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
        self.signal_stop = threading.Event()
        self.signal_thread = None
        self.signal_events = []
        self.signal_events_dropped = 0
        self.trace_path = self.output / 'emulator-exit-trace.txt'
        self.receipt = {'scope': 'Host emulator exit/signal diagnostics only; not app fault attribution',
                        'enabled': os.environ.get('COMPARISON_TRACE_EMULATOR_EXIT') == 'yes',
                        'syscalls': ['exit', 'exit_group', 'kill', 'tgkill'],
                        'symbolic_code_stacks_only': os.environ.get('COMPARISON_TRACE_CODE_STACKS') == 'yes',
                        'signal_identity_scope': 'Fatal traced-signal PID plus sanitized owned /proc identity only; no arguments, environment, or memory',
                        'raw_memory_dumped': False, 'environment_collected': False}

    def _watch_signal_identities(self):
        stream = None
        try:
            while not self.signal_stop.is_set():
                if stream is None:
                    try:
                        stream = self.trace_path.open('r', errors='replace')
                    except FileNotFoundError:
                        self.signal_stop.wait(.05)
                        continue
                line = stream.readline()
                if not line:
                    self.signal_stop.wait(.05)
                    continue
                event = signal_identity_from_trace_line(line, self.receipt.get('pid'))
                if event is None:
                    continue
                if len(self.signal_events) < 64:
                    self.signal_events.append(event)
                else:
                    self.signal_events_dropped += 1
        except Exception as error:
            self.receipt['signal_identity_observer_error'] = type(error).__name__
        finally:
            if stream is not None:
                stream.close()

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
            # Pre-create a user-readable trace file. strace runs under sudo;
            # the concurrent identity watcher must be able to read signal lines.
            self.trace_path.touch(mode=0o644, exist_ok=True)
            self.trace_path.chmod(0o644)
            command=['sudo', '-n', 'strace', '-f', '-ttt']
            if self.receipt['symbolic_code_stacks_only']:
                # strace -k emits module/function/code offsets, not arguments or
                # local variables. Avoid frequent SIGUSR1/tgkill kick stacks.
                command+=['-k','-e','trace=exit,exit_group','-e',
                          'signal=SIGSEGV,SIGABRT,SIGBUS,SIGILL,SIGFPE,SIGKILL,SIGTERM,SIGINT']
                self.receipt['syscalls']=['exit','exit_group']
            else:
                command+=['-e','trace=exit,exit_group,kill,tgkill','-e','signal=all']
            command+=['-o',str(self.trace_path),'-p',str(pids[0])]
            self.process = subprocess.Popen(command,
                stdin=subprocess.DEVNULL, stdout=self.stderr, stderr=self.stderr, start_new_session=True)
            self.signal_thread = threading.Thread(target=self._watch_signal_identities, daemon=True)
            self.signal_thread.start()
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
            self.signal_stop.set()
            if self.signal_thread is not None:
                self.signal_thread.join(timeout=2)
            self.receipt['signal_process_identities'] = self.signal_events
            self.receipt['signal_process_identity_events_dropped'] = self.signal_events_dropped
            if self.stderr is not None:
                self.stderr.close()
            (self.output / 'emulator-exit-trace-receipt.json').write_text(json.dumps(self.receipt, indent=2))

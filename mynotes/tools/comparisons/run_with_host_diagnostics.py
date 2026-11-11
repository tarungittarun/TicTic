"""Run one comparison command and collect public CI host diagnostics in finally.

The emulator action executes each input line in a separate shell. Keep exception
handling and diagnostics in this process, not a multiline shell/heredoc input.
No process arguments, environment, credentials, private library or host home files.
"""
import json
import os
from pathlib import Path
import runpy
import subprocess
import traceback


def qemu_process_status(pid, proc_root=Path('/proc')):
    """Distinguish a running QEMU from a zombie, missing PID, or reused PID."""
    if isinstance(pid, bool) or not isinstance(pid, int) or pid <= 0:
        return {'state': 'unknown', 'running': False, 'terminated': False}
    process = Path(proc_root) / str(pid)
    try:
        comm = (process / 'comm').read_text().strip()
        stat = (process / 'stat').read_text()
    except FileNotFoundError:
        return {'state': 'not_present', 'running': False, 'terminated': True}
    except OSError:
        return {'state': 'unreadable', 'running': False, 'terminated': False}
    if not comm.startswith('qemu-system'):
        return {'state': 'different_process', 'running': False, 'terminated': True}
    closing_paren = stat.rfind(')')
    fields = stat[closing_paren + 2:].split() if closing_paren >= 0 else []
    if not fields:
        return {'state': 'unknown', 'running': False, 'terminated': False}
    state = fields[0]
    if state == 'Z':
        return {'state': 'zombie', 'running': False, 'terminated': True}
    if state == 'X':
        return {'state': 'dead', 'running': False, 'terminated': True}
    return {'state': 'running', 'running': True, 'terminated': False}


def collect_host(output):
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    from emulator_crash_metadata import collect
    trace_receipt = {}
    receipt_path = output / 'emulator-exit-trace-receipt.json'
    try:
        if receipt_path.is_file() and not receipt_path.is_symlink():
            trace_receipt = json.loads(receipt_path.read_text())
    except (OSError, ValueError):
        trace_receipt = {}
    qemu_pid = trace_receipt.get('pid')
    qemu_status = qemu_process_status(qemu_pid)
    trace_ended_before_harness = (
        trace_receipt.get('stopped_by_harness') is False
        and trace_receipt.get('returncode_before_harness_stop') is not None
    )
    apport_metadata = {'wait_attempted': False, 'waited_seconds': 0,
                       'qemu_report_count': 0, 'reports': [],
                       'raw_reports_retained': False, 'raw_cores_read': False}
    wait_enabled = os.environ.get('COMPARISON_WAIT_FOR_CRASH_REPORT') == 'yes'
    if wait_enabled and qemu_status['terminated'] and trace_ended_before_harness:
        from apport_crash_metadata import wait_for_reports
        try:
            limit = int(os.environ.get('COMPARISON_CRASH_REPORT_WAIT_SECONDS', '120'))
        except ValueError:
            limit = 120
        apport_metadata = wait_for_reports(Path('/var/crash'), timeout_seconds=limit)
    if not wait_enabled:
        apport_metadata['wait_reason'] = 'disabled_by_configuration'
    elif not qemu_status['terminated']:
        apport_metadata['wait_reason'] = 'qemu_exit_not_confirmed'
    elif not trace_ended_before_harness:
        apport_metadata['wait_reason'] = 'tracer_not_ended_before_harness'
    else:
        apport_metadata['wait_reason'] = 'bounded_post_exit_wait'
    data = {
        'scope': 'Public CI host diagnostics; not app fault attribution', 'proc': {},
        'qemu_process': {'pid': qemu_pid, **qemu_status,
                         'trace_ended_before_harness': trace_ended_before_harness},
        'emulator_crash_metadata': collect(),
        'apport_crash_metadata': apport_metadata,
    }
    for name in ['meminfo', 'pressure/memory', 'pressure/io']:
        try:
            data['proc'][name] = (Path('/proc') / name).read_text()
        except OSError as error:
            data['proc'][name] = {'error': str(error)}
    commands = {'processes': ['ps', '-eo', 'pid,comm,rss,stat'],
                'disk': ['df', '-h'],
                'kernel': ['sudo', '-n', 'dmesg', '--level=err,warn']}
    for name, command in commands.items():
        try:
            result = subprocess.run(command, capture_output=True, text=True, timeout=15)
            data[name] = {'returncode': result.returncode, 'stdout': result.stdout[-30000:],
                          'stderr': result.stderr[-2000:]}
        except Exception as error:
            data[name] = {'error': str(error)}
    (output / 'host-after.json').write_text(json.dumps(data, indent=2))


def main():
    tracer = None
    try:
        if os.environ.get('COMPARISON_TRACE_EMULATOR_EXIT') == 'yes':
            from emulator_exit_trace import ExitTracer
            tracer = ExitTracer(Path('comparison37-evidence'))
            tracer.start()
        runpy.run_path(str(Path(__file__).with_name('paired_modules37.py')), run_name='__main__')
        if os.environ.get('COMPARISON_GUEST_LOSS_REPLAY') == 'yes':
            runpy.run_path(str(Path(__file__).with_name('guest_loss_lifecycle_replay.py')),
                           run_name='__main__')
        return 0
    except SystemExit as error:
        return error.code if isinstance(error.code, int) else (0 if error.code is None else 1)
    except Exception:
        traceback.print_exc()
        return 1
    finally:
        try:
            if tracer is not None:
                tracer.stop()
        finally:
            collect_host(Path('comparison37-evidence'))


if __name__ == '__main__':
    raise SystemExit(main())

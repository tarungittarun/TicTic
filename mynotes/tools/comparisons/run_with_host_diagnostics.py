"""Run one comparison command and collect public CI host diagnostics in finally.

The emulator action executes each input line in a separate shell. Keep exception
handling and diagnostics in this process, not a multiline shell/heredoc input.
No process arguments, environment, credentials, private library or host home files.
"""
import json
from pathlib import Path
import runpy
import subprocess
import traceback


def collect_host(output):
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    from emulator_crash_metadata import collect
    data = {'scope': 'Public CI host diagnostics; not app fault attribution', 'proc': {},
            'emulator_crash_metadata': collect()}
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
    try:
        runpy.run_path(str(Path(__file__).with_name('paired_modules37.py')), run_name='__main__')
        return 0
    except SystemExit as error:
        return error.code if isinstance(error.code, int) else (0 if error.code is None else 1)
    except Exception:
        traceback.print_exc()
        return 1
    finally:
        collect_host(Path('comparison37-evidence'))


if __name__ == '__main__':
    raise SystemExit(main())

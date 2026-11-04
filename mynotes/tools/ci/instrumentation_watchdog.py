"""Fail, never pass, an instrumentation command that stops reporting progress.

Only for the disposable CI emulator. No device reset, root, or private-data access.
"""
import os
import re
from pathlib import Path
import signal
import subprocess
import sys
import time



class Progress:
    """Print test identities/status only, never transcripts, screenshots or stacks."""
    def __init__(self, output):
        self.output=Path(output);self.offset=0;self.pending=b'';self.identity={}

    def emit(self):
        with self.output.open('rb') as source:
            source.seek(self.offset);chunk=source.read(65536);self.offset+=len(chunk)
        parts=(self.pending+chunk).split(b'\n');self.pending=parts.pop()[-8192:]
        for raw in parts:
            line=raw.decode('ascii',errors='replace').rstrip('\r')
            match=re.fullmatch(r'INSTRUMENTATION_STATUS: (class|test)=([A-Za-z0-9_.$]+)',line)
            if match:self.identity[match[1]]=match[2]
            code=re.fullmatch(r'INSTRUMENTATION_STATUS_CODE: (-?\d+)',line)
            if code and self.identity.get('class','').startswith('app.space.notes.') and 'test' in self.identity:
                print('Native progress: '+self.identity['class']+'#'+self.identity['test']+' status='+code[1],flush=True)
                self.identity={}


def run(command, output, *, stall_seconds=240, poll_seconds=1):
    output = Path(output)
    with output.open('wb') as stream:
        process = subprocess.Popen(command, stdout=stream, start_new_session=True)
        progress=Progress(output)
        stamp = output.stat().st_mtime_ns
        changed = time.monotonic()
        try:
            while True:
                try:
                    code = process.wait(timeout=poll_seconds)
                    progress.emit()
                    return code if code >= 0 else 128 - code
                except subprocess.TimeoutExpired:
                    progress.emit()
                    latest = output.stat().st_mtime_ns
                    if latest != stamp:
                        stamp, changed = latest, time.monotonic()
                    if time.monotonic() - changed >= stall_seconds:
                        output.with_name('refinement-native-watchdog.txt').write_text(
                            f'FAIL: no instrumentation output for {stall_seconds} seconds. '
                            'Command terminated; all expected assertions still required.\n', encoding='utf-8')
                        return 124
        finally:
            if process.poll() is None:
                # Includes timeout's adb child; never leave instrumentation clients orphaned.
                try:
                    os.killpg(process.pid, signal.SIGTERM)
                except ProcessLookupError:
                    pass
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    try:
                        os.killpg(process.pid, signal.SIGKILL)
                    except ProcessLookupError:
                        pass
                    process.wait(timeout=5)


if __name__ == '__main__':
    sys.exit(run(sys.argv[1:], os.environ.get('NOTES_NATIVE_OUTPUT','refinement-native.txt')))

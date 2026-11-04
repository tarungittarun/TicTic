"""Verify the observed original host stall BEFORE any native launcher ran."""
import hashlib
import json

FILES={'refinement-environment-configured.json','refinement-environment-configured.txt'}
COMMAND='adb shell svc power stayon true'
PREFIX='native (light-landscape, no, landscape)\tRun ReactiveCircus/android-emulator-runner@v2\t'

def verify(log, files):
    if set(files)!=FILES:
        raise ValueError('Original artifact must contain only the two pre-native environment files')
    commands=[line.split('[command]/usr/bin/sh -c ',1)[1].strip() for line in log.splitlines() if line.startswith(PREFIX) and '[command]/usr/bin/sh -c ' in line]
    if not commands or commands[-1]!=COMMAND or any('run_ordered_modules' in c for c in commands):
        raise ValueError('Original timeout must precede the native launcher at the observed host command')
    if any('MODULE ' in line and ' BEGIN ' in line for line in log.splitlines()):
        raise ValueError('Native module started; cannot replace asserted or unknown native outcomes')
    if '##[error]The operation was canceled.' not in log or 'With the provided path, there will be 2 files uploaded' not in log:
        raise ValueError('Actual cancelled job and two-file original upload required')
    environment=json.loads(files['refinement-environment-configured.json'])
    if environment.get('stage')!='configured' or '<no ANR has occurred since boot>' not in environment.get('anr_header',''):
        raise ValueError('Original configured guest/absence of app ANR is not established')
    return {'kind':'pre_native_host_command_timeout','last_command':COMMAND,'native_launcher_started':False,'tests_executed':0,'log_sha256':hashlib.sha256(log.encode()).hexdigest(),'original_artifact_files':{name:hashlib.sha256(raw).hexdigest() for name,raw in sorted(files.items())}}

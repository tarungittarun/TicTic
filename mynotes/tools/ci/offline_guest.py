"""Prove a fresh disposable emulator offline before installing any Notes APK.

API26 svc's Java process can die during bootstrap (retained original exit137).
Use the protected airplane-mode broadcast under temporary emulator root instead;
never ignore command failure, touch app data or dismiss app error dialogs.
"""
import json
from pathlib import Path
import re
import subprocess
import time
from mynotes.tools.ci.adb_state import wait_uid


def adb(*args):
    return subprocess.check_output(['adb',*args],text=True,stderr=subprocess.STDOUT,timeout=30)


def network_off(wifi,connectivity):
    return 'Wi-Fi is disabled' in wifi and bool(re.search(r'Active default network:\s*none\b',connectivity,re.I))


def configure(output=Path('offline-guest-proof.json')):
    if adb('shell','getprop','ro.kernel.qemu').strip()!='1':
        raise RuntimeError('Offline provisioning is restricted to disposable emulator guests')
    for package in ('app.space.notes','app.space.notes.debug'):
        if adb('shell','pm','list','packages',package).strip():
            raise RuntimeError('Bootstrap must precede all Notes installation; never modify a populated device')
    rooted=False
    try:
        try:adb('root')
        except subprocess.CalledProcessError as error:
            # API26 may close this transport while adbd actually restarts as root.
            # Accept only that exact transition, then independently require UID0.
            if 'unable to connect for root: closed' not in (error.output or ''):raise
        rooted=True;wait_uid(0)
        adb('shell','settings','put','global','airplane_mode_on','1')
        adb('shell','am','broadcast','-a','android.intent.action.AIRPLANE_MODE','--ez','state','true')
        stable=0
        for _ in range(60):
            flag=adb('shell','settings','get','global','airplane_mode_on').strip()
            wifi=adb('shell','dumpsys','wifi');connectivity=adb('shell','dumpsys','connectivity')
            stable=stable+1 if flag=='1' and network_off(wifi,connectivity) else 0
            if stable>=3:
                proof={'emulator_only':True,'notes_installed':False,'airplane_mode':True,'wifi_disabled':True,'active_default_network':False,'consecutive_checks':stable,'uninstall_or_clear_data':False,'scope':'Fresh public CI guest offline provisioning; not a physical-device result.'}
                break
            time.sleep(.5)
        else:raise RuntimeError('Actual Wi-Fi/default-network state never proved offline; do not run app fixtures')
    finally:
        if rooted:adb('unroot');wait_uid(2000)
    proof['final_shell_uid']=2000
    output.write_text(json.dumps(proof,indent=2)+'\n')
    print('::notice title=Verified offline guest::'+json.dumps(proof))
    return proof


if __name__=='__main__':configure()

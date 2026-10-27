"""Release gate: stable ID/certificate and strictly increasing Android versionCode.
Uses pinned Android SDK tools in CI; never handles signing secrets.
"""
import argparse
from pathlib import Path
import re
import subprocess


def inspect(apk, sdk):
    tools=Path(sdk)/'build-tools/34.0.0'
    badging=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(apk)],text=True)
    match=re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",badging)
    if not match:
        raise ValueError('Cannot read APK identity')
    certs=subprocess.check_output([str(tools/'apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True)
    signatures=re.findall(r'Signer #\d+ certificate SHA-256 digest: (\w+)',certs)
    if len(signatures)!=1:
        raise ValueError('Expected exactly one signing certificate')
    return match[1],int(match[2]),match[3],signatures[0]


def verify(current, previous):
    if current[0]!='app.space.notes':
        raise ValueError('Release application ID changed')
    for old in previous:
        if old[0]!=current[0] or old[3]!=current[3]:
            raise ValueError('Update would conflict: application ID or signing certificate changed')
        if current[1]<=old[1]:
            raise ValueError('versionCode must increase beyond every published build')


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--sdk',required=True);p.add_argument('--apk',required=True)
    p.add_argument('--releases',required=True);args=p.parse_args()
    current=inspect(args.apk,args.sdk)
    verify(current,[inspect(old,args.sdk) for old in Path(args.releases).glob('MyNotes-*.apk')])
    print('Update identity/certificate/version gate PASS:',current[0],current[1],current[2])

"""Same-signer test-only probe on fresh public CI guests. Never clear app data."""
import argparse
import json
from pathlib import Path
import re
from mynotes.tools.ci.release_update_smoke import Device
from mynotes.tools.ci.results import android

METHOD='app.space.notes.ReleaseRichMigrationProbe#publicRichLibraryRetainsItsOldWriterSnapshot'
TOKEN='public-rich-release64-to66-v1'


def parse_proof(output,phase,version):
    execution=android(output,[METHOD])
    matches=re.findall(r'PUBLIC_RICH_UPGRADE_PROOF=(\{[^\n]*\})',output)
    if not execution['ok'] or len(matches)!=1:raise ValueError('Probe did not execute exactly once successfully')
    proof=json.loads(matches[0])
    if proof['phase']!=phase or proof['version_code']!=version or proof['notes']!=4:raise ValueError('Unexpected probe identity')
    if proof['payload_unchanged'] is not True or proof['private_folder_retained'] is not True:raise ValueError('Rich payload or folder was lost')
    if not re.fullmatch('[0-9a-f]{64}',proof['old_snapshot_sha256']):raise ValueError('Missing old-writer snapshot identity')
    if proof['physical_acceptance'] is not False or proof['ui_rendering_acceptance'] is not False:raise ValueError('Invalid acceptance claim')
    if proof['candidate_write_readback'] is not (phase=='after'):raise ValueError('Candidate write/readback missing or mislabelled')
    return proof


def verify(old,candidate,tester,output):
    device=Device();report={'ok':False,'phases':[],'uninstall_or_clear_data':False,'physical_acceptance':False,'ui_rendering_acceptance':False,'release_approval':False,'scope':'Public generated rich repository payloads only; not real microphone files, SAF, rich UI rendering or an exhaustive user library.'}
    try:
        if device.adb('shell','getprop','ro.kernel.qemu').strip()!='1':raise ValueError('Disposable emulator required')
        if device.adb('shell','pm','list','packages','app.space.notes').strip():raise ValueError('Never seed a populated guest')
        device.adb('install',old);before=device.state();report['from']=before
        if before['version_code']!=64:raise ValueError('Expected exact old release64')
        device.adb('install','-t',tester)
        for phase,version in (('create',64),('before',64),('after',66)):
            device.adb('shell','am','force-stop','app.space.notes')
            if phase=='after':
                device.adb('install','-r',candidate);after=device.state();report['to']=after
                if after['version_code']!=66 or after['first_install_time']!=before['first_install_time']:raise ValueError('Installation identity changed')
            result=device.adb('shell','am','instrument','-w','-r','-e','timeout_msec','120000','-e','explicitPublicFixtureOnly',TOKEN,'-e','richPhase',phase,'-e','expectedReleaseCode',version,'-e','class',METHOD,'app.space.notes.test/androidx.test.runner.AndroidJUnitRunner')
            Path('rich-probe-'+phase+'.txt').write_text(result)
            report['phases'].append(parse_proof(result,phase,version))
        if len({r['old_snapshot_sha256'] for r in report['phases']})!=1:raise ValueError('Old checkpoint identity changed across phases')
        report['api']=int(device.adb('shell','getprop','ro.build.version.sdk').strip());report['ok']=True
    except Exception as error:
        report['error']=str(error);raise
    finally:
        Path(output).write_text(json.dumps(report,indent=2)+'\n')
        print('::'+('notice' if report['ok'] else 'error')+' title=Public rich signed-update probe::'+json.dumps(report))
    return report


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('old','candidate','tester','output'):parser.add_argument('--'+name,required=True)
    args=parser.parse_args();verify(args.old,args.candidate,args.tester,args.output)

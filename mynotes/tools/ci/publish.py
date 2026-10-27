"""Publish exactly accepted bytes. Large APK assets are kept out of the source Git history."""
import json
from pathlib import Path
import shutil
import subprocess
from mynotes.tools.ci.audit import REPO,NOTES,sha
from mynotes.tools.ci.published import validate_receipt,BASE


def run(*args):subprocess.run(args,cwd=REPO,check=True)


def publish(directory):
    directory=Path(directory);audit=json.loads((directory/'audit.json').read_text());name=audit['filename'];apk=directory/name
    if not audit['publish_eligible'] or sha(apk)!=audit['accepted_sha256']:raise ValueError('Unapproved or changed publication bytes')
    destination=NOTES/'releases'
    if (destination/name).exists() or (destination/(name+'.release.json')).exists():raise ValueError('Published version already exists; never overwrite')
    # Sidecars are small immutable provenance documents, regardless of the APK storage channel.
    for suffix in ['.sha256','.source.txt','.audit.json']:
        source=directory/(name+suffix)
        if not source.exists():
            if suffix=='.source.txt':source.write_text(audit['source']+'\n')
            elif suffix=='.audit.json':source.write_text(json.dumps(audit,indent=2)+'\n')
            else:raise ValueError('Missing accepted checksum')
    if apk.stat().st_size<=8*1024*1024:
        shutil.copyfile(apk,destination/name)
    else:
        tag=f"mynotes-v{audit['versionName']}-{audit['versionCode']}"
        receipt={'schema':1,'storage':'github-release','filename':name,'url':BASE+tag+'/'+name,
                 'applicationId':audit['applicationId'],'versionCode':audit['versionCode'],'versionName':audit['versionName'],
                 'certificate':audit['certificate'],'source':audit['source'],'sha256':audit['accepted_sha256'],'bytes':apk.stat().st_size}
        validate_receipt(receipt)
        # Upload while draft, then expose all assets atomically as a release. No --clobber/replacement.
        run('gh','release','create',tag,str(apk),*[str(directory/(name+s)) for s in ['.sha256','.source.txt','.audit.json']],
            '--repo','tarungittarun/TicTic','--target',audit['source'],'--draft','--title',f"MyNotes {audit['versionName']} ({audit['versionCode']})",
            '--notes',f"Offline standalone MyNotes. Exact audited/emulator-verified APK. SHA-256: {audit['accepted_sha256']}. Install over the existing release; do not uninstall or clear data. Source: {audit['source']}. Scope/limits: mynotes/docs/STANDALONE-TEST-GUIDE.md.")
        run('gh','release','edit',tag,'--repo','tarungittarun/TicTic','--draft=false')
        (destination/(name+'.release.json')).write_text(json.dumps(receipt,indent=2)+'\n')
    for suffix in ['.sha256','.source.txt','.audit.json']:shutil.copyfile(directory/(name+suffix),destination/(name+suffix))
    print('Immutable publication:',name,'Git APK' if (destination/name).exists() else 'GitHub Release asset')


if __name__=='__main__':
    import argparse
    parser=argparse.ArgumentParser();parser.add_argument('--directory',default='accepted-release');args=parser.parse_args();publish(args.directory)

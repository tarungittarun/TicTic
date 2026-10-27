"""Audit candidate APK; select exact immutable published bytes for CI-only revalidation."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
from mynotes.tools.verify_update import inspect,verify

REPO=Path(__file__).resolve().parents[3]
NOTES=REPO/'mynotes'
APP_PATHS=['mynotes/app/src/main','mynotes/runtime/src/main','mynotes/domain/src/main',
           'mynotes/app/build.gradle','mynotes/runtime/build.gradle','mynotes/domain/build.gradle',
           'mynotes/build.gradle','mynotes/settings.gradle','mynotes/gradle.properties',
           'mynotes/gradle','mynotes/gradlew','mynotes/gradlew.bat','mynotes/signing']


def release_input(path):
    if not path.startswith('mynotes/'):return False
    relative=path[len('mynotes/'):]
    if relative.startswith('tools/models/'):return True
    if any(relative.startswith(prefix) for prefix in ['qa/','docs/','tools/','releases/']):return False
    if '/src/test/' in path or '/src/androidTest/' in path:return False
    if '/' not in relative and relative.endswith('.md'):return False
    return True


def assert_source_equivalent(source,target='HEAD'):
    changed=subprocess.check_output(['git','diff','--name-only','-z',source,target,'--','mynotes/'],cwd=REPO,text=True).split('\0')
    protected=[path for path in changed if release_input(path)]
    if protected:raise ValueError('App/build inputs changed without a versionCode increase: '+', '.join(protected))


def publication_policy(current,previous):
    # Same-version CI is allowed only for revalidation, never replacement publication.
    if current[0]!='app.space.notes':raise ValueError('Release package ID changed')
    same=[]
    for old in previous:
        if old[0]!=current[0] or old[3]!=current[3]:raise ValueError('Certificate/package changed')
        if old[1]>current[1]:raise ValueError('Release versionCode regressed')
        if old[1]==current[1]:
            if old[2]!=current[2]:raise ValueError('Reused versionCode under another versionName')
            same.append(old)
    if len(same)>1:raise ValueError('Multiple published APKs use the same versionCode')
    if not same:verify(current,previous)
    return not same


def assert_manifest(badging,xml):
    permissions=re.findall(r"uses-permission(?:-sdk-\d+)?: name='([^']+)'",badging)
    # Expand explicitly with optional-permission tests when media/reminder scope lands.
    allowed={'android.permission.RECORD_AUDIO'}
    if set(permissions)-allowed:raise ValueError('Unreviewed APK permissions: '+str(permissions))
    if 'application-debuggable' in badging:raise ValueError('Release APK is debuggable')
    if not re.search(r'android:allowBackup[^\n]*\(type 0x12\)0x0\b',xml):
        raise ValueError('Release must explicitly disable Android backup')
    sdk=re.search(r"sdkVersion:'(\d+)'",badging)
    if not sdk or int(sdk[1])!=26:raise ValueError('minSdk contract changed')
    return permissions


def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--sdk',required=True);p.add_argument('--apk',required=True);p.add_argument('--output',required=True);args=p.parse_args()
    candidate=Path(args.apk);current=inspect(candidate,args.sdk)
    published=sorted((NOTES/'releases').glob('MyNotes-*.apk'))
    identities={path:inspect(path,args.sdk) for path in published}
    eligible=publication_policy(current,list(identities.values()))
    accepted=candidate;accepted_source=None
    if not eligible:
        accepted=next(path for path,identity in identities.items() if identity[1]==current[1])
        source_file=Path(str(accepted)+'.source.txt')
        if not source_file.exists():source_file=accepted.with_suffix('.source.txt')
        source=source_file.read_text().strip()
        if not re.fullmatch('[0-9a-f]{40}',source):raise ValueError('Invalid published source identity')
        assert_source_equivalent(source)
        accepted_source=source
        checksum=Path(str(accepted)+'.sha256').read_text().split()[0]
        if sha(accepted)!=checksum:raise ValueError('Published APK checksum mismatch')
    tools=Path(args.sdk)/'build-tools/34.0.0'
    permissions=None
    for apk in {candidate,accepted}:
        badging=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(apk)],text=True)
        xml=subprocess.check_output([str(tools/'aapt'),'dump','xmltree',str(apk),'AndroidManifest.xml'],text=True)
        permissions=assert_manifest(badging,xml)
    output=Path(args.output);output.mkdir(parents=True,exist_ok=True)
    name=f'MyNotes-v{current[2]}-{current[1]}.apk'
    shutil.copyfile(accepted,output/name)
    digest=sha(output/name)
    (output/(name+'.sha256')).write_text(digest+'  '+name+'\n')
    source_sha=subprocess.check_output(['git','rev-parse','HEAD'],cwd=REPO,text=True).strip()
    metadata={'schema':1,'source':source_sha,'accepted_source':accepted_source or source_sha,'applicationId':current[0],'versionCode':current[1],
              'versionName':current[2],'certificate':current[3],'permissions':permissions,
              'candidate_sha256':sha(candidate),'accepted_sha256':digest,
              'accepted_from':'new candidate' if eligible else 'immutable published APK',
              'publish_eligible':eligible,'filename':name}
    (output/'audit.json').write_text(json.dumps(metadata,indent=2)+'\n')
    print(json.dumps(metadata,indent=2))

"""Independent immutable-publication, isolation and dependency hygiene report. No SDK needed."""
import hashlib
import json
from pathlib import Path
import re
from mynotes.tools.ci.audit import REPO,NOTES
from mynotes.tools.ci.published import validate_receipt


def inventory(root=NOTES):
    releases=[]
    for apk in sorted((root/'releases').glob('*.apk')):
        digest=hashlib.sha256(apk.read_bytes()).hexdigest();checksum=Path(str(apk)+'.sha256').read_text().split()[0]
        if digest!=checksum:raise ValueError('Published checksum mismatch: '+apk.name)
        source=Path(str(apk)+'.source.txt')
        if not source.exists():source=apk.with_suffix('.source.txt')
        if not re.fullmatch('[0-9a-f]{40}',source.read_text().strip()):raise ValueError('Invalid source provenance: '+apk.name)
        releases.append({'file':apk.name,'bytes':apk.stat().st_size,'sha256':digest,'source':source.read_text().strip()})
    names={r['file'] for r in releases}
    for path in sorted((root/'releases').glob('*.release.json')):
        receipt=validate_receipt(json.loads(path.read_text()))
        if receipt['filename'] in names:raise ValueError('Duplicate local/remote publication')
        names.add(receipt['filename']);releases.append({'file':receipt['filename'],'storage':'github-release','bytes':receipt['bytes'],'sha256':receipt['sha256'],'source':receipt['source'],'url':receipt['url'],'verification':'receipt schema here; downloaded bytes and APK signature in APK audit job'})
    if not releases:raise ValueError('No prior install-over baseline available')
    dependencies=[]
    for script in root.rglob('*.gradle'):
        text=script.read_text()
        if re.search(r'SNAPSHOT|[\w.]\+\b|jcenter\(|mavenLocal\(',text):raise ValueError('Unpinned/unreviewed dependency repository in '+str(script))
        dependencies.extend(re.findall(r"(?:implementation|androidTestImplementation|testImplementation)\s+'([^']+)'",text))
    if any(':' not in coordinate or coordinate.endswith(':+') for coordinate in dependencies):raise ValueError('Invalid dependency pin')
    return {'releases':releases,'pinned_direct_dependencies':sorted(dependencies),'scope':'Direct Gradle dependency pins and published APK bytes; not a transitive CVE or license audit'}


if __name__=='__main__':
    result=inventory();out=REPO/'evidence';out.mkdir(exist_ok=True)
    (out/'inventory.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))

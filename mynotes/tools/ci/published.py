"""Immutable large APKs live in GitHub Release assets; small legacy APKs remain in Git."""
import hashlib
import json
from pathlib import Path
import re
import urllib.request

BASE='https://github.com/tarungittarun/TicTic/releases/download/'


def digest(path):
    sha=hashlib.sha256()
    with Path(path).open('rb') as f:
        while block:=f.read(1024*1024):sha.update(block)
    return sha.hexdigest()


def validate_receipt(receipt):
    if receipt.get('schema')!=1 or receipt.get('storage')!='github-release':raise ValueError('Unknown publication receipt')
    version=receipt['versionName'];code=receipt['versionCode']
    if not re.fullmatch(r'\d+\.\d+\.\d+',version) or not isinstance(code,int) or code<=0:raise ValueError('Invalid published version')
    name=f'MyNotes-v{version}-{code}.apk';tag=f'mynotes-v{version}-{code}'
    if receipt['filename']!=name or receipt['url']!=BASE+tag+'/'+name:raise ValueError('Unreviewed APK origin/filename')
    for key,length in [('sha256',64),('source',40),('certificate',64)]:
        if not re.fullmatch('[0-9a-f]{'+str(length)+'}',receipt[key]):raise ValueError('Invalid '+key+' in publication receipt')
    if receipt['applicationId']!='app.space.notes' or not isinstance(receipt['bytes'],int) or receipt['bytes']<=0:raise ValueError('Invalid publication identity/size')
    return receipt


def resolve(directory,cache=None):
    directory=Path(directory);cache=Path(cache or Path.home()/'.cache/mynotes-published');cache.mkdir(parents=True,exist_ok=True)
    result=sorted(directory.glob('MyNotes-*.apk'))
    names={p.name for p in result}
    for path in sorted(directory.glob('*.release.json')):
        receipt=validate_receipt(json.loads(path.read_text()))
        if receipt['filename'] in names:raise ValueError('Duplicate local/remote published APK')
        names.add(receipt['filename']);target=cache/(receipt['sha256']+'-'+receipt['filename'])
        if not target.is_file() or target.stat().st_size!=receipt['bytes'] or digest(target)!=receipt['sha256']:
            temporary=target.with_suffix('.downloading')
            try:
                with urllib.request.urlopen(receipt['url'],timeout=120) as source,temporary.open('wb') as out:
                    count=0
                    while block:=source.read(1024*1024):
                        count+=len(block)
                        if count>receipt['bytes']:raise ValueError('Published asset exceeded pinned size')
                        out.write(block)
                if temporary.stat().st_size!=receipt['bytes'] or digest(temporary)!=receipt['sha256']:raise ValueError('Published APK asset checksum mismatch')
                temporary.replace(target)
            finally:temporary.unlink(missing_ok=True)
        Path(str(target)+'.sha256').write_text(receipt['sha256']+'  '+target.name+'\n')
        Path(str(target)+'.source.txt').write_text(receipt['source']+'\n')
        Path(str(target)+'.release.json').write_text(json.dumps(receipt,indent=2)+'\n')
        result.append(target)
    return result

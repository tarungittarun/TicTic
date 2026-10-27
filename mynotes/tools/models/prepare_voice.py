"""Build-time only speech model acquisition. App never downloads anything."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import tempfile
import urllib.request
import zipfile


def digest(path):
    result=hashlib.sha256()
    with Path(path).open('rb') as f:
        while block:=f.read(1024*1024):result.update(block)
    return result.hexdigest()


def prepare(manifest,output,cache):
    receipt=json.loads(Path(manifest).read_text());cache=Path(cache);cache.mkdir(parents=True,exist_ok=True)
    archive=cache/(receipt['sha256']+'.zip')
    if not archive.is_file() or digest(archive)!=receipt['sha256']:
        temporary=None
        try:
            with tempfile.NamedTemporaryFile(dir=cache,delete=False) as f:
                temporary=Path(f.name)
                with urllib.request.urlopen(receipt['url'],timeout=120) as source:
                    total=0
                    while block:=source.read(1024*1024):
                        total+=len(block)
                        if total>receipt['bytes']:raise ValueError('Model download exceeded pinned size')
                        f.write(block)
            if temporary.stat().st_size!=receipt['bytes'] or digest(temporary)!=receipt['sha256']:raise ValueError('Model checksum/size mismatch; refusing build')
            os.replace(temporary,archive);temporary=None
        finally:
            if temporary is not None:temporary.unlink(missing_ok=True)
    with zipfile.ZipFile(archive) as z:
        actual={i.filename for i in z.infolist() if not i.is_dir()}
        if actual!=set(receipt['files']):raise ValueError('Model member set drift')
        for name,expected in receipt['files'].items():
            info=z.getinfo(name)
            if info.file_size!=expected['bytes'] or hashlib.sha256(z.read(info)).hexdigest()!=expected['sha256']:raise ValueError('Model member hash drift: '+name)
    output=Path(output);output.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(archive,output)
    print('Bundled speech model verified:',receipt['sha256'],receipt['bytes'],'bytes; build-time download only')


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--manifest',required=True);p.add_argument('--output',required=True);p.add_argument('--cache',default=str(Path.home()/'.cache/mynotes-models'));a=p.parse_args();prepare(a.manifest,a.output,a.cache)

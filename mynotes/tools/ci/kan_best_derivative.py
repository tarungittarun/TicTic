"""Nonshipping single-variable Kannada weight study. Never modifies a user APK.
Only the fixed debug foundation is accepted by the workflow; model bytes are pinned.
"""
import argparse
import base64
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile
from mynotes.tools.ci.resign_public_benchmark_apks import payload

MODEL = 'assets/ocr/kan.traineddata'
MANIFEST = 'assets/ocr/models.json'
NOTICE = 'assets/licenses/nonshipping-kan-best-LICENSE.txt'
FAST = 'bd31e6b6ae93271e3bcf5383d306d8eefbb91542937cd6d735a5930c970e61d8'
BEST = ('8f2e98e1d36ee516a1638522e569d2c4d11ac86d', 10233763)
LICENSE = ('d9a10c0d8e868ebf8da0b3dc95bb0be634c34bfe', 10173)
REV = 'e12c65a915945e4c28e237a9b52bc4a8f39a0cec'


def checked_blob(data, identity):
    blob, size = identity
    if len(data) != size or hashlib.sha1(b'blob '+str(size).encode()+b'\0'+data).hexdigest() != blob:
        raise ValueError('Unrecognized pinned public model/license blob')


def fetch(identity):
    blob, _ = identity
    obj = json.loads(subprocess.check_output(['gh','api',f'repos/tesseract-ocr/tessdata_best/git/blobs/{blob}'], timeout=90))
    data = base64.b64decode(obj['content'], validate=False)
    checked_blob(data, identity)
    return data


def derive(app, output, model, notice):
    if app.resolve() == output.resolve():
        raise ValueError('Never overwrite the source APK')
    checked_blob(model, BEST)
    checked_blob(notice, LICENSE)
    before = payload(app)
    if NOTICE in before:
        raise ValueError('Unexpected existing research notice')
    with zipfile.ZipFile(app) as source:
        if hashlib.sha256(source.read(MODEL)).hexdigest() != FAST:
            raise ValueError('Wrong original Kannada weights')
        manifest = json.loads(source.read(MANIFEST))
        entry = manifest['files']['kan.traineddata']
        if entry['sha256'] != FAST:
            raise ValueError('Wrong original Kannada manifest')
        entry.update(sha256=hashlib.sha256(model).hexdigest(), bytes=len(model), git_blob=BEST[0],
                     repository='https://github.com/tesseract-ocr/tessdata_best', commit=REV)
        manifest['research_scope'] = 'NONSHIPPING: eng/hin unchanged fast defaults; kan uses its explicit best repository/commit override. Not user adoption.'
        with zipfile.ZipFile(output, 'w') as target:
            for item in source.infolist():
                data = model if item.filename == MODEL else (json.dumps(manifest, indent=2)+'\n').encode() if item.filename == MANIFEST else source.read(item)
                target.writestr(item, data)
            target.writestr(NOTICE, notice, compress_type=zipfile.ZIP_DEFLATED)
    after = payload(output)
    changed = {name for name in before if before[name] != after.get(name)}
    if changed != {MODEL, MANIFEST} or set(after)-set(before) != {NOTICE}:
        raise ValueError('Unexpected code/resource modification')
    return {'adopted': False, 'scope': 'Nonshipping Kannada best versus existing fast weights; same compiled Java/native/test payload, no training. More storage/CPU is possible; not a lean-user-APK claim.',
            'source_apk_sha256': hashlib.sha256(app.read_bytes()).hexdigest(),
            'changed_payload_entries': sorted(changed), 'added_notice': NOTICE,
            'best_model': {'git_blob': BEST[0], 'bytes': len(model), 'sha256': hashlib.sha256(model).hexdigest(), 'repository_revision': REV},
            'unsigned_derivative_bytes': output.stat().st_size,
            'user_apk_changed': False, 'physical_acceptance': False}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--app', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    args = p.parse_args()
    proof = derive(args.app, args.output, fetch(BEST), fetch(LICENSE))
    args.output.with_suffix('.json').write_text(json.dumps(proof, indent=2)+'\n')
    print('::notice title=Nonshipping Kannada weight identity::'+json.dumps(proof))


if __name__ == '__main__':
    main()

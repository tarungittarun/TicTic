"""Nonshipping debug-only model derivative; byte-prove all other APK payloads unchanged.
No Java/native rebuild, release signing, engine selection, training or adoption.
"""
import argparse
import hashlib
import json
import zipfile
from pathlib import Path
from mynotes.tools.ci.resign_public_benchmark_apks import payload

MODEL = 'assets/voice/trained-whisper.bin'
MANIFEST = 'assets/voice/trained-whisper.json'
IDENTITIES = {
    'f16': (77704698, '1ab22fa9b830d4edc9481a9cb3af1f4d8cfe7fc71a300f19384dc9b39d405b45'),
    'q8_0': (43550778, '65fb6de21bcb73d3ff8f86ea5d8a3da65a25df913f731af60e5b7b952c426d3c'),
}


def checked(data, kind):
    size, digest = IDENTITIES[kind]
    if len(data) != size or hashlib.sha256(data).hexdigest() != digest:
        raise ValueError('Unrecognized '+kind+' model bytes')


def extract(app, output):
    with zipfile.ZipFile(app) as archive:
        data = archive.read(MODEL)
    checked(data, 'f16')
    output.write_bytes(data)


def derive(app, model, output):
    if app.resolve() == output.resolve():
        raise ValueError('Never overwrite the source APK')
    original = payload(app)  # Also rejects duplicate entries.
    compact = model.read_bytes()
    checked(compact, 'q8_0')
    with zipfile.ZipFile(app) as source:
        checked(source.read(MODEL), 'f16')
        manifest = json.loads(source.read(MANIFEST))
        # Preserve the actual training provenance; change only install integrity metadata.
        if manifest.get('sha256') != IDENTITIES['f16'][1] or manifest.get('bytes') != IDENTITIES['f16'][0]:
            raise ValueError('Unexpected source model manifest')
        manifest['sha256'] = IDENTITIES['q8_0'][1]
        manifest['bytes'] = IDENTITIES['q8_0'][0]
        with zipfile.ZipFile(output, 'w') as target:
            for entry in source.infolist():
                data = compact if entry.filename == MODEL else (json.dumps(manifest, indent=2)+'\n').encode() if entry.filename == MANIFEST else source.read(entry)
                target.writestr(entry, data)
    after = payload(output)
    changed = {name for name in original if after.get(name) != original[name]}
    if original.keys() != after.keys() or changed != {MODEL, MANIFEST}:
        raise ValueError('Unexpected non-model payload modification')
    return {'scope': 'Debug-only Android compatibility smoke, not accuracy/latency/memory qualification or adoption.',
            'adopted': False, 'changed_payload_entries': sorted(changed),
            'source_apk_sha256': hashlib.sha256(app.read_bytes()).hexdigest(),
            'source_apk_bytes': app.stat().st_size, 'unsigned_derivative_bytes': output.stat().st_size,
            'model_identity': {'bytes': len(compact), 'sha256': hashlib.sha256(compact).hexdigest()},
            'unchanged_entries': len(original)-2,
            'remaining': ['Independent Android accuracy corpus', 'Paired decode latency and peak memory',
                          'Old installed model cache cleanup/migration', 'Physical device acceptance']}


def main():
    p = argparse.ArgumentParser()
    p.add_argument('operation', choices=['extract', 'derive'])
    p.add_argument('--app', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--model', type=Path)
    args = p.parse_args()
    if args.operation == 'extract':
        extract(args.app, args.output)
    else:
        if args.model is None:
            p.error('derive requires --model')
        proof = derive(args.app, args.model, args.output)
        args.output.with_suffix('.json').write_text(json.dumps(proof, indent=2)+'\n')


if __name__ == '__main__':
    main()

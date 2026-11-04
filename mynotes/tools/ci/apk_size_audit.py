"""Read-only APK byte accounting; no extraction and no runtime dependency."""
import argparse
import json
import zipfile
from collections import defaultdict
from pathlib import Path


def category(name):
    if name.startswith('lib/'):
        return 'native/' + name.split('/')[1]
    if name.startswith('assets/voice/'):
        return 'models/speech'
    if name.startswith('assets/ocr/'):
        return 'models/ocr'
    if name.startswith('assets/'):
        return 'assets/other'
    if name.endswith('.dex'):
        return 'code/dex'
    if name.startswith('res/'):
        return 'resources'
    return 'other'


def audit(path):
    groups = defaultdict(lambda: dict(entries=0, compressed_bytes=0, uncompressed_bytes=0))
    largest = []
    with zipfile.ZipFile(path) as archive:
        names = set()
        for info in archive.infolist():
            if info.filename in names:
                raise ValueError('Duplicate APK entry')
            names.add(info.filename)
            if info.is_dir():
                continue
            group = groups[category(info.filename)]
            group['entries'] += 1
            group['compressed_bytes'] += info.compress_size
            group['uncompressed_bytes'] += info.file_size
            largest.append(dict(path=info.filename, compressed_bytes=info.compress_size, uncompressed_bytes=info.file_size))
    size = path.stat().st_size
    packed = sum(row['compressed_bytes'] for row in groups.values())
    if packed > size:
        raise ValueError('Invalid APK byte accounting')
    return dict(apk_bytes=size, zip_headers_signing_and_other_bytes=size-packed,
                categories=dict(sorted(groups.items())),
                largest_entries=sorted(largest,key=lambda row:row['compressed_bytes'],reverse=True)[:25],
                scope='ZIP byte accounting only. Not installed footprint, speed or accuracy evidence.')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('apk',type=Path)
    p.add_argument('output',type=Path)
    args = p.parse_args()
    result = audit(args.apk)
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2))


if __name__ == '__main__':
    main()

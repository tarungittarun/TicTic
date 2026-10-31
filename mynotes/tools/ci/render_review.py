"""Bounded synthetic fixture previews for visual review when artifact URLs fail.

Original PNGs remain in the normal artifact. JPEGs are review previews, not
pixel-exact assertions. Never point this tool at real-user screenshots.
GitHub workflow annotations truncate at 4 KiB, so image payloads are files.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import re
import tarfile

MAX_IMAGE_BYTES = 8 * 1024 * 1024
MAX_IMAGES = 80


def fixtures(archive):
    """Read bounded regular PNG members, without extracting paths or following links."""
    count = 0
    for member in archive:
        path = PurePosixPath(member.name)
        if not member.isfile() or path.suffix != '.png':
            continue
        if len(path.parts) != 2 or path.parts[0] != 'ux-review' or not re.fullmatch(r'[a-z0-9-]{1,60}', path.stem):
            raise ValueError('Unexpected fixture screenshot path')
        if not 0 < member.size <= MAX_IMAGE_BYTES:
            raise ValueError('Fixture screenshot exceeds size bound')
        count += 1
        if count > MAX_IMAGES:
            raise ValueError('Fixture screenshot count exceeds bound')
        with archive.extractfile(member) as stream:
            data = stream.read(MAX_IMAGE_BYTES + 1)
        if len(data) != member.size:
            raise ValueError('Truncated fixture screenshot')
        yield path.stem, data
    if not count:
        raise ValueError('No rendered fixture screenshots')


def preview_record(name, preview, width, height):
    if len(preview) > 44000:
        raise ValueError('Review preview exceeds bound')
    return {'name': name, 'mime': 'image/jpeg', 'width': width, 'height': height,
            'bytes': len(preview), 'sha256': hashlib.sha256(preview).hexdigest()}


def main():
    from PIL import Image
    Image.MAX_IMAGE_PIXELS = 16_000_000
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--archive', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    records = []
    with tarfile.open(args.archive, 'r:*') as archive:
        for name, data in fixtures(archive):
            with Image.open(io.BytesIO(data)) as image:
                if image.format != 'PNG' or image.width * image.height > Image.MAX_IMAGE_PIXELS:
                    raise ValueError('Unexpected fixture image format or geometry')
                image = image.convert('RGB')
                image.thumbnail((480, 1800), Image.Resampling.LANCZOS)
                # Bound preview file size independently from PNG compression ratio.
                for quality in (78, 65, 50, 35):
                    out = io.BytesIO()
                    image.save(out, format='JPEG', quality=quality, optimize=True)
                    if len(out.getvalue()) <= 44000:
                        break
                record = preview_record(name, out.getvalue(), image.width, image.height)
                record['original_png_sha256'] = hashlib.sha256(data).hexdigest()
                (output / (name + '.jpg')).write_bytes(out.getvalue())
                records.append(record)
    (output / 'manifest.json').write_text(json.dumps({'schema': 1, 'scope': 'synthetic debug-host fixtures only', 'previews': records}, indent=2) + '\n')
    print('Wrote', len(records), 'bounded fixture previews to', output)


if __name__ == '__main__':
    main()

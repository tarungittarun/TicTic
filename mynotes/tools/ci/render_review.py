"""Bounded fixture-only visual evidence in check annotations when artifact URLs fail.

Original PNG screenshots remain in the normal artifact. JPEG annotations are review
previews, not pixel-exact assertions. Never point this at real-user screenshots.
"""
import argparse
import base64
import hashlib
import io
import json
from pathlib import PurePosixPath
import re
import tarfile

MAX_IMAGE_BYTES = 8 * 1024 * 1024
MAX_IMAGES = 40


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


def annotation(name, preview, width, height):
    row = {'name': name, 'mime': 'image/jpeg', 'width': width, 'height': height,
           'sha256': hashlib.sha256(preview).hexdigest(),
           'data': base64.b64encode(preview).decode('ascii')}
    message = 'NOTES_RENDER=' + json.dumps(row, separators=(',', ':'))
    if len(message) > 60000:
        raise ValueError('Review annotation exceeds bound')
    return f'::notice title=Rendered fixture {name}::{message}'


def main():
    from PIL import Image
    Image.MAX_IMAGE_PIXELS = 16_000_000
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--archive', required=True)
    args = parser.parse_args()
    with tarfile.open(args.archive, 'r:*') as archive:
        for name, data in fixtures(archive):
            with Image.open(io.BytesIO(data)) as image:
                if image.format != 'PNG' or image.width * image.height > Image.MAX_IMAGE_PIXELS:
                    raise ValueError('Unexpected fixture image format or geometry')
                image = image.convert('RGB')
                image.thumbnail((480, 1800), Image.Resampling.LANCZOS)
                # Bound annotation payload independently from PNG compression ratio.
                for quality in (78, 65, 50, 35):
                    out = io.BytesIO()
                    image.save(out, format='JPEG', quality=quality, optimize=True)
                    if len(out.getvalue()) <= 44000:
                        break
                print(annotation(name, out.getvalue(), image.width, image.height))


if __name__ == '__main__':
    main()

"""Offline representation audit, NOT OCR inference or an emoji recognition benchmark.

Reads SHA-pinned shipped traineddata and Unicode's pinned public emoji test list.
A missing output code point proves a repertoire limitation; presence does not prove
recognition accuracy. Never reads user images, runs training, or changes models.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct

EMOJI_BLOB = 'e42fdef494399d2a05b9b75930a3231fe81b2436'


def tokens(data):
    if len(data) < 4 or len(data) > 32 * 1024 * 1024:
        raise ValueError('Traineddata size outside audit budget')
    count = struct.unpack_from('<i', data)[0]
    if not 22 <= count <= 64 or len(data) < 4 + count * 8:
        raise ValueError('Unsupported traineddata directory')
    offsets = struct.unpack_from('<' + 'q' * count, data, 4)
    present = [offset for offset in offsets if offset >= 0]
    if any(offset < 4 + count * 8 or offset >= len(data) for offset in present) or len(present) != len(set(present)):
        raise ValueError('Invalid traineddata offsets')
    start = offsets[21]  # TESSDATA_LSTM_UNICHARSET, Tesseract tessdatamanager.h
    if start < 0:
        raise ValueError('Missing LSTM unicharset')
    end = min([offset for offset in present if offset > start] + [len(data)])
    lines = data[start:end].decode('utf-8').splitlines()
    declared = int(lines[0])
    if declared != len(lines) - 1 or not 3 <= declared <= 100000:
        raise ValueError('Unicharset count mismatch')
    values = [line.split(' ', 1)[0] for line in lines[1:]]
    if values[:3] != ['NULL', 'Joined', '|Broken|0|1']:
        raise ValueError('Unsupported reserved-character convention')
    return declared, set(values[3:]) | {' '}


def emoji_sequences(data):
    digest = hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()
    if digest != EMOJI_BLOB:
        raise ValueError('Unicode emoji list is not the pinned Git blob')
    result = set()
    for line in data.decode('utf-8').splitlines():
        value = line.split('#', 1)[0]
        if ';' not in value:
            continue
        codepoints, status = value.split(';', 1)
        if status.strip() == 'fully-qualified':
            result.add(''.join(chr(int(part, 16)) for part in codepoints.split()))
    if not result:
        raise ValueError('No fully qualified emoji sequences')
    return result


def audit(manifest, directory, emoji_data):
    emoji = emoji_sequences(emoji_data)
    rows, union = {}, set()
    for name, expected in manifest['files'].items():
        data = (Path(directory) / name).read_bytes()
        if len(data) != expected['bytes'] or hashlib.sha256(data).hexdigest() != expected['sha256']:
            raise ValueError('Shipped model identity mismatch: ' + name)
        declared, values = tokens(data)
        union.update(values)
        available = set(''.join(values))
        possible = sorted(item for item in emoji if set(item) <= available)
        rows[name] = {'sha256': expected['sha256'], 'declared_unicharset_entries_including_3_reserved': declared,
                      'exact_emoji_tokens': sorted(values & emoji),
                      'emoji_sequences_without_missing_codepoints_necessary_not_sufficient': len(possible)}
    available = set(''.join(union))
    return {'schema': 1, 'scope': 'Output-repertoire audit only. No inference, training, font coverage or accuracy claim.',
            'model_commit': manifest['commit'], 'unicode_version': '17.0', 'emoji_git_blob': EMOJI_BLOB,
            'emoji_sha256': hashlib.sha256(emoji_data).hexdigest(), 'fully_qualified_emoji_sequences': len(emoji),
            'models': rows, 'union_sequences_without_missing_codepoints_necessary_not_sufficient':
            sum(set(item) <= available for item in emoji),
            'example_required_codepoints': {text: {f'U+{ord(c):04X}': c in available for c in text}
                                             for text in ['😀', '😊', '❤️', '👨‍👩‍👧‍👦', '🇮🇳', '1️⃣']}}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--manifest', required=True)
    parser.add_argument('--models', required=True)
    parser.add_argument('--emoji-test', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    result = audit(json.loads(Path(args.manifest).read_text()), args.models, Path(args.emoji_test).read_bytes())
    Path(args.output).write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(result, ensure_ascii=False, indent=2))

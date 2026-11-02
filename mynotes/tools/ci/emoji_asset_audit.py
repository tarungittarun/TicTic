"""Offline candidate-asset filename audit. NOT training, detection or OCR accuracy.

Read only public pinned Git tree metadata and the pinned Unicode17 list. Graphics
must not be confused with the repository's software license. No image downloads,
user input, runtime changes or implicit permission to redistribute a model.
"""
import argparse
import json
import re
from pathlib import Path
from .emoji_charset_audit import emoji_sequences

REPOSITORY = 'jdecked/twemoji'
COMMIT = 'b6b55fef1e8636b540a6d016a4729ca8cdf2e60b'
TREE = '09b56fba51275e05f0f1d2002efd069ed57298ba'
GRAPHICS_LICENSE_BLOB = 'dc8853a70cfa9d7e8933c080caa147815c659fab'


def normalized(codepoints):
    # Presentation-selector equivalence is only a filename candidate rule.
    # Never collapse ZWJ, skin tones, tags, gender or keycap components.
    return tuple(value for value in codepoints if value != 0xfe0f)


def candidates(tree):
    if tree.get('sha') != TREE or tree.get('truncated') is not False:
        raise ValueError('Require the complete pinned public asset tree')
    result, paths = {}, set()
    for item in tree['tree']:
        path = item['path']
        if not path.startswith('assets/svg/') or item['type'] != 'blob':
            continue
        match = re.fullmatch(r'assets/svg/([0-9a-f]+(?:-[0-9a-f]+)*)\.svg', path)
        if not match or path in paths or not re.fullmatch('[0-9a-f]{40}', item['sha']):
            raise ValueError('Invalid or duplicate SVG metadata')
        points = [int(value, 16) for value in match[1].split('-')]
        if any(value > 0x10ffff or 0xd800 <= value <= 0xdfff for value in points):
            raise ValueError('Invalid Unicode scalar in asset filename')
        paths.add(path)
        result.setdefault(normalized(points), []).append({'path': path, 'blob': item['sha']})
    return result


def audit(tree, unicode_data):
    assets = candidates(tree)
    sequences = emoji_sequences(unicode_data)
    matched, missing, ambiguous = 0, [], []
    for text in sorted(sequences):
        options = assets.get(normalized(map(ord, text)), [])
        if len(options) == 1:
            matched += 1
        elif options:
            ambiguous.append({'sequence': text, 'candidates': options})
        else:
            missing.append(text)
    return {'schema': 1, 'scope': 'Public metadata/normalized filename candidates only. No pixels inspected, training, inference, detection or accuracy claim.',
            'repository': REPOSITORY, 'commit': COMMIT, 'tree': TREE,
            'graphics_license': 'CC-BY-4.0 (not the MIT software license)',
            'graphics_license_blob': GRAPHICS_LICENSE_BLOB,
            'unicode_version': '17.0', 'fully_qualified_sequences': len(sequences),
            'svg_metadata_entries': sum(map(len, assets.values())),
            'single_filename_candidates': matched, 'missing': missing, 'ambiguous': ambiguous,
            'runtime_models_changed': False, 'training_steps': 0,
            'recognition_accuracy': None, 'release_approval': False}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--tree', required=True)
    parser.add_argument('--emoji-test', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    result = audit(json.loads(Path(args.tree).read_text()), Path(args.emoji_test).read_bytes())
    Path(args.output).write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({key: value for key, value in result.items() if key not in ('missing', 'ambiguous')}))

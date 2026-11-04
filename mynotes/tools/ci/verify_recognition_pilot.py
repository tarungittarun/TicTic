"""Require exact executed native methods; adb exit zero alone is not proof."""
import argparse
import json
from pathlib import Path
from mynotes.tools.build_e2e_report import methods
from mynotes.tools.ci.results import android


def verify(text, classes):
    expected = [name for name in methods() if name.split('#')[0] in classes]
    if not expected or {name.split('#')[0] for name in expected} != set(classes):
        raise ValueError('Unknown or empty pilot test class')
    return android(text, expected)


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--classes', required=True)
    p.add_argument('--log', type=Path, default=Path('refinement-native.txt'))
    p.add_argument('--output', type=Path, default=Path('refinement-native-verified.json'))
    args = p.parse_args()
    result = verify(args.log.read_text(errors='replace') if args.log.exists() else '', args.classes.split(','))
    args.output.write_text(json.dumps(result, indent=2)+'\n')
    summary = json.dumps({k:v for k,v in result.items() if k != 'cases'})
    print(summary)
    if not result['ok']:
        print('::error title=Recognition pilot incomplete::'+summary.replace('%','%25').replace('\n','%0A'))
        raise SystemExit(1)


if __name__ == '__main__':
    main()

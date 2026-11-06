"""Retain bounded original public CI evidence and independently recheck exact native outcomes.

No APK, model, private data or generated dependency is retained. Failed runs remain failed.
"""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import tarfile

from mynotes.tools.ci.refinement_plan import selected_cases
from mynotes.tools.ci.results import android
from mynotes.tools.ci.test_plan import build_plan

MAX_BYTES = 96 * 1024 * 1024


def retain(source, metadata, output):
    source, output = Path(source), Path(output)
    if metadata['status'] != 'completed':
        raise ValueError('Cannot publish an unfinished run as completed qualification')
    output.mkdir(parents=True, exist_ok=True)
    manifest, total = [], 0

    def save(data, relative):
        nonlocal total
        relative = PurePosixPath(relative)
        if relative.is_absolute() or '..' in relative.parts:
            raise ValueError('Unsafe evidence path')
        total += len(data)
        if total > MAX_BYTES:
            raise ValueError('Retention bound exceeded; never silently truncate evidence')
        target = output / str(relative)
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists() and target.read_bytes() != data:
            raise ValueError('Refusing to overwrite different original evidence')
        target.write_bytes(data)
        manifest.append({'path': str(relative), 'bytes': len(data),
                         'sha256': hashlib.sha256(data).hexdigest(), 'source_bytes_modified': False})

    reports = {}
    for lane in sorted(source.iterdir()):
        if not lane.is_dir() or not lane.name.startswith('full71-'):
            continue
        for path in sorted(lane.rglob('*')):
            if not path.is_file():
                continue
            if path.suffix in {'.json', '.txt'} or path.name.startswith('TEST-') and path.suffix == '.xml':
                save(path.read_bytes(), str(Path(lane.name) / path.relative_to(lane)))
        for archive in lane.glob('refinement-captures.tar'):
            if not archive.stat().st_size:
                continue
            with tarfile.open(archive) as tar:
                count = 0
                for member in tar:
                    if not member.isfile():
                        continue
                    path = PurePosixPath(member.name)
                    if len(path.parts) != 2 or path.parts[0] != 'ux-review' or path.suffix not in {'.png', '.jsonl'}:
                        raise ValueError('Unexpected capture path: ' + member.name)
                    count += 1
                    if count > 200 or not 0 < member.size <= 8 * 1024 * 1024:
                        raise ValueError('Capture exceeds bounded public-fixture contract')
                    save(tar.extractfile(member).read(), str(Path(lane.name) / 'captures' / path.name))
        if lane.name.startswith('full71-native-'):
            expected_file = lane / 'refinement-expected.json'
            raw_file = lane / 'refinement-native.txt'
            if not expected_file.exists():
                reports[lane.name] = {'ok': False, 'error': 'No selected inventory; lane did not qualify'}
                continue
            expected = json.loads(expected_file.read_text())
            shard = int(lane.name.rsplit('-s', 1)[1])
            if expected != selected_cases(build_plan(), 'all', shard):
                raise ValueError('Lane inventory differs from exhaustive current-source shard')
            result = android(raw_file.read_text(errors='replace') if raw_file.exists() else '', expected)
            receipt = lane / 'refinement-native.json'
            if receipt.exists() and json.loads(receipt.read_text()) != result:
                raise ValueError('Retained receipt disagrees with original native log')
            reports[lane.name] = {key: value for key, value in result.items() if key != 'cases'}

    profiles = {}
    for lane, report in reports.items():
        profiles.setdefault(lane.rsplit('-s', 1)[0], []).append(report)
    inventory = len(selected_cases(build_plan(), 'all'))
    completed_profiles = {name: len(rows) == 2 and all(row.get('ok') for row in rows)
                          and sum(row.get('expected', 0) for row in rows) == inventory
                          for name, rows in profiles.items()}
    summary = {
        'run': metadata['databaseId'], 'source': metadata['headSha'], 'url': metadata['url'],
        'workflow_conclusion': metadata['conclusion'], 'native_unique_methods': inventory,
        'native_executions_passed': sum(row.get('passed', 0) for row in reports.values()),
        'native_expected_executions': inventory * 6, 'native_profiles': completed_profiles,
        'native_lanes': reports,
        'all_six_profiles_exact_native_pass': len(reports) == 12 and len(completed_profiles) == 6
                                            and all(completed_profiles.values()),
        'whole_app_ux_acceptance': False, 'physical_device_acceptance': False,
        'paired_obsidian_acceptance': False, 'public_release_approval': False,
        'scope': 'Full discovered native-test inventory on six synthetic emulator profiles. '
                 'Not every possible behavior, real-device quality or paired UX acceptance. '
                 'Capture dimensions vary by helper; some fixtures were resized before artifact creation. Original artifact bytes are retained.'}
    (output / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    (output / 'run.json').write_text(json.dumps(metadata, indent=2) + '\n')
    (output / 'retention.json').write_text(json.dumps({'files': manifest, 'bytes': total,
                                                      'source_bytes_modified': False}, indent=2) + '\n')
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--artifacts', required=True)
    parser.add_argument('--metadata', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    summary = retain(args.artifacts, json.loads(Path(args.metadata).read_text()), args.output)
    print(json.dumps(summary, indent=2))


if __name__ == '__main__':
    main()

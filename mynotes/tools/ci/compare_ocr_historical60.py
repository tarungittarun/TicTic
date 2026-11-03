"""Compare retained v59 observations with v60; not a fresh same-guest A/B."""
import argparse
import json
import tarfile
from pathlib import Path
from mynotes.tools.build_module_e2e_report import PROFILES
from mynotes.tools.ci.compare_ocr_holdout import compare_rows


def pair_rows(old, new):
    for key in ('id', 'input_sha256', 'reference', 'kind', 'condition'):
        if old[key] != new[key]:
            raise ValueError('Historical fixture mismatch: ' + key)
    return compare_rows(old, new)


def summarize(historical, artifacts):
    retained = historical['pairs']
    keys = {(p['profile'], p['id']) for p in retained}
    if len(retained) != 512 or len(keys) != 512:
        raise ValueError('Historical cohort incomplete or duplicated')
    pairs = []
    for profile in PROFILES:
        folder = artifacts / ('regression60-' + profile + '-holdout')
        execution = json.loads((folder / 'module-execution.json').read_text())
        if not execution['ok'] or execution['expected'] != 4 or execution['passed'] != 4:
            raise ValueError('Incomplete native execution: ' + profile)
        with tarfile.open(folder / 'module-benchmark.tar') as archive:
            member = archive.getmember('module-benchmark/ocr-holdout.json')
            if not member.isfile() or member.size > 4 * 1024 * 1024:
                raise ValueError('Invalid bounded report')
            report = json.load(archive.extractfile(member))
        rows = report['cases']
        indexed = {row['id']: row for row in rows}
        old = {p['id']: p['candidate'] for p in retained if p['profile'] == profile}
        if (report['module'] != 'ocr-holdout' or report['expected'] != 128
                or len(rows) != 128 or len(indexed) != 128 or set(indexed) != set(old)):
            raise ValueError('Incomplete or changed frozen cohort: ' + profile)
        for key in sorted(old):
            pairs.append({'profile': profile, **pair_rows(old[key], indexed[key])})
    return {
        'scope': 'Historical v59 versus new v60 observations, not fresh same-guest A/B. '
                 '128 distinct synthetic images repeated across four UI profiles. '
                 'Exact pixels, reference, kind and condition required. Timing is non-causal.',
        'candidate_build_run': 41704429338,
        'pairs': pairs,
        'improved_pairs': sum(p['improvement'] for p in pairs),
        'regressed_pairs': sum(p['regression'] for p in pairs),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('historical', type=Path)
    parser.add_argument('artifacts', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    result = summarize(json.loads(args.historical.read_text()), args.artifacts)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n')
    print(json.dumps({k: v for k, v in result.items() if k != 'pairs'}))
    if result['regressed_pairs']:
        raise SystemExit(1)


if __name__ == '__main__':
    main()

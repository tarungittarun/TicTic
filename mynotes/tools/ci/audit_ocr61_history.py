"""Historical same-input OCR comparison, not same-guest A/B or real-photo qualification."""
import argparse
import json
from pathlib import Path
import statistics
import tarfile
from mynotes.tools.build_module_e2e_report import independent_score

PROFILES = ('light-portrait', 'dark-portrait', 'dark-landscape')
FIELDS = ('id', 'input_sha256', 'reference', 'kind', 'font', 'font_px',
          'input_dark', 'rotation_degrees', 'width', 'height')


def pair(old, new):
    if any(old[k] != new[k] for k in FIELDS):
        raise ValueError('Historical fixture mismatch')
    for row in (old, new):
        independent_score(row, 'ocr')
    ok = old['status'] == new['status'] == 'OK'
    return {**{k: old[k] for k in FIELDS},
            'baseline': {k: old.get(k) for k in ('status', 'recognized', 'cer', 'strict_wer', 'elapsed_ms')},
            'candidate': {k: new.get(k) for k in ('status', 'recognized', 'cer', 'strict_wer', 'elapsed_ms')},
            'improved': ok and new['cer'] < old['cer']-1e-8,
            'regressed': not ok or new['cer'] > old['cer']+1e-8}


def load(folder):
    execution = json.loads((folder/'module-execution.json').read_text())
    if not execution['ok'] or execution['expected'] != 4 or execution['passed'] != 4:
        raise ValueError('Incomplete native benchmark execution')
    with tarfile.open(folder/'module-benchmark.tar') as archive:
        member = archive.getmember('module-benchmark/ocr.json')
        if not member.isfile() or member.size > 8_000_000:
            raise ValueError('Unbounded OCR report')
        report = json.load(archive.extractfile(member))
    rows = report['cases']
    indexed = {row['id']: row for row in rows}
    if report['module'] != 'ocr' or report['expected'] != 384 or len(rows) != 384 or set(indexed) != {'ocr-%04d' % i for i in range(384)}:
        raise ValueError('Incomplete, duplicate or changed OCR cohort')
    return indexed


def summarize(baseline, candidate, baseline_run=41704429338, candidate_run=41767304608):
    pairs = []
    for profile in PROFILES:
        name = 'module-benchmark-'+profile+'-ocr'
        old, new = load(baseline/name), load(candidate/name)
        for key in sorted(old):
            pairs.append({'profile': profile, **pair(old[key], new[key])})
    summary = {}
    for version in ('baseline', 'candidate'):
        rows = [p[version] for p in pairs]
        ok = [r for r in rows if r['status'] == 'OK']
        summary[version] = {'observations': len(rows), 'operational_errors': len(rows)-len(ok),
                            'accuracy_targets_met': sum(r['cer'] <= .2 for r in ok),
                            'mean_cer': statistics.mean(r['cer'] for r in ok) if ok else None,
                            'median_historical_elapsed_ms': statistics.median(r['elapsed_ms'] for r in rows)}
    return {'scope': f'Historical run {baseline_run} versus {candidate_run}; 384 synthetic images repeated across three profiles, NOT 1152 independent real photos. Exact original input pixels/reference/render fields required. Timing non-causal: different historical guests.',
            'excluded_profile': 'Fixed three-profile cohort inherited from absent v60 light-landscape artifact; not an accuracy-based exclusion',
            'source_runs': {'baseline': baseline_run, 'candidate': candidate_run},
            'summary': summary, 'improved_pairs': sum(p['improved'] for p in pairs),
            'regressed_pairs': sum(p['regressed'] for p in pairs), 'adopted': False, 'pairs': pairs}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('baseline', type=Path)
    p.add_argument('candidate', type=Path)
    p.add_argument('output', type=Path)
    p.add_argument('--baseline-run', type=int, default=41704429338)
    p.add_argument('--candidate-run', type=int, default=41767304608)
    args = p.parse_args()
    result = summarize(args.baseline, args.candidate, args.baseline_run, args.candidate_run)
    args.output.write_text(json.dumps(result, indent=2, ensure_ascii=False)+'\n')
    summary = {k:v for k,v in result.items() if k != 'pairs'}
    print('::notice title=Historical OCR measurement::'+json.dumps(summary))
    if result['regressed_pairs']:
        print('::error title=Historical OCR regressions retained::'+str(result['regressed_pairs']))
        raise SystemExit(1)


if __name__ == '__main__':
    main()

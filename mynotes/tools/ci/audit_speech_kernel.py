"""Independent raw-hypothesis audit of opt-in PUBLIC CPU-kernel research, never adoption."""
import argparse
import json
import math
from pathlib import Path
import re
import statistics
from mynotes.tools.ci.quality_reports import distance


def words(text):
    return re.sub("[^a-z0-9']+", ' ', text.lower()).strip().split()


def validate(report, expected=146):
    if report['variant'] not in ('standard', 'flash') or report['shard'] not in (0, 1):
        raise ValueError('Unknown research phase')
    rows = report['cases']
    if report['expected'] != expected or len(rows) != expected or len({r['id'] for r in rows}) != expected:
        raise ValueError('Incomplete/duplicate public observations')
    if report['training_steps'] != 0 or report['adopted'] is not False or report['warmup']['status'] != 'OK':
        raise ValueError('Invalid research provenance/warmup')
    for row in rows:
        if row['status'] != 'OK' or row.get('audio_unchanged') is not True:
            raise ValueError('Operational/source-preservation failure retained')
        if not re.fullmatch('[0-9a-f]{64}', row['input_sha256']):
            raise ValueError('Missing input identity')
        score = distance(words(row['reference']), words(row['recognized']))
        if not math.isfinite(row['normalized_wer']) or abs(row['normalized_wer']-score) > 1e-8:
            raise ValueError('Native WER disagrees with independent hypothesis scoring')
        if type(row['elapsed_ms']) is not int or row['elapsed_ms'] < 0:
            raise ValueError('Invalid wall time')
    return {r['id']: r for r in rows}


def compare(standard, flash, expected=146):
    a, b = validate(standard, expected), validate(flash, expected)
    if standard['variant'] != 'standard' or flash['variant'] != 'flash' or standard['shard'] != flash['shard'] or a.keys() != b.keys():
        raise ValueError('Mismatched research phases')
    pairs=[]
    for key in sorted(a):
        before, after = a[key], b[key]
        for field in ('reference', 'input_sha256', 'kind', 'input_bytes', 'seconds'):
            if before[field] != after[field]:
                raise ValueError('Public input/reference mismatch')
        pairs.append({'id': key, 'reference': before['reference'], 'input_sha256': before['input_sha256'],
                      'standard': before, 'flash': after,
                      'regressed': after['normalized_wer'] > before['normalized_wer']+1e-8,
                      'improved': after['normalized_wer'] < before['normalized_wer']-1e-8})
    ratios=[p['standard']['elapsed_ms']/p['flash']['elapsed_ms'] for p in pairs
            if p['standard']['elapsed_ms'] > 0 and p['flash']['elapsed_ms'] > 0 and p['standard']['kind'] != 'silence-control']
    return {'scope': 'Exact-model ordered same-guest CPU attention comparison, balanced order across guests; observational end-to-end emulator timings, not causal phone speed, physical qualification, new training or adoption.',
            'adopted': False, 'shard': standard['shard'], 'pairs': pairs,
            'observations': len(pairs), 'improved': sum(p['improved'] for p in pairs),
            'regressed': sum(p['regressed'] for p in pairs),
            'observed_median_ratio_standard_over_flash': statistics.median(ratios) if ratios else None}


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('standard', type=Path);p.add_argument('flash', type=Path);p.add_argument('output', type=Path)
    a=p.parse_args();report=compare(json.loads(a.standard.read_text()), json.loads(a.flash.read_text()))
    a.output.write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    print('::notice title=Nonshipping CPU attention parity::'+json.dumps({k:v for k,v in report.items() if k != 'pairs'}))
    if report['regressed']:
        raise SystemExit('Regressions retained; default attention stays unchanged')

if __name__ == '__main__':
    main()

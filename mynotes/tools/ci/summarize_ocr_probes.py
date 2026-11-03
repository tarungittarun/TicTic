"""Summarize actual public-only failed-case probes; never fit or select app output."""
import argparse,json,tarfile
from pathlib import Path
from mynotes.tools.build_module_e2e_report import independent_score,independent_public_probes

def summarize(report):
    rows=report['cases']
    if report['module']!='ocr' or len(rows)!=384 or report['expected']!=384:raise ValueError('Incomplete OCR cohort')
    if {r['id'] for r in rows}!={f'ocr-{i:04d}' for i in range(384)}:raise ValueError('Unexpected or duplicate OCR IDs')
    failures=[]
    for row in rows:
        independent_score(row,'ocr');independent_public_probes(row)
        if row['status']!='OK' or not row.get('accuracy_target_met'):failures.append(row)
    return {'scope':'Actual public synthetic failed fixtures only; candidate modes are diagnostics, not production choices. Scores independently recomputed. No private inputs or training.','observed':len(rows),'operational_errors':sum(r['status']!='OK' for r in rows),'target_passes':sum(bool(r.get('accuracy_target_met')) for r in rows),'diagnostic_errors':sum('probe_error' in r for r in failures),'failures':failures}

def main():
    parser=argparse.ArgumentParser();parser.add_argument('archive',type=Path);parser.add_argument('output',type=Path);args=parser.parse_args()
    with tarfile.open(args.archive) as bundle:
        member=bundle.getmember('module-benchmark/ocr.json')
        if not member.isfile() or member.size>4*1024*1024:raise ValueError('Unbounded OCR report')
        summary=summarize(json.loads(bundle.extractfile(member).read()))
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(summary,indent=2,ensure_ascii=False)+'\n')
    print(json.dumps({k:v for k,v in summary.items() if k!='failures'}))
if __name__=='__main__':main()

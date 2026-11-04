"""Composite exact-build evidence for lanes with zero original tests due to failed boot.
Never replace asserted model/UI failures or relabel the original workflow green.
"""
import argparse,json
from pathlib import Path
from mynotes.tools.build_module_e2e_report import ingest,render,write,PROFILES


def validate_receipt(receipt):
    if receipt['original_conclusion']!='failure' or receipt['original_source']!=receipt['source']:
        raise ValueError('Original failure/source must remain explicit')
    expected=set(receipt['missing_lanes'])
    if not expected or not expected <= {p+'-ocr' for p in PROFILES} or len(expected)!=len(receipt['missing_lanes']):
        raise ValueError('Missing/duplicate supplement lanes')
    failed=receipt['original_failed_jobs']
    if {r['lane'] for r in failed}!=expected or len(failed)!=len(expected):
        raise ValueError('Supplement cannot hide another failed job')
    captures=receipt['lane_capture_runs']
    if set(captures)!=expected or any(type(v) is not int or v<=0 for v in captures.values()):
        raise ValueError('Explicit actual supplemental capture runs required')
    names=set(receipt['original_artifact_names'])
    for row in failed:
        if row['conclusion']!='failure' or row['boot_timeout_verified'] is not True or row['tests_executed']!=0:
            raise ValueError('Only independently verified zero-test boot failures allowed')
        if 'module-benchmark-'+row['lane'] in names:
            raise ValueError('Original lane has evidence; never overwrite it')
    return expected


def build(artifacts,output,receipt,run):
    lanes=validate_receipt(receipt)
    data,rows,inventory=ingest(artifacts,output,receipt['source'],str(run))
    for lane in lanes:
        execution=data['benchmark_executions'].get(lane,{})
        selected=[r for r in rows if r['lane']==lane]
        if not execution.get('ok') or execution.get('passed')!=4 or len(selected)!=384:
            raise ValueError('Supplement must execute all four original assertions and exact 384-case OCR cohort')
    for row in rows:row['capture_run']=receipt['lane_capture_runs'].get(row['lane'],receipt['build_run'])
    for frame in data['screens']:
        # Native UI screenshots belong to the original run; OCR input specimens
        # are supplied by the new light-portrait benchmark, not the UI suite.
        capture=receipt['lane_capture_runs'].get('light-portrait-ocr',receipt['build_run']) if frame['profile']=='synthetic-input-not-UI' else receipt['build_run']
        frame['capture_run']=str(capture)
    data.update(composite_evidence=True,build_run=receipt['build_run'],original_workflow_conclusion='failure',benchmark_capture_runs={lane:receipt['lane_capture_runs'].get(lane,receipt['build_run']) for lane in data['benchmark_executions']},
                supplemental_provenance=receipt,source_run_note='Original build workflow remains failed at emulator boot. Only its zero-test missing OCR lanes were executed separately against the exact same compiled APKs. Other measurements/native outcomes retain original capture runs. No assertion failures replaced.')
    write(output/'provenance.json',data);write(output/'measurements.json',rows)
    write(output/'boot-supplement-receipt.json',receipt)
    summary=render(data,rows,inventory,output)
    if not data['native_complete'] or not data['benchmark_complete']:
        raise ValueError('Composite still incomplete; retained unqualified report')
    return summary


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--artifacts',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--receipt',type=Path,required=True);p.add_argument('--run',required=True)
    a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True);print(json.dumps(build(a.artifacts,a.output,json.loads(a.receipt.read_text()),a.run),indent=2))

if __name__=='__main__':main()

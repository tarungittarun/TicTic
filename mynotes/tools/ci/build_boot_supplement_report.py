"""Composite exact-build evidence for lanes with zero original tests due to failed boot or verified pre-native host setup.
Never replace asserted model/UI failures or relabel the original workflow green.
"""
import argparse,json,re,shutil
from mynotes.tools.ci.pre_native_setup_evidence import verify,FILES,COMMAND
from pathlib import Path
from mynotes.tools.build_module_e2e_report import ingest,render,write,PROFILES


def validate_receipt(receipt):
    if receipt['original_conclusion']!='failure' or receipt['original_source']!=receipt['source']:
        raise ValueError('Original failure/source must remain explicit')
    expected=set(receipt['missing_lanes'])
    if not expected or not expected <= {p+suffix for p in PROFILES for suffix in ('-ocr','-ordered')} or len(expected)!=len(receipt['missing_lanes']):
        raise ValueError('Missing/duplicate supplement lanes')
    failed=receipt['original_failed_jobs']
    if {r['lane'] for r in failed}!=expected or len(failed)!=len(expected):
        raise ValueError('Supplement cannot hide another failed job')
    captures=receipt['lane_capture_runs']
    if set(captures)!=expected or any(type(v) is not int or v<=0 for v in captures.values()):
        raise ValueError('Explicit actual supplemental capture runs required')
    names=set(receipt['original_artifact_names'])
    for row in failed:
        setup=row.get('setup_failure_proof')
        setup_ok=(row['lane']=='light-landscape-ordered' and row['conclusion']=='cancelled' and row.get('pre_native_setup_verified') is True and isinstance(setup,dict)
                  and setup.get('kind')=='pre_native_host_command_timeout' and setup.get('last_command')==COMMAND and setup.get('native_launcher_started') is False
                  and type(setup.get('tests_executed')) is int and setup.get('tests_executed')==0 and set(setup.get('original_artifact_files',{}))==FILES
                  and all(isinstance(v,str) and re.fullmatch('[0-9a-f]{64}',v) for v in [setup.get('log_sha256',''),*setup.get('original_artifact_files',{}).values()]))
        boot_ok=row['conclusion']=='failure' and row.get('boot_timeout_verified') is True
        if type(row['tests_executed']) is not int or row['tests_executed']!=0 or not (boot_ok or setup_ok):
            raise ValueError('Only independently verified zero-test host setup/boot failures allowed')
        prefix='module-native-' if row['lane'].endswith('-ordered') else 'module-benchmark-'
        if prefix+row['lane'] in names and not setup_ok:
            raise ValueError('Original lane has evidence; never overwrite it')
        if setup_ok and prefix+row['lane'] not in names:
            raise ValueError('Original two-file configured-host artifact must remain explicit')
    return expected


def build(artifacts,output,receipt,run,additional_scenes_required=False):
    lanes=validate_receipt(receipt)
    for row in receipt['original_failed_jobs']:
        if not row.get('pre_native_setup_verified'):continue
        original=artifacts/('original-zero-test-'+row['lane'])
        log=artifacts/('original-zero-test-'+row['lane']+'-job-log.txt')
        files={p.name:p.read_bytes() for p in original.iterdir()}
        if verify(log.read_text(),files)!=row['setup_failure_proof']:
            raise ValueError('Original setup evidence was not preserved byte-for-byte')
        dest=output/'original-zero-test-setup'/row['lane'];dest.mkdir(parents=True,exist_ok=True)
        for name in files:shutil.copyfile(original/name,dest/name)
        shutil.copyfile(log,dest/'original-job-log.txt')
    data,rows,inventory=ingest(artifacts,output,receipt['source'],str(run),additional_scenes_required)
    for lane in lanes:
        if lane.endswith('-ordered'):
            execution=data.get('native',{}).get(lane,{})
            if not inventory or execution.get('ok') is not True or execution.get('passed')!=len(inventory) or len(execution.get('cases',[]))!=len(inventory) or set(execution['cases'])!=set(inventory):
                raise ValueError('Native supplement must execute the complete exact original inventory once')
            continue
        execution=data['benchmark_executions'].get(lane,{})
        selected=[r for r in rows if r['lane']==lane]
        if not execution.get('ok') or execution.get('passed')!=4 or len(selected)!=384:
            raise ValueError('Supplement must execute all four original assertions and exact 384-case OCR cohort')
    for row in rows:row['capture_run']=receipt['lane_capture_runs'].get(row['lane'],receipt['build_run'])
    for frame in data['screens']:
        # Native UI screenshots belong to the original run; OCR input specimens
        # are supplied by the new light-portrait benchmark, not the UI suite.
        capture=receipt['lane_capture_runs'].get('light-portrait-ocr',receipt['build_run']) if frame['profile']=='synthetic-input-not-UI' else receipt['lane_capture_runs'].get(frame.get('lane'),receipt['build_run'])
        frame['capture_run']=str(capture)
    data.update(composite_evidence=True,build_run=receipt['build_run'],original_workflow_conclusion='failure',benchmark_capture_runs={lane:receipt['lane_capture_runs'].get(lane,receipt['build_run']) for lane in data['benchmark_executions']},
                native_capture_runs={lane:receipt['lane_capture_runs'].get(lane,receipt['build_run']) for lane in data.get('native',{})},supplemental_provenance=receipt,source_run_note='Original build workflow remains failed/cancelled during emulator boot or verified pre-native host setup. Only independently verified zero-test missing lanes were executed separately against the exact same compiled APKs. Other measurements/native outcomes retain original capture runs. No assertion failures replaced.')
    if additional_scenes_required:
        public=data.get('additional_public_scenes',{})
        for row in public.get('rows',[]):row['capture_run']=receipt['lane_capture_runs'].get(row['profile']+'-ocr',receipt['build_run'])
        write(output/'additional-public-scenes'/'independent-results.json',public)
    write(output/'provenance.json',data);write(output/'measurements.json',rows)
    write(output/'boot-supplement-receipt.json',receipt)
    summary=render(data,rows,inventory,output)
    if not data['native_complete'] or not data['benchmark_complete'] or (additional_scenes_required and not data.get('additional_public_scenes',{}).get('complete')):
        raise ValueError('Composite still incomplete; retained unqualified report')
    return summary


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--artifacts',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--receipt',type=Path,required=True);p.add_argument('--run',required=True);p.add_argument('--additional-scenes-required',action='store_true')
    a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True);print(json.dumps(build(a.artifacts,a.output,json.loads(a.receipt.read_text()),a.run,a.additional_scenes_required),indent=2))

if __name__=='__main__':main()

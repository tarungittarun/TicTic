"""Independent frozen-reference A/B comparison; always retain individual regressions."""
import argparse,json,statistics,tarfile
from pathlib import Path
from mynotes.tools.build_module_e2e_report import independent_score,normalize,PROFILES
from mynotes.tools.ci.quality_reports import distance
ROOT=Path(__file__).resolve().parents[3]

def compare_rows(baseline,candidate):
    if baseline['id']!=candidate['id'] or baseline['input_sha256']!=candidate['input_sha256'] or baseline['reference']!=candidate['reference']:raise ValueError('A/B fixture mismatch')
    for row in [baseline,candidate]:independent_score(row,'ocr')
    ok=baseline['status']==candidate['status']=='OK'
    return {'id':baseline['id'],'reference':baseline['reference'],'input_sha256':baseline['input_sha256'],'kind':baseline['kind'],'condition':baseline['condition'],'baseline':baseline,'candidate':candidate,'delta_cer':candidate['cer']-baseline['cer'] if ok else None,'regression':not ok or candidate['cer']>baseline['cer']+1e-8,'improvement':ok and candidate['cer']<baseline['cer']-1e-8}

def load(folder):
    execution=json.loads((folder/'module-execution.json').read_text())
    if not execution['ok'] or execution['expected']!=4 or execution['passed']!=4:raise ValueError('Incomplete native holdout execution')
    proof=json.loads((folder/'benchmark-signing-proof.json').read_text())
    with tarfile.open(folder/'module-benchmark.tar') as bundle:
        member=bundle.getmember('module-benchmark/ocr-holdout.json')
        if not member.isfile() or member.size>4*1024*1024:raise ValueError('Unbounded holdout JSON')
        report=json.loads(bundle.extractfile(member).read())
    if report['module']!='ocr-holdout' or report['expected']!=128 or len(report['cases'])!=128:raise ValueError('Incomplete holdout cohort')
    rows={r['id']:r for r in report['cases']}
    if len(rows)!=128:raise ValueError('Duplicate holdout ID')
    return rows,proof,execution

def summarize(artifacts):
    corpus=json.loads((ROOT/'mynotes/docs/regression/ocr-heldout-v1.json').read_text())
    expected={r['id']+'-'+str(i):(' '.join(r['lines']),r['kind'],condition) for r in corpus['records'] for i,condition in enumerate(corpus['conditions'])}
    pairs=[];proofs={};fixture_hashes={};test_payloads=set()
    for profile in PROFILES:
        versions={}
        for version in ['baseline58','candidate59']:
            rows,proof,execution=load(artifacts/('regression59-heldout-'+version+'-'+profile))
            if set(rows)!=set(expected):raise ValueError('Missing or unexpected holdout IDs')
            if proof['baseline58']!=(version=='baseline58'):raise ValueError('Wrong APK role')
            app=next(f for f in proof['files'] if f['role']=='app');code=58 if version=='baseline58' else 59
            if "versionCode='"+str(code)+"'" not in app['manifest'] or "name='app.space.notes.debug'" not in app['manifest']:raise ValueError('Wrong target APK')
            if version=='baseline58' and app['original_sha256']!='07bfb4f062124dd97d3e0bd29a2524004e62be7c8d975adabd60cfba025d897d':raise ValueError('Baseline58 byte identity failed')
            test_payloads.add(next(f for f in proof['files'] if f['role']=='test')['unsigned_payload_sha256'])
            proofs[profile+'-'+version]={'payload_proof':proof,'native_execution':execution}
            for id,row in rows.items():
                reference,kind,condition=expected[id]
                if (row['reference'],row['kind'],row['condition'])!=(reference,kind,condition):raise ValueError('Frozen reference or condition changed')
                if id in fixture_hashes and fixture_hashes[id]!=row['input_sha256']:raise ValueError('Cross-profile pixel drift')
                fixture_hashes[id]=row['input_sha256']
            versions[version]=rows
        for id in sorted(expected):pairs.append({'profile':profile,**compare_rows(versions['baseline58'][id],versions['candidate59'][id])})
    if len(test_payloads)!=1:raise ValueError('A/B instrumentation payload differs')
    versions={}
    for key in ['baseline','candidate']:
        rows=[p[key] for p in pairs];ok=[r for r in rows if r['status']=='OK'];units=sum(len(normalize(r['reference'])) for r in ok)
        versions[key]={'observations':len(rows),'operational_errors':len(rows)-len(ok),'target_passes':sum(r.get('accuracy_target_met',False) for r in rows),'mean_cer':statistics.mean(r['cer'] for r in ok) if ok else None,'micro_cer':sum(distance(normalize(r['reference']),normalize(r['recognized']))*len(normalize(r['reference'])) for r in ok)/units if units else None,'median_elapsed_ms':statistics.median(r['elapsed_ms'] for r in rows)}
    regressions=[p for p in pairs if p['regression']]
    return {'scope':'Frozen before tuning/inference.32 own-authored text references ×4 render conditions =128 distinct synthetic inputs, repeated on4 API34 UI profiles per version. Not512 independent photographs or population estimates. Debug APKs re-signed solely for cross-build instrumentation, with all unsigned payload entries verified unchanged. No training/private input.','baseline_app_source':'3fc0ea7b49267a9f10427458452aa619fcbb101d','pairs':pairs,'summary':versions,'improved_pairs':sum(p['improvement'] for p in pairs),'regressed_pairs':len(regressions),'no_case_regressions':not regressions,'source_proofs':proofs}

def main():
    p=argparse.ArgumentParser();p.add_argument('artifacts',type=Path);p.add_argument('output',type=Path);args=p.parse_args();result=summarize(args.artifacts)
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n')
    print(json.dumps({k:v for k,v in result.items() if k not in ['pairs','source_proofs']}))
    if not result['no_case_regressions']:raise SystemExit(1)
if __name__=='__main__':main()

"""Complete a registry census from explicit independent native capture segments.

An interrupted native run stays interrupted in provenance. This only certifies
that every exact registry ID has one collected entry, never semantic acceptance
or a successful uninterrupted 205-action execution.
"""
import json,re,shutil,sys
from pathlib import Path
PIN='2d92966fba5441a8976ae86b39cf138c0a845d3dcd318e6f0d29d304950bca5d'
def validate_segments(inventories,identities,entries,tail_complete):
 expected=[x['id'] for x in inventories[0]['commands']]
 if any([x['id'] for x in i['commands']]!=expected for i in inventories):raise ValueError('Registry identity/order mismatch')
 if any(i.get('apk_sha256')!=PIN for i in identities):raise ValueError('APK mismatch')
 ids=[re.search(r'Command=([^;]+);',r['observed']).group(1) for r in entries]
 if len(ids)!=len(set(ids)) or set(ids)!=set(expected):raise ValueError('Incomplete or duplicate command evidence')
 if not tail_complete:raise ValueError('Continuation did not finish')
 return len(ids)
def main(first,tail,out):
 roots=[Path(first),Path(tail)];out=Path(out);out.mkdir(parents=True,exist_ok=True)
 load=lambda r,n:json.loads((r/n).read_text())
 invs=[load(r,'feature-inventory.json') for r in roots];identities=[load(r,'apk-identity.json') for r in roots]
 allrows=[load(r,'feature-results.json') for r in roots]
 commandrows=[x for a in allrows for x in a if x['feature'].startswith('Command entry: ')]
 tail_summary=load(roots[1],'coverage-summary.json');count=validate_segments(invs,identities,commandrows,tail_summary.get('execution_complete') is True)
 rows=[];segments=[]
 for n,(r,a,identity) in enumerate(zip(roots,allrows,identities)):
  old=load(r,'coverage-summary.json') if (r/'coverage-summary.json').exists() else {'execution_complete':False}
  segments.append({'apk_identity':identity,'native_execution_complete':old.get('execution_complete') is True,'command_entries':sum(x['feature'].startswith('Command entry: ') for x in a)})
  for row in a:
   if not row['feature'].startswith('Command entry: ') and n==0:continue
   row=dict(row);name=f'segment-{n}-'+row['screenshot'];shutil.copyfile(r/row['screenshot'],out/name);row.update(screenshot=name,capture_run=identity['run']);rows.append(row)
  for f in r.iterdir():
   if f.suffix in {'.txt','.json'}:shutil.copyfile(f,out/(f'segment-{n}-'+f.name))
 summary={**tail_summary,'execution_complete':True,'execution_model':'Complete exact-ID census from independent native command segments; NOT one uninterrupted session','native_sessions_all_completed':False,'segments':segments,'command_entries':count,'features_recorded':len(rows),'full_functional_coverage':False,'interrupted_segment_preservation_check':'Not reached in the interrupted source run; not inferred from the independent continuation'}
 for name,data in [('feature-results.json',rows),('coverage-summary.json',summary),('feature-inventory.json',invs[1]),('apk-identity.json',identities[1])]: (out/name).write_text(json.dumps(data,indent=2))
 shutil.copyfile(roots[1]/'installed-package.txt',out/'installed-package.txt')
if __name__=='__main__':main(*sys.argv[1:])

"""Retain a bounded original session, not a polished or reconstructed UI export."""
from pathlib import Path
import hashlib,json,shutil,sys
source=Path(sys.argv[1]);run=sys.argv[2];root=Path(__file__).resolve().parents[3]
destination=root/'mynotes/docs/ux-improvement-ledger/sessions'/run
destination.mkdir(parents=True,exist_ok=True)
# Multi-directory artifact preserves paired-ux-evidence and onboarding separately.
candidates=list(source.rglob('session.json'))
if len(candidates)!=1:raise SystemExit('Expected one exact session.json; no app session evidence may be invented')
session=candidates[0].parent
paths=[p for p in session.iterdir() if p.is_file() and p.suffix in {'.json','.xml','.png','.txt'}]
size=sum(p.stat().st_size for p in paths)
if size>12*1024*1024:raise SystemExit('Session exceeds retained evidence bound; preserve artifact and review selection rather than duplicate a large corpus')
manifest=[]
for path in sorted(paths):
 target=destination/path.name;shutil.copyfile(path,target)
 manifest.append({'path':path.name,'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'source_bytes_modified':False})
(destination/'retention.json').write_text(json.dumps({'run':run,'source_url':'https://github.com/tarungittarun/TicTic/actions/runs/'+run,'artifact_name':'paired-ux-fresh-session','method':'Exact original files from fresh disposable emulator; no image alteration or reconstructed UI. Retention does not imply task acceptance.','files':manifest},indent=2))
print('Retained',len(paths),'original files;',size,'bytes')

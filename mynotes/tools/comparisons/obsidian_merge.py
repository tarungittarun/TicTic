"""Merge independent real-device sessions without hiding missing/failed groups."""
from pathlib import Path
import collections,json,shutil,os
src=Path('obsidian-sessions');out=Path('obsidian-evidence');out.mkdir(exist_ok=True)
rows=[];commands={};plugins={};settings={};sessions={};seen=set()
for phase in ['walkthrough','commands','settings','variants']:
 root=src/('obsidian-session-'+phase)
 summary=json.loads((root/'coverage-summary.json').read_text()) if (root/'coverage-summary.json').exists() else {'execution_complete':False}
 summary['apk_identity']=json.loads((root/'apk-identity.json').read_text()) if (root/'apk-identity.json').exists() else {}
 sessions[phase]=summary
 if summary['apk_identity']:assert summary['apk_identity']['apk_sha256']=='2d92966fba5441a8976ae86b39cf138c0a845d3dcd318e6f0d29d304950bca5d'
 if (root/'feature-results.json').exists():
  for row in json.loads((root/'feature-results.json').read_text()):
   if row['feature'] in seen:continue
   seen.add(row['feature']);name=phase+'-'+row['screenshot'];shutil.copyfile(root/row['screenshot'],out/name);row['screenshot']=name;row['session']=phase;rows.append(row)
 if (root/'feature-inventory.json').exists():
  inv=json.loads((root/'feature-inventory.json').read_text())
  for item in inv.get('commands',[]):commands[item['id']]=item
  for item in inv.get('plugins',[]):plugins[item['id']]=item
  for item in inv.get('settings',[]):settings[item['id']]=item
 for name in ['apk-identity.json','installed-package.txt']:
  if (root/name).exists() and not (out/name).exists():shutil.copyfile(root/name,out/name)
 for f in root.iterdir():
  if f.suffix in {'.txt','.json'}:shutil.copyfile(f,out/(phase+'-'+f.name))
identity=json.loads((out/'apk-identity.json').read_text());assert identity['apk_sha256']=='2d92966fba5441a8976ae86b39cf138c0a845d3dcd318e6f0d29d304950bca5d'
summary={'version':'1.15.0','run':os.environ['GITHUB_RUN_ID'],'api':36,'session_execution_complete':all(s.get('execution_complete') is True for s in sessions.values()),'full_functional_coverage':False,'evidence_entries':len(rows),'status_counts':dict(collections.Counter(r['status'] for r in rows)),'sessions':{p:{k:v for k,v in s.items() if k!='inventory'} for p,s in sessions.items()},'limits':['Entry invocation/inspection is not exhaustive functional acceptance.','Accounts/paid services/external integrations and community plugins are not fully tested.','A command needing another context remains blocked, not passed.','No physical-device acceptance.']}
(out/'feature-results.json').write_text(json.dumps(rows,indent=2));(out/'coverage-summary.json').write_text(json.dumps(summary,indent=2));(out/'feature-inventory.json').write_text(json.dumps({'commands':list(commands.values()),'plugins':list(plugins.values()),'settings':list(settings.values())},indent=2))
print(json.dumps(summary,indent=2))

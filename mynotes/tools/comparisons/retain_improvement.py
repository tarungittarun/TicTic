"""Bounded retention of public candidate qualification receipts; never APKs or app data."""
from pathlib import Path
import hashlib,json,re,shutil,sys,tarfile,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[3]
source,build,run=Path(sys.argv[1]),Path(sys.argv[2]),sys.argv[3]
if not run.isdigit():raise ValueError('Invalid run ID')
out=root/'mynotes/docs/ux-improvement-ledger/implementations'/run
out.mkdir(parents=True,exist_ok=True)
files=[];total=0

def retain(data,destination):
 global total
 total+=len(data)
 if total>24*1024*1024:raise ValueError('Evidence exceeds retention bound; do not silently truncate')
 path=out/destination;path.parent.mkdir(parents=True,exist_ok=True)
 if path.exists() and path.read_bytes()!=data:raise ValueError('Refuse to overwrite differing retained evidence')
 path.write_bytes(data)
 files.append({'path':str(destination),'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),'source_bytes_modified':False})

for lane in sorted(source.iterdir()):
 if not lane.is_dir() or not re.fullmatch(r'improvement70-api[0-9]+-(portrait|large|landscape)',lane.name):continue
 for path in sorted(lane.iterdir()):
  if path.is_file() and path.suffix in {'.txt','.json'}:
   retain(path.read_bytes(),Path(lane.name)/path.name)
 archive=lane/'refinement-captures.tar'
 if archive.exists() and archive.stat().st_size:
  with tarfile.open(archive) as tar:
   for member in tar:
    if not member.isfile():continue
    name=Path(member.name).name
    if not re.fullmatch(r'[a-z0-9-]{1,60}\.png|capture-owners\.jsonl',name):continue
    if member.size>3*1024*1024:raise ValueError('Oversized screenshot')
    retain(tar.extractfile(member).read(),Path(lane.name)/'captures'/name)

jvm={'tests':0,'failures':0,'errors':0,'skipped':0,'scope':'Actual JUnit XML totals; not native/UI or physical acceptance.'}
for path in build.rglob('TEST-*.xml'):
 tree=ET.parse(path).getroot()
 for key in ['tests','failures','errors','skipped']:jvm[key]+=int(tree.get(key,'0'))
 if path.name=='TEST-app.space.notes.domain.ImprovementLedgerTest.xml':retain(path.read_bytes(),Path('jvm')/path.name)
(out/'jvm-summary.json').write_text(json.dumps(jvm,indent=2)+'\n')
(out/'retention.json').write_text(json.dumps({'run':run,'url':'https://github.com/tarungittarun/TicTic/actions/runs/'+run,'scope':'Original public-fixture qualification artifacts. Instrumentation screenshots were captured/resized to 540px by the existing helper before artifact retention; no reconstruction or retention-time pixel alteration. Counts are not whole-app UX acceptance.','files':files},indent=2)+'\n')
print('Retained',len(files),'files,',total,'bytes;',jvm)

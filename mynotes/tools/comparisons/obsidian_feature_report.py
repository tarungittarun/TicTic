"""Evidence PDF: per-feature brief + real image + honest coverage classification."""
import collections,json,os,shutil,textwrap
from obsidian_coverage import command_coverage,command_brief,normalize_requested_variants
import re
from pathlib import Path
from PIL import Image
from reportlab.pdfgen.canvas import Canvas
from reportlab.lib.pagesizes import A4
src=Path(os.environ.get('OBSIDIAN_REPORT_INPUT','obsidian-evidence'));out=Path('mynotes/docs/comparisons/obsidian/audit')/os.environ['GITHUB_RUN_ID'];out.mkdir(parents=True,exist_ok=True)
rows=normalize_requested_variants(json.loads((src/'feature-results.json').read_text()));summary=json.loads((src/'coverage-summary.json').read_text()) if (src/'coverage-summary.json').exists() else {'full_functional_coverage':False,'interrupted':True}
c=Canvas(str(out/'Obsidian-Features-and-Screenshots.pdf'),pagesize=A4);c.setTitle('Obsidian 1.15.0 Android — feature walkthrough and coverage limits')
def lines(text,y,size=10,width=95):
 c.setFont('Helvetica',size)
 for line in textwrap.wrap(str(text),width=width):c.drawString(30,y,line);y-=size+4
 return y
c.setFont('Helvetica-Bold',18);c.drawString(30,805,'Obsidian 1.15.0 Android')
y=lines('Actual feature screenshots and coverage report',778,13)
y=lines('Official unmodified APK. Public dummy data only. Native emulator screenshots; no generated UI mockups. Feature inspection uses real app commands, CDP input and the app vault API, not an Obsidian-owned test suite.',y-14)
y=lines('Important: this is NOT 100% functional feature coverage. A screenshot proves the shown state, not every behavior. Paid/account/cloud services, desktop-only capabilities, community plugins, settings combinations and unexecuted commands are not certified.',y-14)
y=lines('Captured feature entries: '+str(len(rows))+'; status counts: '+str(dict(collections.Counter(r['status'] for r in rows))),y-14)
y=lines('Full per-feature briefs, actions/observations and screenshots follow. Successful harness execution is not a claim that every feature passed.',y-14)
y=lines('Source: github.com/obsidianmd/obsidian-releases/releases/tag/v1.15.0. Obsidian is a third-party product; no affiliation or endorsement is implied.',y-14)
c.showPage()
last_group=None
for i,r in enumerate(rows):
 group=r.get('session','inspection')
 if group!=last_group:
  key='group-'+group;c.bookmarkPage(key);c.addOutlineEntry(group.title()+' — actual screenshots',key,level=0,closed=True);last_group=group
 key='feature-'+str(i);c.bookmarkPage(key);c.addOutlineEntry(r['feature'],key,level=1)
 c.setFont('Helvetica-Bold',13);y=lines(str(i+1)+'. '+r['feature'],805,13,73)
 y=lines('Coverage: '+r['status'],y-4,10)
 cid=re.search(r'(?:Command=|^)([\w:-]+);',r['observed'])
 if cid:
  name=r['feature'].removeprefix('Command entry: ').removeprefix('Editor: ')
  y=lines('About the feature: '+command_brief(cid.group(1),name),y-3,10)
 y=lines('Check scope: '+r['brief'],y-3,9)
 y=lines('Observed: '+r['observed'],y-3,9)
 source_image=src/r['screenshot'];name=r['screenshot'].replace('.png','.jpg')
 if not source_image.exists():source_image=src/name
 if source_image.suffix.lower()=='.jpg':shutil.copyfile(source_image,out/name)
 else:
  im=Image.open(source_image).convert('RGB');im.thumbnail((1280,1280));im.save(out/name,quality=80,optimize=True)
 c.drawImage(str(out/name),30,44,width=535,height=max(200,y-65),preserveAspectRatio=True,anchor='c')
 c.setFont('Helvetica',8);c.drawString(30,23,'Real Android emulator capture | Obsidian 1.15.0 | '+os.environ['GITHUB_RUN_ID']);c.showPage()
c.save()
for name in ['feature-results.json','feature-inventory.json','coverage-summary.json','apk-identity.json','installed-package.txt','runtime-error.json','driver-error.txt','module-enablement.json','last-cdp-request.json','failure-logcat.txt']:
 if (src/name).exists():shutil.copyfile(src/name,out/name)
(out/'feature-results.json').write_text(json.dumps(rows,indent=2))
summary['status_counts']=dict(collections.Counter(r['status'] for r in rows))
summary['report_review_note']='Requested landscape Canvas captions are blocked when the paired real viewport check remained portrait; no image restyling or new device-test claim.'
(out/'coverage-summary.json').write_text(json.dumps(summary,indent=2))
# Full command census: every offered command is accounted for as inventoried, not automatically passed.
inv=json.loads((src/'feature-inventory.json').read_text()) if (src/'feature-inventory.json').exists() else {}
commands=command_coverage(inv.get('commands',[]),rows)
(out/'command-coverage.json').write_text(json.dumps(commands,indent=2))
c=Canvas(str(out/'Obsidian-Coverage-Matrix.pdf'),pagesize=A4)
c.setTitle('Obsidian 1.15.0 Android: command and core-module coverage matrix')
y=805
for text in ['Obsidian 1.15.0 - coverage matrix','This is not 100% functional acceptance.', 'Commands offered by the inspected mobile configuration: '+str(len(commands)),str(dict(collections.Counter(x['status'] for x in commands))), 'Paid services, credentials, community add-ons and all value combinations are not fully tested.']:
 y=lines(text,y,11,86)-8
for section,data in [('Core module definitions',inv.get('plugins',[])),('Command census',commands)]:
 y=lines(section,y-12,13,80)
 for row in data:
  text=(row.get('name') or row['id'])+' | '+row['id']+' | '+row.get('status','definition inventoried; enabled='+str(row.get('enabled')))
  wrapped=textwrap.wrap(text,width=103)
  if y-14*len(wrapped)<38:c.showPage();y=805
  y=lines(text,y,9,103)-5
c.save()
(out/'README.md').write_text('# Actual Obsidian Android feature inspection\n\nRead the PDF cover and per-feature status. This is not 100% functional acceptance. The command census is inventory, not executed-command proof. Screens are compressed copies of actual adb screenshots. APK is not redistributed.\n')

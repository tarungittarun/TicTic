"""Seed pending task/control inventory. Does not promote historical audit into testing."""
from pathlib import Path
import hashlib,json,re
ROOT=Path(__file__).resolve().parents[3]
OUT=ROOT/'mynotes/docs/ux-improvement-ledger';OUT.mkdir(parents=True,exist_ok=True)
ASPECTS=['page_navigation','feature_outcome','icon_discovery','visual_hierarchy','layout_and_keyboard','animation','efficiency','smoothness','timing','accessibility','save_and_recovery']
old=json.loads((ROOT/'mynotes/docs/comparisons/ourapp-vs-obsidian/all-modules-review.json').read_text())
plan=[]
for m in old['modules']:
 plan.append({'id':'PLAN-'+m['id'],'module':m['id'],'scope':m['scope'],'name':m['title'],'task':m['task'],'acceptance':m['acceptance'],'status':'not_tested_in_fresh_paired_study','aspects':{a:'not_tested' for a in ASPECTS},'source_audit_is_not_execution':True,'prior_audit_reference':'mynotes/docs/comparisons/ourapp-vs-obsidian/all-modules-review.json#'+m['id']})
base=ROOT/'mynotes/runtime/src/main/java/app/space/notes/runtime'
files=['NotesController.java','DocumentEditorView.java','FormatControls.java','InsertPalette.java','DrawingPad.java','DiagramView.java','LocalPdfPreview.java','SpreadsheetPreview.java','WordPageFile.java','NotesDesign.java','DiagnosticLogView.java','ImageTextSelectionDialog.java','ImageEditView.java','VoicePlaybackCard.java']
controls=[]
for name in files:
 path=base/name;text=path.read_text();digest=hashlib.sha256(path.read_bytes()).hexdigest();seen={}
 # Literal definitions only. Dynamic names, conditionals and OS dialogs need runtime discovery.
 pattern=r'\b(header|scrollContent|setTitle|setContentDescription|button|icon|tool|choiceSheet)\(\s*(?:\w+\s*,\s*)?"([^"\n]{1,140})"'
 for match in re.finditer(pattern,text):
  call,label=match.groups();key=call+'\0'+label;seen[key]=seen.get(key,0)+1
  relative=str(path.relative_to(ROOT));identity=relative+'\0'+key+'\0'+str(seen[key]);sid='SURF-'+hashlib.sha256(identity.encode()).hexdigest()[:12]
  controls.append({'id':sid,'app':'MyNotes','literal_label':label,'definition_kind':call,'path':relative,'line':text.count('\n',0,match.start())+1,'source_sha256':digest,'runtime_status':'not_tested','inventory_limit':'Literal source definition only; not proof this control is currently visible, reachable or invoked.'})
(OUT/'source-surfaces.json').write_text(json.dumps({'method':'Literal-source inventory, not acceptance. Dynamic/generated names, OS/provider UI and unvisited states are not exhaustively enumerated. Add runtime discoveries without renumbering existing items.','controls':controls},indent=2))
ledger=OUT/'ledger.json'
if not ledger.exists():
 ledger.write_text(json.dumps({'schema':1,'title':'MyNotes improvement execution ledger','target_app_source':'a014e015731103a041b092a0e3c2802ac2805d3a','comparison_scope':'Pinned MyNotes 0.69.0/code69 and official Obsidian Android 1.15.0/code396. No whole-app acceptance claimed.','plan':plan,'executions':[],'paired_observations':[],'work_items':[],'measurements':[],'untested_aspects':['physical devices','participant usability','precise touch latency','animation quality','frame jank','battery','all unexecuted plan tasks'],'fixes_implemented':False},indent=2))
print('Pending module tasks:',len(plan),'literal source definitions:',len(controls))

"""Render the living execution record, without promoting pending tasks to findings."""
from pathlib import Path
import json,hashlib
from html import escape
from reportlab.platypus import SimpleDocTemplate,Paragraph,Spacer,PageBreak,Table,TableStyle,Image,KeepTogether
from reportlab.lib.styles import getSampleStyleSheet,ParagraphStyle
from reportlab.lib import colors
from PIL import Image as PILImage
ROOT=Path(__file__).resolve().parents[3];OUT=ROOT/'mynotes/docs/ux-improvement-ledger'
data=json.loads((OUT/'ledger.json').read_text());styles=getSampleStyleSheet()
styles.add(ParagraphStyle(name='BodyLedger',fontSize=9,leading=12,spaceAfter=4));styles.add(ParagraphStyle(name='SmallLedger',fontSize=7.5,leading=9.5,spaceAfter=5));styles.add(ParagraphStyle(name='HeadLedger',keepWithNext=True,fontName='Helvetica-Bold',fontSize=15,leading=19,spaceAfter=10,textColor=colors.HexColor('#142B43')))
phase=data.get('phase_summary','No app fixes implemented.')
flow=[];md=['# MyNotes — improvement execution ledger','', '**Status: ongoing, incomplete. No whole-app or physical-device acceptance. '+phase+'**','', 'This document records fresh bounded tests in both apps before a comparative improvement is promoted. Earlier screenshot/source audits remain hypotheses, not new tests. Each named finding has its own scope, evidence, confidence and proposed acceptance task.','']
def p(text,style='BodyLedger'):return Paragraph(escape(str(text)),styles[style])
def para(text):flow.append(p(text))
def section(title,new=False):
 if new:flow.append(PageBreak())
 flow.append(p(title,'HeadLedger'));md.extend(['## '+title,''])
def block(label,text,style="BodyLedger"):
 text=str(text);flow.append(Paragraph('<b>'+escape(label)+':</b> '+escape(text),styles[style]));md.extend(['**'+label+':** '+text,''])
def table(rows,widths):
 t=Table([[p(x,'SmallLedger') for x in row] for row in rows],colWidths=widths,repeatRows=1);t.setStyle(TableStyle([('VALIGN',(0,0),(-1,-1),'TOP'),('BACKGROUND',(0,0),(-1,0),colors.HexColor('#E7F1F4')),('LINEBELOW',(0,0),(-1,-1),.3,colors.HexColor('#D6DFE6')),('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5)]));flow.append(t)
section('Improvement execution ledger')
para('ONGOING / INCOMPLETE — fresh bounded installed-app probes, not whole-app acceptance. '+phase+'  Native evidence and raw traces are retained in this repository; no user library was accessed.')
block('Document contract','Do not treat this document as a completed comparison of every control. Untested pages/features/aspects stay not tested. Knowing our source helps form a mechanism hypothesis; it does not prove the exclusive root cause or that a proposed fix works.')
block('Pinned baseline apps','MyNotes 0.69.0/code69, signed development APK SHA256 03d839d58b828ac72e211b69a1904e00e5f44f9f5c2b37704d4312501431d167; official Obsidian 1.15.0/code396 SHA256 2d92966fba5441a8976ae86b39cf138c0a845d3dcd318e6f0d29d304950bca5d.')
block('Method','Fresh scripted interactions on one disposable Actions emulator, followed by visual evidence review and source tracing. This is not a human participant study. UI inspection and command/API setup shortcuts are identified separately. No E2E pass total is interpreted as UX quality.')
block('Known measurement limits','Activity launch TotalTime is not interactive-ready time. Host command elapsed durations include inspection/adb overhead, not touch latency or human efficiency. Gfx/memory dumps are diagnostics, not a cross-engine smoothness rank. Animations, frame jank, battery and physical playback are not accepted without separate experiments.')
block('Implementation phase',phase)
block('Scope still outstanding',', '.join(data['untested_aspects']))
section('Actual runs and provenance',True)
rows=[['Run','Execution / scope']]
for run in data['executions']:
 rows.append([run['run'],run['status']+' — '+run['meaning']]);md.extend(['- ['+run['run']+']('+run['url']+'): '+run['status']+' — '+run['meaning'],''])
table(rows,[100,415])
for obs in data['paired_observations']:
 block(obs['id']+' / '+obs['task'],obs['summary'])
 md.extend(['Evidence: '+', '.join('['+ref+']('+ref+')' for ref in obs['evidence']),''])
section('Evidence review and corrected claims',True)
for review in data.get('evidence_reviews',[]):
 block(review['id'],review['conclusion'])
 md.extend(['Evidence: '+', '.join('['+ref+']('+ref+')' for ref in review['evidence']),''])
if data.get('candidate_qualification'):
 q=data['candidate_qualification'];section('Changed-build qualification — bounded, not closed',True)
 block('Current candidate',data['phase_summary']);block('Exact tested source',q['source_revision']);block('Scope and limits',q['scope'])
 block('JVM result',str(q['jvm']['tests'])+' tests; failures='+str(q['jvm']['failures'])+'; errors='+str(q['jvm']['errors'])+'; skipped='+str(q['jvm']['skipped']))
 table([['Native profile','Selected methods / actual result']]+[[r['lane'],str(r['passed'])+'/'+str(r['expected'])+'; exact receipt ok='+str(r['ok'])] for r in q['profiles']],[260,255])
 for item in data['work_items']:
  block(item['id']+' implementation',item['implementation_summary']);block('Implementation commits',item['implementation_commit']+' / '+item.get('followup_implementation_commit',''));block(item['id']+' candidate result',item['candidate_result']);block('Acceptance still outstanding',item['next_test'])
  md.extend(['Retest evidence: '+', '.join('['+r+']('+r+')' for r in item['retest_evidence']),''])
 for item in data['work_items']:
  section(item['id']+' | Candidate71 native evidence',True)
  para('Changed MyNotes build only. Public repository-seeded fixture; not a new paired speed/typing comparison. Capture helper resized actual screenshots to 540px before artifact retention; no UI reconstruction or retention-time pixel alteration. Left/right profile labels below.')
  images=[OUT/r for r in item['after_images']];cells=[];captions=[]
  for path in images:
   with PILImage.open(path) as image:w,h=image.size
   cells.append(Image(str(path),width=410*w/h,height=410))
   url='https://github.com/tarungittarun/TicTic/blob/'+q['retention_commit']+'/mynotes/docs/ux-improvement-ledger/'+str(path.relative_to(OUT))
   captions.append(Paragraph('<link href="'+escape(url,quote=True)+'" color="#195E91">'+escape(path.parent.parent.name)+' | open original</link>',styles['SmallLedger']))
  flow.append(Table([cells,captions],colWidths=[257.5,257.5]))
  para(item['candidate_result'])
section('Measured quantities — narrowly defined',True)
if not data['measurements']:block('No measurement result yet','Pending/blocked quantities are not assigned zeros or estimated values.')
for m in data['measurements']:
 block(m['id'],m['definition']);block('App / samples / units',m['app']+' | '+str(m['samples'])+' | '+m['unit']);block('Interpretation limit',m['limit']);md.extend(['Source: ['+m['source']+']('+m['source']+')',''])
section('Actionable improvements — evidence-gated',True)
if not data['work_items']:block('No promoted item yet','Wait for both-app evidence and review. Historical audit candidates are not silently imported as fresh findings.')
for idx,item in enumerate(data['work_items']):
 if idx:flow.append(PageBreak())
 flow.append(p(item['id']+' | '+item['title'],'HeadLedger'));md.extend(['### '+item['id']+' — '+item['title'],''])
 for label,key in [('Status','status'),('Owner / execution status','owner'),('Acceptance on changed build','acceptance_status'),('Bounded task / expected result','task'),('Preconditions and exact actions','reproduction'),('Baseline MyNotes actual result','ours'),('Baseline Obsidian actual result','obsidian'),('User consequence','impact'),('Reproduction and severity','severity'),('Root-cause confidence','cause_confidence'),('Baseline source-supported mechanism','cause'),('Alternative explanations / limits','alternatives'),('Original proposal','proposal'),('Protected requirements / risks','risks'),('Acceptance task','acceptance'),('Next discriminating test','next_test')]:block(label,item[key])
 source_start=len(flow)
 for s in item['source_refs']:
  block('Source reference',s['path']+':'+str(s['line'])+' '+s['symbol']+' | sha256='+s['sha256'],'SmallLedger')
  if s.get('immutable_url'):
   md.extend(['[View immutable source]('+s['immutable_url']+')',''])
   flow.append(Paragraph('<link href="'+escape(s['immutable_url'],quote=True)+'" color="#195E91">View immutable source and exact line</link>',styles['SmallLedger']))
 source_flow=flow[source_start:];del flow[source_start:]
 md.extend(['Evidence: '+', '.join('['+r+']('+r+')' for r in item['evidence']),''])
 images=[OUT/r for r in item['evidence'] if r.endswith('.png')][:2]
 if images:
  flow.append(PageBreak());flow.append(p(item['id']+' | Paired native evidence','HeadLedger'))
  para('BASELINE MyNotes 0.69.0 (left) / Obsidian 1.15.0 (right), NOT changed-build proof. This pair illustrates only the bounded state described in the work item; it is not a global speed or functionality ranking.')
  cells=[];captions=[]
  for path in images:
   with PILImage.open(path) as image:w,h=image.size
   cells.append(Image(str(path),width=410*w/h,height=410));run=path.parent.name;commit=data.get('evidence_commits',{}).get(run)
   url='https://github.com/tarungittarun/TicTic/blob/'+commit+'/mynotes/docs/ux-improvement-ledger/'+str(path.relative_to(OUT)) if commit else None
   captions.append(Paragraph('<link href="'+escape(url,quote=True)+'" color="#195E91">'+escape(path.name)+' | open original</link>',styles['SmallLedger']) if url else p(path.name,'SmallLedger'))
  flow.append(Table([cells,captions],colWidths=[257.5]*len(cells)));flow.append(p('Whole native screenshots; see evidence paths for exact original PNGs. No UI reconstruction.','SmallLedger'))
 flow.extend(source_flow)
section('Pending module task inventory — not findings',True)
block('Coverage rule','A bounded test on one page does not mark its whole module or all its aspects complete. 37 module plans and 231 literal source control definitions are starting inventories, not 268 completed tests or an exhaustive inventory of generated/OS states.')
block('Per-aspect scope','Pages/navigation, feature outcomes, icon discovery, visual hierarchy, layout/keyboard, animation, efficiency, smoothness, timing, accessibility and save/recovery are tracked separately. A screenshot does not pass any performance/gesture/assistive task.')
for plan in data['plan']:
 start=len(flow)
 block(plan['id']+' / '+plan['name'],plan['status']);block('Task still required',plan['task']);block('Acceptance',plan['acceptance']);block('Aspect statuses', '; '.join(k.replace('_',' ')+': '+v for k,v in plan['aspects'].items()))
 group=flow[start:];del flow[start:];flow.append(KeepTogether(group))
section('How this ledger must drive later fixes',True)
for label,text in [('Stable IDs','Do not renumber or delete items to make the record look clean. Add dated evidence, affected builds and changed confidence. Preserve blockers and failed attempts.'),('Before implementation','Confirm actual reproduction and source mechanism, isolate alternatives, define a minimal change and protect earlier requested behavior. Do not copy Obsidian architecture by default.'),('After implementation','Record the implementation commit, run the same task on the new signed app, retain before/after evidence, verify persistence and regressions, then change the item status. Proposed is not fixed.'),('Source inventory','source-surfaces.json inventories literal first-party definitions only. Dynamic names, generated controls, OS/provider dialogs and states not visited are not exhaustively enumerated.'),('No unsupported certainty','Never report general speed, smoothness, reliability, accessibility, user satisfaction or exclusive causality from the current bounded emulator evidence. Unknowns require named next tests, not confident filler.')]:block(label,text)
md.extend(['## Supporting records','- [Machine-readable ledger](ledger.json)','- [Literal source-surface inventory](source-surfaces.json)','- [Status and confidence definitions](README.md)',''])
(OUT/'Improvement_Execution_Ledger.md').write_text('\n'.join(md))
def footer(c,d):
 c.setFont('Helvetica',7);c.setFillColor(colors.HexColor('#526279'));c.drawString(40,23,'MyNotes improvement ledger | ongoing | candidate changes, acceptance incomplete');c.drawRightString(555,23,str(d.page))
class LedgerDoc(SimpleDocTemplate):
 def afterFlowable(self,item):
  if isinstance(item,Paragraph) and item.style.name=='HeadLedger':
   key='section-'+str(self.page)+'-'+str(len(item.getPlainText()));self.canv.bookmarkPage(key);self.canv.addOutlineEntry(item.getPlainText(),key,level=0,closed=False)
LedgerDoc(str(OUT/'Improvement_Execution_Ledger.pdf'),pagesize=(595.276,841.89),leftMargin=40,rightMargin=40,topMargin=40,bottomMargin=42,title='MyNotes Improvement Execution Ledger').build(flow,onFirstPage=footer,onLaterPages=footer)
print(OUT/'Improvement_Execution_Ledger.pdf')

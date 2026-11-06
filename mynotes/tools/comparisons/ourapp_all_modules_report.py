"""Build the all-module evidence review, keeping the original phase-one review intact.

Requires Pillow, ReportLab and PyMuPDF. Never launches, builds or changes either app.
Module inclusion and document checks must not be presented as live usability acceptance.
"""
from pathlib import Path
from html import escape
import collections
import hashlib
import json
import tempfile
import pymupdf
from PIL import Image as PILImage
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle, Image
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib import colors

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / 'mynotes/docs/comparisons/ourapp-vs-obsidian'
DATA = json.loads((OUT / 'all-modules-review.json').read_text())
ASSETS = DATA['assets']
STYLES = getSampleStyleSheet()
for name, size, leading, color, bold in [('body',9,11.5,'#24354A',False),('small',7.4,9.3,'#526279',False),('title',25,31,'#142B43',True),('heading',15,19,'#142B43',True),('sub',10.3,13,'#087E86',True)]:
    STYLES.add(ParagraphStyle(name='all-'+name,fontName='Helvetica-Bold' if bold else 'Helvetica',fontSize=size,leading=leading,spaceAfter=6,textColor=colors.HexColor(color)))
FLOW=[]
def paragraph(text,style='body'):
    return Paragraph(escape(str(text)),STYLES['all-'+style])
def para(text,style='body'):
    FLOW.append(paragraph(text,style))
def block(label,text,style='body'):
    FLOW.append(Paragraph('<b>'+escape(label)+'</b> '+escape(text),STYLES['all-'+style]))
def page(title):
    if FLOW:FLOW.append(PageBreak())
    para(title,'heading')
def table(rows,widths):
    t=Table([[paragraph(c,'small') for c in r] for r in rows],colWidths=widths,repeatRows=1,hAlign='LEFT')
    t.setStyle(TableStyle([('VALIGN',(0,0),(-1,-1),'TOP'),('BACKGROUND',(0,0),(-1,0),colors.HexColor('#E7F1F4')),('LINEBELOW',(0,0),(-1,-1),.35,colors.HexColor('#D6DFE6')),('LEFTPADDING',(0,0),(-1,-1),5),('RIGHTPADDING',(0,0),(-1,-1),5),('TOPPADDING',(0,0),(-1,-1),5),('BOTTOMPADDING',(0,0),(-1,-1),5)]))
    FLOW.append(t)
def figures(ids):
    cells=[];captions=[]
    for key in ids:
        item=ASSETS[key];path=ROOT/item['path']
        with PILImage.open(path) as image:w,h=image.size
        fig=Image(str(path),width=245*w/h,height=245);fig.hAlign='CENTER';cells.append(fig)
        captions.append(paragraph(key+' | '+item['app']+' | '+item['state'],'small'))
    if len(cells)==1:
        cells.append(paragraph('Context frame only. No matched comparator result is established for this tool. Do not interpret this blank comparison cell as a missing feature or a performance result.','small'))
        captions.append(paragraph('No matched interaction evidence','small'))
    t=Table([cells,captions],colWidths=[257.5,257.5]);t.setStyle(TableStyle([('VALIGN',(0,0),(-1,-1),'TOP'),('ALIGN',(0,0),(-1,0),'CENTER'),('BOTTOMPADDING',(0,0),(-1,-1),5)]));FLOW.append(t)

def footer(canvas,doc):
    canvas.saveState();canvas.setStrokeColor(colors.HexColor('#D6DFE6'));canvas.line(40,32,555,32);canvas.setFont('Helvetica',7);canvas.setFillColor(colors.HexColor('#526279'));canvas.drawString(40,21,'OurApp_vs_Obsidian | All-module evidence review | Live UX measurements outstanding');canvas.drawRightString(555,21,str(doc.page));canvas.restoreState()

class ReviewDoc(SimpleDocTemplate):
    def __init__(self,*args,**kwargs):
        super().__init__(*args,**kwargs);self.sections=[]
    def afterFlowable(self,item):
        if isinstance(item,Paragraph) and item.style.name=='all-heading':
            title=item.getPlainText();key='section-'+str(len(self.sections));self.canv.bookmarkPage(key);self.canv.addOutlineEntry(title,key,0,False);self.sections.append((title,self.page))

para('OurApp_vs_Obsidian','title')
para('All-module UX evidence review','heading')
para('MyNotes 0.69.0 / code 69 vs Obsidian Android 1.15.0 / code 396','sub')
para('Expanded 6 November 2026 | Comparison first; no implementation fixes','sub')
block('What is now included:', '37 functional-area chapters covering the identified MyNotes surfaces and every one of the 29 internal Obsidian module definitions in the retained version-pinned registry (28 exposed core controls). The original 20 detailed findings for the first two modules are preserved in the appendix.')
block('What “all modules” means:', DATA['scope']+' MyNotes here means the standalone notes app, not unrelated Clock/Calculator or other MySpace apps.')
block('What it does NOT mean:', 'No new live touch session, physical-device study, participant study, matched task timing, frame-jank trace or bug-reproduction run was performed. This is expanded evidence review, NOT completion of the proper fresh hands-on testing requested. Every proposed module task is explicitly NOT RUN.')
block('Main judgment:', 'Our strongest established weaknesses are obstructed/crowded task views, unclear scope and storage concepts, a constrained portable-backup model and retained recognition-quality limits. Competitor complexity, setup failures and untested services remain visible rather than being treated as automatic wins.')
block('Comparison rule:', 'Compare shared user jobs, not feature totals. OCR, voice transcription, widgets, graph tools, Bases and cloud services are described without pretending they have identical counterparts or awarding extra-feature credit.')
para('Whole native frames are proportionally placed, never recreated or retouched. A screenshot proves the captured state, not smoothness, correctness across all data or current-device bug recurrence.','small')
page('How to read the evidence and the result')
table([['Label','What it establishes / what it does not'],['Visible issue / constraint','A retained state can be inspected, e.g. obscured preview or tiny editor viewport. Frequency and current-device reproduction remain unknown.'],['Source-supported limitation','A named implementation branch or documented bound exists, e.g. portable backup 8 MiB. This is not a newly observed execution or crash.'],['Retained measured limit','An earlier exact-source qualification receipt records bounded recognition results. This is historical evidence, not a new user study or a score against Obsidian.'],['Expert inference / scoped gap','A plausible usability concern or equivalent route not established in this inspection. Absence of evidence is not proof of absence across the entire app.'],['Different scope / not ranked','Capabilities do not match directly, or require accounts/permissions. An entry screen is not successful completion.']],[145,370])
para('Presentation, efficiency, effectiveness, simplicity, discovery, feedback, safety, accessibility and performance are considered separately. There is no defensible overall star rating, pass percentage or speed winner from these unequal historical fixtures.')
block('Review extent:', '104 retained source frames are catalogued: 33 from phase one plus 71 additional unique frames inspected in contact sheets/context views. This does not mean every frame in either earlier audit was personally inspected. Source paths, dimensions and SHA-256 digests are in all-modules-evidence-manifest.json.')
block('Known comparison limits:', 'Earlier emulators, fixtures, keyboards, JPEG sizes and OS environments were not matched. Obsidian requested landscape stayed portrait. Its templates, audio recording and several context-dependent actions remained blocked/unverified. Our physical-device acceptance and recognition accuracy targets were not fully met.')
block('No fixes:', 'Only report data, report tooling and document-integrity tests changed. No Android build, installation, model training, login, purchase, cloud upload or app runtime modification was performed.')
page('Your five concerns — expanded assessment')
for label,text in [
 ('Unpolished existing features','Supported by retained obstruction/editor crowding and source/visual scope complexity. X01–X06, X22–X30 explain concrete risks without claiming every feature is broken.'),
 ('Missing functionality','Internal-note relations/outline and structured database workflows are candidate scope gaps. Exact equivalents must be verified before declaring absence. Optional specialised tools and third-party plugins do not become mandatory just because Obsidian has them.'),
 ('Pages are not simple','Library roles, row/block/table action scopes, appearance scopes and several storage/recovery concepts increase interpretation work. Proposed directions preserve the existing required tools rather than removing useful capability.'),
 ('155 MB but feels empty','APK payloads are 155,195,444 and 14,511,986 bytes (~10.7:1). This is not installed footprint/RAM or proof of slowness. Judge time-to-first-useful-outcome and discoverability, not whitespace or seeded note counts.'),
 ('Many bugs','Your report remains unresolved evidence to investigate. Candidate scope/empty-state cases, gesture failures, save/provider interruptions and physical playback need fresh reproduction. Historical test passes cannot refute reported friction.')]:block(label+':',text)
para('The review does not dismiss your concerns; it identifies what the retained evidence supports and names the tests still missing. A long report is not a substitute for running those tasks.','sub')
page('Triage: what should be validated before fixing')
table([['Order','Area / evidence','Why / next validation'],['1','N01 + E01 / X01–X02','Historical visible preview obstruction and writing-context collapse. Recreate on the delivered signed app; verify large text, keyboard and safe content area.'],['2','X18–X19','Retained OCR/speech targets not all met. Use varied real scenes/speakers and measure correction effort; do not claim extra features compensate.'],['3','X23–X25 + X27','8 MiB backup bound, omitted categories and several durability concepts. Verify understandable export/recovery on disposable rich fixtures.'],['4','N07–N09 + X01','Source-supported global scope/body-result/empty-state candidates. Run populated, filtered and archived cases and inspect the actual visible feedback.'],['5','X03–X17 + X28–X30','Control scope, viewer return, copy/import fidelity and action discovery. Execute equivalent jobs where possible and state non-equivalence where not.'],['Parallel','X22 + X32–X33','Matched touch/assistive/performance and failure studies. No latency, jank or physical reliability result exists yet.']],[42,145,328])
para('P1 means expert triage urgency, not observed incidence or a newly reproduced bug. Not ranked means the counterpart/result is not established; it does not mean the feature is unimportant.')
page('Module directory — complete expanded scope')
para('Each X chapter includes observed/reference evidence, comparative trade-offs, a scoped judgment, proposed direction and a specific manual task with acceptance checks. All live-task states remain NOT RUN. The earlier N/E detailed findings are in the appendix.')
table([['ID','Functional area','Triage']] + [[m['id'],m['title'],m['priority']] for m in DATA['modules']],[40,420,55])
page('Manual study design — not executed')
for label,text in [
 ('Matched setup','Use the same physical Android device/OS, font scale, keyboard, orientation, offline state and public dummy corpus. Pin both APK identities. Keep our installed user data untouched; do not uninstall/clear/downgrade the real library.'),
 ('Tasks rather than scripts','Ask an operator to reach the stated module outcome without a coached tap sequence. Record detours, wrong scopes, failed outcomes, lost context and recovery. Expert walkthroughs and participant studies must remain separately identified.'),
 ('Measurements','Time from deliberate input to visible response and to verified outcome; collect frame traces for drag/typing, cold/warm launch and file-open timings separately. Record task effectiveness, errors, correction time, help requests and reported confidence.'),
 ('Safety/failure probes','Use disposable data for interrupted writes, denied permissions, revoked document access and recovery. Verify original bytes and intended destination content. Do not simulate destructive failure against private user notes.'),
 ('Reporting','Publish observations, fixtures, exact versions, failures and non-equivalent tasks. Report account/paid/desktop/unsupported cases as blocked or out of scope, not zero scores. Do not imply a reproducible population statistic from a few demonstrations.'),
 ('What is available now','Visual/source inspection plus retained qualification limitations. Module inclusion and document checks are not new live usability results; every chapter below names the task still to run.')]:block(label+':',text)
for m in DATA['modules']:
    page(m['id']+' | '+m['title'])
    para(m['priority']+' | '+m['kind'],'sub')
    block('Scope:',m['scope'],'small')
    block('Evidence:',m['observed'])
    figures(m['evidence'])
    block('Comparator / fairness:',m['comparison'])
    block('Judgment:',m['judgment'])
    block('Direction, not an implemented fix:',m['direction'])
    block('Manual task — NOT RUN:',m['task'])
    block('Acceptance / remaining evidence:',m['acceptance'])
    src='; '.join(Path(s['path']).name+(':'+s['anchor'] if s['anchor'] else '') for s in m['sources'])
    block('Source anchors:',src,'small')
    para('Limit: no fresh live session or matched timing. Full source paths/hashes and historical provenance are in the manifest. Different or missing counterpart evidence is not an automatic win.','small')
page('Obsidian core-module census — no missing registry entries')
para('Exact retained registry: 29 internal definitions, 28 exposed controls. These rows route every known module to a chapter; they are not 29 functional passes. Enabled means the historical inspected audit state, not the user’s settings. Publish was disabled; Sync still needs an account. Internal editor-status is not a separate exposed toggle.')
table([['Core module ID','Chapter','Audit enabled / control']] + [[r['id'],r['module'],('Yes' if r['enabled'] else 'No')+' / '+('exposed' if r['exposed_control'] else 'internal')] for r in DATA['obsidian_module_registry']],[210,55,250])
page('Settings-section coverage and additional boundaries')
para('All 20 retained settings-section overviews are routed below. Their 377 catalogue entries include initial corpus records, section/control entries and hotkeys; they are not 377 distinct accepted workflows. Individual settings-value combinations, touch accessibility and novice discovery were not newly exercised.')
table([['Retained section','Chapter','Evidence status']] + [[r['section'],r['primary_module'],r['status']] for r in DATA['obsidian_settings_sections']],[215,65,235])
para('The pinned Android registry is the census boundary, not a claim about every desktop feature, Web Clipper, external integration, undocumented feature, future release or community extension. Those require their own authorised scope and evidence; no absent runtime module is silently declared unsupported.')
page('Command registry routing — references, not new acceptance')
para('All 205 retained registered commands are listed with their original evidence class and a primary functional-family route. Routing is not a fresh per-command UX review; many editor commands have cross-cutting scopes. The original audit had 27 narrow functional readbacks, 129 UI-only and 49 blocked/unverified entries. No pass percentage or usability score is derived.')
short={'functional_readback_pass':'readback','invoked_ui_inspected':'UI only','blocked_or_unverified':'blocked'}
table([['Command ID / name','Chapter','Original class']] + [[r['id']+' | '+r['name'],r['module'],short.get(r['status'],r['status'])] for r in DATA['obsidian_command_routing']],[375,45,95])
page('Source-frame catalogue')
para('Frames are historical screenshots, not new captures. Whole images are proportionally embedded; no fake UI, hidden cropping, recolouring or reconstructed controls. Several chapters intentionally use two same-app context frames when a matched counterpart is unavailable; app names remain in captions. Source-only summaries/widgets are explicitly not illustrated as successful outputs.')
table([['ID','App','Captured state']] + [[k,'MyNotes' if a['app'].startswith('MyNotes') else 'Obsidian',a['state']] for k,a in ASSETS.items()],[45,75,395])
page('Detailed first-two-module appendix follows')
para('The following 39 pages preserve the original phase-one report and its 20 detailed N01–N10/E01–E10 findings, figures, provenance limits and T01–T08 protocol. Their original “two modules only” wording describes that earlier installment; the X01–X37 chapters above now expand the module scope.')
para('The appendix is not a second live study. Its historical baseline and old proposed-next-module text are retained for transparency, not used to claim the remaining modules are still absent from this expanded document.')
para('Companions: all-modules-review.json; all-modules-evidence-manifest.json; report-integrity.json; OurApp_vs_Obsidian.md. Original phase-one data and standalone PDF remain independently reproducible.')

manifest={}
for key,a in ASSETS.items():
    path=ROOT/a['path']
    with PILImage.open(path) as im:w,h=im.size
    manifest[key]={**a,'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'width':w,'height':h,'source_pixels_modified':False}
sources={}
for m in DATA['modules']:
    for s in m['sources']:
        path=ROOT/s['path'];text=path.read_text()
        for anchor in s['anchor'].split('/'):
            if anchor:assert anchor in text,(m['id'],anchor)
        sources[s['path']]=hashlib.sha256(path.read_bytes()).hexdigest()
(OUT/'all-modules-evidence-manifest.json').write_text(json.dumps({'baseline_revision':DATA['baseline_revision'],'method':DATA['method'],'source_frame_count':len(manifest),'new_live_sessions':0,'new_runtime_bugs_reproduced':0,'matched_timing_results':None,'assets':manifest,'referenced_source_file_hashes':sources},indent=2))
with tempfile.TemporaryDirectory(prefix='all-module-review-') as tmp:
    temp=Path(tmp)/'expanded.pdf';doc=ReviewDoc(str(temp),pagesize=(595.276,841.89),leftMargin=40,rightMargin=40,topMargin=40,bottomMargin=43,title='OurApp_vs_Obsidian — all modules',author='MyNotes comparative UX evidence review')
    doc.build(FLOW,onFirstPage=footer,onLaterPages=footer)
    main=pymupdf.open(temp);prefix_pages=len(main);original_toc=main.get_toc();appendix=pymupdf.open(OUT/'OurApp_vs_Obsidian_Phase1.pdf');main.insert_pdf(appendix)
    # Report footer only, never a screenshot region. Keep a single continuous page sequence.
    for number in range(prefix_pages,len(main)):
        p=main[number];p.draw_rect(pymupdf.Rect(36,801,560,841.89),color=None,fill=(1,1,1));p.draw_line((40,809.89),(555,809.89),color=(.84,.88,.91),width=.5)
        p.insert_text((40,821),'OurApp_vs_Obsidian | Historical phase-one appendix | No fresh live UX timings',fontsize=7,color=(.32,.38,.47));p.insert_text((535,821),str(number+1),fontsize=7,color=(.32,.38,.47))
    toc=original_toc+[[1,'Historical phase-one appendix',prefix_pages+1]]+[[2,title,prefix_pages+n] for _,title,n in appendix.get_toc()]
    main.set_toc(toc);main.set_metadata({'title':'OurApp_vs_Obsidian — all-module UX evidence review','author':'MyNotes comparison','subject':'37 functional families; live usability testing explicitly outstanding'})
    main.save(OUT/'OurApp_vs_Obsidian.pdf',garbage=4,deflate=True)

pdf=pymupdf.open(OUT/'OurApp_vs_Obsidian.pdf');module_pages={}
for m in DATA['modules']:
    needle=' '.join((m['id']+' | '+m['title']).split());matches=[i+1 for i,p in enumerate(pdf) if needle in ' '.join(p.get_text().split())]
    assert len(matches)==1,(m['id'],matches);n=matches[0];text=pdf[n-1].get_text();assert 'Acceptance / remaining evidence:' in text and 'Limit:' in text,(m['id'],'split chapter');module_pages[m['id']]=n
for i,p in enumerate(pdf):
    for b in p.get_text('dict')['blocks']:
        for line in b.get('lines',[]):
            for span in line['spans']:
                x0,y0,x1,y1=span['bbox'];assert min(x0,y0)>=-1 and x1<=p.rect.width+1 and y1<=p.rect.height+1,(i+1,span['text'])
summary={'pages':len(pdf),'expanded_pages':prefix_pages,'phase1_appendix_pages':len(appendix),'module_chapters':len(DATA['modules']),'module_pages':module_pages,'obsidian_internal_modules':len(DATA['obsidian_module_registry']),'obsidian_exposed_controls':sum(r['exposed_control'] for r in DATA['obsidian_module_registry']),'retained_commands_routed':len(DATA['obsidian_command_routing']),'settings_sections_routed':len(DATA['obsidian_settings_sections']),'catalogued_source_frames':len(ASSETS),'one_complete_page_per_module_chapter':True,'all_text_within_page_bounds':True,'new_live_sessions':0,'matched_timing_results':None,'new_runtime_bugs_reproduced':0,'sha256':hashlib.sha256((OUT/'OurApp_vs_Obsidian.pdf').read_bytes()).hexdigest(),'note':'Document/source integrity checks are not fresh app usability acceptance.'}
(OUT/'report-integrity.json').write_text(json.dumps(summary,indent=2))
md=['# OurApp_vs_Obsidian — all-module evidence review','',DATA['scope'],'',DATA['method'],'','**No fresh live usability testing has been completed. All chapter tasks are NOT RUN. No speed/jank/crash-frequency ranking. No app fixes.**','','## Scope',f"37 chapters; all 29 internal pinned Obsidian definitions, 28 exposed controls; 205 commands and 20 settings sections routed. These are coverage references, not feature-count scores. Main PDF: {len(pdf)} pages, including the historical 39-page phase-one appendix.",'']
for m in DATA['modules']:
    md += ['## '+m['id']+' — '+m['title'],m['priority']+' | '+m['kind'],'']
    for label,key in [('Scope','scope'),('Evidence','observed'),('Comparator/fairness','comparison'),('Judgment','judgment'),('Direction, not implemented','direction'),('Manual task — NOT RUN','task'),('Acceptance / remaining evidence','acceptance')]:md += ['**'+label+':** '+m[key],'']
    md += ['Frames: '+', '.join(m['evidence']),'','Sources: '+ '; '.join(s['path']+(':'+s['anchor'] if s['anchor'] else '') for s in m['sources']),'']
md += ['## Retained Obsidian modules','']+['- '+p['id']+' → '+p['module']+'; historical enabled='+str(p['enabled']) for p in DATA['obsidian_module_registry']]
md += ['','## Detailed first-two-module findings','See OurApp_vs_Obsidian_Phase1.md and the PDF appendix. Companion manifests contain full source paths and SHA-256 values. Module inclusion is not functional acceptance.']
(OUT/'OurApp_vs_Obsidian.md').write_text('\n'.join(md))
print(json.dumps(summary,indent=2))

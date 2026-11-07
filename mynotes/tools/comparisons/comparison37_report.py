"""Preserve and report real paired-module attempts without turning inspections into acceptance."""
import argparse
from collections import Counter
import hashlib
import gzip
from html import escape
import json
from pathlib import Path

PROFILES = ['light-portrait', 'dark-portrait', 'light-landscape', 'dark-landscape']
MAX_BYTES = 96 * 1024 * 1024


def retain(artifacts, output, metadata):
    artifacts, output = Path(artifacts), Path(output)
    if metadata['status'] != 'completed':
        raise ValueError('Unfinished execution cannot be final comparison evidence')
    output.mkdir(parents=True, exist_ok=True)
    manifest, profiles, total = [], {}, 0
    expected = {('PLAN-X%02d'%i, app) for i in range(1,38) for app in ['mynotes','obsidian']}
    source_bytes=0;retained_paths=set()
    for profile in PROFILES:
        prefix='comparison37-'+profile
        lanes=sorted(lane for lane in artifacts.glob(prefix+'*') if lane.is_dir() and (lane.name==prefix or lane.name.startswith(prefix+'-chunk')))
        rows=[];module_files=[]
        for lane in lanes:
            records=list(lane.rglob('modules.json'))
            if len(records)>1:raise ValueError('Ambiguous original module record')
            if records:
                data=json.loads(records[0].read_text())
                if data.get('whole_module_acceptance') is not False or data.get('physical_acceptance') is not False:raise ValueError('Unsupported acceptance claim')
                rows.extend(data['results']);module_files.append(str(Path(lane.name)/records[0].relative_to(lane)))
            for source in sorted(lane.rglob('*')):
                if not source.is_file() or source.suffix not in {'.json','.png','.xml','.txt','.pdf','.docx','.pptx','.odt','.rtf','.html','.md','.csv'}:continue
                data=source.read_bytes();source_bytes+=len(data);digest=hashlib.sha256(data).hexdigest()
                relative=Path(lane.name)/source.relative_to(lane)
                if 'build' in relative.parts:raise ValueError('Ignored build directory would lose evidence')
                # Repeated public input bytes across fresh guests are kept once,
                # content-addressed. Original paths and every hash remain in the
                # receipt; no screenshot, XML or module record is deduplicated.
                retained=Path('public-fixtures-by-sha256')/(digest+'-'+source.name) if source.parent.name=='public-fixtures' else relative
                compressed=source.suffix in {'.txt','.json','.xml'} and source.name!='modules.json' and len(data)>65536
                stored=gzip.compress(data,compresslevel=9,mtime=0) if compressed else data
                if compressed:
                    assert gzip.decompress(stored)==data,'Lossless original storage verification failed'
                    retained=Path(str(retained)+'.gz')
                destination=output/retained;destination.parent.mkdir(parents=True,exist_ok=True)
                if retained not in retained_paths:
                    retained_paths.add(retained);total+=len(stored)
                    if total>MAX_BYTES:raise ValueError('Original evidence retention exceeds96MiB; do not silently truncate')
                if destination.exists():
                    if destination.read_bytes()!=stored:raise ValueError('Refuse to overwrite different originals')
                else:destination.write_bytes(stored)
                manifest.append({'path':str(relative),'retained_path':str(retained),'bytes':len(data),'stored_bytes':len(stored),'storage_encoding':'gzip' if compressed else 'identity','sha256':digest,'source_bytes_modified':False})
        keys=[(r['module'],r['app']) for r in rows]
        if len(keys)!=len(set(keys)) or not set(keys)<=expected:raise ValueError('Duplicate/unknown paired result across chunks')
        profiles[profile]={'module_file':module_files[0] if module_files else None,'module_files':module_files,
            'attempts':len(rows),'fresh_guest_sessions':len(lanes),
            'all74_attempt_records_present':set(keys)==expected and all(r['status']!='running' for r in rows),
            'counts':dict(Counter(r['status'] for r in rows)),
            'requested_tasks_accepted':sum(r.get('requested_task_acceptance') is True for r in rows),
            'complete_original_snapshot_records':sum('snapshot' in r and not r.get('capture_error') for r in rows),
            'records_with_capture_error':sum(bool(r.get('capture_error')) for r in rows),
            'target_app_present_in_capture_records':sum(r.get('capture_contains_target_app') is True for r in rows),
            'capture_owner_fields_recorded':sum('captured_packages' in r for r in rows)}
        if not module_files:profiles[profile]['error']='No original modules.json; no app coverage inferred'
    summary={'run':metadata['databaseId'],'source':metadata['headSha'],'url':metadata['url'],
        'workflow_conclusion':metadata['conclusion'],'profiles':profiles,
        'all_profiles_have74_attempt_records':all(p['all74_attempt_records_present'] for p in profiles.values()),
        'whole_module_acceptance':False,'physical_acceptance':False,'fix_phase_authorized_by_results':False,
        'scope':'37 task families attempted per profile where provisioning succeeded. Page visits, API shortcuts, functional checks and blockers remain separate. This is NOT completion of the original37 full acceptance tasks.'}
    (output/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    (output/'run.json').write_text(json.dumps(metadata,indent=2)+'\n')
    (output/'retention.json').write_text(json.dumps({'files':manifest,'bytes':total,'source_bytes':source_bytes,'public_fixture_deduplication':'Byte-identical public inputs share a content-addressed retained_path; large text records use verified lossless gzip. All original entries, original byte counts and uncompressed hashes retained.','source_bytes_modified':False},indent=2)+'\n')
    return summary


def render(evidence, commit):
    from reportlab.platypus import SimpleDocTemplate, Paragraph, Table, TableStyle, Spacer, PageBreak, Image
    from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
    from reportlab.lib import colors
    from PIL import Image as PILImage
    evidence=Path(evidence).resolve();root=Path(__file__).resolve().parents[3]
    summary=json.loads((evidence/'summary.json').read_text())
    plans=json.loads((root/'mynotes/docs/ux-improvement-ledger/ledger.json').read_text())['plan']
    base='https://github.com/tarungittarun/TicTic/blob/'+commit+'/'+str(evidence.relative_to(root))+'/'
    styles=getSampleStyleSheet();styles.add(ParagraphStyle(name='Small37',fontSize=8,leading=10,spaceAfter=5))
    flow=[];md=['# Installed-app37-family paired comparison probes','']
    def p(text,style='BodyText'):return Paragraph(escape(str(text)),styles[style])
    def paragraph(text):flow.extend([p(text),Spacer(1,7)]);md.extend([str(text),''])
    def heading(text,new=False):
        if new:flow.append(PageBreak())
        flow.append(p(text,'Heading1'));md.extend(['## '+text,''])
    def link(label,url):
        flow.append(Paragraph('<link href="'+escape(url,quote=True)+'" color="#185A92">'+escape(label)+'</link>',styles['Small37']))
        md.extend(['['+label+']('+url+')',''])
    heading('Installed-app comparison — runtime evidence, acceptance still open')
    paragraph('Pinned MyNotes0.71.0/code71 release APK SHA256 a7d6f685992c68fe4ffc7f3a5705d0514986c9d9fd82ca67c6f68d392ca41de3; official Obsidian1.15.0/code396 SHA256 2d92966fba5441a8976ae86b39cf138c0a845d3dcd318e6f0d29d304950bca5d. Neither APK is rebuilt or re-signed. Fresh public disposable emulator fixtures only, no user library/account access.')
    paragraph('READ FIRST: runtime page inspection is not a functional pass,74 attempt records are not74 accepted tasks, and these probes do not complete the original37 requested acceptance tasks. All whole-module, physical and fix-phase approval flags remain false. Failures and unavailable routes are preserved, not assigned zero feature scores or interpreted as feature absence.')
    link('Exact Actions run, source and job outcomes',summary['url']);link('Machine-readable coverage and limitations',base+'summary.json');link('Original-file SHA256 manifest',base+'retention.json')
    rows=[['Profile','Attempts /74','Actual outcome types']]
    for profile in PROFILES:
        row=summary['profiles'][profile];rows.append([profile,str(row['attempts']),str(row.get('counts',row.get('error')))])
    table=Table([[p(c,'Small37') for c in row] for row in rows],colWidths=[120,75,320],repeatRows=1)
    table.setStyle(TableStyle([('VALIGN',(0,0),(-1,-1),'TOP'),('BACKGROUND',(0,0),(-1,0),colors.HexColor('#E7F1F4')),('LINEBELOW',(0,0),(-1,-1),.3,colors.lightgrey)]));flow.append(table)
    for profile in PROFILES:
        info=summary['profiles'][profile]
        paragraph(profile+' capture health: '+str(info.get('complete_original_snapshot_records',0))+' complete original snapshot records; '+str(info.get('records_with_capture_error',0))+' capture errors. Capture-owner fields exist for '+str(info.get('capture_owner_fields_recorded',0))+' records. Zero owner fields in older originals means unrecorded, not that no app frames exist.')
    paragraph('Short-session evidence may span several fresh guests per profile. This is disclosed setup isolation, NOT proof that a long session is reliable. Repeated public inputs may share a SHA256-addressed retained_path in the manifest; every original entry and its exact uncompressed hash is retained. Large text records may use verified lossless gzip to avoid a huge duplicate dataset. Modules and PNG screenshots remain directly readable; the manifest identifies each stored path and encoding.')
    paragraph('Methods differ and are disclosed: MyNotes public note is entered using native touch/input and reopened after process restart; Obsidian public fixtures are seeded using its official vault API, and command/settings APIs navigate some surfaces. Offscreen selector targets may be scrolled into view by the harness before touch. Those shortcuts are not user-route, unaided discoverability, creation-efficiency or latency evidence. Native screens are real adb captures; no UI reconstruction. Standard font scale1.0; light/dark portrait/landscape at API34, not physical phones or accessibility acceptance.')
    if summary['run']==42342628990:paragraph('Fixture validity warning: this first run generates incomplete DOCX/ODT/PPTX ZIP containers. Any failure on those files is INVALID as an app-format or fidelity finding until standards-valid fixtures are rerun. The PDF is a small synthetic3-page file, not the requested18MB+ varied-document acceptance corpus.')
    else:paragraph('Public synthetic fixture bytes are retained with hashes where generated before the failure. Office packages use real format libraries; these are still small test documents, not complex-layout fidelity or18MB+ PDF acceptance.')
    paragraph('Activity launch milestones and gfx/memory diagnostics, if present, are NOT time-to-ready, touch latency, human completion time, animation quality, battery or a cross-engine performance ranking. Accounts, Sync/Publish service operation and community-plugin execution are not performed. Original task text and actual smaller probe are printed separately on every page.')
    for profile in PROFILES:
        info=summary['profiles'][profile]
        if not info.get('module_file') or not info['attempts']:
            heading(profile+' — provisioning blocked',True);paragraph(info.get('error','Zero paired attempts; provisioning failed.'))
            for lane in evidence.glob('comparison37-'+profile+'*'):
                for fatal in lane.rglob('fatal.json'):
                    paragraph(fatal.read_text());link('Original provisioning failure',base+str(fatal.relative_to(evidence)))
            continue
        records={};origins={}
        for relative in info.get('module_files') or [info['module_file']]:
            module_file=evidence/relative;data=json.loads(module_file.read_text())
            for row in data['results']:
                key=(row['module'],row['app']);records[key]=row;origins[key]=module_file
        for plan in plans:
            heading(plan['id']+' | '+plan['name']+' | planned '+profile,True)
            paragraph('Original acceptance task (NOT automatically accepted): '+plan['task'])
            screenshots=[]
            for app in ['mynotes','obsidian']:
                row=records.get((plan['id'],app))
                if row is None:paragraph(app+': NOT EXECUTED — no original record.');screenshots.append(p('No original capture','Small37'));continue
                paragraph(app+' — '+row['status']+'. Actual bounded check: '+row.get('check',row.get('error','No verified outcome'))[:800])
                if row.get('captured_image_profile'):paragraph('Original PNG profile: '+json.dumps(row['captured_image_profile'])+'; planned orientation is not accepted when the original image disagrees.')
                if row.get('observed_profile'):paragraph('Actual WebView profile observation: '+json.dumps(row['observed_profile'])+'; failed profile assertions remain blocked, never a landscape/theme pass.')
                if row.get('capture_error'):paragraph('Capture blocker: '+row['capture_error'])
                module_file=origins[(plan['id'],app)]
                link(app+' original exact result record',base+str(module_file.relative_to(evidence)))
                filename=module_file.parent/(app+'-'+plan['id']+'.png')
                if filename.exists():
                    with PILImage.open(filename) as image:w,h=image.size
                    scale=min(250/w,290/h);screenshots.append(Image(str(filename),width=w*scale,height=h*scale))
                    link(app+' original screenshot',base+str(filename.relative_to(evidence)))
                else:screenshots.append(p('No original capture','Small37'))
                if row.get('subtasks'):
                    paragraph('Format subtasks: '+'; '.join(r.get('fixture','')+':'+r['status'] for r in row['subtasks']))
            flow.append(Table([screenshots],colWidths=[257.5,257.5]))
            paragraph('No whole-family acceptance. Remaining: '+plan['acceptance'])
    (evidence/'Comparison37_Runtime_Probes.md').write_text('\n'.join(md))
    def footer(canvas,doc):
        canvas.setFont('Helvetica',7);canvas.drawString(40,23,'Paired runtime probes | not whole-module or physical acceptance');canvas.drawRightString(555,23,str(doc.page))
    destination=evidence/'Comparison37_Runtime_Probes.pdf'
    SimpleDocTemplate(str(destination),pagesize=(595.276,841.89),leftMargin=40,rightMargin=40,topMargin=36,bottomMargin=42,title='MyNotes71 and Obsidian1.15 Runtime Comparison Probes').build(flow,onFirstPage=footer,onLaterPages=footer)
    return destination


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('mode',choices=['retain','render']);parser.add_argument('--source');parser.add_argument('--output',required=True);parser.add_argument('--metadata');parser.add_argument('--commit');args=parser.parse_args()
    if args.mode=='retain':print(json.dumps(retain(args.source,args.output,json.loads(Path(args.metadata).read_text())),indent=2))
    else:print(render(args.output,args.commit))

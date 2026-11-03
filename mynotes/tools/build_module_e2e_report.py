"""Separate module-ordered actual E2E + ground-truth/performance report.
Never substitutes expected counts for observations or scores a clean hypothesis as truth.
"""
import argparse,hashlib,io,json,math,os,re,shutil,statistics,tarfile,unicodedata
from pathlib import Path,PurePosixPath
from collections import defaultdict
from mynotes.tools.build_e2e_report import methods,describe,ROOT
from mynotes.tools.ci.quality_reports import distance
from mynotes.tools.ci.render_review import fixtures

PROFILES=['light-portrait','dark-portrait','light-landscape','dark-landscape']
METHOD_PLACEMENT=json.loads((ROOT/'mynotes/docs/module-e2e-method-order.json').read_text())
METHOD_MODULE={name:module for module,names in METHOD_PLACEMENT.items() for name in names}
MODULES=['Notes and checklists','Rich Doc and tables','Diagrams','Read image / OCR','Voice inbox and transcripts','Images and drawing','File viewers','Home, search and Library','Folders and storage','Backup, history and Trash','Reminders','Settings and diagnostics']

def module_for(name):
    if name in METHOD_MODULE:return METHOD_MODULE[name]
    text=name.lower()
    for module,pattern in [(3,r'ocr|imageselection|read.image'),(4,r'voice|speech|whisper|recording|transcript'),(2,r'diagram|shape|connector'),(11,r'diagnostic|settings|appearance|about|theme|environment'),(10,r'reminder|notification|alarm'),(9,r'backup|restore|history|trash|archive|generationprotocol'),(8,r'folder|storage|repository|persistence|transfer'),(6,r'viewed|spreadsheet|pdf|wordprepared|filepicker|reader'),(5,r'image|drawing|visualzoom'),(7,r'home|search|filter|library|shareandlabel|workspac|motion'),(1,r'doc|table|content|canonical|stage')]:
        if re.search(pattern,text):return MODULES[module]
    return MODULES[0]

SCENE_ORDER=json.loads((ROOT/'mynotes/docs/module-e2e-scene-order.json').read_text())
SCENE_MODULE={scene:module for module,scenes in SCENE_ORDER.items() for scene in scenes}

def scene_module(scene,owner):
    if scene in SCENE_MODULE:return SCENE_MODULE[scene]
    if scene.startswith(('e2e-library-','e2e-page-')):return module_for(scene)
    if scene in ['word-synthetic-paper-page']:return MODULES[6]
    if scene.startswith('reminder-'):return MODULES[10]
    return module_for(owner or scene)

def normalize(text):return re.sub(r'[ \t\r\n\f\v]+',' ',re.sub(r'^[\x00-\x20]+|[\x00-\x20]+$','',unicodedata.normalize('NFC',text)))
def strict_words(text):return text.lower().split(' ') if text else []
def fair(text):return re.sub("[^a-z0-9']+",' ',normalize(text).lower()).strip()
def independent_score(row,module):
    if row['status']!='OK':return
    a,b=normalize(row['reference']),normalize(row['recognized'])
    for key,reference,hypothesis in [('cer',a,b),('strict_wer',strict_words(a),strict_words(b))]+([('normalized_wer',fair(a).split(),fair(b).split())] if module=='voice' else []):
        if max(len(reference),len(hypothesis))>4096:raise ValueError('Score input exceeds evaluation bound')
        measured=distance(reference,hypothesis)
        if not math.isfinite(row[key]) or abs(row[key]-measured)>1e-8:raise ValueError('Independent ground-truth score mismatch: '+row['id']+' '+key)
    target=(row['cer']<=.2) if module=='ocr' else (row['normalized_wer']<=.35 if row['reference'] else not row['recognized'].strip())
    if row.get('accuracy_target_met') is not target:raise ValueError('False accuracy flag: '+row['id'])
    if row['elapsed_ms']<0:raise ValueError('Negative elapsed time')

def sha(data):return hashlib.sha256(data).hexdigest()
def write(path,data):path.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')
def ingest(artifacts,output,source,run):
    from PIL import Image
    Image.MAX_IMAGE_PIXELS=16000000
    inventory=methods(source);lanes={};screens=[];observations=[];executions={}
    images=output/'images';images.mkdir(parents=True,exist_ok=True)
    def preview(raw,dest):
        if not 0<len(raw)<=8*1024*1024:raise ValueError('Unbounded image')
        with Image.open(io.BytesIO(raw)) as original:
            if original.width*original.height>16000000:raise ValueError('Unbounded geometry')
            image=original.convert('RGB');image.thumbnail((720,1280),Image.Resampling.LANCZOS)
            for quality in [78,60,40,25]:
                stream=io.BytesIO();image.save(stream,'JPEG',quality=quality,optimize=True)
                if stream.tell()<=85000:break
            data=stream.getvalue()
            if len(data)>85000:raise ValueError('Unbounded preview')
            dest.write_bytes(data)
            return {'width':image.width,'height':image.height,'sha256':sha(data),'original_png_sha256':sha(raw),'path':str(dest.relative_to(output))}
    for folder in sorted(artifacts.glob('module-native-*')):
        lane=folder.name.removeprefix('module-native-');report=folder/'refinement-native.json'
        lanes[lane]=json.loads(report.read_text()) if report.exists() else {'ok':False,'cases':[],'failures':['No result artifact']}
        archive=folder/'refinement-captures.tar'
        if archive.is_file() and archive.stat().st_size:
            with tarfile.open(archive) as bundle:
                for scene,raw in fixtures(bundle):
                    candidates=[n for n,m in inventory.items() if scene in m['scenes'] and n in lanes[lane].get('cases',[])]
                    if scene.startswith(('e2e-page-','e2e-library-')):candidates=['app.space.notes.CoreJourneysTest#wholeAppLibraryTraversalRecordsEachVisitedPage']
                    helper={'content-item-drag':'app.space.notes.CoreJourneysTest#canonicalChecklistHandleTapIsContextualAndNativeDragPreservesRichRows','image-text-selection':'app.space.notes.runtime.ImageSelectionTest#controllerSelectionChangesOnlyReviewUntilExplicitAppend'}
                    if scene in helper:candidates=[helper[scene]]
                    owner=candidates[0] if len(candidates)==1 else None
                    screens.append({'scene':scene,'lane':lane,'profile':next(p for p in PROFILES if lane.startswith(p+'-')),'owner':owner,'module':scene_module(scene,owner),**preview(raw,images/(lane+'-'+scene+'.jpg'))})
        for file in folder.glob('refinement-environment-*.json'):shutil.copyfile(file,output/(lane+'-'+file.name))
    for folder in sorted(artifacts.glob('module-benchmark-*')):
        lane=folder.name.removeprefix('module-benchmark-');profile=next((p for p in PROFILES if lane.startswith(p+'-')),None)
        if profile is None:raise ValueError('Unknown benchmark profile')
        result=folder/'module-execution.json';executions[lane]=json.loads(result.read_text()) if result.exists() else {'ok':False,'failures':['No execution artifact']}
        archive=folder/'module-benchmark.tar'
        if not archive.exists() or not archive.stat().st_size:continue
        with tarfile.open(archive) as bundle:
            count=0
            for member in bundle:
                if not member.isfile():continue
                path=PurePosixPath(member.name)
                if path.parts[0]!='module-benchmark' or '..' in path.parts:raise ValueError('Unsafe benchmark artifact path')
                if path.suffix=='.json' and len(path.parts)==2:
                    if member.size>4*1024*1024:raise ValueError('Oversized report')
                    report=json.loads(bundle.extractfile(member).read());module='ocr' if path.stem=='ocr' else 'voice'
                    for row in report['cases']:
                        independent_score(row,module);observations.append({**row,'module':module,'profile':profile,'lane':lane})
                    write(output/(lane+'-measurements.json'),report)
                elif path.suffix=='.png' and profile=='light-portrait':
                    count+=1
                    if count>384 or not re.fullmatch('ocr-\\d{4}',path.stem):raise ValueError('Unexpected OCR specimen')
                    raw=bundle.extractfile(member).read(8*1024*1024+1)
                    meta=preview(raw,images/(path.stem+'.jpg'));meta.update(scene=path.stem,profile='synthetic-input-not-UI',module=MODULES[3]);screens.append(meta)
    foundation=artifacts/'foundation'
    for file in foundation.rglob('refinement-jvm.json'):shutil.copyfile(file,output/'executed-jvm.json')
    for file in foundation.rglob('trained-whisper.json'):shutil.copyfile(file,output/'experimental-model-provenance.json')
    for file in foundation.rglob('manifest.json'):shutil.copyfile(file,output/'public-corpus-provenance.json')
    for file in foundation.rglob('*-COPYING.txt'):shutil.copyfile(file,output/file.name)
    expected_lanes={p+'-'+s for p in PROFILES for s in ['a','b']};expected_bench={p+'-'+s for p in PROFILES for s in ['ocr','voice0','voice1']}
    native_ok=(set(lanes)==expected_lanes or set(lanes)=={p+'-ordered' for p in PROFILES}) and all(r.get('ok') for r in lanes.values())
    for p in PROFILES:
        actual=[n for lane,r in lanes.items() if lane.startswith(p+'-') for n in r.get('cases',[])]
        native_ok=native_ok and len(actual)==len(set(actual))==len(inventory) and set(actual)==set(inventory)
    expected_measurements={(p,m):384 if m=='ocr' else 804 for p in PROFILES for m in ['ocr','voice']}
    complete=set(executions)==expected_bench and all(r.get('ok') for r in executions.values())
    for (p,m),count in expected_measurements.items():
        rows=[r for r in observations if r['profile']==p and r['module']==m]
        complete=complete and len(rows)==len({r['id'] for r in rows})==count and all(r['status']=='OK' for r in rows)
    quality_ok=bool(complete) and bool(observations) and all(r.get('accuracy_target_met') is True for r in observations)
    result={'schema':1,'source':source,'run':run,'native_complete':bool(native_ok),'benchmark_complete':bool(complete),'all_accuracy_targets_met':quality_ok,'profiles':PROFILES,'native_methods':len(inventory),'native_outcomes':sum(r.get('passed',0) for r in lanes.values()),'observed_inferences':len(observations),'expected_inferences':4752,'native':lanes,'benchmark_executions':executions,'screens':screens,'model_changed':False,'training_steps':0,'physical_acceptance':False}
    write(output/'provenance.json',result);write(output/'measurements.json',observations)
    return result,observations,inventory

def performance_summary(observations):
    groups=defaultdict(list)
    for row in observations:groups[(row['profile'],row['module'],row['kind'])].append(row)
    result=[]
    for (profile,module,kind),rows in sorted(groups.items()):
        good=[r for r in rows if r['status']=='OK'];times=sorted(r['elapsed_ms'] for r in good)
        item={'distinct_hashed_inputs':len({r['input_sha256'] for r in rows if r.get('input_sha256')}),'unhashed_inputs':sum(not r.get('input_sha256') for r in rows),'distinct_reference_texts':len({r['reference'] for r in rows}),'profile':profile,'module':module,'kind':kind,'observed':len(rows),'errors':len(rows)-len(good),'accuracy_target_passes':sum(bool(r.get('accuracy_target_met')) for r in rows)}
        if times:
            item.update(median_ms=statistics.median(times),p95_ms=times[math.ceil(.95*len(times))-1],minimum_ms=min(times),maximum_ms=max(times),total_elapsed_ms=sum(times),input_bytes_min=min(r['input_bytes'] for r in good),input_bytes_max=max(r['input_bytes'] for r in good),sampled_java_heap_max_bytes=max((r.get('heap_used_bytes',0) for r in good),default=0),sampled_native_heap_max_bytes=max((r.get('native_heap_bytes',0) for r in good),default=0))
            key='cer' if module=='ocr' else 'normalized_wer';item['mean_'+key]=statistics.mean(r[key] for r in good)
            # Silence is reported separately; do not dilute a corpus WER denominator.
            total_reference=0;total_edits=0
            for row in good:
                ref=normalize(row['reference']) if module=='ocr' else fair(row['reference']).split();hyp=normalize(row['recognized']) if module=='ocr' else fair(row['recognized']).split()
                if ref:total_reference+=len(ref);total_edits+=round(distance(ref,hyp)*len(ref))
            item['micro_error_rate_excluding_empty_reference']=total_edits/total_reference if total_reference else None
            if module=='voice':
                seconds=sum(r['duration_seconds'] for r in good);item.update(total_audio_seconds=seconds,duration_seconds_min=min(r['duration_seconds'] for r in good),duration_seconds_max=max(r['duration_seconds'] for r in good),aggregate_real_time_factor=sum(times)/1000/seconds)
            else:item['input_pixel_counts']=sorted({r['pixels'] for r in good})
        result.append(item)
    return result


def voice_speaker_summary(observations):
    groups=defaultdict(list)
    for row in observations:
        if row['module']=='voice' and row.get('speaker') and row['status']=='OK':groups[(row['profile'],row['speaker'])].append(row)
    result=[]
    for (profile,speaker),rows in sorted(groups.items()):
        clean=[r for r in rows if r['kind']=='clean'];seconds=sum(r['duration_seconds'] for r in rows)
        result.append({'profile':profile,'speaker':speaker,'observed':len(rows),'distinct_source_clips':len({r['source_sha256'] for r in rows}),'distinct_prompts':len({r['reference'] for r in rows}),'mean_normalized_wer':statistics.mean(r['normalized_wer'] for r in rows),'clean_mean_normalized_wer':statistics.mean(r['normalized_wer'] for r in clean) if clean else None,'accuracy_target_passes':sum(r['accuracy_target_met'] for r in rows),'processed_audio_seconds':seconds,'elapsed_ms':sum(r['elapsed_ms'] for r in rows),'aggregate_real_time_factor':sum(r['elapsed_ms'] for r in rows)/1000/seconds})
    return result


def apply_recheck(data,observations,proof,output):
    """Accept exact-source setup corrections without erasing rejected history."""
    if proof['artifact_source_sha']!=data['source'] or proof['artifact_source_run']!=data['run']:raise ValueError('Recheck must use exact original build')
    rows=proof['measurements'];module='ocr' if rows and rows[0]['id'].startswith('ocr-') else 'voice'
    lane=proof.get('lane','dark-landscape-ocr' if module=='ocr' else None)
    if lane not in data['benchmark_executions'] or not lane.startswith(proof['profile']+'-'):raise ValueError('Unknown correction lane')
    old=data['benchmark_executions'][lane];execution=proof['execution']
    if old['ok'] or not old.get('failures') or any('EnvironmentQualificationTest#orientationMatchesRequestedLane:' not in failure for failure in old['failures']):raise ValueError('Only observed orientation setup failures may be replaced')
    if not execution['ok'] or execution['passed']!=4 or execution.get('failures'):raise ValueError('Recheck must pass unchanged four-case assertions')
    originals=[r for r in observations if r['lane']==lane];expected={r['id']:r for r in originals}
    if len(rows)!=len(originals) or len(rows)!=len({r['id'] for r in rows}) or {r['id'] for r in rows}!=set(expected):raise ValueError('Correction cohort must match exactly')
    accepted=[]
    for row in rows:
        previous=expected[row['id']]
        if any(row.get(k)!=previous.get(k) for k in ['reference','input_sha256','source_sha256','input_bytes','duration_seconds','kind']):raise ValueError('Correction changed inputs or ground truth')
        if row['status']!='OK':raise ValueError('Operational correction errors')
        independent_score(row,module);accepted.append({**row,'module':module,'profile':proof['profile'],'lane':lane,'capture_run':proof['run']})
    if not (output/'original-provenance.json').exists():write(output/'original-provenance.json',data)
    write(output/(lane+'-rejected-original-observations.json'),{'reason':'Real Notes Activity orientation was portrait, not requested landscape. These model measurements are retained but excluded from accepted landscape cohort.','run':data['run'],'rows':originals})
    write(output/(lane+'-accepted-recheck.json'),proof)
    observations[:]=[r for r in observations if r['lane']!=lane]+accepted
    for row in observations:row.setdefault('capture_run',data['run'])
    data.setdefault('rechecks',[]).append({k:v for k,v in proof.items() if k!='measurements'})
    data['benchmark_executions'][lane]=execution
    data['benchmark_complete']=all(r['ok'] for r in data['benchmark_executions'].values()) and len(observations)==4752 and all(r['status']=='OK' for r in observations)
    data['all_accuracy_targets_met']=data['benchmark_complete'] and all(r['accuracy_target_met'] for r in observations)
    data['total_inferences_including_rejected']=len(observations)+sum(len(json.loads(p.read_text())['rows']) for p in output.glob('*-rejected-original-observations.json'))
    write(output/'provenance.json',data);write(output/'measurements.json',observations)


def specimen_crop(image):
    """Report-only whitespace crop; never changes scored images or their hashes."""
    from PIL import Image,ImageChops
    image=image.convert('RGB');background=Image.new('RGB',image.size,image.getpixel((0,0)))
    bounds=ImageChops.difference(image,background).convert('L').point(lambda p:255 if p>30 else 0).getbbox()
    if bounds:
        x0,y0,x1,y1=bounds;bounds=(max(0,x0-8),max(0,y0-8),min(image.width,x1+8),min(image.height,y1+8))
        return image.crop(bounds),True
    return image.copy(),False


def report_markup(value):
    from xml.sax.saxutils import escape
    # Report-only OFL fonts: preserve shaped Hindi/Kannada, not missing-glyph boxes.
    runs=[];font=None;part=''
    for character in str(value):
        code=ord(character);selected='Devanagari' if 0x900<=code<=0x97f else 'Kannada' if 0xc80<=code<=0xcff else 'Body'
        if selected!=font and part:runs.append((font,part));part=''
        font=selected;part+=character
    if part:runs.append((font,part))
    return ''.join('<font name="'+font+'">'+escape(text).replace('\n','<br/>')+'</font>' for font,text in runs)


def render(data,observations,inventory,output):
    from reportlab.pdfgen.canvas import Canvas
    from reportlab.pdfbase import pdfmetrics
    from reportlab.pdfbase.ttfonts import TTFont
    from reportlab.platypus import Paragraph
    from reportlab.lib.styles import ParagraphStyle
    from xml.sax.saxutils import escape
    pdfmetrics.registerFont(TTFont('Body','/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'))
    for script in ['Devanagari','Kannada']:pdfmetrics.registerFont(TTFont(script,str(ROOT/'mynotes/report-assets/fonts'/('NotoSans'+script+'-Regular.ttf')),shapable=True))
    style=ParagraphStyle('body',fontName='Body',fontSize=9,leading=12,shaping=1)
    small=ParagraphStyle('small',parent=style,fontSize=7,leading=10)
    c=Canvas(str(output/'MyNotes-Module-E2E-Report.pdf'),pagesize=(842,595),invariant=1);page=0;module_pages=[]
    def new(title,bookmark=None):
        nonlocal page
        if page:c.showPage()
        page+=1;c.setFillColorRGB(.07,.15,.25);c.rect(0,536,842,59,fill=1,stroke=0);c.setFillColorRGB(1,1,1);c.setFont('Body',15);c.drawString(30,564,title[:96]);c.setFont('Body',7);c.drawString(30,546,'ACTUAL MODULE E2E · source '+data['source'][:12]+' · no training / no physical acceptance');c.setFillColorRGB(.1,.15,.2);c.setFont('Body',7);c.drawString(30,16,'Evidence bundle '+data['run']+' · page '+str(page))
        if bookmark:c.bookmarkPage(bookmark);c.addOutlineEntry(title,bookmark,0)
        return 518
    def text(value,y,x=30,width=782,tiny=False):
        paragraph=Paragraph(report_markup(value),small if tiny else style);_,height=paragraph.wrap(width,10000)
        if height>450:raise ValueError('Paragraph exceeds bounded page')
        if y-height<35:y=new('Module results · continued')
        paragraph.drawOn(c,x,y-height);return y-height-9
    y=new('Separate module-wise E2E and recognition performance', 'start')
    y=text('Native complete: '+str(data['native_complete'])+' · Corpus execution complete: '+str(data['benchmark_complete'])+' · ALL accuracy targets met: '+str(data['all_accuracy_targets_met']),y)
    y=text(f"{data['native_methods']} source-discovered native methods; {data['native_outcomes']} actual native outcomes. {data['observed_inferences']}/{data['expected_inferences']} model observations. Four explicitly paired API34 profiles: light/dark × portrait/landscape. No model change or training in this baseline.",y)
    if data.get('strict_native_run'):y=text('Strict native module schedule run '+data['strict_native_run']+' validates all419 methods per profile in chapter order. Original native run remains separately retained. Native screenshots below identify their actual capture run.',y)
    if data.get('rechecks'):y=text('SETUP CORRECTIONS: original run failed landscape orientation checks. Accepted cohorts below include explicitly source-matched rechecks '+', '.join(sorted({r['run'] for r in data['rechecks']}))+'. Original failed configuration and rejected observations remain retained; no failed run is relabelled successful.',y)
    y=text('Reading order / PDF bookmarks\n'+'\n'.join(f'{i+1:02d}. {m}' for i,m in enumerate(MODULES)),y)
    y=text('Accuracy targets fixed before execution: OCR CER≤0.20, normalized English speech WER≤0.35, silence must remain empty. These are measurement flags, not an assertion that every difficult input passes. Failures, hallucinations, errors and missing observations remain visible. Execution completion is not an accuracy pass.',y)
    y=new('Corpus, measurement and visibility boundaries','limits')
    for value in ['OCR:384 deterministic rendered conditions, 8 content kinds ×4 font families ×3 sizes ×2 polarities ×2 geometry/rotation settings. Two input sizes:1.12M/2.4M pixels. Input polarity is separate from app theme. Synthetic fonts are not physical photos/handwriting, universal scripts or the supplied private image. Input font variety does not qualify recovered font/style fidelity.', 'Speech:200 distinct licensed CMU ARCTIC utterances,100 each KSP/SLT, four conditions plus four silence controls =804 inputs, repeated on four UI profiles. Only2 speakers reading100 distinct prompts, not804 people or804 independent scripts. English read speech; not spontaneous dictation, all accents, multilingual or microphone/noise capture. Default bundled Vosk is measured; large experimental Whisper corpus evaluation remains unqualified.', 'Ground truth is the original public transcript or generated source text, never the recognizer’s own clean output. CER uses NFC plus ASCII-whitespace normalization and remains case-sensitive. Strict WER additionally lowercases with punctuation retained. English normalized WER additionally replaces everything except a–z, digits and apostrophes with separators; it does not normalize number words or contractions. Silence uses denominator1 count convention, not ordinary corpus accuracy.', 'Timing is emulator wall clock for recognition plus scoring and transcript codec/dedup/source-preservation checks; fixture synthesis excluded. Initialization is included; Vosk constructs and closes its native model for every transcription, while materialized model files may be warm. Different CI hosts/load make physical-latency or causal theme-speed comparisons invalid. Report actual ms, bytes, pixels/audio seconds, RTF and sampled heaps; sampled heap is not peak process memory.', 'Visibility evidence: Native screenshots are paired by orientation and light/dark profile. Recorded resource assertions check real night/orientation. Screenshots support review, not automatic WCAG certification, vendor keyboard coverage or physical gesture approval.', 'Production runtime/models remain v57 baseline; this opt-in instrumentation source set and public assets never ship in the application. No private logs, supplied screenshots, notes or recordings enter Actions.']:
        y=text(value,y)
    for index,module in enumerate(MODULES):
        y=new(f'{index+1:02d}. {module}','module-'+str(index));module_pages.append({'module':module,'page':page})
        members={n:m for n,m in inventory.items() if module_for(n)==module}
        y=text(f'{len(members)} native methods assigned to this module. Captures are ordered by owning source workflow, then scene; actual selected profile outcomes follow.',y)
        for name,method in sorted(members.items(),key=lambda item:(item[1]['file'],item[1]['line'])):
            passed=[lane for lane,r in data['native'].items() if name in r.get('cases',[])];failed=[lane for lane,r in data['native'].items() if any(str(f).startswith(name+':') for f in r.get('failures',[]))]
            y=text(name.split('#')[-1]+f"\nActual passed shards:{', '.join(passed) or 'NONE'}; failed:{', '.join(failed) or 'none reported'}. Source {method['file']}:{method['line']}.\nActions: "+(' → '.join(method['actions']) or 'Direct runtime/storage assertions; no UI route inferred.'),y,tiny=True)
        scene_groups=defaultdict(list)
        for frame in data['screens']:
            if frame.get('lane') and frame['module']==module:scene_groups[frame['scene']].append(frame)
        guide={}
        for path in [ROOT/'mynotes/docs/user-guide/screen-guide.tsv',ROOT/'mynotes/docs/e2e-workflow-captions.tsv']:
            for line in path.read_text().splitlines():
                if line and not line.startswith('#'):
                    key,*description=line.split('|');guide[key]=tuple(description)
        for scene,frames in sorted(scene_groups.items(),key=lambda item:(SCENE_ORDER.get(module,[]).index(item[0]) if item[0] in SCENE_ORDER.get(module,[]) else 99999,item[0])):
            route,purpose,boundary=describe(scene,guide)
            for orientation in ['portrait','landscape']:
                y=new(module+' · '+scene+' · '+orientation)
                y=text(route+'\n'+purpose+'\nBoundary: '+boundary,y)
                for column,polarity in enumerate(['light','dark']):
                    profile=polarity+'-'+orientation;frame=next((f for f in frames if f['profile']==profile),None);x=30+column*401
                    text(profile+(' · capture run '+frame.get('capture_run',data['run'])+' · '+str(frame.get('owner')) if frame else ' · NO ACTUAL CAPTURE'),y,x,380,True)
                    if frame:
                        image=output/frame['path'];assert sha(image.read_bytes())==frame['sha256'];c.drawImage(str(image),x,45,380,max(80,y-90),preserveAspectRatio=True,anchor='c')
        if module in [MODULES[3],MODULES[4]]:
            kind='ocr' if module==MODULES[3] else 'voice';rows=[r for r in observations if r['module']==kind]
            if kind=='voice':
                y=new(module+' · speaker-specific descriptive results')
                y=text('Two speakers only: KSP Indian-English and SLT US-English, reading the same100 prompts. This is not a population, gender or general-accent comparison. Short read utterances, not long spontaneous dictation. All four amplitude/noise/gain conditions are counted; clean-only WER is separate.',y)
                for item in voice_speaker_summary(rows):y=text(f"{item['profile']} / {item['speaker']}: {item['observed']} observations, {item['distinct_source_clips']} source clips, {item['distinct_prompts']} prompts; targets {item['accuracy_target_passes']}/{item['observed']}; mean normalized WER all conditions={item['mean_normalized_wer']:.3f}, clean={item['clean_mean_normalized_wer']:.3f}; {item['processed_audio_seconds']:.2f}s processed in {item['elapsed_ms']}ms, aggregate RTF={item['aggregate_real_time_factor']:.3f}.",y,tiny=True)
            y=new(module+' · measured performance summary')
            for item in performance_summary(rows):
                score='cer' if kind=='ocr' else 'normalized_wer'
                y=text(f"{item['profile']} / {item['kind']}: n={item['observed']}, errors={item['errors']}, target passes={item['accuracy_target_passes']}/{item['observed']}. Distinct hashed inputs={item['distinct_hashed_inputs']} (unhashed controls={item['unhashed_inputs']}), distinct reference texts={item['distinct_reference_texts']}.",y,tiny=True)
                if 'median_ms' in item:
                    amount=(f"pixels={item['input_pixel_counts']}" if kind=='ocr' else f"PCM16 mono16kHz, {item['duration_seconds_min']:.2f}–{item['duration_seconds_max']:.2f}s per input, {item['total_audio_seconds']:.2f}s processed, aggregate RTF={item['aggregate_real_time_factor']:.3f}")
                    y=text(f"INPUT {item['input_bytes_min']}–{item['input_bytes_max']} bytes; {amount}. TIME median={item['median_ms']:.0f}ms, p95={item['p95_ms']}ms, range={item['minimum_ms']}–{item['maximum_ms']}ms, total={item['total_elapsed_ms']}ms. Mean {score}={item['mean_'+score]:.3f}; micro error excluding empty reference={item['micro_error_rate_excluding_empty_reference']}.",y,tiny=True)
            by_id=defaultdict(list)
            for row in rows:by_id[row['id']].append(row)
            for rejected_file in output.glob('*-rejected-original-observations.json'):
                for rejected in json.loads(rejected_file.read_text())['rows']:
                    if rejected['module']==kind:by_id[rejected['id']].append({**rejected,'capture_run':data['run'],'configuration_rejected':True})
            specimens={f['scene']:f for f in data['screens'] if f.get('profile')=='synthetic-input-not-UI'}
            y=0
            for identity,samples in sorted(by_id.items()):
                y=new(module+' · '+identity) if kind=='ocr' or y<260 else text('Next measured input · '+identity,y)
                first=samples[0];y=text('Reference: '+first['reference']+('\nInput: '+first.get('font','')+f" {first.get('font_px')}px, {first.get('width')}×{first.get('height')}, dark={first.get('input_dark')}, rotation={first.get('rotation_degrees')}°" if kind=='ocr' else f"\n{first.get('speaker','silence')} / {first['kind']} · {first.get('duration_seconds')}s PCM16 mono16kHz"),y)
                if identity in specimens:
                    frame=specimens[identity]
                    from PIL import Image
                    from reportlab.lib.utils import ImageReader
                    with Image.open(output/frame['path']) as image:crop,cropped=specimen_crop(image)
                    c.drawImage(ImageReader(crop),30,y-135,782,125,preserveAspectRatio=True,anchor='c');y-=145
                    y=text('Input preview: whitespace cropped for legibility; scored source dimensions, pixels and hashes unchanged.' if cropped else 'Blank control preview; no foreground ink detected.',y,tiny=True)
                for row in sorted(samples,key=lambda r:PROFILES.index(r['profile'])):
                    score=row.get('cer') if kind=='ocr' else row.get('normalized_wer');rate=f'{score:.3f}' if isinstance(score,(float,int)) else 'UNAVAILABLE'
                    y=text(f"{('REJECTED ORIENTATION — requested ' if row.get('configuration_rejected') else '')+row['profile']} · run {row.get('capture_run',data['run'])} · {row['status']} · {'CER' if kind=='ocr' else 'normalized WER'} {rate} · {row['elapsed_ms']}ms · {row.get('input_bytes')} bytes · RTF {row.get('real_time_factor','n/a')} · target {row.get('accuracy_target_met',False)}\nActual: "+row.get('recognized','')+(' · ERROR '+row['error'] if 'error' in row else ''),y,tiny=True)
    write(output/'performance-summary.json',performance_summary(observations));write(output/'voice-speaker-summary.json',voice_speaker_summary(observations))
    import csv
    if observations:
        keys=sorted(set().union(*(row.keys() for row in observations)))
        with (output/'measurements.csv').open('w',newline='') as stream:
            writer=csv.DictWriter(stream,fieldnames=keys);writer.writeheader();writer.writerows(observations)
    c.save();pdf=output/'MyNotes-Module-E2E-Report.pdf';summary={k:v for k,v in data.items() if k not in ['screens','native','benchmark_executions']};summary.update(pages=page,pdf_sha256=sha(pdf.read_bytes()),module_order=module_pages)
    write(output/'report-summary.json',summary)
    return summary

def main():
    p=argparse.ArgumentParser();p.add_argument('--artifacts',type=Path);p.add_argument('--rebuild',action='store_true');p.add_argument('--rechecks',type=Path,nargs='*',default=[]);p.add_argument('--output',type=Path,required=True);p.add_argument('--source',required=True);p.add_argument('--run',required=True);a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
    if a.rebuild:
        data=json.loads((a.output/'provenance.json').read_text());rows=json.loads((a.output/'measurements.json').read_text());inventory=methods(data['source'])
        if data['source']!=a.source or data['run']!=a.run:raise ValueError('Rebuild cannot relabel source/run')
        for frame in data['screens']:
            if frame.get('lane'):frame['module']=scene_module(frame['scene'],frame.get('owner'))
        write(a.output/'provenance.json',data)
    else:
        if a.artifacts is None:raise ValueError('Artifacts required for real ingestion')
        data,rows,inventory=ingest(a.artifacts,a.output,a.source,a.run)
    for path in a.rechecks:
        proof=json.loads(path.read_text())
        for entry in (proof if isinstance(proof,list) else [proof]):apply_recheck(data,rows,entry,a.output)
    summary=render(data,rows,inventory,a.output);print(json.dumps(summary,indent=2))
if __name__=='__main__':main()

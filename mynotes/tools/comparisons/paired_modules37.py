"""Installed-app, module-by-module comparison probes on PUBLIC disposable fixtures.

Each result names its exact bounded check. Visiting a page is not a functional
pass, a missing route is not proof of missing capability, and 37 attempted task
families are not exhaustive UX/physical acceptance. No app builds or fixes here.
"""
import base64
import hashlib
import json
import os
from pathlib import Path
import time
import re
import shutil

import paired_ux_session as h
from obsidian_cdp import Device
from obsidian_probe import main as onboard

OUT = Path('comparison37-evidence')
OUT.mkdir(exist_ok=True)
h.OUT = OUT
TITLE = 'Comparison37 Meeting'
BODY = 'Public comparison fixture. Decision marker cobalt42 belongs here. Keep the original text unchanged.'
h.TITLE = TITLE
h.BODY = BODY
h.MARKER = 'cobalt42'
ROOT = Path(__file__).resolve().parents[3]
PLANS = json.loads((ROOT/'mynotes/docs/ux-improvement-ledger/ledger.json').read_text())['plan']
RESULTS = []
D = None
PATH = 'Comparison37/Meeting.md'
FIXTURES = Path('/tmp/comparison37-fixtures')
FIXTURES.mkdir(exist_ok=True)


def save():
    (OUT/'modules.json').write_text(json.dumps({
        'schema': 1, 'source': os.environ.get('GITHUB_SHA'), 'run': os.environ.get('GITHUB_RUN_ID'),
        'profile': os.environ.get('COMPARISON_PROFILE'), 'results': RESULTS,
        'whole_module_acceptance': False, 'physical_acceptance': False,
        'paired_task_families_planned': 37,
        'scope': 'Bounded live probes; functional readback, UI inspection, unavailable routes and blockers remain distinct. '
                 'Command/vault APIs are disclosed setup/navigation shortcuts, not human efficiency evidence. '
                 'No accounts, external services, community plugins, private data or production fixes.'}, indent=2))


def hide_ime():
    # Never send Back unless the system explicitly reports a shown keyboard.
    # Landscape extract-mode IME fields must not be mistaken for app fields.
    for _ in range(2):
        state=h.adb('shell','dumpsys','input_method').decode(errors='replace')
        if not re.search(r'(?:mInputShown|mIsInputViewShown)=true',state):return
        h.back()
    state=h.adb('shell','dumpsys','input_method').decode(errors='replace')
    assert not re.search(r'(?:mInputShown|mIsInputViewShown)=true',state),'Keyboard did not dismiss; do not navigate or type blindly'


def node_input(hint, value):
    hide_ime()
    nodes = h.tree()[1]
    candidates = [n for n in nodes if n.get('package')=='app.space.notes' and
                  n.get('class') == 'android.widget.EditText' and
                  hint in (n.get('hint'),n.get('text'),n.get('content-desc'))]
    assert len(candidates) == 1, ('Exact app editable field not established; no positional/IME fallback', hint, candidates)
    h.tap_node(candidates[0]); h.type_native(value);hide_ime()
    fields=[n for n in h.tree()[1] if n.get('package')=='app.space.notes' and n.get('class')=='android.widget.EditText']
    assert sum(n.get('text')==value for n in fields)==1,('Typed value not read back in the intended visible editor',hint,fields)
    h.capture('mynotes','fixture-'+('title' if hint=='Title' else 'body'))


def home():
    h.adb('shell', 'am', 'force-stop', 'app.space.notes')
    h.adb('shell', 'am', 'start', '-W', '-n', h.resolve('app.space.notes'))
    time.sleep(1)
    # Loading is allowed to settle; never dismiss an application error dialog.
    for _ in range(12):
        nodes = h.tree()[1]
        if any(n.get('content-desc') == 'Library' or n.get('text') == 'Library' for n in nodes): return
        time.sleep(.5)
    raise RuntimeError('Home Library control not ready; app error is not suppressed')


def native_labels():
    return '\n'.join(n.get('text', '') or n.get('content-desc', '') for n in h.tree()[1])


def note():
    home(); h.tap(TITLE)
    fields = h.edits()
    assert any(n.get('text') == BODY for n in fields), 'Original fixture body not read back in editor'


def route(*labels):
    home()
    for label in labels: h.tap(label)
    return {'status': 'interaction_observed', 'check': 'Named native controls opened; no implicit persistence, discoverability or module acceptance.', 'route': list(labels), 'visible_labels': native_labels()}


def functional(check, **extra):
    return dict(status='bounded_outcome_verified', check=check, **extra)


def command(pattern):
    commands = D.js('Object.values(app.commands.commands).map(c=>({id:c.id,name:c.name}))')
    match = next((c for c in commands if re.search(pattern, c['id']+' '+c['name'], re.I)), None)
    if not match: raise RuntimeError('No matching installed core command established for '+pattern+'; not proof of feature absence')
    ran = D.js('app.commands.executeCommandById('+json.dumps(match['id'])+')')
    assert ran, ('Installed command did not acknowledge invocation', match)
    time.sleep(.6)
    return match


def obs_note():
    D.js('app.setting.close();void 0')
    D.js('(async()=>{const f=app.vault.getAbstractFileByPath('+json.dumps(PATH)+');await app.workspace.getLeaf(false).openFile(f);await app.workspace.activeLeaf.setViewState({type:"markdown",state:{file:'+json.dumps(PATH)+',mode:"source"}})})()')
    time.sleep(.4)


def obs_inspect(pattern):
    obs_note(); match = command(pattern)
    return {'status':'interaction_observed','command':match,'check':'Official command API navigation shortcut; resulting real UI captured, not a functional or touch-route efficiency pass.'}


def make_fixtures():
    from PIL import Image, ImageDraw
    from reportlab.pdfgen import canvas
    image = Image.new('RGB', (1200, 800), 'white'); draw = ImageDraw.Draw(image)
    draw.text((50, 100), 'Public comparison cobalt42 + 25 = 30', fill='black', font_size=40)
    image.save(FIXTURES/'Comparison.png')
    pdf = canvas.Canvas(str(FIXTURES/'Comparison.pdf'))
    for number in range(1, 4):
        pdf.drawString(50, 730, 'Public cobalt42 page '+str(number)); pdf.showPage()
    pdf.save()
    (FIXTURES/'Comparison.txt').write_text(('Public ordinary text line.\n'*85000)+'TailNeedle cobalt42\n')
    (FIXTURES/'Comparison.csv').write_text('Name,Value\ncobalt42,123\nSecond,456\n')
    (FIXTURES/'Comparison.md').write_text('# Public cobalt42\n\nOriginal body.\n')
    (FIXTURES/'Comparison.html').write_text('<h1>Public cobalt42</h1><p>Original body.</p>')
    (FIXTURES/'Comparison.rtf').write_text(r'{\rtf1\ansi Public cobalt42\par Original body.}')
    from office_fixtures import write_office
    write_office(FIXTURES)
    manifest = [{'name':p.name,'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(FIXTURES.iterdir())]
    (OUT/'fixture-manifest.json').write_text(json.dumps(manifest, indent=2))
    h.adb('shell','mkdir','-p','/sdcard/Download/Comparison37')
    (OUT/'public-fixtures').mkdir(exist_ok=True)
    for p in FIXTURES.iterdir():
        shutil.copyfile(p,OUT/'public-fixtures'/p.name)
        h.adb('push',str(p),'/sdcard/Download/Comparison37/'+p.name)


def native_file(name):
    home(); h.tap('Library'); h.tap('Open file')
    # The system picker is exercised, not replaced by a private app import API.
    for _ in range(8):
        labels = native_labels()
        if name in labels:
            h.tap(name); break
        if 'Comparison37' in labels: h.tap('Comparison37'); continue
        if 'Downloads' in labels: h.tap('Downloads'); continue
        if 'Show roots' in labels: h.tap('Show roots'); continue
        if 'Download' in labels: h.tap('Download'); continue
        raise RuntimeError('Public fixture not reachable through actual picker: '+name+'; '+labels[:900])
    else: raise RuntimeError('Picker did not expose public fixture '+name)
    time.sleep(1)
    labels = native_labels()
    if 'MyNotes pages' in labels: h.tap('MyNotes pages'); time.sleep(1); labels = native_labels()
    return {'status':'interaction_observed','check':'Actual system picker selected the specified original; viewer/reader state inspected, not complete fidelity/search acceptance.','fixture':name,'visible_labels':labels,
            'public_source_sha256_after':h.adb('shell','sha256sum','/sdcard/Download/Comparison37/'+name).decode().split()[0]}


def obs_file(name):
    obs_note()
    path = 'Comparison37/'+name
    D.js('(async()=>{await app.workspace.getLeaf(false).openFile(app.vault.getAbstractFileByPath('+json.dumps(path)+'))})()')
    time.sleep(.8)
    view = D.js('({type:app.workspace.activeLeaf.view.getViewType(),file:app.workspace.activeLeaf.view.file?.path,body:document.body.innerText})')
    assert view.get('file') == path, ('Target file view not established', view)
    return {'status':'interaction_observed','check':'Public attachment opened through official vault/workspace API shortcut; actual view type captured, not full format fidelity or user import-route acceptance.','fixture':name,'view':view}


def ours_task(number):
    if number == 1:
        home(); h.tap('Search all content & settings'); h.type_native('cobalt42'); time.sleep(.7)
        nodes = h.tree()[1]
        labels = '\n'.join(n.get('text','') for n in nodes if n.get('class') != 'android.widget.EditText')
        assert TITLE in labels and 'cobalt42' in labels, 'Result title AND body passage must occur outside the query field'
        return functional('Body-only search returns the known title and marker passage outside the search input; not latency or broad search acceptance.', visible_labels=labels)
    if number == 2:
        note(); return functional('Exact public body read back after force-stop/relaunch and opening the saved note; bounded plain-note persistence only.')
    if number == 3: return route('Library','+ Checklist')
    if number == 4: return route('Doc','New document','Add to document','Table')
    if number == 5:
        note(); h.tap('Add to note'); h.tap('Image'); return {'status':'interaction_observed','check':'Image insertion/picker entry, not image-import success.'}
    if number == 6:
        note(); h.tap('Add to note'); h.tap('Hyperlink'); return {'status':'interaction_observed','check':'Link editor entry only; external destination is not opened offline.'}
    if number in {7,9,34,35,36}:
        r = route('Library','Settings'); r.update(status='counterpart_not_established', check='Requested counterpart not established in the visited first-party settings surface. This is NOT proof of app-wide absence or a comparative score.'); return r
    if number == 8:
        note(); h.tap('More actions'); h.tap('Count'); return {'status':'interaction_observed','check':'Actual note count surface; numeric count comparison remains unverified.'}
    if number == 10: return route('New text note')
    if number == 11:
        note(); h.tap('Add to note'); h.tap('Drawing'); return {'status':'interaction_observed','check':'Actual drawing entry, not a shared Canvas capability or saved drawing pass.'}
    if number == 12:
        note(); h.tap('Add to note'); h.tap('Diagram'); h.tap('Add shape'); h.tap('Rectangle'); return {'status':'interaction_observed','check':'Rectangle tool activated; no comparator equivalence or persisted graph acceptance.'}
    if number in {13,14,15,16}:
        return native_file({13:'Comparison.pdf',14:'Comparison.docx',15:'Comparison.txt',16:'Comparison.csv'}[number])
    if number == 17:
        outcomes=[]
        for name in ['Comparison.md','Comparison.html','Comparison.rtf','Comparison.odt','Comparison.pptx']:
            try: outcomes.append(native_file(name))
            except Exception as error: outcomes.append({'fixture':name,'status':'blocked','error':str(error)})
        return {'status':'interaction_observed','subtasks':outcomes,'check':'Each named format attempted separately; blocked or text-only views are not full import fidelity passes.'}
    if number == 18:
        note(); h.tap('Add to note'); h.tap('Read image'); return {'status':'interaction_observed','check':'OCR entry only. Actual varied-scene recognition/accuracy remains unverified in this paired task.'}
    if number == 19: return route('Library','Voice inbox')
    if number == 20:
        note(); h.tap('Add to note'); h.tap('Reminder'); return {'status':'interaction_observed','check':'Reminder dialog entry; notification delivery/time persistence not inferred.'}
    if number == 21:
        home(); value=h.adb('shell','dumpsys','appwidget').decode(errors='replace'); (OUT/'mynotes-widget-providers.txt').write_text(value)
        return {'status':'inspection_only','check':'Android provider diagnostic retained; widget placement, input and usability NOT tested.'}
    if number == 22: return route('Library','Settings','Appearance')
    if number in {23,24}: return route('Library','Settings','Storage & recovery')
    if number == 25: return route('Library','Trash')
    if number == 26: return route('Library','Archive')
    if number == 27:
        note(); h.tap('More actions'); return {'status':'interaction_observed','check':'Editor action surface; actual Undo/Redo/history outcome not inferred.'}
    if number == 28:
        home(); h.tap('Doc'); assert 'New document' in native_labels(); h.tap('Notes'); assert 'New text note' in native_labels()
        return functional('One actual tab-touch Doc-to-Notes roundtrip exposes the matching create actions. Not repeated swipes, IME acceptance or Obsidian tab equivalence.')
    if number == 29: return route('Library','Settings')
    if number == 30:
        note(); h.tap('More actions'); h.tap('Share'); return {'status':'interaction_observed','check':'Share entry/resolver only; no external app, message or network operation is performed.'}
    if number == 31:
        note(); h.tap('More actions'); h.tap('Quick summary'); return {'status':'interaction_observed','check':'Local summary surface only; no saving or summary accuracy acceptance.'}
    if number == 32: return route('Library','Settings','Diagnostics')
    if number == 33:
        start=h.launch_samples('mynotes','app.space.notes'); start['status']='diagnostic_only'; return start
    if number == 37: return route('Library','Settings','About')
    raise ValueError(number)


def obs_task(number):
    if number == 1:
        obs_note(); command('^global-search:open$')
        assert D.tap('.search-input-container input'), 'Visible vault-search field unavailable'
        D.type('cobalt42'); time.sleep(1.5)
        matches=D.js('[...document.querySelectorAll(".search-result-file-match")].filter(e=>e.getBoundingClientRect().height>0&&e.getBoundingClientRect().top<innerHeight&&e.getBoundingClientRect().bottom>0).map(e=>e.textContent)')
        titles=D.js('[...document.querySelectorAll(".search-result-file-title")].map(e=>e.textContent)')
        assert any('cobalt42' in x for x in matches) and any('Meeting' in x for x in titles), 'Visible search result containers did not verify fixture'
        return functional('Actual query input yields marker passage and target file title in visible result containers; command API opened pane, not touch-route efficiency.',matches=matches,titles=titles)
    if number == 2:
        obs_note(); value=D.js('(async()=>await app.vault.read(app.vault.getAbstractFileByPath('+json.dumps(PATH)+')))()')
        assert value==BODY; return functional('Official vault readback retains exact API-seeded public body. Fixture setup is not a UI creation or cross-app typing-efficiency pass.')
    if number in {13,14,15,16}: return obs_file({13:'Comparison.pdf',14:'Comparison.docx',15:'Comparison.txt',16:'Comparison.csv'}[number])
    if number == 17:
        outcomes=[]
        for name in ['Comparison.md','Comparison.html','Comparison.rtf','Comparison.odt','Comparison.pptx']:
            try: outcomes.append(obs_file(name))
            except Exception as error: outcomes.append({'fixture':name,'status':'blocked','error':str(error)})
        return {'status':'interaction_observed','subtasks':outcomes,'check':'Each real attachment attempted independently; view type is not import fidelity.'}
    if number in {22,23,29,32,34,35,37}:
        tab={22:'appearance',23:'file',29:'options',32:'file',34:'core-plugins',35:'sync',37:'about'}[number]
        D.js('app.setting.open();app.setting.openTabById('+json.dumps(tab)+');void 0'); time.sleep(.5)
        return {'status':'interaction_observed','tab_requested':tab,'check':'Official settings API navigation; actual visible surface retained. Requested tab is not presumed reached without subsequent evidence review.'}
    if number == 21:
        value=h.adb('shell','dumpsys','appwidget').decode(errors='replace');(OUT/'obsidian-widget-providers.txt').write_text(value)
        return {'status':'inspection_only','check':'Android provider diagnostic, not placement, usability or proof of all entry points.'}
    if number == 33:
        D.ws.close(); result=h.launch_samples('obsidian','md.obsidian')
        globals()['D']=Device();result['status']='diagnostic_only';return result
    patterns={3:'editor:toggle-checklist-status',4:'editor:insert-table',5:'attachment',6:'editor:insert-link',7:'backlink|outgoing',8:'outline|word-count',9:'properties|bases',10:'daily-notes',11:'canvas',12:'canvas',18:'ocr|recogniz',19:'audio-recorder',20:'reminder',24:'backup|file-recovery',25:'file-recovery',26:'bookmarks',27:'editor:undo',28:'workspace:show-tab|workspace:new-tab',30:'workspace:share',31:'summari',36:'slides'}
    result=obs_inspect(patterns[number])
    if number in {11,12,18,20,24,25,26,31}:
        result['equivalence']='Not automatically equivalent to the MyNotes tool/task; no extra-feature score advantage.'
    return result


def run_case(plan, app, function):
    number=int(plan['id'].split('X')[1]);row={'module':plan['id'],'name':plan['name'],'requested_task':plan['task'],'app':app,'status':'running','aspect_status':{'functional_outcome':'not_verified','layout':'captured_not_reviewed','icons':'captured_not_reviewed','animation':'not_measured','efficiency':'not_measured','smoothness':'not_measured','human_task_time':'not_measured','accessibility':'not_tested','physical_acceptance':False}}
    RESULTS.append(row);save()
    try:
        if app=='obsidian':
            h.adb('shell','am','start','-W','-n',h.resolve('md.obsidian'))
            try:D.js('!!app.vault')
            except Exception:globals()['D']=Device()
            expected_dark=os.environ.get('COMPARISON_NIGHT')=='yes'
            if D.js('document.body.classList.contains("theme-dark")')!=expected_dark:
                D.js('app.commands.executeCommandById("theme:toggle-light-dark")');time.sleep(.5)
            actual=D.js('({dark:document.body.classList.contains("theme-dark"),landscape:screen.width>screen.height,width:innerWidth,height:innerHeight})')
            row['observed_profile']=actual
            assert actual['dark']==expected_dark and actual['landscape']==(os.environ.get('COMPARISON_ORIENTATION')=='landscape'), 'Requested profile not achieved'
        row['requested_task_acceptance']=False
        row['bounded_probe_not_original_task_acceptance']=True
        row.update(function(number))
        if row['status']=='bounded_outcome_verified':row['aspect_status']['functional_outcome']='bounded_check_verified_only'
    except Exception as error:
        row.update(status='blocked',error=str(error));print('BLOCKED',app,plan['id'],str(error),flush=True)
    try:
        if app=='obsidian':D.settle()
        h.capture(app,plan['id'],D if app=='obsidian' else None)
        row['snapshot']=h.SNAPSHOTS[-1]['id']
    except Exception as error:row['capture_error']=str(error)
    save();print('MODULE RESULT',app,plan['id'],row['status'],flush=True)


def main():
    global D
    make_fixtures()
    h.adb('shell','am','start','-W','-n',h.resolve('app.space.notes'));time.sleep(1)
    h.tap('New text note');h.capture('mynotes','fixture-empty');node_input('Title',TITLE);node_input('Write your note…',BODY)
    h.tap('Back');home();h.tap(TITLE)
    assert any(n.get('text')==BODY for n in h.edits()), 'Typed public note not retained; no downstream MyNotes fixture claimed'
    (OUT/'mynotes-fixture.json').write_text(json.dumps({'method':'Actual native touch/input then Back and force-stop/relaunch editor readback','title':TITLE,'body_sha256':hashlib.sha256(BODY.encode()).hexdigest()}))
    onboard();D=Device();assert D.js('!!app.vault'),'Private vault unavailable'
    D.js('(async()=>{if(!app.vault.getAbstractFileByPath("Comparison37"))await app.vault.createFolder("Comparison37");const p='+json.dumps(PATH)+',f=app.vault.getAbstractFileByPath(p);if(f)await app.vault.modify(f,'+json.dumps(BODY)+');else await app.vault.create(p,'+json.dumps(BODY)+')})()')
    for file in FIXTURES.iterdir():
        encoded=base64.b64encode(file.read_bytes()).decode();path='Comparison37/'+file.name
        D.js('(async()=>{const b=Uint8Array.from(atob('+json.dumps(encoded)+'),c=>c.charCodeAt(0));const p='+json.dumps(path)+';if(!app.vault.getAbstractFileByPath(p))await app.vault.createBinary(p,b.buffer)})()')
    (OUT/'obsidian-fixture.json').write_text(json.dumps({'method':'Official vault API seeds public fixtures; NOT UI creation or file-picker import acceptance','path':PATH,'body_sha256':hashlib.sha256(BODY.encode()).hexdigest()}))
    for plan in PLANS:
        run_case(plan,'mynotes',ours_task)
        run_case(plan,'obsidian',obs_task)
    save()
    assert len(RESULTS)==74 and {r['module'] for r in RESULTS}=={p['id'] for p in PLANS}, 'Missing paired task attempts'
    (OUT/'execution-complete.json').write_text(json.dumps({'all37_families_attempted_in_both_apps':True,'whole_module_acceptance':False,'blocked':sum(r['status']=='blocked' for r in RESULTS),'bounded_outcomes_verified':sum(r['status']=='bounded_outcome_verified' for r in RESULTS),'probes':len(RESULTS)},indent=2))


if __name__=='__main__':
    try:main()
    except Exception as error:
        failure={'error':str(error),'module_attempts_completed':len(RESULTS),'all37_families_attempted_in_both_apps':False}
        print('::error title=Comparison harness fatal; not an app finding::'+str(error)[:3000].replace('%','%25').replace('\r','%0D').replace('\n','%0A'),flush=True)
        try:h.capture('mynotes' if D is None else 'obsidian','fatal',D)
        except Exception as capture_error:failure['capture_error']=str(capture_error)
        (OUT/'fatal.json').write_text(json.dumps(failure,indent=2));save();raise

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
import subprocess

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
        'paired_task_families_planned': 37, 'selected_range':os.environ.get('COMPARISON_RANGE','1-37'),
        'scope': 'Bounded live probes; functional readback, UI inspection, unavailable routes and blockers remain distinct. '
                 'Command/vault APIs are disclosed setup/navigation shortcuts, not human efficiency evidence. '
                 'Native exact-label routes may scroll one unambiguous public ScrollView; not unaided discoverability evidence. '
                 'No accounts, external services, community plugins, private data or production fixes.'}, indent=2))


def hide_ime():
    # mIsInputViewShown can remain true for a hidden IME view. It is not a
    # keyboard-window visibility assertion and must never trigger repeated Back.
    for _ in range(2):
        state=h.adb('shell','dumpsys','input_method').decode(errors='replace')
        (OUT/('input-method-'+str(len(h.COMMANDS))+'.txt')).write_text(state)
        shown=re.findall(r'\bmInputShown=(true|false)\b',state)
        assert shown and len(set(shown))==1, 'Live keyboard-manager state unavailable/ambiguous'
        if shown[0]=='false':return
        h.back()
    state=h.adb('shell','dumpsys','input_method').decode(errors='replace')
    (OUT/('input-method-'+str(len(h.COMMANDS))+'.txt')).write_text(state)
    assert re.findall(r'\bmInputShown=(true|false)\b',state)==['false'],'Keyboard still requested; no blind typing or navigation'


def node_input(hint, value):
    hide_ime()
    nodes = h.tree()[1]
    candidates = [n for n in nodes if n.get('package')=='app.space.notes' and
                  n.get('class') == 'android.widget.EditText' and
                  hint in (n.get('hint'),n.get('text'),n.get('content-desc'))]
    if not candidates:
        hide_ime();nodes=h.tree()[1]
        scrolls=[n for n in nodes if n.get('package')=='app.space.notes' and n.get('class')=='android.widget.ScrollView' and n.get('scrollable')=='true' and not n.get('content-desc')]
        if len(scrolls)==1:
            left,top,right,bottom=map(int,re.findall(r'\d+',scrolls[0]['bounds']))
            x=(left+right)//2;y1=top+(bottom-top)*2//3;y2=top+(bottom-top)//3
            if hint=='Title':y1,y2=y2,y1
            h.adb('shell','input','swipe',str(x),str(y1),str(x),str(y2),'350');time.sleep(.5)
        candidates=[n for n in h.tree()[1] if n.get('package')=='app.space.notes' and n.get('class')=='android.widget.EditText' and hint in (n.get('hint'),n.get('text'),n.get('content-desc'))]
    assert len(candidates) == 1, ('Exact app editable field not established; no positional/IME fallback', hint, candidates)
    h.tap_node(candidates[0]); h.type_native(value);hide_ime()
    fields=[n for n in h.tree()[1] if n.get('package')=='app.space.notes' and n.get('class')=='android.widget.EditText']
    assert sum(n.get('text')==value for n in fields)==1,('Typed value not read back in the intended visible editor',hint,fields)
    h.capture('mynotes','fixture-'+('title' if hint=='Title' else 'body'))


def cancel_public_session_picker():
    # Only cancel known disposable-session external chooser surfaces. Never
    # suppress app error/ANR dialogs, delete a file, or accept a permission.
    nodes=h.tree()[1];packages={n.get('package') for n in nodes}
    labels={n.get('text') for n in nodes}
    # Deny only the exact notification prompt observed in this public fixture
    # session. Capture it first; do not grant permissions or dismiss other
    # permission requests, errors or ANRs. This is cleanup, not delivery proof.
    permission_packages={'com.google.android.permissioncontroller','com.android.permissioncontroller'}
    notification_prompt='Allow MyNotes to send you notifications?'
    deny=[n for n in nodes if n.get('package') in permission_packages
          and n.get('resource-id','').endswith(':id/permission_deny_button')
          and n.get('enabled')=='true' and n.get('clickable')=='true']
    if packages and packages <= permission_packages and notification_prompt in labels and len(deny)==1:
        snapshot='notification-permission-before-denial-'+str(len(h.SNAPSHOTS))
        h.capture('mynotes',snapshot)
        h.tap_node(deny[0]);time.sleep(.5)
        assert not any(n.get('text')==notification_prompt for n in h.tree()[1]), 'Notification denial did not dismiss the known prompt'
        return {'cancelled':True,'surface':'system_notification_permission','decision':'denied',
                'original_prompt_snapshot':snapshot,'purpose':'Public-session isolation; not reminder delivery or permission-allow acceptance'}
    document_picker=bool(packages & {'com.google.android.documentsui','com.android.documentsui'})
    share_chooser=packages=={'android'} and {'Share','Just once','Always'} <= labels
    if document_picker or share_chooser:
        h.back()
        return {'cancelled':True,'surface':'system_document_picker' if document_picker else 'system_share_chooser','purpose':'Cancel external surface opened by this public test session, not an application error'}
    return {'cancelled':False}


def restore_requested_orientation():
    orientation=os.environ['COMPARISON_ORIENTATION']
    assert orientation in {'portrait','landscape'}
    size=h.adb('shell','wm','size').decode(errors='replace')
    match=re.search(r'Physical size:\s*(\d+)x(\d+)',size)
    assert match, 'Natural display size unavailable; do not guess rotation'
    width,height=map(int,match.groups())
    rotation=int((width>height)!=(orientation=='landscape'))
    # Same explicit CI rotation policy as provisioning, reapplied after launch.
    # This is controlled-emulator setup, not proof of normal phone rotation.
    h.adb('shell','settings','put','system','accelerometer_rotation','0')
    h.adb('shell','settings','put','system','user_rotation',str(rotation))
    h.adb('shell','wm','set-ignore-orientation-request','true')
    h.adb('shell','wm','fixed-to-user-rotation','enabled')
    h.adb('shell','wm','user-rotation','lock',str(rotation))
    time.sleep(1)
    return {'requested':orientation,'natural_size':[width,height],'rotation':rotation,
            'policy':'Explicit emulator rotation reapplied after launch; achieved profile independently checked'}


def home():
    cancel_public_session_picker()
    h.adb('shell', 'am', 'force-stop', 'app.space.notes')
    h.adb('shell', 'am', 'start', '-W', '-n', h.resolve('app.space.notes'))
    restore_requested_orientation()
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
    match = next((c for c in commands if (re.search(pattern,c['id'],re.I) or re.search(pattern,c['name'],re.I))), None)
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


def picker_click_target(nodes,label):
    # DocumentsUI labels are children of the actual clickable item. Use the
    # smallest containing item_root, never a header or a guessed coordinate.
    bounds=list(map(int,re.findall(r'\d+',label.get('bounds',''))))
    if len(bounds)!=4:return label
    containers=[]
    for n in nodes:
        if n.get('package')!=label.get('package') or n.get('enabled')=='false' or n.get('clickable')!='true':continue
        if not n.get('resource-id','').endswith(':id/item_root'):continue
        box=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
        if len(box)==4 and box[0]<=bounds[0]<bounds[2]<=box[2] and box[1]<=bounds[1]<bounds[3]<=box[3]:
            containers.append(((box[2]-box[0])*(box[3]-box[1]),n))
    return min(containers,key=lambda item:item[0])[1] if containers else label


def tap_with_public_scroll(*labels):
    for attempt in range(6):
        try:return ORIGINAL_TAP(*labels)
        except RuntimeError as error:
            if not str(error).startswith('No visible enabled exact-label control:') or attempt==5:raise
            nodes=h.tree()[1]
            # Only one unambiguous native vertical scroll container. Never
            # swipe a permission/error dialog or dismiss it to force progress.
            scrolls=[n for n in nodes if n.get('package')=='app.space.notes'
                     and n.get('class')=='android.widget.ScrollView' and n.get('scrollable')=='true']
            if len(scrolls)!=1:raise
            box=list(map(int,re.findall(r'\d+',scrolls[0].get('bounds',''))))
            if len(box)!=4:raise
            left,top,right,bottom=box
            if right<=left or bottom-top<60:raise
            x=(left+right)//2
            h.adb('shell','input','swipe',str(x),str(top+(bottom-top)*3//4),str(x),str(top+(bottom-top)//4),'350')
            time.sleep(.5)


def picker_target(nodes,name):
    providers={'com.google.android.documentsui','com.android.documentsui'}
    visible=[n for n in nodes if n.get('package') in providers and n.get('enabled')!='false']
    # A breadcrumb is navigation state, never a file/directory list entry.
    entries=[n for n in visible if not n.get('resource-id','').endswith(':id/breadcrumb_text')]
    for n in entries:
        if n.get('text')==name or n.get('content-desc','').startswith(name+','):
            return 'file',n
    for n in entries:
        if n.get('text')=='Comparison37' and n.get('resource-id')=='android:id/title':
            return 'folder',n
    if any(n.get('resource-id','').endswith(':id/breadcrumb_text') and n.get('text') in {'Comparison37','Downloads','Download'} for n in visible):
        return 'wait',None
    for n in entries:
        if n.get('text') in {'Downloads','Download'} and n.get('resource-id')=='android:id/title':
            return 'downloads',n
    for n in entries:
        if n.get('content-desc')=='Show roots':return 'roots',n
    return 'wait',None


def search_public_picker(name):
    providers={'com.google.android.documentsui','com.android.documentsui'}
    nodes=h.tree()[1]
    search=[n for n in nodes if n.get('package') in providers and n.get('enabled')!='false'
            and n.get('content-desc')=='Search']
    assert len(search)==1, 'A unique enabled DocumentsUI search control was not established'
    h.tap_node(search[0])
    nodes=h.tree()[1]
    fields=[n for n in nodes if n.get('package') in providers and n.get('class')=='android.widget.EditText'
            and n.get('enabled')!='false']
    assert len(fields)==1, 'A unique DocumentsUI query editor was not established'
    h.tap_node(fields[0]);h.type_native(name)
    h.adb('shell','input','keyevent','KEYCODE_ENTER');time.sleep(1)
    return {'method':'Actual DocumentsUI search input and touch; independent fallback, not original folder-route success',
            'query':name}


def native_file(name):
    home(); h.tap('Library'); h.tap('Open file')
    # The system picker is exercised, not replaced by a private app import API.
    navigated=set();selected=False;search_route=None
    for _ in range(8):
        nodes=h.tree()[1]
        action,target=picker_target(nodes,name)
        if action=='file':h.tap_node(picker_click_target(nodes,target));selected=True;break
        key=(action,target.get('text'),target.get('content-desc')) if target else None
        if target and key not in navigated:
            h.tap_node(picker_click_target(nodes,target));navigated.add(key)
        time.sleep(1)
    if not selected:
        # Preserve the unsuccessful directory route before trying a distinct,
        # real system-picker search route. Never relabel the first as passed.
        h.capture('mynotes','unresolved-picker-'+name)
        search_route=search_public_picker(name)
        for _ in range(12):
            nodes=h.tree()[1];action,target=picker_target(nodes,name)
            if action=='file':h.tap_node(picker_click_target(nodes,target));selected=True;break
            time.sleep(1)
    assert selected, 'Neither directory navigation nor real picker search exposed '+name+'; '+native_labels()[:900]
    time.sleep(1)
    labels = native_labels()
    if 'MyNotes pages' in labels: h.tap('MyNotes pages'); time.sleep(1); labels = native_labels()
    return {'status':'interaction_observed','check':'Actual system picker selected the specified original; viewer/reader state inspected, not complete fidelity/search acceptance.','fixture':name,'visible_labels':labels,'independent_picker_search':search_route,
            'original_directory_route':'not_established' if search_route else 'selected',
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
    if number == 4: return route('Doc','New document','Insert','Table')
    if number == 5:
        note(); h.tap('Add to note'); h.tap('Image'); return {'status':'interaction_observed','check':'Image insertion/picker entry, not image-import success.'}
    if number == 6:
        note(); h.tap('Add to note'); h.tap('Hyperlink'); return {'status':'interaction_observed','check':'Link editor entry only; external destination is not opened offline.'}
    if number in {7,9,34,35,36}:
        r = route('Library','Settings'); r.update(status='counterpart_not_established', check='Requested counterpart not established in the visited first-party settings surface. This is NOT proof of app-wide absence or a comparative score.'); return r
    if number == 8:
        note();h.tap('Find in note');h.type_native('cobalt42');time.sleep(.7)
        nodes=h.tree()[1];assert any('Find result' in n.get('content-desc','') and 'cobalt42' in n.get('content-desc','') for n in nodes),'In-note result passage not verified'
        return functional('Actual in-note query produces the marker result passage; not a long-note heading/footnote or reading-location acceptance.')
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
    if number == 37: return route('Library','Settings','About & offline models')
    raise ValueError(number)


def type_obsidian_query_with_android_ime(value):
    # CDP insertText alone can leave Android's keyboard closed. Use the actual
    # focused official-app accessibility field and keep keyboard conditions
    # explicit rather than comparing IME-open native UI with IME-closed WebView.
    fields=[n for n in h.tree()[1] if n.get('package')=='md.obsidian'
            and n.get('class')=='android.widget.EditText' and n.get('focused')=='true'
            and n.get('enabled')!='false']
    assert len(fields)==1, 'Unique focused official-app query field not established; no positional fallback'
    h.tap_node(fields[0]);time.sleep(.5)
    state=h.adb('shell','dumpsys','input_method').decode(errors='replace')
    (OUT/'obsidian-search-input-method.txt').write_text(state)
    assert re.findall(r'\bmInputShown=(true|false)\b',state)==['true'], 'Matched Android keyboard-open condition not established'
    h.type_native(value)
    return {'method':'Native Android tap and text into confirmed focused official-app field',
            'android_keyboard_requested':True,'cdp_insert_text_used':False}


def obs_task(number):
    if number == 1:
        obs_note(); command('^global-search:open$')
        assert D.tap('.search-input-container input'), 'Visible vault-search field unavailable'
        query_input=type_obsidian_query_with_android_ime('cobalt42'); time.sleep(1.5)
        matches=D.js('[...document.querySelectorAll(".search-result-file-match")].filter(e=>e.getBoundingClientRect().height>0&&e.getBoundingClientRect().top<innerHeight&&e.getBoundingClientRect().bottom>0).map(e=>e.textContent)')
        titles=D.js('[...document.querySelectorAll(".search-result-file-title")].map(e=>e.textContent)')
        assert any('cobalt42' in x for x in matches) and any('Meeting' in x for x in titles), 'Visible search result containers did not verify fixture'
        return functional('Actual query input yields marker passage and target file title in visible result containers; command API opened pane, not touch-route efficiency.',matches=matches,titles=titles,query_input=query_input)
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
        tab={22:'appearance',23:'file',29:'interface',32:'file',34:'plugins',35:'sync',37:'about'}[number]
        tabs=D.js('[...app.setting.settingTabs,...app.setting.pluginTabs].map(t=>({id:t.id,name:t.name}))')
        assert any(t['id']==tab for t in tabs),('Requested installed settings tab not present',tab,tabs)
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
        row['prior_external_surface_cleanup']=cancel_public_session_picker()
        if app=='obsidian':
            h.adb('shell','am','start','-W','-n',h.resolve('md.obsidian'))
            row['rotation_setup']=restore_requested_orientation()
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
        row['prior_external_surface_cleanup']=cancel_public_session_picker()
        if app=='obsidian':D.settle()
        h.capture(app,plan['id'],D if app=='obsidian' else None)
        row['snapshot']=h.SNAPSHOTS[-1]['id']
        png=(OUT/h.SNAPSHOTS[-1]['image']).read_bytes()
        assert png[:8]==b'\x89PNG\r\n\x1a\n', 'Original capture is not PNG'
        width,height=int.from_bytes(png[16:20],'big'),int.from_bytes(png[20:24],'big')
        row['captured_image_profile']={'width':width,'height':height,'landscape':width>height}
        if (width>height)!=(os.environ['COMPARISON_ORIENTATION']=='landscape'):
            row['probe_status_before_profile_failure']=row['status']
            row.update(status='blocked',error='Original PNG orientation does not match requested profile')

        packages=sorted({n.get('package','') for n in h.SNAPSHOTS[-1]['nodes']})
        row['captured_packages']=packages
        row['capture_contains_target_app']=('app.space.notes' if app=='mynotes' else 'md.obsidian') in packages
    except Exception as error:row['capture_error']=str(error)
    try:row['external_surface_cleanup_after_capture']=cancel_public_session_picker()
    except Exception as error:row['cleanup_error']=str(error)
    save();print('MODULE RESULT',app,plan['id'],row['status'],flush=True)


def prepare_native():
    h.adb('shell','am','start','-W','-n',h.resolve('app.space.notes'));time.sleep(1)
    h.tap('New text note');h.capture('mynotes','fixture-empty');node_input('Title',TITLE);node_input('Write your note…',BODY)
    h.tap('Back');home();h.tap(TITLE)
    assert any(n.get('text')==BODY for n in h.edits()), 'Typed public note not retained; no downstream MyNotes fixture claimed'
    (OUT/'mynotes-fixture.json').write_text(json.dumps({'method':'Actual native touch/input then Back and force-stop/relaunch editor readback','title':TITLE,'body_sha256':hashlib.sha256(BODY.encode()).hexdigest()}))


def prepare_obsidian():
    global D
    onboard();D=Device();assert D.js('!!window.app?.vault'),'Private vault unavailable'
    D.js('(async()=>{if(!app.vault.getAbstractFileByPath("Comparison37"))await app.vault.createFolder("Comparison37");const p='+json.dumps(PATH)+',f=app.vault.getAbstractFileByPath(p);if(f)await app.vault.modify(f,'+json.dumps(BODY)+');else await app.vault.create(p,'+json.dumps(BODY)+')})()')
    for file in FIXTURES.iterdir():
        encoded=base64.b64encode(file.read_bytes()).decode();path='Comparison37/'+file.name
        D.js('(async()=>{const b=Uint8Array.from(atob('+json.dumps(encoded)+'),c=>c.charCodeAt(0));const p='+json.dumps(path)+';if(!app.vault.getAbstractFileByPath(p))await app.vault.createBinary(p,b.buffer)})()')
    (OUT/'obsidian-fixture.json').write_text(json.dumps({'method':'Official vault API seeds public fixtures; NOT UI creation or file-picker import acceptance','path':PATH,'body_sha256':hashlib.sha256(BODY.encode()).hexdigest()}))


def main():
    globals()['ORIGINAL_TAP']=h.tap
    h.tap=tap_with_public_scroll
    start,end=map(int,os.environ.get('COMPARISON_RANGE','1-37').split('-'))
    assert 1<=start<=end<=37,'Invalid explicit probe range'
    selected=[p for p in PLANS if start<=int(p['id'].split('X')[1])<=end]
    make_fixtures();setup_errors={}
    for app,prepare in [('mynotes',prepare_native),('obsidian',prepare_obsidian)]:
        try:prepare()
        except Exception as error:
            setup_errors[app]={'error':str(error),'module_probes_executed':0,'acceptance':False}
            (OUT/'setup-failures.json').write_text(json.dumps(setup_errors,indent=2))
            message=(app+': '+str(error))[:3000].replace('%','%25').replace('\r','%0D').replace('\n','%0A')
            print('::error title=Original per-app fixture setup failed; other app may still be inspected::'+message,flush=True)
            try:h.capture('foreground',app+'-setup-failed')
            except Exception as capture_error:setup_errors[app]['capture_error']=str(capture_error)
    for plan in selected:
        for app,function in [('mynotes',ours_task),('obsidian',obs_task)]:
            if app in setup_errors:continue  # NOT EXECUTED, never37 fabricated blocked attempts.
            assert h.adb('get-state').decode().strip()=='device','Guest unavailable; remaining cases are NOT executed'
            run_case(plan,app,function)
    save()
    assert not setup_errors,('Incomplete paired setup; successful app probes retained, not paired acceptance',setup_errors)
    assert len(RESULTS)==2*len(selected) and {r['module'] for r in RESULTS}=={p['id'] for p in selected}, 'Missing paired task attempts'
    (OUT/'execution-complete.json').write_text(json.dumps({'all_selected_families_attempted_in_both_apps':True,'all37_families_attempted_in_both_apps':len(selected)==37,'fresh_guest_chunk_not_long_session_reliability':True,'whole_module_acceptance':False,'blocked':sum(r['status']=='blocked' for r in RESULTS),'bounded_outcomes_verified':sum(r['status']=='bounded_outcome_verified' for r in RESULTS),'probes':len(RESULTS)},indent=2))


if __name__=='__main__':
    try:main()
    except Exception as error:
        failure={'error':str(error),'module_attempts_completed':len(RESULTS),'all37_families_attempted_in_both_apps':False}
        # Host diagnostics before the runner performs normal emulator cleanup.
        # No environment dump, credentials, private library or destructive reset.
        diagnostics={}
        for name,command in {'memory':['cat','/proc/meminfo'], 'memory_pressure':['cat','/proc/pressure/memory'],
                             'processes':['ps','-eo','pid,comm,rss'], 'kernel':['dmesg','--ctime'],
                             'disk':['df','-h']}.items():
            try:
                result=subprocess.run(command,capture_output=True,text=True,timeout=10)
                diagnostics[name]={'returncode':result.returncode,'stdout':result.stdout[-40000:],'stderr':result.stderr[:1000]}
            except Exception as diagnostic_error:diagnostics[name]={'error':str(diagnostic_error)}
        (OUT/'host-before-runner-cleanup.json').write_text(json.dumps(diagnostics,indent=2))

        print('::error title=Comparison harness fatal; not an app finding::'+str(error)[:3000].replace('%','%25').replace('\r','%0D').replace('\n','%0A'),flush=True)
        try:h.capture('foreground','fatal')
        except Exception as capture_error:failure['capture_error']=str(capture_error)
        (OUT/'fatal.json').write_text(json.dumps(failure,indent=2));save();raise

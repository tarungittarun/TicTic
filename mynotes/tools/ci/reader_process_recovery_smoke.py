"""Actual background-process kill/relaunch of fixed public reader fixtures.
Disposable CI emulator only, already installed release APK. Never force-stop,
uninstall, clear data, upload private files or equate this with phone acceptance.
"""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import subprocess
import struct
import time
import zipfile
import xml.etree.ElementTree as ET
from .release_update_smoke import Device, PACKAGE, named, center


def pdf_fixture():
    objects=[b'<< /Type /Catalog /Pages 2 0 R >>',b'<< /Type /Pages /Kids [3 0 R 4 0 R] /Count 2 >>',
        b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 600 900] /Resources << /Font << /F1 5 0 R >> >> /Contents 6 0 R >>',
        b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 600 900] /Resources << /Font << /F1 5 0 R >> >> /Contents 7 0 R >>',
        b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>']
    for page in (1,2):
        stream=('BT /F1 24 Tf 60 800 Td (PublicReaderPage'+str(page)+') Tj ET').encode()
        objects.append(b'<< /Length '+str(len(stream)).encode()+b' >>\nstream\n'+stream+b'\nendstream')
    output=bytearray(b'%PDF-1.4\n');offsets=[0]
    for index,value in enumerate(objects,1):
        offsets.append(len(output));output.extend(str(index).encode()+b' 0 obj\n'+value+b'\nendobj\n')
    xref=len(output);output.extend(b'xref\n0 8\n0000000000 65535 f \n')
    for offset in offsets[1:]:output.extend(f'{offset:010d} 00000 n \n'.encode())
    output.extend(f'trailer\n<< /Size 8 /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n'.encode());return bytes(output)


def word_fixture():
    memory=io.BytesIO()
    with zipfile.ZipFile(memory,'w') as archive:
        archive.writestr('[Content_Types].xml','<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>')
        archive.writestr('_rels/.rels','<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="doc" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>')
        archive.writestr('word/document.xml','<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body><w:p><w:r><w:t>PublicWordPageOne</w:t></w:r></w:p><w:p><w:r><w:br w:type="page"/><w:t>PublicWordPageTwo</w:t></w:r></w:p></w:body></w:document>')
    return memory.getvalue()


def visible_rows(items):
    rows=[]
    for item in items:
        match=re.match(r'Spreadsheet row (\d+);',item.get('content-desc',''))
        if match:rows.append(int(match.group(1)))
    return sorted(set(rows))


def target_activity_record(dump):
    lines=dump.splitlines()
    for index,line in enumerate(lines):
        if re.search(r'\*\s+Hist\s+#\d+:',line) is None or PACKAGE+'/' not in line:continue
        indent=len(line)-len(line.lstrip());part=[]
        for value in lines[index+1:]:
            if len(value)-len(value.lstrip())<=indent and re.match(r'\s*\*\s+(?:Hist\s+#|Task)',value):break
            part.append(value)
        return '\n'.join([line]+part)
    return ''


def stopped_record(dump):
    return re.search(r'\b(?:mState|state)=STOPPED\b|\bstopped=true\b',target_activity_record(dump)) is not None


def wait_until(predicate,seconds=25):
    end=time.monotonic()+seconds
    while time.monotonic()<end:
        result=predicate()
        if result:return result
        time.sleep(.2)
    raise RuntimeError('Actual reader process recovery condition timed out')


def pid():
    result=subprocess.run(['adb','shell','pidof',PACKAGE],capture_output=True,text=True,timeout=10)
    value=result.stdout.strip()
    if result.returncode not in (0,1):raise RuntimeError('Cannot inspect main process PID')
    if not value:return None
    if not re.fullmatch(r'[1-9][0-9]*',value):raise RuntimeError('Expected exactly one main process')
    return int(value)



def picker_nodes(xml,enabled_only=True):
    allowed={'com.google.android.documentsui','com.android.documentsui','android'}
    return [node for node in ET.fromstring(xml).iter('node') if node.get('package') in allowed and (not enabled_only or node.get('enabled')=='true')]



def reachable_click(device,label):
    for _ in range(6):
        items=device.ui();target=named(items,label)
        if target is not None:
            try:point=center(target.get('bounds',''))
            except ValueError:point=None
            if point is not None:
                device.adb('shell','input','tap',*point);return
        scroll=next((node for node in items if node.get('scrollable')=='true'),None)
        if scroll is None:break
        bounds=scroll.get('bounds','');center(bounds)
        left,top,right,bottom=map(int,re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',bounds).groups())
        x=(left+right)//2
        device.adb('shell','input','swipe',x,top+(bottom-top)*85//100,x,top+(bottom-top)*20//100,400)
    raise RuntimeError('Public app menu target not reachable: '+label)

def capture_public(root,scene):
    if not re.fullmatch(r'[a-z0-9-]{1,60}',scene):raise ValueError('Invalid public scene')
    raw=subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=20)
    if not raw.startswith(b'\x89PNG\r\n\x1a\n') or not 24<=len(raw)<=8*1024*1024:raise RuntimeError('Invalid emulator screenshot')
    width,height=struct.unpack('>II',raw[16:24])
    if not 0<width*height<=16000000:raise RuntimeError('Unbounded emulator screenshot')
    shots=root/'screens';shots.mkdir(exist_ok=True);(shots/(scene+'.png')).write_bytes(raw)
    manifest=root/'screenshot-manifest.json';data=json.loads(manifest.read_text()) if manifest.exists() else {'scope':'Fresh disposable emulator/public fixtures only; no private uploads','captures':[]}
    data['captures'].append({'name':scene,'width':width,'height':height,'bytes':len(raw),'original_png_sha256':hashlib.sha256(raw).hexdigest(),'capture_run':os.environ.get('GITHUB_RUN_ID')})
    manifest.write_text(json.dumps(data,indent=2)+'\n')


def picker_target(items,label):
    # ElementTree leaf nodes are falsey despite being found. Return a nonempty
    # attribute snapshot, never the element itself, to a truth-based wait helper.
    return next((dict(node.attrib) for node in items if label.casefold() in (node.get('text','').casefold(),node.get('content-desc','').casefold())),None)


def export_public_diagnostics(device,root):
    device.home();capture_public(root,'reader-diagnostics-home');device.click('Library');capture_public(root,'reader-diagnostics-library');reachable_click(device,'Settings');capture_public(root,'reader-diagnostics-settings');reachable_click(device,'Diagnostics');capture_public(root,'reader-diagnostics-save-page');reachable_click(device,'Save diagnostic log as a file')
    checkpoint(root,'diagnostic-create-document-picker','MyNotes-diagnostics.txt')
    def picker(enabled_only=True):
        device.adb('shell','uiautomator','dump','/sdcard/notes-diagnostic-save.xml')
        return picker_nodes(device.adb('exec-out','cat','/sdcard/notes-diagnostic-save.xml'),enabled_only)
    def pick(label):
        try:node=wait_until(lambda:picker_target(picker(),label),40)
        except RuntimeError as failure:
            summary=json.dumps([{key:node.get(key,'') for key in ('text','content-desc','bounds','resource-id','enabled')} for node in picker(False) if node.get('text') or node.get('content-desc')],ensure_ascii=True)[:3500]
            raise RuntimeError('Public diagnostic create-document target missing: '+label+'; '+summary) from failure
        device.adb('shell','input','tap',*center(node.get('bounds','')))
    wait_until(lambda:picker_target(picker(),'Show roots') or picker_target(picker(),'Hide roots'),40)
    capture_public(root,'reader-diagnostics-create-picker');items=picker()
    if named(items,'Hide roots') is None:pick('Show roots')
    capture_public(root,'reader-diagnostics-save-roots');pick('Downloads');capture_public(root,'reader-diagnostics-save-picker');pick('Save')
    device.wait(lambda items:named(items,'Collect diagnostic logs') is not None)
    def saved():
        result=subprocess.run(['adb','exec-out','cat','/sdcard/Download/MyNotes-diagnostics.txt'],capture_output=True,timeout=20)
        return result.stdout if result.returncode==0 and result.stdout else None
    data=wait_until(saved)
    if not data.startswith(b'MyNotes diagnostics v1') or len(data)>3*1024*1024:raise RuntimeError('Diagnostic file export did not produce the bounded expected document')
    capture_public(root,'reader-diagnostics-file-saved')
    report={'ok':True,'source':'Actual Android create-document picker and completed readback','bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),'no_upload':True,'scope':'Fresh synthetic app diagnostics, not a personal export; file contents deliberately not copied into this evidence.'}
    (root/'diagnostic-save.json').write_text(json.dumps(report,indent=2)+'\n')
    return report


def pick_local_file(device,folder,name,root=None):
    # Real SAF user flow owns/persists the read grant; root-issued VIEW grants are
    # not a substitute for DocumentsUI permission semantics.
    device.click('Library');
    if root is not None:capture_public(root,'reader-'+name.rsplit('.',1)[-1]+'-library')
    reachable_click(device,'Open file')
    def ui(enabled_only=True):
        device.adb('shell','uiautomator','dump','/sdcard/notes-reader-picker.xml')
        return picker_nodes(device.adb('exec-out','cat','/sdcard/notes-reader-picker.xml'),enabled_only)
    def click(label):
        try:items=wait_until(lambda: (items if named(items,label) is not None else None) if (items:=ui()) else None,40)
        except RuntimeError as failure:
            summary=json.dumps([{key:node.get(key,'') for key in ('text','content-desc','bounds','resource-id','enabled')} for node in ui(False) if node.get('text') or node.get('content-desc')],ensure_ascii=True)[:3500]
            raise RuntimeError('Public SAF picker target missing: '+label+'; '+summary) from failure
        device.adb('shell','input','tap',*center(named(items,label).get('bounds','')))
    click('Show roots');click('Downloads');click(folder)
    if named(ui(),'List view') is not None:click('List view')
    if root is not None:capture_public(root,'reader-'+name.rsplit('.',1)[-1]+'-local-picker')
    click(name)


def checkpoint(root,phase,name):
    (root/'probe-progress.json').write_text(json.dumps({'phase':phase,'fixture':name,'acceptance':False})+'\n')
    print('::notice title=Public reader probe phase::'+phase+' '+name,flush=True)

def verify(output,expected_code=37):
    if not isinstance(expected_code,int) or isinstance(expected_code,bool) or expected_code<1:raise ValueError('Require an explicit positive verified APK version')
    device=Device()
    if os.environ.get('GITHUB_ACTIONS')!='true' or device.adb('shell','getprop','ro.kernel.qemu').strip()!='1':
        raise RuntimeError('Recovery probe is restricted to a disposable GitHub Actions emulator')
    run_id=os.environ.get('GITHUB_RUN_ID','');attempt=os.environ.get('GITHUB_RUN_ATTEMPT','')
    if not re.fullmatch(r'[1-9][0-9]*',run_id) or not re.fullmatch(r'[1-9][0-9]*',attempt):
        raise RuntimeError('Missing disposable CI run identity')
    if device.state()['version_code']!=expected_code:raise RuntimeError('Probe requires the exact verified candidate installed by its workflow')
    folder='NotesReaderRecoveryPublic-'+run_id+'-'+attempt
    # Root is confined to fixture creation on a disposable emulator, NOT URI grants.
    device.adb('root')
    from .adb_state import wait_uid
    wait_uid(0)
    root=Path(output);root.mkdir(parents=True,exist_ok=True)
    records=[]
    fixtures=[('ReaderRecovery.pdf','application/pdf',pdf_fixture()),
        ('ReaderRecovery.docx','application/vnd.openxmlformats-officedocument.wordprocessingml.document',word_fixture()),
        ('ReaderRecovery.csv','text/csv',('A,B,C,D,E,F\n'+''.join(f'Row{i},b,c,d,PublicFind{i},f\n' for i in range(1,81))).encode())]
    device.adb('shell','mkdir','-p','/sdcard/Download')
    # Fail on an existing leaf instead of overwriting any prior files.
    device.adb('shell','mkdir','/sdcard/Download/'+folder)
    for name,mime,data in fixtures:
        source=root/name;source.write_bytes(data)
        path='/sdcard/Download/'+folder+'/'+name
        device.adb('push',str(source),path)
    # Populate the entire public directory before the first SAF visit so a later
    # fixture does not depend on DocumentsUI refreshing its cached directory.
    for name,mime,data in fixtures:
        path='/sdcard/Download/'+folder+'/'+name
        checkpoint(root,'opening-through-real-local-SAF-picker',name)
        device.adb('shell','am','start','-W','-n',PACKAGE+'/.NotesActivity','-a','android.intent.action.MAIN')
        device.home();pick_local_file(device,folder,name,root)
        spreadsheet=name.endswith('.csv')
        control='Find in current sheet' if spreadsheet else 'Find in pages'
        items=device.wait(lambda items:named(items,control) is not None or named(items,'MyNotes pages') is not None)
        if named(items,'MyNotes pages') is not None:
            capture_public(root,'reader-docx-view-choice');device.click('MyNotes pages')
        device.wait(lambda items:named(items,control) is not None)
        checkpoint(root,'viewer-opened',name);capture_public(root,'reader-'+name.rsplit('.',1)[-1]+'-opened')
        if not spreadsheet:
            device.click('Read options');device.click('Go to page')
            items=device.wait(lambda items:sum(x.get('class')=='android.widget.EditText' for x in items)==1)
            field=next(x for x in items if x.get('class')=='android.widget.EditText')
            device.adb('shell','input','tap',*center(field.get('bounds','')));device.adb('shell','input','text','2');device.click('Go')
            device.wait(lambda items:named(items,'2 / 2') is not None)
        device.click(control)
        device.fill('Find values or formulas' if spreadsheet else 'Find embedded text','PublicFind40' if spreadsheet else 'PublicReaderQuery')
        expected_query='PublicFind40' if spreadsheet else 'PublicReaderQuery'
        if spreadsheet:
            device.click('Find')
            items=device.wait(lambda items:41 in visible_rows(items))
            rows_before=visible_rows(items);first_row=min(rows_before)
        else:first_row=None;rows_before=[]
        before=pid()
        if before is None:raise RuntimeError('No reader process before backgrounding')
        capture_public(root,'reader-'+name.rsplit('.',1)[-1]+'-before-kill')
        checkpoint(root,'backgrounding-configured-reader',name)
        device.adb('shell','input','keyevent','KEYCODE_HOME')
        def background_stopped():
            dump=device.adb('shell','dumpsys','activity','activities')
            record=target_activity_record(dump)
            observation='\n'.join(line for line in record.splitlines() if re.search(r'Hist|State=|state=|stopped=|HaveState|haveState|visible=|VisibleRequested',line))
            if not record:observation='No parsed target record. Matching headers: '+str([line for line in dump.splitlines() if PACKAGE+'/' in line])
            (root/'activity-stop-observation.txt').write_text(observation[:3000]+'\n')
            return stopped_record(dump)
        wait_until(background_stopped)
        # am kill preserves task/saved Activity state, unlike force-stop/clear-data.
        checkpoint(root,'stopped-state-confirmed',name)
        device.adb('shell','am','kill',PACKAGE)
        wait_until(lambda:pid() is None)
        checkpoint(root,'old-process-gone-relaunching-main',name)
        device.adb('shell','am','start','-W','-n',PACKAGE+'/.NotesActivity','-a','android.intent.action.MAIN')
        items=device.wait(lambda items:any(x.get('class')=='android.widget.EditText' and x.get('text')==expected_query for x in items))
        if spreadsheet:
            items=device.wait(lambda items:bool(visible_rows(items)))
            rows_after=visible_rows(items)
            if min(rows_after)!=first_row:raise RuntimeError('Visible spreadsheet anchor row was not restored')
            if 41 not in rows_after:raise RuntimeError('Searched spreadsheet row41 was not restored into the visible viewport')
        else:rows_after=[];device.wait(lambda items:named(items,'2 / 2') is not None)
        after=pid()
        if after is None or after==before:raise RuntimeError('Main process did not actually change')
        copied=subprocess.check_output(['adb','exec-out','cat',path],timeout=20)
        if copied!=data:raise RuntimeError('Original public fixture bytes changed')
        checkpoint(root,'reader-restored-after-actual-kill',name);capture_public(root,'reader-'+name.rsplit('.',1)[-1]+'-restored')
        records.append({'name':name,'sha256':hashlib.sha256(data).hexdigest(),'before_pid':before,'after_pid':after,'stopped_before_kill':True,'query_restored':True,'page_2_restored':not spreadsheet,'visible_row_restored':first_row,'visible_row_before':first_row,'visible_row_after':min(rows_after) if rows_after else None,'visible_rows_before':rows_before,'visible_rows_after':rows_after,'original_unchanged':True})
        device.home()
    diagnostic_save=export_public_diagnostics(device,root)
    report={'ok':True,'diagnostic_file_save':diagnostic_save,'package_code':expected_code,'api':int(device.adb('shell','getprop','ro.build.version.sdk').strip()),'records':records,
        'force_stop_uninstall_clear_data':False,'release_approval':False,
        'scope':'Actual emulator background-process kill and retained-task relaunch: PDF/Word page+query and CSV query+visible row, fixed public fixtures. Not image OCR recovery, zoom pixel proof, exact Office fidelity, physical acceptance or exhaustive migration.'}
    (root/'reader-process-recovery.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report),flush=True)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',required=True);parser.add_argument('--expected-code',type=int,default=37);args=parser.parse_args()
    try:verify(args.output,args.expected_code)
    except Exception as failure:
        progress=Path(args.output)/'probe-progress.json'
        observation=Path(args.output)/'activity-stop-observation.txt'
        message=type(failure).__name__+': '+str(failure)+(' '+progress.read_text() if progress.is_file() else '')+(' '+observation.read_text() if observation.is_file() else '')
        print('::error title=Reader process recovery not accepted::'+message.replace('%','%25').replace('\r','%0D').replace('\n','%0A')[:3000],flush=True)
        raise

"""Fresh-emulator release33→34 update: real UI, Notes + Doc, no data clearing.
Not a physical-device or exhaustive migration acceptance test. No Android build.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE='app.space.notes'
NOTE=('UpgradeNoteFixture','OriginalNoteBeforeUpdate')
DOC=('UpgradeDocFixture','OriginalDocBeforeUpdate')


def nodes(xml):
    return [x for x in ET.fromstring(xml).iter('node')
            if x.get('package')==PACKAGE and x.get('enabled')=='true']


def named(items,name):
    matches=[x for x in items if name in (x.get('text'),x.get('content-desc'))]
    return next((x for x in matches if x.get('clickable')=='true'),matches[0] if matches else None)


def center(bounds):
    match=re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',bounds)
    if not match:raise ValueError('Invalid UI bounds')
    x,y,right,bottom=map(int,match.groups())
    if right<=x or bottom<=y:raise ValueError('Empty UI target')
    return (x+right)//2,(y+bottom)//2


def identity(text):
    version=re.search(r'^\s*versionCode=(\d+)\b',text,re.M)
    first=re.search(r'^\s*firstInstallTime=(.+)$',text,re.M)
    if not version or not first:raise ValueError('Missing package installation identity')
    return {'version_code':int(version.group(1)),'first_install_time':first.group(1).strip()}


class Device:
    def adb(self,*args):return subprocess.check_output(['adb',*map(str,args)],text=True,timeout=45)
    def ui(self):
        self.adb('shell','uiautomator','dump','/sdcard/notes-update-ui.xml')
        return nodes(self.adb('exec-out','cat','/sdcard/notes-update-ui.xml'))
    def wait(self,condition):
        until=time.monotonic()+40
        while time.monotonic()<until:
            items=self.ui()
            if condition(items):return items
            time.sleep(.3)
        raise RuntimeError('Release UI fixture state not reached')
    def click(self,name):
        items=self.wait(lambda items:named(items,name) is not None)
        self.adb('shell','input','tap',*center(named(items,name).get('bounds','')))
    def fill(self,name,value):
        if not re.fullmatch('[A-Za-z0-9]+',value):raise ValueError('Only fixed public ASCII fixture input is supported')
        self.click(name);self.adb('shell','input','text',value)
    def state(self):return identity(self.adb('shell','dumpsys','package',PACKAGE))
    def home(self):
        for _ in range(4):
            items=self.ui()
            if named(items,'New text note') is not None or named(items,'New document') is not None:return
            self.adb('shell','input','keyevent',4)
        self.wait(lambda items:named(items,'New text note') is not None or named(items,'New document') is not None)
    def launch(self):
        self.adb('shell','am','force-stop',PACKAGE)
        self.adb('shell','am','start','-W','-n',PACKAGE+'/.NotesActivity')
        self.wait(lambda items:named(items,'New text note') is not None or named(items,'New document') is not None)
    def check(self,workspace,fixture):
        self.click(workspace);self.click(fixture[0])
        self.wait(lambda items:all(any(x.get('class')=='android.widget.EditText' and x.get('text')==value for x in items) for value in fixture))
        self.home()


def verify(old,candidate,output):
    device=Device();device.adb('install',old);before=device.state()
    if before['version_code']!=33:raise RuntimeError('Expected original release code33')
    device.launch();device.click('Notes');device.click('New text note')
    device.fill('Title',NOTE[0]);device.fill('Write your note…',NOTE[1]);device.home()
    device.click('Doc');device.click('New document');device.fill('Title',DOC[0]);device.fill('Text section',DOC[1])
    # Canonical Doc exposes save acknowledgement. The same serial writer also flushes
    # the preceding legacy Note; do not force-stop based on an arbitrary sleep.
    device.wait(lambda items:any(x.get('text','').startswith('Saved · internal') for x in items))
    device.launch();device.check('Notes',NOTE);device.check('Doc',DOC)
    device.adb('shell','am','force-stop',PACKAGE);device.adb('install','-r',candidate);after=device.state()
    if after['version_code']!=34 or before['first_install_time']!=after['first_install_time']:
        raise RuntimeError('Not the expected version/retained installation identity')
    device.launch();device.check('Notes',NOTE);device.check('Doc',DOC)
    report={'ok':True,'package':PACKAGE,'from':before,'to':after,
            'api':int(device.adb('shell','getprop','ro.build.version.sdk').strip()),
            'old_release_cold_restart_checked':True,'notes_and_doc_retained':True,
            'uninstall_or_clear_data':False,'release_approval':False,
            'scope':'Public ASCII Note and Doc survive actual release33→34 install-over on a fresh emulator. Not physical or exhaustive rich-data acceptance.'}
    Path(output).write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--old',required=True);parser.add_argument('--candidate',required=True)
    parser.add_argument('--output',required=True);args=parser.parse_args()
    verify(args.old,args.candidate,args.output)

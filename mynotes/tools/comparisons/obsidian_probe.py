"""Real-device discovery. No screenshot mockups, account entry or coverage claims."""
import hashlib,json,os,re,subprocess,time,xml.etree.ElementTree as ET
from pathlib import Path
OUT=Path(os.environ.get('OBSIDIAN_OUTPUT','obsidian-evidence'));OUT.mkdir(parents=True,exist_ok=True)
steps=[]
def adb(*args):
 return subprocess.check_output(['adb',*args],timeout=45)
def capture(label):
 i=len(steps);stem=f'{i:02d}-{label}'
 # Keep the unaltered screenshot plus the corresponding real accessibility tree.
 (OUT/f'{stem}.png').write_bytes(adb('exec-out','screencap','-p'))
 try:
  adb('shell','uiautomator','dump','/sdcard/obsidian-window.xml');xml=adb('exec-out','cat','/sdcard/obsidian-window.xml');root=ET.fromstring(xml)
 except Exception as e:
  xml=b'<hierarchy/>';root=ET.fromstring(xml);print('Accessibility capture failure',str(e),flush=True)
 (OUT/f'{stem}.xml').write_bytes(xml)
 nodes=[dict(n.attrib) for n in root.iter('node')]
 steps.append({'id':stem,'screenshot':stem+'.png','nodes':nodes,'visible_text':[n.get('text') or n.get('content-desc') for n in nodes if n.get('text') or n.get('content-desc')]})
 (OUT/'observations.json').write_text(json.dumps(steps,indent=2));print(stem,steps[-1]['visible_text'],flush=True)
 return nodes
def click(nodes,*labels):
 for label in labels:
  for node in nodes:
   text=(node.get('text','')+' '+node.get('content-desc','')).strip()
   if text.casefold()==label.casefold():
    nums=list(map(int,re.findall(r'\d+',node.get('bounds',''))))
    if len(nums)==4 and nums[2]>nums[0] and nums[3]>nums[1]:
     adb('shell','input','tap',str((nums[0]+nums[2])//2),str((nums[1]+nums[3])//2));time.sleep(1);return True
 return False
def main():
 adb('shell','monkey','-p','md.obsidian','-c','android.intent.category.LAUNCHER','1');time.sleep(8)
 nodes=capture('official-app-first-launch')
 for idx in range(10):
  if click(nodes,'Create new vault','Create vault','Get started','Continue','Allow','While using the app','Use this folder','Got it','Done'):
   nodes=capture(f'onboarding-{idx}');continue
  edits=[n for n in nodes if n.get('class')=='android.widget.EditText']
  if edits and not any('Audit Vault' in n.get('text','') for n in edits):
   n=edits[0];nums=list(map(int,re.findall(r'\d+',n['bounds'])));adb('shell','input','tap',str((nums[0]+nums[2])//2),str((nums[1]+nums[3])//2));adb('shell','input','text','Audit%sVault');adb('shell','input','keyevent','KEYCODE_BACK');nodes=capture(f'vault-name-{idx}');continue
  break
 sockets=adb('shell','cat','/proc/net/unix').decode(errors='replace');(OUT/'webview-sockets.txt').write_text('\n'.join(x for x in sockets.splitlines() if 'devtools' in x))
 (OUT/'installed-package.txt').write_bytes(adb('shell','dumpsys','package','md.obsidian'))
 (OUT/'logcat.txt').write_bytes(adb('logcat','-d','-t','300'))
 capture('discovery-end-state')
if __name__=='__main__':main()

"""Fresh signed-app interaction probes. Scripted touches != participant usability testing.

Runs only on a disposable Actions emulator. Does not build/re-sign apps, install
plugins, login, upload notes, or treat an interaction count as UX acceptance.
"""
import datetime, hashlib, json, os, re, subprocess, time, traceback
from pathlib import Path
import xml.etree.ElementTree as ET

OUT=Path('paired-ux-evidence');OUT.mkdir(exist_ok=True)
COMMANDS=[];CASES=[];SNAPSHOTS=[]
TITLE='UX Meeting'
BODY=('Shared agenda. Compare editor controls while the keyboard is open. '
      'Decision marker cobalt42 belongs near the middle. Keep this sentence readable '
      'after formatting. Save this note and find it by its body text.')
MARKER='cobalt42'
def now():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def save():
 (OUT/'session.json').write_text(json.dumps({'schema':1,'run':os.environ.get('GITHUB_RUN_ID'),'source_revision':os.environ.get('GITHUB_SHA'),'method':'Scripted fresh installed-app interactions followed by separate visual review; not a human participant or physical-device study.','fixtures':{'title':TITLE,'body':BODY,'query':MARKER},'cases':CASES,'snapshots':SNAPSHOTS,'commands':COMMANDS},indent=2))
def adb(*args,timeout=45,check=True):
 start=time.monotonic();r=subprocess.run(['adb',*args],capture_output=True,timeout=timeout)
 COMMANDS.append({'at':now(),'args':list(args),'host_command_elapsed_ms':round((time.monotonic()-start)*1000,2),'returncode':r.returncode,'output_excerpt':r.stdout.decode(errors='replace')[:1500],'stderr':r.stderr.decode(errors='replace')[:500],'duration_warning':'Host command duration includes adb/harness overhead; NOT touch latency or human task completion time.'});save()
 if check and r.returncode:raise RuntimeError(str(args)+': '+r.stderr.decode(errors='replace')[:500])
 return r.stdout

def tree():
 adb('shell','uiautomator','dump','/sdcard/paired-window.xml',timeout=35)
 raw=adb('exec-out','cat','/sdcard/paired-window.xml');root=ET.fromstring(raw)
 return raw,[dict(n.attrib) for n in root.iter('node')]
def capture(app,label,device=None):
 stem=app+'-'+label
 raw=adb('exec-out','screencap','-p');(OUT/(stem+'.png')).write_bytes(raw)
 xml,nodes=tree();(OUT/(stem+'.xml')).write_bytes(xml)
 item={'id':stem,'image':stem+'.png','image_sha256':hashlib.sha256(raw).hexdigest(),'tree':stem+'.xml','at':now(),'nodes':nodes,'visible_labels':[n.get('text') or n.get('content-desc') for n in nodes if n.get('text') or n.get('content-desc')]}
 if device:
  item['dom_state']=device.js('({body:document.body.innerText,viewport:{width:innerWidth,height:innerHeight,dpr:devicePixelRatio},active:document.activeElement?.outerHTML?.slice(0,1000)})')
  (OUT/(stem+'-dom.json')).write_text(json.dumps(item['dom_state'],indent=2))
 SNAPSHOTS.append(item);save();print('SNAPSHOT',stem,item['visible_labels'][:20],flush=True)
 return nodes

def tap_node(n):
 a=list(map(int,re.findall(r'\d+',n['bounds'])));assert len(a)==4 and a[2]>a[0] and a[3]>a[1],n
 adb('shell','input','tap',str((a[0]+a[2])//2),str((a[1]+a[3])//2));time.sleep(.4)
def tap(*labels):
 _,nodes=tree()
 for label in labels:
  for n in nodes:
   if n.get('enabled')=='false':continue
   if any(n.get(k,'').casefold()==label.casefold() for k in ['text','content-desc']):tap_node(n);return n
 raise RuntimeError('No visible enabled exact-label control: '+repr(labels)+'; labels='+str([n.get('text') or n.get('content-desc') for n in nodes])[:1500])
def type_native(text):
 assert re.fullmatch(r'[A-Za-z0-9 .]+',text), 'Only safe public ASCII fixture input'
 adb('shell','input','text',text.replace(' ','%s'));time.sleep(.5)
def edits():return [n for n in tree()[1] if n.get('class')=='android.widget.EditText']
def back():adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(.5)
def probe(app,case,fn):
 row={'app':app,'case':case,'started':now(),'status':'running','evidence':[],'human_completion_time_ms':None,'touch_latency_ms':None};CASES.append(row);save();start=len(SNAPSHOTS)
 try:
  result=fn() or {};row.update(result);row['status']=result.get('status','interaction_observed')
 except Exception as error:
  row['status']='blocked';row['error']=str(error);row['traceback']=traceback.format_exc();print('BLOCKED',app,case,str(error),flush=True)
  try:capture(app,case+'-blocked')
  except Exception as capture_error:row['capture_error']=str(capture_error)
 finally:
  row['evidence']=[s['id'] for s in SNAPSHOTS[start:]];row['finished']=now();save()

def resolve(pkg):
 text=adb('shell','cmd','package','resolve-activity','--brief','-c','android.intent.category.LAUNCHER',pkg).decode()
 return next(line.strip() for line in reversed(text.splitlines()) if '/' in line)
def launch_samples(app,pkg):
 component=resolve(pkg);values=[]
 for i in range(3):
  adb('shell','am','force-stop',pkg)
  report=adb('shell','am','start','-W','-n',component).decode();values.append({'repeat':i+1,'raw':report,'android_total_time_ms':int(re.search(r'TotalTime:\s*(\d+)',report)[1]) if re.search(r'TotalTime:\s*(\d+)',report) else None});time.sleep(4)
 capture(app,'L01-post-launch')
 return {'status':'milestone_measured','samples':values,'metric':'Android am start -W TotalTime, process stopped, persistent data retained; first activity launch milestone, NOT time to usable editor. Different restored start content prevents a general speed ranking.'}

def ours():
 pkg='app.space.notes';adb('shell','monkey','-p',pkg,'-c','android.intent.category.LAUNCHER','1');time.sleep(5);capture('mynotes','first-launch')
 probe('mynotes','L01',lambda:launch_samples('mynotes',pkg))
 def create():
  tap('New text note','New note');capture('mynotes','W01-new-keyboard');fields=edits();assert len(fields)>=2,fields
  tap_node(fields[0]);type_native(TITLE);tap_node(edits()[-1]);type_native(BODY)
  nodes=capture('mynotes','W01-typed');assert any(n.get('text')==BODY for n in nodes),'Body was not read back from the live accessibility tree'
  back();tap('Back');nodes=capture('mynotes','W01-home-saved');assert any(TITLE in n.get('text','') for n in nodes),'Saved note title not visible on return'
  return {'status':'visible_outcome_verified','verified':'Typed body accessible readback and title visible on home after explicit Back; not private-file durability or process-death proof.'}
 probe('mynotes','W01',create)
 def formatting():
  tap(TITLE);fields=edits();assert fields;tap_node(fields[-1]);capture('mynotes','W02-keyboard-before-format');tap('Format');capture('mynotes','W02-format-keyboard');tap('Bold');capture('mynotes','W02-bold-state')
  tap('Close formatting');back();tap('Back')
  return {'status':'interaction_observed','verified':'Formatting opened and Bold control activated; styled persistence requires separate payload/visual verification. No character was typed in bold during this task.'}
 probe('mynotes','W02',formatting)
 def search():
  tap('Search all content & settings');type_native(MARKER);time.sleep(1);nodes=capture('mynotes','S01-body-match-results');labels='\n'.join(n.get('text','') for n in nodes)
  assert TITLE in labels,'Expected body-only matching note title not found'
  return {'status':'visible_outcome_verified','title_present':True,'query':MARKER,'result_labels':labels,'setup':'Native home search touched and query typed; UIAutomator inspection overhead is not search latency.'}
 probe('mynotes','S01',search)
 def settings():
  back();back();tap('Library');tap('Settings');capture('mynotes','P01-settings');tap('Appearance');capture('mynotes','P01-appearance')
  return {'status':'interaction_observed','verified':'Settings and Appearance entry opened; theme values were not switched, contrast/TalkBack were not tested.'}
 probe('mynotes','P01',settings)
 (OUT/'mynotes-gfxinfo.txt').write_bytes(adb('shell','dumpsys','gfxinfo',pkg,'framestats'))
 (OUT/'mynotes-memory.txt').write_bytes(adb('shell','dumpsys','meminfo',pkg))
 (OUT/'mynotes-package.txt').write_bytes(adb('shell','dumpsys','package',pkg))
 adb('shell','am','force-stop',pkg)

def obsidian():
 # Onboarding uses the documented existing discovery helper; no user folders/accounts.
 from obsidian_probe import main as onboard
 from obsidian_cdp import Device
 onboard();d=Device();assert d.js('!!window.app?.vault'),'Private vault not ready';capture('obsidian','first-private-vault',d)
 def launch():
  r=launch_samples('obsidian','md.obsidian');return r
 probe('obsidian','L01',launch);d=Device()
 def dom_touch(expression):
  selector=d.js('(()=>{const e='+expression+';if(!e)return null;e.setAttribute("data-paired-target","target");return "[data-paired-target=target]"})()')
  if not selector or not d.tap(selector):raise RuntimeError('Visible DOM touch target absent: '+expression)
 def ctrl_a():
  d.call('Input.dispatchKeyEvent',{'type':'keyDown','key':'a','code':'KeyA','modifiers':2,'windowsVirtualKeyCode':65});d.call('Input.dispatchKeyEvent',{'type':'keyUp','key':'a','code':'KeyA','modifiers':2,'windowsVirtualKeyCode':65})
 def create():
  dom_touch('[...document.querySelectorAll("button,a,.clickable-icon,.empty-state-action")].find(e=>e.textContent.trim().startsWith("New note") || e.getAttribute("aria-label")==="New note")')
  time.sleep(1);capture('obsidian','W01-new-note',d)
  if not d.tap('.inline-title'):raise RuntimeError('New note inline title not reachable')
  ctrl_a();d.type(TITLE);d.call('Input.dispatchKeyEvent',{'type':'keyDown','key':'Enter','code':'Enter','windowsVirtualKeyCode':13});d.call('Input.dispatchKeyEvent',{'type':'keyUp','key':'Enter','code':'Enter','windowsVirtualKeyCode':13})
  if not d.tap('.cm-content'):raise RuntimeError('Source editor content not reachable')
  d.type(BODY);time.sleep(1);capture('obsidian','W01-typed',d)
  read=d.js('app.workspace.activeLeaf.view.editor?.getValue()');assert read and BODY in read,read
  path=d.js('app.workspace.activeLeaf.view.file?.path');time.sleep(2)
  stored=d.js('(async()=>await app.vault.read(app.vault.getAbstractFileByPath('+json.dumps(path)+')))()')
  assert BODY in stored,'No saved body readback';assert TITLE in path,path
  return {'status':'visible_outcome_verified','verified':'UI touch and input created note; live editor and actual vault readback contain the fixture. No forced save API was invoked.','path':path,'saved_text':stored}
 probe('obsidian','W01',create)
 def formatting():
  if not d.tap('.cm-content'):raise RuntimeError('Editor not reachable')
  capture('obsidian','W02-keyboard-before-format',d)
  dom_touch('[...document.querySelectorAll(".mobile-toolbar-option,.clickable-icon,button")].find(e=>/^(toggle )?bold$/i.test(e.getAttribute("aria-label")||e.getAttribute("title")||""))')
  capture('obsidian','W02-bold-state',d)
  return {'status':'interaction_observed','verified':'Keyboard/editor and visible bold affordance inspected; no styled-persistence or latency acceptance.'}
 probe('obsidian','W02',formatting)
 def search():
  # Disclosed setup shortcut, not counted as touch navigation efficiency.
  d.js('app.commands.executeCommandById("global-search:open")');time.sleep(1)
  if not d.tap('.search-input-container input'):raise RuntimeError('Global search query field not visible')
  d.type(MARKER);time.sleep(2);capture('obsidian','S01-body-match-results',d)
  visible=d.js('document.body.innerText');assert TITLE in visible,'Expected body-only matching title absent'
  return {'status':'visible_outcome_verified','title_present':True,'query':MARKER,'result_labels':visible,'setup':'Built-in global-search command API opens the pane; actual query uses UI input. This is NOT a touch-route efficiency comparison with MyNotes.'}
 probe('obsidian','S01',search)
 def settings():
  d.js('app.setting.open();app.setting.openTabById("appearance");void 0');time.sleep(1);capture('obsidian','P01-appearance',d)
  return {'status':'interaction_observed','setup':'Appearance opened using built-in settings API, not a measured user route. Values/contrast/accessibility untested.'}
 probe('obsidian','P01',settings)
 (OUT/'obsidian-gfxinfo.txt').write_bytes(adb('shell','dumpsys','gfxinfo','md.obsidian','framestats'))
 (OUT/'obsidian-memory.txt').write_bytes(adb('shell','dumpsys','meminfo','md.obsidian'))
 (OUT/'obsidian-package.txt').write_bytes(adb('shell','dumpsys','package','md.obsidian'))

def main():
 for scope,key in [('global','window_animation_scale'),('global','transition_animation_scale'),('global','animator_duration_scale')]:adb('shell','settings','put',scope,key,'1.0')
 adb('shell','settings','put','system','font_scale','1.0');adb('shell','settings','put','system','accelerometer_rotation','0');adb('shell','settings','put','system','user_rotation','0')
 adb('shell','settings','put','global','airplane_mode_on','1');adb('shell','svc','wifi','disable');adb('shell','svc','data','disable')
 receipt={key:adb('shell',*args).decode(errors='replace') for key,args in {'size':['wm','size'],'density':['wm','density'],'android':['getprop','ro.build.version.release'],'sdk':['getprop','ro.build.version.sdk'],'fingerprint':['getprop','ro.build.fingerprint'],'font':['settings','get','system','font_scale'],'window_animations':['settings','get','global','window_animation_scale'],'transition_animations':['settings','get','global','transition_animation_scale'],'animator_duration':['settings','get','global','animator_duration_scale'],'input_method':['settings','get','secure','default_input_method'],'connectivity':['dumpsys','connectivity']}.items()}
 receipt.update({'physical_device':False,'fresh_disposable_guest':True,'no_user_library_access':True,'animations_disabled':False,'host_timing_is_not_touch_latency':True,'new_recordings_audio_tested':False,'animation_quality_measured':False});(OUT/'environment.json').write_text(json.dumps(receipt,indent=2))
 try:ours()
 except Exception as e:CASES.append({'app':'mynotes','case':'session','status':'blocked','error':str(e),'traceback':traceback.format_exc()});save()
 try:obsidian()
 except Exception as e:CASES.append({'app':'obsidian','case':'session','status':'blocked','error':str(e),'traceback':traceback.format_exc()});save()
 (OUT/'logcat.txt').write_bytes(adb('logcat','-d','-t','1500'));save()
 print('SESSION FINISHED',[(x['app'],x['case'],x['status']) for x in CASES],flush=True)
if __name__=='__main__':main()

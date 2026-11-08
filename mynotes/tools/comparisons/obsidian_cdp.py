"""Use official app's exposed debug endpoint; never patch or re-sign its APK."""
import json,re,subprocess,time,urllib.request,os
import websocket
class Device:
 def __init__(self):
  deadline=time.monotonic()+60;targets=[];last='No debug target yet'
  while time.monotonic()<deadline:
   try:
    pid=subprocess.check_output(['adb','shell','pidof','md.obsidian'],text=True,timeout=10).strip().split()[0]
    sockets=subprocess.check_output(['adb','shell','cat','/proc/net/unix'],text=True,timeout=10)
    sock='webview_devtools_remote_'+pid
    if '@'+sock not in sockets:time.sleep(.5);continue
    subprocess.check_call(['adb','forward','tcp:9222','localabstract:'+sock])
    targets=json.load(urllib.request.urlopen('http://127.0.0.1:9222/json',timeout=5))
    if targets:break
   except Exception as e:last=str(e)
   time.sleep(.5)
  if not targets:raise RuntimeError('Official app debug target not ready within60s: '+last)
  target=next((x for x in targets if x.get('type')=='page'),targets[0])
  self.ws=websocket.create_connection(target['webSocketDebuggerUrl'],suppress_origin=True,timeout=60);self.seq=0
 def call(self,method,params=None):
  from pathlib import Path
  Path('obsidian-evidence/last-cdp-request.json').write_text(json.dumps({'method':method,'params':params or {}},indent=2))
  self.seq+=1;i=self.seq;self.ws.send(json.dumps({'id':i,'method':method,'params':params or {}}))
  while True:
   r=json.loads(self.ws.recv())
   if r.get('id')==i:
    if 'error' in r:raise RuntimeError(r['error'])
    return r.get('result',{})
 def js(self,expression):
  r=self.call('Runtime.evaluate',{'expression':expression,'returnByValue':True,'awaitPromise':True})
  if 'exceptionDetails' in r:
   from pathlib import Path
   p=Path('obsidian-evidence/runtime-error.json');p.write_text(json.dumps({'expression':expression,'details':r['exceptionDetails']},indent=2))
   raise RuntimeError(r['exceptionDetails'])
  return r.get('result',{}).get('value')
 def tap(self,selector):
  # Scroll API-assisted selectors into the visible viewport before actual touch.
  # This helper is not a measurement of unaided discoverability or touch efficiency.
  box=self.js('(()=>{const e=document.querySelector('+json.dumps(selector)+');if(!e)return null;e.scrollIntoView({block:"center",inline:"nearest",behavior:"instant"});const r=e.getBoundingClientRect(),left=Math.max(0,r.left),right=Math.min(innerWidth,r.right),top=Math.max(0,r.top),bottom=Math.min(innerHeight,r.bottom);if(right<=left||bottom<=top)return null;return {x:(left+right)/2,y:(top+bottom)/2,w:right-left,h:bottom-top}})()')
  if not box or box['w']<=0 or box['h']<=0:return False
  p={k:box[k] for k in ['x','y']};self.call('Input.dispatchTouchEvent',{'type':'touchStart','touchPoints':[p]});self.call('Input.dispatchTouchEvent',{'type':'touchEnd','touchPoints':[]});time.sleep(.5);return True
 def text_button(self,text):
  selector=self.js('(()=>{const e=[...document.querySelectorAll("button,a,.clickable-icon")].find(e=>e.textContent.trim()==='+json.dumps(text)+');if(!e)return null;document.querySelectorAll("[data-audit-target]").forEach(x=>x.removeAttribute("data-audit-target"));e.setAttribute("data-audit-target","button");return "[data-audit-target=button]"})()')
  if selector:return self.tap(selector)
  return False
 def type(self,text):self.call('Input.insertText',{'text':text})
 def settle(self):
  """Fence WebView painting before a separate, unmodified Android screencap.

  The CDP bitmap is discarded: evidence always comes from adb screencap,
  including native bars, keyboard and system dialogs.
  """
  frames=self.js('new Promise(resolve=>{const t=setTimeout(()=>resolve("timeout"),2000);requestAnimationFrame(()=>requestAnimationFrame(()=>{clearTimeout(t);resolve("two-frames")}))})')
  if os.environ.get('COMPARISON_CAPTURE_POLICY')=='adb_only':
   time.sleep(.35)
   return {'animation_frames':frames,'compositor_fence':False,
           'policy':'Observer-isolation recheck: no extra CDP screenshot; not a proven guest-loss fix',
           'evidence_source':'adb exec-out screencap -p; no substituted CDP bitmap'}
  try:
   snapshot=self.call('Page.captureScreenshot',{'format':'png','fromSurface':True,'captureBeyondViewport':False})
   painted=bool(snapshot.get('data'))
  except RuntimeError:painted=False
  time.sleep(.35)
  return {'animation_frames':frames,'compositor_fence':painted,'evidence_source':'adb exec-out screencap -p; CDP bitmap discarded'}

"""Use official app's exposed debug endpoint; never patch or re-sign its APK."""
import json,re,subprocess,time,urllib.request
import websocket
class Device:
 def __init__(self):
  sockets=subprocess.check_output(['adb','shell','cat','/proc/net/unix'],text=True)
  sock=re.findall(r'@(webview_devtools_remote_\d+)',sockets)[-1]
  subprocess.check_call(['adb','forward','tcp:9222','localabstract:'+sock])
  targets=json.load(urllib.request.urlopen('http://127.0.0.1:9222/json'))
  target=next((x for x in targets if x.get('type')=='page'),targets[0])
  self.ws=websocket.create_connection(target['webSocketDebuggerUrl'],suppress_origin=True,timeout=30);self.seq=0
 def call(self,method,params=None):
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
  box=self.js('(()=>{const e=document.querySelector('+json.dumps(selector)+');if(!e)return null;const r=e.getBoundingClientRect();return {x:r.x+r.width/2,y:r.y+r.height/2,w:r.width,h:r.height}})()')
  if not box or box['w']<=0 or box['h']<=0:return False
  p={k:box[k] for k in ['x','y']};self.call('Input.dispatchTouchEvent',{'type':'touchStart','touchPoints':[p]});self.call('Input.dispatchTouchEvent',{'type':'touchEnd','touchPoints':[]});time.sleep(.5);return True
 def text_button(self,text):
  selector=self.js('(()=>{const e=[...document.querySelectorAll("button,a,.clickable-icon")].find(e=>e.textContent.trim()==='+json.dumps(text)+');if(!e)return null;e.setAttribute("data-audit-target","button");return "[data-audit-target=button]"})()')
  if selector:return self.tap(selector)
  return False
 def type(self,text):self.call('Input.insertText',{'text':text})

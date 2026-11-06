"""State-based inspection of unmodified Obsidian. UI inspection != functional pass."""
import json,os,re,subprocess,time
from pathlib import Path
from obsidian_cdp import Device
from obsidian_probe import main as onboard,adb
OUT=Path('obsidian-evidence');rows=[];PHASE=os.environ.get('OBSIDIAN_PHASE','walkthrough')
def main():
 onboard();d=Device()
 assert d.js('!!window.app?.vault'), 'Vault onboarding did not finish; no feature coverage claim'
 def capture(feature,brief,level='screen_inspected',observed='',check=None,text_override=None):
  idx=len(rows);stem=f'feature-{idx:03d}'
  text=(d.js('document.body.innerText') or '') if text_override is None else text_override
  if check is not None:level='functional_pass' if check else 'failed'
  # Native screenshot only, never reconstruct or restyle the target app.
  (OUT/(stem+'.png')).write_bytes(adb('exec-out','screencap','-p'))
  rows.append({'feature':feature,'brief':brief,'status':level,'observed':observed,'screenshot':stem+'.png','visible_text':text[:16000]})
  (OUT/'feature-results.json').write_text(json.dumps(rows,indent=2));print(feature,level,flush=True)
 def close():
  for _ in range(3):
   d.call('Input.dispatchKeyEvent',{'type':'keyDown','key':'Escape','code':'Escape','windowsVirtualKeyCode':27});d.call('Input.dispatchKeyEvent',{'type':'keyUp','key':'Escape','code':'Escape','windowsVirtualKeyCode':27})
  d.js('app.setting?.close();void 0');time.sleep(.15)
 def restart():
  nonlocal d
  try:d.ws.close()
  except Exception:pass
  adb('shell','am','force-stop','md.obsidian');adb('shell','monkey','-p','md.obsidian','-c','android.intent.category.LAUNCHER','1');time.sleep(5);d=Device()
  for _ in range(30):
   if d.js('!!window.app?.vault?.getAbstractFileByPath("Feature Audit/Playground.md")'):return
   time.sleep(1)
  raise RuntimeError('Same-data app restart did not return to the existing audit vault')
 def openfile(path):
  close();d.js('(async()=>{const f=app.vault.getAbstractFileByPath('+json.dumps(path)+');await app.workspace.getLeaf(false).openFile(f)})()');time.sleep(.8)
 def command(name,pattern):
  openfile('Feature Audit/Playground.md');cmd=d.js('Object.values(app.commands.commands).find(c=>new RegExp('+json.dumps(pattern)+',"i").test(c.name))?.id')
  if not cmd:capture(name,'Command not found in the installed mobile command registry.','blocked','Harness found no matching command; not proof the feature is absent');return
  try:
   ran=d.js('app.commands.executeCommandById('+json.dumps(cmd)+')');time.sleep(.8)
   capture(name,'Built-in action invoked by the official app command API; screenshot records the resulting real UI.','screen_inspected' if ran else 'blocked',str(cmd)+'; invoked='+str(ran))
  except Exception as e:capture(name,'Action could not be completed by this harness.','blocked',str(e)[:500])
 capture('Local private vault','Onboarding selected private app storage without an account.','functional_pass','Official application exposed its initialized vault object.')
 seed={
 'Feature Audit/Linked note.md':'# Linked note\nA second note with a return link to [[Playground]]. #audit\n',
 'Feature Audit/Playground.md':'''---
title: Feature playground
tags: [audit, demo]
count: 7
completed: false
audit_date: 2026-11-06
aliases: [Demo note]
---
# Feature playground
A public dummy note; no real user data.

## Formatting
**Bold**, *italic*, ~~strike~~, ==highlight== and `inline code`.

- Bullet
  - Nested bullet
1. Numbered item
2. Second item
- [ ] Pending task
- [x] Completed task

> [!note] Callout
> A callout keeps related content together.

| Feature | Value |
| --- | --- |
| Table | Supported |

```python
print("dummy example")
```

## Linking
[[Linked note]] and [[Linked note#Linked note]]. #audit
![[Linked note]]
A footnote reference.[^one]
[^one]: Footnote text.

## Math
$E=mc^2$

## Folding
Content under a heading can be folded.

## Multilingual text
ಕನ್ನಡ — हिन्दी — English
''',
 'Feature Audit/Templates/Daily.md':'# Daily template\nDate: {{date}}\n- [ ] Review dummy tasks\n',
 'Feature Audit/Board.canvas':json.dumps({'nodes':[{'id':'a','type':'text','text':'First idea','x':0,'y':0,'width':250,'height':100},{'id':'b','type':'file','file':'Feature Audit/Linked note.md','x':350,'y':0,'width':250,'height':200}],'edges':[{'id':'edge1','fromNode':'a','fromSide':'right','toNode':'b','toSide':'left'}]}),
 'Feature Audit/Notes.base':'views:\n  - type: table\n    name: Audit table\n'}
 for folder in ['Feature Audit','Feature Audit/Templates']:
  d.js('(async()=>{if(!app.vault.getAbstractFileByPath('+json.dumps(folder)+'))await app.vault.createFolder('+json.dumps(folder)+')})()')
 for path,content in seed.items():
  d.js('(async()=>{const p='+json.dumps(path)+',v='+json.dumps(content)+';const f=app.vault.getAbstractFileByPath(p);if(f)await app.vault.modify(f,v);else await app.vault.create(p,v)})()')
 time.sleep(2);openfile('Feature Audit/Playground.md')
 check=d.js('app.vault.getFiles().filter(f=>f.path.startsWith("Feature Audit/")).length')==len(seed)
 capture('Markdown files, folders and local readback','Create a nested dummy vault corpus through the app file API, then open it in the real editor.',check=check)
 # Only enable optional local modules actually exposed by this mobile settings UI.
 close();d.js('app.setting.open();app.setting.openTabById("plugins");void 0');time.sleep(.7)
 module_results=[]
 for pid,label in [('footnotes','Footnotes view'),('slash-command','Slash commands'),('zk-prefixer','Unique note creator'),('random-note','Random note'),('slides','Slides'),('audio-recorder','Audio recorder'),('workspaces','Workspaces')]:
  result=d.js('(()=>{const row=[...document.querySelectorAll(".setting-item")].find(e=>e.querySelector(".setting-item-name")?.innerText.trim()==='+json.dumps(label)+');if(!row)return {exposed:false};row.scrollIntoView({block:"center"});const box=row.querySelector(".checkbox-container");if(box&&!box.classList.contains("is-enabled"))box.click();return {exposed:true}})()')
  time.sleep(.25);result['id']=pid;result['enabled']=d.js('!!app.internalPlugins.plugins['+json.dumps(pid)+']?.enabled');module_results.append(result)
 (OUT/'module-enablement.json').write_text(json.dumps(module_results,indent=2));close();openfile('Feature Audit/Playground.md')
 scratch='Feature Audit/Command scratch.md'
 # Preserve every offered command as inventory, not as an executed test.
 inventory=d.js('({commands:Object.values(app.commands.commands).map(c=>({id:c.id,name:c.name})),plugins:Object.entries(app.internalPlugins.plugins).map(([id,p])=>({id,enabled:p.enabled})),settings:app.setting.settingTabs.map(t=>({id:t.id,name:t.name}))})')
 (OUT/'feature-inventory.json').write_text(json.dumps(inventory,indent=2))
 if PHASE=='walkthrough':
  for mode in ['source','preview']:
   d.js('(async()=>{await app.workspace.activeLeaf.setViewState({type:"markdown",state:{file:"Feature Audit/Playground.md",mode:'+json.dumps(mode)+'}})})()');time.sleep(1)
   capture('Markdown '+mode+' mode','Same dummy note shown in an editor or rendered reading view.','screen_inspected')
  for title,selector,brief in [
   ('Properties','.metadata-container','Structured fields, tags, aliases, a number and a checkbox in note frontmatter.'),
   ('Rich Markdown formatting','strong','Bold/italic/strike/highlight, lists, code and task syntax in a real rendered note.'),
   ('Callouts','.callout','An admonition with a title and body.'),
   ('Tables','table','A Markdown table rendered as rows and columns.'),
   ('Code blocks','pre','A fenced code sample rendered without executing its code.'),
   ('Internal links and embeds','.internal-embed','An internal note link and an embedded second note.'),
   ('Math','.math','Inline mathematical notation rendered by the app.'),
   ('Footnotes','.footnotes','Footnote references and their note body.')]:
   found=d.js('(()=>{const e=document.querySelector('+json.dumps(selector)+');if(e){e.scrollIntoView({block:"center"});return true}return false})()');time.sleep(.35)
   capture(title,brief,'screen_inspected' if found else 'blocked','Rendered selector found='+str(found))
  openfile('Feature Audit/Board.canvas');capture('Canvas','A real canvas document with a text card, a linked-file card and a connecting edge.')
  openfile('Feature Audit/Notes.base');capture('Bases','A local .base document opened by the app; no database account is used.')
  openfile('Feature Audit/Playground.md')
  for name,pattern in [('File explorer','show file explorer'),('Quick switcher','quick switcher.*open|open quick switcher'),('Vault search','search.*all files|search.*search in all'),('Command palette','command palette.*open|open command palette'),('Global graph','open graph view'),('Local graph','open local graph'),('Backlinks','backlinks.*open'),('Outgoing links','outgoing links.*open'),('Outline','outline.*show'),('Tags view','tags.*show|tags.*open'),('Bookmarks','bookmarks.*show|bookmarks.*open'),('Daily notes','daily notes.*open today'),('File recovery','recovery.*open|open saved snapshots')]:
   command(name,pattern)
  # Exercise the finite installed editor formatting family on a disposable scratch note.
  scratch='Feature Audit/Command scratch.md'
  formats=[('toggle-bold','**'),('toggle-italics','*'),('toggle-strikethrough','~~'),('toggle-highlight','=='),('toggle-code','`'),('toggle-blockquote','>'),('toggle-bullet-list','-'),('toggle-numbered-list','1.'),('toggle-checklist-status','['),('insert-callout','[!'),('insert-codeblock','```'),('insert-horizontal-rule','---'),('insert-mathblock','$$'),('insert-table','|'),('insert-footnote','[^')]
  formats += [('set-heading-'+str(i),'#'*i) for i in range(1,7)]
  formats += [('toggle-highlight-'+color,None) for color in ['red','orange','yellow','green','blue','purple']]
  for suffix,expected in formats:
   cid='editor:'+suffix
   if not d.js('!!app.commands.commands['+json.dumps(cid)+']'):continue
   original='Audit selection'
   try:
    d.js('(async()=>{const p='+json.dumps(scratch)+',f=app.vault.getAbstractFileByPath(p);if(f)await app.vault.modify(f,"Audit selection");else await app.vault.create(p,"Audit selection")})()')
    openfile(scratch)
    d.js('(async()=>{await app.workspace.activeLeaf.setViewState({type:"markdown",state:{file:'+json.dumps(scratch)+',mode:"source"}});const e=app.workspace.activeLeaf.view.editor;e.focus();e.setSelection({line:0,ch:0},{line:0,ch:4});return true})()')
    ran=d.js('app.commands.executeCommandById('+json.dumps(cid)+')');time.sleep(.2)
    edited=d.js('(async()=>{await app.workspace.activeLeaf.view.save();return await app.vault.read(app.vault.getAbstractFileByPath('+json.dumps(scratch)+'))})()')
    ok=ran and edited!=original and (expected is None or expected in edited)
    capture('Editor: '+suffix,'Execute the installed editor command on selected dummy text, save through the app and inspect actual Markdown readback.','functional_pass' if ok else 'blocked','Command='+cid+'; resulting Markdown='+edited[:300])
   except Exception as e:capture('Editor: '+suffix,'This command needs more manual/context-specific testing.','blocked',str(e)[:400])
  for name,pattern in [('Footnotes view','footnotes.*show|footnotes.*open'),('Unique note creator','unique note'),('Random note','random note'),('Templates','templates.*insert template'),('Slash commands','slash commands'),('Slides','slides.*start'),('Workspaces','workspaces.*manage'),('Mobile toolbar','configure mobile toolbar'),('Mobile quick action','configure mobile quick action'),('Tab overview','show tab overview'),('Sync sign-in entry','sync.*set up'),('Properties view','properties view.*show all')]:command(name,pattern)
 if PHASE=='commands':
  # Account for the complete installed command registry, not just a handpicked menu subset.
  # This is an entry/context check, not a claim that every resulting workflow completed.
  unsafe={'app:reload':'Reload action is not invoked; the separate process-restart check is not equivalent.',
          'app:migrate-frontmatter':'Vault-wide conversion is not run on the preserved reference corpus.',
          'app:open-vault':'Vault manager is inspected last because it unloads the JS context.',
          'app:switch-vault':'Switching vaults unloads this inspection context.'}
  for entry in inventory['commands']:
   cid=entry['id']
   already=any(cid+';' in r['observed'] or 'Command='+cid+';' in r['observed'] for r in rows)
   if already:continue
   try:
    command_text='Audit sample\n\n| Name | Value |\n| --- | --- |\n| One | Two |\n\n[[Linked note]]'
    d.js('(async()=>{const p='+json.dumps(scratch)+',v='+json.dumps(command_text)+',f=app.vault.getAbstractFileByPath(p);if(f)await app.vault.modify(f,v);else await app.vault.create(p,v)})()')
    path='Feature Audit/Notes.base' if cid.startswith('bases:') else 'Feature Audit/Board.canvas' if cid.startswith('canvas:') else scratch
    openfile(path)
    if path==scratch:
     d.js('(async()=>{await app.workspace.activeLeaf.setViewState({type:"markdown",state:{file:'+json.dumps(scratch)+',mode:"source"}});const e=app.workspace.activeLeaf.view.editor;e.focus();e.setSelection({line:0,ch:0},{line:0,ch:5});return true})()')
     if cid.startswith('editor:table-'):d.js('app.workspace.activeLeaf.view.editor.setCursor({line:2,ch:2});void 0')
    if cid in unsafe or cid.startswith(('sync:','publish:')):
     reason=unsafe.get(cid,'Account/cloud operation: entry is inventoried; no login or service operation is performed.')
     d.js('app.commands.executeCommandById("command-palette:open")');time.sleep(.3)
     if d.tap('.prompt-input'):d.type(entry['name'])
     capture('Command entry: '+entry['name'],'The real command entry is documented without completing its sensitive/context-resetting operation.','blocked','Command='+cid+'; '+reason)
    else:
     available=d.js('(()=>{const c=app.commands.commands['+json.dumps(cid)+'];return !!c && (c.checkCallback?!!c.checkCallback(true):true)})()')
     if available:
      invoked=d.js('app.commands.executeCommandById('+json.dumps(cid)+')');time.sleep(.4)
      capture('Command entry: '+entry['name'],'Invoke this registered built-in action in a seeded Markdown, Canvas or Bases context. Screenshot documents the resulting entry/state, not every follow-on behavior.','screen_inspected','Command='+cid+'; invoked='+str(invoked)+'; semantic outcome not individually certified.')
     else:capture('Command entry: '+entry['name'],'This registered action needs another selection/context; it was not falsely counted as passed.','blocked','Command='+cid+'; checkCallback did not permit invocation in the seeded context.')
    focus=adb('shell','dumpsys','window','windows').decode(errors='replace')
    line=next((x for x in focus.splitlines() if 'mCurrentFocus=' in x),'')
    if 'md.obsidian' not in line:
     adb('shell','input','keyevent','KEYCODE_BACK');adb('shell','monkey','-p','md.obsidian','-c','android.intent.category.LAUNCHER','1');time.sleep(.5)
    if not d.js('!!window.app?.vault'):restart()
   except Exception as e:
    capture('Command interrupted: '+entry['name'],'Driver/context interruption. No functional acceptance is claimed.','blocked','Command='+cid+'; '+str(e)[:300],text_override='Driver could not inspect DOM. Actual device screenshot is retained.')
    restart()
 if PHASE=='settings':
  # Inspect every mobile settings tab and every setting row, with one real screenshot per row.
  close();d.js('app.setting.open();void 0');time.sleep(.5)
  tabs=d.js('[...app.setting.settingTabs,...app.setting.pluginTabs].map(t=>({id:t.id,name:t.name}))')
  for tab in tabs:
   try:
    d.js('app.setting.openTabById('+json.dumps(tab['id'])+');void 0');time.sleep(.4)
    capture('Settings: '+tab['name'],'Installed mobile settings category. Settings are not blindly changed, especially account, sync or security controls.')
    items=d.js('[...document.querySelectorAll(".vertical-tab-content-container .setting-item")].map((e,i)=>({i,name:e.querySelector(".setting-item-name")?.innerText||"",description:e.querySelector(".setting-item-description")?.innerText||""})).filter(x=>x.name)') or []
    for item in items:
     d.js('document.querySelectorAll(".vertical-tab-content-container .setting-item")['+str(item['i'])+']?.scrollIntoView({block:"center"})');time.sleep(.15)
     capture(tab['name']+': '+item['name'],item['description'] or 'Setting offered by this version of the Android application.','screen_inspected','Control inspected only; not all values/branches functionally verified.')
   except Exception as e:
    capture('Settings blocked: '+tab['name'],'The inspection driver could not finish this section; this is not an app functional pass.','blocked',str(e)[:500],text_override='DOM inspection interrupted; screenshot shows the actual device at interruption.')
    (OUT/('settings-error-'+tab['id']+'.txt')).write_text(str(e))
    restart();d.js('app.setting.open();void 0');time.sleep(.4)
  close();openfile('Feature Audit/Playground.md')
 # Readback and cold restart check local persistence, not cloud sync or power-loss recovery.
 d.js('app.workspace.activeLeaf.view.save?.()');time.sleep(1)
 readback=d.js('(async()=>await app.vault.read(app.vault.getAbstractFileByPath("Feature Audit/Playground.md")))()')
 capture('Local source preservation','Read back the seeded Markdown through the official app vault API.',check=readback==seed['Feature Audit/Playground.md'])
 if PHASE=='variants':
  # Real process restart: same installed app and private vault, no clear/uninstall.
  restart()
  restored=d.js('(async()=>await app.vault.read(app.vault.getAbstractFileByPath("Feature Audit/Playground.md")))()')
  openfile('Feature Audit/Playground.md');capture('Cold restart and retained local note','Force-stop and relaunch the same installed app, then compare the exact original Markdown.',check=restored==seed['Feature Audit/Playground.md'])
  # Representative light/dark and portrait/landscape views, using app commands and Android rotation.
  for dark in [False,True]:
   current=d.js('document.body.classList.contains("theme-dark")')
   if current!=dark:d.js('app.commands.executeCommandById("theme:toggle-light-dark")');time.sleep(.5)
   for landscape in [False,True]:
    adb('shell','settings','put','system','user_rotation','1' if landscape else '0');time.sleep(1.5)
    openfile('Feature Audit/Playground.md')
    label=('dark' if dark else 'light')+' '+('landscape' if landscape else 'portrait')
    actual=d.js('({dark:document.body.classList.contains("theme-dark"),landscape:innerWidth>innerHeight,width:innerWidth,height:innerHeight})')
    capture('Appearance variant: '+label,'Official app theme-toggle command and actual Android device rotation; no screenshot restyling.','screen_inspected' if actual['dark']==dark and actual['landscape']==landscape else 'blocked',str(actual))
    openfile('Feature Audit/Board.canvas');capture('Canvas variant: '+label,'Native screenshot of the same real Canvas file under the selected device/theme configuration.')
  # Managing vaults can intentionally unload the app context, so it is the final action.
  command('Vault management','manage vaults')
 summary={'version':'1.15.0','phase':PHASE,'execution_complete':True,'features_recorded':len(rows),'functional_passes':sum(r['status']=='functional_pass' for r in rows),'failed':sum(r['status']=='failed' for r in rows),'inventory':inventory,'full_functional_coverage':False,'limitations':['Settings screens are inspected, not every possible value/combination.','Not every command is executed.','Paid Sync/Publish, sign-in and external integrations are not end-to-end verified.','No community plugin is installed or executed.','No physical-device acceptance.','Desktop-only features are not claimed as Android support.']}
 (OUT/'coverage-summary.json').write_text(json.dumps(summary,indent=2))
if __name__=='__main__':
 try:main()
 except Exception as e:
  (OUT/'driver-error.txt').write_text(str(e))
  try:(OUT/'failure-logcat.txt').write_bytes(adb('logcat','-d','-t','200'));(OUT/'driver-failure.png').write_bytes(adb('exec-out','screencap','-p'))
  except Exception:pass
  raise

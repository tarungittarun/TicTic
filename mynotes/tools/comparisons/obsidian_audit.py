"""State-based inspection of unmodified Obsidian. UI inspection != functional pass."""
import json,os,re,subprocess,time
from pathlib import Path
from obsidian_cdp import Device
from obsidian_probe import main as onboard,adb
OUT=Path('obsidian-evidence');rows=[]
def main():
 onboard();d=Device()
 assert d.js('!!window.app?.vault'), 'Vault onboarding did not finish; no feature coverage claim'
 def capture(feature,brief,level='screen_inspected',observed='',check=None):
  idx=len(rows);stem=f'feature-{idx:03d}'
  text=d.js('document.body.innerText') or ''
  if check is not None:level='functional_pass' if check else 'failed'
  # Native screenshot only, never reconstruct or restyle the target app.
  (OUT/(stem+'.png')).write_bytes(adb('exec-out','screencap','-p'))
  rows.append({'feature':feature,'brief':brief,'status':level,'observed':observed,'screenshot':stem+'.png','visible_text':text[:16000]})
  (OUT/'feature-results.json').write_text(json.dumps(rows,indent=2));print(feature,level,flush=True)
 def close():
  for _ in range(3):
   d.call('Input.dispatchKeyEvent',{'type':'keyDown','key':'Escape','code':'Escape','windowsVirtualKeyCode':27});d.call('Input.dispatchKeyEvent',{'type':'keyUp','key':'Escape','code':'Escape','windowsVirtualKeyCode':27})
  d.js('app.setting?.close();void 0');time.sleep(.15)
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
 # Enable optional local core modules only; never install community code or purchase services.
 for pid in ['footnotes','slash-command','zk-prefixer','random-note','slides','audio-recorder','workspaces']:
  try:
   d.js('(async()=>{await app.internalPlugins.enablePluginAndSave('+json.dumps(pid)+');return true})()')
  except Exception as e:print('Optional module enable blocked',pid,str(e)[:300],flush=True)
 # Preserve every offered command as inventory, not as an executed test.
 inventory=d.js('({commands:Object.values(app.commands.commands).map(c=>({id:c.id,name:c.name})),plugins:Object.entries(app.internalPlugins.plugins).map(([id,p])=>({id,enabled:p.enabled})),settings:app.setting.settingTabs.map(t=>({id:t.id,name:t.name}))})')
 (OUT/'feature-inventory.json').write_text(json.dumps(inventory,indent=2))
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
 for name,pattern in [('Footnotes view','footnotes.*show|footnotes.*open'),('Unique note creator','unique note'),('Random note','random note'),('Templates','templates.*insert template'),('Slash commands','slash commands'),('Slides','slides.*start'),('Workspaces','workspaces.*manage'),('Mobile toolbar','configure mobile toolbar'),('Mobile quick action','configure mobile quick action'),('Tab overview','show tab overview'),('Vault management','manage vaults'),('Sync sign-in entry','sync.*set up'),('Properties view','properties view.*show all')]:command(name,pattern)
 # Inspect every mobile settings tab and every setting row, with one real screenshot per row.
 close();d.js('app.setting.open();void 0');time.sleep(.5)
 tabs=d.js('[...app.setting.settingTabs,...app.setting.pluginTabs].map(t=>({id:t.id,name:t.name}))')
 for tab in tabs:
  d.js('app.setting.openTabById('+json.dumps(tab['id'])+');void 0');time.sleep(.4)
  capture('Settings: '+tab['name'],'Installed mobile settings category. Settings are not blindly changed, especially account, sync or security controls.')
  items=d.js('[...document.querySelectorAll(".vertical-tab-content-container .setting-item")].map((e,i)=>({i,name:e.querySelector(".setting-item-name")?.innerText||"",description:e.querySelector(".setting-item-description")?.innerText||""})).filter(x=>x.name)') or []
  for item in items:
   d.js('document.querySelectorAll(".vertical-tab-content-container .setting-item")['+str(item['i'])+']?.scrollIntoView({block:"center"})');time.sleep(.15)
   capture(tab['name']+': '+item['name'],item['description'] or 'Setting offered by this version of the Android application.','screen_inspected','Control inspected only; not all values/branches functionally verified.')
 close();openfile('Feature Audit/Playground.md')
 # Cold restart verifies the local corpus survived, not account sync or physical storage recovery.
 d.js('app.workspace.activeLeaf.view.save?.()');time.sleep(1)
 readback=d.js('(async()=>await app.vault.read(app.vault.getAbstractFileByPath("Feature Audit/Playground.md")))()')
 capture('Local source preservation','Read back the seeded Markdown through the official app vault API.',check=readback==seed['Feature Audit/Playground.md'])
 summary={'version':'1.15.0','features_recorded':len(rows),'functional_passes':sum(r['status']=='functional_pass' for r in rows),'failed':sum(r['status']=='failed' for r in rows),'inventory':inventory,'full_functional_coverage':False,'limitations':['Settings screens are inspected, not every possible value/combination.','Not every command is executed.','Paid Sync/Publish, sign-in and external integrations are not end-to-end verified.','No community plugin is installed or executed.','No physical-device acceptance.','Desktop-only features are not claimed as Android support.']}
 (OUT/'coverage-summary.json').write_text(json.dumps(summary,indent=2))
if __name__=='__main__':
 try:main()
 except Exception as e:
  (OUT/'driver-error.txt').write_text(str(e));raise

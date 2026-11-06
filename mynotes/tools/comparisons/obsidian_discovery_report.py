"""Publish actual captured discovery screens; no fabricated feature-pass status."""
import json,os,shutil
from pathlib import Path
from PIL import Image
from reportlab.pdfgen.canvas import Canvas
from reportlab.lib.pagesizes import A4
src=Path('obsidian-evidence');out=Path('mynotes/docs/comparisons/obsidian/discovery')/os.environ['GITHUB_RUN_ID'];out.mkdir(parents=True,exist_ok=True)
rows=json.loads((src/'observations.json').read_text());pdf=Canvas(str(out/'Obsidian-discovery.pdf'),pagesize=A4)
for r in rows:
 im=Image.open(src/r['screenshot']).convert('RGB');name=r['screenshot'].replace('.png','.jpg');im.save(out/name,quality=92)
 pdf.setFont('Helvetica-Bold',14);pdf.drawString(32,805,'Obsidian 1.15.0 Android: discovery')
 pdf.setFont('Helvetica',9);pdf.drawString(32,788,r['id']);pdf.drawString(32,774,'Real emulator screenshot. Discovery only; not full feature acceptance.')
 pdf.drawImage(str(out/name),32,90,width=530,height=670,preserveAspectRatio=True,anchor='c')
 pdf.showPage()
 shutil.copyfile(src/(r['id']+'.xml'),out/(r['id']+'.xml'))
pdf.save()
for name in ['observations.json','installed-package.txt','webview-sockets.txt','apk-identity.json']:
 if (src/name).exists():shutil.copyfile(src/name,out/name)
(out/'README.md').write_text('# Obsidian official Android discovery\n\nThis first-run pilot discovers the actual installed mobile interface. It is not 100% coverage or a completed comparative feature audit. Screens are actual adb captures, not generated imagery. No personal data or account was used.\n')

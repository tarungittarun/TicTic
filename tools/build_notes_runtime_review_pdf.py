#!/usr/bin/env python3
"""Build an evidence-first overview, not a second set of aspirational app mockups.
Requires reportlab and Pillow. Inputs are bounded, provenance-pinned emulator captures.
Usage: python3 tools/build_notes_runtime_review_pdf.py --run RUN_ID
"""
import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path
from xml.sax.saxutils import escape
from reportlab.lib.colors import HexColor
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfgen import canvas
from reportlab.platypus import Paragraph
from reportlab.lib.utils import ImageReader

ROOT = Path(__file__).resolve().parents[1]
W, H = landscape(A4)
PAPER, INK, MUTED, HONEY = map(HexColor, ('#FAFAF8', '#24282D', '#59636F', '#76551D'))
VARIANTS = ('full-api34', 'night-api34', 'large-font-api34', 'landscape-api34', 'tablet-api34')
STYLE = ParagraphStyle('copy', fontName='Helvetica', fontSize=10, leading=14, textColor=INK)
SMALL = ParagraphStyle('small', parent=STYLE, fontSize=8, leading=11, textColor=MUTED)


def para(c, text, x, top, width, max_height=200, style=STYLE):
    p = Paragraph(text, style)
    _, height = p.wrap(width, max_height)
    if height > max_height:
        raise ValueError('PDF copy does not fit; shorten it instead of overlapping the next element')
    p.drawOn(c, x, top-height)
    return height


def build(run, output):
    folder = ROOT/'mynotes/docs/ux-review'/run
    manifest = json.loads((folder/'provenance.json').read_text())
    assert manifest['run'] == run
    source = manifest['source']
    assert len(source) == 40
    gradle = subprocess.check_output(['git', 'show', source+':mynotes/app/build.gradle'], cwd=ROOT, text=True)
    version = re.search(r"versionName '([^']+)'", gradle).group(1)
    code = re.search(r'versionCode (\d+)', gradle).group(1)
    records = {(r['lane'], r['scene']): r for r in manifest['images']}
    for r in records.values():
        path = folder/r['path']
        assert path.resolve().is_relative_to(folder.resolve())
        assert hashlib.sha256(path.read_bytes()).hexdigest() == r['sha256']
    all_green = all((folder/v/'results.json').is_file() and json.loads((folder/v/'results.json').read_text()).get('ok') is True for v in VARIANTS)
    approval = json.loads((folder.parent/'approval.json').read_text())
    approved = all_green and approval.get('reviewed_source') == source and str(approval.get('review_run')) == run
    status = 'Reviewed candidate' if approved else 'Pending review / not release approval'
    c = canvas.Canvas(str(output), pagesize=landscape(A4), pageCompression=1)
    c.setTitle(f'MyNotes {version} - Paper & Ink / actual runtime overview')
    c.setAuthor('MyNotes project')
    page = 0

    def heading(title, subtitle):
        nonlocal page
        page += 1
        c.setFillColor(PAPER); c.rect(0, 0, W, H, fill=1, stroke=0)
        c.setFillColor(INK); c.setFont('Helvetica-Bold', 23); c.drawString(32, H-45, title)
        para(c, subtitle, 32, H-61, W-64, 40, SMALL)
        c.setStrokeColor(HexColor('#D9DDD8')); c.line(32, 36, W-32, 36)
        c.setFillColor(MUTED); c.setFont('Helvetica', 7)
        c.drawString(32, 23, f'Runtime evidence overview | run {run} | source {source[:12]} | {status}')
        c.drawRightString(W-32, 23, str(page))

    def image(variant, scene, x, y, width, height, caption):
        r = records.get((variant, scene))
        if not r:
            c.setStrokeColor(HexColor('#D9DDD8')); c.rect(x, y, width, height)
            para(c, 'Evidence missing. This is not a successful review.', x+12, y+height-20, width-24, height-24)
            return
        iw, ih = r['width'], r['height']; scale = min(width/iw, height/ih)
        dw, dh = iw*scale, ih*scale
        c.drawImage(ImageReader(str(folder/r['path'])), x+(width-dw)/2, y+height-dh, dw, dh, preserveAspectRatio=True, mask='auto')
        para(c, caption, x, y-7, width, 32, SMALL)

    heading('Paper & Ink', f'MyNotes {version} ({code}) - actual runtime captures and bounded interaction evidence.')
    image('full-api34', 'editor', 37, 90, 235, 400, 'One writing surface. Optional title. Native caret and selection.')
    para(c, '<b>Less interface. More note.</b><br/><br/>Warm paper and charcoal ink; honey accents only where they help. A shared visual language across the editor, cards, diagrams and settings.<br/><br/><b>Context, not a button wall.</b><br/>Add, formatting and note actions appear when needed. Colour and labels have dedicated named controls in the interaction repair. Copy/Trash belong to list selection.<br/><br/><b>Paragraphs must behave like paragraphs.</b><br/>H1/H2 remeasure the native text layout. Following lines move naturally; composing a word and pressing Enter are tested.', 310, 470, 475, 260)
    para(c, '<b>Evidence boundary</b><br/>These are real emulator screen captures from synthetic fixtures, not photographs of a physical phone and not signed-release UI captures. Signed APK identity/update checks are separate. Green case counts do not automatically approve the look or first-use experience.<br/><br/>The old 0.8 functional build was published, but the user rejected its UI. This review replaces that acceptance claim; final device/user experience is still decisive.', 310, 194, 475, 130, SMALL)
    c.showPage()

    scenes = [
        ('Your notes, not a control panel', 'home', 'Quiet cards, off-thread search and clear creation actions. Card columns adapt to width and large text.'),
        ('Search should leave room for results', 'search-keyboard', 'In cramped landscape, non-search chrome collapses. A native test requires a readable result title above the keyboard.'),
        ('A calm place to write', 'editor', 'Optional title, direct body editing, restrained top and bottom chrome; no permanent command stack.'),
        ('The reported case: H1 on 01', 'numeric-h1', '01 is a heading; 02-09 remain ordinary paragraphs. Images precede the diagnostic raster colour mutation.'),
        ('The reported case: H2 on 01', 'numeric-h2', 'Changing heading size should reflow the paragraph, not paint larger letters over the next line.'),
        ('Formatting stays contextual', 'format', 'Text / H1 / H2 describe paragraphs; B / I / U / strike apply inline. The formatting deck replaces the normal dock.'),
        ('Keep writing with the keyboard', 'format-keyboard', 'Where there is room, formatting stays above the keyboard. Cramped landscape restores writing space rather than hiding the note.'),
        ('Lists that stay readable', 'checklist', 'Native editable list items, large checkbox targets and a simple Add item route. No empty progress counter.'),
        ('Diagrams share the same language', 'diagram', 'A focused canvas with contextual Add, text/style, arrangement and More tools. Editable objects are retained.'),
        ('Settings without a second design system', 'settings', 'Local storage, recovery and diagnostics use the same typography, spacing and plain-language hierarchy.'),
        ('Recovery is understandable', 'storage', 'Folder selection and verified switching preserve the previous files. Long explanations wrap and scroll at 200% font.'),
    ]
    labels = ('Light', 'Dark', '200% system font')
    for title, scene, description in scenes:
        heading(title, description)
        for i, variant in enumerate(VARIANTS[:3]):
            image(variant, scene, 35+i*259, 88, 247, 403, labels[i])
        c.showPage()
        if scene == 'diagram':
            heading('A simple flowchart, still editable', 'Schematic guide to the six current shape types and directed labelled arrows. This page is explanatory, not an emulator capture.')
            def block(kind, x, y, width, height, label):
                c.setStrokeColor(HONEY); c.setFillColor(HexColor('#F0F1EE')); c.setLineWidth(1.5)
                if kind == 'decision':
                    path=c.beginPath();path.moveTo(x+width/2,y+height);path.lineTo(x+width,y+height/2);path.lineTo(x+width/2,y);path.lineTo(x,y+height/2);path.close();c.drawPath(path,fill=1,stroke=1)
                elif kind == 'data':
                    path=c.beginPath();path.moveTo(x+14,y+height);path.lineTo(x+width,y+height);path.lineTo(x+width-14,y);path.lineTo(x,y);path.close();c.drawPath(path,fill=1,stroke=1)
                elif kind == 'ellipse':c.ellipse(x,y,x+width,y+height,fill=1,stroke=1)
                elif kind == 'terminator':c.roundRect(x,y,width,height,height/2,fill=1,stroke=1)
                elif kind == 'process':c.rect(x,y,width,height,fill=1,stroke=1)
                c.setFillColor(INK);c.setFont('Helvetica',10);c.drawCentredString(x+width/2,y+height/2-3,label)
            def arrow(x1,y1,x2,y2,label=''):
                import math
                c.setStrokeColor(HONEY);c.setLineWidth(1.5);c.line(x1,y1,x2,y2)
                angle=math.atan2(y2-y1,x2-x1)
                for turn in (-.45,.45):c.line(x2,y2,x2-8*math.cos(angle+turn),y2-8*math.sin(angle+turn))
                if label:c.setFillColor(MUTED);c.setFont('Helvetica',9);c.drawCentredString((x1+x2)/2,(y1+y2)/2+10,label)
            block('terminator',60,374,110,46,'Start')
            block('process',245,374,140,46,'Write a note')
            block('decision',465,357,110,80,'Ready?')
            block('terminator',670,374,110,46,'Keep')
            arrow(170,397,245,397);arrow(385,397,465,397);arrow(575,397,670,397,'Yes')
            block('process',450,235,140,46,'Review / edit');arrow(520,357,520,281,'No')
            para(c, '<b>Other current building blocks</b>', 60, 194, 720, 20)
            block('data',60,110,160,48,'Input / output')
            block('ellipse',330,110,160,48,'Ellipse')
            block('text',600,110,160,48,'Editable text box')
            para(c, 'Add a shape, edit its words, connect endpoints, then move or resize. Save/reopen retains editable objects and labels. Database, curves, layers and grouping are not claimed as delivered.',60,85,720,35,SMALL)
            c.showPage()

    heading('Tools appear when they are useful', 'Capture is a menu, colours are labelled swatches, and labels remain editable. These are real app surfaces, not concept drawings.')
    for i, (scene, caption) in enumerate((('capture', 'Plus: add to this note'), ('palette', 'Colour: names and a selected outline'), ('labels', 'Labels: reusable organisation'))):
        image('full-api34', scene, 35+i*259, 88, 247, 403, caption)
    c.showPage()

    if ('full-api34', 'add-reminder') in records:
        heading('Add, do not hide; navigate without restarting', 'Reminder belongs under Plus. Copy/Trash are list actions. Repeated native navigation must still reach Settings.')
        image('full-api34', 'add-reminder', 35, 88, 247, 403, 'Plus: reminder alongside capture and diagram')
        image('night-api34', 'settings-after-navigation', 294, 88, 247, 403, 'Dark: Settings after repeated sheet/page navigation')
        image('full-api36', 'settings-after-navigation', 553, 88, 247, 403, 'Android16 / 0.85 font: Settings without restarting')
        c.showPage()

    heading('Landscape and tablet are real layouts', 'The landscape editor requests no fullscreen/extract IME. Narrow writing columns and bounded sheets avoid stretching every control across a tablet.')
    image('landscape-api34', 'editor-keyboard', 35, 280, 370, 205, 'Landscape with the actual keyboard visible')
    image('landscape-api34', 'format', 435, 280, 370, 205, 'Formatting in constrained height')
    image('tablet-api34', 'home', 35, 83, 370, 170, 'Tablet: adaptive note-card columns')
    image('tablet-api34', 'editor', 435, 83, 370, 170, 'Tablet: centred document, not edge-to-edge text')
    c.showPage()

    heading('What stays, and what is not claimed', 'The redesign removes presentation noise, not existing note capabilities. Features below describe the standalone app, not a completed universal certification.')
    para(c, '<b>Still available</b><br/><br/>Rich text and checklists; labels and 12 colours; search, filters and sorting; pin, Archive, seven-day Trash and Undo; copy/share and conversion; reminders and re-placeable home widget; local folder storage, generations and JSON backup/recovery.<br/><br/><b>Capture</b><br/>Editable diagrams, shapes, text boxes and connectors; vector PDF export; offline OCR with bundled English/Hindi/Kannada models; optional microphone and automatic bundled offline English transcription. Original audio survives recognition failure and has playback/export/retry.<br/><br/><b>Diagram scope</b><br/>The six current types are process, start/end, decision, input/output, ellipse and text box. See DIAGRAMS-AND-EXPORT-v0.5.md for exact implemented operations. Full grouping/layers/curves/smart guides are not silently claimed as complete.', 35, 478, 365, 380)
    para(c, '<b>Privacy and diagnostics</b><br/><br/>Collection is off by default. Enable in Settings before reproducing a problem; export or delete deliberately. Timing, device/configuration and reviewed event outcomes help investigation. Note text, titles, audio, transcripts, images, clipboard and destination URIs are not collected.<br/><br/><b>Qualification boundaries</b><br/>Emulator interaction/layout/raster evidence is not physical OEM, SD-provider, TalkBack, thermal, ARM/16KiB or accent/noise validation. A native/durable component soak is not human typing. No claim of perfect compatibility or automatic aesthetic acceptance.<br/><br/><b>Install safely</b><br/>Use only a gated immutable APK with the stable app.space.notes package/certificate and higher versionCode. Install over the existing app; do not uninstall or clear data. Export important JSON and original voice WAVs separately. Calculator and MySpace remain frozen.', 440, 478, 365, 380)
    c.showPage(); c.save()
    print(f'Wrote {output}: {page} pages; {status}; five review lanes green={all_green}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', required=True)
    parser.add_argument('--output', type=Path, default=ROOT/'NOTES-UI-OVERVIEW.pdf')
    args = parser.parse_args()
    if not args.run.isdigit():
        parser.error('run must be a numeric GitHub Actions run ID')
    build(args.run, args.output)

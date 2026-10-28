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


def geometry(c, name, x, y, width, height):
    """Explanatory vector legend; not presented as a runtime screenshot."""
    import math
    c.saveState();c.translate(x,y);c.scale(width,height);c.setStrokeColor(HONEY);c.setFillColor(HexColor('#F0F1EE'));c.setLineWidth(.022)
    def polygon(points):
        p=c.beginPath();p.moveTo(*points[0])
        for point in points[1:]:p.lineTo(*point)
        p.close();c.drawPath(p,fill=1,stroke=1)
    if name=='Rectangle':c.rect(0,0,1,1,fill=1,stroke=1)
    elif name=='Diamond':polygon([(.5,1),(1,.5),(.5,0),(0,.5)])
    elif name=='Capsule':c.roundRect(0,0,1,1,.35,fill=1,stroke=1)
    elif name=='Parallelogram':polygon([(.2,1),(1,1),(.8,0),(0,0)])
    elif name=='Oval':c.ellipse(0,0,1,1,fill=1,stroke=1)
    elif name=='Triangle':polygon([(.5,1),(1,0),(0,0)])
    elif name=='Right triangle':polygon([(0,1),(1,0),(0,0)])
    elif name in ('Pentagon','Hexagon','Octagon','Star'):
        count={'Pentagon':5,'Hexagon':6,'Octagon':8,'Star':10}[name]
        polygon([(.5+( .23 if name=='Star' and i%2 else .5)*math.cos(math.pi/2+i*2*math.pi/count),.5+( .23 if name=='Star' and i%2 else .5)*math.sin(math.pi/2+i*2*math.pi/count)) for i in range(count)])
    elif name=='Trapezoid':polygon([(.2,1),(.8,1),(1,0),(0,0)])
    elif name=='Cylinder':
        c.rect(0,.15,1,.7,fill=1,stroke=0);c.line(0,.15,0,.85);c.line(1,.15,1,.85);c.ellipse(0,.7,1,1,fill=1,stroke=1);c.bezier(0,.15,0,-.05,1,-.05,1,.15)
    elif name=='Cube':
        polygon([(0,.2),(.6,0),(1,.3),(1,.8),(.4,1),(0,.7)]);c.line(0,.7,.6,.5);c.line(.6,.5,1,.8);c.line(.6,.5,.6,0)
    elif name=='Wavy rectangle':
        p=c.beginPath();p.moveTo(0,1);p.lineTo(0,.2);p.curveTo(.1,0,.3,.4,.5,.2);p.curveTo(.7,0,.9,.4,1,.2);p.lineTo(1,1);p.close();c.drawPath(p,fill=1,stroke=1)
    elif name=='Cloud':
        p=c.beginPath();p.moveTo(.2,.1);p.curveTo(-.1,.1,-.05,.55,.15,.6);p.curveTo(.05,.9,.35,1.1,.5,.85);p.curveTo(.7,1.1,1,.9,.85,.65);p.curveTo(1.15,.6,1.05,.15,.8,.15);p.curveTo(.7,-.05,.35,-.05,.2,.1);p.close();c.drawPath(p,fill=1,stroke=1)
    elif name=='Right arrow':polygon([(0,.25),(.6,.25),(.6,0),(1,.5),(.6,1),(.6,.75),(0,.75)])
    elif name=='Chevron':polygon([(0,0),(.65,0),(1,.5),(.65,1),(0,1),(.35,.5)])
    elif name=='Semicircle':c.wedge(0,-1,1,1,0,180,fill=1,stroke=1)
    elif name=='Heart':
        p=c.beginPath();p.moveTo(.5,0);p.curveTo(.4,.1,0,.45,0,.7);p.curveTo(0,1,.3,1,.5,.8);p.curveTo(.7,1,1,1,1,.7);p.curveTo(1,.45,.6,.1,.5,0);p.close();c.drawPath(p,fill=1,stroke=1)
    else:raise ValueError(name)
    c.restoreState()


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
    para(c, '<b>Evidence boundary</b><br/>These are real emulator screen captures from synthetic fixtures, not photographs of a physical phone and not signed-release UI captures. Signed APK identity/update checks are separate. Green case counts do not automatically approve the look or first-use experience.<br/><br/>Version 0.11 was published and passed emulator gates, but real-device feedback rejected folders, media import, drawing and Settings. This repair review does not erase that feedback: physical-device and user experience remain decisive.', 310, 194, 475, 130, SMALL)
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
        ('Diagrams share the same language', 'diagram', '20 named shapes plus text annotation; Edit shape, Undo/Redo and grouping. Drag shapes directly; no Position form.'),
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
            heading('A simple flowchart, still editable', 'Rectangle, Diamond and Capsule make a familiar flowchart. This explanatory guide is not an emulator capture.')
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
            para(c, '<b>A few more building blocks</b>', 60, 194, 720, 20)
            block('data',60,110,160,48,'Parallelogram')
            block('ellipse',330,110,160,48,'Ellipse')
            block('text',600,110,160,48,'Text annotation')
            para(c, 'Add a shape, edit its words, connect endpoints, then move or resize. Save/reopen retains editable objects and labels. 20 named shapes and text annotation are available. Basic grouping moves linked shapes together; curves, layers and smart guides are not claimed.',60,85,720,35,SMALL)
            c.showPage()

    heading('20 shapes, named for what you see', 'Explanatory vector legend, not runtime captures. Text annotation is an additional tool. Saved legacy shape IDs stay compatible.')
    names=['Rectangle','Diamond','Capsule','Parallelogram','Oval','Triangle','Right triangle','Pentagon','Hexagon','Octagon','Star','Trapezoid','Cylinder','Cube','Wavy rectangle','Cloud','Right arrow','Chevron','Semicircle','Heart']
    for i, name in enumerate(names):
        col,row=i%5,i//5;x=35+col*155;y=400-row*94
        geometry(c,name,x+45,y,65,40)
        c.setFillColor(INK);c.setFont('Helvetica',9);c.drawCentredString(x+77,y-17,name)
    para(c,'<b>Make it yours:</b> add a shape, tap to select, Edit shape to change words/colour, and Connect to draw an arrow. Drag shapes directly. Group selected shapes to move them together. Diagram Undo/Redo and freehand stroke Undo/Redo are separate histories.',35,88,W-70,42,SMALL)
    c.showPage()

    new_surfaces = [
        ('Find the view that fits', ('filters-view','filters-sort','filters-labels'), ('View','Sort','Labels'), 'Swipe between the tabs. Label checkboxes support all, none or any chosen combination.'),
        ('Paper that belongs to the note', ('backgrounds','background-editor','image-editor'), ('16 backgrounds','Independent paper and colour','Embedded image / saved drawing'), 'Images are copied into the note; source picker access is not needed after import. Backups preserve the embedded pixels.'),
        ('Drawing, not just a scratch pad', ('drawing','drawing-tools','drawing-focus'), ('Ink, shapes and widths','Pen, pencil, marker and eraser','Focus workspace; pinch and pan'), 'Saved drawings reopen for editing. Stroke tools preserve their own colour/width. Clear can be undone; export saves the full paper, not the zoomed viewport.'),
        ('Trash is under your control', ('trash','trash-confirmation','settings'), ('Restore or select notes','Confirm permanent deletion','Grouped settings with clear actions'), 'Empty Trash or delete selected trashed notes now. Live notes are kept. Original audio and previous backups remain separate; deletion is not forensic erasure.'),
        ('Tools within reach', ('library','drawing','voice-inbox'), ('Library in the lower screen','Freehand drawing','Separate voice cards and transport'), 'Swipe from the left quarter, including 20% in, to open Library without relying on the OS edge. Drawing has tools/history, focus and pinch navigation; voice cards keep progress and transport together.'),
    ]
    for title, names, captions, description in new_surfaces:
        if all(('full-api34',name) in records for name in names):
            heading(title, description)
            for i, name in enumerate(names):image('full-api34', name, 35+i*259, 88, 247, 403, captions[i])
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
    para(c, '<b>Still available</b><br/><br/>Rich text and checklists; images and freehand drawing; 16 paper backgrounds; labels and 12 colours; search, filters and sorting; pin, Archive, seven-day Trash with explicit permanent deletion and Undo; copy/share and conversion; reminders and re-placeable home widget; real folders and note moves under the chosen storage directory; verified generations and JSON backup/recovery. The View / Sort / Labels sheet supports swipe tabs and any label subset.<br/><br/><b>Capture</b><br/>Editable diagrams, shapes, text boxes and connectors; vector PDF export; offline OCR with bundled English/Hindi/Kannada models; optional microphone and automatic bundled offline English transcription. Original audio survives recognition failure and has playback/export/retry. Separate cards provide play/pause and a seek bar; pause-based transcript punctuation is not semantic restoration of every question or exclamation.<br/><br/><b>Diagram scope</b><br/>20 named geometric/common shapes plus text annotation, editable connectors, basic grouping and diagram Undo/Redo. Drag to move; Edit shape changes words and appearance. Nested groups, layers, curves and smart guides are not claimed.', 35, 478, 365, 380)
    para(c, '<b>Privacy and diagnostics</b><br/><br/>Collection is off by default. Enable in Settings before reproducing a problem; export or delete deliberately. Timing, device/configuration and reviewed event outcomes help investigation. Note text, titles, audio, transcripts, images, clipboard and destination URIs are not collected.<br/><br/><b>Qualification boundaries</b><br/>Emulator interaction/layout/raster evidence is not physical OEM, SD-provider, TalkBack, thermal, ARM/16KiB or accent/noise validation. A native/durable component soak is not human typing. No claim of perfect compatibility or automatic aesthetic acceptance.<br/><br/><b>Install safely</b><br/>Use only a gated immutable APK with the stable app.space.notes package/certificate and higher versionCode. Install over the existing app; do not uninstall or clear data. JSON backups include note images, but original voice recordings remain separate: export those too. Standalone appearance offers System / Light / Dark; future host embedding can supply its design and settings, but actual MySpace integration is not certified. Calculator and MySpace remain frozen.', 440, 478, 365, 380)
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

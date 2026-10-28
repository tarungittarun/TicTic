#!/usr/bin/env python3
"""Build an evidence-first overview, not a second set of aspirational app mockups.
Requires reportlab and Pillow. Inputs are bounded, provenance-pinned emulator captures.
Usage: python3 tools/build_notes_runtime_review_pdf.py --run RUN_ID
"""
import argparse
import hashlib
import json
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
    c.setTitle('MyNotes 0.9 - Paper & Ink / actual runtime overview')
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
        c.drawString(32, 23, f'Actual synthetic debug-host captures | run {run} | source {source[:12]} | {status}')
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

    heading('Paper & Ink', 'MyNotes 0.9.0 (10) - a content-first redesign, with practical evidence instead of a mockup-only promise.')
    image('full-api34', 'editor', 37, 90, 235, 400, 'One writing surface. Optional title. Native caret and selection.')
    para(c, '<b>Less interface. More note.</b><br/><br/>Warm paper and charcoal ink; honey accents only where they help. A shared visual language across the editor, cards, diagrams and settings.<br/><br/><b>Context, not a button wall.</b><br/>Capture, formatting, organisation and note actions appear when needed. Existing capabilities stay available.<br/><br/><b>Paragraphs must behave like paragraphs.</b><br/>H1/H2 remeasure the native text layout. Following lines move naturally; composing a word and pressing Enter are tested.', 310, 470, 475, 260)
    para(c, '<b>Evidence boundary</b><br/>These are real emulator screen captures from synthetic fixtures, not photographs of a physical phone and not signed-release UI captures. Signed APK identity/update checks are separate. Green case counts do not automatically approve the look or first-use experience.<br/><br/>The old 0.8 functional build was published, but the user rejected its UI. This review replaces that acceptance claim; final device/user experience is still decisive.', 310, 194, 475, 130, SMALL)
    c.showPage()

    scenes = [
        ('Your notes, not a control panel', 'home', 'Quiet cards, off-thread search and clear creation actions. Card columns adapt to width and large text.'),
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

    heading('Tools appear when they are useful', 'Capture is a menu, colours are labelled swatches, and labels remain editable. These are real app surfaces, not concept drawings.')
    for i, (scene, caption) in enumerate((('capture', 'Plus: add to this note'), ('palette', 'Colour: names and a selected outline'), ('labels', 'Labels: reusable organisation'))):
        image('full-api34', scene, 35+i*259, 88, 247, 403, caption)
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

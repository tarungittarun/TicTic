#!/usr/bin/env python3
"""Build NOTES-UI-OVERVIEW.pdf -- the Figma-style visual overview of the Notes
rework (Google Keep reference): every page of the app drawn as a board, with
numbered highlight callouts and simple-word explanations.

Run:  python3 tools/build_notes_ui_overview_pdf.py   (needs: pip install reportlab)
Out:  NOTES-UI-OVERVIEW.pdf at the repository root.

Design language of the deck itself: dark board (the app's signature), gold
accent (the honeycomb mark), Keep's 12 note colours, real text in mockups.
Every sidebar uses auto-wrapping frames with a hard fit-check, so an
explanation that does not fit fails the build loudly instead of overflowing.
"""
from reportlab.lib.colors import HexColor
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfgen import canvas as rl_canvas
from reportlab.platypus import Frame, Paragraph

PAGE_W, PAGE_H = landscape(A4)  # 841.9 x 595.3

# ---------------------------------------------------------------- palette
BG        = HexColor("#17181D")
BOARD     = HexColor("#1E2026")
BOARD_LN  = HexColor("#2C2F38")
GOLD      = HexColor("#E8B33C")
GOLD_DK   = HexColor("#B7862A")
INK       = HexColor("#EDEDF0")
INK_2     = HexColor("#B9BCC6")
INK_3     = HexColor("#7E828E")
GREEN     = HexColor("#7BD88F")
BLUE      = HexColor("#7FB2E8")
PINK      = HexColor("#FF7BAC")
PHONE_BG  = HexColor("#101116")
PHONE_LN  = HexColor("#3A3D47")
GLASS     = HexColor("#23252C")
GLASS_LN  = HexColor("#3C404B")

# Keep's 12 note colours, dark-theme tints (mockup-accurate enough to judge by)
KEEP_DARK = {
    "Default":  "#2A2C31", "Red":     "#60302C", "Orange":  "#5F4122",
    "Yellow":   "#5D5526", "Green":   "#2C4F3A", "Teal":    "#24514E",
    "Blue":     "#2A4E6C", "DarkBlue":"#33487C", "Purple":  "#4A3570",
    "Pink":     "#63374F", "Brown":   "#4C3D33", "Gray":    "#3E4147",
}
KEEP_LIGHT = {
    "Default":  "#FFFFFF", "Red":     "#F28B82", "Orange":  "#FCBCA4",
    "Yellow":   "#FDF49F", "Green":   "#CCFF90", "Teal":    "#A7FFEB",
    "Blue":     "#CBF0F8", "DarkBlue":"#AECBFA", "Purple":  "#D7AEFB",
    "Pink":     "#FBCFE8", "Brown":   "#E6C9A8", "Gray":    "#E8EAED",
}

STY_H1 = ParagraphStyle("h1", fontName="Helvetica-Bold", fontSize=26, leading=30, textColor=INK)
STY_H2 = ParagraphStyle("h2", fontName="Helvetica-Bold", fontSize=15, leading=18, textColor=INK)
STY_SUB = ParagraphStyle("sub", fontName="Helvetica", fontSize=10.5, leading=14, textColor=INK_2)
STY_ITEM = ParagraphStyle("item", fontName="Helvetica", fontSize=9.4, leading=12.6, textColor=INK_2, spaceAfter=5)
STY_TINY = ParagraphStyle("tiny", fontName="Helvetica", fontSize=8, leading=10.5, textColor=INK_3)

OUT = "NOTES-UI-OVERVIEW.pdf"
PAGES = 18
_overflow = []


# ---------------------------------------------------------------- helpers
def new_page(c, title, subtitle, page_no):
    c.setFillColor(BG)
    c.rect(0, 0, PAGE_W, PAGE_H, stroke=0, fill=1)
    # faint board grid dots, figma style
    c.setFillColor(HexColor("#232429"))
    for gx in range(24, int(PAGE_W), 48):
        for gy in range(24, int(PAGE_H), 48):
            c.circle(gx, gy, 0.7, stroke=0, fill=1)
    # header
    c.setFillColor(INK)
    c.setFont("Helvetica-Bold", 17)
    c.drawString(40, PAGE_H - 44, title)
    if subtitle:
        c.setFillColor(INK_3)
        c.setFont("Helvetica", 9.5)
        c.drawString(40, PAGE_H - 60, subtitle)
    # honeycomb mini-mark, top right
    hexmark(c, PAGE_W - 52, PAGE_H - 44, 7)
    # footer
    c.setFillColor(INK_3)
    c.setFont("Helvetica", 7.5)
    c.drawString(40, 20, "MySpace Notes · UI & feature overview · Google Keep reference · branch arena/3caca636-tictic")
    c.drawRightString(PAGE_W - 40, 20, f"{page_no} / {PAGES}")


def hexagon(c, cx, cy, r, fill, stroke=None):
    from math import cos, sin, pi
    p = c.beginPath()
    for i in range(6):
        a = pi / 2 + i * pi / 3
        x, y = cx + r * cos(a), cy + r * sin(a)
        if i == 0:
            p.moveTo(x, y)
        else:
            p.lineTo(x, y)
    p.close()
    c.setFillColor(fill)
    if stroke:
        c.setStrokeColor(stroke)
        c.setLineWidth(0.8)
        c.drawPath(p, stroke=1, fill=1)
    else:
        c.drawPath(p, stroke=0, fill=1)


def hexmark(c, cx, cy, r):
    """The owner's honeycomb: six gray hexagons around one gold."""
    from math import cos, sin, pi
    gray = HexColor("#5A5D66")
    for i in range(6):
        a = i * pi / 3
        hexagon(c, cx + 1.78 * r * cos(a), cy + 1.78 * r * sin(a), r * 0.94, gray)
    hexagon(c, cx, cy, r * 0.94, GOLD)


def phone(c, x, y, w=232, h=474, label=None):
    """A phone frame; returns (inner_x, inner_y, inner_w, inner_h)."""
    c.setFillColor(PHONE_LN)
    c.roundRect(x, y, w, h, 22, stroke=0, fill=1)
    c.setFillColor(PHONE_BG)
    c.roundRect(x + 3, y + 3, w - 6, h - 6, 19, stroke=0, fill=1)
    ix, iy, iw, ih = x + 10, y + 10, w - 20, h - 20
    # status bar
    c.setFillColor(INK_3)
    c.setFont("Helvetica", 7)
    c.drawString(ix + 6, iy + ih - 12, "12:30")
    for k in range(3):
        c.rect(ix + iw - 34 + k * 9, iy + ih - 11, 6, 3 + k * 2, stroke=0, fill=1)
    c.rect(ix + iw - 14, iy + ih - 11, 9, 6, stroke=0, fill=1)
    # gesture bar
    c.setFillColor(HexColor("#4A4D57"))
    c.roundRect(ix + iw / 2 - 34, iy + 6, 68, 3.4, 1.7, stroke=0, fill=1)
    if label:
        c.setFillColor(INK_3)
        c.setFont("Helvetica-Bold", 8.5)
        c.drawCentredString(x + w / 2, y - 14, label.upper())
    return ix, iy, iw, ih


def dot(c, x, y, n):
    """Gold numbered highlight callout."""
    c.setFillColor(GOLD)
    c.setStrokeColor(HexColor("#000000"))
    c.setLineWidth(1.1)
    c.circle(x, y, 8.6, stroke=1, fill=1)
    c.setFillColor(HexColor("#20160A"))
    c.setFont("Helvetica-Bold", 9.5)
    c.drawCentredString(x, y - 3.3, str(n))


def sidebar(c, x, y, w, h, title, items, note=None):
    """items = list of (num, text, status) ; status in {'NEW','EXISTS','UPGRADE',''}.
    Auto-wrapping frame + hard fit check."""
    if title:
        c.setFillColor(INK)
        c.setFont("Helvetica-Bold", 12)
        c.drawString(x, y + h + 6, title)
    tagcol = {"NEW": GOLD, "EXISTS": GREEN, "UPGRADE": BLUE, "": None}
    story = []
    for num, text, status in items:
        tag = ""
        if status and tagcol.get(status):
            tag = (f'  <font name="Helvetica-Bold" size="7.4" color="#{tagcol[status].hexval()[2:]}">'
                   f'[{status}]</font>')
        story.append(Paragraph(
            f'<font name="Helvetica-Bold" color="#E8B33C">{num}</font>&nbsp;&nbsp;{text}{tag}',
            STY_ITEM))
    if note:
        story.append(Paragraph(note, STY_TINY))
    f = Frame(x, y, w, h, leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0, showBoundary=0)
    f.addFromList(story, c)
    if story:
        _overflow.append(f"sidebar '{title}' overflowed with {len(story)} paragraphs")


def textbars(c, x, y, w, n, color, gap=6, bh=3.2, widths=None):
    c.setFillColor(color)
    for i in range(n):
        bw = widths[i] if widths else w * (1 - (i % 3) * 0.18)
        c.roundRect(x, y - i * gap, bw, bh, bh / 2, stroke=0, fill=1)


def note_card(c, x, y, w, h, color, title=None, bars=3, pin=False,
              chips=None, checked=None, image=False):
    """A Keep-style note card at (x,y)=bottom-left."""
    c.setFillColor(HexColor(color))
    c.setStrokeColor(HexColor("#00000066"))
    c.setLineWidth(0.6)
    c.roundRect(x, y, w, h, 8, stroke=1, fill=1)
    ty = y + h - 13
    if image:
        c.setFillColor(HexColor("#0E0F13"))
        c.roundRect(x + 5, y + h - 46, w - 10, 41, 5, stroke=0, fill=1)
        c.setFillColor(HexColor("#31343D"))
        c.circle(x + w - 22, y + h - 22, 6, stroke=0, fill=1)  # sun
        p = c.beginPath()  # mountains
        p.moveTo(x + 8, y + h - 44); p.lineTo(x + 22, y + h - 26); p.lineTo(x + 34, y + h - 40)
        p.lineTo(x + 46, y + h - 30); p.lineTo(x + w - 8, y + h - 44); p.close()
        c.setFillColor(HexColor("#454A57")); c.drawPath(p, stroke=0, fill=1)
        ty = y + h - 58
    if title:
        c.setFillColor(INK)
        c.setFont("Helvetica-Bold", 8.6)
        c.drawString(x + 8, ty, title[:int(w / 4.6)])
        ty -= 10
    if bars:
        textbars(c, x + 8, ty, w - 16, bars, HexColor("#00000055"))
    if checked:  # checklist rows
        ry = ty + 2
        for i, done in enumerate(checked):
            ry -= 9
            c.setStrokeColor(INK_2); c.setLineWidth(0.9)
            c.rect(x + 9, ry - 1, 6, 6, stroke=1, fill=0)
            if done:
                c.setStrokeColor(GOLD); c.setLineWidth(1.4)
                p = c.beginPath(); p.moveTo(x + 10, ry + 2); p.lineTo(x + 12, ry); p.lineTo(x + 15, ry + 5)
                c.drawPath(p, stroke=1, fill=0)
            c.setFillColor(HexColor("#00000066"))
            c.roundRect(x + 20, ry, w - 32, 3.4, 1.7, stroke=0, fill=1)
    if pin:
        c.setFillColor(GOLD)
        px, py = x + w - 12, y + h - 10
        p = c.beginPath(); p.moveTo(px, py + 5); p.lineTo(px + 4, py - 1); p.lineTo(px - 4, py - 1); p.close()
        c.drawPath(p, stroke=0, fill=1)
        c.rect(px - 0.7, py - 6, 1.4, 5, stroke=0, fill=1)
    if chips:
        cy2 = y + 8
        cx2 = x + 8
        c.setFont("Helvetica", 6.2)
        for ch in chips:
            wch = 10 + 3.5 * len(ch)
            c.setFillColor(HexColor("#00000066"))
            c.roundRect(cx2, cy2 - 3, wch, 11, 5.5, stroke=0, fill=1)
            c.setFillColor(INK_2)
            c.drawString(cx2 + 5, cy2, ch)
            cx2 += wch + 5


def searchbar(c, x, y, w, hint="Search your notes"):
    c.setFillColor(GLASS)
    c.setStrokeColor(GLASS_LN)
    c.setLineWidth(0.8)
    c.roundRect(x, y, w, 24, 12, stroke=1, fill=1)
    c.setStrokeColor(INK_3)
    c.setLineWidth(1.2)
    c.circle(x + 14, y + 13, 4.4, stroke=1, fill=0)
    c.line(x + 17.2, y + 9.8, x + 20.5, y + 6.5)
    c.setFillColor(INK_3)
    c.setFont("Helvetica", 8.4)
    c.drawString(x + 27, y + 10, hint)


def fab(c, x, y, r=17, glyph="+"):
    c.setFillColor(GOLD)
    c.circle(x, y, r, stroke=0, fill=1)
    c.setFillColor(HexColor("#20160A"))
    c.setFont("Helvetica-Bold", 17)
    c.drawCentredString(x, y - 6, glyph)


def pill(c, x, y, w, h, text, fill=GLASS, ink=INK_2, ln=GLASS_LN, fs=7.6, bold=False):
    c.setFillColor(fill)
    if ln:
        c.setStrokeColor(ln); c.setLineWidth(0.8)
        c.roundRect(x, y, w, h, h / 2, stroke=1, fill=1)
    else:
        c.roundRect(x, y, w, h, h / 2, stroke=0, fill=1)
    c.setFillColor(ink)
    c.setFont("Helvetica-Bold" if bold else "Helvetica", fs)
    c.drawCentredString(x + w / 2, y + h / 2 - fs * 0.36, text)


def section_head(c, x, y, text):
    c.setFillColor(INK_3)
    c.setFont("Helvetica-Bold", 7.6)
    c.drawString(x, y, text.upper())


def panel(c, x, y, w, h, title=None):
    c.setFillColor(BOARD)
    c.setStrokeColor(BOARD_LN)
    c.setLineWidth(1)
    c.roundRect(x, y, w, h, 10, stroke=1, fill=1)
    if title:
        c.setFillColor(INK)
        c.setFont("Helvetica-Bold", 10.5)
        c.drawString(x + 12, y + h - 16, title)


# ================================================================ pages
def p01_cover(c):
    c.setFillColor(BG)
    c.rect(0, 0, PAGE_W, PAGE_H, stroke=0, fill=1)
    c.setFillColor(HexColor("#232429"))
    for gx in range(24, int(PAGE_W), 48):
        for gy in range(24, int(PAGE_H), 48):
            c.circle(gx, gy, 0.7, stroke=0, fill=1)
    hexmark(c, 118, PAGE_H - 150, 26)
    c.setFillColor(INK)
    c.setFont("Helvetica-Bold", 34)
    c.drawString(170, PAGE_H - 142, "MySpace Notes")
    c.setFont("Helvetica-Bold", 15)
    c.setFillColor(GOLD)
    c.drawString(170, PAGE_H - 168, "UI & FEATURE OVERVIEW  ·  referenced to Google Keep")
    c.setFillColor(INK_2)
    c.setFont("Helvetica", 11)
    lines = [
        "Every page of the app, drawn and explained in simple words — the map we build from.",
        "Gold numbers on each screen match the explanation list beside it.  [NEW] = to be built,",
        "[EXISTS] = already working today,  [UPGRADE] = exists but grows Keep-grade.",
        "",
        "100 Keep features + diagrams  ·  Offline OCR + speech  ·  User-selected local folder",
        "Companion documents:  NOTES-PLAN.md (the full 100, in detail)  ·  MASTERCLASS.md (how we work)",
    ]
    yy = PAGE_H - 205
    for ln in lines:
        c.drawString(170, yy, ln)
        yy -= 17
    # colour strip: the 12 Keep colours, dark + light
    x0 = 170
    c.setFillColor(INK_3); c.setFont("Helvetica-Bold", 8)
    c.drawString(x0, 150, "THE 12 NOTE COLOURS — DARK THEME")
    for i, (name, col) in enumerate(KEEP_DARK.items()):
        c.setFillColor(HexColor(col)); c.setStrokeColor(BOARD_LN); c.setLineWidth(0.7)
        c.roundRect(x0 + i * 34, 118, 28, 26, 6, stroke=1, fill=1)
    c.drawString(x0, 96, "LIGHT THEME")
    for i, (name, col) in enumerate(KEEP_LIGHT.items()):
        c.setFillColor(HexColor(col)); c.setStrokeColor(BOARD_LN)
        c.roundRect(x0 + i * 34, 64, 28, 26, 6, stroke=1, fill=1)
    c.setFillColor(INK_3); c.setFont("Helvetica", 8)
    c.drawString(x0, 40, "2026-10-27 · prepared for the owner · the calculator stays frozen while Notes is built (microservice rule)")


def p02_map(c):
    new_page(c, "The app map — every page and how you reach it", "One drawing of the whole app. Boxes are pages; arrows are taps.", 2)
    def box(x, y, w, h, t, s=None, gold=False):
        c.setFillColor(GLASS if not gold else HexColor("#33290F"))
        c.setStrokeColor(GOLD if gold else GLASS_LN); c.setLineWidth(1.1 if gold else 0.8)
        c.roundRect(x, y, w, h, 8, stroke=1, fill=1)
        c.setFillColor(INK); c.setFont("Helvetica-Bold", 9.5); c.drawCentredString(x + w / 2, y + h - 15, t)
        if s:
            c.setFillColor(INK_3); c.setFont("Helvetica", 7.4); c.drawCentredString(x + w / 2, y + h - 26, s)
    def arrow(x1, y1, x2, y2, lab=None):
        c.setStrokeColor(INK_3); c.setLineWidth(1)
        c.line(x1, y1, x2, y2)
        from math import atan2, cos, sin
        a = atan2(y2 - y1, x2 - x1)
        p = c.beginPath()
        p.moveTo(x2, y2)
        p.lineTo(x2 - 7 * cos(a - 0.42), y2 - 7 * sin(a - 0.42))
        p.lineTo(x2 - 7 * cos(a + 0.42), y2 - 7 * sin(a + 0.42))
        p.close(); c.setFillColor(INK_3); c.drawPath(p, stroke=0, fill=1)
        if lab:
            c.setFont("Helvetica", 7); c.setFillColor(INK_3)
            c.drawCentredString((x1 + x2) / 2, (y1 + y2) / 2 + 5, lab)
    # centre column
    box(330, 400, 170, 44, "HOME — card wall", "pinned, colours, chips", gold=True)
    box(60, 400, 150, 44, "NAV DRAWER", "☰ menu")
    box(60, 320, 150, 34, "Reminders page")
    box(60, 276, 150, 34, "Label pages (one per label)")
    box(60, 232, 150, 34, "Archive")
    box(60, 188, 150, 34, "Trash  (7-day)")
    box(60, 144, 150, 34, "Settings")
    arrow(210, 420, 330, 420, "tap page")
    arrow(270, 400, 270, 178)
    for yv in (337, 293, 249, 205, 161):
        c.setStrokeColor(INK_3); c.setLineWidth(1); c.line(210, yv, 270, yv)
    # right of home
    box(600, 470, 180, 40, "EDITOR — text note", "tap a card / FAB +")
    box(600, 418, 180, 40, "EDITOR — checklist", "list button / convert")
    box(600, 366, 180, 40, "DRAWING canvas", "pen button")
    box(600, 314, 180, 40, "VOICE recorder", "mic button")
    box(600, 262, 180, 40, "PHOTO note", "camera / picker")
    box(600, 210, 180, 40, "SEARCH + filters", "tap search bar")
    box(600, 158, 180, 40, "SELECTION mode", "long-press a card")
    arrow(500, 430, 600, 486); arrow(500, 426, 600, 436)
    arrow(500, 420, 600, 384); arrow(500, 414, 600, 332)
    arrow(500, 408, 600, 280); arrow(500, 402, 600, 228)
    arrow(500, 398, 600, 176)
    # capture entrances from outside
    box(330, 150, 170, 36, "SHARE SHEET IN", "any app → new note")
    box(330, 100, 170, 36, "HOME-SCREEN WIDGETS", "capture · note · list")
    arrow(415, 186, 415, 400, "creates")
    arrow(415, 136, 415, 150)
    c.setFillColor(INK_3); c.setFont("Helvetica", 8)
    c.drawCentredString(415, 84, "everything works offline, with no account — the whole map lives on the phone")
    sidebar(c, 330, 300 - 60, 450, 60, None, [
        ("★", "The five daily surfaces: HOME (everything at a glance), EDITOR (autosave, no Save button), "
              "SEARCH (finds text, images' words, labels), DRAWER (Reminders · Labels · Archive · Trash), WIDGETS (capture without opening).", ""),
    ])


def p03_home(c):
    new_page(c, "Page 1 — Home, the card wall", "Open the app: everything you own, at a glance. This is Keep's face, and ours.", 3)
    ix, iy, iw, ih = phone(c, 44, 64)
    y = iy + ih - 34
    searchbar(c, ix + 4, y - 16, iw - 60)
    # view toggle + hamburger
    c.setStrokeColor(INK_3); c.setLineWidth(1.1)
    for k in range(3):
        c.line(ix + iw - 44, y - 6 - k * 4.4, ix + iw - 30, y - 6 - k * 4.4)
    c.rect(ix + iw - 22, y - 14, 5, 5, stroke=1, fill=0); c.rect(ix + iw - 14, y - 14, 5, 5, stroke=1, fill=0)
    c.rect(ix + iw - 22, y - 6, 5, 5, stroke=1, fill=0); c.rect(ix + iw - 14, y - 6, 5, 5, stroke=1, fill=0)
    section_head(c, ix + 6, y - 34, "Pinned")
    note_card(c, ix + 5, y - 106, iw / 2 - 8, 66, KEEP_DARK["Yellow"], title="Rent receipt", bars=2,
              pin=True, chips=["⏰ Tomorrow"])
    note_card(c, ix + iw / 2 + 3, y - 118, iw / 2 - 8, 78, KEEP_DARK["Teal"], title="Trip packing",
              checked=[True, False, False], pin=True)
    section_head(c, ix + 6, y - 126, "Others")
    note_card(c, ix + 5, y - 196, iw / 2 - 8, 62, KEEP_DARK["Default"], title="Groceries",
              checked=[True, True, False, False])
    note_card(c, ix + iw / 2 + 3, y - 190, iw / 2 - 8, 56, KEEP_DARK["Pink"], title="Gift ideas",
              bars=3, chips=["#family"])
    note_card(c, ix + 5, y - 288, iw / 2 - 8, 84, KEEP_DARK["Default"], image=True, title=None, bars=2)
    note_card(c, ix + iw / 2 + 3, y - 272, iw / 2 - 8, 74, KEEP_DARK["Purple"], title="Song lines",
              bars=4, chips=["🎙 0:41"])
    note_card(c, ix + 5, y - 340, iw / 2 - 8, 44, KEEP_DARK["Green"], title="WiFi password", bars=1)
    note_card(c, ix + iw / 2 + 3, y - 348, iw / 2 - 8, 68, KEEP_DARK["Default"], title="Sketch idea",
              image=True, bars=0)
    fab(c, ix + iw - 24, iy + 30)
    # quick capture icons beside FAB
    c.setStrokeColor(INK_2); c.setLineWidth(1)
    c.circle(ix + iw - 62, iy + 30, 4, stroke=1, fill=0)  # list
    c.line(ix + iw - 68, iy + 38, ix + iw - 56, iy + 38)
    c.line(ix + iw - 70, iy + 24, ix + iw - 62, iy + 34)  # draw
    c.rect(ix + iw - 96, iy + 25, 11, 9, stroke=1, fill=0)  # camera
    c.circle(ix + iw - 90.5, iy + 29.5, 2.4, stroke=1, fill=0)
    # callouts
    dot(c, ix + 20, y - 4, 1); dot(c, ix + iw - 50, y - 8, 2)
    dot(c, ix + 30, y - 36, 3); dot(c, ix + iw - 30, y - 60, 4)
    dot(c, ix + 30, y - 150, 5); dot(c, ix + iw / 2 - 6, y - 176, 6)
    dot(c, ix + iw - 44, iy + 44, 7); dot(c, ix + iw - 78, iy + 44, 8)
    sidebar(c, 316, 96, 486, 400, "What you see, in simple words", [
        ("1", "<b>Search bar</b> — tap and type; notes filter while you type (off-thread, so it never lags).", "UPGRADE"),
        ("2", "<b>Drawer ☰ and view toggle</b> — menu on the left; the squares icon switches the wall between two columns and one.", "NEW"),
        ("3", "<b>“Pinned” section</b> — pinned notes always ride on top, under their own small header.", "UPGRADE"),
        ("4", "<b>Pin mark</b> — gold pin on the card corner; pinning is one tap in the editor too.", "EXISTS"),
        ("5", "<b>“Others” section</b> — newest first. New notes always land here at the top.", "UPGRADE"),
        ("6", "<b>Colours</b> — 12 Keep colours paint whole cards; every colour is contrast-checked so text stays readable in dark and light.", "UPGRADE"),
        ("7", "<b>FAB “+”</b> — one tap opens a fresh note with the keyboard already up. Back = saved. There is no Save button, like Keep.", "EXISTS"),
        ("8", "<b>Quick-capture row</b> — beside the FAB: new checklist, new drawing, new voice note, new photo. Four taps to any note type.", "NEW"),
        ("9", "<b>Chips on cards</b> — a card shows what it carries: ⏰ reminder time, 🎙 audio length, #label names — without opening it.", "NEW"),
        ("10", "<b>Image & drawing cards</b> — the picture IS the card; text sits under it. Tap for the full-screen viewer.", "NEW"),
        ("11", "<b>Long-press any card</b> — selection mode for bulk actions (page 11). Long-press + drag reorders the wall (ships last — riskiest gesture).", "NEW"),
    ], note="Empty states speak: when a section has nothing, it says what belongs there — never a dead screen.")


def p04_drawer(c):
    new_page(c, "Page 2 — The drawer, Archive, Trash, Reminders, Settings", "The ☰ menu: five shelves where notes live besides the wall.", 4)
    ix, iy, iw, ih = phone(c, 44, 64)
    # drawer panel over dimmed home
    c.setFillColor(HexColor("#000000"))
    c.setFillAlpha(0.45)
    c.rect(ix, iy, iw, ih, stroke=0, fill=1)
    c.setFillAlpha(1)
    c.setFillColor(GLASS)
    c.roundRect(ix, iy, iw * 0.82, ih, 0, stroke=0, fill=1)
    c.setStrokeColor(GLASS_LN); c.setLineWidth(0.8)
    c.line(ix + iw * 0.82, iy, ix + iw * 0.82, iy + ih)
    hexmark(c, ix + 26, iy + ih - 40, 9)
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 12)
    c.drawString(ix + 44, iy + ih - 44, "MySpace Notes")
    rows = [("Notes", True), ("Reminders  ·  2", False), ("LABELS", None),
            ("#family", False), ("#work", False), ("#ideas  ·  12", False),
            ("Archive", False), ("Trash", False), ("Settings", False)]
    ry = iy + ih - 74
    for name, active in rows:
        if active is None:
            section_head(c, ix + 16, ry, name); ry -= 20; continue
        if active:
            c.setFillColor(HexColor("#33290F"))
            c.roundRect(ix + 8, ry - 6, iw * 0.82 - 16, 22, 11, stroke=0, fill=1)
        c.setFillColor(GOLD if active else INK_2)
        c.setFont("Helvetica-Bold" if active else "Helvetica", 9.4)
        c.drawString(ix + 18, ry, name)
        ry -= 28
    pill(c, ix + 16, ry + 2, iw * 0.82 - 40, 20, "+  Create new label", fill=HexColor("#00000000"), ink=GOLD, ln=GOLD, fs=8.4)
    dot(c, ix + iw * 0.82 - 14, iy + ih - 100, 1)
    dot(c, ix + iw * 0.82 - 14, iy + ih - 190, 2)
    dot(c, ix + iw * 0.82 - 14, iy + ih - 296, 3)
    dot(c, ix + iw * 0.82 - 14, iy + 120, 4)
    # lifecycle diagram panel
    panel(c, 316, 96, 486, 210, "Trash & archive lifecycle — nothing is ever lost by accident")
    def stage(x, y, w, h, t, s, col):
        c.setFillColor(HexColor("#26282F")); c.setStrokeColor(col); c.setLineWidth(1.2)
        c.roundRect(x, y, w, h, 7, stroke=1, fill=1)
        c.setFillColor(INK); c.setFont("Helvetica-Bold", 8.6); c.drawCentredString(x + w / 2, y + h - 13, t)
        c.setFillColor(INK_3); c.setFont("Helvetica", 7)
        for i, sl in enumerate(s):
            c.drawCentredString(x + w / 2, y + h - 24 - i * 9, sl)
    stage(332, 120, 100, 58, "ON THE WALL", ["normal life"], INK_3)
    stage(452, 120, 100, 58, "ARCHIVE", ["hidden, searchable", "forever · 1-tap back"], BLUE)
    stage(572, 120, 100, 58, "TRASH", ["7-day countdown", "Restore / Delete forever"], GOLD)
    stage(692, 120, 96, 58, "GONE", ["auto-purged", "on day 8"], HexColor("#8A5A5A"))
    c.setStrokeColor(INK_3); c.setLineWidth(1)
    c.line(432, 149, 452, 149); c.line(552, 149, 572, 149); c.line(672, 149, 692, 149)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7)
    c.drawCentredString(442, 154, "archive"); c.drawCentredString(562, 154, "delete"); c.drawCentredString(682, 154, "day 8")
    c.setFont("Helvetica", 8.2); c.setFillColor(INK_2)
    c.drawString(332, 196, "Undo snackbar follows every move: “Moved to trash · UNDO”.  “Empty trash” asks once, then obeys.")
    sidebar(c, 316, 320 - 8, 486, 170, None, [
        ("1", "<b>Reminders shelf</b> — every note with an upcoming reminder, soonest first.", "NEW"),
        ("2", "<b>Labels shelf</b> — your labels listed like folders, but one note can live in many. Create up to 50.", "NEW"),
        ("3", "<b>Archive</b> — filed away, still searchable and editable. Filing, not shredding.", "EXISTS"),
        ("4", "<b>Trash</b> — deleted notes rest 7 days; restore, delete forever, or empty in one tap.", "NEW"),
        ("5", "<b>Settings</b> — theme (dark/light/system), font size S/M/L, list behaviour, reminder defaults.", "NEW"),
    ])


def p05_editor(c):
    new_page(c, "Page 3 — The text editor & formatting", "Tap any card (or +): title, body, and a bottom bar that does everything. Back = saved.", 5)
    ix, iy, iw, ih = phone(c, 44, 64)
    # top bar
    c.setStrokeColor(INK_2); c.setLineWidth(1.3)
    c.line(ix + 8, iy + ih - 26, ix + 16, iy + ih - 32); c.line(ix + 16, iy + ih - 32, ix + 8, iy + ih - 38)
    c.line(ix + 8, iy + ih - 32, ix + 20, iy + ih - 32)
    c.setStrokeColor(GOLD); c.setLineWidth(1.2)
    px, py = ix + iw - 18, iy + ih - 30
    p = c.beginPath(); p.moveTo(px, py + 6); p.lineTo(px + 5, py - 2); p.lineTo(px - 5, py - 2); p.close()
    c.drawPath(p, stroke=1, fill=0); c.rect(px - 0.8, py - 7, 1.6, 5, stroke=1, fill=0)
    # title & body
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 13)
    c.drawString(ix + 10, iy + ih - 62, "Diwali shopping")
    c.setFont("Helvetica", 9.4); c.setFillColor(INK_2)
    body = ["Sweets for the family", "Rangoli colours — ", "new torans for the door", "gift for Anitha aunt"]
    yy = iy + ih - 82
    for b in body:
        c.drawString(ix + 10, yy, b); yy -= 15
    c.setStrokeColor(GOLD); c.setLineWidth(1.4)
    c.line(ix + 118, yy + 18, ix + 118, yy + 6)  # caret
    # selection + formatting toolbar (Keep's underlined-A bar)
    c.setFillColor(HexColor("#00000055"))
    c.roundRect(ix + 10, yy + 16, 96, 12, 3, stroke=0, fill=1)  # selected text highlight
    sel_y = yy - 4
    c.setFillColor(GLASS); c.setStrokeColor(GLASS_LN); c.setLineWidth(0.8)
    c.roundRect(ix + 6, sel_y - 24, iw - 12, 26, 13, stroke=1, fill=1)
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 8.6)
    labels = ["H1", "H2", "Aa", "B", "I", "U", "S", "⌫fmt"]
    xx = ix + 18
    for lb in labels:
        if lb == "B": c.setFont("Helvetica-Bold", 9)
        elif lb == "I": c.setFont("Helvetica-Oblique", 9)
        else: c.setFont("Helvetica", 8.4)
        c.setFillColor(GOLD if lb in ("H1", "B") else INK_2)
        c.drawString(xx, sel_y - 15, lb)
        xx += 25
    # bottom bar
    by = iy + 22
    c.setStrokeColor(GLASS_LN); c.setLineWidth(0.8)
    c.line(ix + 4, by + 18, ix + iw - 4, by + 18)
    c.setFillColor(INK_2); c.setFont("Helvetica-Bold", 12)
    c.drawString(ix + 12, by + 4, "+")
    c.setStrokeColor(INK_2); c.circle(ix + 40, by + 8, 6.4, stroke=1, fill=0)  # palette
    c.setFillColor(INK_2)
    for k, a in enumerate(range(0, 360, 60)):
        from math import cos, sin, radians
        c.circle(ix + 40 + 4 * cos(radians(a)), by + 8 + 4 * sin(radians(a)), 1.5, stroke=0, fill=1)
    c.setFont("Helvetica-Bold", 9.4)  # format A
    c.drawString(ix + 58, by + 4, "A̲")
    c.setStrokeColor(INK_2); c.setLineWidth(1)  # bell
    c.circle(ix + iw - 58, by + 9, 4.6, stroke=1, fill=0)
    c.line(ix + iw - 63, by + 4, ix + iw - 53, by + 4)
    c.setFont("Helvetica-Bold", 10); c.setFillColor(INK_2)  # 3-dot
    c.drawString(ix + iw - 26, by + 4, "⋮")
    c.setFont("Helvetica", 6.6); c.setFillColor(INK_3)
    c.drawString(ix + 10, by - 8, "Edited 27 Oct, 12:40")
    dot(c, ix + iw - 18, iy + ih - 12, 1); dot(c, ix + 20, iy + ih - 58, 2)
    dot(c, ix + iw / 2, yy + 34, 3); dot(c, ix + 30, by + 26, 4)
    dot(c, ix + iw - 40, by + 26, 5); dot(c, ix + 52, by - 10, 6)
    sidebar(c, 316, 96, 486, 400, "How it works, in simple words", [
        ("1", "<b>Pin at the top-right</b> — one tap pins this note to the top of the wall.", "EXISTS"),
        ("2", "<b>Title is optional</b> — no title? The first body line becomes the card's face.", "EXISTS"),
        ("3", "<b>Formatting toolbar (the underlined “A”)</b> — select text → H1/H2 headings, Bold, Italic, Underline, Strikethrough, clear-formatting. Keep got this in Oct 2023; cards render it too.", "NEW"),
        ("4", "<b>Markdown-style triggers</b> — type * or - then space: the line becomes a bullet. Type 1. then space: a numbered list starts itself.", "NEW"),
        ("5", "<b>Bottom bar</b> — + Add (image · drawing · audio), colour palette, format A, reminder bell, 3-dot (Delete · Make a copy · Send · Labels · Convert to checklist · Merge).", "NEW"),
        ("6", "<b>“Edited …” timestamp</b> — Indian-locale dates, subtle at the bottom; sorting can use it.", "NEW"),
        ("7", "<b>Autosave</b> — there is no Save button. Back, or tapping outside, saves. Empty note? It quietly disappears.", "EXISTS"),
        ("8", "<b>Links are live</b> — a URL in the body opens on tap, copies on long-press.", "NEW"),
        ("9", "<b>Font size</b> — Settings chooses Small / Medium / Large for editor and cards alike.", "NEW"),
    ], note="Undo snackbars follow delete/archive/colour changes — a wrong tap is never a lost note.")


def p06_checklist(c):
    new_page(c, "Page 4 — Checklist notes & list customization", "Keep's grocery-list engine: checkboxes, nesting, drag-reorder, hidden checked items, custom bullets.", 6)
    ix, iy, iw, ih = phone(c, 44, 64)
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 12)
    c.drawString(ix + 10, iy + ih - 44, "Groceries")
    items = [("Milk 2L", True, 0), ("Bread", True, 0), ("Paneer", False, 0), ("For curry:", False, 0),
             ("Tomatoes ½kg", False, 1), ("Coriander", False, 1), ("Ghee", False, 0), ("Sweets for puja", False, 0)]
    ry = iy + ih - 66
    for text, done, indent in items:
        bx = ix + 12 + indent * 16
        c.setStrokeColor(INK_3 if not done else GOLD); c.setLineWidth(1)
        c.rect(bx, ry - 2, 8, 8, stroke=1, fill=0)
        if done:
            c.setStrokeColor(GOLD); c.setLineWidth(1.5)
            p = c.beginPath(); p.moveTo(bx + 1.4, ry + 2); p.lineTo(bx + 3.6, ry - 0.4); p.lineTo(bx + 7, ry + 5)
            c.drawPath(p, stroke=1, fill=0)
        c.setFillColor(INK_3 if done else INK_2)
        c.setFont("Helvetica", 9)
        c.drawString(bx + 14, ry, text)
        if indent:
            c.setStrokeColor(HexColor("#3A3D47")); c.setLineWidth(0.8)
            c.line(bx - 7, ry + 10, bx - 7, ry - 4)
        # drag grip
        c.setFillColor(HexColor("#4A4D57"))
        for k in range(3):
            c.circle(ix + iw - 16, ry + 2 - k * 3.4, 0.9, stroke=0, fill=1)
        ry -= 18
    # hidden checked footer
    c.setFillColor(GLASS); c.setStrokeColor(GLASS_LN); c.setLineWidth(0.8)
    c.roundRect(ix + 10, ry - 6, iw - 20, 20, 10, stroke=1, fill=1)
    c.setFillColor(INK_3); c.setFont("Helvetica", 8)
    c.drawString(ix + 20, ry, "2 checked items  ▾")
    # customize-list popup (our dark-pill signature)
    py = iy + 34
    c.setFillColor(HexColor("#000000")); c.setFillAlpha(0.4)
    c.rect(ix, iy, iw, ih - 10, stroke=0, fill=1); c.setFillAlpha(1)
    c.setFillColor(GLASS); c.setStrokeColor(GOLD); c.setLineWidth(1)
    c.roundRect(ix + 14, py, iw - 28, 92, 16, stroke=1, fill=1)
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 9)
    c.drawString(ix + 28, py + 76, "Customize list")
    glyphs = ["✓", "•", "■", "◆", "★", "♥"]
    for k, g in enumerate(glyphs):
        c.setFillColor(GOLD if k == 0 else INK_2)
        c.setFont("Helvetica-Bold", 11)
        c.drawCentredString(ix + 36 + k * 30, py + 52, g)
    for k, col in enumerate(["#E8B33C", "#7BD88F", "#7FB2E8", "#FF7BAC", "#EDEDF0"]):
        c.setFillColor(HexColor(col))
        c.circle(ix + 36 + k * 30, py + 24, 7, stroke=0, fill=1)
    c.setStrokeColor(HexColor("#20160A")); c.setLineWidth(1.6)
    c.circle(ix + 36, py + 24, 7, stroke=1, fill=0)
    dot(c, ix + iw - 16, iy + ih - 100, 1); dot(c, ix + 30, iy + ih - 122, 2)
    dot(c, ix + iw / 2, ry + 4, 3); dot(c, ix + iw / 2, py + 100, 4)
    sidebar(c, 316, 96, 486, 400, "Every list behaviour, in simple words", [
        ("1", "<b>Drag-reorder</b> — the grip dots on the right: hold and slide any item to a new place.", "NEW"),
        ("2", "<b>Nested items</b> — indent any item to make it a sub-step (Keep's nested bullets); the guide line shows the family.", "NEW"),
        ("3", "<b>“N checked items” footer</b> — hide-checked collapses done items into one tappable row. The grocery list stays short.", "NEW"),
        ("4", "<b>Customize list</b> — Keep's 2024 feature, in OUR signature dark pill (never a stock dialog): pick the bullet glyph (✓ • ■ ◆ ★ ♥) and the theme colour; the card wears it too.", "NEW"),
        ("5", "<b>Enter adds the next checkbox</b> — the list grows as you type; a blank last line silently drops on save.", "UPGRADE"),
        ("6", "<b>Swipe a checked item</b> — flick a done item away; Undo snackbar if you change your mind.", "NEW"),
        ("7", "<b>Text ⇄ checklist</b> — 3-dot converts any text note's lines into checkboxes and back, losslessly (our lists are stored as plain text — search and export always work).", "NEW"),
        ("8", "<b>Links inside items</b> — a URL in a list item stays clickable.", "UPGRADE"),
    ], note="Checkboxes tick instantly; Keep does not strikethrough by default — done items dim or hide, never cross out.")


def p07_capture(c):
    new_page(c, "Page 5 — Five ways a note gets born", "Keep's capture paths: FAB, quick actions, share-in from any app, and the home-screen widget.", 7)
    # four mini panels
    def mini(x, y, w, h, title):
        panel(c, x, y, w, h, title)
    mini(40, 300, 236, 210, "1 · FAB + quick row")
    ix2, iy2 = 60, 320
    fab(c, ix2 + 176, iy2 + 40, 16)
    c.setStrokeColor(INK_2); c.setLineWidth(1)
    c.circle(ix2 + 130, iy2 + 40, 5, stroke=1, fill=0)
    c.line(ix2 + 122, iy2 + 50, ix2 + 138, iy2 + 50)
    c.line(ix2 + 118, iy2 + 32, ix2 + 128, iy2 + 46)
    c.rect(ix2 + 92, iy2 + 34, 12, 10, stroke=1, fill=0)
    c.circle(ix2 + 98, iy2 + 39, 2.6, stroke=1, fill=0)
    c.rect(ix2 + 66, iy2 + 33, 8, 13, stroke=1, fill=0)
    c.arc(ix2 + 62, iy2 + 40, ix2 + 78, iy2 + 52, 200, 140)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7.6)
    c.drawString(ix2, iy2 + 8, "+ note   ·   list   ·   drawing   ·   camera   ·   mic")
    c.drawString(ix2, iy2 - 8, "one tap each; the editor opens already in that mode")
    c.setFillColor(INK_2); c.setFont("Helvetica", 8.2)
    c.drawString(ix2, iy2 + 78, "Home, bottom-right:")
    c.drawString(ix2, iy2 + 120, "The keyboard is already up when the")
    c.drawString(ix2, iy2 + 108, "note opens — capture takes one tap.")

    mini(292, 300, 236, 210, "2 · SHARE SHEET IN  [NEW]")
    sx, sy = 312, 322
    c.setFillColor(HexColor("#26282F")); c.setStrokeColor(GLASS_LN); c.setLineWidth(0.8)
    c.roundRect(sx, sy + 96, 196, 44, 8, stroke=1, fill=1)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7.4)
    c.drawString(sx + 10, sy + 126, "Chrome · article selected")
    c.setFillColor(INK_2); c.setFont("Helvetica", 8)
    c.drawString(sx + 10, sy + 110, "“Diwali sale ends Monday…”  → Share")
    c.setFillColor(GLASS); c.roundRect(sx, sy + 16, 196, 66, 10, stroke=1, fill=1)
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 8.4)
    c.drawString(sx + 10, sy + 68, "Share to")
    for k, nm in enumerate(["MySpace Notes", "Messages", "Gmail"]):
        c.setFillColor(GOLD if k == 0 else INK_3)
        c.circle(sx + 24 + k * 62, sy + 44, 9, stroke=0, fill=1)
        c.setFont("Helvetica", 6.6)
        c.drawCentredString(sx + 24 + k * 62, sy + 26, nm)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7.6)
    c.drawString(sx, sy, "text or images from ANY app land as a new note")

    mini(544, 300, 258, 210, "3 · HOME-SCREEN WIDGET  [NEW]")
    wx, wy = 564, 340
    c.setFillColor(HexColor("#26282F")); c.setStrokeColor(GLASS_LN)
    c.roundRect(wx, wy + 60, 218, 52, 10, stroke=1, fill=1)
    for k, (g, nm) in enumerate([("+", "Note"), ("☑", "List"), ("🎙", "Voice"), ("📷", "Camera")]):
        c.setFillColor(GOLD); c.circle(wx + 30 + k * 54, wy + 92, 12, stroke=0, fill=1)
        c.setFillColor(HexColor("#20160A")); c.setFont("Helvetica-Bold", 10)
        c.drawCentredString(wx + 30 + k * 54, wy + 88, g)
        c.setFillColor(INK_3); c.setFont("Helvetica", 6.8)
        c.drawCentredString(wx + 30 + k * 54, wy + 70, nm)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7.6)
    c.drawString(wx, wy + 40, "Keep's signature quick-capture widget: four doors,")
    c.drawString(wx, wy + 28, "resizable, re-placeable (your standing widget rule).")
    c.drawString(wx, wy + 8, "Two more widgets live on page 14: single-note & list.")

    mini(40, 96, 762, 176, "Why capture matters — the Keep lesson")
    sidebar(c, 60, 116, 720, 130, None, [
        ("★", "Keep's own help text says it plainly: <b>“Quickly capture text, links, or images from any mobile app via the Share menu.”</b> "
              "The note app people keep is the one that is faster to feed than to forget. Everything above exists for speed: one tap from the wall, "
              "one tap from any other app, one tap from the home screen — and never a Save button anywhere.", ""),
        ("!", "Share-target and widgets are <b>host-level</b> surfaces (they live in the MySpace / MyNotes app shell, not inside the notes engine) — "
              "so they arrive in the integration round without touching the feature module. That is the microservice rule keeping other apps safe.", ""),
    ])


def p08_media(c):
    new_page(c, "Page 6 — Images, “grab image text”, and drawings", "Photos become notes; their words become searchable; fingers and styluses draw.", 8)
    ix, iy, iw, ih = phone(c, 44, 64)
    # image note editor
    c.setFillColor(HexColor("#0E0F13"))
    c.roundRect(ix + 6, iy + ih - 170, iw - 12, 130, 6, stroke=0, fill=1)
    c.setFillColor(HexColor("#31343D")); c.circle(iw - 40 + ix, iy + ih - 70, 14, stroke=0, fill=1)
    p = c.beginPath()
    p.moveTo(ix + 10, iy + ih - 166); p.lineTo(ix + 60, iy + ih - 100); p.lineTo(ix + 100, iy + ih - 140)
    p.lineTo(ix + 140, iy + ih - 96); p.lineTo(ix + iw - 10, iy + ih - 150); p.lineTo(ix + iw - 10, iy + ih - 166)
    p.close(); c.setFillColor(HexColor("#454A57")); c.drawPath(p, stroke=0, fill=1)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7)
    c.drawCentredString(ix + iw / 2, iy + ih - 60, "photo note — tap for full screen")
    c.setFillColor(INK); c.setFont("Helvetica", 9.2)
    c.drawString(ix + 10, iy + ih - 192, "Bill — Diwali shopping")
    c.setFillColor(INK_2); c.setFont("Helvetica", 8.4)
    c.drawString(ix + 10, iy + ih - 210, "₹4,350 · Big Bazaar")
    # OCR flow
    oy = iy + ih - 250
    c.setFillColor(GLASS); c.setStrokeColor(GOLD); c.setLineWidth(1)
    c.roundRect(ix + 8, oy, iw - 16, 62, 8, stroke=1, fill=1)
    c.setFillColor(GOLD); c.setFont("Helvetica-Bold", 8.4)
    c.drawString(ix + 18, oy + 46, "3-dot ▸ Grab image text  (OCR)")
    c.setFillColor(INK_2); c.setFont("Helvetica", 7.6)
    c.drawString(ix + 18, oy + 30, "reads the photo's words ON THE PHONE,")
    c.drawString(ix + 18, oy + 19, "drops them into the note body — and now")
    c.drawString(ix + 18, oy + 8, "search finds “Big Bazaar” inside a photo.")
    # drawing canvas
    dy = iy + 30
    c.setFillColor(HexColor("#141519")); c.setStrokeColor(GLASS_LN); c.setLineWidth(0.8)
    c.roundRect(ix + 8, dy + 40, iw - 16, 110, 8, stroke=1, fill=1)
    c.setStrokeColor(PINK); c.setLineWidth(2.4)
    p = c.beginPath(); p.moveTo(ix + 30, dy + 120)
    p.curveTo(ix + 60, dy + 150, ix + 90, dy + 90, ix + 130, dy + 130)
    p.curveTo(ix + 150, dy + 148, ix + 170, dy + 100, ix + 190, dy + 118)
    c.drawPath(p, stroke=1, fill=0)
    c.setStrokeColor(GOLD); c.setLineWidth(5)
    c.setFillAlpha(0.35)
    p = c.beginPath(); p.moveTo(ix + 40, dy + 80); p.lineTo(ix + 110, dy + 70); c.drawPath(p, stroke=1, fill=0)
    c.setFillAlpha(1)
    c.setStrokeColor(BLUE); c.setLineWidth(1.6)
    p = c.beginPath(); p.moveTo(ix + 130, dy + 66); p.lineTo(ix + 180, dy + 86); c.drawPath(p, stroke=1, fill=0)
    for k, col in enumerate(["#EDEDF0", "#E8B33C", "#FF7BAC", "#7FB2E8", "#7BD88F"]):
        c.setFillColor(HexColor(col))
        c.circle(ix + 30 + k * 22, dy + 52, 5.4, stroke=0, fill=1)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7.6)
    c.drawString(ix + 150, dy + 49, "pen · highlighter · undo")
    dot(c, ix + iw - 20, iy + ih - 46, 1); dot(c, ix + iw / 2, oy + 70, 2)
    dot(c, ix + iw / 2, dy + 156, 3); dot(c, ix + 30, dy + 52, 4)
    sidebar(c, 316, 96, 486, 400, "Simple words", [
        ("1", "<b>Photo notes</b> — camera (no permission needed — the system camera app does the work) or the photo picker. Several images per note; the image IS the card; tap = full-screen viewer with swipe.", "NEW"),
        ("2", "<b>Grab image text (OCR)</b> — Keep's search superpower, done offline: bundled on-device text recognition copies the photo's words into the body, so search finds text you never typed. <b>[decision D2]</b>", "NEW"),
        ("3", "<b>Drawing notes</b> — finger or stylus; pen and highlighter strokes, five ink colours, widths, undo. Saved as the note's image; lives on the wall like any card.", "NEW"),
        ("4", "<b>Our copies are ours</b> — picked photos are copied into the app's private folder, so a note never breaks when the original moves or deletes (Keep leans on Drive; we lean on nobody).", "NEW"),
        ("5", "<b>Voice lives here too</b> — next page: recording, playback, and the honest word on transcription.", "NEW"),
    ])


def p09_voice(c):
    new_page(c, "Page 7 — Voice notes", "Tap the mic: record, play, scrub. The card shows the audio chip. One honest caveat inside.", 9)
    ix, iy, iw, ih = phone(c, 44, 64)
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 11)
    c.drawString(ix + 10, iy + ih - 46, "Morning thought")
    # audio chip + waveform
    ay = iy + ih - 140
    c.setFillColor(GLASS); c.setStrokeColor(GLASS_LN); c.setLineWidth(0.8)
    c.roundRect(ix + 8, ay, iw - 16, 70, 10, stroke=1, fill=1)
    c.setFillColor(GOLD); c.circle(ix + 30, ay + 35, 12, stroke=0, fill=1)
    c.setFillColor(HexColor("#20160A")); c.setFont("Helvetica-Bold", 10)
    c.drawCentredString(ix + 30, ay + 31, "▶")
    import math
    for k in range(34):
        hgt = 4 + abs(math.sin(k * 0.9)) * 16 + (k % 5) * 1.4
        c.setFillColor(GOLD_DK if k < 12 else HexColor("#4A4D57"))
        c.roundRect(ix + 52 + k * 4.6, ay + 35 - hgt / 2, 2.4, hgt, 1.2, stroke=0, fill=1)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7.4)
    c.drawString(ix + 52, ay + 8, "0:17 / 0:41   ·   scrub by sliding")
    # recording state
    ry = iy + 120
    c.setFillColor(HexColor("#2A1416")); c.setStrokeColor(HexColor("#8A5A5A")); c.setLineWidth(1)
    c.roundRect(ix + 8, ry, iw - 16, 84, 10, stroke=1, fill=1)
    c.setFillColor(HexColor("#FF6B6B")); c.circle(ix + 26, ry + 62, 6, stroke=0, fill=1)
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 10)
    c.drawString(ix + 40, ry + 58, "Recording…  0:07")
    for k in range(26):
        hgt = 3 + abs(math.sin(k * 1.7)) * 13
        c.setFillColor(HexColor("#FF6B6B"))
        c.roundRect(ix + 22 + k * 7, ry + 30 - hgt / 2, 3, hgt, 1.5, stroke=0, fill=1)
    pill(c, ix + 24, ry + 8, 64, 18, "Cancel", fill=HexColor("#00000000"), ink=INK_2, ln=GLASS_LN, fs=8)
    pill(c, ix + iw - 92, ry + 8, 64, 18, "Stop ✓", fill=GOLD, ink=HexColor("#20160A"), ln=None, fs=8, bold=True)
    # card with audio chip
    note_card(c, ix + 10, iy + 40, iw - 20, 56, KEEP_DARK["Purple"], title="Morning thought", bars=1, chips=["🎙 0:41"])
    dot(c, ix + 30, ay + 60, 1); dot(c, ix + iw - 30, ry + 66, 2)
    dot(c, ix + iw / 2, iy + 56, 3); dot(c, ix + iw / 2, ry - 14, 4)
    sidebar(c, 316, 96, 486, 400, "Simple words — and one honest caveat", [
        ("1", "<b>Playback</b> — play/pause, slide to scrub, position shown; recordings live in the app's private folder (the Recorder app's proven pattern).", "NEW"),
        ("2", "<b>Recording</b> — live waveform and timer; Cancel or Stop. Needs the RECORD_AUDIO permission — the one permission Notes would declare. <b>[decision D1]</b>", "NEW"),
        ("3", "<b>Audio chip on the card</b> — 🎙 + length, visible on the wall without opening.", "NEW"),
        ("4", "<b>Transcription — the honest caveat</b> — Keep auto-transcribes voice using Google's cloud. We are offline-first: <b>offline automatic transcription is REQUIRED (owner approved)</b>. Bundle a local model, transcribe after saving audio, show progress and allow cancel/retry. Preserve audio if transcription fails; no cloud fallback or first-use download. Qualify speed, memory and languages on real devices.", "NEW"),
        ("5", "<b>Search still finds voice notes</b> — by title, labels, colour and the “Voice” type filter, transcript or not.", "NEW"),
    ])


def p10_org(c):
    new_page(c, "Page 8 — Labels, colours, and search filters", "Folders that aren't folders, the 12-colour palette, and search that narrows by type/colour/label.", 10)
    ix, iy, iw, ih = phone(c, 44, 64)
    # search page with filter chips
    searchbar(c, ix + 6, iy + ih - 40, iw - 40, "sweets")
    c.setStrokeColor(INK_3); c.setLineWidth(1.2)
    c.line(ix + iw - 24, iy + ih - 22, ix + iw - 14, iy + ih - 32)
    c.line(ix + iw - 14, iy + ih - 22, ix + iw - 24, iy + ih - 32)
    fy = iy + ih - 62
    for k, chipw in enumerate([52, 44, 48, 58]):
        pass
    labels = ["Lists", "Voice", "Images", "Drawings"]
    xx = ix + 8
    for k, lb in enumerate(labels):
        wch = 12 + 5.1 * len(lb)
        pill(c, xx, fy, wch, 16, lb, fill=HexColor("#33290F") if k == 2 else GLASS,
             ink=GOLD if k == 2 else INK_2, ln=GOLD if k == 2 else GLASS_LN, fs=7.2)
        xx += wch + 6
    xx = ix + 8
    for lb, col in [("#family", None), ("Yellow", "#5D5526"), ("Red", "#60302C")]:
        wch = 14 + 5.1 * len(lb)
        pill(c, xx, fy - 22, wch, 16, lb, fill=GLASS, ink=INK_2, ln=GLASS_LN, fs=7.2)
        if col:
            c.setFillColor(HexColor(col)); c.circle(xx + 9, fy - 14, 3.6, stroke=0, fill=1)
        xx += wch + 6
    note_card(c, ix + 8, iy + ih - 190, iw - 16, 60, KEEP_DARK["Yellow"], title="Sweets for puja", bars=2, chips=["#family"])
    note_card(c, ix + 8, iy + ih - 252, iw / 2 - 12, 52, KEEP_DARK["Default"], image=True, bars=0)
    note_card(c, ix + iw / 2 + 4, iy + ih - 252, iw / 2 - 12, 52, KEEP_DARK["Default"], title="Mithai recipes", bars=2)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7.4)
    c.drawString(ix + 10, iy + ih - 268, "search sees inside photos too (OCR)")
    # labels dialog
    ly = iy + 30
    c.setFillColor(HexColor("#000000")); c.setFillAlpha(0.4); c.rect(ix, iy + 16, iw, 120, stroke=0, fill=1); c.setFillAlpha(1)
    c.setFillColor(GLASS); c.setStrokeColor(GOLD); c.setLineWidth(1)
    c.roundRect(ix + 12, ly, iw - 24, 104, 12, stroke=1, fill=1)
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 9)
    c.drawString(ix + 26, ly + 86, "Labels")
    for k, (nm, on) in enumerate([("family", True), ("work", False), ("ideas", True)]):
        yy2 = ly + 64 - k * 20
        c.setStrokeColor(GOLD if on else INK_3); c.setLineWidth(1)
        c.rect(ix + 26, yy2, 8, 8, stroke=1, fill=0)
        if on:
            p = c.beginPath(); p.moveTo(ix + 27.4, yy2 + 4); p.lineTo(ix + 29.6, yy2 + 1.6); p.lineTo(ix + 33, yy2 + 7)
            c.setStrokeColor(GOLD); c.setLineWidth(1.4); c.drawPath(p, stroke=1, fill=0)
        c.setFillColor(INK_2); c.setFont("Helvetica", 8.6)
        c.drawString(ix + 42, yy2 + 1, nm)
    c.setFillColor(GOLD); c.setFont("Helvetica", 8.2)
    c.drawString(ix + 26, ly + 6, "+  Create new label")
    dot(c, ix + iw / 2, iy + ih - 52, 1); dot(c, ix + iw / 2, fy - 30, 2)
    dot(c, ix + iw - 24, ly + 52, 3); dot(c, ix + 40, iy + ih - 266, 4)
    sidebar(c, 316, 96, 486, 400, "Simple words", [
        ("1", "<b>Type filters</b> — Lists · Voice · Images · Drawings: one tap narrows results to that note type (Keep's documented filter set).", "NEW"),
        ("2", "<b>Colour & label filters</b> — narrow by background colour or by #label; filters stack (yellow + #family).", "NEW"),
        ("3", "<b>Labels dialog</b> — checkboxes, many labels per note, “Create new label” inline. Typing <b>#</b> in a note body offers labels as you type and creates new ones on the fly.", "NEW"),
        ("4", "<b>Search sees what you never typed</b> — OCR'd photo words (page 6) are indexed like text: “sweets” finds the picture of the sweet box.", "NEW"),
        ("5", "<b>Instant, never laggy</b> — filtering runs off the UI thread and updates cards in place; a contract fails the build if typing ever rebuilds the wall (your old submit-search order stays honoured in spirit: no lag). <b>[decision D5]</b>", "UPGRADE"),
        ("6", "<b>Label pages</b> — every label is also a page in the drawer: its own wall, its own search.", "NEW"),
    ])


def p11_bulk(c):
    new_page(c, "Page 9 — Multi-select & bulk actions", "Long-press one card: the wall turns into a basket. Archive, delete, label, colour, share, merge — in one move.", 11)
    ix, iy, iw, ih = phone(c, 44, 64)
    # selection top bar
    c.setFillColor(GLASS)
    c.roundRect(ix + 4, iy + ih - 44, iw - 8, 26, 13, stroke=0, fill=1)
    c.setStrokeColor(INK_2); c.setLineWidth(1.3)
    c.line(ix + 16, iy + ih - 26, ix + 24, iy + ih - 34); c.line(ix + 24, iy + ih - 34, ix + 16, iy + ih - 42 - 0)
    c.line(ix + 16, iy + ih - 34, ix + 28, iy + ih - 34)
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 9.4)
    c.drawString(ix + 36, iy + ih - 35, "3 selected")
    c.setFillColor(INK_2); c.setFont("Helvetica", 9)
    c.drawString(ix + iw - 96, iy + ih - 35, "⌗  🏷  🎨  ↗  ⋮")
    # cards with selection checks
    rows = [(KEEP_DARK["Yellow"], "Rent receipt", True), (KEEP_DARK["Teal"], "Trip packing", True),
            (KEEP_DARK["Pink"], "Gift ideas", True), (KEEP_DARK["Default"], "Groceries", False),
            (KEEP_DARK["Green"], "WiFi password", False)]
    ry = iy + ih - 118
    for k, (col, t, sel) in enumerate(rows):
        cx2 = ix + 8 + (k % 2) * (iw / 2)
        cy2 = ry - (k // 2) * 76
        note_card(c, cx2, cy2, iw / 2 - 12, 66, col, title=t, bars=2)
        c.setFillColor(GOLD if sel else HexColor("#00000000"))
        c.setStrokeColor(GOLD if sel else INK_3); c.setLineWidth(1.2)
        c.circle(cx2 + iw / 2 - 24, cy2 + 56, 7.5, stroke=1, fill=1 if sel else 0)
        if sel:
            c.setStrokeColor(HexColor("#20160A")); c.setLineWidth(1.6)
            p = c.beginPath(); p.moveTo(cx2 + iw / 2 - 27.5, cy2 + 56); p.lineTo(cx2 + iw / 2 - 25, cy2 + 53.5)
            p.lineTo(cx2 + iw / 2 - 20.5, cy2 + 59); c.drawPath(p, stroke=1, fill=0)
    # merge diagram
    my = iy + 34
    c.setFillColor(GLASS); c.setStrokeColor(GOLD); c.setLineWidth(1)
    c.roundRect(ix + 8, my, iw - 16, 74, 10, stroke=1, fill=1)
    c.setFillColor(GOLD); c.setFont("Helvetica-Bold", 8.6)
    c.drawString(ix + 20, my + 58, "3-dot ▸ Merge")
    for k, col in enumerate([KEEP_DARK["Yellow"], KEEP_DARK["Teal"], KEEP_DARK["Pink"]]):
        c.setFillColor(HexColor(col)); c.roundRect(ix + 20 + k * 26, my + 30, 20, 20, 4, stroke=0, fill=1)
    c.setFillColor(INK_3); c.setFont("Helvetica-Bold", 10)
    c.drawString(ix + 104, my + 36, "→")
    c.setFillColor(HexColor(KEEP_DARK["Yellow"])); c.roundRect(ix + 124, my + 24, 30, 32, 5, stroke=0, fill=1)
    c.setFillColor(INK_2); c.setFont("Helvetica", 6.8)
    c.drawString(ix + 162, my + 40, "one note, all text kept;")
    c.drawString(ix + 162, my + 30, "sources go to Trash (undo-able)")
    dot(c, ix + iw / 2, iy + ih - 31, 1); dot(c, ix + iw - 60, iy + ih - 31, 2)
    dot(c, ix + iw / 4, ry - 60, 3); dot(c, ix + iw / 2, my + 82, 4)
    sidebar(c, 316, 96, 486, 400, "Simple words", [
        ("1", "<b>Long-press a card</b> — selection mode: cards grow a gold check circle; tap more cards to add; ✖ or Back exits.", "NEW"),
        ("2", "<b>The action bar swaps</b> — “3 selected” replaces the title; icons: copy/send · label · colour · share · 3-dot (archive, delete, merge).", "NEW"),
        ("3", "<b>Bulk everything</b> — archive 20 notes, trash 5, label 3 as #work, recolour the wall — each in one tap, each with ONE Undo snackbar for the batch.", "NEW"),
        ("4", "<b>Merge</b> — combines selected notes into one (titles become first lines) and opens it for editing; the sources go to Trash, restorable for 7 days.", "NEW"),
        ("5", "<b>Bulk share</b> — selected notes concatenated into one share payload; photo notes attach their images.", "NEW"),
    ])


def p12_reminders(c):
    new_page(c, "Page 10 — Reminders, natively ours", "Keep exiled reminders to Google Tasks in 2025 and killed location reminders. Ours stay inside the app — no account, offline, alive after reboot.", 12)
    ix, iy, iw, ih = phone(c, 44, 64)
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 11)
    c.drawString(ix + 10, iy + ih - 44, "Pay rent")
    c.setFillColor(INK_2); c.setFont("Helvetica", 8.6)
    c.drawString(ix + 10, iy + ih - 60, "₹18,500 to landlord")
    # reminder picker sheet (dark pill signature)
    py = iy + 150
    c.setFillColor(HexColor("#000000")); c.setFillAlpha(0.4); c.rect(ix, iy + 16, iw, ih - 60, stroke=0, fill=1); c.setFillAlpha(1)
    c.setFillColor(GLASS); c.setStrokeColor(GOLD); c.setLineWidth(1)
    c.roundRect(ix + 12, py, iw - 24, 150, 14, stroke=1, fill=1)
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 9.4)
    c.drawString(ix + 26, py + 130, "Remind me")
    for k, opt in enumerate(["Tomorrow morning · 8:00", "Later today · 18:00", "Pick date & time…"]):
        yy2 = py + 100 - k * 26
        c.setFillColor(HexColor("#26282F"))
        c.roundRect(ix + 22, yy2 - 6, iw - 44, 21, 10, stroke=0, fill=1)
        c.setFillColor(GOLD if k == 0 else INK_2); c.setFont("Helvetica", 8.6)
        c.drawString(ix + 34, yy2, opt)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7)
    c.drawString(ix + 26, py + 10, "exact-alarm permission explained with consequences (Clock's pattern)")
    # chip on card + notification
    note_card(c, ix + 10, py - 76, iw - 20, 58, KEEP_DARK["Yellow"], title="Pay rent", bars=1, chips=["⏰ Tomorrow, 8:00 AM"])
    ny = iy + 26
    c.setFillColor(GLASS); c.setStrokeColor(GLASS_LN); c.setLineWidth(0.8)
    c.roundRect(ix + 6, ny, iw - 12, 44, 9, stroke=1, fill=1)
    hexmark(c, ix + 22, ny + 30, 5)
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 7.6)
    c.drawString(ix + 34, ny + 30, "MySpace Notes · now")
    c.setFillColor(INK_2); c.setFont("Helvetica", 7.8)
    c.drawString(ix + 34, ny + 18, "Pay rent — ₹18,500 to landlord")
    pill(c, ix + 34, ny + 3, 46, 12, "Open", fill=HexColor("#00000000"), ink=GOLD, ln=GOLD, fs=6.6)
    pill(c, ix + 86, ny + 3, 52, 12, "Snooze", fill=HexColor("#00000000"), ink=INK_2, ln=GLASS_LN, fs=6.6)
    pill(c, ix + 144, ny + 3, 60, 12, "Complete", fill=HexColor("#00000000"), ink=INK_2, ln=GLASS_LN, fs=6.6)
    dot(c, ix + iw / 2, py + 158, 1); dot(c, ix + iw - 30, py - 40, 2)
    dot(c, ix + iw / 2, ny + 52, 3)
    sidebar(c, 316, 96, 486, 400, "Simple words", [
        ("1", "<b>Bell → sheet</b> — Keep's quick picks (Tomorrow morning · Later today · Pick date & time) in our dark-pill design. A saved reminder shows as a chip under the note and on its card.", "NEW"),
        ("2", "<b>Reminder chips</b> — “⏰ Tomorrow, 8:00 AM” on the wall: what is due, without opening anything. The Reminders drawer page lists them all, soonest first.", "NEW"),
        ("3", "<b>The notification works</b> — Open · Snooze (+10 min / +1 h / tomorrow) · Complete. Survives reboot (the Clock's re-arm receiver pattern, reused not reinvented).", "NEW"),
        ("4", "<b>Why this beats Keep now</b> — Google moved Keep's time reminders into the Tasks app (late 2025) and discontinued location reminders entirely. Ours never leave the phone: no account, no migration, no surprise.", "NEW"),
        ("5", "<b>Every ring is logged</b> — set, fired, snoozed, completed: outcomes in the transcript, like everything else we build.", "NEW"),
    ])


def p13_trash(c):
    new_page(c, "Page 11 — Safety: Undo, Trash, and the 7-day window", "The whole promise in one picture: a wrong tap is never a lost note.", 13)
    panel(c, 40, 300, 370, 210, "Undo snackbar — after EVERY quiet action")
    sx, sy = 60, 330
    c.setFillColor(GLASS); c.setStrokeColor(GOLD); c.setLineWidth(1)
    c.roundRect(sx, sy + 96, 330, 40, 10, stroke=1, fill=1)
    c.setFillColor(INK_2); c.setFont("Helvetica", 9)
    c.drawString(sx + 16, sy + 112, "Note archived")
    c.setFillColor(GOLD); c.setFont("Helvetica-Bold", 9.6)
    c.drawString(sx + 250, sy + 112, "UNDO")
    for k, t in enumerate(["“Moved to trash · UNDO”", "“3 notes labelled #work · UNDO”", "“Colour changed · UNDO”", "“Notes merged · UNDO”"]):
        c.setFillColor(INK_3); c.setFont("Helvetica", 8.4)
        c.drawString(sx + 6, sy + 66 - k * 16, t)
    c.setFillColor(INK_2); c.setFont("Helvetica", 8.6)
    c.drawString(sx, sy - 14, "Keep replaced confirm-dialogs with undo. So do we —")
    c.drawString(sx, sy - 26, "the current “Delete this note?” dialog retires. [D8]")
    panel(c, 430, 300, 372, 210, "Trash page — the 7-day window")
    tx, ty = 450, 330
    note_card(c, tx, ty + 88, 100, 54, KEEP_DARK["Red"], title="Old bills", bars=2, chips=["5 days left"])
    note_card(c, tx + 112, ty + 88, 100, 54, KEEP_DARK["Default"], title="Draft", bars=2, chips=["1 day left"])
    pill(c, tx + 226, ty + 116, 100, 20, "Empty trash", fill=HexColor("#00000000"), ink=HexColor("#E88A8A"), ln=HexColor("#8A5A5A"), fs=8)
    pill(c, tx + 226, ty + 90, 100, 20, "Restore all?", fill=HexColor("#00000000"), ink=INK_2, ln=GLASS_LN, fs=8)
    c.setFillColor(INK_3); c.setFont("Helvetica", 8.2)
    c.drawString(tx, ty + 58, "each card: 3-dot ▸ Restore / Delete forever")
    c.drawString(tx, ty + 42, "day 8: auto-purge, checked every time the app opens —")
    c.drawString(tx, ty + 26, "no server needed, the phone keeps its own calendar")
    c.setFillColor(INK_2)
    c.drawString(tx, ty, "Keep Help says 7 days; some 2025 web reports say 30 —")
    c.drawString(tx, ty - 14, "we pin 7 in ONE constant; it can move in a line. [D4]")
    sidebar(c, 60, 100, 740, 160, None, [
        ("★", "<b>The safety stack, bottom to top:</b>  Undo snackbar (instant regret) → Trash 7-day window (slow regret) → Archive forever (not regret, just tidying) → "
              "the JSON mirror + export bundle (a note outlives even the app). Four nets under every note — this is exactly Keep's model, plus our offline export.", ""),
    ])


def p14_widgets(c):
    new_page(c, "Page 12 — Widgets: Notes on the home screen", "Keep's three widgets, rendered by the suite's existing widget engine — resizable, re-placeable, re-configurable.", 14)
    def widget(x, y, w, h, title):
        c.setFillColor(HexColor("#26282F")); c.setStrokeColor(GLASS_LN); c.setLineWidth(1)
        c.roundRect(x, y, w, h, 12, stroke=1, fill=1)
        c.setFillColor(INK_3); c.setFont("Helvetica-Bold", 7.6)
        c.drawString(x, y - 13, title.upper())
    # quick capture
    widget(50, 330, 230, 120, "Quick capture — 4 doors")
    for k, (g, nm) in enumerate([("+", "Note"), ("☑", "List"), ("🎙", "Voice"), ("📷", "Camera")]):
        c.setFillColor(GOLD); c.circle(88 + k * 54, 400, 15, stroke=0, fill=1)
        c.setFillColor(HexColor("#20160A")); c.setFont("Helvetica-Bold", 12)
        c.drawCentredString(88 + k * 54, 395, g)
        c.setFillColor(INK_3); c.setFont("Helvetica", 7)
        c.drawCentredString(88 + k * 54, 372, nm)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7.6)
    c.drawString(62, 342, "each button opens that capture directly — no browsing")
    # single note widget
    widget(310, 330, 230, 120, "Single note — live content")
    c.setFillColor(HexColor(KEEP_DARK["Yellow"]))
    c.roundRect(324, 356, 202, 80, 8, stroke=0, fill=1)
    c.setFillColor(INK); c.setFont("Helvetica-Bold", 8.6)
    c.drawString(336, 420, "Rent receipt")
    c.setFillColor(HexColor("#00000055"))
    for i in range(3):
        c.roundRect(336, 404 - i * 10, 170 - i * 34, 3.6, 1.8, stroke=0, fill=1)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7.6)
    c.drawString(324, 342, "pin one note to the home screen; tap opens it")
    # list widget
    widget(570, 330, 230, 120, "Checklist — tick from home")
    c.setFillColor(HexColor(KEEP_DARK["Teal"]))
    c.roundRect(584, 356, 202, 80, 8, stroke=0, fill=1)
    for k, (t, done) in enumerate([("Milk", True), ("Bread", True), ("Paneer", False)]):
        yy3 = 414 - k * 18
        c.setStrokeColor(GOLD if done else INK_2); c.setLineWidth(1)
        c.rect(596, yy3, 7, 7, stroke=1, fill=0)
        if done:
            p = c.beginPath(); p.moveTo(597.4, yy3 + 3.4); p.lineTo(599.4, yy3 + 1.2); p.lineTo(602.4, yy3 + 6)
            c.setStrokeColor(GOLD); c.setLineWidth(1.3); c.drawPath(p, stroke=1, fill=0)
        c.setFillColor(INK_2); c.setFont("Helvetica", 8)
        c.drawString(610, yy3, t)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7.6)
    c.drawString(584, 342, "checkboxes tick on the home screen — writes through")
    sidebar(c, 60, 100, 740, 190, None, [
        ("1", "<b>Rendered by the suite's widget engine</b> — the same one the Clock's 20 providers use: resize-aware (150×84dp minimum hints), re-placeable, launcher long-press ▸ Configure re-enters Widget Studio for that instance.", "NEW"),
        ("2", "<b>Host-level, integration round</b> — widgets live in the app shell, not inside the notes engine, so developing them cannot touch Notes' internals or any other app (the microservice rule again).", "NEW"),
        ("3", "<b>Live content</b> — single-note and list widgets show REAL current content, and a tick on the home screen updates the note instantly.", "NEW"),
    ])


def p15_design(c):
    new_page(c, "Page 13 — The design language", "Keep's 2025 look as the reference; OUR signature as the skin. Every rule below was paid for in the calculator rounds.", 15)
    panel(c, 40, 320, 372, 190, "Our signature (non-negotiable, owner-set)")
    items = [
        "Dark-pill popups everywhere — a stock AlertDialog was rejected",
        "   on sight in the calculator (“outdated… match the app's design”).",
        "Glass surfaces + GelMotion press feels; every click animates.",
        "Honeycomb mark forever — icon, header, widgets, notifications.",
        "Contrast guard: ink vs fill within 0.35 luminance → auto-swapped",
        "   near-black/near-white. A colour cannot ship unreadable text.",
        "True dark mode: exact-dark, not gray; no white flashes.",
    ]
    c.setFillColor(INK_2); c.setFont("Helvetica", 8.6)
    yy4 = 486
    for it in items:
        c.drawString(56, yy4, it); yy4 -= 15
    panel(c, 430, 320, 372, 190, "Keep's Material 3 Expressive (the reference)")
    items2 = [
        "Bolder typography, expressive shapes and buttons (Aug 2025).",
        "Springy motion: cards expand from their position into the editor,",
        "   staggered entrance on the wall, FAB morph, snackbar slides.",
        "12 flat note colours, rounded 8dp cards, generous whitespace.",
        "Chips as the universal language: reminders, labels, filters.",
    ]
    c.setFillColor(INK_2); c.setFont("Helvetica", 8.6)
    yy4 = 486
    for it in items2:
        c.drawString(446, yy4, it); yy4 -= 15
    # colour swatch wall: dark + light side by side
    panel(c, 40, 100, 762, 190, "The 12 colours, both themes — every pairing contrast-guarded")
    names = list(KEEP_DARK.keys())
    for k, nm in enumerate(names):
        x5 = 64 + k * 61
        c.setFillColor(HexColor(KEEP_DARK[nm])); c.setStrokeColor(BOARD_LN); c.setLineWidth(0.7)
        c.roundRect(x5, 196, 52, 46, 7, stroke=1, fill=1)
        c.setFillColor(INK); c.setFont("Helvetica-Bold", 7)
        c.drawString(x5 + 7, 224, "Aa")
        c.setFillColor(HexColor(KEEP_LIGHT[nm])); c.setStrokeColor(BOARD_LN)
        c.roundRect(x5, 140, 52, 46, 7, stroke=1, fill=1)
        c.setFillColor(HexColor("#1B1C20")); c.setFont("Helvetica-Bold", 7)
        c.drawString(x5 + 7, 168, "Aa")
        c.setFillColor(INK_3); c.setFont("Helvetica", 6.4)
        c.drawCentredString(x5 + 26, 126, nm)
    c.setFillColor(INK_3); c.setFont("Helvetica", 7.6)
    c.drawString(64, 108, "top row: dark theme · bottom row: light theme · the calculator's LED lesson applied from day one: readable in BOTH, always")


def p16_plan(c):
    new_page(c, "Page 14 — The plan, the promises, and your 8 decisions", "How we build without touching anything that works — and what needs your word before Phase 0.", 18)
    # phases timeline
    panel(c, 40, 396, 762, 110, "Five phases")
    phases = [
        ("P0", "Skeleton", "standalone MyNotes host · isolation contracts · logging · stamp"),
        ("P1", "Keep's core loop", "wall · drawer pages · trash · editor chrome · colours · bulk · undo"),
        ("P2", "Lists & text power", "formatting · checklists full set · customize-list · merge · share"),
        ("P3", "Media", "photos · OCR · drawing · voice + offline transcription"),
        ("P4", "Reminders & search", "native reminders · instant search + filters · labels · sort · drag-order last"),
        ("P5", "Integration", "into MySpace: registry swap · share target · widgets · full regression of ALL apps"),
    ]
    for k, (tag, t, s) in enumerate(phases):
        x6 = 56 + k * 124
        c.setFillColor(GOLD if k == 0 else HexColor("#26282F"))
        c.circle(x6 + 8, 478, 9, stroke=0, fill=1)
        c.setFillColor(HexColor("#20160A") if k == 0 else INK_2)
        c.setFont("Helvetica-Bold", 7.6); c.drawCentredString(x6 + 8, 475.5, tag)
        c.setFillColor(INK); c.setFont("Helvetica-Bold", 8.4); c.drawString(x6, 456, t)
        c.setFillColor(INK_3); c.setFont("Helvetica", 6.9)
        words, line, y7 = s.split(" · "), "", 444
        for wd in words:
            if len(line) + len(wd) > 24:
                c.drawString(x6, y7, line); y7 -= 9; line = wd
            else:
                line = (line + " · " + wd) if line else wd
        if line: c.drawString(x6, y7, line)
        if k < 5:
            c.setStrokeColor(BOARD_LN); c.setLineWidth(1)
            c.line(x6 + 22, 478, x6 + 112, 478)
    # promises
    panel(c, 40, 250, 372, 130, "The microservice promises")
    prom = [
        "Notes imports only :core-* — pinned by gate contracts both ways.",
        "MySpace's APK does not change until the ONE integration round.",
        "The calculator stays FROZEN; its 59 stories keep running as proof.",
        "Development + your testing happen on a standalone MyNotes APK",
        "   (the MyCalc pattern: perfect it alone, then bring it home).",
    ]
    c.setFillColor(INK_2); c.setFont("Helvetica", 8.2)
    yy5 = 356
    for pr in prom:
        c.drawString(56, yy5, pr); yy5 -= 13.5
    # test promises
    panel(c, 430, 250, 372, 130, "The testing promises (calculator lessons pre-paid)")
    tst = [
        "Every feature gets an end-user story, not a widget test.",
        "“Visible” means pixels counted on a bitmap — never “intended”.",
        "Logs record OUTCOMES from day one; the transcript rides out of CI.",
        "Gestures dispatch through views (the farm playbook, pre-applied).",
        "Two red runs max before instrumenting — no rerun loops. Ever.",
    ]
    c.setFillColor(INK_2); c.setFont("Helvetica", 8.2)
    yy5 = 356
    for tt in tst:
        c.drawString(446, yy5, tt); yy5 -= 13.5
    # decisions
    panel(c, 40, 96, 762, 140, "Owner approvals recorded — see NOTES-DEVELOPMENT-BASELINE.md for binding scope")
    dec = [
        ("D1", "RECORD_AUDIO permission for voice notes", "declare it (Recorder precedent)"),
        ("D2", "On-device OCR (bundled ML Kit, offline, a few MB)", "yes — size is not a constraint"),
        ("D3", "Offline auto-transcription of voice", "APPROVED: bundled, offline only"),
        ("D4", "Trash window: Keep Help says 7 days, some reports 30", "pin 7, one constant"),
        ("D5", "Instant search vs your old submit-search order", "instant, off-thread, no-lag contract"),
        ("D6", "Drag-and-drop note reordering on the wall", "ship, but LAST (riskiest gesture)"),
        ("D7", "Standalone MyNotes APK during development", "yes — the MyCalc pattern"),
        ("D8", "Delete-confirm dialog → trash + Undo snackbar", "adopt Keep's model"),
    ]
    for k, (tag, q, a) in enumerate(dec):
        x8 = 56 + (k % 2) * 380
        y8 = 196 - (k // 2) * 25
        c.setFillColor(GOLD); c.setFont("Helvetica-Bold", 8.4); c.drawString(x8, y8, tag)
        c.setFillColor(INK_2); c.setFont("Helvetica", 8)
        c.drawString(x8 + 24, y8, q)
        c.setFillColor(GREEN); c.setFont("Helvetica-Bold", 7.6)
        c.drawString(x8 + 24, y8 - 9.5, "→ propose: " + a)


def diagram_shape(c, x, y, w, h, kind, label="", accent=False):
    c.setStrokeColor(GOLD if accent else BLUE)
    c.setFillColor(HexColor("#332D20") if accent else GLASS)
    c.setLineWidth(1.2)
    if kind == "diamond":
        p = c.beginPath(); p.moveTo(x+w/2,y+h); p.lineTo(x+w,y+h/2)
        p.lineTo(x+w/2,y); p.lineTo(x,y+h/2); p.close(); c.drawPath(p,stroke=1,fill=1)
    elif kind == "circle":
        c.ellipse(x,y,x+w,y+h,stroke=1,fill=1)
    elif kind == "para":
        p=c.beginPath(); p.moveTo(x+10,y); p.lineTo(x+w,y); p.lineTo(x+w-10,y+h); p.lineTo(x,y+h); p.close(); c.drawPath(p,stroke=1,fill=1)
    elif kind == "database":
        c.rect(x,y+5,w,h-10,stroke=1,fill=1)
        c.ellipse(x,y+h-10,x+w,y+h,stroke=1,fill=1)
        c.arc(x,y,x+w,y+10,180,180)
    elif kind == "text":
        c.setDash(3,2); c.rect(x,y,w,h,stroke=1,fill=0); c.setDash()
    else:
        c.roundRect(x,y,w,h,h/2 if kind == "pill" else 4,stroke=1,fill=1)
    c.setFillColor(INK); c.setFont("Helvetica",8)
    c.drawCentredString(x+w/2,y+h/2-3,label)


def diagram_link(c, points, label=None):
    from math import atan2, sin, cos
    c.setStrokeColor(INK_2); c.setLineWidth(1.2)
    for a,b in zip(points,points[1:]): c.line(*a,*b)
    a,b=points[-2:]; angle=atan2(b[1]-a[1],b[0]-a[0])
    p=c.beginPath(); p.moveTo(*b)
    for d in [-0.45,0.45]: p.lineTo(b[0]-6*cos(angle+d),b[1]-6*sin(angle+d))
    p.close(); c.setFillColor(INK_2); c.drawPath(p,stroke=0,fill=1)
    if label:
        c.setFont("Helvetica",7); c.drawString(points[0][0]+5,points[0][1]-12,label)


def p17_diagram(c):
    new_page(c,"New section — Diagram & flowchart editor","Your addition: turn ideas into shapes, blocks, arrows and text boxes, inside a note.",16)
    ix,iy,iw,ih=phone(c,44,64,h=450)
    c.setFillColor(INK); c.setFont("Helvetica-Bold",11); c.drawString(ix+9,iy+ih-38,"<  Login flow")
    c.setFont("Helvetica",8); c.drawRightString(ix+iw-8,iy+ih-38,"Undo  Redo")
    for xx in range(int(ix+8),int(ix+iw-6),14):
        for yy in range(int(iy+85),int(iy+ih-55),14):
            c.setFillColor(BOARD_LN); c.circle(xx,yy,0.6,stroke=0,fill=1)
    diagram_shape(c,ix+67,iy+336,78,26,"pill","Start")
    diagram_link(c,[(ix+106,iy+336),(ix+106,iy+308)])
    diagram_shape(c,ix+62,iy+260,88,48,"diamond","Valid?",True)
    diagram_link(c,[(ix+106,iy+260),(ix+106,iy+222)],"Yes")
    diagram_shape(c,ix+68,iy+194,76,28,"rect","Open home")
    diagram_link(c,[(ix+150,iy+284),(ix+183,iy+284),(ix+183,iy+157),(ix+147,iy+157)],"No")
    diagram_shape(c,ix+66,iy+143,81,28,"rect","Show error")
    diagram_shape(c,ix+15,iy+99,131,22,"text","Add an explanation...")
    c.setStrokeColor(GOLD); c.setDash(3,2); c.rect(ix+59,iy+257,94,54,stroke=1,fill=0); c.setDash()
    for dx,dy in [(59,257),(153,257),(59,311),(153,311)]:
        c.setFillColor(GOLD); c.rect(ix+dx-2,iy+dy-2,4,4,stroke=0,fill=1)
    pill(c,ix+16,iy+364,179,20,"Fill   Stroke   Aa   Duplicate   Delete",fs=7)
    pill(c,ix+8,iy+30,iw-16,34,"Select   Shapes   Arrow   Text   Pen",fs=8)
    pill(c,ix+150,iy+76,50,16,"Fit  100%",fs=7)
    for x,y,n in [(ix+14,iy+66,1),(ix+160,iy+312,2),(ix+188,iy+229,3),(ix+149,iy+111,4),(ix+30,iy+388,5)]: dot(c,x,y,n)
    sidebar(c,316,90,486,408,"A diagram tool, not just a doodle",[
        ("1","<b>One toolbox:</b> Select, Shapes, Arrow, Text and Pen. Tap Shapes to open the library on the next board. Start from the Home capture menu: New diagram.","NEW"),
        ("2","<b>Editable objects:</b> drag blocks, resize with handles, rotate where useful, and double-tap to edit the words inside. Select several objects to move or group them together.","NEW"),
        ("3","<b>Connected arrows:</b> straight, elbow or curved paths. Snap each end to a block; moving the block keeps its arrows attached. Add Yes/No labels and choose arrowheads at either end.","NEW"),
        ("4","<b>Text boxes anywhere:</b> add a title, explanation or caption. Change text size, alignment and style. Shape text and captions are included in note search.","NEW"),
        ("5","<b>Style inspector:</b> fill colour, border colour, line thickness, solid/dashed borders and text settings. Duplicate, copy/paste, delete, group and layer controls sit here too.","NEW"),
        ("6","<b>Comfortable canvas:</b> pinch to zoom, two-finger pan, dot-grid snap, smart alignment guides, Fit to content, Undo and Redo.","NEW"),
        ("7","<b>Still a note:</b> autosave the editable diagram, show its thumbnail on Home, and support labels, pin, archive, reminders, trash and Undo. Export a PNG to share; the original stays editable.","NEW"),
    ],note="Proposed design only. The sample uses a selected decision diamond (gold handles) and a floating style bar. Other Notes features and the frozen calculator remain isolated.")


def p18_library(c):
    new_page(c,"New section — Shape library & diagram workflow","Common diagram building blocks, plus the controls that make them useful.",17)
    panel(c,40,286,372,222,"Shape picker — starter blocks")
    entries=[("rect","Process"),("pill","Start / End"),("diamond","Decision"),("circle","Circle"),("para","Input / Output"),("database","Database"),("text","Text box"),("rect","Container")]
    for k,(kind,title) in enumerate(entries):
        x=62+(k%4)*87; y=443-(k//4)*86
        diagram_shape(c,x,y,65,34,kind,"Aa" if kind=="text" else "",kind=="diamond")
        c.setFillColor(INK_2); c.setFont("Helvetica",7); c.drawCentredString(x+32,y-13,title)
    sidebar(c,440,290,355,212,"Expanded library & connectors",[
        ("A","<b>More shapes:</b> triangle, hexagon, document, cloud, speech bubble, star, rounded rectangle and swimlane. Containers and swimlanes help organise larger diagrams.","NEW"),
        ("B","<b>Lines:</b> plain line, straight arrow, elbow arrow and curved connector; solid or dashed; one-way or two-way heads; editable connector labels.","NEW"),
        ("C","<b>Object controls:</b> align left/centre/right, distribute spacing, group/ungroup, bring forward/send backward, lock/unlock and duplicate.","NEW"),
        ("D","<b>Templates:</b> blank canvas, simple flowchart, mind map and swimlane. Start with a small structure and replace the sample words.","NEW"),
    ])
    sidebar(c,56,76,730,179,"Simple workflow & proposed delivery",[
        ("1","<b>Create:</b> Home > New diagram > Blank or Template. Add blocks, double-tap to type, connect them with arrows, then adjust colours and spacing.","NEW"),
        ("2","<b>Save safely:</b> editable objects and connections live in Notes-owned storage, not a flattened screenshot. Returning to Home shows a refreshed thumbnail. PNG export creates a separate shareable image.","NEW"),
        ("3","<b>Phase 3 starter:</b> basic blocks, text boxes, straight/elbow connectors, move/resize, style controls, autosave, undo/redo and PNG sharing. <b>Phase 4 expansion:</b> full library, curves, grouping, layers, smart guides and templates. This is a proposal, not a build already shipped.","NEW"),
        ("4","<b>Acceptance checks:</b> save/reopen preserves every object; moving a block preserves connections; undo/redo restores edits; long text remains readable; zoom works in both themes; export includes all content; calculator and other apps remain unchanged.","NEW"),
    ])


def main():
    c = rl_canvas.Canvas(OUT, pagesize=landscape(A4))
    c.setTitle("MySpace Notes — UI & Feature Overview (Google Keep reference)")
    c.setAuthor("arena/3caca636-tictic")
    for fn in (p01_cover, p02_map, p03_home, p04_drawer, p05_editor, p06_checklist,
               p07_capture, p08_media, p09_voice, p10_org, p11_bulk, p12_reminders,
               p13_trash, p14_widgets, p15_design, p17_diagram, p18_library, p16_plan):
        fn(c)
        c.showPage()
    c.save()
    if _overflow:
        print("OVERFLOW WARNINGS:")
        for o in _overflow:
            print("  -", o)
        raise SystemExit(1)
    print(f"wrote {OUT}: {PAGES} pages")


if __name__ == "__main__":
    main()

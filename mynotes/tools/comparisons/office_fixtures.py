"""Generate small public Office fixtures with real format libraries, not XML-only ZIPs.

These establish package validity, not a complex-document fidelity test corpus.
"""
from pathlib import Path


def write_office(directory):
    from docx import Document
    from pptx import Presentation
    from pptx.util import Inches
    from odf.opendocument import OpenDocumentText
    from odf.text import P
    directory=Path(directory);directory.mkdir(parents=True,exist_ok=True)
    document=Document();document.add_paragraph('Public cobalt42 Word original')
    document.save(directory/'Comparison.docx')
    slides=Presentation();slide=slides.slides.add_slide(slides.slide_layouts[6])
    box=slide.shapes.add_textbox(Inches(1), Inches(1), Inches(8), Inches(2))
    box.text_frame.text='Public cobalt42 slide original';slides.save(directory/'Comparison.pptx')
    odt=OpenDocumentText();odt.text.addElement(P(text='Public cobalt42 ODT original'))
    odt.save(str(directory/'Comparison.odt'))

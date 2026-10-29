#!/usr/bin/env python3
"""Slide-style user map + proposed refinement wireframes. No private screenshots/logs embedded."""
from pathlib import Path
import json, textwrap
import pymupdf as fitz
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'MYNOTES-USER-MAP-AND-REFINEMENT.pdf'
BG=(.055,.065,.08); PANEL=(.10,.12,.15); INK=(.94,.95,.97); MUTED=(.68,.73,.79); GOLD=(.94,.74,.40); GREEN=(.42,.83,.70)
slides=[
('Start here','A notebook, not a collection of disconnected tools','map',[
('What this document is','A complete screen map, plain-language feature glossary, practical walkthrough and proposed interaction specification. Landscape slides are designed for presentation or comfortable zooming.'),
('How to read the status','CURRENT = available in published 0.12, not a promise that every device behaves identically. PROPOSED = requested redesign, not yet shipped. VALIDATE = a behaviour that must be checked in the replacement build.'),
('Figma handoff','The wireframes are real vector layouts, not screenshots of a finished build. An SVG companion can be imported into Figma. This is not a hosted Figma file or a claimed Figma prototype.')]),
('01 / The app map','Where everything belongs','map',[
('Library = where you browse','Notes, Archive and Trash are library destinations. Folders and labels organise notes. Search, filters, view and sort change how you find or see them.'),
('A note = the document you work in','Text or a checklist is the main content. Images, drawings and diagrams are visual content. A reminder is a scheduled property of a note, not a kind of drawing or a separate notebook.'),
('Capture and app services','Read image extracts text from a picture. Record voice captures audio and may produce a transcript. Voice inbox holds originals. Settings, Storage & recovery, Backups and About are app-level services.')]),
('02 / Vocabulary','One name for each thing','glossary',[
('Drawing / image / diagram','Drawing: ink and simple geometric marks made on a canvas. Image: a photo or imported picture. Diagram: separately editable shapes and connectors, such as a flowchart.'),
('Reminder / archive / trash','Reminder: a date and time attached to a note. Archive: keep a note out of the active list without deleting it. Trash: deleted notes awaiting recovery or permanent deletion.'),
('Library / folder / label','Library: the collection and its destinations. Folder: a note location, reflected under the chosen main storage path when configured. Label: a reusable tag; a note can have several.')]),
('03 / First use','Create, write, return, find again','library',[
('1. Create your first note','Open Notes. Choose New text note or Checklist. Add a useful title and content. Use Back to return to the library; normal edits are saved automatically.'),
('2. Check the save outcome','A save or storage warning matters. Automatic saving is not a reason to ignore a failed destination update. Private recovery and the selected folder can have different states.'),
('3. Find it again','Search a distinctive word. If the note is missing, clear labels/filters, check the selected folder and look in Archive or Trash before assuming deletion.')]),
('04 / Library','Your browsing home','library',[
('Primary actions','Search, open a note, create a text note or checklist. A pinned note is visually marked with a filled pin. Opening Library should reveal clear destinations, not a second confusing toolbar.'),
('Selection is deliberate','Choose notes using visible selection controls; arbitrary combinations are allowed. Selection mode should show a count and the available bulk actions. Cancel selection without changing notes.'),
('Gesture and navigation','The Library swipe begins inside the left portion of the screen, rather than the system Back edge. The visible Library control remains available; a gesture must never be the only route.')]),
('05 / Filter & view','A lens on the library, not another storage location','filters',[
('View','Choose list or grid. This changes presentation, not note contents or folder paths. Keep this separate from selection mode and content editing.'),
('Sort','Order by created or edited time. A sort option is not a label and does not move a note into another folder. The active choice should remain visibly selected.'),
('Labels and reset','Select any useful combination of labels. Show active filters and a clear reset path. A filtered empty result must say that it is filtered, not imply the entire library is empty.')]),
('06 / Folders and labels','Location versus meaning','library',[
('Folders: a practical walkthrough','Choose the main local folder in Storage & recovery. Create a named folder, then move selected notes into it. Verify the folder and readable note filenames in the Android file manager.'),
('Labels: a practical walkthrough','Open Labels on a note, choose or create labels, then return to the library and filter by a label. Several labels can describe one note without making extra note copies.'),
('What to communicate','A folder move is a file operation, not just a renamed chip. Report a failed move and retained source honestly. Filenames use a useful note prefix and timestamp rather than opaque random names.')]),
('07 / Note editor','The document, its content and its properties','editor',[
('Top bar','Back, pin, archive and contextual undo belong to the note. Note-level Copy and Trash belong to library/list actions; native copying of selected text remains available in the editor.'),
('Main document','Title and text/checklist first. Visual items follow in clearly separated cards. A reminder chip is a property near the note header, not a miscellaneous image attachment.'),
('Bottom actions','Add opens content/capture choices. Text formatting applies to text. Colour/background and labels organise appearance and classification. More holds less-frequent actions, not essential editing.')]),
('08 / Text and checklists','Write naturally; keep formatting predictable','editor',[
('Text editing','Use native Android caret movement and precise text selection. Select words or ranges, then apply formatting. The selected-text Copy action is different from copying a whole note.'),
('Formatting','Bold, italic, underline, strike and headings should act on an understandable range. Larger headings must reflow following paragraphs rather than overlap them.'),
('Checklists','Create items, check them, change indentation and reorder using the available item controls. Hide checked items only when wanted. Conversion to or from text is an explicit operation.')]),
('09 / Add to note','Separate content creation from capture','add',[
('Visual content','Image adds an existing picture. Drawing opens an ink canvas. Diagram opens structured shapes and connectors. These should be separately named and separately recognisable.'),
('Capture actions','Read image extracts words from a picture; it is not the same as attaching that picture. Record voice captures an original recording and attempts offline transcription.'),
('Scheduled property','Reminder opens date and time selection for this note. It belongs in Add as requested, then appears as a distinct, tappable reminder chip in the note.')]),
('10 / Visual content cards','Stop treating every visual item as the same image','attachments',[
('PROPOSED: consistent anatomy','Every card has a type badge, preview and compact action strip. Use Drawing, Image or Diagram as the primary type. Show a filename or caption only when useful, not as a second competing heading.'),
('PROPOSED: compact controls','Use an edit icon with a specific accessible name, and a secondary More menu for removal and related actions. Do not place tall, full-width Edit drawing / Remove image text rows under every preview.'),
('Removal semantics','Remove drawing, Remove image and Remove diagram must describe the actual content. Removal is not deletion of the entire note. Destructive actions need a clear target and a recovery/confirmation policy.')]),
('11 / Drawing: current issue','Why the supplied screen feels unfinished','drawing',[
('CURRENT: what your screenshots show','The landscape white paper sits inside a tall dark page, leaving large empty regions. The selected tool looks like plain text. Save is a text button and delete competes with frequent controls.'),
('Your requested direction','Let the workspace use the available screen between compact top and bottom controls. Give tools real selection states, icons and names. Remove the persistent gesture paragraph.'),
('Acceptance, not decoration','The redesign must work for portrait, landscape and larger text. Canvas coordinates and exports must not stretch. Existing saved drawings and drafts must remain readable.')]),
('12 / Drawing: target layout','Canvas first; tools always recognisable','drawing',[
('PROPOSED: top bar','Back and Drawing on the left. Save icon immediately before the top-right three-dot menu. Save attaches or updates the drawing in the original note; Back retains the draft without pretending it was attached.'),
('PROPOSED: first bottom row','A horizontal tool strip: Pen, Pencil, Highlighter, Eraser, Line, Rectangle, Oval. Each has an icon and short label. The selected tool has a filled accent surface and selected accessibility state.'),
('PROPOSED: second bottom row','Ink colour, stroke width, Undo, Redo and Fit occupy a compact dock. Large-font or narrow screens can scroll the tool strip; targets remain at least 48 dp. No persistent instruction paragraph.')]),
('13 / Drawing: secondary actions','Three dots hold options, not the main workflow','drawing-menu',[
('PROPOSED: Grid lines','An explicit checked toggle shows or hides a workspace grid. Grid lines are a guide, not drawing content: they must not appear in saved images or copied images unless a future explicit export option says so.'),
('PROPOSED: Copy drawing as image','Copy the rendered drawing image, not a screenshot of the app. Android clipboard receives a readable PNG URI with a proper content type and grant. Say Copied only after the image is available.'),
('PROPOSED: Delete / clear','Delete or clear must identify its scope: clear this canvas versus remove this attached drawing from the note. Keep destructive options in More and confirm them; preserve draft/Undo behaviour where applicable.')]),
('14 / Drawing walkthrough','From the first stroke to a reusable picture','drawing',[
('1. Draw','Open Add > Drawing. Choose a visibly selected tool; choose ink and width in the lower dock. Draw in the canvas. Pinch and two-finger navigation can remain supported without occupying a permanent text paragraph.'),
('2. Refine','Switch tools, Undo or Redo. Use Fit to recover the view. Use Grid lines from More when alignment helps. The guide must not change the exported artwork.'),
('3. Finish','Use the top Save icon to attach/update the drawing. Copy as image is independent of Save and must not silently edit the note. Return to the note and see a Drawing card with a compact edit action.')]),
('15 / Images and Read image','Two different jobs, clearly named','attachments',[
('Attach an Image','Add > Image opens the Android picker. Select a supported local picture. The note gets an Image card. This operation preserves the note if decoding, size checks or the picker fails.'),
('Read image, formerly Scan image','Add > Read image starts offline OCR. Choose the available language, select an image, process it and review the recognised text. OCR can make mistakes; it does not certify the image text as correct.'),
('Keep the distinction visible','An imported photo is not a Drawing. OCR output is editable text, not a Diagram. Do not label all three with the same generic image removal wording or claim OCR attaches the original automatically.')]),
('16 / Diagram','Structured visual thinking, not freehand ink','diagram',[
('What a diagram is','A flowchart or structured sketch made from separate shapes and connectors. Each shape has a type, label, position and size. Unlike a flattened drawing, its elements remain individually editable.'),
('CURRENT operations','Choose among the supported shape types, select a shape, edit its label/type, move it, connect it and use Undo/Redo. Grouping and arrow editing have distinct controls.'),
('PROPOSED direct sizing','Tap a shape to show resize handles at the appropriate corners or fixed points. Drag a handle for a live size preview, then release to commit one undoable change. Remove Wider/Narrower/Taller/Shorter from the normal menu.')]),
('17 / Diagram interaction contract','Select, move, resize, connect','diagram',[
('Selection is visible','Show a clear outline and reachable handles. A first tap selects; a drag inside moves. A drag beginning on a handle resizes. Connector endpoints must not be mistaken for resize handles.'),
('Shape-aware resizing','Rectangles and ellipses use bounds handles; diamonds use the meaningful bounds/vertices. Keep minimum sizes and readable labels. Do not invert a shape or silently drag it off the document.'),
('Coordinate correctness','Handle hit targets are screen-sized even when zoomed. Convert pointer positions through the viewport transform. Resizing grouped shapes, moving connectors, rotated displays and Undo need explicit tests before acceptance.')]),
('18 / Reminder','A schedule attached to a note','reminder',[
('Set it','Open a note, choose Add > Reminder, then choose date and time. A distinct reminder chip shows the scheduled time. Tap the chip to edit; a clear action removes the schedule without deleting content.'),
('Android delivery','Notification permission and device power/background policies affect delivery. Permission denial must leave normal note editing usable. Check notification settings if a reminder did not alert.'),
('Scope and language','Use Reminder, not remainder. Reminder is a note property and an app notification workflow, not an attachment. Rich recurrence/calendar functionality is not claimed as fully delivered.')]),
('19 / Voice and Voice inbox','Editable words plus a retained original','voice',[
('Capture','Record voice is optional and contextual; microphone denial must not block text notes. Stop recording to retain the audio before offline recognition is attempted.'),
('Use the recording','Voice inbox is a library destination for originals. Cards show playback progress and play/pause controls; seeking should be understandable. Export WAV and retry transcript are secondary actions.'),
('Understand the result','Review recognition and conservative punctuation. Failure of an offline model must not erase the original audio. Technical engine/model information belongs in About, not the primary capture controls.')]),
('20 / Pin, Archive and Trash','Three distinct lifecycle actions','library',[
('Pin','Mark a note as important without moving or duplicating it. The selected pin is filled, and its accessible name should change to Unpin.'),
('Archive','Remove a note from the active Notes view while retaining it in Archive. Unarchive returns it to active notes; this is not recovery from deletion.'),
('Trash','Deleted notes can be restored or permanently deleted. Automatic expiry is seven days; manual permanent deletion and emptying Trash are also available. Explain permanence and never purge live note files by mistake.')]),
('21 / Appearance','Consistent signature, not a stock-looking island','appearance',[
('CURRENT choice','System follows the device appearance. Light and Dark are explicit app choices. Embedded MySpace hosts own their appearance policy under the host contract.'),
('PROPOSED choice surface','Replace the visually disconnected default radio dialog shown in your screenshot with a signature sheet: three clearly selected cards, matching surfaces, rounded geometry and readable contrast.'),
('A rule for all screens','Use the same sheet/menu language for tools, colours, widths and confirmations. Platform file pickers and permission prompts remain Android-owned; do not disguise them as app-designed screens.')]),
('22 / Colour and backgrounds','Appearance of a note, not its location','editor',[
('Note colour','Choose a note colour for recognition. Colour must remain readable in both light and dark themes. It is not a label, folder or reminder priority.'),
('Background','Background options are a separate visual property, including the available coloured tints. Preserve content and precise text selection when changing backgrounds.'),
('Selection clarity','Show the active swatch with more than colour alone: an outline or check, plus a readable accessible name. Do not rely on a user remembering which similarly coloured dot they tapped.')]),
('23 / Settings','Four questions, four clear destinations','settings',[
('Make it yours','Appearance and Home widget. Widget placement is initiated by Android and may differ by launcher. Users can remove and re-add widgets without deleting notes.'),
('Your notes and files','Storage & recovery and Backups. Show current destination and any pending save clearly. Keep advanced recovery choices below the normal choose-folder/retry actions.'),
('Help and app information','Diagnostics is opt-in troubleshooting. About contains version, offline model information and licences. It should not compete visually with everyday writing controls.')]),
('24 / Storage & recovery','Know where your notes are safe','settings',[
('Normal path','Use device storage or select a main local folder. Folder selection goes through the Android picker and needs useful read/write permission. Local file names and physical subfolders should reflect actual operations.'),
('A failed destination','A private recovery copy and a successful external save are not the same claim. If the folder is unavailable or revoked, show the pending state and Retry save rather than silently claiming completion.'),
('Before destructive changes','Do not clear app data or uninstall to solve a storage warning before exporting/verifying a backup. Opening an existing library requires validation and an understandable merge preview.')]),
('25 / Backups and restore','A portable copy is not automatic cloud sync','settings',[
('CURRENT published backup scope','The published JSON backup preserves supported note data, including embedded images. It does not contain original voice recordings. Export originals separately from Voice inbox when needed.'),
('CURRENT safe restore flow','Pick a local backup, validate it, review added/identical/conflicting notes and confirm. Changed-ID conflicts are preserved as copies; a previous-state checkpoint is written.'),
('UNPUBLISHED archive work is paused','Audio-inclusive ZIP staging, validation and conflict-planning foundations exist in development. They are not an end-user feature yet. This request takes priority; no archive UI or complete transactional restore is claimed.')]),
('26 / Export, sharing and clipboard','Different outputs for different purposes','attachments',[
('Share or copy text','A selected-text copy uses Android text selection. Whole-note copy is a library/list action. Sharing text does not create a complete backup of drawings, originals or app state.'),
('PDF export','PDF is for reading/presentation, including supported diagram output. A readable PDF is not a restorable editable note backup. Success requires valid output, not just a created zero-byte filename.'),
('PROPOSED drawing clipboard','Copy as image supplies a PNG through Android clipboard. Pasting depends on the receiving app supporting image clips; a text-only destination cannot be promised to accept it.')]),
('27 / Search and organisation walkthrough','Find a note without losing your place','filters',[
('Example: a project note','Create Project planning, assign a project folder, add a Work label and pin it while active. Put a diagram in the note when its content needs a flowchart.'),
('Search and narrow','Search a word, then choose the relevant folder or labels and your preferred view/sort. Make active constraints visible so you know why other notes are absent.'),
('Finish the project','Unpin and archive the note when complete. Use Trash only when deletion is intended. Recovery belongs in Trash or Backups, not in View or Sort.')]),
('28 / Mixed-content walkthrough','One document, several clearly divided components','attachments',[
('Example: a site visit','Write observations as text or checklist items. Attach a photo as Image. Use Read image if you need words extracted. Add a Drawing for annotations and a Diagram for the process flow.'),
('PROPOSED visual hierarchy','Keep title and properties at the top, main writing next and typed content cards after it. Use consistent spacing and a small type badge; show actions only beside their own content.'),
('Example: follow-up','Add a Reminder to the note. If voice is useful, record it and retain the original in Voice inbox. None of these actions should create an unexplained stack of generic image controls.')]),
('29 / Accessibility and layout','Compact does not mean tiny or cryptic','drawing',[
('Icons need meaning','Frequently repeated actions can be icons, but every icon needs a specific accessible label and long-press help. Drawing tools keep both icon and text as requested; essential state must be visibly selected.'),
('Large fonts and small screens','Keep 48 dp hit targets, scrollable tool rows and reachable actions. Test portrait, landscape, tablet and 200% text. Do not use smaller text simply to fit a crowded toolbar.'),
('Avoid unnecessary decoration','No permanent explanatory paragraph crowding the canvas. Put optional gesture help in More. Do not sacrifice native text selection, contrast or Back behaviour to match a mockup.')]),
('30 / Refinement acceptance','What must be checked before calling this finished','diagram',[
('Drawing and content cards','Full available workspace; distinct selected tool chips; two compact rows; top Save and More; working grid toggle; real PNG clipboard; scoped deletion; correct Drawing/Image labels and no stale draft loss.'),
('Diagram and signature surfaces','Direct handles replace size command buttons. Test every supported shape and connector behaviour with zoom and Undo. Review signature sheets in light/dark, large font and narrow layouts.'),
('User device decides','New native tests, rendered screen review and real-device retest are separate evidence. An authored test is not a passing test; a passing emulator is not acceptance on the Motorola Android 16 device.')]),
('31 / Progress card','This document is the first deliverable, not a completion claim','map',[
('DONE for this handoff','App vocabulary and categories documented. Detailed screen and feature walkthrough supplied. Requested drawing, attachment, appearance and diagram changes specified as proposals. Vector PDF/SVG handoff supplied.'),
('NEXT in the refinement streak','Implement canvas/dock and tool selection; add More actions and image clipboard; compact typed attachment cards; rename Read image; implement diagram resize handles; unify priority sheets.'),
('PENDING acceptance','Runtime changes for this new request, build/lint, focused native interaction tests, rendered review, updated APK and physical retest. The older archive qualification does not qualify this redesign.')]),
('32 / Quick reference','If you want to...','glossary',[
('Find, organise or recover','Find: Search. Change list/grid/order: Filter & view. Move location: Folders. Tag: Labels. Keep out of active list: Archive. Recover deletion: Trash. Recover a library: Backups/Storage & recovery.'),
('Create or capture content','Write: Text/Checklist. Add a photo: Image. Ink: Drawing. Editable flowchart: Diagram. Extract words from a photo: Read image. Capture speech: Record voice; originals live in Voice inbox.'),
('Change behaviour or appearance','Schedule: Reminder in Add, then its note chip. Note appearance: Colour/Background. App appearance: Settings > Appearance. File destination: Storage & recovery. Troubleshoot: Diagnostics. Version/models: About.')])
]
def text(p,rect,s,size=14,color=INK):
    rc=p.insert_textbox(fitz.Rect(rect),s,fontname='helv',fontsize=size,color=color,lineheight=1.35)
    if rc<0: raise RuntimeError(f'Text overflow ({rc}): {s[:90]}')
def box(p,r,color=PANEL,border=None,radius=0): p.draw_rect(fitz.Rect(r),color=border,fill=color,width=.8)
def pill(p,x,y,label,active=False,w=100):
    box(p,(x,y,x+w,y+29),GOLD if active else PANEL)
    text(p,(x+7,y+6,x+w-4,y+27),label,10,BG if active else INK)
def wire(p,kind):
    x,y,w,h=45,145,310,355
    box(p,(x,y,x+w,y+h),(.09,.10,.12),(.24,.27,.31));text(p,(x+15,y+12,x+w-15,y+40),'MyNotes  /  '+kind.replace('-',' ').title(),15)
    if kind.startswith('drawing'):
        text(p,(x+15,y+46,x+w-15,y+66),'Back     Drawing                  Save   More',11,GOLD)
        box(p,(x+10,y+74,x+w-10,y+257),(.98,.98,.96))
        if kind=='drawing-menu':
            box(p,(x+125,y+83,x+w-17,y+218),PANEL);text(p,(x+137,y+95,x+w-22,y+210),'Grid lines      [on]\nCopy as image\nClear canvas\nGesture help',11)
        else:
            p.draw_polyline([(x+38,y+212),(x+100,y+110),(x+174,y+198),(x+260,y+125)],color=(.12,.35,.6),width=2)
        for i,label in enumerate(['Pen','Pencil','Ink','Erase']):pill(p,x+10+i*73,y+267,label,i==0,68)
        text(p,(x+15,y+310,x+w-15,y+343),'Colour   Width    Undo   Redo   Fit',12,MUTED)
    elif kind=='diagram':
        box(p,(x+12,y+58,x+w-12,y+320),(.96,.97,.96));p.draw_rect((x+85,y+97,x+220,y+161),color=(.16,.42,.6),width=2)
        for xx in [x+85,x+220]:
            for yy in [y+97,y+161]:box(p,(xx-5,yy-5,xx+5,yy+5),GOLD)
        text(p,(x+103,y+120,x+215,y+150),'Selected shape',11,(.1,.16,.2));p.draw_line((x+151,y+162),(x+151,y+213),color=(.16,.42,.6),width=2);p.draw_oval((x+91,y+214,x+213,y+267),color=(.16,.42,.6),width=2)
        text(p,(x+20,y+327,x+w-12,y+350),'Add shape    Connect    Undo',11,MUTED)
    elif kind in ['attachments','editor','add','reminder']:
        text(p,(x+17,y+52,x+w-17,y+78),'Project planning',15);text(p,(x+17,y+84,x+w-17,y+115),'Your writing stays the main content.',11,MUTED)
        for i,label in enumerate(['IMAGE  /  Site photo','DRAWING  /  Sketch','DIAGRAM  /  Workflow']):
            yy=y+128+i*58;box(p,(x+12,yy,x+w-12,yy+50),PANEL);text(p,(x+22,yy+11,x+w-20,yy+45),label+'      Edit  More',10)
        text(p,(x+17,y+320,x+w-17,y+349),'Add   Format   Colour   Labels   More',10,GOLD)
    elif kind in ['settings','appearance']:
        for i,label in enumerate(['Appearance','Home widget','Storage & recovery','Backups','Diagnostics','About & offline models']):
            yy=y+53+i*45;box(p,(x+12,yy,x+w-12,yy+37),PANEL);text(p,(x+23,yy+10,x+w-20,yy+35),label+'    >',11)
        if kind=='appearance':
            box(p,(x+28,y+111,x+w-28,y+266),(.17,.20,.24));text(p,(x+42,y+125,x+w-35,y+160),'Appearance',16,GOLD)
            for i,label in enumerate(['System','Light','Dark  / selected']):text(p,(x+43,y+164+i*27,x+w-30,y+193+i*27),label,12)
    elif kind=='filters':
        for i,label in enumerate(['View','Sort','Labels']):pill(p,x+12+i*97,y+52,label,i==0,91)
        text(p,(x+22,y+104,x+w-22,y+180),'List   [selected]\nGrid\n\nActive filters: Work',13)
        for i in range(3):box(p,(x+12,y+200+i*38,x+w-12,y+231+i*38),PANEL)
    elif kind=='voice':
        for i in range(2):
            yy=y+62+i*112;box(p,(x+12,yy,x+w-12,yy+99),PANEL);text(p,(x+24,yy+12,x+w-18,yy+90),'Recording '+str(i+1)+'\nPlay / Pause   ----- progress\nOpen note                 More',12)
    else:
        for i,label in enumerate(['Notes / active documents','Archive / kept, not deleted','Trash / recover or delete','Folders / locations','Voice inbox / originals']):
            yy=y+57+i*52;box(p,(x+12,yy,x+w-12,yy+43),PANEL);text(p,(x+22,yy+12,x+w-20,yy+40),label,11,GOLD if i==0 else INK)
    text(p,(45,510,355,540),'SCHEMATIC / proposed visual hierarchy\nNot a screenshot or shipped-build proof.',9,MUTED)
def main():
    doc=fitz.open()
    for idx,(eyebrow,title,kind,blocks) in enumerate(slides,1):
        p=doc.new_page(width=1000,height=625);box(p,(0,0,1000,625),BG)
        text(p,(42,24,955,45),'MYNOTES  /  USER GUIDE + REFINEMENT SPECIFICATION  /  29 OCT 2026',10,GOLD)
        text(p,(42,55,955,80),eyebrow.upper(),12,MUTED);text(p,(42,86,957,130),title,25)
        wire(p,kind)
        for j,(head,body) in enumerate(blocks):
            yy=145+j*130;box(p,(382,yy,956,yy+118),PANEL);text(p,(398,yy+12,940,yy+37),head,15,GOLD);text(p,(398,yy+40,938,yy+113),body,12)
        text(p,(42,581,955,612),f'CURRENT = published 0.12 scope   |   PROPOSED = requested, not shipped   |   Page {idx:02d} / {len(slides):02d}',10,MUTED)
    doc.set_metadata({'title':'MyNotes — Detailed user map, walkthrough and refinement specification','author':'MyNotes project','subject':'Current app guide and explicitly proposed UX refinements; no private diagnostics embedded'})
    doc.save(OUT,garbage=4,deflate=True)
    svgdir=ROOT/'mynotes/docs/design-2026-10-29';svgdir.mkdir(parents=True,exist_ok=True)
    for page in [1,11,12,13,16,17,21]:
        (svgdir/f'frame-{page+1:02d}.svg').write_text(doc[page].get_svg_image(text_as_path=False))
    (svgdir/'walkthrough-source.json').write_text(json.dumps(slides,indent=2,ensure_ascii=False)+'\n')
    doc.close();print(OUT)
if __name__=='__main__':main()

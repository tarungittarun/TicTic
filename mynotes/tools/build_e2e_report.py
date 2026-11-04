"""Source-pinned executed E2E report. Only consumes synthetic CI evidence, never uploads.
PDF-only dependencies: reportlab, Pillow. Standard-library ingestion has archive bounds.
Usage: python -m mynotes.tools.build_e2e_report --run ID --source SHA --artifacts DIR --output DIR
Download only refinement-native-* artifacts into DIR; no APKs or private attachments.
A screenshot is not a passing test; owner outcomes and gaps remain separate.
"""
from pathlib import Path
import argparse
import hashlib
import io
import json
import re
import shutil
import subprocess
import tarfile
from xml.sax.saxutils import escape
from mynotes.tools.ci.render_review import fixtures
from mynotes.tools.ci.test_plan import mask_comments_and_strings

ROOT = Path(__file__).resolve().parents[2]
PROFILES = ['current-api36', 'current-api36-b', 'min-api26', 'min-api26-b',
            'landscape-api34', 'landscape-api34-b', 'large-font-api34', 'large-font-api34-b']
GROUPS = [
 ('Home / Notes / Doc / Library', 'Workspace tabs, native search, selection, pins, filters and user arrangement retain content and return correctly.', r'CoreJourneysTest#.*(Home|Workspace|[Ss]earch|[Ff]ilter|Library|bottom|[Ss]elect|[Pp]in)'),
 ('Notes writing / formatting', 'Edit text; preserve font, size, colours, paragraph styles, selection, lists and downstream numbering through save/reopen.', r'CoreJourneysTest#.*([Nn]otes|[Nn]umber|[Ff]ormat|[Tt]yping|Heading|Paragraph)'),
 ('Doc writing / mixed content', 'Insert at the caret; retain independently editable rich text, checklists, media, table and diagram blocks; Undo/Redo retain structure.', r'CoreJourneysTest#.*([Dd]oc|canonical|[Cc]ontent|[Bb]lock)'),
 ('Checklists / tables / hyperlinks', 'Scoped cell/row editing, insertion, selection conversion, movement and link actions do not flatten neighbouring content.', r'(CoreJourneysTest|StructuredCodecTest)#.*([Cc]hecklist|[Tt]able|[Ll]ink|[Rr]ow|[Cc]ell)'),
 ('Drawing / image editing / diagrams', 'Drawing tools and shape transforms preserve stored source detail; shape/group operations remain scoped.', r'(DrawingToolsTest|Diagram.*Test|ImageSelectionTest)#|CoreJourneysTest#.*([Dd]rawing|[Dd]iagram|[Ii]mage|[Ss]hape|[Gg]roup|[Vv]isual)'),
 ('Read image / OCR', 'Local bounded recognition, image-coordinate selection, review and cancellation retain source images. Synthetic correctness is separate from real-image accuracy.', r'(OfflineOcrTest|ImageSelectionTest|PdfPageOcrTest)#|CoreJourneysTest#.*([Oo]cr|[Rr]eadImage)'),
 ('Voice recording / inbox / playback', 'Optional microphone, interruption, original retention, seek/pause and transcript recovery operate offline.', r'(Voice.*Test|OfflineSpeechTest|TrainedWhisperTest|WhisperConnectionJourneysTest)#|CoreJourneysTest#.*([Vv]oice|[Pp]layback|[Rr]ecording)'),
 ('TXT / PDF / Word / spreadsheets / Viewed files', 'Read-only originals never silently become Home notes; Find, counts, pages and explicit copy preserve original bytes.', r'(ViewedFileJourneysTest|LocalFilePickerTest|PdfPageOcrTest)#|RealFolderStorageTest#.*([Tt]xt|[Pp]df|[Ww]ord|[Cc]sv|[Vv]iewed|[Ss]preadsheet)|CoreJourneysTest#.*[Vv]iewed'),
 ('Folders / storage / optional copies', 'Authoritative private content survives absent/broken providers; folder changes and explicitly reviewed recovery preserve conflicts.', r'(RealFolderStorageTest|GenerationProtocolTest|RepositoryTransferTest|RepositoryConcurrencyJourneysTest|StorageLayoutTest|NotesPersistenceTest)#'),
 ('Backups / portable documents / history', 'Validation precedes replacement, restore/checkpoints preserve data, and unsupported inputs fail without destructive migration.', r'(BackupCodecTest|HistoryRecoveryTest|StructuredCodecTest|ArchiveSemanticTest)#'),
 ('Archive / Trash / bulk operations', 'Archive differs from Trash; restore, Undo and confirmed permanent operations respect selection and retention.', r'(ShareAndLabelsJourneysTest|RepositoryTransferTest)#|CoreJourneysTest#.*([Tt]rash|[Aa]rchive|[Bb]ulk|[Mm]erge|[Cc]opy|[Dd]elete|[Ll]abel)'),
 ('Settings / diagnostics / models / styles', 'Local preferences, opt-in bounded diagnostics, saved styles and explicit file export preserve privacy.', r'(DiagnosticsJourneysTest|StylePresetStoreTest)#|CoreJourneysTest#.*([Ss]ettings|[Dd]iagnostic|[Ss]tyle|[Aa]ppearance|[Ss]peech)'),
 ('Reminders / widget / Android integration', 'PendingIntent identity, permission behaviour and OS surfaces preserve note intent; launcher/vendor behaviour still needs physical checks.', r'(ReminderJourneysTest|SystemSurfacesTest|LocalFilePickerTest)#'),
 ('Environment / motion / lifecycle / soak', 'Actual configured themes/fonts/orientation/motion and recovery boundaries are verified rather than inferred from profile names.', r'(EnvironmentQualificationTest|NotesMotionTest|NativeSoakTest)#|CoreJourneysTest#.*([Kk]eyboard|[Ll]ifecycle|[Rr]otation|[Nn]avigation|[Bb]ack)'),
 ('Export PDF / semantic codecs', 'Exported geometry, annotations and structured content are independently validated; successful export does not prove original Word fidelity.', r'(PdfExportTest|DiagramCodecTest|StructuredCodecTest)#'),
]
GAPS = [
 ('Physical drag and selection', 'New native long-hold regression also injects a late board refresh while held. It does not certify exact placement, top-edge control or perceived animation smoothness on your phone.'),
 ('OCR accuracy — OPEN', 'The supplied image misses text and invents other-script characters. This candidate compares automatic/sparse segmentation for better coverage, but that is not training or proof that your image is fixed. Universal fonts, languages, emoji and arbitrary screenshots remain unqualified.'),
 ('Voice accuracy / audibility — OPEN', 'You have not physically tested the recent voice changes. Synthetic recognition/playback outcomes do not establish real microphone quality, Indian-accent generalisation, noise removal or physical loudness.'),
 ('Viewer limitations', 'TXT Find is current-page only; Viewed files uses a bounded, possibly partial index. Word pages remain approximate. Rotated/scanned PDF selection differs from embedded, unrotated text. Spreadsheet rendering is cached values, not Excel fidelity.'),
 ('Physical user library / migration', 'No private attachment, library or screenshot is uploaded to CI. Synthetic storage fixtures cannot establish acceptance of your actual pending-folder difference. Install-over and process-death proofs must be separately run for the exact candidate; earlier candidate55 proofs do not qualify56.'),
 ('UI inventory boundaries', 'This report includes every screenshot actually captured and all selected native method outcomes, not a claim that every possible dialog, error, provider, language, permission combination and vendor page was visited. Unpictured source surfaces are explicitly listed.'),
 ('System pages / external applications', 'Some controller journeys use a test NotesHost to observe a request instead of opening another app. Real SAF provider tests are separately identified. No successful mock host callback is presented as actual Android Save/Share/launcher completion.'),
]

def sha(data):
    return hashlib.sha256(data).hexdigest()


def source_files(directory,source=None):
    if source is None:
        return [(p,p.read_text()) for p in sorted((ROOT/directory).rglob('*.java'))]
    if not re.fullmatch(r'[a-f0-9]{40}',source):raise ValueError('Exact source SHA required')
    names=subprocess.check_output(['git','ls-tree','-r','--name-only',source,'--',directory],cwd=ROOT,text=True).splitlines()
    return [(ROOT/name,subprocess.check_output(['git','show',source+':'+name],cwd=ROOT,text=True)) for name in names if name.endswith('.java')]


def version_info(source):
    if not re.fullmatch(r'[a-f0-9]{40}',source):raise ValueError('Exact source SHA required')
    gradle=subprocess.check_output(['git','show',source+':mynotes/app/build.gradle'],cwd=ROOT,text=True)
    return int(re.search(r'versionCode\s+(\d+)',gradle)[1]),re.search(r"versionName\s+'([^']+)'",gradle)[1]


def methods(source=None):
    result = {}
    for path,text in source_files('mynotes/app/src/androidTest',source):
        if not path.name.endswith('Test.java'):continue
        masked = mask_comments_and_strings(text)
        package = re.search(r'package\s+([\w.]+);', text)[1]
        for match in re.finditer(r'@Test\b[^{};]*?\bvoid\s+(\w+)\s*\([^)]*\)[^{]*\{', masked):
            start = match.end()-1; depth = 1; end = start+1
            while depth and end < len(masked):
                depth += (masked[end]=='{')-(masked[end]=='}'); end += 1
            body = text[start:end]; name = package+'.'+path.stem+'#'+match[1]
            actions = re.findall(r'\b(?:tap|click|dialogTap|previewOption)\("([^"\n]*)"', body)
            scenes = re.findall(r'(?:screenshot|storeScreenshot)\("([a-z0-9-]+)"\s*[,)]', body)+re.findall(r'"([a-z0-9-]+)\.png"', body)
            for call in re.findall(r'(?:screenshot|storeScreenshot)\(([^;]+?)\);',body):
                scenes+=re.findall(r'"([a-z0-9-]+)"',call)
            result[name] = dict(name=name, file=str(path.relative_to(ROOT)), line=text[:start].count('\n')+1,
                                actions=actions, scenes=scenes,
                                assertions=len(re.findall(r'\bassert\w+\s*\(', body)), body=body)
    return result


def source_surfaces(source=None):
    result = []
    for path,text in source_files('mynotes/runtime/src/main/java',source):
        for match in re.finditer(r'\b(?:header|scrollContent|panel|setTitle|choiceSheet|confirmSheet|menu)\("([^"\n]{1,100})"', text):
            result.append(dict(title=match[1], file=path.name, line=text[:match.start()].count('\n')+1))
    return sorted({(x['title'],x['file'],x['line']):x for x in result}.values(),key=lambda x:(x['file'],x['line']))


def ingest(artifacts, output, run, source):
    """Never extract an arbitrary archive path or ingest logs from outside a native artifact."""
    from PIL import Image
    Image.MAX_IMAGE_PIXELS = 16_000_000
    output.mkdir(parents=True,exist_ok=True)
    result = {'schema':1,'run':run,'source_sha':source,'scope':'synthetic CI only; no private user uploads','profiles':{},'screens':[]}
    for lane in PROFILES:
        folder=artifacts/('refinement-native-'+lane)
        if not folder.is_dir():
            result['profiles'][lane]={'ok':False,'unavailable':True};continue
        target=output/lane;target.mkdir(exist_ok=True)
        native=folder/'refinement-native.json'; expected=folder/'refinement-expected.json'
        if native.exists():
            result['profiles'][lane]=json.loads(native.read_text());shutil.copyfile(native,target/'native-results.json')
        else:result['profiles'][lane]={'ok':False,'unavailable':True}
        if expected.exists():shutil.copyfile(expected,target/'expected.json')
        for name in ['refinement-environment-before.json','refinement-environment-configured.json','refinement-environment-after.json','refinement-quality-ocr-common-text-report.json','refinement-pdf-results.json']:
            path=folder/name
            if path.is_file() and path.stat().st_size<=2*1024*1024:shutil.copyfile(path,target/name)
        archive_path=folder/'refinement-captures.tar'
        if not archive_path.is_file():continue
        owners={}
        with tarfile.open(archive_path) as archive:
            for member in archive:
                if member.name=='ux-review/capture-owners.jsonl' and member.isfile() and member.size<=256*1024:
                    for line in archive.extractfile(member).read().decode().splitlines():
                        record=json.loads(line)
                        if record.get('synthetic') is True and re.fullmatch(r'[a-z0-9-]{1,60}',record.get('scene','')):owners[record['scene']]=record['owner']
        with tarfile.open(archive_path) as archive:
            for scene,data in fixtures(archive):
                with Image.open(io.BytesIO(data)) as image:
                    if image.format!='PNG' or image.width*image.height>Image.MAX_IMAGE_PIXELS:raise ValueError('Invalid synthetic image')
                    image=image.convert('RGB');image.thumbnail((720,1800),Image.Resampling.LANCZOS)
                    for quality in (85,75,65,50,35):
                        buffer=io.BytesIO();image.save(buffer,format='JPEG',quality=quality,optimize=True)
                        if len(buffer.getvalue())<=85000:break
                    preview=buffer.getvalue()
                    if len(preview)>85000:raise ValueError('Oversized preview')
                    path=target/(scene+'.jpg');path.write_bytes(preview)
                    result['screens'].append(dict(scene=scene,lane=lane,path=str(path.relative_to(output)),
                        owner=owners.get(scene),width=image.width,height=image.height,
                        sha256=sha(preview),original_png_sha256=sha(data)))
    (output/'provenance.json').write_text(json.dumps(result,indent=2)+'\n')
    return result


def from_preserved(root,output,run,source):
    """Use the workflow's committed JPEG previews when artifact-host access is unavailable.
    Validate run/source and every preview hash. Never substitute an older candidate's images.
    """
    import os
    metadata=json.loads((root/'provenance.json').read_text())
    if metadata['source_sha']!=source or str(metadata.get('capture_run',metadata.get('run')))!=run:
        raise ValueError('Preserved source/run mismatch; old images cannot qualify a new candidate')
    evidence={'schema':1,'run':run,'source_sha':source,'scope':'source-pinned synthetic CI JPEG previews; no private user inputs','profiles':{},'screens':[]}
    output.mkdir(parents=True,exist_ok=True)
    for lane in PROFILES:
        folder=root/lane;report=folder/'native-results.json'
        evidence['profiles'][lane]=json.loads(report.read_text()) if report.is_file() else {'ok':False,'unavailable':True}
        manifest=folder/'manifest.json'
        if not manifest.exists():continue
        for record in json.loads(manifest.read_text())['previews']:
            scene=record['name']
            if not re.fullmatch(r'[a-z0-9-]{1,60}',scene):raise ValueError('Invalid preserved scene')
            if not 0<record['width']<=720 or not 0<record['height']<=1800:raise ValueError('Unbounded system capture dimensions')
            image=folder/(scene+'.jpg')
            if not 0<image.stat().st_size<=85000:raise ValueError('Unbounded system capture bytes')
            if sha(image.read_bytes())!=record['sha256']:raise ValueError('Preserved image hash mismatch')
            evidence['screens'].append(dict(scene=scene,lane=lane,path=os.path.relpath(image,output),owner=None,
                width=record['width'],height=record['height'],sha256=record['sha256'],original_png_sha256=record['original_png_sha256']))
    return evidence


def case_status(name, evidence):
    passed=[];failed=[];missing=[]
    for lane,report in evidence['profiles'].items():
        if name in report.get('cases',[]):passed.append(lane)
        actual_failure=any(str(x).startswith(name+':') for x in report.get('failures',[]))
        if actual_failure:failed.append(lane)
        elif name in report.get('missing',[]):missing.append(lane)
    status='FAIL' if failed else 'PASS' if passed else 'UNEXECUTED'
    return dict(status=status,passed=passed,failed=failed,missing=missing)


def qualification_gate(evidence,names):
    executed=set()
    for lane in PROFILES:
        report=evidence['profiles'].get(lane,{})
        cases=report.get('cases',[])
        if not report.get('ok') or report.get('passed')!=report.get('expected') or report.get('passed')!=len(cases) or len(cases)!=len(set(cases)):
            return False
        if any(report.get(key) for key in ['missing','unexpected','duplicates','failures']):return False
        executed.update(cases)
    return executed==set(names)


def describe(scene, guide):
    if scene in guide:return guide[scene]
    if scene.startswith('reader-diagnostics-'):
        phase=scene.removeprefix('reader-diagnostics-').replace('-',' ')
        return 'Signed release > Diagnostics > '+phase,'Actual app/Android picker state in the optional diagnostic-file saving workflow. It navigates Home, Library, Settings, Diagnostics, Android document roots and Downloads, then saves MyNotes-diagnostics.txt and verifies its bounded bytes by readback.','Fresh synthetic app logs only; the saved log contents are not copied into the report. A completed whole workflow, not the screenshot alone, proves saving.'
    if scene=='reader-docx-view-choice':return 'Open local Word file > choose a view','The actual Word chooser precedes selecting MyNotes pages in the signed release. Editable copy and original-reader routes remain distinct.','MyNotes paper rendering is approximate; no Office-fidelity claim.'
    if scene.startswith('reader-'):
        parts=scene.split('-');kind=parts[1].upper();phase=' '.join(parts[2:])
        if phase=='local picker':purpose='Navigate the real local Android DocumentsUI provider and select the fixed public '+kind+' fixture. The actual picker, not a root-issued URI grant, owns the persisted read permission.'
        elif phase=='library':purpose='Open the real Library menu immediately before choosing Open file for the public '+kind+' fixture.'
        elif phase=='opened':purpose='Show the actual signed-release '+kind+' viewer after opening. The following steps configure a query and PDF/Word page2 or the CSV searched row before a real background-process kill.'
        elif phase=='before kill':purpose='Capture the configured query/page/row immediately before backgrounding and killing the stopped app process. Original fixture bytes are hashed before and after.'
        else:purpose='After the old PID disappears, relaunch the retained task. Verify a changed PID, restored query, PDF/Word page2 or exact CSV pre/post visible anchor with searched row41 visible, and unchanged original bytes.'
        return 'Signed release > '+kind+' > '+phase,purpose,'This source-matching API34 system probe is separate from native-method counts and from physical-device acceptance.'
    if scene.startswith('e2e-library-'):
        destination=scene.removeprefix('e2e-library-').replace('-',' ')
        return 'Home > Library > '+destination,'Capture the actual Library sheet immediately before opening '+destination+'. The following captured page records that destination, and Back returns to Home.','The menu capture proves a visible navigation state, not data mutation or a successful export.'
    viewer68={
      'word68-attached-filename-toolbar':('Word > attached approximate page viewer','Actual attached Word preview window after rendering a public generated DOCX. Checks the compact filename toolbar and real page raster.','Approximate Word pagination, not Office fidelity. This replaces neither original-byte assertions nor physical acceptance.'),
      'pdf68-compact-filename-toolbar':('PDF > initial attached viewer','One Back/filename/Search/options row with the continuous page viewport below.','PixelCopy of the attached public-fixture window; not proof of every file size or vendor configuration.'),
      'pdf68-file-zoom-distant-page':('PDF > whole-file zoom > distant page','Injected two-finger input changes measured document width, then page3 is checked at the enlarged height.','Emulator input and actual geometry assertions, not physical-phone acceptance.'),
      'pdf68-draggable-page-position':('PDF > right page rail','Current/total page state after direct native touch dispatch to the rail; auto-hide is asserted separately.','Direct View dispatch is not a system-injected physical drag.'),
      'pdf68-search-above-keyboard':('PDF > Search > keyboard visible','Attached bottom search controls with IME visibility confirmed; system screenshot includes the separate keyboard window.','Public generated PDF only. File text is populated by the test, not manually typed by a person.'),
      'pdf68-search-result-highlight':('PDF > Enter search > first highlighted match','Window-dispatched Enter triggers search and the test requires real glyph highlight geometry on the matching page.','Only embedded text is searched; scanned-page OCR is separate.'),
      'pdf68-offline-recognition-loader':('PDF > Recognize page offline','A real visible indeterminate ProgressBar is asserted before cancelling queued recognition.','Worker is deliberately held: this proves loader/cancel presentation, not OCR accuracy or measured latency.'),
      'pdf68-pen-and-highlighter-tools':('PDF > Edit / mark > pen and highlight','Attached marking toolbar and actual window-injected pen/highlighter strokes; normalized draft content is asserted.','Marked-copy rendering and SAF readback are separate native assertions. No original PDF is edited.'),
      'word-synthetic-paper-page':('Word renderer > prepared public page','A rendered page bitmap checks header/logo, stored breaks and original-byte preservation.','This is NOT an attached viewer screenshot; it cannot qualify toolbar, keyboard, scrolling or navigation design.')}
    if scene in viewer68:return viewer68[scene]
    descriptions={
      'e2e-viewed-files-count':('Library > Viewed files','Two synthetic index entries are shown with a count, typed file badges and individual open actions. No Home note is imported.','Index-only fixture: the two displayed URI records are not proof of opening two actual originals.'),
      'e2e-viewed-files-search':('Viewed files > Search Alpha','Enter a term from indexed body text. Only First.pdf should remain and the count must be one.','A matching indexed word is not proof that every word in a file was indexed.'),
      'e2e-viewed-files-no-match':('Viewed files > Search notpresent','Enter a nonexistent term. Expect zero matching files and an explicit empty state.','History search does not scan arbitrary unviewed files.'),
      'e2e-large-txt-page-one':('Android local document grant > Larger.txt > page 1','Create a multilingual file over512 KiB and100,000 characters through the real local DocumentsProvider; open read-only and verify exact first-page text.','Text-page Find remains current-page only. The source file is separately read back byte-for-byte.'),
      'e2e-large-txt-page-two':('TXT preview > Next page','Move to page2; assert exact lossless TextPages content, reject an oversized editable copy through overflow and verify no Home note was created.','The screenshot shows page2; preservation is established by separate assertions, not pixels.'),
      'e2e-notes-insert-numbered-point':('Notes > numbered list > Enter after First','Insert a newline at the first item. Expect a new2. point, following items3./4., retained formatting, correct caret and saved text.','Only literal numbered paragraphs are renumbered; ordinary paragraphs end the run.'),
      'e2e-doc-insert-numbered-point':('Doc > numbered writing > Enter after First','Insert before the existing9./10. items. Expect9. blank,10./11. downstream numbers, caret at the new item and exact saved rich-document text.','This is an actual editor mutation, not a screenshot-only numbering demonstration.'),
    }
    if scene in descriptions:return descriptions[scene]
    plain=scene.removeprefix('e2e-').replace('-',' ')
    route=('Home > Library > '+plain.removeprefix('page ')) if scene.startswith('e2e-page-') else ('Home > Library' if scene.startswith('e2e-library-') else 'Whole-app workflow > '+plain)
    return route,'Actual captured '+plain+' state. Consult the owning workflow and assertions below for the exact route and outcome.','A captured page may precede a later failure in its owner; it is not independently declared passed.'


def supplemental_evidence(evidence):
    base=ROOT/'mynotes/docs/design-2026-10-29';updates=[];recoveries=[]
    for identity in (base/'updates').glob('*/*/release-apks.json'):
        apks=json.loads(identity.read_text());candidate=apks.get('candidate',{})
        if candidate.get('source_sha')!=evidence['source_sha']:continue
        report=identity.parent/'release-update-smoke.json'
        if report.exists():updates.append({'identity':apks,'report':json.loads(report.read_text()),'path':str(report.relative_to(ROOT))})
    for identity in (base/'process-recovery').glob('*/reader-apk-identity.json'):
        apk=json.loads(identity.read_text())
        if apk.get('source_sha')!=evidence['source_sha']:continue
        report=identity.parent/'reader-process-recovery.json'
        if report.exists():recoveries.append({'identity':apk,'report':json.loads(report.read_text()),'path':str(report.relative_to(ROOT))})
    return {'updates':sorted(updates,key=lambda x:x['report']['from']['version_code']),'reader_recovery':recoveries}


SYSTEM_SCENES={f'reader-{kind}-{phase}' for kind in ['pdf','docx','csv'] for phase in ['library','local-picker','opened','before-kill','restored']}|{'reader-docx-view-choice'}|{'reader-diagnostics-'+phase for phase in ['home','library','settings','save-page','create-picker','save-roots','save-picker','file-saved']}

def supplemental_gate(supplements,screens):
    upgrades={item['report']['from']['version_code'] for item in supplements['updates'] if item['report'].get('ok') is True and item['report'].get('notes_and_doc_retained') is True and item['report'].get('uninstall_or_clear_data') is False}
    if not {33,35,40,52,55}.issubset(upgrades):return False
    for item in supplements['reader_recovery']:
        report=item['report'];records=report.get('records',[]);diagnostic=report.get('diagnostic_file_save',{})
        if report.get('ok') is not True or diagnostic.get('ok') is not True or len(records)!=3:continue
        if not all(row.get('original_unchanged') is True and row.get('query_restored') is True and row.get('before_pid')!=row.get('after_pid') for row in records):continue
        run=Path(item['path']).parent.name
        captured={row['scene'] for row in screens if row.get('proof_kind')=='signed-release-system' and str(row.get('capture_run'))==run}
        if SYSTEM_SCENES.issubset(captured):return True
    return False


def append_system_captures(evidence,output,supplements):
    import os
    evidence['screens']=[x for x in evidence['screens'] if x.get('proof_kind')!='signed-release-system']
    for item in supplements['reader_recovery']:
        folder=ROOT/item['path'];folder=folder.parent
        manifest=folder/'screenshot-previews.json'
        if not manifest.exists():continue
        if item['report'].get('ok') is not True:continue
        captures=json.loads(manifest.read_text())['captures']
        if not 1<=len(captures)<=40:raise ValueError('Unbounded system capture manifest')
        for record in captures:
            scene=record['name']
            if not re.fullmatch(r'reader-[a-z0-9-]{1,53}',scene):raise ValueError('Invalid signed-release scene')
            if str(record.get('capture_run'))!=folder.name:raise ValueError('System capture run mismatch')
            if not 0<record['width']<=720 or not 0<record['height']<=1800:raise ValueError('Unbounded system capture dimensions')
            image=folder/(scene+'.jpg')
            if not 0<image.stat().st_size<=85000:raise ValueError('Unbounded system capture bytes')
            if sha(image.read_bytes())!=record['sha256']:raise ValueError('Signed-release screenshot hash mismatch')
            evidence['screens'].append({**record,'scene':scene,'lane':'reader-api34','path':os.path.relpath(image,output),
                'owner':'mynotes.tools.ci.reader_process_recovery_smoke#'+('export_public_diagnostics' if scene.startswith('reader-diagnostics-') else 'verify'),'probe_outcome':'PASS','proof_kind':'signed-release-system'})


def build_pdf(evidence, output):
    from reportlab.pdfgen.canvas import Canvas
    from reportlab.pdfbase import pdfmetrics
    from reportlab.pdfbase.ttfonts import TTFont
    from reportlab.lib.colors import HexColor,white
    from reportlab.lib.styles import ParagraphStyle
    from reportlab.platypus import Paragraph
    from reportlab.lib.utils import ImageReader
    for name,file in [('Audit','DejaVuSans.ttf'),('AuditBold','DejaVuSans-Bold.ttf')]:
        pdfmetrics.registerFont(TTFont(name,'/usr/share/fonts/truetype/dejavu/'+file))
    methods_by_name=methods(evidence['source_sha']);code,version=version_info(evidence['source_sha']);guide={}
    supplements=supplemental_evidence(evidence);append_system_captures(evidence,output,supplements)
    for line in (ROOT/'mynotes/docs/user-guide/screen-guide.tsv').read_text().splitlines():
        if line and not line.startswith('#'):
            key,route,body,boundary=line.split('|');guide[key]=(route,body,boundary)
    for line in ((ROOT/'mynotes/docs/e2e-workflow-captions.tsv').read_text().splitlines() if code>=56 else []):
        if line.strip() and not line.startswith('#'):
            key,route,body,boundary=line.strip().split('|');guide[key]=(route,body,boundary)
    for screen in evidence['screens']:
        if not screen.get('owner'):
            candidates=[name for name,method in methods_by_name.items() if screen['scene'] in method['scenes']]
            if screen['scene'].startswith(('e2e-page-','e2e-library-')):candidates=['app.space.notes.CoreJourneysTest#wholeAppLibraryTraversalRecordsEachVisitedPage']
            if screen['scene'].startswith('e2e-large-txt-'):candidates=['app.space.notes.RealFolderStorageTest#largerTxtUsesLosslessPagesAndNeverCreatesAHomeNote']
            if screen['scene']=='content-item-drag':candidates=['app.space.notes.CoreJourneysTest#canonicalChecklistHandleTapIsContextualAndNativeDragPreservesRichRows']
            if screen['scene']=='image-text-selection':candidates=['app.space.notes.runtime.ImageSelectionTest#controllerSelectionChangesOnlyReviewUntilExplicitAppend']
            lane_report=evidence['profiles'].get(screen['lane'],{});ordered=lane_report.get('cases',[])
            expected_lane=set(ordered)|set(lane_report.get('missing',[]))
            if expected_lane:candidates=[name for name in candidates if name in expected_lane]
            screen['owner']=candidates[0] if len(candidates)==1 else None
    selected={}
    for screen in evidence['screens']:
        scene=screen['scene'];rank=(PROFILES.index(screen['lane']) if screen['lane'] in PROFILES else len(PROFILES),-int(screen.get('capture_run',evidence['run'])),screen['path'])
        if scene not in selected or rank<selected[scene][0]:selected[scene]=(rank,screen)
    statuses={name:case_status(name,evidence) for name in methods_by_name}
    passed=sum(x.get('passed',0) for x in evidence['profiles'].values());expected=sum(x.get('expected',0) for x in evidence['profiles'].values())
    failures=[f'{lane}: {failure}' for lane,data in evidence['profiles'].items() for failure in data.get('failures',[])]
    missing=[f'{lane}: {name}' for lane,data in evidence['profiles'].items() for name in data.get('missing',[]) if not any(str(f).startswith(name+':') for f in data.get('failures',[]))]
    package_file=ROOT/'mynotes/docs/design-2026-10-29/native'/evidence['run']/'apk-verification.json'
    package=json.loads(package_file.read_text()) if package_file.exists() else {}
    if package and (package.get('source_sha')!=evidence['source_sha'] or str(package.get('source_run'))!=evidence['run'] or package.get('version_code')!=code):raise ValueError('Independent package verification source mismatch')
    package_qualified=bool(package) and package.get('raw_apk_not_wrapped_zip') is True and package.get('internet_permission') is False and package.get('native_qualification_conclusion')=='success'
    native_qualified=qualification_gate(evidence,methods_by_name);supplemental_qualified=supplemental_gate(supplements,evidence['screens']);qualified=native_qualified and supplemental_qualified and package_qualified
    target=output/'MyNotes-Executed-E2E-Report.pdf';c=Canvas(str(target),pagesize=(842,595),pageCompression=1,invariant=1)
    c.setTitle('MyNotes executed end-to-end workflow report');c.setAuthor('MyNotes engineering audit')
    style=ParagraphStyle('body',fontName='Audit',fontSize=10,leading=14,textColor=HexColor('#263347'))
    small=ParagraphStyle('small',parent=style,fontSize=8,leading=11)
    page=0
    def text(value,x,y,width=740,small_text=False):
        p=Paragraph(escape(str(value)).replace('\n','<br/>'),small if small_text else style);w,h=p.wrap(width,10000)
        p.drawOn(c,x,y-h);return y-h-10
    def new(title,sub='EXECUTED EVIDENCE · NOT A USER GUIDE'):
        nonlocal page
        if page:c.showPage()
        page+=1;c.setFillColor(HexColor('#152b46'));c.rect(0,533,842,62,fill=1,stroke=0);c.setFillColor(white);c.setFont('AuditBold',16);c.drawString(34,563,title[:88]);c.setFont('Audit',8);c.drawString(34,545,sub[:170]);c.setFillColor(HexColor('#4c596a'));c.setFont('Audit',7);c.drawString(34,18,f"Run {evidence['run']} · source {evidence['source_sha'][:12]} · synthetic emulator fixtures · page {page}");return 510
    y=new('MyNotes · executed entire-app workflow audit')
    y=text(f'Candidate{code} · version{version} · Android builds and emulator execution in GitHub Actions only.',38,y)
    y=text(('AUTOMATION GATE PASSED' if qualified else 'NOT QUALIFIED — failures / unexecuted evidence remain')+f'\n{passed}/{expected} native method outcomes reported across eight shards. {len(selected)} distinct captured scenes; {len(evidence["screens"])} profile-specific images. The appendix includes every discovered native method, including unexecuted ones.',38,y)
    y=text('This is the newly executed report requested after physical candidate55 feedback, not the old116-page usage guide. All illustrated screens are synthetic CI screenshots. No supplied personal log, phone screenshot, note text, recording or private library was uploaded.',38,y)
    y=text('What this report can and cannot establish\nEach illustrated page includes its route, purpose, owner test result and image hash. A passing assertion is not a physical usability sign-off. All discovered native tests and literal UI-title declarations are audited; uncovered branches, unpictured surfaces and physical/OCR/voice limits remain explicit.',38,y)
    y=text('Source: '+evidence['source_sha']+'\nRun: https://github.com/tarungittarun/TicTic/actions/runs/'+evidence['run']+'\nReader order: result summary → workflow groups → limitations/failures → illustrated traversals → complete method ledger → literal UI-surface inventory.',38,y,small_text=True)
    y=new('Execution environment and result accounting')
    for lane in PROFILES:
        data=evidence['profiles'].get(lane,{})
        y=text(f"{lane}: {'PASS' if data.get('ok') else 'INCOMPLETE / FAIL'} · {data.get('passed',0)}/{data.get('expected','unknown')} · {len(data.get('failures',[]))} failure records · {len(data.get('missing',[]))} unexecuted/missing methods",38,y)
    text('Profiles are two disjoint method shards each: minimum API26; API34 landscape; API34 font2/night; current API36. Focused subsets are not every method on every profile. No tablet or ordinary portrait API34 execution is implied. Original PNG hashes and bounded JPEG preview hashes are retained in provenance.json.',38,y)
    y=new('Independent raw APK and JVM verification')
    if not package:y=text('No source-matching independent package verification available. This report is not fully qualified.',38,y)
    else:
        y=text(package['filename']+f"\n{package['bytes']} bytes · app.space.notes · version{package['version_name']}/code{package['version_code']} · minSDK{package['min_sdk']} · targetSDK{package['target_sdk']}",38,y)
        y=text('Raw APK SHA256 '+package['sha256']+'\nStable signer certificate SHA256 '+package['signer_sha256'],38,y)
        y=text(f"Exact executed JVM gate: {package['jvm_tests']} passed; native {package['native_outcomes']} outcomes / {package['unique_native_methods']} distinct methods. The verifier independently downloaded the already-built raw artifact and foundation report; no local Android build or APK reconstruction.\nINTERNET permission: absent. Development artifact, not public-release approval. Artifact expiry: {package['expires_at']}.",38,y)
        y=text('Download: https://github.com/tarungittarun/TicTic/actions/runs/'+evidence['run']+'/artifacts/'+str(package['artifact_id'])+'\n'+package['scope'],38,y,small_text=True)
    y=new('Exact-byte upgrade and actual process recovery')
    if not supplements['updates']:y=text('No source-matching install-over report available. Earlier candidate55 proofs do not qualify this source.',38,y)
    for item in supplements['updates']:
        report=item['report'];apk=item['identity']['candidate']
        y=text(f"{report['from']['version_code']} → {report['to']['version_code']}: {'PASS' if report.get('ok') else 'FAIL'} · API{report.get('api')} · Note/Doc retained={report.get('notes_and_doc_retained')} · uninstall/clear={report.get('uninstall_or_clear_data')}\nSource-matching raw APK SHA256 {apk['sha256']}",38,y,small_text=True)
    y=text('Install-over fixtures are public synthetic Note/Doc content. They do not establish physical user-library acceptance, arbitrary rich-data migration or complete folder-recovery correctness.',38,y)
    if not supplements['reader_recovery']:y=text('No source-matching actual process-death report available. Ordinary controller recreation is not silently substituted for an OS process kill.',38,y)
    for item in supplements['reader_recovery']:
        report=item['report']
        if y<200:y=new('Actual reader process recovery · continued')
        y=text(f"Reader process recovery: {'PASS' if report.get('ok') else 'FAIL'} · API{report.get('api')} · code{report.get('package_code')}",38,y)
        for record in report.get('records',[]):
            y=text(record['name']+f" · PID {record.get('before_pid')} → {record.get('after_pid')} · query restored={record.get('query_restored')} · page2={record.get('page_2_restored')} · visible row={record.get('visible_row_restored')} · original unchanged={record.get('original_unchanged')}",38,y,small_text=True)
        y=text(report.get('scope',''),38,y,small_text=True)
        diagnostic=report.get('diagnostic_file_save')
        if diagnostic:y=text('Real diagnostic file save: '+str(diagnostic['ok'])+' · '+str(diagnostic['bytes'])+' bytes · readback SHA256 '+diagnostic['sha256']+' · contents intentionally not included.',38,y,small_text=True)
    y=new('Physical55 feedback · disposition and proof boundary')
    feedback=[
      ('Viewed files search / count / file icons','Search matches bounded indexed text; counts and type badges replace gear rows. See the seeded-index search/count/no-match workflow and fresh screenshots.'),
      ('TXT controls / screen space','Permanent secondary buttons were moved to Text file options; navigation and Find stay in one compact viewer row. Real SAF tests retain lossless pages, explicit copy and unchanged originals.'),
      ('Rearrangement / accidental Doc switch','Gesture lifetime suppresses workspace swipes even after drag cleanup; late board refreshes are deferred and target bounds follow scrolling. The actual native hold/drop test injects a late refresh. Physical top-edge precision remains open.'),
      ('PDF / Word / sheet presentation','Redundant48dp metadata rows are removed; details remain in overflow. Existing page selection, zoom, Find, counts, sharing and original-byte tests remain selected. Word is still approximate.'),
      ('Inserted numbered point','New Notes and Doc workflows insert a middle point and assert downstream literal renumbering, caret, formatting and saved text, including9→10 digit-width changes.'),
      ('Voice inbox click / progress','The card now toggles existing playback and displays Preparing playback while loading; seek/pause/stop remain. Existing transport tests do not independently assert every new whole-card tap target. No physical voice-quality claim.'),
      ('Read image misses text / false language','Sparse segmentation is always compared for bounded coverage improvement. This is not training or a demonstrated correction of the supplied image. False-script accuracy remains OPEN.'),
      ('Optional log file saving','Diagnostics now explicitly says Save diagnostic log as a file. It uses the existing user-initiated Android create-document path; filtered saving remains. Screenshots alone are not proof of a completed real destination write. The separate signed-release diagnostic-save record reports real file readback only when that complete workflow passed.'),
      ('Entire-app executed report','All source-pinned native outcomes and distinct captured pages are included below, with unresolved owners and unpictured surfaces explicit. Passing automation is not blanket physical sign-off.'),
    ]
    for title,detail in feedback:
        if y<140:y=new('Physical55 feedback · disposition continued')
        y=text(title+'\n'+detail,38,y)
    y=new('End-to-end workflow map · execution, not promises');c.bookmarkPage('workflow');c.addOutlineEntry('Workflow and expected behaviour','workflow',0)
    for index,(title,expected_text,pattern) in enumerate(GROUPS):
        matched=[name for name in statuses if re.search(pattern,name)]
        counts={state:sum(statuses[n]['status']==state for n in matched) for state in ['PASS','FAIL','UNEXECUTED']}
        if y<155:y=new('End-to-end workflow map · continued')
        y=text(title,38,y);y=text(expected_text+f"\nLinked native methods (overlapping groups): {len(matched)}; PASS {counts['PASS']}, FAIL {counts['FAIL']}, UNEXECUTED {counts['UNEXECUTED']}. Exact method/profile ledger follows.",38,y,small_text=True)
    y=new('Open limitations and physical acceptance');c.bookmarkPage('limitations');c.addOutlineEntry('Open limits and physical acceptance','limitations',0)
    for title,detail in GAPS:
        if y<145:y=new('Open limitations and physical acceptance · continued')
        y=text(title+'\n'+detail,38,y)
    if failures or missing:
        y=new('Failed and unexecuted checks — retained, not hidden')
        for item in failures+missing:
            if y<110:y=new('Failed and unexecuted checks · continued')
            y=text(item[:3000],38,y,small_text=True)
    y=new('Illustrated traversal · screenshot index');c.bookmarkPage('screens');c.addOutlineEntry('Illustrated traversal','screens',0)
    text('Use the PDF bookmarks to jump to each captured page. The displayed image is one named profile; all captured variants remain in the evidence manifest. Screens shown before a later method failure remain explicitly labelled with that failure.',38,y)
    # Every distinct captured page is included; variants are enumerated instead of silently omitted.
    for scene,(_,screen) in sorted(selected.items(),key=lambda item:(not item[0].startswith('e2e-'),item[0])):
        y=new('Captured page · '+scene,('SIGNED-RELEASE SYSTEM PROBE · capture run '+str(screen['capture_run'])) if screen.get('proof_kind') else 'SYNTHETIC NATIVE EMULATOR CAPTURE · source-pinned');c.bookmarkPage('scene-'+scene);c.addOutlineEntry(scene,'scene-'+scene,1)
        route,purpose,boundary=describe(scene,guide);owner=screen.get('owner');status=statuses.get(owner,{'status':screen.get('probe_outcome','OWNER UNRESOLVED')})
        variants=[x['lane'] for x in evidence['screens'] if x['scene']==scene]
        image=output/screen['path']
        if sha(image.read_bytes())!=screen['sha256']:raise ValueError('Image hash mismatch')
        landscape=screen['width']>screen['height']
        if landscape:
            c.drawImage(ImageReader(str(image)),38,235,width=766,height=285,preserveAspectRatio=True,anchor='c',mask='auto')
            y=text('Route: '+route,38,222,766);y=text(purpose,38,y,766);y=text('Boundary: '+boundary,38,y,766,True)
            text('Owner: '+str(owner)+' · '+status['status']+'\nShown: '+screen['lane']+'; captured variants: '+', '.join(variants)+'\nJPEG SHA256 '+screen['sha256'],38,min(y,110),766,True)
        else:
            c.drawImage(ImageReader(str(image)),38,55,width=292,height=465,preserveAspectRatio=True,anchor='c',mask='auto')
            y=text('Route: '+route,354,510,450);y=text(purpose,354,y,450)
            y=text('Expected / actual\n'+('Actual signed-release Android workflow; exact-byte source identity, recovery and file-readback assertions passed.' if screen.get('proof_kind') else 'Assertions in '+owner.split('#')[-1]+'.' if owner else 'No unique owner association; screenshot is visual evidence only.')+('\nSigned-release workflow outcome: ' if screen.get('proof_kind') else '\nOwner method outcome: ')+status['status'],354,y,450)
            y=text('Boundary: '+boundary,354,y,450)
            y=text('Owner: '+str(owner)+'\nShown: '+screen['lane']+'\nCaptured profiles: '+', '.join(variants)+('\nImage capture run: '+str(screen['capture_run']) if screen.get('capture_run') else ''),354,y,450,True)
            text('JPEG SHA256 '+screen['sha256']+'\nOriginal PNG SHA256 '+screen['original_png_sha256'],354,min(y,100),450,True)
    y=new('Complete native-method execution ledger');c.bookmarkPage('ledger');c.addOutlineEntry('Complete native method ledger','ledger',0)
    y=text('PASS means a successful execution with no reported failure for that method; profile-specific missing outcomes remain listed. UNEXECUTED is not a failed assertion. A parser/storage method need not have a visible UI. Abbreviations:36a/b=current API36;26a/b=minimum API26;L34a/b=landscape;F34a/b=font2/night. All exact source locations refer to the pinned source SHA on every page.',38,y)
    aliases=dict(zip(PROFILES,['36a','36b','26a','26b','L34a','L34b','F34a','F34b']))
    previous_class=None
    for name,method in sorted(methods_by_name.items()):
        result=statuses[name];scene_names=sorted({x['scene'] for x in evidence['screens'] if x.get('owner')==name})
        class_name=name.split('#')[0].split('.')[-1]
        value=(class_name+' · '+name.split('#')[-1]+' · '+result['status']+'\n'
            +'Expected: '+re.sub(r'(?<=[a-z])(?=[A-Z])',' ',name.split('#')[-1])+'.\n'
            +'Actual PASS: '+(', '.join(aliases[x] for x in result['passed']) or 'none')
            +'; FAIL: '+(', '.join(aliases[x] for x in result['failed']) or 'none')
            +'; missing selected: '+(', '.join(aliases[x] for x in result['missing']) or 'none reported')+'\n'
            +'Actions: '+(' → '.join(method['actions']) or 'Direct runtime/domain/Android operations; no UI-click route inferred.')+'\n'
            +'Captured: '+(', '.join(scene_names) or 'NO ASSOCIATED SCREENSHOT')+'\n'
            +'Source: '+method['file']+':'+str(method['line'])+f" · {method['assertions']} direct assertion calls (plus helpers).")
        paragraph=Paragraph(escape(value).replace('\n','<br/>'),small);_,height=paragraph.wrap(766,10000)
        if y-height<45:y=new('Complete native-method ledger · continued')
        if class_name!=previous_class:
            c.bookmarkPage('class-'+class_name);c.addOutlineEntry(class_name,'class-'+class_name,1);previous_class=class_name
        paragraph.drawOn(c,38,y-height);y-=height+16
    y=new('Literal UI-surface inventory · coverage gaps exposed');c.bookmarkPage('surfaces');c.addOutlineEntry('Literal UI surfaces / remaining coverage gaps','surfaces',0)
    y=text('Every statically discoverable literal header / panel / dialog title is listed below. Dynamic titles and Android/external pages are outside this literal scan. A title in source is NOT evidence of traversal. Captured-scene associations are descriptive, not automatic approval.',38,y)
    for row in source_surfaces(evidence['source_sha']):
        if y<65:y=new('Literal UI-surface inventory · continued')
        y=text(row['title']+' — '+row['file']+':'+str(row['line']),38,y,small_text=True)
    c.save()
    summary=dict(run=evidence['run'],source_sha=evidence['source_sha'],version_code=code,version_name=version,automation_gate_ok=qualified,native_gate_ok=native_qualified,supplemental_gate_ok=supplemental_qualified,release_approval=False,physical_acceptance=False,raw_apk_verified=package_qualified,jvm_tests=package.get('jvm_tests'),illustrated_images=len(selected),
        passed_outcomes=passed,expected_outcomes=expected,pages=page,distinct_scenes=len(selected),profile_images=len(evidence['screens']),
        native_methods=len(methods_by_name),method_status_counts={s:sum(v['status']==s for v in statuses.values()) for s in ['PASS','FAIL','UNEXECUTED']},
        pdf_sha256=sha(target.read_bytes()),private_user_data_included=False)
    (output/'report-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    (output/'supplemental-evidence.json').write_text(json.dumps(supplements,indent=2)+'\n')
    (output/'case-ledger.json').write_text(json.dumps(statuses,indent=2)+'\n')
    (output/'provenance.json').write_text(json.dumps(evidence,indent=2)+'\n')
    return target,summary


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True);parser.add_argument('--source',required=True);parser.add_argument('--artifacts',type=Path);parser.add_argument('--preserved',type=Path);parser.add_argument('--output',required=True,type=Path);args=parser.parse_args()
    if not args.run.isdigit() or not re.fullmatch(r'[a-f0-9]{40}',args.source):raise ValueError('Exact run/source required')
    if args.artifacts and args.preserved:raise ValueError('Choose artifacts OR preserved evidence')
    evidence=from_preserved(args.preserved,args.output,args.run,args.source) if args.preserved else ingest(args.artifacts,args.output,args.run,args.source) if args.artifacts else json.loads((args.output/'provenance.json').read_text())
    if evidence['run']!=args.run or evidence['source_sha']!=args.source:raise ValueError('Source/run mismatch')
    target,summary=build_pdf(evidence,args.output);print(target);print(json.dumps(summary,indent=2))

if __name__=='__main__':main()

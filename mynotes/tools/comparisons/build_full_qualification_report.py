"""Render actual exhaustive automation results without promoting them to whole-app UX acceptance."""
import argparse
from collections import Counter
from html import escape
import json
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak

from mynotes.tools.build_module_e2e_report import module_for, MODULES
from mynotes.tools.ci.test_plan import build_plan, cases

ROOT = Path(__file__).resolve().parents[3]


def render(evidence, evidence_commit, root_count, notes_count):
    evidence = Path(evidence)
    summary = json.loads((evidence / 'summary.json').read_text())
    run = json.loads((evidence / 'run.json').read_text())
    ledger = json.loads((ROOT / 'mynotes/docs/ux-improvement-ledger/ledger.json').read_text())
    base = 'https://github.com/tarungittarun/TicTic/blob/' + evidence_commit + '/' + str(evidence.relative_to(ROOT)) + '/'
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(name='SmallFull', fontSize=8, leading=10, spaceAfter=5))
    flow, md = [], []

    def text(value, style='BodyText'):
        return Paragraph(escape(str(value)), styles[style])

    def section(title, new=False):
        if new:
            flow.append(PageBreak())
        flow.append(text(title, 'Heading1'))
        md.extend(['## ' + title, ''])

    def paragraph(value):
        flow.extend([text(value), Spacer(1, 8)])
        md.extend([str(value), ''])

    def link(label, url):
        flow.append(Paragraph('<link href="' + escape(url, quote=True) + '" color="#155A91">' + escape(label) + '</link>', styles['SmallFull']))
        md.extend(['[' + label + '](' + url + ')', ''])

    def table(rows, widths):
        tab = Table([[text(cell, 'SmallFull') for cell in row] for row in rows], colWidths=widths, repeatRows=1)
        tab.setStyle(TableStyle([('VALIGN', (0, 0), (-1, -1), 'TOP'),
                                 ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#E7F1F4')),
                                 ('LINEBELOW', (0, 0), (-1, -1), .3, colors.lightgrey),
                                 ('TOPPADDING', (0, 0), (-1, -1), 5), ('BOTTOMPADDING', (0, 0), (-1, -1), 5)]))
        flow.append(tab)
        md.extend(['| ' + ' | '.join(map(str, row)).replace('\n', ' ') + ' |' for row in rows[:1]])
        md.append('|' + '|'.join(['---'] * len(rows[0])) + '|')
        md.extend(['| ' + ' | '.join(map(str, row)).replace('\n', ' ') + ' |' for row in rows[1:]])
        md.append('')

    section('MyNotes candidate71 — full automation qualification')
    paragraph('Actual completed-run results; not a claim that every possible behavior or both apps have been fully accepted. No public APK release approval. Physical-device, novice-use and exhaustive paired Obsidian acceptance remain open.')
    paragraph('Workflow outcome: ' + str(summary['workflow_conclusion']) + '. Exact tested source: ' + summary['source'] + '. Candidate: 0.71.0 / versionCode 71; stable app.space.notes identity and release signing are checked separately below.')
    link('Completed qualification run and individual jobs', summary['url'])
    link('Original run metadata and job outcomes', base + 'run.json')
    link('Independently reparsed native outcome summary', base + 'summary.json')
    paragraph('Fresh local checks: root Python QA ' + str(root_count) + ' passed; MyNotes Python QA ' + str(notes_count) + ' passed. These are separately discovered suites, not additional native/device tests. Calculator and its paused workflow were not changed or built.')
    paragraph('The native selection was expanded from eight selected methods per profile to all ' + str(summary['native_unique_methods']) + ' discovered methods. There are 37 Java test classes; that number is coincidental and does not establish acceptance of the 37 paired UX task families.')
    paragraph('Native executions passed: ' + str(summary['native_executions_passed']) + ' / ' + str(summary['native_expected_executions']) + ' planned across six profiles. Every profile uses two disjoint method shards. Missing, duplicate, skipped, failed and unexpected outcomes are rejected. Native result counts are reparsed from original instrumentation, not inferred from process exit zero.')

    section('Build, JVM, release identity and signed install-over', True)
    build = evidence / 'full71-build-evidence'
    jvm_paths = list(build.rglob('full-jvm.json'))
    if jvm_paths:
        jvm = json.loads(jvm_paths[0].read_text())
        paragraph('Fresh domain JVM: ' + str(jvm.get('passed', 0)) + '/' + str(jvm.get('expected', 0)) + '; exact result ok=' + str(jvm.get('ok')) + '. Failures: ' + str(jvm.get('failures', [])))
        link('Exact JVM method receipt', base + str(jvm_paths[0].relative_to(evidence)))
    else:
        paragraph('Fresh JVM receipt missing: not verified.')
    for job in run.get('jobs', []):
        if job['name'] == 'build' or job['name'].startswith('update '):
            paragraph(job['name'] + ': ' + str(job.get('conclusion')))
            link('Job steps and actual conclusion', job['url'])
    for audit in build.rglob('release-audit.json'):
        data = json.loads(audit.read_text())
        paragraph('Internal release APK: ' + str(data['bytes']) + ' bytes; SHA256 ' + data['sha256'] + '. Permissions: ' + ', '.join(data['permissions']) + '. No INTERNET permission is allowed. Explicit Android backup disabling, minimum API26, non-debuggable release, stable package/certificate and 156,213,948-byte size cap are enforced.')
        link('Release byte identity and manifest audit', base + str(audit.relative_to(evidence)))
    for proof in sorted(evidence.glob('full71-release69-to71-*/release-rich-update-proof.json')):
        data = json.loads(proof.read_text())
        paragraph(proof.parent.name + ': ok=' + str(data.get('ok')) + '; uninstall/clear=' + str(data.get('uninstall_or_clear_data')) + '. ' + data.get('scope', ''))
        link('Original signed install-over proof', base + str(proof.relative_to(evidence)))
    paragraph('Install-over probes use a disposable public generated rich library, signed release69 followed by release71 with adb install -r. They check old-writer snapshots, private folders, installation identity and candidate write/readback. They do not prove physical SAF providers, real microphone files, every user-library history or UI rendering fidelity. Internal CI APK artifacts are not a public release.')

    rechecks_file = evidence.parent / 'rechecks.json'
    if rechecks_file.exists():
        rechecks = json.loads(rechecks_file.read_text())
        section('Completed corrections and isolated rechecks — original score unchanged', True)
        paragraph(rechecks['conclusion'])
        table([['Recheck profile / scope', 'Passed / selected', 'Observed outcome']]+[[row['profile'], str(row['passed'])+'/'+str(row['expected']), row['meaning']] for row in rechecks['native']], [190, 85, 240])
        for row in rechecks['native']:
            link('Original rerun assertions: ' + row['profile'], row['log_url'])
            link('Exact independently checked receipt', row['receipt_url'])
        paragraph('Do not add these rerun counts to the original six-profile result as new unique coverage. No production implementation changed in this check phase. The PDF observer correction is instrumentation-only; it is not a new app UX fix. Three unresolved checks reproduce in isolation; the original spreadsheet discrepancy is not reproduced, not explained away or marked fixed.')
        for attempt in rechecks['attempts']:
            paragraph(attempt['run'] + ': ' + attempt['meaning'])
            link('Preserved attempt', attempt['url'])
        identity = evidence.parent / 'recheck-apk-identity.json'
        if identity.exists():
            proof = json.loads(identity.read_text())
            paragraph('Debug-pair provenance: original app SHA256 ' + proof['original_app']['sha256'] + '; used app SHA256 ' + proof['used_app']['sha256'] + '. App/test certificate matches=' + str(proof['app_test_same_certificate']) + '; all unsigned ZIP payload entries equal=' + str(proof['unsigned_payload_equal']) + '. Changed unsigned entries: ' + str(proof['changed_unsigned_entries']) + '. Different signatures are not byte-identical APKs. User release signing and release APKs were not changed.')
            link('Actual debug payload and certificate proof', base.rstrip('/').rsplit('/', 1)[0] + '/recheck-apk-identity.json')
            paragraph('These are source-equivalent rebuilds, not exact original-APK retests. The entry comparison finds three differing native Whisper shared libraries; it does not justify pooling cross-build speech speed/accuracy or declaring the binaries identical. No production source or model weights changed in this check phase.')

    section('Five actual failed checks — triage, not waived', True)
    findings_file = evidence.parent / 'findings.json'
    if findings_file.exists():
        findings = json.loads(findings_file.read_text())['findings']
        for finding in findings:
            paragraph(finding['id'] + ' | ' + finding['profile'] + ' | ' + finding['observation'])
            paragraph('Classification: ' + finding['classification'] + '. Status: ' + finding['status'] + '.')
            if finding.get('retest'):
                paragraph('Completed recheck: ' + finding['retest'])
            paragraph('Next discriminating check: ' + finding['next_test'])
            link('Original failed method: ' + finding['method'], base.rstrip('/').rsplit('/', 1)[0] + '/' + finding['source'])
        for finding in findings:
            if 'image' not in finding:
                continue
            from PIL import Image as PILImage
            from reportlab.platypus import Image
            section(finding['id'] + ' — reviewed large-text capture', True)
            path = evidence.parent / finding['image']
            with PILImage.open(path) as image:
                w, h = image.size
            flow.append(Image(str(path), width=490*w/h, height=490))
            paragraph('Actual retained screenshot from another journey on this same large-text profile; not a failure-instant capture or changed-build proof. Original pixels are unaltered. ' + finding['observation'])
            link('Open original screenshot', base.rstrip('/').rsplit('/', 1)[0] + '/' + finding['image'])

    section('Every native profile and exact method results', True)
    table([['Profile shard', 'Passed / expected', 'Exact native result']]+[
        [name.removeprefix('full71-native-'), str(row.get('passed', 0)) + '/' + str(row.get('expected', '?')), str(row.get('ok', False))]
        for name, row in summary['native_lanes'].items()], [280, 105, 130])
    paragraph('Profiles: API26 light portrait; API36 light portrait; API34 dark portrait; API34 dark portrait at font scale 2.0; API34 light landscape; API34 dark landscape. Nexus 5 synthetic x86_64 guests. API26 uses the default image; API34/36 use Google APIs images. These are not six physical phones or a universal device matrix.')
    paragraph('Airplane mode, disabled Wi-Fi/data and absence of a default network are verified before installation. Microphone runtime permission is granted only on API36; other profiles exercise denied state. Fixtures are public/synthetic. API36 app animator scale is 1; other native profiles use deterministic scale 0. This does not measure comparative animation quality, jank, typing latency or human efficiency.')
    for name, row in summary['native_lanes'].items():
        link(name + ' — original assertions', base + name + '/refinement-native.txt')
        for failure in row.get('failures', []):
            paragraph('FAILED: ' + failure[:2500])
        if row.get('missing'):
            paragraph('Methods not passed (including recorded failures): ' + ', '.join(row['missing'])[:3500])

    section('Functional automation inventory, not UX ratings', True)
    planned = cases(build_plan()['android'])
    module_expected = Counter(module_for(name) for name in planned)
    passed = Counter()
    for receipt in evidence.glob('full71-native-*/refinement-native.json'):
        for name in json.loads(receipt.read_text()).get('cases', []):
            passed[module_for(name)] += 1
    table([['Internal test area', 'Unique methods', 'Passed executions / planned']]+[
        [name, module_expected[name], str(passed[name]) + '/' + str(module_expected[name]*6)] for name in MODULES], [280, 85, 150])
    paragraph('This categorisation groups existing test methods for navigation; it is not a new module-ordered run or a matched Obsidian experiment. The full original method inventories and outputs are retained per shard. File formats, rich text, persistence, folders, Trash, permissions, OCR, speech, playback state, exports, selection, diagrams and UI journeys are checked only to the extent of those named methods and fixtures.')
    paragraph('Quantitative controlled OCR and speech-degradation reports are independently rescored in CI when their owning method is selected. Exported PDF link annotations and geometry are parsed independently using pypdf in shard0. The reports below are raw observations, not a general OCR/audio accuracy or speed ranking.')
    for name in sorted(evidence.glob('full71-native-*/refinement-quality-*.json')):
        link('Quantitative public-fixture report: ' + name.parent.name + '/' + name.name, base + str(name.relative_to(evidence)))
    for name in sorted(evidence.glob('full71-native-*/refinement-pdf-results.json')):
        link('Independent PDF verification: ' + name.parent.name, base + str(name.relative_to(evidence)))

    section('What remains unaccepted — no misleading “everything passed”', True)
    paragraph('1. All 37 paired task families still require wider matched installed-app observations against official Obsidian 1.15.0. Prior bounded comparison evidence remains valid only for its stated versions/tasks; this run is MyNotes-only. Extra MyNotes features are not comparative score advantages.')
    paragraph('2. The two implemented improvements remain partially verified. Search-passage provenance/highlighting and compact Format regressions are included in the full inventory. This does not establish similar-note discovery at scale, all rich-content variants, TalkBack, novice discoverability or universal three-line keyboard-open writing context. Earlier landscape captures without the keyboard are not keyboard-open landscape acceptance.')
    paragraph('3. Real-device touch/selection/reorder feel, large text, keyboard transitions, microphone/playback quality, physical provider persistence, accessibility and install-over on the user device remain separate acceptance tasks. No private content or recording was accessed.')
    paragraph('4. Real-world multilingual/tiny/damaged/image-scene OCR, broad speech accents/noise and held-out ground-truth benchmarks beyond these native fixtures remain unaccepted on candidate71. No model training or cloud inference was performed. Passing bundled-fixture tests is not a general accuracy claim.')
    paragraph('5. Human task time, efficiency, frame jank, battery, startup-to-ready, large-library responsiveness, all malformed-file/security inputs, every migration history and all OS/vendor combinations are not exhausted by this suite. The retained failures below, if any, are not waived by other passes.')
    paragraph('This completes the recorded automated check scope only when the completed run says success; it cannot honestly close physical or exhaustive two-app UX acceptance. The existing execution ledger remains ongoing rather than marking untested tasks complete.')
    table([['Paired task family', 'Remaining acceptance task']]+[[p['id'] + ' / ' + p['name'], p['task']] for p in ledger['plan']], [240, 275])
    section('Evidence preservation and reproducibility', True)
    paragraph('Original native logs, receipts, environment snapshots, public screenshots, quantitative reports, JVM XML and migration proofs are retained byte-for-byte where present. A missing receipt remains missing. The hash manifest records actual retained bytes. APKs/models/dependencies are excluded from Git. Capture dimensions vary by helper: some fixtures are resized to 540px before artifact creation; others use full-window PixelCopy. Retention does not reconstruct or alter pixels.')
    link('Original-file SHA256 retention manifest', base + 'retention.json')
    link('Exact qualification workflow source', 'https://github.com/tarungittarun/TicTic/blob/' + summary['source'] + '/.github/workflows/notes-full71.yml')
    link('Previous bounded improvement ledger — not superseded as paired evidence', 'https://github.com/tarungittarun/TicTic/blob/211fac005aa0f2fc8083aec64c879506efdbce10/mynotes/docs/ux-improvement-ledger/Improvement_Execution_Ledger.md')
    out = evidence.parent
    (out / 'Full_Qualification.md').write_text('\n'.join(md))

    def footer(canvas, doc):
        canvas.setFont('Helvetica', 7)
        canvas.drawString(40, 23, 'MyNotes candidate71 | automated scope only | physical / paired acceptance open')
        canvas.drawRightString(555, 23, str(doc.page))

    SimpleDocTemplate(str(out / 'Full_Qualification.pdf'), pagesize=(595.276, 841.89),
                      leftMargin=40, rightMargin=40, topMargin=38, bottomMargin=42,
                      title='MyNotes candidate71 Full Automation Qualification').build(flow, onFirstPage=footer, onLaterPages=footer)
    return out / 'Full_Qualification.pdf'


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--evidence', required=True)
    parser.add_argument('--evidence-commit', required=True)
    parser.add_argument('--root-count', type=int, required=True)
    parser.add_argument('--notes-count', type=int, required=True)
    args = parser.parse_args()
    print(render(args.evidence, args.evidence_commit, args.root_count, args.notes_count))

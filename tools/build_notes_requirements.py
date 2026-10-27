"""Rebuild initial scope ledger; do not use once evidence is added by hand."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
s=(ROOT/'NOTES-PLAN.md').read_text().split('## 4. The top 100 Google Keep features',1)[1].split('## 5.',1)[0]
items=re.findall(r'^(\d+)\. (.+)$',s,re.M)
assert [int(n) for n,t in items]==list(range(1,101))
lines=['# Notes requirement and evidence ledger','',
'Permanent IDs, 2026-10-27. Original scope snapshots below retain historical markers for the OLD MySpace implementation. They are NOT evidence for new MyNotes. Owner-approved development baseline overrides historical proposals.',
'', 'All new-runtime requirements start Planned. AT-* are reserved acceptance scenario IDs, not executable tests. Before each slice, specify Given/When/Then cases and link executable tests plus runtime evidence. No item becomes Verified from a source grep alone.', '', '## Original inventory', '']
for n,t in items:
    lines += [f'### K{int(n):03}',f'- Scope: {t}', '- State: **Planned** for new runtime.',f'- Acceptance reservation: `AT-K{int(n):03}`; scenarios due before implementation.', '- Evidence: none.', '']
lines += ['## Expanded scope', '', '| ID | Acceptance obligation | State / evidence |','|---|---|---|']
groups={
'DG':[
'Basic and expanded shapes serialize/reopen without geometry loss.',
'Attached straight/elbow/curved connectors follow moved shapes; labels and heads persist.',
'Free and block text remains editable, styled, searchable and readable.',
'Grouping, layers, lock, duplicate, copy/paste, align and distribute retain relationships.',
'Zoom/pan/grid/guides, templates, undo/redo and full-content PNG pass both themes.'],
'ST':[
'Chosen supported local tree holds the actual authoritative library; cancel preserves destination.',
'Interrupted writes recover without false Saved feedback.',
'Folder move verifies documents/media before switching and preserves source on failure.',
'Revoked/full/unplugged/provider-failed storage exposes recovery without silent relocation.',
'Schema migration, backup/import and duplicate-ID handling retain documents and assets.',
'Stale writes and forks are blocked, not silently overwritten.'],
'ML':[
'Bundled OCR works from clean offline install; script coverage and quality are documented.',
'Bundled automatic offline ASR preserves audio on failure, supports progress/cancel/retry.',
'Microphone deny/permanent deny/revoke leaves other features and imported-audio ASR usable.',
'Engine/model hashes/licenses, ABIs and real-device RAM/latency/quality results are documented.'],
'HX':[
'New runtime stays standalone-only until deliberate integration; no old feature consumes it.',
'Both host adapters pass route/theme/permission/share/notification/widget contracts.',
'Receiving host acquires folder grant and migrates data without loss/duplicate IDs.'],
'NF':[
'10,000-note search is off-thread, latest query wins and input focus does not churn.',
'Both themes, large text, TalkBack, lifecycle and device/API matrix pass.',
'Diagnostics record outcomes, omit content by default and share only explicitly.']}
for group,requirements in groups.items():
    for i,text in enumerate(requirements,1):
        id=f'{group}-{i:02}'
        evidence='Planned; no runtime evidence'
        if id in ['ST-02','ST-06']:evidence='Protocol probe only: qa/test_notes_storage_probe.py; Android/SAF pending'
        lines.append(f'| {id} | {text} | {evidence} |')
(ROOT/'docs/notes/REQUIREMENTS.md').write_text('\n'.join(lines)+'\n')

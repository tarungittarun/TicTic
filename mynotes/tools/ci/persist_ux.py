"""Persist only bounded synthetic debug-host screenshots; never app files or user attachments."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import struct
from mynotes.tools.ci.audit import REPO

VARIANTS={'full-api36','full-api34','night-api34','large-font-api34','landscape-api34','tablet-api34'}
SCENES={'notes-workspace','doc-workspace','doc-header-table','stage-aa-selection','stage-aa-typing','stage-named-checklist','stage-checklist-drop-preview','document-mixed','document-table','document-saved-styles','document-typing-style','document-pdf-options','document-format','document-keyboard','document-visuals','document-dragged','checklist-drag-failure','writing-lines','image-crop-preview','checklist-item-actions','group-prerequisites','shape-picker','read-image-language','ocr-review-empty','ocr-review','backups','restore-preview','editor-focus-failure','folders','diagnostics','diagram-group','diagram-options','reminder-time','shape-dialog','reminder-date','remove-image-confirmation','input-injection-failure','mixed-note-header','drawing-options','control-failure','trash','trash-confirmation','drawing-tools','drawing-focus','bootstrap-failure','filters-view','filters-sort','filters-labels','library','backgrounds','background-editor','drawing','image-editor','voice-inbox','about','add-reminder','settings-after-navigation','search-keyboard','numeric-h1','numeric-h2','home','editor','editor-keyboard','format','format-keyboard','capture','palette','labels','diagram','checklist','settings','storage'}

def persist(evidence,run,source):
    if not run.isdigit() or len(source)!=40 or any(c not in '0123456789abcdef' for c in source):raise ValueError('Invalid run/source provenance')
    output=REPO/'mynotes/docs/ux-review'/run
    records=[]
    for image in sorted(Path(evidence).rglob('ux-*.png')):
        lane=image.parent.name;scene=image.stem[3:]
        if lane not in VARIANTS or scene not in SCENES:continue
        data=image.read_bytes()
        if len(data)<24 or len(data)>512*1024 or not data.startswith(b'\x89PNG\r\n\x1a\n'):continue
        width,height=struct.unpack('>II',data[16:24])
        if not 1<=width<=1080 or not 1<=height<=3000:raise ValueError('Unexpected proof dimensions')
        target=output/lane/(scene+'.png');target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(image,target)
        records.append({'lane':lane,'scene':scene,'path':str(target.relative_to(output)),'width':width,'height':height,'sha256':hashlib.sha256(data).hexdigest()})
    outcomes=[]
    for result in sorted(Path(evidence).rglob('results.json')):
        if result.parent.name not in VARIANTS:continue
        if result.stat().st_size>256*1024:raise ValueError('Unexpectedly large runner result')
        value=json.loads(result.read_text());target=output/result.parent.name/'results.json';target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(value,indent=2)+'\n');outcomes.append(result.parent.name)
    if records or outcomes:
        (output/'provenance.json').write_text(json.dumps({'run':run,'source':source,'scope':'Real Android screen captures from synthetic, isolated debug-host fixtures. Not physical-device or official-release screenshots; test success and aesthetic inspection are separate.','images':records,'runner_outcomes':outcomes},indent=2)+'\n')
    print(f'Persisted {len(records)} bounded synthetic screenshots from run {run}')
    return len(records)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--evidence',required=True);a=p.parse_args();persist(a.evidence,os.environ['GITHUB_RUN_ID'],os.environ['GITHUB_SHA'])

"""Independent scoring/retention of a completed public study, never an OCR oracle."""
import argparse, html, json, math, re, statistics, unicodedata
from pathlib import Path

def normalize(value):
    # Match Java NFC + trim + default ASCII regex \\s, not Python's broader Unicode whitespace.
    return re.sub(r'[ \t\n\v\f\r]+',' ',unicodedata.normalize('NFC',value).strip(''.join(map(chr,range(33)))))
def distance(a,b):
    if len(a)>4096 or len(b)>4096 or len(a)*len(b)>4000000:raise ValueError('Unbounded evaluation pair')
    previous=list(range(len(b)+1))
    for i,x in enumerate(a,1):
        row=[i]
        for j,y in enumerate(b,1):row.append(min(row[-1]+1,previous[j]+1,previous[j-1]+(x!=y)))
        previous=row
    return previous[-1]
def independent_row(row):
    if row['status']!='OK' or row['source_pixels_unchanged'] is not True:raise ValueError('Operational/source preservation failure retained')
    if type(row['cer']) not in (int,float) or not math.isfinite(row['cer']):raise ValueError('Nonfinite or invalid CER')
    if type(row['elapsed_ms']) not in (int,float) or not math.isfinite(row['elapsed_ms']):raise ValueError('Nonfinite elapsed time')
    a,b=normalize(row['reference']),normalize(row['recognized']);edits=distance(a,b)
    if edits!=row['character_edits'] or len(a)!=row['reference_characters'] or abs(edits/max(1,len(a))-row['cer'])>1e-9:raise ValueError('Independent CER mismatch')
    if row['elapsed_ms']<0:raise ValueError('Invalid elapsed time')
    return edits/max(1,len(a))
def build(font_path,scene_path,manifest_path,output):
    fonts=json.loads(Path(font_path).read_text());scenes=json.loads(Path(scene_path).read_text());manifest=json.loads(Path(manifest_path).read_text())
    for data,count in [(fonts,144),(scenes,5)]:
        if data['expected']!=count or len(data['cases'])!=count or data['production_changed'] or data['physical_acceptance']:raise ValueError('Study count/scope mismatch')
        for row in data['cases']:independent_row(row)
    expected={r['id']:r for r in manifest['records']}
    if {r['id'] for r in scenes['cases']}!=set(expected):raise ValueError('Public scene inventory differs')
    for row in scenes['cases']:
        if row['reference']!=expected[row['id']]['reference'] or row['crop']!=expected[row['id']]['crop']:raise ValueError('Frozen public reference/crop mismatch')
    pairs={}
    for row in fonts['cases']:
        identity=row['id'].rsplit('-',1)[0];pair=pairs.setdefault(identity,{})
        if row['candidate'] in pair:raise ValueError('Duplicate study candidate')
        pair[row['candidate']]=row
    if len(pairs)!=72 or any(set(p)!={False,True} for p in pairs.values()):raise ValueError('Incomplete paired font inventory')
    better=worse=equal=0
    for pair in pairs.values():
        a,b=pair[False],pair[True]
        if any(a[k]!=b[k] for k in ['font','font_px','dark','reference','source_pixels_sha256']):raise ValueError('Pair is not source/condition matched')
        if b['character_edits']<a['character_edits']:better+=1
        elif b['character_edits']>a['character_edits']:worse+=1
        else:equal+=1
    def metrics(rows):return {'count':len(rows),'exact':sum(r['character_edits']==0 for r in rows),'mean_cer':sum(r['cer'] for r in rows)/len(rows),'median_ms':statistics.median(r['elapsed_ms'] for r in rows),'max_ms':max(r['elapsed_ms'] for r in rows)}
    summary={'schema':1,'run':41830346993,'source':'cba7533f2a1a58fd90fed1a54d36a3bd68ab960d','production_changed':False,'model_changed':False,'physical_acceptance':False,'final_acceptance':False,'paired_inputs':72,'font_measurements':144,'public_scene_inputs':5,'candidate_better':better,'candidate_worse':worse,'candidate_equal':equal,'baseline':metrics([p[False] for p in pairs.values()]),'candidate4x':metrics([p[True] for p in pairs.values()]),'public_scenes':metrics(scenes['cases']),'public_scene_non_latin_letter_outputs':sum(r['non_latin_letters']>0 for r in scenes['cases']),'negative_controls':sum(not r['reference'] for r in scenes['cases']),'negative_false_positives':sum(not r['reference'] and bool(r['recognized'].strip()) for r in scenes['cases'])}
    summary['public_text_inputs']=metrics([r for r in scenes['cases'] if r['reference']])
    summary['public_scene_indic_glyph_outputs']=sum(any(0x0900<=ord(c)<=0x097f or 0x0c80<=ord(c)<=0x0cff for c in r['recognized']) for r in scenes['cases'])
    output=Path(output);output.mkdir(parents=True,exist_ok=True);(output/'independent-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    def cell(text):return html.escape(str(text)).replace('|','&#124;').replace('\n','<br>')
    lines=['# Executed small-font and varied public-scene study','', 'Run41830346993; sourcecba7533f. This recovers already completed inference, not a rerun. The workflow failed because a copied43-case interaction verifier was applied after the two study methods; the study itself verifies two methods and149 measurements. No production OCR change, training, physical phone acceptance or final APK qualification.','', '72 authored font/script/size/polarity inputs, each measured twice; not144 independent scenes. Five public inputs from three original iOS screenshots: two complete photo/dialog screenshots, one selected-row crop, two no-text nature-photo crops. Not positive street-sign OCR or untouched heldout data.','', 'Timings are emulator wall-clock inference, counterbalanced order in font pairs; first call includes cold provisioning. Decode/synthesis/upscale/scoring excluded. CER keeps characters/symbols; empty-reference denominator is one. Whole-image CER does not erase script/status-icon inventions. Public text-input metrics exclude the empty-reference controls; the aggregate public_scenes mean mixes positive CER with empty-reference edit counts and is NOT an accuracy percentage. Non-Latin-letter counts exclude digits; indic-glyph counts also catch invented Kannada/Devanagari digits/marks.','', '```json',json.dumps(summary,indent=2),'```','', '## Public inputs: every hypothesis, including failures','', '| Input | Reference | Actual output | CER | ms | Non-Latin letters |','|---|---|---|---:|---:|---:|']
    for r in scenes['cases']:lines.append('| '+' | '.join([cell(r['id']),cell(r['reference']),cell(r['recognized']),f"{r['cer']:.6f}",str(r['elapsed_ms']),str(r['non_latin_letters'])])+' |')
    lines+=['','## Paired authored small-font inputs: all72 pairs','', '| ID / condition | Reference | Baseline output | 4x output | CER base /4x | ms base /4x |','|---|---|---|---|---:|---:|']
    for identity,pair in pairs.items():
        a,b=pair[False],pair[True];lines.append('| '+' | '.join([cell(f"{identity} {a['font']} {a['font_px']}px {'dark' if a['dark'] else 'light'}"),cell(a['reference']),cell(a['recognized']),cell(b['recognized']),f"{a['cer']:.6f} / {b['cer']:.6f}",f"{a['elapsed_ms']} / {b['elapsed_ms']}"])+' |')
    lines+=['','## Adoption','', 'Study only. Do not adopt a4x global rewrite from these paired fixtures. Any improvement needs source-only eligibility, readable-native-script preservation, varied positive text photos, original full-corpus regression and honest latency/miss accounting. Existing OCR remains unchanged.']
    (output/'Study.md').write_text('\n'.join(lines)+'\n');return summary
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--fonts',required=True);p.add_argument('--scenes',required=True);p.add_argument('--manifest',required=True);p.add_argument('--output',required=True);a=p.parse_args();print(json.dumps(build(a.fonts,a.scenes,a.manifest,a.output),indent=2))

"""Read-only regression review. Never remove characters from production output."""
import json
from pathlib import Path
import unicodedata


def review(reference,recognized):
    # These two frozen screenshots contain ASCII text. Never apply this rule to
    # legitimate Indic/mixed references or use it as a production language filter.
    if not reference.isascii():return {'applicable':False,'reason':'Non-ASCII reference; no ASCII-only inference is justified'}
    unexpected=[]
    for i,char in enumerate(recognized):
        if ord(char)>127 and unicodedata.category(char)[0] in ('L','N'):
            unexpected.append({'character':char,'unicode_name':unicodedata.name(char,'UNKNOWN'),'offset':i,'context':recognized[max(0,i-12):i+13]})
    return {'applicable':True,'no_unexpected_script_letters_or_digits':not unexpected,'unexpected':unexpected}


def audit(root):
    root=Path(root);rows=[]
    files=sorted(root.glob('*-public-screenshot-independent.json'))
    if len(files)!=4:raise ValueError('Require original four-profile independent audit files')
    for file in files:
        report=json.loads(file.read_text());assert report['ok'] and len(report['cases'])==6
        for row in report['cases']:
            result=review(row['reference'],row['recognized']);assert result['applicable']
            rows.append({'profile':file.name.removesuffix('-public-screenshot-independent.json'),'id':row['id'],'cer_target_met':row['accuracy_target_met'],'cer':row['cer'],**result})
    return {'capture_run':41811062528,'observations':len(rows),'observations_with_unexpected_scripts':sum(not r['no_unexpected_script_letters_or_digits'] for r in rows),'review_passed':all(r['no_unexpected_script_letters_or_digits'] for r in rows),'original_cer_passes_not_relabelled':True,'production_output_modified':False,'model_changed':False,'physical_acceptance':False,'scope':'Separate read-only review of two manually transcribed ASCII public screenshots. Unexpected letters/digits are retained as counterexamples, not erased. This is not a universal language classifier or a new inference run.','cases':rows}


if __name__=='__main__':
    root=Path('mynotes/docs/recognition/public-screenshots/41811062528')
    report=audit(root);(root/'script-invention-review.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k!='cases'},indent=2))

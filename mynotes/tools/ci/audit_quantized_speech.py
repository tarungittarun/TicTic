"""Independent rescore of retained quantization predictions; never model adoption."""
import argparse
import json
import re
import statistics
from pathlib import Path


def edits(reference, hypothesis):
    prior=list(range(len(hypothesis)+1))
    for i,word in enumerate(reference,1):
        current=[i]
        for j,other in enumerate(hypothesis,1):
            current.append(min(current[-1]+1,prior[j]+1,prior[j-1]+(word!=other)))
        prior=current
    return prior[-1]


def words(text):
    return re.findall(r"[a-z0-9]+(?:'[a-z0-9]+)?",text.lower())


def audit(report):
    expected={speaker+'-arctic_b'+str(i).zfill(4) for speaker in ('ksp','slt') for i in range(33,65)}
    by_model={model:{} for model in ('f16','q8_0','q5_1')}
    if len(report['cases'])!=192:
        raise ValueError('Missing or duplicated observations')
    for row in report['cases']:
        group=by_model[row['model']]
        if row['id'] not in expected or row['id'] in group or row['status']!='OK':
            raise ValueError('Missing, duplicated or operationally failed case')
        reference=words(row['reference'])
        if not reference:
            raise ValueError('Empty ground truth')
        count=edits(reference,words(row['hypothesis']))
        if abs(count/len(reference)-row['wer'])>1e-8:
            raise ValueError('Reported WER does not match stored hypothesis')
        group[row['id']]={**row,'word_edits':count,'reference_words':len(reference)}
    summary={}
    for model,rows in by_model.items():
        if set(rows)!=expected:
            raise ValueError('Model cohort incomplete')
        regressed=[];improved=[];changed=[]
        for identity,row in rows.items():
            base=by_model['f16'][identity]
            if any(base[key]!=row[key] for key in ('input_sha256','reference','speaker')):
                raise ValueError('Paired input/reference drift')
            if row['word_edits']>base['word_edits']:regressed.append(identity)
            if row['word_edits']<base['word_edits']:improved.append(identity)
            if row['hypothesis']!=base['hypothesis']:changed.append(identity)
        summary[model]={'cases':len(rows),'word_edits':sum(r['word_edits'] for r in rows.values()),'reference_words':sum(r['reference_words'] for r in rows.values()),'model_bytes':report['source_model'][model]['bytes'],'regressed_ids':regressed,'improved_ids':improved,'changed_hypothesis_ids':changed,'median_process_seconds':statistics.median(r['process_seconds'] for r in rows.values())}
    return {'adopted':False,'scope':'Independently rescored retained predictions. Desktop CPU, not Android timing or unseen Indian-speaker acceptance. No runtime model changed. Process times include loading; ordered runs are descriptive, not causal speed estimates.','summary':summary}


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('report',type=Path);parser.add_argument('output',type=Path);args=parser.parse_args()
    result=audit(json.loads(args.report.read_text()));args.output.write_text(json.dumps(result,indent=2)+'\n')
    print('::notice title=Independent compact speech prediction audit::'+json.dumps(result))

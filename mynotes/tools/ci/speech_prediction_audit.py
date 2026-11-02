"""Recompute public research predictions; no training, inference, or adoption decision."""
import argparse
import hashlib
import json
from pathlib import Path
import re


def edits(reference, hypothesis):
    previous=list(range(len(hypothesis)+1))
    for i,word in enumerate(reference,1):
        row=[i]
        for j,other in enumerate(hypothesis,1):
            row.append(min(row[-1]+1,previous[j]+1,previous[j-1]+(word!=other)))
        previous=row
    return previous[-1]


def tokens(text):
    return re.findall(r"[a-z0-9']+",text.lower())


def audit(report):
    if set(report['baseline'])!=set(report['adapted']):
        raise ValueError('Speaker groups differ')
    groups={}
    for speaker,baseline in report['baseline'].items():
        adapted=report['adapted'][speaker]
        indexed=[]
        for result in (baseline,adapted):
            cases={}
            for case in result['cases']:
                key=case['id']
                if key in cases or case['speaker']!=speaker:
                    raise ValueError('Duplicate case or speaker mismatch')
                reference=tokens(case['reference'])
                if not reference or len(reference)!=case['reference_words'] or edits(reference,tokens(case['hypothesis']))!=case['word_edits']:
                    raise ValueError('Reported prediction metrics do not match text')
                cases[key]=case
            if not cases:
                raise ValueError('Empty evaluation group')
            for key in ('word_edits','reference_words'):
                if sum(case[key] for case in cases.values())!=result[key]:
                    raise ValueError('Incorrect aggregate')
            indexed.append(cases)
        before,after=indexed
        if before.keys()!=after.keys():
            raise ValueError('Paired case identities differ')
        changes=[]
        for key,case in before.items():
            other=after[key]
            if case['reference']!=other['reference']:
                raise ValueError('Paired references differ')
            changes.append({'id':key,'baseline_edits':case['word_edits'],'adapted_edits':other['word_edits'],'delta':other['word_edits']-case['word_edits']})
        groups[speaker]={
            'cases':len(changes),'reference_words':baseline['reference_words'],
            'baseline_edits':baseline['word_edits'],'adapted_edits':adapted['word_edits'],
            'improved':sum(c['delta']<0 for c in changes),
            'unchanged':sum(c['delta']==0 for c in changes),
            'regressed':sum(c['delta']>0 for c in changes),
            'regressions':[c for c in changes if c['delta']>0],
        }
    return {'scope':'Independent recomputation of stored public predictions only; no model inference, no general-accuracy or unseen-speaker claim',
            'adopted':False,'groups':groups}


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report',type=Path)
    parser.add_argument('output',type=Path)
    args=parser.parse_args()
    raw=args.report.read_bytes()
    result=audit(json.loads(raw))
    result['source_report_sha256']=hashlib.sha256(raw).hexdigest()
    args.output.write_text(json.dumps(result,indent=2)+'\n')

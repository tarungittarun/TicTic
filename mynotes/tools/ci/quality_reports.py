"""Independently require and re-score quantitative reports for selected native methods."""
import argparse
import json
import math
import re
import unicodedata
from pathlib import Path

REPORTS={
 'app.space.notes.OfflineOcrTest#controlledCommonTextCorpusReportsStrictCharacterErrorWithoutErasingSymbols':('refinement-quality-ocr-common-text-report.json','ocr'),
 'app.space.notes.OfflineSpeechTest#attenuatedAndSeededNoiseSpeechHaveBoundedMeasuredRegressionAndPreserveSource':('refinement-quality-speech-degradation-report.json','speech')}

def distance(a,b):
    prior=list(range(len(b)+1))
    for i,x in enumerate(a,1):
        row=[i]
        for j,y in enumerate(b,1):row.append(min(row[-1]+1,prior[j]+1,prior[j-1]+(x!=y)))
        prior=row
    return prior[-1]/max(1,len(a))

def validate(report,kind):
    rows=report['cases']
    expected={0,1,2} if kind=='ocr' else {'one-third-amplitude','seeded-plus-minus-200-pcm16'}
    if len(rows)!=len(expected) or {row['variant'] for row in rows}!=expected:raise ValueError('Missing/duplicate quantitative variants')
    if kind=='speech' and not re.fullmatch('[0-9a-f]{64}',report.get('source_sha256','')):raise ValueError('Missing source hash')
    for row in rows:
        reference=row['reference'] if kind=='ocr' else row['baseline'];actual=row['recognized']
        if not isinstance(reference,str) or not isinstance(actual,str) or not reference.strip() or max(len(reference),len(actual))>4000:raise ValueError('Invalid bounded measurement text')
        if kind=='ocr':
            a=unicodedata.normalize('NFC',' '.join(reference.split()));b=unicodedata.normalize('NFC',' '.join(actual.split()));key='cer';threshold=.2
            if '+' not in actual or '=' not in actual or not 0<=row['confidence']<=100:raise ValueError('Missing required symbols/confidence')
        else:a=reference.lower().split();b=actual.lower().split();key='relative_wer';threshold=.6
        score=distance(a,b)
        if not actual.strip() or not isinstance(row[key],(float,int)) or not math.isfinite(row[key]) or abs(score-row[key])>1e-8 or score>threshold:raise ValueError('Invalid or inconsistent recognition score')
        if not isinstance(row['elapsed_ms'],(int,float)) or not math.isfinite(row['elapsed_ms']) or row['elapsed_ms']<0:raise ValueError('Invalid elapsed measurement')
    return True

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--expected',required=True);parser.add_argument('--root',default='.');args=parser.parse_args()
    selected=json.loads(Path(args.expected).read_text())
    for case,(filename,kind) in REPORTS.items():
        if case in selected:
            path=Path(args.root)/filename
            if not path.is_file() or path.stat().st_size>65536:raise ValueError('Selected quantitative report missing or oversized: '+filename)
            validate(json.loads(path.read_text()),kind);print('Verified '+filename)
if __name__=='__main__':main()

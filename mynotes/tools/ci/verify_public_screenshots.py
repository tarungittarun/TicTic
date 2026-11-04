"""Independent Python scorer for public pixels; references never select OCR output."""
import json
import math
from pathlib import Path
import re
import unicodedata
from mynotes.tools.ci.results import android

ROOT=Path('mynotes/report-assets/public-ocr-screenshots')


def normalized(value):
    return ' '.join(unicodedata.normalize('NFC',value).split())


def edits(reference,actual):
    previous=list(range(len(actual)+1))
    for i,left in enumerate(reference,1):
        current=[i]
        for j,right in enumerate(actual,1):
            current.append(min(previous[j]+1,current[j-1]+1,previous[j-1]+(left!=right)))
        previous=current
    return previous[-1]


def score(report,manifest):
    expected={f'{record["id"]}-{variant}':(record,variant) for record in manifest['records'] for variant in range(3)}
    rows=report['cases']
    if len(rows)!=6 or len(expected)!=6 or len({row['id'] for row in rows})!=6 or {row['id'] for row in rows}!=set(expected):
        raise ValueError('Missing, duplicate or unexpected public screenshots')
    scored=[]
    for row in rows:
        record,variant=expected[row['id']]
        if row['source_sha256']!=record['sha256'] or not re.fullmatch('[0-9a-f]{64}',row['input_sha256']):
            raise ValueError('Incorrect public input identity')
        if variant==0 and row['input_sha256']!=record['sha256']:raise ValueError('Original screenshot must not be transformed')
        divisor=2 if variant==1 else 1
        if (row['width'],row['height'])!=(record['width']//divisor,record['height']//divisor):raise ValueError('Wrong pixel dimensions')
        if row['condition']!=('original','half-size-derived','jpeg50-derived')[variant]:raise ValueError('Wrong derivative label')
        if type(row['elapsed_ms']) not in (float,int) or not math.isfinite(row['elapsed_ms']) or row['elapsed_ms']<0:raise ValueError('Invalid inference duration')
        if row['status']!='OK':
            scored.append({'id':row['id'],'accuracy_target_met':False,'operational_error':True});continue
        if row['reference']!=record['reference']:raise ValueError('Reference changed after inference')
        reference=normalized(record['reference']);actual=normalized(row['recognized']);distance=edits(reference,actual);cer=distance/max(1,len(reference));met=cer<=.15
        if row['source_pixels_unchanged'] is not True or not math.isfinite(row['cer']) or abs(cer-row['cer'])>1e-10 or row['accuracy_target_met'] is not met:
            raise ValueError('Native score/source invariant disagrees with independent audit')
        scored.append({'id':row['id'],'reference':record['reference'],'recognized':row['recognized'],'cer':cer,'character_edits':distance,'accuracy_target_met':met,'elapsed_ms':row['elapsed_ms']})
    return {'expected':6,'observed':len(scored),'all_accuracy_targets_met':all(row['accuracy_target_met'] for row in scored),'cases':scored,'physical_acceptance':False,'training_steps':0,'scope':manifest['scope']}


def main():
    output=Path('public-screenshot-independent.json')
    try:
        file=Path('public-screenshot-benchmark.json')
        if not 0<file.stat().st_size<100000:raise ValueError('Missing or oversized retained public benchmark')
        report=score(json.loads(file.read_text()),json.loads((ROOT/'manifest.json').read_text()))
        expected=['app.space.notes.EnvironmentQualificationTest#'+name for name in ('fontScaleMatchesRequestedLane','orientationMatchesRequestedLane','nightResourcesMatchRequestedLane')]+['app.space.notes.PublicScreenshotBenchmark#verifiedPublicScreenshotsMeasureProductionRecognitionAndSourcePreservation']
        execution=android(Path('refinement-native.txt').read_text(errors='replace'),expected)
        report['execution']=execution
        report['ok']=execution['ok'] and report['all_accuracy_targets_met']
    except Exception as error:
        report={'ok':False,'error':str(error),'physical_acceptance':False}
    output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(('::notice' if report['ok'] else '::error')+' title=Independent public screenshot audit::'+json.dumps({k:v for k,v in report.items() if k!='cases'})[:3500])
    if not report['ok']:raise SystemExit(1)


if __name__=='__main__':main()

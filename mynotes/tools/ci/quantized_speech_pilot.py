"""Nonshipping quantization parity study; public speech, not general ASR approval."""
import argparse
import hashlib
import json
import re
import subprocess
import time
from pathlib import Path
from mynotes.tools.ci.quality_reports import distance


def tokens(text):
    return re.findall(r"[a-z0-9]+(?:'[a-z0-9]+)?", text.lower())


def regression(base, candidate):
    if any(base[k] != candidate[k] for k in ('id','input_sha256','reference')):
        raise ValueError('Quantization fixture mismatch')
    return candidate['status'] != 'OK' or base['status'] != 'OK' or candidate['wer'] > base['wer'] + 1e-8


def run(cli, models, corpus, output):
    output.mkdir(parents=True,exist_ok=True)
    manifest=json.loads((corpus/'manifest.json').read_text())
    records=[r for r in manifest['records'] if 33 <= int(r['id'].rsplit('b',1)[1]) <= 64]
    if len(records)!=64 or len({r['id'] for r in records})!=64:
        raise ValueError('Expected 32 fixed prompts per speaker')
    rows=[];sizes={}
    for label,model in models.items():
        with model.open('rb') as stream:
            sizes[label]={'bytes':model.stat().st_size,'sha256':hashlib.file_digest(stream,'sha256').hexdigest()}
        for record in records:
            wave=corpus/record['file']
            if hashlib.sha256(wave.read_bytes()).hexdigest()!=record['sha256']:
                raise ValueError('Audio bytes changed')
            destination=output/(label+'-'+record['id'])
            began=time.monotonic()
            result=subprocess.run([str(cli),'-m',str(model),'-f',str(wave),'-l','en','-t','2','-bs','1','-bo','1','-tp','0','-nf','-nt','-np','-oj','-of',str(destination)],capture_output=True,text=True,timeout=120)
            row=dict(id=record['id'],speaker=record['speaker'],model=label,input_sha256=record['sha256'],reference=record['reference'],audio_seconds=record['seconds'],process_seconds=time.monotonic()-began,status='ERROR',hypothesis='',wer=None)
            if result.returncode==0 and destination.with_suffix('.json').is_file():
                parsed=json.loads(destination.with_suffix('.json').read_text())
                text=' '.join(segment['text'] for segment in parsed['transcription']).strip()
                row.update(status='OK',hypothesis=text,wer=distance(tokens(record['reference']),tokens(text)))
            else:
                row['error']=result.stderr[-2000:]
            rows.append(row)
            print(json.dumps(row),flush=True)
            # Preserve partial evidence before the next inference.
            (output/'partial-cases.json').write_text(json.dumps(rows,indent=2)+'\n')
    baseline={row['id']:row for row in rows if row['model']=='f16'}
    summary={}
    for label in models:
        group=[r for r in rows if r['model']==label]
        summary[label]={'observations':len(group),'errors':sum(r['status']!='OK' for r in group),'regressions_against_f16':sum(regression(baseline[r['id']],r) for r in group),'model_bytes':sizes[label]['bytes']}
    report=dict(scope='Quantization-only comparison of the existing adapted experimental model. Not new training, default-engine adoption, unseen Indian-speaker evidence, Android latency or physical acceptance. Fixed public CMU B0033-B0064 prompts, two speakers; KSP is the same speaker used in earlier adaptation. Per-process timings include model loading. All regressions retained.',source_model=sizes,summary=summary,cases=rows)
    (output/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print('::notice title=Nonshipping speech quantization results::'+json.dumps(summary))
    if any(r['status']!='OK' for r in rows):
        raise SystemExit(1)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--cli',type=Path,required=True);p.add_argument('--models',type=Path,required=True);p.add_argument('--corpus',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();run(a.cli.resolve(),{label:(a.models/(label+'.bin')).resolve() for label in ('f16','q8_0','q5_1')},a.corpus,a.output)

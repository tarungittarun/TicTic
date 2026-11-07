"""Retain completed public comparison job log bytes; diagnose infrastructure, not UX."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess


def collect(run, output):
    output=Path(output);output.mkdir(parents=True,exist_ok=True)
    metadata=json.loads(subprocess.check_output(['gh','run','view',str(run),'--json','databaseId,headSha,status,conclusion,url,jobs']))
    if metadata['status']!='completed':raise ValueError('Only completed original logs can be retained')
    files=[]
    for job in metadata['jobs']:
        endpoint='repos/tarungittarun/TicTic/actions/jobs/'+str(job['databaseId'])+'/logs'
        receipt={'job':job['databaseId'],'name':job['name'],'endpoint':'https://api.github.com/'+endpoint}
        try:
            response=subprocess.run(['gh','api',endpoint],capture_output=True,timeout=120)
            # New gh versions reject raw ANSI-bearing job logs, even when
            # captured to a pipe. Opt in only for this exact refusal; save the
            # bytes to a file, never replay raw terminal controls.
            if response.returncode and b'pass --allow-escape-sequences' in response.stderr:
                response=subprocess.run(['gh','api',endpoint,'--allow-escape-sequences'],capture_output=True,timeout=120)
                receipt['raw_escape_preserving_retry']=True

            if response.returncode:
                detail=re.sub(r'(https?://[^?\s]+)\?\S+',r'\1?[signed-query-redacted]',response.stderr.decode(errors='replace'))[:1500]
                receipt.update(retained=False,returncode=response.returncode,error=detail)
                files.append(receipt);continue
            data=response.stdout
            path=output/(str(job['databaseId'])+'.txt');path.write_bytes(data)
            receipt.update(path=path.name,bytes=len(data),sha256=hashlib.sha256(data).hexdigest(),source_bytes_modified=False)
            lines=[line for line in data.decode(errors='replace').splitlines() if re.search(r'Out of memory|Killed process|SIGSEGV|Segmentation fault|Fatal signal|emulator.*(?:exit|kill|crash)|Keyboard did not dismiss|mInputShown|mIsInputViewShown',line,re.I)]
            if lines:
                text=('run'+str(run)+' '+job['name']+'\n'+'\n'.join(lines[-12:]))[:3500]
                text=re.sub(r'[\x00-\x08\x0b\x0c\x0e-\x1f\x7f-\x9f]','?',text)
                print('::notice title=Original log diagnostic (not app fault attribution)::'+text.replace('%','%25').replace('\r','%0D').replace('\n','%0A'))
        except Exception as error:receipt.update(error=str(error),retained=False)
        files.append(receipt)
    (output/'manifest.json').write_text(json.dumps({'run':metadata['databaseId'],'source':metadata['headSha'],'url':metadata['url'],'files':files,'scope':'Exact completed-job log responses. Errors retain retrieval failure, never an inferred app result.'},indent=2)+'\n')
    return files


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('run',type=int);parser.add_argument('output');args=parser.parse_args()
    print(json.dumps(collect(args.run,args.output),indent=2))

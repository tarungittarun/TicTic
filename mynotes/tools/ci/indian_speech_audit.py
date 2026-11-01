"""Public metadata research only. No gated downloads, credentials, training or private audio."""
import hashlib,json,urllib.request
from pathlib import Path


def get(url,limit=4*1024*1024):
    request=urllib.request.Request(url,headers={'User-Agent':'MyNotes-public-research/1'})
    with urllib.request.urlopen(request,timeout=45) as response:
        data=response.read(limit+1)
    if len(data)>limit:raise ValueError('Metadata exceeds bound')
    return data


def main():
    report={'scope':'Dataset/model availability and licensing audit only; no training or accuracy claim','datasets':[]}
    for name,role in [('ai4bharat/Svarah','held-out Indian-accent benchmark; never train on evaluation data'),('ai4bharat/IndicVoices','candidate multilingual training corpus; inspect language coverage, consent and license')]:
        row={'dataset':name,'role':role}
        try:
            info=json.loads(get('https://huggingface.co/api/datasets/'+name));row.update({'revision':info.get('sha'),'gated':info.get('gated'),'license':info.get('cardData',{}).get('license'),'files':info.get('siblings',[])[:50]})
            if not row['revision'] or row.get('gated'):row['status']='blocked: revision absent or dataset gated; do not download audio'
            else:
                card=get('https://huggingface.co/datasets/'+name+'/raw/'+row['revision']+'/README.md',512*1024)
                row.update({'card_sha256':hashlib.sha256(card).hexdigest(),'card_excerpt':card.decode('utf-8')[:7000],'status':'metadata retrieved; audio not downloaded, licensing and split review still required'})
        except Exception as error:row.update({'status':'unavailable; no training performed','error':type(error).__name__+': '+str(error)[:200]})
        report['datasets'].append(row)
    report['model_candidates']=[{'model':'vosk-model-small-en-in-0.4','publisher_license':'Apache-2.0 per alphacep/vosk-space/models.md','published_NPTEL_Pure_WER_percent':49.05,'decision':'not evidence of PRO accuracy; benchmark against current Vosk and Whisper before adoption'},{'model':'Whisper tiny/base multilingual via sherpa-onnx','decision':'CPU Android candidate; prior American fixture is not an Indian-accent benchmark. Do not call inference training.'}]
    Path('indian-speech-audit.json').write_text(json.dumps(report,indent=2))
    for row in report['datasets']:
        print('::notice title=Indian speech corpus audit::'+json.dumps({key:value for key,value in row.items() if key not in {'files','card_excerpt'}}))
    print(json.dumps(report,indent=2))

if __name__=='__main__':main()

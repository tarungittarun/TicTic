"""Public, checksum/license-verified evaluation only. No training or personal inputs.
Run in Actions; large archives/generated waveforms stay out of Git.
"""
import argparse,hashlib,io,json,re,tarfile,wave
from pathlib import Path
from mynotes.tools.ci.indian_speech_pilot import ARCHIVES,download

VARIANTS=['clean','quarter-amplitude','snr10-uniform-seed1729','gain3-clipped']
COUNT_PER_SPEAKER=100

def retained_grant(speaker,digest):
    # Some original CMU archives omit COPYING. Reuse the exact primary-source
    # grant already retrieved and hash-bound to this archive in the earlier audit.
    proof=Path(__file__).resolve().parents[2]/'docs/speech-acoustic/41393453222'
    metadata=json.loads((proof/'data-report.json').read_text())['archives'][speaker]
    grant=(proof/'licenses'/(speaker+'-COPYING.txt')).read_text()
    if metadata['sha256']!=digest or metadata['license_sha256']!=hashlib.sha256(grant.encode()).hexdigest():raise ValueError('Retained primary license provenance mismatch')
    return grant,metadata['primary_web_license_url']


def prepare(output):
    output.mkdir(parents=True,exist_ok=True);records=[];licenses={}
    for speaker,digest in ARCHIVES.items():
        print('Preparing checksum/license-verified public speaker '+speaker,flush=True)
        archive=output/(speaker+'.tar.bz2')
        errors=[]
        for host in ['https://festvox.org/','http://festvox.org/']:
            try:
                download(host+f'cmu_arctic/packed/cmu_us_{speaker}_arctic.tar.bz2',archive,400*1024*1024)
                if hashlib.sha256(archive.read_bytes()).hexdigest()!=digest:raise ValueError('Archive checksum mismatch')
                break
            except Exception as error:errors.append(str(error)[:180])
        else:raise RuntimeError('Public corpus unavailable; no substitute/fabricated samples: '+str(errors))
        # Decompress once to a bounded temporary tar: bz2 backward seeks otherwise
        # repeatedly decompress the source during large-cohort extraction.
        import bz2,shutil
        unpacked=output/(speaker+'.tar')
        with bz2.open(archive,'rb') as src,unpacked.open('wb') as dst:
            total=0
            while True:
                chunk=src.read(1024*1024)
                if not chunk:break
                total+=len(chunk)
                if total>800*1024*1024:raise ValueError('Corpus expansion exceeds bound')
                dst.write(chunk)
        with tarfile.open(unpacked,'r:') as bundle:
            members=bundle.getmembers()
            if len(members)>50000:raise ValueError('Unbounded public inventory')
            suffix={m.name.removeprefix('./').split('/',1)[-1]:m for m in members if m.isfile()}
            license_member=suffix.get('COPYING')
            if license_member is not None:
                assert license_member.size<50000
                grant=bundle.extractfile(license_member).read().decode('utf8');license_source='COPYING inside pinned source archive'
            else:grant,license_source=retained_grant(speaker,digest)
            if not all(term in grant.lower() for term in ['free for use for any purpose','commercial or otherwise','modifications must be clearly marked','original authors']):raise ValueError('Explicit source permission missing')
            (output/(speaker+'-COPYING.txt')).write_text(grant)
            licenses[speaker]={'archive_sha256':digest,'license_sha256':hashlib.sha256(grant.encode()).hexdigest(),'url':host+f'cmu_arctic/packed/cmu_us_{speaker}_arctic.tar.bz2','license_source':license_source}
            text=suffix['etc/txt.done.data'];assert text.size<1000000
            prompts=dict(re.findall(r'\(\s*(arctic_[ab]\d+)\s+"(.*?)"\s*\)',bundle.extractfile(text).read().decode()))
            for i in range(21,21+COUNT_PER_SPEAKER):
                identity=f'arctic_b{i:04d}';member=suffix['wav/'+identity+'.wav'];assert 44<member.size<=2*1024*1024
                raw=bundle.extractfile(member).read()
                with wave.open(io.BytesIO(raw)) as audio:
                    assert audio.getframerate()==16000 and audio.getnchannels()==1 and audio.getsampwidth()==2 and audio.getcomptype()=='NONE'
                    seconds=audio.getnframes()/16000
                assert 0<seconds<=15
                name=speaker+'-'+identity+'.wav';(output/name).write_bytes(raw)
                records.append({'id':speaker+'-'+identity,'speaker':speaker,'reference':prompts[identity],'file':name,'sha256':hashlib.sha256(raw).hexdigest(),'seconds':seconds,'bytes':len(raw)})
        archive.unlink();unpacked.unlink()
    manifest={'schema':1,'records':records,'variants':VARIANTS,'licenses':licenses,'expected_voice_cases':len(records)*len(VARIANTS)+4,'unique_speakers':2,'training_steps':0,'scope':'200 distinct public read utterances, KSP Indian-English male and SLT US-English female; 4 deterministic conditions plus 4 silence controls. Not 804 speakers, spontaneous dictation, multilingual ASR or physical microphone evaluation. KSP was used in earlier experimental Whisper tuning; this cohort measures unchanged default Vosk, not a held-out-speaker Whisper claim.'}
    (output/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
    print(json.dumps({k:v for k,v in manifest.items() if k!='records'},indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--output',type=Path,required=True);prepare(parser.parse_args().output)

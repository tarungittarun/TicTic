"""CPU-only public-fixture experiment; never imports weights into the Android product.
No training claim. Reports lexical WER separately from punctuation and model load cost.
"""
import argparse, base64, hashlib, json, os, re, resource, subprocess, sys, tarfile, time, wave, zipfile
from pathlib import Path

REFERENCE = 'and so my fellow americans ask not what your country can do for you ask what you can do for your country'
BLOB = '3184d372cd2f8b804d3a540c70ec50d927b335d2'
ASSET = 'https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-tiny.en.tar.bz2'

def wer(reference, hypothesis):
    a=re.findall(r'\w+',reference.lower()); b=re.findall(r'\w+',hypothesis.lower())
    previous=list(range(len(b)+1))
    for i,x in enumerate(a,1):
        row=[i]
        for j,y in enumerate(b,1):row.append(min(previous[j]+1,row[-1]+1,previous[j-1]+(x!=y)))
        previous=row
    return {'edits':previous[-1],'reference_words':len(a),'lexical_wer':previous[-1]/max(1,len(a))}

def child(args):
    import numpy as np
    path=Path(args.root)/('input-'+args.variant+'.wav')
    with wave.open(str(path)) as f:
        if f.getnchannels()!=1 or f.getsampwidth()!=2 or f.getframerate()!=16000:raise ValueError('Expected PCM16 mono 16 kHz')
        pcm=f.readframes(f.getnframes())
    started=time.perf_counter()
    if args.engine=='whisper-tiny-en':
        import sherpa_onnx
        model=Path(args.root)/'sherpa-onnx-whisper-tiny.en'
        recognizer=sherpa_onnx.OfflineRecognizer.from_whisper(encoder=str(model/'tiny.en-encoder.int8.onnx'),decoder=str(model/'tiny.en-decoder.int8.onnx'),tokens=str(model/'tiny.en-tokens.txt'),num_threads=2,debug=False)
        loaded=time.perf_counter(); stream=recognizer.create_stream();stream.accept_waveform(16000,np.frombuffer(pcm,dtype='<i2').astype(np.float32)/32768);recognizer.decode_stream(stream);text=stream.result.text
    else:
        import vosk
        vosk.SetLogLevel(-1);model=vosk.Model(str(Path(args.root)/'vosk-model-small-en-us-0.15'));recognizer=vosk.KaldiRecognizer(model,16000);loaded=time.perf_counter();parts=[]
        for at in range(0,len(pcm),8000):
            if recognizer.AcceptWaveform(pcm[at:at+8000]):parts.append(json.loads(recognizer.Result()).get('text',''))
        parts.append(json.loads(recognizer.FinalResult()).get('text',''));text=' '.join(parts).strip()
    result={'engine':args.engine,'variant':args.variant,'text':text,'load_seconds':loaded-started,'inference_seconds':time.perf_counter()-loaded,'process_peak_rss_kib':resource.getrusage(resource.RUSAGE_SELF).ru_maxrss,'input_sha256':hashlib.sha256(path.read_bytes()).hexdigest()}
    result.update(wer('' if args.variant=='silence' else REFERENCE,text));print(json.dumps(result))

def prepare(root):
    import numpy as np
    root.mkdir(parents=True,exist_ok=True)
    blob=json.loads(subprocess.check_output(['gh','api',f'repos/ggml-org/whisper.cpp/git/blobs/{BLOB}'],text=True));audio=base64.b64decode(blob['content'])
    if len(audio)!=352078 or hashlib.sha1(b'blob '+str(len(audio)).encode()+b'\0'+audio).hexdigest()!=BLOB:raise ValueError('Public fixture identity drift')
    original=root/'original.wav';original.write_bytes(audio)
    with wave.open(str(original)) as f:params=f.getparams();samples=np.frombuffer(f.readframes(f.getnframes()),dtype='<i2').astype(np.int32)
    rng=np.random.default_rng(20261101)
    variants={'clean':samples,'attenuated':np.trunc(samples/3).astype(np.int32),'seeded-noise':samples+rng.integers(-200,201,len(samples)),'silence':np.zeros(len(samples),dtype=np.int32)}
    for name,values in variants.items():
        with wave.open(str(root/('input-'+name+'.wav')),'wb') as f:f.setparams(params);f.writeframes(np.clip(values,-32768,32767).astype('<i2').tobytes())
    archive=root/'whisper.tar.bz2'
    subprocess.run(['curl','--fail','--location','--max-time','180','--max-filesize','118071777',ASSET,'-o',str(archive)],check=True)
    if archive.stat().st_size!=118071777:raise ValueError('Research archive size drift')
    # No tar extraction paths/symlinks, scripts, or model-supplied code are executed.
    wanted={'tiny.en-encoder.int8.onnx','tiny.en-decoder.int8.onnx','tiny.en-tokens.txt'};members={}
    with tarfile.open(archive) as tar:
        for member in tar:
            if member.isfile() and Path(member.name).name in wanted:
                name=Path(member.name).name
                if name in members or member.size>200_000_000:raise ValueError('Unsafe research model member')
                dest=root/'sherpa-onnx-whisper-tiny.en'/name;dest.parent.mkdir(exist_ok=True);data=tar.extractfile(member).read(member.size+1)
                if len(data)!=member.size:raise ValueError('Incomplete model member')
                dest.write_bytes(data);members[name]=hashlib.sha256(data).hexdigest()
    if set(members)!=wanted:raise ValueError('Missing model files')
    subprocess.run([sys.executable,'-m','mynotes.tools.models.prepare_voice','--manifest','mynotes/runtime/src/main/assets/voice/model.json','--output',str(root/'vosk.zip'),'--cache',str(root/'cache')],check=True)
    with zipfile.ZipFile(root/'vosk.zip') as z:
        for item in z.infolist():
            if item.is_dir():continue
            dest=root/item.filename
            if not dest.resolve().is_relative_to(root.resolve()):raise ValueError('Unsafe Vosk member')
            dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(z.read(item))
    return {'fixture_git_blob':BLOB,'fixture_sha256':hashlib.sha256(audio).hexdigest(),'whisper_asset_id':179374011,'whisper_archive_sha256_observed':hashlib.sha256(archive.read_bytes()).hexdigest(),'whisper_member_sha256':members,'provenance_limit':'Whisper GitHub asset has no publisher digest; observed hashes are research receipts, not prior independent authenticity verification.'}

def main():
    p=argparse.ArgumentParser();p.add_argument('--root',default='.cache/recognition-research');p.add_argument('--engine',choices=['vosk-small-en-us','whisper-tiny-en']);p.add_argument('--variant',choices=['clean','attenuated','seeded-noise','silence']);args=p.parse_args()
    if args.engine:child(args);return
    root=Path(args.root);receipt=prepare(root);rows=[]
    for engine in ['vosk-small-en-us','whisper-tiny-en']:
        for variant in ['clean','attenuated','seeded-noise','silence']:
            output=subprocess.check_output([sys.executable,__file__,'--root',str(root),'--engine',engine,'--variant',variant],text=True,timeout=180)
            rows.append(json.loads(output.strip().splitlines()[-1]))
    report={'schema':1,'scope':'Single public JFK speech fixture; desktop Linux CPU 2 requested threads, not Android performance, held-out corpus accuracy, or training. Lexical WER ignores punctuation. Silence edits count hallucinated words. No product-model replacement authorized by this benchmark.','reference':REFERENCE,'receipt':receipt,'results':rows}
    Path('recognition-research.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
if __name__=='__main__':main()

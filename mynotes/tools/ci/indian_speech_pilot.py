"""Bounded, public-data acoustic adaptation experiment. Never packaged into the app.
CMU ARCTIC attribution/conditions travel with the modified decoder checkpoint.
No private audio, gated datasets, credentials, paid APIs, or accuracy adoption claim.
"""
import argparse, base64, hashlib, json, math, os, platform, random, re, tarfile, time, urllib.request, wave
from pathlib import Path

# SHA256 values independently published by pytorch/audio at this exact revision.
HASH_SOURCE='https://github.com/pytorch/audio/blob/9b19155e6fce04a83704eadc85d3dfe99e520c0c/src/torchaudio/datasets/cmuarctic.py'
ARCHIVES={'ksp':'8029cafce8296f9bed3022c44ef1e7953332b6bf6943c14b929f468122532717','slt':'7c173297916acf3cc7fcab2713be4c60b27312316765a90934651d367226b4ea'}
SEED=1729

def sha(data):return hashlib.sha256(data).hexdigest()
def write(path,value):path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n')
def normalize(text):return re.sub(r"[^a-z0-9']+",' ',text.lower()).strip().split()
def distance(a,b):
    row=list(range(len(b)+1))
    for i,x in enumerate(a,1):
        nxt=[i]
        for j,y in enumerate(b,1):nxt.append(min(nxt[-1]+1,row[j]+1,row[j-1]+(x!=y)))
        row=nxt
    return row[-1]
def splits():
    # Different speakers AND different utterance IDs/text across train and evaluation.
    return {'train':[('ksp',f'arctic_a{i:04d}') for i in range(1,33)],'evaluation':[(speaker,f'arctic_b{i:04d}') for speaker in ('slt',) for i in range(1,17)]}

def download(url,path,limit):
    request=urllib.request.Request(url,headers={'User-Agent':'MyNotes-licensed-public-research/1'})
    with urllib.request.urlopen(request,timeout=45) as response,open(path,'wb') as out:
        total=0
        while True:
            chunk=response.read(65536)
            if not chunk:break
            total+=len(chunk)
            if total>limit:raise ValueError('Public download exceeds bound')
            out.write(chunk)
    return path.read_bytes() if total<2*1024*1024 else None

def primary(relative,path,limit=1000000):
    errors=[]
    for host in ('https://festvox.org/','http://festvox.org/'):
        try:
            data=download(host+relative,path,limit)
            return data.decode('utf-8',errors='replace'),host+relative
        except Exception as error:errors.append(type(error).__name__+': '+str(error)[:120])
    raise RuntimeError('Primary corpus source unavailable ('+relative+'): '+'; '.join(errors))

def prepare(output):
    output.mkdir(parents=True,exist_ok=True);data=output/'data';data.mkdir(exist_ok=True);licenses=output/'licenses';licenses.mkdir(exist_ok=True)
    roster,roster_url=primary('cmu_arctic/',licenses/'corpus-roster.html')
    # Require the primary corpus page to identify the training speaker as Indian English.
    rows=re.findall(r'<(?:tr|li)\b[^>]*>(.*?)</(?:tr|li)>',roster,flags=re.S|re.I)
    ksp_rows=[re.sub('<[^>]+>',' ',row) for row in rows if re.search(r'\bksp\b',row,re.I)]
    if not any(re.search('Indian',row,re.I) for row in ksp_rows):
        metadata,_=primary('cmu_arctic/cmu_arctic/cmu_us_ksp_arctic/README',licenses/'ksp-README.txt')
        if not re.search(r'Indian(?:[ -]English)?',metadata,re.I):raise ValueError('Cannot verify Indian-English training speaker in primary sources; no optimization permitted')
        ksp_rows=[metadata]
    report={'schema':1,'scope':'small read-speech research pilot; not an app model or general Indian dictation qualification','hash_source':HASH_SOURCE,'roster_url':roster_url,'roster_sha256':sha(roster.encode()),'training_speaker_primary_roster_rows':ksp_rows,'archives':{},'records':[]}
    requested=splits()
    for speaker,expected in ARCHIVES.items():
        print('Preparing licensed speaker '+speaker,flush=True)
        grant=None;url='http://festvox.org/cmu_arctic/cmu_arctic/cmu_us_'+speaker+'_arctic/COPYING'
        try:grant,url=primary(f'cmu_arctic/cmu_arctic/cmu_us_{speaker}_arctic/COPYING',licenses/f'{speaker}-web-COPYING.txt')
        except RuntimeError as unavailable:print(str(unavailable)+'; inspect checksum-pinned primary archive COPYING instead',flush=True)
        archive=data/f'{speaker}.tar.bz2';errors=[]
        if not archive.is_file() or sha(archive.read_bytes())!=expected:
            for host in ('https://festvox.org/','http://festvox.org/'):
                try:
                    archive_url=host+f'cmu_arctic/packed/cmu_us_{speaker}_arctic.tar.bz2';download(archive_url,archive,400*1024*1024)
                    if sha(archive.read_bytes())!=expected:raise ValueError('Corpus archive checksum mismatch')
                    break
                except Exception as error:errors.append(type(error).__name__+': '+str(error)[:150])
            else:raise RuntimeError('Public corpus archive unavailable: '+'; '.join(errors))
        with tarfile.open(archive,'r:bz2') as bundle:
            members=bundle.getmembers()
            if len(members)>50000:raise ValueError('Archive inventory exceeds bound')
            by_suffix={}
            for member in members:
                if member.isfile():
                    name=member.name
                    while name.startswith('./'):name=name[2:]
                    by_suffix[name.split('/',1)[-1]]=member
            copyright_member=by_suffix.get('COPYING')
            if copyright_member is not None and copyright_member.size>50000:raise ValueError('Archive COPYING exceeds bound')
            if copyright_member is None and grant is None:raise ValueError('Neither source archive nor primary web page provides a license for '+speaker)
            archive_grant=grant if copyright_member is None else bundle.extractfile(copyright_member).read().decode('utf-8')
            if not all(term in archive_grant.lower() for term in ('free for use for any purpose','commercial or otherwise','modifications must be clearly marked','original authors')):raise ValueError('Archive source grant is absent or changed; no training permitted')
            (licenses/f'{speaker}-COPYING.txt').write_text(archive_grant)
            report['archives'][speaker]={'sha256':expected,'primary_web_license_url':url,'web_license_retrieved':grant is not None,'license_source':'original primary web COPYING' if copyright_member is None else 'COPYING inside checksum-pinned CMU source archive','license_sha256':sha(archive_grant.encode())}
            metadata_member=by_suffix.get('README')
            if metadata_member is not None and metadata_member.size<100000:
                metadata=bundle.extractfile(metadata_member).read().decode('utf-8',errors='replace');(licenses/f'{speaker}-README.txt').write_text(metadata)
                report['archives'][speaker]['primary_metadata_excerpt']=metadata[:4000]
            text_member=by_suffix['etc/txt.done.data']
            if text_member.size>1000000:raise ValueError('Transcript exceeds bound')
            transcript=bundle.extractfile(text_member).read().decode('utf-8')
            prompts=dict(re.findall(r'\(\s*(arctic_[ab]\d+)\s+"(.*?)"\s*\)',transcript))
            for split,identities in requested.items():
                for selected_speaker,identity in identities:
                    if selected_speaker!=speaker:continue
                    member=by_suffix[f'wav/{identity}.wav']
                    if member.size>2*1024*1024:raise ValueError('Audio fixture exceeds bound')
                    raw=bundle.extractfile(member).read();path=data/f'{speaker}-{identity}.wav';path.write_bytes(raw)
                    with wave.open(str(path)) as wav:
                        if wav.getframerate()!=16000 or wav.getnchannels()!=1 or wav.getsampwidth()!=2 or wav.getcomptype()!='NONE':raise ValueError('Unsupported PCM source; no implicit resampling')
                        seconds=wav.getnframes()/16000
                    if not 0<seconds<=15:raise ValueError('Audio fixture outside bounded duration')
                    report['records'].append({'split':split,'speaker':speaker,'id':identity,'text':prompts[identity],'audio_sha256':sha(raw),'seconds':seconds,'file':str(path.relative_to(output))})
    train_text={' '.join(normalize(row['text'])) for row in report['records'] if row['split']=='train'}
    if any(' '.join(normalize(row['text'])) in train_text for row in report['records'] if row['split']=='evaluation'):raise ValueError('Transcript leakage across splits')
    report['speaker_disjoint']=True;report['transcript_disjoint']=True;report['evaluation_accent_limit']='SLT is a US-English out-of-training-speaker regression control. No unseen Indian speaker is evaluated: do not claim Indian-accent WER or qualification. This tiny corpus is not spontaneous phone dictation.'
    license_metadata=json.loads(urllib.request.urlopen('https://api.github.com/repos/openai/whisper/contents/LICENSE?ref=31243bad24cc746f07d4c8bfdd2d974872cb1803',timeout=30).read())
    model_license=base64.b64decode(license_metadata['content']);(licenses/'Whisper-MIT-LICENSE.txt').write_bytes(model_license)
    if b'MIT License' not in model_license:raise ValueError('Whisper model/code license not verified')
    report['whisper_license_sha256']=sha(model_license);write(output/'data-report.json',report)
    print(json.dumps({'prepared_training_clips':32,'heldout_clips':16,'speaker_disjoint':True,'actual_optimization':False}))

def train(output):
    import importlib.metadata
    import numpy as np
    import torch
    import whisper
    started=time.time();random.seed(SEED);np.random.seed(SEED);torch.manual_seed(SEED);torch.set_num_threads(2);torch.set_num_interop_threads(1);torch.use_deterministic_algorithms(True)
    corpus=json.loads((output/'data-report.json').read_text());records=corpus['records'];model=whisper.load_model('tiny.en',device='cpu',download_root=str(output/'data'/'models'))
    base_path=output/'data'/'models'/'tiny.en.pt';base_sha=sha(base_path.read_bytes());base_url=whisper._MODELS['tiny.en'];assert base_sha==base_url.split('/')[-2]
    for name,p in model.named_parameters():p.requires_grad_(name.startswith('decoder.blocks.3.'))
    parameters={name:p for name,p in model.named_parameters() if p.requires_grad};before={name:p.detach().clone() for name,p in parameters.items()}
    # The frozen encoder's exact features are shared by baseline, training, and final evaluation.
    features={}
    model.eval()
    with torch.no_grad():
        for record in records:
            with wave.open(str(output/record['file'])) as wav:audio=np.frombuffer(wav.readframes(wav.getnframes()),dtype='<i2').astype(np.float32)/32768
            mel=whisper.log_mel_spectrogram(whisper.pad_or_trim(audio),n_mels=model.dims.n_mels)
            features[(record['speaker'],record['id'])]=model.encoder(mel.unsqueeze(0)).detach()
    options=whisper.DecodingOptions(language='en',task='transcribe',fp16=False,without_timestamps=True,temperature=0,sample_len=80)
    def evaluate():
        model.eval();rows=[]
        with torch.no_grad():
            for record in records:
                if record['split']!='evaluation':continue
                began=time.monotonic();decoded=whisper.decode(model,features[(record['speaker'],record['id'])][0],options).text;reference=normalize(record['text']);actual=normalize(decoded)
                rows.append({'speaker':record['speaker'],'id':record['id'],'reference':record['text'],'hypothesis':decoded,'reference_words':len(reference),'word_edits':distance(reference,actual),'seconds':time.monotonic()-began})
        return {'cases':rows,'word_edits':sum(row['word_edits'] for row in rows),'reference_words':sum(row['reference_words'] for row in rows)}
    baseline=evaluate();tokenizer=whisper.tokenizer.get_tokenizer(multilingual=False,language='en',task='transcribe');optimizer=torch.optim.AdamW(list(parameters.values()),lr=0.00003,weight_decay=0.01);losses=[];training=[row for row in records if row['split']=='train'];random.shuffle(training)
    model.train()
    for step,record in enumerate(training,1):
        prefix=list(tokenizer.sot_sequence_including_notimestamps);tokens=torch.tensor([prefix+tokenizer.encode(' '+record['text'])+[tokenizer.eot]],dtype=torch.long)
        targets=tokens[:,1:].clone();targets[:,:len(prefix)-1]=-100
        logits=model.decoder(tokens[:,:-1],features[(record['speaker'],record['id'])]);loss=torch.nn.functional.cross_entropy(logits.reshape(-1,logits.shape[-1]),targets.reshape(-1))
        if not torch.isfinite(loss):raise ValueError('Non-finite training loss; do not adopt')
        optimizer.zero_grad(set_to_none=True);loss.backward();torch.nn.utils.clip_grad_norm_(list(parameters.values()),1);optimizer.step();losses.append(float(loss.detach()));print(json.dumps({'optimizer_step':step,'speaker':record['speaker'],'id':record['id'],'loss':losses[-1]}),flush=True)
    changed=sum(int(torch.count_nonzero(parameters[name].detach()!=value)) for name,value in before.items());assert changed>0
    final=evaluate();checkpoint=output/'experimental-decoder-last-block.pt';torch.save({name:p.detach().cpu() for name,p in parameters.items()},checkpoint)
    report={'schema':1,'source_revision':os.environ.get('GITHUB_SHA'),'scope':'actual limited speech-conditioned decoder optimization on one licensed Indian-English read-speech speaker; quarantined, NOT app adoption','seed':SEED,'base_model':'OpenAI Whisper tiny.en','base_url':base_url,'base_sha256':base_sha,'checkpoint_sha256':sha(checkpoint.read_bytes()),'checkpoint_bytes':checkpoint.stat().st_size,'modified_parameters':changed,'trainable_parameter_count':sum(p.numel() for p in parameters.values()),'trainable_names':list(parameters),'optimizer':'AdamW','learning_rate':0.00003,'weight_decay':0.01,'gradient_norm_cap':1,'optimizer_steps':len(losses),'losses':losses,'training_speakers':['ksp'],'evaluation_speakers':['slt'],'training_clips':len(training),'training_seconds':sum(row['seconds'] for row in training),'data_report_sha256':sha((output/'data-report.json').read_bytes()),'baseline':baseline,'adapted':final,'normalization':'lowercase; retain ASCII alphanumerics/apostrophes; collapse all other characters to word boundaries; report original hypotheses too','decoder':'greedy; English; no timestamps; maximum 80 tokens; identical baseline/adapted','hardware':{'platform':platform.platform(),'cpu_count':os.cpu_count(),'torch_threads':2,'torch':torch.__version__,'whisper':importlib.metadata.version('openai-whisper')},'elapsed_seconds':time.time()-started,'started_unix':started,'finished_unix':time.time(),'adopted':False,'adoption_blockers':['Only 32 read-speech training clips from one Indian-English speaker','No unseen Indian speaker evaluated; SLT is a US regression control, not an Indian-accent benchmark','Only 16 evaluation utterances; no spontaneous dictation, Hindi/Kannada code-switch, noise, or silence coverage','This compares Whisper before/after, not the production Vosk baseline','Experimental Whisper decoder is not compatible with the existing Vosk app engine','No Android runtime, battery, memory, latency, packaging or physical phone qualification']}
    for result in (report['baseline'],report['adapted']):result['wer']=result['word_edits']/result['reference_words']
    report['wer_delta']=report['adapted']['wer']-report['baseline']['wer']
    if report['wer_delta']>0:report['adoption_blockers'].append('Measured US held-out regression: adapted WER exceeds baseline')
    write(output/'training-report.json',report);(output/'MODIFICATIONS.txt').write_text('Research-only modification: 32 AdamW updates to Whisper tiny.en final decoder block, trained on CMU ARCTIC KSP public recordings. Original authors and licenses are retained in licenses/. Frozen base model is required; its SHA256 is in training-report.json. This checkpoint is NOT shipped in MyNotes or accepted as an accuracy improvement.\n')
    print(json.dumps({key:report[key] for key in ('optimizer_steps','modified_parameters','training_seconds','elapsed_seconds','adopted')}),flush=True)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('mode',choices=['prepare','train']);parser.add_argument('--output',type=Path,required=True);args=parser.parse_args();(prepare if args.mode=='prepare' else train)(args.output)

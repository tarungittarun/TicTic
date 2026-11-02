"""One-voice, expanded public-data acoustic/decoder research, NOT an app model.
Same original verified CMU grants as the pilot. KSP evaluation is prompt-heldout,
NOT speaker-heldout. No claim of general Indian dictation, physical acceptance or adoption.
"""
import argparse,hashlib,json,os,platform,random,time,wave
from pathlib import Path
from mynotes.tools.ci import indian_speech_pilot as pilot

def splits():
 return {'train':[('ksp',f'arctic_a{i:04d}') for i in range(1,594)],'evaluation':[(speaker,f'arctic_b{i:04d}') for speaker in ('ksp','slt') for i in range(1,33)]}
def selected_parameter(name):return name.startswith(('encoder.blocks.3.','decoder.blocks.3.'))
def train(output):
 import importlib.metadata
 import numpy as np
 import torch,whisper
 started=time.time();random.seed(pilot.SEED);np.random.seed(pilot.SEED);torch.manual_seed(pilot.SEED);torch.set_num_threads(2);torch.set_num_interop_threads(1);torch.use_deterministic_algorithms(True)
 corpus=json.loads((output/'data-report.json').read_text());records=corpus['records'];model=whisper.load_model('tiny.en',device='cpu',download_root=str(output/'data/models'))
 base=output/'data/models/tiny.en.pt';base_url=whisper._MODELS['tiny.en'];assert pilot.sha(base.read_bytes())==base_url.split('/')[-2]
 for name,p in model.named_parameters():p.requires_grad_(selected_parameter(name))
 parameters={name:p for name,p in model.named_parameters() if p.requires_grad};before={name:p.detach().clone() for name,p in parameters.items()}
 def frozen_checksum():
  digest=hashlib.sha256()
  for name,tensor in sorted(model.state_dict().items()):
   if name in parameters:continue
   digest.update((name+str(tensor.dtype)+str(tuple(tensor.shape))).encode());digest.update(tensor.detach().contiguous().numpy().tobytes())
  return digest.hexdigest()
 frozen_before=frozen_checksum();mels={}
 for record in records:
  with wave.open(str(output/record['file'])) as wav:audio=np.frombuffer(wav.readframes(wav.getnframes()),dtype='<i2').astype(np.float32)/32768
  mels[(record['speaker'],record['id'])]=whisper.log_mel_spectrogram(whisper.pad_or_trim(audio),n_mels=model.dims.n_mels).unsqueeze(0)
 options=whisper.DecodingOptions(language='en',task='transcribe',fp16=False,without_timestamps=True,temperature=0,sample_len=80)
 def evaluate(speaker):
  model.eval();rows=[]
  with torch.no_grad():
   for record in records:
    if record['split']!='evaluation' or record['speaker']!=speaker:continue
    began=time.monotonic()
    # Recompute with CURRENT encoder. Reusing frozen pretraining features would invalidate acoustic evaluation.
    audio=model.encoder(mels[(record['speaker'],record['id'])]);text=whisper.decode(model,audio[0],options).text;reference=pilot.normalize(record['text']);actual=pilot.normalize(text)
    rows.append({'speaker':speaker,'id':record['id'],'reference':record['text'],'hypothesis':text,'reference_words':len(reference),'word_edits':pilot.distance(reference,actual),'seconds':time.monotonic()-began})
  edits=sum(row['word_edits'] for row in rows);words=sum(row['reference_words'] for row in rows)
  return {'cases':rows,'word_edits':edits,'reference_words':words,'wer':edits/words}
 baseline={speaker:evaluate(speaker) for speaker in ('ksp','slt')};tokenizer=whisper.tokenizer.get_tokenizer(multilingual=False,language='en',task='transcribe');optimizer=torch.optim.AdamW(list(parameters.values()),lr=0.00003,weight_decay=0.01)
 training=[row for row in records if row['split']=='train'];assert len(training)==593;random.shuffle(training);losses=[];model.train()
 for step,record in enumerate(training,1):
  prefix=list(tokenizer.sot_sequence_including_notimestamps);tokens=torch.tensor([prefix+tokenizer.encode(' '+record['text'])+[tokenizer.eot]],dtype=torch.long);targets=tokens[:,1:].clone();targets[:,:len(prefix)-1]=-100
  optimizer.zero_grad(set_to_none=True)
  # Intentionally NOT torch.no_grad: the final acoustic encoder block learns from speech.
  audio=model.encoder(mels[(record['speaker'],record['id'])]);logits=model.decoder(tokens[:,:-1],audio);loss=torch.nn.functional.cross_entropy(logits.reshape(-1,logits.shape[-1]),targets.reshape(-1))
  if not torch.isfinite(loss):raise ValueError('Nonfinite loss: reject experiment')
  loss.backward();torch.nn.utils.clip_grad_norm_(list(parameters.values()),1);optimizer.step();losses.append(float(loss.detach()))
  if step==1 or step%25==0 or step==len(training):print(json.dumps({'optimizer_step':step,'utterance':record['id'],'loss':losses[-1]}),flush=True)
 modified={component:sum(int(torch.count_nonzero(parameters[name].detach()!=value)) for name,value in before.items() if name.startswith(component+'.')) for component in ('encoder','decoder')}
 assert all(count>0 for count in modified.values()),modified
 frozen_after=frozen_checksum();assert frozen_after==frozen_before,'Frozen weights changed: reject experiment'
 adapted={speaker:evaluate(speaker) for speaker in ('ksp','slt')};checkpoint=output/'experimental-acoustic-decoder-blocks.pt';torch.save({name:p.detach().cpu() for name,p in parameters.items()},checkpoint)
 report={'schema':1,'source_revision':os.environ.get('GITHUB_SHA'),'scope':'593 one-pass updates on a single Indian read-speech voice; final acoustic encoder AND final decoder block only; no app adoption','base_model':'OpenAI Whisper tiny.en','base_url':base_url,'base_sha256':pilot.sha(base.read_bytes()),'seed':pilot.SEED,'optimizer':'AdamW','learning_rate':0.00003,'weight_decay':0.01,'gradient_norm_cap':1,'optimizer_steps':len(losses),'losses':losses,'trainable_parameter_count':sum(p.numel() for p in parameters.values()),'trainable_names':list(parameters),'modified_parameters_by_component':modified,'frozen_weights_sha256_before':frozen_before,'frozen_weights_sha256_after':frozen_after,'training_clips':len(training),'training_seconds':sum(row['seconds'] for row in training),'unique_training_speakers':['ksp'],'evaluation_protocol':{'ksp':'Indian-English, SAME training speaker, different prompts; NOT unseen-speaker Indian performance','slt':'US-English, different speaker and prompts; regression control; shares heldout prompts with KSP for descriptive paired comparison'},'baseline':baseline,'adapted':adapted,'normalization':'lowercase ASCII alphanumeric/apostrophe tokens; original hypotheses retained; edit distance / total reference words','decoder':'identical baseline/adapted greedy English no timestamps maximum 80 tokens','checkpoint_sha256':pilot.sha(checkpoint.read_bytes()),'checkpoint_bytes':checkpoint.stat().st_size,'data_report_sha256':pilot.sha((output/'data-report.json').read_bytes()),'hardware':{'platform':platform.platform(),'cpu_count':os.cpu_count(),'torch_threads':2,'torch':torch.__version__,'whisper':importlib.metadata.version('openai-whisper')},'started_unix':started,'finished_unix':time.time(),'elapsed_seconds':time.time()-started,'adopted':False,'adoption_blockers':['Only one Indian read-speech training speaker; no unseen Indian speaker test','Seen-speaker prompt-heldout results are not general Indian dictation accuracy','No production Vosk baseline comparison, spontaneous/code-switch/noise/silence coverage','No compatible qualified Android Whisper runtime, resource/battery benchmark or physical-phone acceptance','Any observed group regression is retained and blocks adoption; no threshold is retrofitted']}
 for speaker in ('ksp','slt'):report.setdefault('wer_delta',{})[speaker]=adapted[speaker]['wer']-baseline[speaker]['wer']
 pilot.write(output/'training-report.json',report)
 (output/'MODIFICATIONS.txt').write_text('Research-only modified final acoustic encoder and final decoder blocks of Whisper tiny.en, after 593 AdamW updates on CMU ARCTIC KSP A prompts. Frozen base required; exact hashes, unchanged-frozen-weight proof and original authors/licenses retained. KSP B evaluation is the SAME Indian speaker, not unseen-speaker qualification. No weights shipped in MyNotes.\n')
 print('::notice title=Expanded real acoustic training, not app adoption::'+json.dumps({key:report[key] for key in ['optimizer_steps','modified_parameters_by_component','training_seconds','elapsed_seconds','wer_delta','adopted']}),flush=True)
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('mode',choices=['prepare','train']);parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
 if args.mode=='prepare':
  pilot.prepare(args.output,splits());path=args.output/'data-report.json';data=json.loads(path.read_text());data['scope']='Expanded 593-clip single-voice training; paired KSP SAME-speaker and SLT US prompt-heldout controls, not general Indian dictation';pilot.write(path,data)
 else:train(args.output)

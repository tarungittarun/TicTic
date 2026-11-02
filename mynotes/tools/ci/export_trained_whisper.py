"""Export the actual licensed 593-step checkpoint for offline Android CPU inference.
Runs in Actions, never on user recordings. Refuses wrong base/checkpoint bytes or keys.
"""
import argparse,hashlib,json,subprocess,sys
from pathlib import Path

BASE_SHA='d3dd57d32accea0b295c96e26691aa14d8822fac7d9d27d5dc00b4ca2826dd03'
BLOCK_SHA='84dcc55f0c1bcdc53e62d90a50c4dbe284669222f13ecbcbf458d62af42bc338'
CPP_REV='4979e04f5dcaccb36057e059bbaed8a2f5288315'

def digest(path):
 with Path(path).open('rb') as source:return hashlib.file_digest(source,'sha256').hexdigest()

def main():
 import torch,whisper
 p=argparse.ArgumentParser();p.add_argument('--checkpoint',type=Path,required=True);p.add_argument('--cpp',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
 assert digest(a.checkpoint)==BLOCK_SHA,'Wrong trained checkpoint'
 a.output.mkdir(parents=True,exist_ok=True)
 # Official MIT base downloader also verifies the embedded upstream digest.
 whisper.load_model('tiny.en',device='cpu',download_root=str(a.output/'base'))
 base=a.output/'base/tiny.en.pt';assert digest(base)==BASE_SHA
 state=torch.load(base,map_location='cpu',weights_only=True)
 blocks=torch.load(a.checkpoint,map_location='cpu',weights_only=True)
 expected={name for name in state['model_state_dict'] if name.startswith(('encoder.blocks.3.','decoder.blocks.3.'))}
 assert set(blocks)==expected,'Incomplete/unexpected trained tensors'
 for name,value in blocks.items():
  assert state['model_state_dict'][name].shape==value.shape and torch.isfinite(value).all()
  state['model_state_dict'][name]=value
 merged=a.output/'merged.pt';torch.save(state,merged)
 subprocess.run([sys.executable,str(a.cpp/'models/convert-pt-to-ggml.py'),str(merged),str(Path(whisper.__file__).resolve().parent.parent),str(a.output)],check=True)
 model=a.output/'ggml-model.bin'
 report=dict(schema=1,engine='Whisper tiny.en, experimental Indian-English adaptation',training_run=41393453222,optimizer_steps=593,training_speakers=1,checkpoint_sha256=BLOCK_SHA,base_sha256=BASE_SHA,whisper_cpp_revision=CPP_REV,filename='trained-whisper.bin',bytes=model.stat().st_size,sha256=digest(model),offline=True,default_engine=False,physical_acceptance=False,limitations=['English only','One Indian training speaker; no unseen Indian-speaker qualification','Experimental opt-in; existing Vosk remains available','Review recognized text; audio is never replaced'])
 model.rename(a.output/'trained-whisper.bin');(a.output/'trained-whisper.json').write_text(json.dumps(report,indent=2)+'\n')
 print(json.dumps(report,indent=2))
if __name__=='__main__':main()

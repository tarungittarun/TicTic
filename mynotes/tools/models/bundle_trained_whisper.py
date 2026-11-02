"""Validate Actions-exported trained model, copy only runtime bytes/notices (never PT)."""
import argparse,hashlib,json,shutil
from pathlib import Path

def bundle(source,output):
 manifests=list(source.rglob('trained-whisper.json'))
 if len(manifests)!=1:raise ValueError('Exactly one exported manifest required')
 manifest=manifests[0];data=json.loads(manifest.read_text());model=manifest.with_name('trained-whisper.bin')
 if data['training_run']!=41393453222 or data['optimizer_steps']!=593 or data['default_engine'] is not False or data['base_sha256']!='d3dd57d32accea0b295c96e26691aa14d8822fac7d9d27d5dc00b4ca2826dd03' or data['checkpoint_sha256']!='84dcc55f0c1bcdc53e62d90a50c4dbe284669222f13ecbcbf458d62af42bc338':raise ValueError('Unexpected training provenance')
 with model.open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
 if model.stat().st_size!=data['bytes'] or digest!=data['sha256']:raise ValueError('Exported trained model checksum mismatch')
 from mynotes.tools.models.whisper_ggml import validate
 validate(model)
 voice=output/'voice';voice.mkdir(parents=True,exist_ok=True)
 shutil.copyfile(model,voice/'trained-whisper.bin');shutil.copyfile(manifest,voice/'trained-whisper.json')
 notices=manifest.parent/'licenses'
 if not notices.is_dir() or not any(notices.iterdir()):raise ValueError('Original notices missing')
 licenses=output/'licenses';licenses.mkdir(exist_ok=True)
 for file in notices.iterdir():
  if file.is_file():shutil.copyfile(file,licenses/('trained-whisper-'+file.name))
 print(json.dumps(data,indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();bundle(a.source,a.output)

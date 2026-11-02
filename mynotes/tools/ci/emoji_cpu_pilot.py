"""Nonpublishing, public-data-only CPU feasibility experiment, never a runtime model.
Twemoji training; a small *separate art style* OpenMoji evaluation. Not universal OCR.
"""
import argparse
import base64
import hashlib
import io
import json
import math
import os
from pathlib import Path
import random
import time
import urllib.request
import zipfile
from .emoji_asset_audit import COMMIT, TREE, normalized
from .emoji_charset_audit import EMOJI_BLOB, emoji_sequences

ARCHIVE_SHA = 'd91bdab1e9ac66f2935425e539f5ac1cd83a491fa27b2021c77630a0ba8e09ff'
OPENMOJI_COMMIT = '626bdc684b85738ebdba73ad83c499c2eb88c5dd'


def blob_sha(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()


def fetch(url, limit=24*1024*1024):
    headers={'User-Agent':'MyNotes-public-emoji-feasibility'}
    if url.startswith('https://api.github.com/') and os.environ.get('GH_TOKEN'):
        headers['Authorization']='Bearer '+os.environ['GH_TOKEN']
    with urllib.request.urlopen(urllib.request.Request(url,headers=headers),timeout=45) as response:
        data=response.read(limit+1)
    if len(data)>limit:raise ValueError('Public input exceeds budget')
    return data


def api(path):return json.loads(fetch('https://api.github.com/repos/'+path))


def blob(repo, sha):
    data=base64.b64decode(api(repo+'/git/blobs/'+sha)['content'])
    if blob_sha(data)!=sha:raise ValueError('Git blob mismatch')
    return data


def png_entries(tree, prefix):
    if tree.get('truncated') is not False:raise ValueError('Incomplete asset tree')
    result={}
    for item in tree['tree']:
        path=item['path']
        if item['type']!='blob' or not path.startswith(prefix) or not path.endswith('.png'):continue
        key=normalized(int(value,16) for value in Path(path).stem.split('-'))
        if key in result:raise ValueError('Ambiguous normalized asset filename')
        result[key]=item
    return result


def run(output, epochs=3):
    import torch
    from torch import nn
    from PIL import Image, ImageDraw, ImageFont
    torch.set_num_threads(2);torch.manual_seed(1337);random.seed(1337)
    torch.use_deterministic_algorithms(True)
    out=Path(output);out.mkdir(parents=True,exist_ok=True);licenses=out/'licenses';licenses.mkdir(exist_ok=True);(licenses/'ATTRIBUTION.txt').write_text('Training graphics: Twemoji, Twitter and contributors / jdecked maintainers. CC-BY-4.0. https://github.com/jdecked/twemoji/tree/'+COMMIT+'\nEvaluation graphics only: OpenMoji, HfG Gmuend and contributors. CC-BY-SA-4.0. https://github.com/hfg-gmuend/openmoji/tree/'+OPENMOJI_COMMIT+'\nTransformed locally for a nonpublishing cropped-glyph experiment. No evaluation pixels used for fitting. Full licenses attached.\n')
    unicode_data=blob('unicode-org/unicodetools',EMOJI_BLOB)
    sequences=sorted(emoji_sequences(unicode_data));assert len(sequences)==3944
    tree=api('jdecked/twemoji/git/trees/'+TREE+'?recursive=1');assert tree['sha']==TREE
    assets=png_entries(tree,'assets/72x72/')
    archive=fetch('https://codeload.github.com/jdecked/twemoji/zip/'+COMMIT)
    if hashlib.sha256(archive).hexdigest()!=ARCHIVE_SHA:raise ValueError('Archive identity mismatch')
    prefix='twemoji-'+COMMIT+'/'
    grouped={}
    with zipfile.ZipFile(io.BytesIO(archive)) as source:
        license_data=source.read(prefix+'LICENSE-GRAPHICS')
        assert blob_sha(license_data)=='dc8853a70cfa9d7e8933c080caa147815c659fab'
        (licenses/'twemoji-CC-BY-4.0.txt').write_bytes(license_data)
        for sequence in sequences:
            item=assets[normalized(map(ord,sequence))];info=source.getinfo(prefix+item['path'])
            if info.file_size>100000:raise ValueError('PNG budget exceeded')
            data=source.read(info)
            if blob_sha(data)!=item['sha']:raise ValueError('Training PNG identity mismatch')
            group=grouped.setdefault(item['sha'],{'sequences':[],'data':data})
            group['sequences'].append(sequence)
    groups=sorted(grouped.values(),key=lambda value:value['sequences'][0]);assert len(groups)==3941;labels=[value['sequences'] for value in groups]
    label_index={normalized(map(ord,text)):i for i,aliases in enumerate(labels) for text in aliases}
    Image.MAX_IMAGE_PIXELS=100000
    def image(data):
        with Image.open(io.BytesIO(data)) as value:
            if value.format!='PNG' or value.size!=(72,72):raise ValueError('Unexpected public PNG dimensions')
            value.load();return value.convert('RGBA')
    images=[image(group['data']) for group in groups]
    def tensor(value,augment=False):
        background=random.choice([(255,255,255),(24,27,31),(235,235,235)]) if augment else (255,255,255)
        canvas=Image.new('RGBA',(72,72),background+(255,));canvas.alpha_composite(value)
        if augment:
            edge=random.randint(52,68);glyph=canvas.resize((edge,edge),Image.Resampling.BILINEAR);canvas=Image.new('RGBA',(72,72),background+(255,));canvas.alpha_composite(glyph,(random.randint(0,72-edge),random.randint(0,72-edge)))
        rgb=canvas.convert('RGB').resize((64,64),Image.Resampling.BILINEAR)
        data=torch.frombuffer(bytearray(rgb.tobytes()),dtype=torch.uint8).reshape(64,64,3).permute(2,0,1).float()/255
        if augment:data=(data+torch.randn_like(data)*random.uniform(0,.025)).clamp(0,1)
        return data
    class Data(torch.utils.data.Dataset):
        def __len__(self):return len(images)*4
        def __getitem__(self,index):index%=len(images);return tensor(images[index],True),index
    model=nn.Sequential(nn.Conv2d(3,16,3,2,1),nn.ReLU(),nn.Conv2d(16,32,3,2,1),nn.ReLU(),nn.Conv2d(32,48,3,2,1),nn.ReLU(),nn.Conv2d(48,64,3,2,1),nn.ReLU(),nn.Flatten(),nn.Linear(1024,128),nn.ReLU(),nn.Linear(128,len(groups)))
    initial={key:value.clone() for key,value in model.state_dict().items()}
    optimizer=torch.optim.AdamW(model.parameters(),lr=.002);criterion=nn.CrossEntropyLoss()
    loader=torch.utils.data.DataLoader(Data(),batch_size=64,shuffle=True,num_workers=0,generator=torch.Generator().manual_seed(1337))
    steps=instances=0;epoch_losses=[];started=time.perf_counter()
    for epoch in range(epochs):
        model.train();loss_sum=0
        for inputs,target in loader:
            optimizer.zero_grad(set_to_none=True);loss=criterion(model(inputs),target);assert torch.isfinite(loss).all();loss.backward();optimizer.step();steps+=1;instances+=len(target);loss_sum+=float(loss.detach())*len(target)
        epoch_losses.append(loss_sum/len(loader.dataset));print(json.dumps({'epoch':epoch+1,'steps':steps,'mean_cross_entropy':epoch_losses[-1]}),flush=True)
    elapsed=time.perf_counter()-started;model.eval()
    changed=sum(int((initial[key]!=value).sum()) for key,value in model.state_dict().items());assert changed>0
    def evaluate(values,targets):
        top1=top5=0
        with torch.no_grad():
            for start in range(0,len(values),64):
                guesses=model(torch.stack([tensor(value) for value in values[start:start+64]])).topk(5,dim=1).indices.tolist()
                for target,predictions in zip(targets[start:start+64],guesses):top1+=target==predictions[0];top5+=target in predictions
        return {'examples':len(values),'top1_correct':top1,'top5_correct':top5}
    seen=evaluate(images,list(range(len(groups))))
    # A different art style is never used for fitting, selecting epochs or thresholds.
    other=api('hfg-gmuend/openmoji/git/trees/'+OPENMOJI_COMMIT+'?recursive=1');assert other['sha']==OPENMOJI_COMMIT
    other_assets=png_entries(other,'color/72x72/')
    other_license=blob('hfg-gmuend/openmoji','cc3e2459dc5c46f1936b6e3b54137099063818e0');(licenses/'openmoji-evaluation-CC-BY-SA-4.0.txt').write_bytes(other_license)
    keys=sorted(set(other_assets)&set(label_index),key=lambda key:hashlib.sha256(bytes('-'.join(map(str,key)),'ascii')).hexdigest())[:64]
    held_images=[];held_targets=[];held_identity=[]
    for key in keys:
        item=other_assets[key];held_images.append(image(blob('hfg-gmuend/openmoji',item['sha'])));held_targets.append(label_index[key]);held_identity.append({'path':item['path'],'blob':item['sha'],'target_aliases':labels[label_index[key]]})
    assert len(held_images)==64
    held=evaluate(held_images,held_targets)
    model_file=out/'EXPERIMENTAL-NOT-RUNTIME-emoji-cpu.pt';torch.save(model.state_dict(),model_file)
    (out/'class-aliases.json').write_text(json.dumps(labels,ensure_ascii=False)+'\n')
    report={'schema':1,'scope':'Public-asset cropped-glyph CPU feasibility only. No detection, photos, universal OCR, calibrated confidence, mobile performance or physical acceptance. Model is NOT approved/shipped.',
      'twemoji_commit':COMMIT,'twemoji_tree':TREE,'twemoji_archive_sha256':ARCHIVE_SHA,'training_graphics_license':'CC-BY-4.0; Twemoji contributors, see full attached license',
      'openmoji_evaluation_commit':OPENMOJI_COMMIT,'evaluation_graphics_license':'CC-BY-SA-4.0; OpenMoji, see full attached license; no evaluation pixels used for fitting',
      'unicode_fully_qualified_sequences':len(sequences),'pixel_alias_classes':len(groups),'training_unique_png_blobs':len(images),'training_steps':steps,'sampled_training_instances':instances,'epochs':epochs,'epoch_losses':epoch_losses,'changed_parameters':changed,'trainable_parameters':sum(p.numel() for p in model.parameters()),'training_cpu_seconds':elapsed,
      'seen_style_clean_not_held_out':seen,'separate_art_style_64_only':held,'held_out_asset_identities':held_identity,'weight_file_sha256':hashlib.sha256(model_file.read_bytes()).hexdigest(),'seed':1337,'device':'cpu','threads':2,'runtime_models_changed':False,'release_approval':False}
    (out/'report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps({key:value for key,value in report.items() if key!='held_out_asset_identities'},ensure_ascii=False),flush=True)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',required=True);args=parser.parse_args();run(args.output)

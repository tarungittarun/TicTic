"""Diagnose a failed public pilot, not train/approve a universal emoji recognizer.
Reads only the exact archived checkpoint, then performs small-batch memorization
sanity experiments. No held-out OpenMoji pixels, mobile integration or new weights.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import time
import zipfile
from .emoji_cpu_pilot import ARCHIVE_SHA, COMMIT, fetch, normalized

WEIGHT_SHA = '9863796ea470135cf42ae16c23fb03ccc5c3b2daab9bae90eea4b05c1a75f63e'


def require_identity(pilot):
    """Untrusted serialized tensors are never loaded before exact byte identity."""
    pilot = Path(pilot)
    expected = Path(__file__).resolve().parents[2] / 'docs/emoji-pilot/41457619334'
    for name in ('report.json', 'class-aliases.json'):
        if (pilot / name).read_bytes() != (expected / name).read_bytes():
            raise ValueError('Wrong immutable pilot metadata')
    weights = pilot / 'EXPERIMENTAL-NOT-RUNTIME-emoji-cpu.pt'
    if weights.stat().st_size > 8 * 1024 * 1024:
        raise ValueError('Checkpoint exceeds budget')
    if hashlib.sha256(weights.read_bytes()).hexdigest() != WEIGHT_SHA:
        raise ValueError('Wrong immutable checkpoint bytes')
    labels = json.loads((pilot / 'class-aliases.json').read_text())
    if len(labels) != 3941:
        raise ValueError('Wrong alias class count')
    return weights, labels


def model_for(nn, count, stabilized=False):
    def activation():
        return nn.LeakyReLU(.1) if stabilized else nn.ReLU()
    layers = [nn.Conv2d(3,16,3,2,1),activation(),nn.Conv2d(16,32,3,2,1),activation(),
              nn.Conv2d(32,48,3,2,1),activation(),nn.Conv2d(48,64,3,2,1),activation(),
              nn.Flatten(),nn.Linear(1024,128)]
    if stabilized:
        layers.append(nn.LayerNorm(128))
    layers += [activation(), nn.Linear(128,count)]
    return nn.Sequential(*layers)


def run(pilot, output):
    weights, labels = require_identity(pilot)
    import torch
    from torch import nn
    from PIL import Image
    torch.set_num_threads(2)
    torch.use_deterministic_algorithms(True)
    torch.manual_seed(1337)
    Image.MAX_IMAGE_PIXELS = 100000
    archive = fetch('https://codeload.github.com/jdecked/twemoji/zip/'+COMMIT)
    if hashlib.sha256(archive).hexdigest() != ARCHIVE_SHA:
        raise ValueError('Wrong public archive bytes')
    # Deterministic hash sample, selected without reading inference results.
    indices = sorted(range(len(labels)), key=lambda i: hashlib.sha256(labels[i][0].encode()).digest())[:64]
    inputs, tensor_identities = [], []
    with zipfile.ZipFile(io.BytesIO(archive)) as source:
        members = {}
        for member in source.infolist():
            if not member.filename.startswith('twemoji-'+COMMIT+'/assets/72x72/') or not member.filename.endswith('.png'):
                continue
            key = normalized(int(part,16) for part in Path(member.filename).stem.split('-'))
            if key in members:
                raise ValueError('Ambiguous normalized archive filename')
            members[key] = member
        for index in indices:
            # Normalization is a lookup key, NOT a reversible asset filename.
            # ZWJ assets may retain FE0F in their actual pinned archive path.
            member = members[normalized(map(ord,labels[index][0]))]
            if member.file_size > 100000:
                raise ValueError('Public glyph exceeds budget')
            with Image.open(io.BytesIO(source.read(member))) as image:
                if image.format != 'PNG' or image.size != (72,72):
                    raise ValueError('Unexpected glyph format/size')
                canvas = Image.new('RGBA',(72,72),(255,255,255,255))
                canvas.alpha_composite(image.convert('RGBA'))
                rgb = canvas.convert('RGB').resize((64,64),Image.Resampling.BILINEAR)
                tensor_identities.append(hashlib.sha256(rgb.tobytes()).hexdigest())
                inputs.append(torch.frombuffer(bytearray(rgb.tobytes()),dtype=torch.uint8).reshape(64,64,3).permute(2,0,1).float()/255)
    data = torch.stack(inputs)
    # uint8 -> float32/255 is injective; no optional NumPy dependency is needed.
    unique = len(set(tensor_identities))
    if unique != 64:
        raise ValueError('Sanity sample includes duplicate input tensors')
    baseline = model_for(nn,len(labels))
    baseline.load_state_dict(torch.load(weights,map_location='cpu',weights_only=True),strict=True)
    baseline.eval()
    activation_stats = []
    with torch.no_grad():
        value = data
        for index, layer in enumerate(baseline):
            value = layer(value)
            activation_stats.append({'layer':index,'type':type(layer).__name__,
                'min':float(value.min()),'max':float(value.max()),
                'zero_fraction':float((value==0).float().mean()),
                'mean_feature_std_across_inputs':float(value.std(dim=0,unbiased=False).mean())})
        guesses = value.argmax(dim=1)
        archived_sample = {'examples':64,'distinct_top1_predictions':len(set(guesses.tolist())),
            'correct':int((guesses==torch.tensor(indices)).sum())}
    # Intentionally an overfit sanity check: 16 repeated clean *training* inputs.
    # Neither this nor the 64 archived-input checks are held-out accuracy evidence.
    fits = []
    for stabilized in (False,True):
        torch.manual_seed(1337)
        model = model_for(nn,16,stabilized)
        initial = {key:value.clone() for key,value in model.state_dict().items()}
        optimizer = torch.optim.AdamW(model.parameters(),lr=.0005 if stabilized else .002)
        training = data[:16]*2-1 if stabilized else data[:16]
        targets = torch.arange(16)
        losses = []
        started = time.perf_counter()
        for step in range(300):
            model.train()
            optimizer.zero_grad(set_to_none=True)
            loss = nn.functional.cross_entropy(model(training),targets)
            if not torch.isfinite(loss):
                raise ValueError('Non-finite sanity loss')
            loss.backward()
            optimizer.step()
            if step in (0,49,99,199,299):
                losses.append({'step':step+1,'loss_before_update':float(loss.detach())})
        seconds = time.perf_counter()-started
        model.eval()
        with torch.no_grad():
            logits = model(training)
            correct = int((logits.argmax(dim=1)==targets).sum())
        fits.append({'variant':'centered-input-leaky-relu-layernorm-lr0.0005' if stabilized else 'original-architecture-lr0.002',
            'scope':'16 clean repeated TRAINING examples; NOT held-out accuracy or universal OCR',
            'steps':300,'sampled_training_instances':4800,'examples':16,'training_top1_correct':correct,
            'final_training_loss':float(nn.functional.cross_entropy(logits,targets)),
            'changed_parameters':sum(int((initial[key]!=value).sum()) for key,value in model.state_dict().items()),
            'training_cpu_seconds':seconds,'loss_observations':losses})
    out = Path(output)
    out.mkdir(parents=True,exist_ok=True)
    report = {'schema':1,'source_pilot_run':41457619334,'checkpoint_sha256':WEIGHT_SHA,
        'twemoji_commit':COMMIT,'twemoji_archive_sha256':ARCHIVE_SHA,'sample_class_indices':indices,
        'sample_unique_input_tensors':unique,'archived_seen_style_sample_not_held_out':archived_sample,
        'archived_layer_statistics':activation_stats,'small_training_set_memorization_sanity_only':fits,
        'openmoji_pixels_read':False,'runtime_models_changed':False,'new_weights_saved':False,
        'release_approval':False,'seed':1337,'threads':2,'device':'cpu',
        'scope':'Diagnostic research only. Tests feature collapse and tiny training-set fitting, NOT generalization, detection, photos, calibrated unknowns or mobile/physical acceptance.'}
    (out/'diagnosis.json').write_text(json.dumps(report,indent=2)+'\n')
    # Reuse complete original source notices; no generated glyph pixels are retained.
    import shutil
    shutil.copytree(Path(pilot)/'licenses',out/'licenses',dirs_exist_ok=True)
    print(json.dumps(report),flush=True)


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pilot',required=True)
    parser.add_argument('--output',required=True)
    args=parser.parse_args()
    try:
        run(args.pilot,args.output)
    except Exception as failure:
        message=(type(failure).__name__+': '+str(failure))[:2000]
        print('::error title=Public diagnostic experiment failed::'+message.replace('%','%25').replace('\r','%0D').replace('\n','%0A'),flush=True)
        raise

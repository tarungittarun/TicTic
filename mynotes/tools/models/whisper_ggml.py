"""Bounded structural validation of the unquantized official Whisper GGML stream.
Reject mislabeled tensor payloads before native code (which trusts dimension headers).
"""
import math,struct
from pathlib import Path

def validate(path):
 size=Path(path).stat().st_size
 if not 100<size<200_000_000:raise ValueError('GGML model size outside runtime budget')
 with Path(path).open('rb') as stream:
  def ints(n):
   data=stream.read(4*n)
   if len(data)!=4*n:raise ValueError('Truncated GGML header')
   return struct.unpack('<'+'i'*n,data)
  header=ints(12)
  if header[0]!=0x67676d6c or header[1:11]!=(51864,1500,384,6,4,448,384,6,4,80) or header[11] not in (0,1):raise ValueError('Unexpected tiny.en dimensions/encoding')
  mels,bins=ints(2)
  if (mels,bins)!=(80,201):raise ValueError('Unexpected mel filters')
  stream.seek(mels*bins*4,1)
  vocab,=ints(1)
  if not 50000<=vocab<=51864:raise ValueError('Unexpected vocabulary count')
  for _ in range(vocab):
   length,=ints(1)
   if not 0<=length<=1024 or stream.tell()+length>size:raise ValueError('Invalid token bytes')
   stream.seek(length,1)
  names=set()
  while stream.tell()<size:
   dims,length,dtype=ints(3)
   if not 1<=dims<=4 or not 1<=length<=256 or dtype not in (0,1):raise ValueError('Invalid tensor header (possible float32 bytes labeled float16)')
   shape=ints(dims)
   if any(not 1<=x<=60000 for x in shape):raise ValueError('Invalid tensor dimensions')
   name=stream.read(length).decode('ascii')
   if not name.startswith(('encoder.','decoder.')) or name in names:raise ValueError('Invalid/duplicate tensor name')
   names.add(name);payload=math.prod(shape)*(4 if dtype==0 else 2)
   if stream.tell()+payload>size:raise ValueError('Truncated tensor payload')
   stream.seek(payload,1)
  if len(names)!=167:raise ValueError(f'Incomplete tiny.en tensor set: {len(names)}')
 return len(names)

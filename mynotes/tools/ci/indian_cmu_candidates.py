"""Audit additional primary-source Indian-English voices; zero ML optimization.
Only explicit per-voice/source grants admit candidates. Missing permission stays blocked.
"""
import argparse, hashlib, json, os, re, tarfile
from pathlib import Path
from mynotes.tools.ci.indian_speech_pilot import primary,download,HASH_SOURCE
EXPECTED={'aup':'2c55bc3050caa996758869126ad10cf42e1441212111db034b3a45189c18b6fc','axb':'dd65c3d2907d1ee52f86e44f578319159e60f4bf722a9142be01161d84e330ff','gka':'dc82e7967cbd5eddbed33074b0699128dbd4482b41711916d58103707e38c67f','slp':'54345ed55e45c23d419e9a823eef427f1cc93c83a710735ec667d068c916abf1'}
def permitted(text):return all(value in text.lower() for value in ('free for use for any purpose','commercial or otherwise','modifications must be clearly marked','original authors'))
def digest(path):
 with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()
def main(output):
 output.mkdir(parents=True,exist_ok=True);work=output/'archives';work.mkdir(exist_ok=True);proof=output/'evidence';proof.mkdir(exist_ok=True)
 roster,url=primary('cmu_arctic/',proof/'primary-roster.html');rows=re.findall(r'<(?:tr|li)\b[^>]*>(.*?)</(?:tr|li)>',roster,re.S|re.I);report={'schema':1,'source_sha':os.environ.get('GITHUB_SHA'),'scope':'public primary-source corpus/license audit only; no optimization, accuracy claim or app model replacement','hash_source':HASH_SOURCE,'primary_roster_url':url,'candidates':[]}
 for speaker,expected in EXPECTED.items():
  row={'speaker':speaker,'metadata_rows':[re.sub('<[^>]+>',' ',row) for row in rows if re.search(r'\b'+speaker+r'\b',row,re.I)],'archive_sha256_expected':expected,'web_attempts':[],'explicit_permissive_grant_verified':False,'eligible_for_future_training':False};report['candidates'].append(row)
  try:
   for relative in [f'cmu_arctic/cmu_arctic/cmu_us_{speaker}_arctic/COPYING',f'cmu_arctic/cmu_us_{speaker}_arctic/COPYING']:
    try:
     grant,source=primary(relative,proof/f'{speaker}-web-COPYING.txt');row['web_attempts'].append({'url':source,'retrieved':True})
     if permitted(grant):row.update(explicit_permissive_grant_verified=True,license_source=source,license_sha256=hashlib.sha256(grant.encode()).hexdigest());break
    except Exception as error:row['web_attempts'].append({'path':relative,'error':str(error)[:350]})
   archive=work/f'{speaker}.tar.bz2';errors=[]
   for host in ('https://festvox.org/','http://festvox.org/'):
    try:
     download(host+f'cmu_arctic/packed/cmu_us_{speaker}_arctic.tar.bz2',archive,400*1024*1024)
     if digest(archive)!=expected:raise ValueError('Archive checksum mismatch')
     break
    except Exception as error:errors.append(str(error)[:180])
   else:raise ValueError('Archive unavailable: '+'; '.join(errors))
   row['archive_sha256_verified']=True;row['metadata_files']=[]
   with tarfile.open(archive,'r:bz2') as bundle:
    members=bundle.getmembers()
    if len(members)>50000:raise ValueError('Archive inventory exceeds bound')
    for member in members:
     name=Path(member.name).name.lower()
     if member.isfile() and name.startswith(('readme','copying','license')) and member.size<=64000:
      if len(row['metadata_files'])==20:break
      text=bundle.extractfile(member).read().decode('utf-8',errors='replace');row['metadata_files'].append({'path':member.name,'sha256':hashlib.sha256(text.encode()).hexdigest(),'excerpt':text[:1800]})
      if permitted(text):
       (proof/f'{speaker}-archive-COPYING.txt').write_text(text);row.update(explicit_permissive_grant_verified=True,license_source='Original metadata inside checksum-verified primary source archive: '+member.name,license_sha256=hashlib.sha256(text.encode()).hexdigest())
   row['eligible_for_future_training']=row['explicit_permissive_grant_verified'] and any('indian' in text.lower() for text in row['metadata_rows'])
   row['decision']='eligible for future controlled training/evaluation; no training performed here' if row['eligible_for_future_training'] else 'blocked: no explicit per-voice/source grant verified; public download/secondary citations alone are not permission'
  except Exception as error:row['decision']='blocked: '+type(error).__name__+': '+str(error)[:500]
  print(json.dumps({'speaker':speaker,'decision':row['decision'],'optimization_steps':0}),flush=True)
 (proof/'candidate-report.json').write_text(json.dumps(report,indent=2)+'\n')
 print('::notice title=Additional Indian voice audit::'+json.dumps({'voices':len(report['candidates']),'eligible':sum(row['eligible_for_future_training'] for row in report['candidates']),'optimizer_steps':0}))
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--output',type=Path,required=True);main(parser.parse_args().output)

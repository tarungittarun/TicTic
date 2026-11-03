"""Assemble exact-build multi-run evidence; never turn failed history into success."""
import argparse,json,shutil,subprocess,tempfile
from pathlib import Path
from mynotes.tools.build_module_e2e_report import ROOT,MODULES,PROFILES,ingest,apply_recheck,render,write
from mynotes.tools.ci.run_ordered_modules import plan

def main():
    p=argparse.ArgumentParser();p.add_argument('--ordered',type=Path,required=True);p.add_argument('--voice',type=Path,required=True);p.add_argument('--foundation',type=Path,required=True);a=p.parse_args()
    output=ROOT/'mynotes/docs/module-e2e/41650596888';data=json.loads((output/'provenance.json').read_text());observations=json.loads((output/'measurements.json').read_text())
    if data['source']!='dcd3e2c3cecded90cd4d990c6850ee4437c02151' or data['run']!='41650596888':raise ValueError('Unexpected parent evidence')
    proofs=[json.loads((ROOT/'mynotes/docs/module-e2e/early-41650596888/actual-orientation-correction.json').read_text())]
    import tarfile
    for shard in ['voice0','voice1']:
        folder=a.voice/('module-profile-correction-dark-landscape-'+shard);execution=json.loads((folder/'module-execution.json').read_text())
        with tarfile.open(folder/'module-benchmark.tar') as archive:
            member=archive.getmember('module-benchmark/voice-'+shard[-1]+'.json')
            if member.size>4*1024*1024:raise ValueError('Oversized correction')
            rows=json.loads(archive.extractfile(member).read())['cases']
        proofs.append({'run':'41654076991','artifact_source_run':data['run'],'artifact_source_sha':data['source'],'profile':'dark-landscape','lane':'dark-landscape-'+shard,'execution':execution,'measurements':rows,'setup_correction':'Same post-focus rotation ordering proven by OCR recheck; exact original test APKs, assertions and inputs.'})
    for proof in proofs:apply_recheck(data,observations,proof,output)
    ordered=plan();timings={}
    for profile in PROFILES:
        folder=a.ordered/('module-ordered-native-'+profile)
        destination=a.ordered/('module-native-'+profile+'-ordered');folder.rename(destination)
        executions=json.loads((destination/'ordered-module-execution.json').read_text())
        if [r['module'] for r in executions]!=MODULES:raise ValueError('Native modules did not execute in PDF order')
        for row in executions:
            if not row['ok'] or row['process_exit']!=0 or len(row['cases'])!=len(set(row['cases'])) or set(row['cases'])!=set(ordered[row['module']]):raise ValueError('Incomplete strict native execution')
        timings[profile]=executions
    with tempfile.TemporaryDirectory() as temp:
        scratch=Path(temp);native,_,inventory=ingest(a.ordered,scratch,data['source'],'41654548239')
        if not native['native_complete'] or native['native_outcomes']!=1676:raise ValueError('Native schedule audit incomplete')
        for frame in native['screens']:
            frame['capture_run']='41654548239';source=scratch/frame['path'];shutil.copyfile(source,output/frame['path'])
        for file in scratch.glob('*refinement-environment-*.json'):shutil.copyfile(file,output/file.name)
        # Original screenshots remain on disk, hash-bound in original-provenance.
        data.update(native=native['native'],native_complete=True,native_outcomes=1676,screens=[r for r in data['screens'] if not r.get('lane')]+native['screens'],strict_native_run='41654548239',strict_native_module_order=MODULES,native_outcomes_including_original=3352)
    write(output/'strict-native-module-executions.json',timings)
    for pattern,name in [('refinement-jvm.json','executed-jvm.json'),('trained-whisper.json','experimental-model-provenance.json'),('manifest.json','public-corpus-provenance.json')]:
        files=list(a.foundation.rglob(pattern))
        if len(files)!=1:raise ValueError('Missing or ambiguous source-build metadata: '+pattern)
        shutil.copyfile(files[0],output/name)
    baseline='38401d2c6c18334c3dcd20baea845930c6344a6b';trees={}
    for path in ['mynotes/app/src/main','mynotes/runtime/src/main','mynotes/domain/src/main']:
        before=subprocess.check_output(['git','rev-parse',baseline+':'+path],text=True).strip();after=subprocess.check_output(['git','rev-parse',data['source']+':'+path],text=True).strip()
        if before!=after:raise ValueError('Unexpected production source change')
        trees[path]={'baseline_tree':before,'executed_tree':after,'identical':True}
    data['production_source_baseline']={'commit':baseline,'trees':trees,'scope':'Production source equality, not binary equivalence to a different build. All rechecks reuse exact original source-run APK artifact.'}
    data['primary_run_conclusion']='failure: three benchmark landscape orientation checks';data['qualification_runs']=['41650596888','41652746046','41654076991','41654548239']
    write(output/'provenance.json',data);write(output/'measurements.json',observations);summary=render(data,observations,inventory,output)
    print(json.dumps(summary,indent=2))
if __name__=='__main__':main()

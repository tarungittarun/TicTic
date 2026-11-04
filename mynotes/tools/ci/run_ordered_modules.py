"""Run every native method once, with strict module barriers, on an existing APK.
No app/model changes. Preserve failed modules and continue to collect other evidence.
"""
import argparse,json,os,shutil,subprocess,time
from pathlib import Path
from mynotes.tools.build_module_e2e_report import MODULES,ROOT
from mynotes.tools.build_e2e_report import methods
from mynotes.tools.ci.results import android,compare

def plan():
    ordered=json.loads((ROOT/'mynotes/docs/module-e2e-method-order.json').read_text())
    actual=methods();ids=[n for names in ordered.values() for n in names]
    if list(ordered)!=MODULES or len(ids)!=len(set(ids)) or set(ids)!=set(actual):raise ValueError('Module plan must cover actual native inventory exactly once')
    return ordered

def preserve_partial(expected,executions,ordered):
    """Never lose already captured assertions if the next adb command stalls."""
    passed=[name for row in executions for name in row['cases']]
    failures=[failure for row in executions for failure in row['failures']]
    failures += ['Module launcher failed: '+row['module'] for row in executions if row['process_exit']!=0]
    report=compare(expected,passed,failures)
    report.update(cases=passed,instrumentation_invocations=len(executions),module_order=list(ordered))
    report['ok']=report['ok'] and len(executions)==len(ordered) and all(r['process_exit']==0 and r['ok'] for r in executions)
    Path('refinement-native.json').write_text(json.dumps(report,indent=2)+'\n')
    return report

def main():
    p=argparse.ArgumentParser();p.add_argument('--night',required=True);p.add_argument('--orientation',required=True);a=p.parse_args()
    ordered=plan();combined='';executions=[]
    expected=[n for names in ordered.values() for n in names]
    preserve_partial(expected,executions,ordered)
    for index,(module,selected) in enumerate(ordered.items()):
        print(f'MODULE {index+1:02d}/{len(ordered)} BEGIN {module}: {len(selected)} methods',flush=True)
        # Reassert only host configuration, never relax app assertions or repeat a test.
        subprocess.run(['adb','shell','am','start','-W','-n','app.space.notes.debug/app.space.notes.NotesActivity'],check=True,timeout=60)
        subprocess.run(['python3','-m','mynotes.tools.ci.native_environment','--orientation',a.orientation],check=True,timeout=120)
        start=time.monotonic();command=['bash','mynotes/tools/ci/run_refinement_instrumentation.sh','-e','timeout_msec','180000','-e','expectedFont','1.0','-e','expectedNight',a.night,'-e','expectedOrientation',a.orientation,'-e','expectedMotion','0','-e','expectedMic','denied','-e','class',','.join(selected)]
        raw=Path(f'ordered-module-{index+1:02d}.txt');environment={**os.environ,'NOTES_NATIVE_OUTPUT':str(raw)}
        result=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,env=environment,timeout=1650);elapsed=round((time.monotonic()-start)*1000)
        Path(f'ordered-module-{index+1:02d}-launcher.txt').write_text(result.stdout)
        # The wrapper overwrites these each module; preserve bounded PUBLIC fixture phases.
        for suffix in ['cpu-phases','test-progress','stall-threads']:
            diagnostic=Path('refinement-native-'+suffix+'.txt')
            if diagnostic.exists() and diagnostic.stat().st_size<=5000000:shutil.copyfile(diagnostic,Path(f'ordered-module-{index+1:02d}-{suffix}.txt'))
        actual=raw.read_text(errors='replace') if raw.exists() else '';observed=android(actual,selected);observed.update(module=module,elapsed_ms=elapsed,process_exit=result.returncode)
        executions.append(observed);combined+=actual+'\n';Path('refinement-native.txt').write_text(combined);Path('ordered-module-execution.json').write_text(json.dumps(executions,indent=2)+'\n')
        preserve_partial(expected,executions,ordered)
        print(f'MODULE {index+1:02d} END '+json.dumps({k:v for k,v in observed.items() if k!='cases'}),flush=True)
    report=preserve_partial(expected,executions,ordered)
    # Preserve real screenshots even when an assertion fails.
    with Path('refinement-captures.tar').open('wb') as stream:subprocess.run(['adb','exec-out','run-as','app.space.notes.debug','tar','-C','files','-cf','-','ux-review'],stdout=stream,check=False,timeout=30)
    if not report['ok'] or not all(r['process_exit']==0 and r['ok'] for r in executions):raise SystemExit(1)
if __name__=='__main__':main()

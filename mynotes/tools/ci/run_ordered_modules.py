"""Run every native method once, with strict module barriers, on an existing APK.
No app/model changes. Preserve failed modules and continue to collect other evidence.
"""
import argparse,json,subprocess,time
from pathlib import Path
from mynotes.tools.build_module_e2e_report import MODULES,ROOT
from mynotes.tools.build_e2e_report import methods
from mynotes.tools.ci.results import android

def plan():
    ordered=json.loads((ROOT/'mynotes/docs/module-e2e-method-order.json').read_text())
    actual=methods();ids=[n for names in ordered.values() for n in names]
    if list(ordered)!=MODULES or len(ids)!=len(set(ids)) or set(ids)!=set(actual):raise ValueError('Module plan must cover actual native inventory exactly once')
    return ordered

def main():
    p=argparse.ArgumentParser();p.add_argument('--night',required=True);p.add_argument('--orientation',required=True);a=p.parse_args()
    ordered=plan();combined='';executions=[]
    for index,(module,selected) in enumerate(ordered.items()):
        print(f'MODULE {index+1:02d}/{len(ordered)} BEGIN {module}: {len(selected)} methods',flush=True)
        # Reassert only host configuration, never relax app assertions or repeat a test.
        subprocess.run(['adb','shell','am','start','-W','-n','app.space.notes.debug/app.space.notes.NotesActivity'],check=True)
        subprocess.run(['python3','-m','mynotes.tools.ci.native_environment','--orientation',a.orientation],check=True)
        start=time.monotonic();command=['bash','mynotes/tools/ci/run_refinement_instrumentation.sh','-e','timeout_msec','180000','-e','expectedFont','1.0','-e','expectedNight',a.night,'-e','expectedOrientation',a.orientation,'-e','expectedMotion','0','-e','expectedMic','denied','-e','class',','.join(selected)]
        result=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);elapsed=round((time.monotonic()-start)*1000)
        Path(f'ordered-module-{index+1:02d}.txt').write_text(result.stdout);observed=android(result.stdout,selected);observed.update(module=module,elapsed_ms=elapsed,process_exit=result.returncode)
        executions.append(observed);combined+=result.stdout+'\n';Path('refinement-native.txt').write_text(combined);Path('ordered-module-execution.json').write_text(json.dumps(executions,indent=2)+'\n')
        print(f'MODULE {index+1:02d} END '+json.dumps({k:v for k,v in observed.items() if k!='cases'}),flush=True)
    expected=[n for names in ordered.values() for n in names];report=android(combined,expected);Path('refinement-native.json').write_text(json.dumps(report,indent=2)+'\n')
    if not report['ok'] or not all(r['process_exit']==0 and r['ok'] for r in executions):raise SystemExit(1)
if __name__=='__main__':main()

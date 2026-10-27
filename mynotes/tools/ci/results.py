"""Machine-readable coverage gates, bounded annotations and summaries on every outcome."""
import argparse
from collections import Counter
import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET
from mynotes.tools.ci.test_plan import build_plan,cases,lane_cases


def compare(expected,passed,failed):
    counts=Counter(passed)
    missing=sorted(set(expected)-set(passed))
    unexpected=sorted(set(passed)-set(expected))
    duplicate=sorted(name for name,count in counts.items() if count!=1)
    return {'ok':not (missing or unexpected or duplicate or failed),'expected':len(expected),
            'passed':len(passed),'missing':missing,'unexpected':unexpected,'duplicates':duplicate,'failures':failed}


def android(text,expected):
    passed=[];failed=[];record={};completions=[];continuation=None
    for line in text.splitlines():
        if line.startswith('INSTRUMENTATION_STATUS: '):
            key,sep,value=line[len('INSTRUMENTATION_STATUS: '):].partition('=')
            if sep:record[key]=value;continuation=key
        elif line.startswith('INSTRUMENTATION_STATUS_CODE: '):
            code=int(line.split(':',1)[1].strip())
            if code<=0:
                name=record.get('class','')+'#'+record.get('test','')
                if not record.get('class') or not record.get('test'):failed.append('Outcome without test identity')
                elif code==0:passed.append(name)
                else:failed.append(name+': status '+str(code)+' '+record.get('stack',''))
            record={};continuation=None
        elif line.startswith('INSTRUMENTATION_CODE: '):completions.append(int(line.split(':',1)[1].strip()))
        elif 'INSTRUMENTATION_FAILED' in line or 'Process crashed' in line or 'shortMsg=' in line:
            failed.append(line)
        elif continuation=='stack':record['stack']=record.get('stack','')+'\n'+line
    if completions!=[-1]:failed.append('Missing, duplicate or unsuccessful instrumentation completion: '+str(completions))
    result=compare(expected,passed,failed)
    result['cases']=passed
    return result


def junit(paths,expected):
    passed=[];failed=[]
    for path in paths:
        root=ET.parse(path).getroot()
        for case in root.iter('testcase'):
            name=case.attrib.get('classname','')+'#'+case.attrib.get('name','')
            problems=case.findall('failure')+case.findall('error')+case.findall('skipped')
            if problems:
                for item in problems:failed.append(name+': '+item.tag+' '+item.attrib.get('message','')+' '+(item.text or ''))
            else:passed.append(name)
    result=compare(expected,passed,failed);result['cases']=passed;return result


def build_diagnostic(path):
    lines=Path(path).read_text(errors='replace').splitlines() if Path(path).exists() else ['No build log produced']
    keep=set(range(min(12,len(lines)))) | set(range(max(0,len(lines)-20),len(lines)))
    for i,line in enumerate(lines):
        if re.search(r'error:|FAILURE:|What went wrong|Duplicate class|FATAL EXCEPTION|AndroidRuntime|NoClassDefFoundError|Could not |Execution failed|> Task .*FAILED|tests completed|PANIC|emulator.*(?:ERROR|FATAL)|SIGSEGV|No space left|KVM',line,re.IGNORECASE):
            keep.update(range(max(0,i-2),min(len(lines),i+8)))
    return '\n'.join(lines[i] for i in sorted(keep)) if keep else '\n'.join(lines[-35:])


def annotate(text):
    for i in range(0,min(len(text),10800),3600):
        chunk=text[i:i+3600].replace('%','%25').replace('\r','%0D').replace('\n','%0A')
        print(f'::error title=MyNotes CI diagnostic {i//3600+1}::{chunk}')


def write_report(result,path):
    Path(path).parent.mkdir(parents=True,exist_ok=True)
    Path(path).write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({k:v for k,v in result.items() if k!='cases'},indent=2))
    if not result['ok']:annotate(json.dumps({k:v for k,v in result.items() if k!='cases'},indent=2))


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('kind',choices=['android','jvm','build']);p.add_argument('--path',required=True)
    p.add_argument('--group',default='all');p.add_argument('--output',default='evidence/results.json');args=p.parse_args()
    if args.kind=='build':annotate(build_diagnostic(args.path));sys.exit(0)
    try:
        plan=build_plan()
        result=android(Path(args.path).read_text(errors='replace'),lane_cases(plan,args.group)) if args.kind=='android' else junit(sorted(Path(args.path).glob('*.xml')),cases(plan['jvm']))
    except Exception as e:result={'ok':False,'error':str(e),'failures':['Evidence missing or invalid']}
    write_report(result,args.output);sys.exit(0 if result['ok'] else 1)

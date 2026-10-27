"""Discover every Java @Test and enforce exhaustive, disjoint Android partitions.
Method additions are automatic; a new class requires an explicit partition assignment.
"""
import argparse
import json
from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[2]


def mask_comments_and_strings(text):
    # Keep source positions/newlines but remove comments/literals before scanning annotations.
    pattern=r'//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\''
    return re.sub(pattern,lambda m:''.join('\n' if c=='\n' else ' ' for c in m[0]),text)


def discover(directory):
    classes={}
    for path in sorted(Path(directory).rglob('*.java')):
        text=mask_comments_and_strings(path.read_text())
        methods=re.findall(r'@(?:org\.junit\.)?Test\b(?:\s*\([^)]*\))?\s+public\s+void\s+(\w+)\s*\(',text)
        if not methods:
            if re.search(r'@(?:org\.junit\.)?Test\b',text):
                raise ValueError(f'Unrecognised @Test declaration: {path}')
            continue
        if len(methods)!=len(set(methods)):
            raise ValueError(f'Duplicate test method: {path}')
        if len(methods)!=len(re.findall(r'@(?:org\.junit\.)?Test\b',text)):
            raise ValueError(f'Not every @Test is discoverable: {path}')
        package=re.search(r'\bpackage\s+([\w.]+)\s*;',text)
        cls=re.search(r'public\s+(?:final\s+)?class\s+(\w+)',text)
        if not package or not cls or cls[1]!=path.stem:
            raise ValueError(f'Unsupported test class layout: {path}')
        name=package[1]+'.'+cls[1]
        if name in classes:raise ValueError(f'Duplicate test class {name}')
        classes[name]=sorted(methods)
    if not classes:raise ValueError(f'No tests found in {directory}')
    return classes


def build_plan(root=ROOT,config=None):
    config=config or json.loads((root/'tools/ci/plan.json').read_text())
    if config.get('schema')!=1:raise ValueError('Unknown CI plan schema')
    for directory in list(root.glob('*/src/test/java'))+list(root.glob('*/src/androidTest/java')):
        if directory not in [root/'domain/src/test/java',root/'app/src/androidTest/java']:
            if any(re.search(r'@(?:org\.junit\.)?Test\b',mask_comments_and_strings(p.read_text())) for p in directory.rglob('*.java')):
                raise ValueError('New test module requires CI wiring: '+str(directory))
    android=discover(root/'app/src/androidTest/java')
    jvm=discover(root/'domain/src/test/java')
    assigned={};groups={}
    for group,names in config['android'].items():
        if not names:raise ValueError('Empty test partition '+group)
        members={}
        for short in names:
            matches=[name for name in android if name.rsplit('.',1)[-1]==short]
            if len(matches)!=1:raise ValueError('Unknown/ambiguous test class '+short)
            name=matches[0]
            if name in assigned:raise ValueError('Class assigned twice: '+name)
            assigned[name]=group;members[name]=android[name]
        groups[group]=members
    if set(assigned)!=set(android):raise ValueError('Assign new test classes: '+str(sorted(set(android)-set(assigned))))
    lanes=config['lanes'];ids=[lane['id'] for lane in lanes]
    if len(ids)!=len(set(ids)) or not lanes:raise ValueError('Duplicate/empty lane IDs')
    for lane in lanes:
        if not re.fullmatch(r'[a-z][a-z0-9-]*',lane['id']):raise ValueError('Invalid lane ID')
        if lane['group'] not in {*groups,'all'}:raise ValueError('Unknown lane group')
        if not isinstance(lane['api'],int) or lane['api']<26:raise ValueError('Unsupported lane API')
    covered={name for lane in lanes for name in (android if lane['group']=='all' else groups[lane['group']])}
    if covered!=set(android):raise ValueError('Some tests have no emulator lane')
    if not any(lane['api']==26 for lane in lanes):raise ValueError('Minimum SDK lane missing')
    if not any(lane['group']=='all' and lane['api']>=34 for lane in lanes):raise ValueError('Full modern API lane missing')
    return {'schema':1,'jvm':jvm,'android':android,'groups':groups,'lanes':lanes}


def cases(classes):
    return sorted(cls+'#'+method for cls,methods in classes.items() for method in methods)


def lane_cases(plan,group):
    return cases(plan['android'] if group=='all' else plan['groups'][group])


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--output');parser.add_argument('--group');parser.add_argument('--classes',action='store_true');parser.add_argument('--matrix',action='store_true');args=parser.parse_args()
    plan=build_plan()
    if args.output:Path(args.output).write_text(json.dumps(plan,indent=2)+'\n')
    if args.matrix:print(json.dumps({'include':plan['lanes']},separators=(',',':')))
    elif args.classes:
        selected=plan['android'] if args.group=='all' else plan['groups'][args.group]
        print(','.join(sorted(selected)))
    else:print(json.dumps({'jvm':len(cases(plan['jvm'])),'android_unique':len(cases(plan['android'])),'lanes':{lane['id']:len(lane_cases(plan,lane['group'])) for lane in plan['lanes']}},indent=2))

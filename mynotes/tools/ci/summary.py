"""Always-run, fail-closed final gate. Trust neither skipped jobs nor incomplete artifacts."""
import argparse
import json
import os
from pathlib import Path
from mynotes.tools.ci.test_plan import build_plan,cases,lane_cases

REQUIRED=['checks','jvm','release-build','test-build','apk-audit','android']


def evaluate(needs,evidence,plan):
    root=Path(evidence);errors=[];lines=['# MyNotes acceptance','', '| Gate | Outcome |','|---|---|']
    for name in REQUIRED+['publish']:
        outcome=needs.get(name,{}).get('result','missing')
        lines.append(f'| {name} | {outcome} |')
        if name in REQUIRED and outcome!='success':errors.append(name+': '+outcome)
    def one(pattern):
        paths=list(root.rglob(pattern))
        if len(paths)!=1:raise ValueError(f'Expected one {pattern}, found {len(paths)}')
        return json.loads(paths[0].read_text())
    try:
        audit=one('audit.json')
        eligible=audit['publish_eligible']
        if not isinstance(eligible,bool):raise ValueError('Invalid eligibility')
        expected_publication='success' if eligible else 'skipped'
        if needs.get('publish',{}).get('result')!=expected_publication:errors.append('Publication did not match audited eligibility')
        # Artifact name isolates accepted-release from any later publication copies.
        lines+=['','## Tested official release',f"- `{audit['filename']}` — {audit['accepted_from']}",
                f"- SHA-256: `{audit['accepted_sha256']}`",f"- Source run: `{audit['source']}`",f"- APK source: `{audit.get('accepted_source','not recorded')}`",
                '- Release-byte verification/upgrade/launch is separate from debug-host instrumentation.','',
                '| Android lane | API | Passed / expected | Installed APK hash matches |','|---|---|---|---|']
        for lane in plan['lanes']:
            paths=list(root.rglob(lane['id']+'/results.json'))
            identities=list(root.rglob(lane['id']+'/identity.json'))
            if len(paths)!=1 or len(identities)!=1:raise ValueError('Missing/duplicate evidence for '+lane['id'])
            result=json.loads(paths[0].read_text());identity=json.loads(identities[0].read_text())
            expected=lane_cases(plan,lane['group'])
            exact=sorted(result.get('cases',[]))==expected
            hash_ok=identity.get('installed_sha256')==identity.get('download_sha256')==identity.get('audited_sha256')==audit['accepted_sha256']
            if not (result.get('ok') and exact and hash_ok and identity.get('api')==lane['api'] and identity.get('group')==lane['group'] and identity.get('lane')==lane['id'] and identity.get('source')==audit['source']):
                errors.append('Invalid/incomplete test or APK identity: '+lane['id'])
            lines.append(f"| {lane['id']} | {lane['api']} | {result.get('passed',0)} / {len(expected)} | {hash_ok} |")
        jvm=one('jvm.json')
        if not jvm.get('ok') or sorted(jvm.get('cases',[]))!=cases(plan['jvm']):errors.append('JVM executed coverage mismatch')
        lines+=['',f"JVM: {jvm.get('passed',0)} cases; Android: {len(cases(plan['android']))} unique cases, {sum(len(lane_cases(plan,l['group'])) for l in plan['lanes'])} planned executions.",
                'Evidence includes raw runner output, package/launch state, screenshots, installed APK and bounded logcat.',
                'This is emulator qualification, not all-phone, physical SAF-provider, accessibility or future ML qualification.']
    except (ValueError,KeyError,OSError) as e:errors.append(str(e))
    lines+=['','## Outcome',('PASS' if not errors else 'FAIL — do not publish/use this run as acceptance')]
    lines+=['- '+error for error in errors]
    return not errors,'\n'.join(lines)+'\n'


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--evidence',required=True);args=p.parse_args()
    ok,markdown=evaluate(json.loads(os.environ.get('NEEDS_JSON','{}')),args.evidence,build_plan())
    print(markdown)
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'],'a') as f:f.write(markdown)
    raise SystemExit(0 if ok else 1)

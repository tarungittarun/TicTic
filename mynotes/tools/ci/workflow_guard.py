"""Validate workflow topology and isolation. Run after installing pinned PyYAML in CI."""
from pathlib import Path
import yaml
from mynotes.tools.ci.audit import REPO
from mynotes.tools.ci.summary import REQUIRED


def validate(workflow):
    jobs=workflow['jobs']
    from mynotes.tools.ci.test_plan import build_plan
    assert len(jobs)-1+len(build_plan()['lanes'])<=45,'Expanded workflow exceeds the 45-job budget'
    def deps(job):
        value=jobs[job].get('needs',[])
        return [value] if isinstance(value,str) else value
    for name in ['checks','inventory','lint','jvm','release-build','test-build']:
        assert not deps(name),name+' must remain independent'
    assert set(deps('android'))=={'checks','test-build','apk-audit'}
    assert set(deps('publish'))==set(REQUIRED),'Publication must require every gate'
    assert set(deps('acceptance'))==set(REQUIRED)|{'publish'}
    assert jobs['acceptance']['if']=='always()'
    assert jobs['android']['strategy']['fail-fast']=='false'
    assert workflow['permissions']=={'contents':'read'}
    assert jobs['publish']['permissions']=={'contents':'write'}
    assert 'publish_eligible' in jobs['publish']['if']
    assert 'refs/heads/arena/3caca636-tictic' in jobs['publish']['if']
    visited=set();stack=set()
    def walk(job):
        assert job in jobs,'Unknown dependency: '+job
        assert job not in stack,'Workflow dependency cycle'
        if job in visited:return
        stack.add(job)
        for dependency in deps(job):walk(dependency)
        stack.remove(job);visited.add(job)
    for name in jobs:
        walk(name)
        assert 1<=int(jobs[name]['timeout-minutes'])<=15,'Bound every job'
        for step in jobs[name]['steps']:
            command=step.get('run','')
            assert ':app:assemble' not in command,'Legacy Android app is frozen'
            assert 'tests.yml' not in command,'Legacy tests workflow is frozen'
    return sorted(jobs)


if __name__=='__main__':
    workflow=yaml.load((REPO/'.github/workflows/notes-ci.yml').read_text(),Loader=yaml.BaseLoader)
    print('Workflow topology/permissions/isolation PASS:',', '.join(validate(workflow)))

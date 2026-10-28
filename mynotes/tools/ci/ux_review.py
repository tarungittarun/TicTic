"""Human-visible rendered evidence is a publication gate, not a replacement for runtime tests."""
import argparse
import json
import os
import re
from pathlib import Path
from mynotes.tools.ci.audit import REPO, assert_source_equivalent

APPROVAL=REPO/'mynotes/docs/ux-review/approval.json'

def approved(source,approval=APPROVAL):
    try:
        value=json.loads(Path(approval).read_text())
        reviewed=value['reviewed_source']
        if not isinstance(reviewed,str) or not re.fullmatch('[0-9a-f]{40}',reviewed):return False
        if set(value.get('inspected_variants',[]))!={'full-api34','night-api34','large-font-api34','landscape-api34','tablet-api34'}:return False
        if not value.get('review_run') or not value.get('findings'):return False
        assert_source_equivalent(reviewed,source)
        return True
    except (OSError,ValueError,KeyError):return False

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--audit',required=True);args=parser.parse_args()
    path=Path(args.audit);audit=json.loads(path.read_text());ok=approved(os.environ['GITHUB_SHA']);audit['ux_review_approved']=ok;path.write_text(json.dumps(audit,indent=2)+'\n')
    with open(os.environ['GITHUB_OUTPUT'],'a') as out:out.write('ux_approved='+str(ok).lower()+'\n')
    print('Rendered UX review: '+('approved for equivalent app inputs' if ok else 'PENDING — publication withheld; inspect the synthetic screenshots'))

"""Conservative command accounting: never infer full coverage from screenshots."""
import re

def command_coverage(commands,features):
 result=[]
 for command in commands:
  token=re.compile(r'(?<![\w:-])'+re.escape(command['id'])+r'(?=[;\s]|$)')
  matches=[f for f in features if token.search(f.get('observed',''))]
  statuses={f['status'] for f in matches}
  status=('functional_readback_pass' if 'functional_pass' in statuses else
          'invoked_ui_inspected' if 'screen_inspected' in statuses else
          'blocked_or_unverified' if matches else 'inventoried_only')
  result.append(dict(command,status=status,evidence_features=[f['feature'] for f in matches]))
 return result

import json
import unittest
from mynotes.tools.ci.release_rich_update_probe import METHOD,parse_proof

class ReleaseRichUpdateProbe(unittest.TestCase):
    def proof(self,phase='after',version=66):
        return dict(phase=phase,version_code=version,notes=4,old_snapshot_sha256='a'*64,payload_unchanged=True,private_folder_retained=True,candidate_write_readback=phase=='after',physical_acceptance=False,ui_rendering_acceptance=False)
    def output(self,proof):
        cls,method=METHOD.split('#')
        return f'INSTRUMENTATION_STATUS: stream=PUBLIC_RICH_UPGRADE_PROOF={json.dumps(proof)}\nINSTRUMENTATION_STATUS_CODE: 2\nINSTRUMENTATION_STATUS: class={cls}\nINSTRUMENTATION_STATUS: test={method}\nINSTRUMENTATION_STATUS_CODE: 0\nINSTRUMENTATION_CODE: -1\n'
    def test_three_explicit_stages_are_verified(self):
        for phase,version in (('create',64),('before',64),('after',66)):
            self.assertEqual(phase,parse_proof(self.output(self.proof(phase,version)),phase,version)['phase'])
    def test_missing_failed_or_duplicate_native_execution_is_rejected(self):
        text=self.output(self.proof())
        for bad in ('',text.replace('INSTRUMENTATION_STATUS_CODE: 0','INSTRUMENTATION_STATUS_CODE: -2'),text+text):
            with self.assertRaises(ValueError):parse_proof(bad,'after',66)
    def test_lost_payload_folder_checkpoint_or_writer_readback_rejected(self):
        for key,value in [('payload_unchanged',False),('private_folder_retained',False),('old_snapshot_sha256','bad'),('candidate_write_readback',False),('notes',3)]:
            proof=self.proof();proof[key]=value
            with self.assertRaises(ValueError):parse_proof(self.output(proof),'after',66)
    def test_physical_ui_or_wrong_version_claim_cannot_pass(self):
        for key,value in [('physical_acceptance',True),('ui_rendering_acceptance',True),('version_code',64),('phase','before')]:
            proof=self.proof();proof[key]=value
            with self.assertRaises(ValueError):parse_proof(self.output(proof),'after',66)

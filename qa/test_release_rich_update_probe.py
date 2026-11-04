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
    def test_explicit_66_to_67_proof_identity_is_checked(self):
        self.assertEqual(67,parse_proof(self.output(self.proof('after',67)),'after',67)['version_code'])
        with self.assertRaises(ValueError):parse_proof(self.output(self.proof('after',66)),'after',67)
    def test_downgrade_or_noninteger_codes_fail_before_any_device_operation(self):
        from mynotes.tools.ci.release_rich_update_probe import validate_upgrade_codes
        for old,new in [(67,66),(66,66),(0,67),(True,67),(66,'67'),(66,2100000001)]:
            with self.assertRaises(ValueError):validate_upgrade_codes(old,new)
        validate_upgrade_codes(66,67)
    def test_new_code_pair_still_uses_one_install_over_and_no_clear(self):
        import tempfile
        from pathlib import Path
        from unittest.mock import Mock,patch
        from mynotes.tools.ci.release_rich_update_probe import verify
        device=Mock();device.state.side_effect=[dict(version_code=66,first_install_time='unchanged'),dict(version_code=67,first_install_time='unchanged')]
        def adb(*args):
            if 'ro.kernel.qemu' in args:return '1'
            if 'ro.build.version.sdk' in args:return '34'
            if 'instrument' in args:
                phase=args[args.index('richPhase')+1];version=args[args.index('expectedReleaseCode')+1]
                return self.output(self.proof(phase,version))
            return ''
        device.adb.side_effect=adb
        with tempfile.TemporaryDirectory() as folder,patch('mynotes.tools.ci.release_rich_update_probe.Device',return_value=device),patch('mynotes.tools.ci.release_rich_update_probe.Path'),patch('builtins.print'):
            result=verify('old.apk','candidate.apk','probe.apk',str(Path(folder)/'proof.json'),66,67)
        self.assertTrue(result['ok']);calls=[c.args for c in device.adb.call_args_list]
        self.assertEqual(1,calls.count(('install','-r','candidate.apk')))
        self.assertFalse(any('uninstall' in c or 'clear' in c for c in calls))
        self.assertFalse(result['ui_rendering_acceptance']);self.assertFalse(result['physical_acceptance'])

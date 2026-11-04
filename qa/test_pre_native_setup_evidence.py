import json
import unittest
from mynotes.tools.ci.pre_native_setup_evidence import verify, PREFIX, COMMAND

class PreNativeSetupEvidence(unittest.TestCase):
    def setUp(self):
        self.files={'refinement-environment-configured.json':json.dumps({'stage':'configured','anr_header':'<no ANR has occurred since boot>'}).encode(),'refinement-environment-configured.txt':b'actual configured host'}
        self.log=PREFIX+'[command]/usr/bin/sh -c '+COMMAND+'\n'+PREFIX+'##[error]The operation was canceled.\nWith the provided path, there will be 2 files uploaded\n'

    def test_observed_zero_test_host_stall_is_distinct_from_app_failure(self):
        result=verify(self.log,self.files)
        self.assertFalse(result['native_launcher_started'])
        self.assertEqual(result['tests_executed'],0)
        self.assertEqual(len(result['log_sha256']),64)

    def test_other_last_command_cannot_be_substituted(self):
        with self.assertRaises(ValueError):verify(self.log.replace(COMMAND,'adb shell different'),self.files)

    def test_native_invocation_never_qualifies_as_pre_native(self):
        with self.assertRaises(ValueError):verify(PREFIX+'[command]/usr/bin/sh -c python3 -m mynotes.tools.ci.run_ordered_modules\n'+self.log,self.files)

    def test_module_start_prevents_zero_native_assertion(self):
        with self.assertRaises(ValueError):verify(self.log+'MODULE 01/13 BEGIN Notes: 20 methods\n',self.files)

    def test_native_result_artifact_prevents_zero_native_assertion(self):
        with self.assertRaises(ValueError):verify(self.log,{**self.files,'refinement-native.json':b'{}'})

    def test_missing_original_environment_file_is_rejected(self):
        with self.assertRaises(ValueError):verify(self.log,{})

    def test_app_anr_is_not_relabelled_as_host_setup(self):
        self.files['refinement-environment-configured.json']=b'{"stage":"configured","anr_header":"ANR app.space.notes.debug"}'
        with self.assertRaises(ValueError):verify(self.log,self.files)

    def test_missing_cancellation_proof_is_rejected(self):
        with self.assertRaises(ValueError):verify(self.log.replace('##[error]The operation was canceled.',''),self.files)

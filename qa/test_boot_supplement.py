import copy
import unittest
from mynotes.tools.ci.build_boot_supplement_report import validate_receipt


def receipt():
    return dict(original_conclusion='failure',source='a'*40,original_source='a'*40,missing_lanes=['light-portrait-ocr'],lane_capture_runs={'light-portrait-ocr':999},original_artifact_names=['module-benchmark-dark-portrait-ocr'],original_failed_jobs=[dict(lane='light-portrait-ocr',conclusion='failure',boot_timeout_verified=True,tests_executed=0)])


class BootSupplement(unittest.TestCase):
    def test_only_zero_test_verified_missing_boot_lane_accepted(self):
        self.assertEqual({'light-portrait-ocr'},validate_receipt(receipt()))

    def test_never_overwrite_original_measurements(self):
        r=receipt();r['original_artifact_names'].append('module-benchmark-light-portrait-ocr')
        with self.assertRaises(ValueError):validate_receipt(r)

    def test_assertion_failure_cannot_be_relabelled_boot(self):
        for key,value in [('tests_executed',1),('boot_timeout_verified',False),('boot_timeout_verified','false')]:
            r=receipt();r['original_failed_jobs'][0][key]=value
            with self.assertRaises(ValueError):validate_receipt(r)

    def test_other_failure_must_block_composite(self):
        r=receipt();r['original_failed_jobs'].append(dict(lane='dark-portrait-ocr',conclusion='failure',boot_timeout_verified=True,tests_executed=0))
        with self.assertRaises(ValueError):validate_receipt(r)

    def test_source_or_original_outcome_cannot_change(self):
        for key,value in [('original_source','b'*40),('original_conclusion','success')]:
            r=receipt();r[key]=value
            with self.assertRaises(ValueError):validate_receipt(r)

    def test_duplicate_unknown_and_nonocr_lane_rejected(self):
        for lanes in ([],['light-portrait-ocr']*2,['unknown-ocr'],['light-portrait-voice0']):
            r=receipt();r['missing_lanes']=lanes
            with self.assertRaises(ValueError):validate_receipt(r)

    def test_every_missing_lane_requires_actual_numeric_capture_run(self):
        for mapping in ({},{'light-portrait-ocr':True},{'light-portrait-ocr':0},{'light-portrait-ocr':'999'},{'light-portrait-ocr':999,'dark-portrait-ocr':888}):
            r=receipt();r['lane_capture_runs']=mapping
            with self.assertRaises(ValueError):validate_receipt(r)

    def test_original_native_screens_and_distinct_supplement_runs_retained(self):
        import tempfile
        from pathlib import Path
        from unittest.mock import patch
        from mynotes.tools.ci import build_boot_supplement_report as module
        r=receipt();r['build_run']=100
        r['missing_lanes'].append('dark-landscape-ocr')
        r['lane_capture_runs']['dark-landscape-ocr']=888
        r['original_failed_jobs'].append(dict(lane='dark-landscape-ocr',conclusion='failure',boot_timeout_verified=True,tests_executed=0))
        data={'screens':[{'profile':'light-portrait'},{'profile':'synthetic-input-not-UI'}],
              'benchmark_executions':{lane:dict(ok=True,passed=4) for lane in r['missing_lanes']},
              'native_complete':True,'benchmark_complete':True}
        rows=[{'lane':lane} for lane in r['missing_lanes'] for i in range(384)]+[{'lane':'dark-portrait-voice0'}]
        with tempfile.TemporaryDirectory() as temp,patch.object(module,'ingest',return_value=(data,rows,{})),patch.object(module,'render',return_value={}):
            module.build(Path(temp),Path(temp),r,999)
        self.assertEqual(['100','999'],[f['capture_run'] for f in data['screens']])
        self.assertEqual({999},set(row['capture_run'] for row in rows[:384]))
        self.assertEqual({888},set(row['capture_run'] for row in rows[384:768]))
        self.assertEqual(100,rows[-1]['capture_run'])

    def test_original_input_specimens_remain_original_if_only_dark_lane_missing(self):
        import tempfile
        from pathlib import Path
        from unittest.mock import patch
        from mynotes.tools.ci import build_boot_supplement_report as module
        r=receipt();r['build_run']=100
        r['missing_lanes']=['dark-landscape-ocr'];r['lane_capture_runs']={'dark-landscape-ocr':888}
        r['original_failed_jobs'][0]['lane']='dark-landscape-ocr'
        data={'screens':[{'profile':'synthetic-input-not-UI'}],
              'benchmark_executions':{'dark-landscape-ocr':dict(ok=True,passed=4)},
              'native_complete':True,'benchmark_complete':True}
        rows=[{'lane':'dark-landscape-ocr'} for i in range(384)]
        with tempfile.TemporaryDirectory() as temp,patch.object(module,'ingest',return_value=(data,rows,{})),patch.object(module,'render',return_value={}):
            module.build(Path(temp),Path(temp),r,999)
        self.assertEqual('100',data['screens'][0]['capture_run'])

    def test_native_boot_lane_accepted_but_partial_or_asserted_original_never_replaced(self):
        r=receipt();r['missing_lanes']=['light-portrait-ordered'];r['lane_capture_runs']={'light-portrait-ordered':999};r['original_failed_jobs'][0]['lane']='light-portrait-ordered'
        self.assertEqual({'light-portrait-ordered'},validate_receipt(r))
        for field,value in [('tests_executed',1),('boot_timeout_verified',False)]:
            wrong=copy.deepcopy(r);wrong['original_failed_jobs'][0][field]=value
            with self.assertRaises(ValueError):validate_receipt(wrong)
        r['original_artifact_names'].append('module-native-light-portrait-ordered')
        with self.assertRaises(ValueError):validate_receipt(r)

    def test_native_supplement_keeps_original_benchmark_capture_and_all_native_methods(self):
        import tempfile
        from pathlib import Path
        from unittest.mock import patch
        from mynotes.tools.ci import build_boot_supplement_report as m
        r=receipt();r.update(build_run=100,missing_lanes=['light-portrait-ordered'],lane_capture_runs={'light-portrait-ordered':999});r['original_failed_jobs'][0]['lane']='light-portrait-ordered'
        data={'screens':[{'profile':'light-portrait','lane':'light-portrait-ordered'},{'profile':'dark-portrait','lane':'dark-portrait-ordered'},{'profile':'synthetic-input-not-UI'}],'benchmark_executions':{},'native':{'light-portrait-ordered':{'ok':True,'passed':2,'cases':['a','b']}},'native_complete':True,'benchmark_complete':True}
        rows=[{'lane':'light-portrait-ocr'}]
        with tempfile.TemporaryDirectory() as temp,patch.object(m,'ingest',return_value=(data,rows,{'a':{},'b':{}})),patch.object(m,'render',return_value={}):m.build(Path(temp),Path(temp),r,999)
        self.assertEqual(['999','100','100'],[f['capture_run'] for f in data['screens']]);self.assertEqual(100,rows[0]['capture_run'])
        self.assertEqual({'light-portrait-ordered':999},data['native_capture_runs'])

    def test_native_supplement_with_duplicate_or_missing_method_is_fatal(self):
        import tempfile
        from pathlib import Path
        from unittest.mock import patch
        from mynotes.tools.ci import build_boot_supplement_report as m
        r=receipt();r.update(build_run=100,missing_lanes=['light-portrait-ordered'],lane_capture_runs={'light-portrait-ordered':999});r['original_failed_jobs'][0]['lane']='light-portrait-ordered'
        for cases in [['a'],['a','a']]:
            data={'screens':[],'benchmark_executions':{},'native':{'light-portrait-ordered':{'ok':True,'passed':2,'cases':cases}},'native_complete':True,'benchmark_complete':True}
            with tempfile.TemporaryDirectory() as temp,patch.object(m,'ingest',return_value=(data,[],{'a':{},'b':{}})),patch.object(m,'render',return_value={}):
                with self.assertRaisesRegex(ValueError,'complete exact original inventory'):m.build(Path(temp),Path(temp),r,999)

class PreNativeComposite(unittest.TestCase):
    def setUp(self):
        import json
        from mynotes.tools.ci.pre_native_setup_evidence import PREFIX,COMMAND,verify
        self.files={'refinement-environment-configured.json':json.dumps({'stage':'configured','anr_header':'<no ANR has occurred since boot>'}).encode(),'refinement-environment-configured.txt':b'actual'}
        self.log=PREFIX+'[command]/usr/bin/sh -c '+COMMAND+'\n'+PREFIX+'##[error]The operation was canceled.\nWith the provided path, there will be 2 files uploaded\n'
        self.r=receipt();self.r.update(build_run=100,missing_lanes=['light-landscape-ordered'],lane_capture_runs={'light-landscape-ordered':999},original_artifact_names=['module-native-light-landscape-ordered'],original_failed_jobs=[dict(lane='light-landscape-ordered',conclusion='cancelled',tests_executed=0,pre_native_setup_verified=True,setup_failure_proof=verify(self.log,self.files))])

    def test_precisely_proven_pre_native_cancel_is_distinct_from_boot(self):
        self.assertEqual({'light-landscape-ordered'},validate_receipt(self.r))

    def test_unknown_cancellation_is_not_accepted(self):
        del self.r['original_failed_jobs'][0]['setup_failure_proof']
        with self.assertRaises(ValueError):validate_receipt(self.r)

    def test_native_launcher_started_cannot_be_zero_test(self):
        self.r['original_failed_jobs'][0]['setup_failure_proof']['native_launcher_started']=True
        with self.assertRaises(ValueError):validate_receipt(self.r)

    def test_original_zero_native_artifact_cannot_be_hidden(self):
        self.r['original_artifact_names']=[]
        with self.assertRaises(ValueError):validate_receipt(self.r)

    def test_other_lane_cannot_use_the_landscape_specific_setup_proof(self):
        self.r['original_failed_jobs'][0]['lane']='light-portrait-ordered';self.r['missing_lanes']=['light-portrait-ordered'];self.r['lane_capture_runs']={'light-portrait-ordered':999}
        with self.assertRaises(ValueError):validate_receipt(self.r)

    def test_boolean_zero_is_not_an_executed_test_counter(self):
        self.r['original_failed_jobs'][0]['tests_executed']=False
        with self.assertRaises(ValueError):validate_receipt(self.r)

    def test_original_environment_is_preserved_before_recovered_native_ingestion(self):
        import tempfile
        from pathlib import Path
        from unittest.mock import patch
        from mynotes.tools.ci import build_boot_supplement_report as m
        data={'screens':[],'benchmark_executions':{},'native':{'light-landscape-ordered':{'ok':True,'passed':2,'cases':['a','b']}},'native_complete':True,'benchmark_complete':True}
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);original=root/'original-zero-test-light-landscape-ordered';original.mkdir();out=root/'report';out.mkdir()
            for name,raw in self.files.items():(original/name).write_bytes(raw)
            (root/'original-zero-test-light-landscape-ordered-job-log.txt').write_text(self.log)
            with patch.object(m,'ingest',return_value=(data,[],{'a':{},'b':{}})),patch.object(m,'render',return_value={}):
                m.build(root,out,self.r,999)
                self.assertEqual((out/'original-zero-test-setup/light-landscape-ordered/original-job-log.txt').read_text(),self.log)
                (original/'refinement-native.json').write_text('{}')
                with self.assertRaises(ValueError):m.build(root,out,self.r,999)

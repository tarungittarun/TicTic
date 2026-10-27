"""Self-tests for the CI gates: simulate omissions, runner skips and publication hazards."""
import copy
import json
from pathlib import Path
import tempfile
import unittest
from mynotes.tools.ci.test_plan import build_plan,cases,discover,lane_cases,ROOT
from mynotes.tools.ci.results import android,junit,build_diagnostic
from mynotes.tools.ci.audit import publication_policy,assert_manifest,APP_PATHS,release_input,assert_source_equivalent
from mynotes.tools.ci.summary import evaluate,REQUIRED

class TestDiscovery(unittest.TestCase):
    def config(self):return json.loads((ROOT/'tools/ci/plan.json').read_text())
    def test_real_inventory_has_exact_disjoint_partitions(self):
        plan=build_plan();groups=[set(cases(c)) for c in plan['groups'].values()]
        self.assertEqual(set(cases(plan['android'])),set.union(*groups))
        for i,group in enumerate(groups):
            for other in groups[i+1:]:self.assertFalse(group & other)
        self.assertGreater(len(cases(plan['jvm'])),0)
    def test_unassigned_class_rejected(self):
        config=self.config();config['android']['storage'].pop()
        with self.assertRaisesRegex(ValueError,'Assign new'):build_plan(config=config)
    def test_duplicate_assignment_rejected(self):
        config=self.config();config['android']['ui'].append(config['android']['storage'][0])
        with self.assertRaisesRegex(ValueError,'twice'):build_plan(config=config)
    def test_unknown_empty_and_ambiguous_partition_rejected(self):
        for replacement in [[],['NonexistentTest']]:
            config=self.config();config['android']['ui']=replacement
            with self.assertRaises(ValueError):build_plan(config=config)
    def test_lanes_cannot_drop_minimum_modern_or_repeat_id(self):
        for change in ['min','modern','duplicate','group','id']:
            config=self.config()
            if change=='min':config['lanes'][0]['api']=27
            if change=='modern':config['lanes'][-1]['group']='storage'
            if change=='duplicate':config['lanes'][-1]['id']=config['lanes'][0]['id']
            if change=='group':config['lanes'][0]['group']='nope'
            if change=='id':config['lanes'][0]['id']='../oops'
            with self.assertRaises(ValueError,msg=change):build_plan(config=config)
    def discover_text(self,text):
        with tempfile.TemporaryDirectory() as d:
            Path(d,'SampleTest.java').write_text('package example; public class SampleTest { '+text+' }')
            return discover(Path(d))
    def test_comments_and_strings_are_not_fake_tests(self):
        result=self.discover_text('''// @Test public void fake() {}
        String x="@Test public void fake2() {}";
        /* @Test public void fake3() {} */
        @Test(expected=IllegalArgumentException.class) public void real() {}''')
        self.assertEqual({'example.SampleTest':['real']},result)
    def test_unknown_declaration_and_duplicate_methods_fail_closed(self):
        for text in ['@Test protected void hidden() {}','@Test public void a() {} @Test public void a() {}','// no tests']:
            with self.assertRaises(ValueError):self.discover_text(text)
    def test_new_test_module_rejected_until_wired(self):
        with tempfile.TemporaryDirectory() as d:
            target=Path(d)/'runtime/src/test/java';target.mkdir(parents=True)
            (target/'New.java').write_text('@Test public void newTest() {}')
            with self.assertRaisesRegex(ValueError,'module'):build_plan(Path(d),self.config())

class ExecutionEvidence(unittest.TestCase):
    def output(self,code=0):
        return '\n'.join(['INSTRUMENTATION_STATUS: class=example.SampleTest','INSTRUMENTATION_STATUS: test=works',
                          'INSTRUMENTATION_STATUS_CODE: '+str(code),'INSTRUMENTATION_CODE: -1'])
    def test_raw_android_complete_pass(self):
        self.assertTrue(android(self.output(),['example.SampleTest#works'])['ok'])
    def test_runner_missing_crashed_ignored_failed_and_assumption_are_red(self):
        for output in ['',self.output().replace('INSTRUMENTATION_CODE: -1',''),self.output()+'\nINSTRUMENTATION_RESULT: shortMsg=Process crashed',
                       self.output()+'\nINSTRUMENTATION_CODE: 0',self.output()+'\nINSTRUMENTATION_CODE: -1',self.output(-1),self.output(-2),self.output(-3),self.output(-4)]:
            self.assertFalse(android(output,['example.SampleTest#works'])['ok'],output)
    def test_duplicate_missing_or_extra_case_rejected(self):
        for output,expected in [(self.output()+'\n'+self.output(),['example.SampleTest#works']),
                                (self.output(),['example.SampleTest#other']),
                                (self.output(),['example.SampleTest#works','example.SampleTest#other'])]:
            self.assertFalse(android(output,expected)['ok'])
    def test_status_without_identity_rejected(self):
        self.assertFalse(android('INSTRUMENTATION_STATUS_CODE: 0\nINSTRUMENTATION_CODE: -1',[])['ok'])
    def junit_result(self,body):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'TEST.xml';p.write_text('<testsuite>'+body+'</testsuite>')
            return junit([p],['example.SampleTest#works'])
    def test_junit_pass_requires_exact_cases(self):
        self.assertTrue(self.junit_result('<testcase classname="example.SampleTest" name="works"/>')['ok'])
        self.assertFalse(self.junit_result('')['ok'])
    def test_junit_failure_error_and_skip_are_red_with_details(self):
        for tag in ['failure','error','skipped']:
            result=self.junit_result('<testcase classname="example.SampleTest" name="works"><'+tag+' message="useful detail"/></testcase>')
            self.assertFalse(result['ok']);self.assertIn('useful detail',result['failures'][0])
    def test_build_diagnostics_capture_compiler_context(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'build.log';p.write_text('before\nSource.java:30: error: bad literal\n bad line\n ^\nafter')
            self.assertIn('bad line',build_diagnostic(p))

class ApkAuditPolicy(unittest.TestCase):
    OLD=('app.space.notes',5,'0.4.0','certificate')
    def test_new_code_eligible_and_same_code_is_revalidation_only(self):
        self.assertFalse(publication_policy(self.OLD,[self.OLD]))
        self.assertTrue(publication_policy(('app.space.notes',6,'0.5.0','certificate'),[self.OLD]))
    def test_old_code_new_name_signature_package_and_duplicate_refused(self):
        for current,old in [(('app.space.notes',4,'0.3.0','certificate'),[self.OLD]),
                            (('app.space.notes',5,'0.4.1','certificate'),[self.OLD]),
                            (('app.space.notes',5,'0.4.0','other'),[self.OLD]),
                            (('other',6,'0.5.0','certificate'),[self.OLD]),(self.OLD,[self.OLD,self.OLD])]:
            with self.assertRaises(ValueError):publication_policy(current,old)
    def test_manifest_privacy_and_floor(self):
        xml='A: android:allowBackup(0x01010280)=(type 0x12)0x0'
        badging="sdkVersion:'26'"
        self.assertEqual([],assert_manifest(badging,xml))
        for bad,x in [(badging+"\nuses-permission: name='android.permission.INTERNET'",xml),
                      (badging+'\napplication-debuggable',xml),("sdkVersion:'25'",xml),
                      (badging,xml.replace('0x0','0xffffffff')),(badging,'')]:
            with self.assertRaises(ValueError):assert_manifest(bad,x)
    def test_unknown_build_inputs_fail_closed_but_ci_docs_and_tests_do_not(self):
        for path in ['mynotes/new.gradle','mynotes/app/new-rules.pro','mynotes/gradle/libs.versions.toml','mynotes/runtime/src/main/assets/model.md','mynotes/runtime/src/main/assets/हिन्दी.txt']:
            self.assertTrue(release_input(path),path)
        for path in ['mynotes/README.md','mynotes/tools/ci/audit.py','mynotes/qa/test_ci_pipeline.py','mynotes/app/src/androidTest/java/Test.java','.github/workflows/notes-ci.yml']:
            self.assertFalse(release_input(path),path)
    def test_minimum_api_privilege_is_provisioning_only(self):
        script=(ROOT/'tools/ci/android_lane.sh').read_text()
        self.assertLess(script.index('adb root'),script.index('adb shell svc wifi disable'))
        self.assertLess(script.index('adb shell svc data disable'),script.index('adb unroot'))
        self.assertLess(script.index('adb unroot'),script.index('adb install -r'))
        self.assertIn('= 2000',script)
    def test_all_runtime_build_identity_paths_protected(self):
        for path in ['app/src/main','domain/src/main','runtime/src/main','gradle','signing','settings.gradle','gradle.properties','app/build.gradle']:
            self.assertIn('mynotes/'+path,APP_PATHS)

class SummaryGate(unittest.TestCase):
    def fixture(self,root):
        plan=build_plan();needs={key:{'result':'success'} for key in REQUIRED};needs['publish']={'result':'skipped'}
        def write(path,data):
            path=Path(root)/path;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data))
        write('release/audit.json',{'publish_eligible':False,'filename':'test.apk','accepted_from':'immutable published APK','accepted_sha256':'abc','source':'source'})
        write('jvm/jvm.json',{'ok':True,'cases':cases(plan['jvm']),'passed':len(cases(plan['jvm']))})
        for lane in plan['lanes']:
            expected=lane_cases(plan,lane['group']);prefix=lane['id']+'/'
            write(prefix+'results.json',{'ok':True,'cases':expected,'passed':len(expected)})
            write(prefix+'identity.json',{'installed_sha256':'abc','download_sha256':'abc','audited_sha256':'abc','api':lane['api'],'lane':lane['id'],'group':lane['group'],'source':'source'})
        return needs,plan
    def test_revalidation_passes_with_publication_intentionally_skipped(self):
        with tempfile.TemporaryDirectory() as d:
            needs,plan=self.fixture(d);ok,text=evaluate(needs,d,plan)
            self.assertTrue(ok,text);self.assertIn('separate from debug',text)
    def test_skipped_gate_failed_publish_or_wrong_installed_apk_rejected(self):
        for change in ['gate','publish','hash','missing','methods','source']:
            with tempfile.TemporaryDirectory() as d:
                needs,plan=self.fixture(d)
                path=Path(d)/plan['lanes'][0]['id']
                if change=='gate':needs['jvm']['result']='skipped'
                elif change=='publish':needs['publish']['result']='success'
                elif change=='missing':(path/'results.json').unlink()
                else:
                    p=path/('results.json' if change=='methods' else 'identity.json');data=json.loads(p.read_text())
                    if change=='hash':data['installed_sha256']='wrong'
                    elif change=='source':data['source']='other'
                    else:data['cases'].pop()
                    p.write_text(json.dumps(data))
                self.assertFalse(evaluate(needs,d,plan)[0],change)
    def test_missing_everything_produces_failure_summary_not_exception(self):
        with tempfile.TemporaryDirectory() as d:
            ok,text=evaluate({},d,build_plan());self.assertFalse(ok);self.assertIn('FAIL',text)

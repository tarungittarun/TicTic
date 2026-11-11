import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile


def load(name):
    spec=importlib.util.spec_from_file_location(name,Path('mynotes/tools/comparisons')/(name+'.py'))
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module


class ComparisonRetentionTest(unittest.TestCase):
    def test_preserves_originals_without_claiming_incomplete_coverage(self):
        report=load('comparison37_report')
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);lane=root/'in/comparison37-light-portrait/comparison37-evidence';lane.mkdir(parents=True)
            original={'results':[{'module':'PLAN-X01','app':'mynotes','status':'blocked','requested_task_acceptance':False}], 'whole_module_acceptance':False,'physical_acceptance':False}
            raw=json.dumps(original).encode();(lane/'modules.json').write_bytes(raw)
            metadata={'status':'completed','databaseId':123,'headSha':'abc','url':'https://github.com/test/run/123','conclusion':'failure'}
            result=report.retain(root/'in',root/'out',metadata)
            self.assertEqual((root/'out/comparison37-light-portrait/comparison37-evidence/modules.json').read_bytes(),raw)
            self.assertFalse(result['all_profiles_have74_attempt_records']);self.assertFalse(result['whole_module_acceptance'])
            self.assertEqual(result['profiles']['dark-portrait']['attempts'],0)
            original['results']*=2;(lane/'modules.json').write_text(json.dumps(original))
            with self.assertRaisesRegex(ValueError,'Duplicate'):report.retain(root/'in',root/'out2',metadata)

    def test_running_workflow_not_reported_final(self):
        report=load('comparison37_report')
        with self.assertRaisesRegex(ValueError,'Unfinished'):report.retain('/tmp/absent-comparison','/tmp/never-written-comparison',{'status':'in_progress'})

    def test_real_format_library_fixtures(self):
        try:
            import docx
            import pptx
            from odf.opendocument import load as load_odt
            from odf import teletype
        except ImportError:self.skipTest('Optional Office corpus libraries not installed')
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);load('office_fixtures').write_office(root)
            self.assertIn('cobalt42',docx.Document(root/'Comparison.docx').paragraphs[0].text)
            slides=pptx.Presentation(root/'Comparison.pptx')
            self.assertIn('cobalt42',slides.slides[0].shapes[0].text)
            self.assertIn('cobalt42',teletype.extractText(load_odt(str(root/'Comparison.odt')).text))
            for extension in ['docx','pptx']:
                with zipfile.ZipFile(root/('Comparison.'+extension)) as archive:
                    self.assertIn('[Content_Types].xml',archive.namelist());self.assertIn('_rels/.rels',archive.namelist())
            with zipfile.ZipFile(root/'Comparison.odt') as archive:
                self.assertEqual(archive.infolist()[0].filename,'mimetype');self.assertEqual(archive.infolist()[0].compress_type,zipfile.ZIP_STORED)
                self.assertEqual(archive.read('mimetype'),b'application/vnd.oasis.opendocument.text')
                self.assertIn('META-INF/manifest.xml',archive.namelist())

    def test_relative_evidence_path_renders_a_linked_blocker_report(self):
        try:
            import fitz
            import reportlab
        except ImportError:self.skipTest('Optional report packages not installed')
        report=load('comparison37_report')
        with tempfile.TemporaryDirectory(prefix='comparison37-report-test-',dir=Path.cwd()/'mynotes/docs') as temporary:
            root=Path(temporary);lane=root/'in/comparison37-light-portrait/comparison37-evidence';lane.mkdir(parents=True)
            (lane/'modules.json').write_text(json.dumps({'results':[],'whole_module_acceptance':False,'physical_acceptance':False}))
            (lane/'fatal.json').write_text(json.dumps({'error':'Public fixture unavailable; no coverage inferred'}))
            report.retain(root/'in',root/'evidence',{'status':'completed','databaseId':1,'headSha':'a'*40,'url':'https://github.com/example/example/actions/runs/1','conclusion':'failure'})
            path=report.render((root/'evidence').relative_to(Path.cwd()),'b'*40)
            with fitz.open(path) as document:
                self.assertGreaterEqual(len(document),5)
                self.assertIn('acceptance still open',' '.join(document[0].get_text().split()))
                self.assertGreater(sum(len(page.get_links()) for page in document),2)
                self.assertIn('Public fixture unavailable',' '.join(page.get_text() for page in document))

    def test_chunk_records_merge_without_double_counting_and_inputs_keep_exact_bytes(self):
        report=load('comparison37_report')
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary)
            for i in [1,2]:
                lane=root/'in'/('comparison37-light-portrait-chunk%02d'%i)/'comparison37-evidence';lane.mkdir(parents=True)
                (lane/'modules.json').write_text(json.dumps({'results':[{'module':'PLAN-X%02d'%i,'app':'mynotes','status':'inspection_only','requested_task_acceptance':False}], 'whole_module_acceptance':False,'physical_acceptance':False}))
                fixtures=lane/'public-fixtures';fixtures.mkdir();(fixtures/'Public.txt').write_bytes(b'Exact public bytes\n'*100)
            metadata={'status':'completed','databaseId':1,'headSha':'a'*40,'url':'https://github.com/example/example/actions/runs/1','conclusion':'failure'}
            result=report.retain(root/'in',root/'out',metadata)
            self.assertEqual(result['profiles']['light-portrait']['attempts'],2)
            self.assertEqual(len(result['profiles']['light-portrait']['module_files']),2)
            self.assertFalse(result['all_profiles_have74_attempt_records'])
            receipt=json.loads((root/'out/retention.json').read_text());inputs=[r for r in receipt['files'] if r['path'].endswith('Public.txt')]
            self.assertEqual(len(inputs),2);self.assertEqual(inputs[0]['retained_path'],inputs[1]['retained_path'])
            self.assertEqual((root/'out'/inputs[0]['retained_path']).read_bytes(),b'Exact public bytes\n'*100)
            report.retain(root/'in',root/'out',metadata)
            self.assertEqual(json.loads((root/'out/retention.json').read_text())['bytes'],receipt['bytes'])

    def test_large_original_text_is_losslessly_compressed_not_truncated(self):
        import gzip,hashlib
        report=load('comparison37_report')
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);lane=root/'in/comparison37-light-portrait/comparison37-evidence';lane.mkdir(parents=True)
            raw=b'Public original diagnostic line\n'*10000;(lane/'public-diagnostic.txt').write_bytes(raw)
            report.retain(root/'in',root/'out',{'status':'completed','databaseId':1,'headSha':'a'*40,'url':'https://github.com/example/example/actions/runs/1','conclusion':'failure'})
            entry=json.loads((root/'out/retention.json').read_text())['files'][0]
            self.assertEqual(entry['storage_encoding'],'gzip')
            self.assertEqual(gzip.decompress((root/'out'/entry['retained_path']).read_bytes()),raw)
            self.assertEqual(entry['sha256'],hashlib.sha256(raw).hexdigest());self.assertEqual(entry['bytes'],len(raw))

    def test_targeted_manual_review_hashes_match_retained_originals(self):
        import hashlib
        observations=json.loads(Path('mynotes/docs/comparisons/comparison37/reviewed-observations.json').read_text())['observations']
        selected=[item for item in observations if item['id'] in {'CMP37-H22','CMP37-H23'}]
        self.assertEqual({item['id'] for item in selected},{'CMP37-H22','CMP37-H23'})
        for item in selected:
            for evidence in item['evidence']:
                original=Path(evidence['path'])
                self.assertTrue(original.is_file(),evidence['path'])
                self.assertEqual(hashlib.sha256(original.read_bytes()).hexdigest(),evidence['sha256'],evidence['path'])
                self.assertIn('/raw/',evidence['url'])

class SingletonArtifactRetentionTest(unittest.TestCase):
    def test_single_download_flat_layout_keeps_original_profile_and_host_snapshot(self):
        report=load('comparison37_report')
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);source=root/'input';lane=source/'comparison37-evidence';lane.mkdir(parents=True)
            original={'results':[{'module':'PLAN-X31','app':'mynotes','status':'blocked','requested_task_acceptance':False}],'whole_module_acceptance':False,'physical_acceptance':False}
            raw=json.dumps(original).encode();(lane/'modules.json').write_bytes(raw)
            host=b'{"scope":"unit fixture only"}';(lane/'host-after.json').write_bytes(host)
            metadata={'status':'completed','databaseId':123,'headSha':'abc','url':'https://github.com/test/run/123','conclusion':'failure','artifacts':{'artifacts':[{'name':'comparison37-light-portrait-chunk05'}]}}
            summary=report.retain(source,root/'out',metadata)
            retained=root/'out/comparison37-light-portrait-chunk05/comparison37-evidence'
            self.assertEqual((retained/'modules.json').read_bytes(),raw)
            self.assertEqual((retained/'host-after.json').read_bytes(),host)
            self.assertEqual(summary['profiles']['light-portrait']['attempts'],1)
            self.assertFalse(summary['whole_module_acceptance'])
            metadata['artifacts']['artifacts'].append({'name':'comparison37-dark-portrait-chunk05'})
            with self.assertRaisesRegex(ValueError,'exactly one'):report.retain(source,root/'ambiguous',metadata)

    def test_explicit_single_profile_diagnostic_alias_preserves_its_original_artifact_name(self):
        report=load('comparison37_report')
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);source=root/'input';lane=source/'comparison37-evidence';lane.mkdir(parents=True)
            original={'results':[{'module':'PLAN-X31','app':'mynotes','status':'interaction_observed','requested_task_acceptance':False},{'module':'PLAN-X31','app':'obsidian','status':'blocked','requested_task_acceptance':False}],'whole_module_acceptance':False,'physical_acceptance':False}
            raw=json.dumps(original).encode();(lane/'modules.json').write_bytes(raw)
            (lane/'emulator-exit-trace.txt').write_bytes(b'bounded public signal trace fixture')
            metadata={'status':'completed','databaseId':43083262273,'headSha':'abc','url':'https://github.com/test/run/43083262273','conclusion':'success','artifacts':{'artifacts':[{'name':'comparison37-dark-landscape-guest-loss-diagnostic'}]}}
            summary=report.retain(source,root/'out',metadata)
            retained=root/'out/comparison37-dark-landscape-guest-loss-diagnostic/comparison37-evidence'
            self.assertEqual((retained/'modules.json').read_bytes(),raw)
            self.assertEqual((retained/'emulator-exit-trace.txt').read_bytes(),b'bounded public signal trace fixture')
            self.assertEqual(summary['profiles']['dark-landscape']['attempts'],2)
            self.assertIn('comparison37-dark-landscape-guest-loss-diagnostic',summary['profiles']['dark-landscape']['module_file'])
            self.assertEqual(summary['profiles']['light-portrait']['attempts'],0)
            self.assertFalse(summary['whole_module_acceptance'])
            metadata['artifacts']['artifacts'][0]['name']='comparison37-unknown-diagnostic'
            with self.assertRaisesRegex(ValueError,'Unknown singleton'):report.retain(source,root/'unknown',metadata)

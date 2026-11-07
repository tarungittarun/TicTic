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
                self.assertEqual(len(document),5)
                self.assertIn('acceptance still open',' '.join(document[0].get_text().split()))
                self.assertGreater(sum(len(page.get_links()) for page in document),2)
                self.assertIn('Public fixture unavailable',document[1].get_text())

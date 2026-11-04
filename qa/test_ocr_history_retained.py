import hashlib
import json
from pathlib import Path
import unittest
from mynotes.tools.ci.audit_ocr61_history import pair


class RetainedHistoricalOcrTest(unittest.TestCase):
    def test_failed_audit_keeps_all_original_predictions_and_source_hashes(self):
        root=Path(__file__).resolve().parents[1]
        folder=root/'mynotes/docs/recognition/ocr-history/41770347277'
        report=json.loads((folder/'independent-audit.json').read_text())
        self.assertEqual('failure',report['audit_conclusion'])
        self.assertFalse(report['adopted'])
        self.assertEqual(1152,len(report['pairs']))
        self.assertEqual(1152,len({(p['profile'],p['id']) for p in report['pairs']}))
        self.assertEqual(51,sum(p['regressed'] for p in report['pairs']))
        self.assertEqual(45,sum(p['improved'] for p in report['pairs']))
        rows={}
        for source in report['source_reports']:
            file=root/source['path'];data=file.read_bytes()
            self.assertEqual(source['sha256'],hashlib.sha256(data).hexdigest())
            rows[str(file.relative_to(root))]={r['id']:r for r in json.loads(data)['cases']}
        for retained in report['pairs']:
            base='mynotes/docs/module-e2e/'
            old=rows[base+'41704429338/'+retained['profile']+'-ocr-measurements.json'][retained['id']]
            new=rows[base+'41767304608/'+retained['profile']+'-ocr-measurements.json'][retained['id']]
            self.assertEqual(retained,{'profile':retained['profile'],**pair(old,new)})
        receipts=json.loads((folder/'artifact-receipts.json').read_text())
        self.assertEqual(6,len(receipts))
        self.assertEqual({41704429338,41767304608},{r['workflow_run']['id'] for r in receipts})

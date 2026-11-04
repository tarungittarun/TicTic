import unittest
from mynotes.tools.ci.audit_quantized_speech import audit


class QuantizedAuditTest(unittest.TestCase):
    def report(self):
        return {'source_model':{m:{'bytes':100} for m in ('f16','q8_0','q5_1')},'cases':[dict(model=m,id=s+'-arctic_b'+str(i).zfill(4),speaker=s,input_sha256='fixed',reference='hello world',hypothesis='hello world',status='OK',wer=0,process_seconds=1) for m in ('f16','q8_0','q5_1') for s in ('ksp','slt') for i in range(33,65)]}

    def test_independent_score_rejects_invented_or_drifted_metrics(self):
        report=self.report();report['cases'][-1]['hypothesis']='wrong words'
        with self.assertRaises(ValueError):audit(report)
        report=self.report();report['cases'][-1]['input_sha256']='changed'
        with self.assertRaises(ValueError):audit(report)

    def test_regression_retained_not_adopted(self):
        report=self.report();report['cases'][-1].update(hypothesis='hello',wer=.5)
        result=audit(report)
        self.assertFalse(result['adopted'])
        self.assertEqual(['slt-arctic_b0064'],result['summary']['q5_1']['regressed_ids'])

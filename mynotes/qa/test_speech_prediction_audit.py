import copy
import unittest
from mynotes.tools.ci.speech_prediction_audit import audit,edits


class SpeechPredictionAuditTests(unittest.TestCase):
    def fixture(self):
        case={'speaker':'ksp','id':'one','reference':'Hello, world!','hypothesis':'hello','reference_words':2,'word_edits':1}
        baseline={'cases':[case],'reference_words':2,'word_edits':1}
        adapted=copy.deepcopy(baseline)
        adapted['cases'][0].update(hypothesis='hello world extra',word_edits=1)
        return {'baseline':{'ksp':baseline},'adapted':{'ksp':adapted}}

    def test_distance(self):
        self.assertEqual(2,edits(['one','two'],['one','extra','three']))
        self.assertEqual(1,edits(['one'],[]))
        self.assertEqual(1,edits([],['one']))

    def test_recomputes_and_exposes_regression(self):
        report=self.fixture()
        report['adapted']['ksp']['cases'][0].update(hypothesis='wrong',word_edits=2)
        report['adapted']['ksp']['word_edits']=2
        result=audit(report)
        self.assertFalse(result['adopted'])
        self.assertEqual(1,result['groups']['ksp']['regressed'])
        self.assertEqual('one',result['groups']['ksp']['regressions'][0]['id'])

    def test_rejects_forged_edits(self):
        report=self.fixture()
        report['adapted']['ksp']['cases'][0]['word_edits']=0
        with self.assertRaises(ValueError):audit(report)

    def test_rejects_duplicate_or_missing_pair(self):
        for mutation in ('duplicate','missing'):
            report=self.fixture()
            cases=report['adapted']['ksp']['cases']
            if mutation=='duplicate':cases.append(copy.deepcopy(cases[0]))
            else:cases[0]['id']='other'
            with self.assertRaises(ValueError):audit(report)

    def test_rejects_aggregate_and_reference_changes(self):
        report=self.fixture();report['baseline']['ksp']['word_edits']=0
        with self.assertRaises(ValueError):audit(report)
        report=self.fixture();report['adapted']['ksp']['cases'][0]['reference']='Hello earth'
        with self.assertRaises(ValueError):audit(report)

    def test_preserved_expanded_predictions_recompute_exactly(self):
        import json
        from pathlib import Path
        source=Path(__file__).resolve().parents[1]/'docs/speech-acoustic/41393453222/training-report.json'
        groups=audit(json.loads(source.read_text()))['groups']
        self.assertEqual((34,22,9,18,5),(groups['ksp']['baseline_edits'],groups['ksp']['adapted_edits'],groups['ksp']['improved'],groups['ksp']['unchanged'],groups['ksp']['regressed']))
        self.assertEqual((13,10,1),(groups['slt']['baseline_edits'],groups['slt']['adapted_edits'],groups['slt']['regressed']))

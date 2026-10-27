"""Protocol-reference fault tests; these are NOT Android/SAF runtime tests."""
import hashlib
import json
import tempfile
import unittest
from pathlib import Path
from mynotes.tools.notes_storage_probe import LibraryProbe, Conflict, IntegrityError, encode


class NotesStorageProtocol(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.store = LibraryProbe(self.root)

    def first(self):
        return self.store.commit({'n1': {'title': 'नोट — ಕನ್ನಡ', 'body': 'hello'}}, None)

    def test_round_trip_unicode_and_reopen(self):
        head = self.first()
        actual, docs = LibraryProbe(self.root).recover()
        self.assertEqual(head, actual)
        self.assertEqual('नोट — ಕನ್ನಡ', docs['n1']['title'])

    def test_partial_write_preserves_previous_generation(self):
        head = self.first()
        with self.assertRaises(OSError):
            self.store.commit({'n2': {}}, head, 'partial_write')
        self.assertEqual(head, self.store.recover()[0])

    def test_failure_before_write_preserves_generation(self):
        head = self.first()
        with self.assertRaises(OSError):
            self.store.commit({}, head, 'before_write')
        self.assertEqual(head, self.store.recover()[0])

    def test_death_before_ack_recovers_complete_commit(self):
        head = self.first()
        with self.assertRaises(OSError):
            self.store.commit({'new': {}}, head, 'after_close')
        self.assertEqual({'new': {}}, self.store.recover()[1])

    def test_stale_writer_rejected_without_writing(self):
        old = self.first()
        self.store.commit({'new': {}}, old)
        count = len(list(self.root.iterdir()))
        with self.assertRaises(Conflict):
            self.store.commit({}, old)
        self.assertEqual(count, len(list(self.root.iterdir())))

    def test_corrupt_latest_recovers_prior(self):
        old = self.first()
        new = self.store.commit({'new': {}}, old)
        (self.root / (new+'.json')).write_text('broken')
        self.assertEqual(old, self.store.recover()[0])

    def test_only_corrupt_data_does_not_become_empty_library(self):
        (self.root/'broken.json').write_text('{')
        with self.assertRaises(IntegrityError):
            self.store.recover()

    def write_generation(self, body):
        envelope = {'body': body, 'sha256': hashlib.sha256(encode(body)).hexdigest()}
        (self.root/(body['id']+'.json')).write_bytes(encode(envelope))

    def test_fork_blocks_writes(self):
        head = self.first()
        self.store.commit({'branch1': {}}, head)
        self.write_generation({'version':1, 'id':'branch2','parent':head,'documents':{}})
        with self.assertRaises(Conflict): self.store.recover()
        with self.assertRaises(Conflict): self.store.commit({}, head)

    def test_missing_parent_is_not_silently_accepted(self):
        self.write_generation({'version':1,'id':'orphan','parent':'missing','documents':{}})
        with self.assertRaises(IntegrityError): self.store.recover()

    def test_future_schema_is_not_silently_ignored(self):
        self.write_generation({'version':2,'id':'future','parent':None,'documents':{}})
        with self.assertRaises(IntegrityError): self.store.recover()

    def test_cycle_rejected(self):
        self.write_generation({'version':1,'id':'a','parent':'b','documents':{}})
        self.write_generation({'version':1,'id':'b','parent':'a','documents':{}})
        with self.assertRaises(IntegrityError): self.store.recover()

    def test_commit_does_not_retain_mutable_caller_state(self):
        docs={'n':{'body':'original'}}
        self.store.commit(docs,None)
        docs['n']['body']='changed'
        self.assertEqual('original',self.store.recover()[1]['n']['body'])

    def test_deletion_and_empty_library_are_real_commits(self):
        head=self.first()
        new=self.store.commit({},head)
        self.assertEqual((new,{}),self.store.recover())

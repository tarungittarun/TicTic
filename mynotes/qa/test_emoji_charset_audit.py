import json
from pathlib import Path
import struct
import unittest
from mynotes.tools.ci.emoji_charset_audit import tokens,emoji_sequences

class EmojiRepertoireAuditTests(unittest.TestCase):
    def test_lstm_component_is_extracted_and_reserved_markers_are_not_characters(self):
        content=b'4\nNULL 0 Common 0\nJoined 7 metadata\n|Broken|0|1 f metadata\nA 5 metadata\n'
        offsets=[-1]*24;offsets[21]=196
        count,values=tokens(struct.pack('<i',24)+struct.pack('<24q',*offsets)+content)
        self.assertEqual(4,count);self.assertEqual({' ','A'},values)
    def test_corrupt_directory_and_unpinned_emoji_reference_fail_closed(self):
        for content in [b'',struct.pack('<i',100000),struct.pack('<i',24)+struct.pack('<24q',*([-1]*24))]:
            with self.assertRaises(ValueError):tokens(content)
        with self.assertRaisesRegex(ValueError,'pinned'):emoji_sequences(b'unverified reference')
    def test_retained_report_matches_the_shipped_model_identities_not_an_accuracy_claim(self):
        root=Path(__file__).resolve().parents[1]
        report=json.loads((root/'docs/EMOJI-OUTPUT-REPERTOIRE-2026-11-02.json').read_text())
        manifest=json.loads((root/'runtime/src/main/assets/ocr/models.json').read_text())
        self.assertEqual(manifest['commit'],report['model_commit'])
        for name,row in report['models'].items():self.assertEqual(manifest['files'][name]['sha256'],row['sha256'])
        self.assertEqual(3944,report['fully_qualified_emoji_sequences'])
        self.assertEqual(0,report['union_sequences_without_missing_codepoints_necessary_not_sufficient'])
        self.assertIn('No inference, training',report['scope'])

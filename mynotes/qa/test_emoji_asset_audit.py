import unittest
from mynotes.tools.ci import emoji_asset_audit as audit


class EmojiAssetAuditTests(unittest.TestCase):
    def tree(self, *names):
        return {'sha': audit.TREE, 'truncated': False,
                'tree': [{'path': 'assets/svg/' + name + '.svg', 'type': 'blob', 'sha': 'a' * 40} for name in names]}

    def test_only_presentation_selector_is_normalized(self):
        self.assertEqual((0x2764,), audit.normalized([0x2764, 0xfe0f]))
        self.assertEqual((0x1f469, 0x1f3fd, 0x200d, 0x1f4bb),
                         audit.normalized([0x1f469, 0x1f3fd, 0x200d, 0x1f4bb]))
        self.assertEqual((0x31, 0x20e3), audit.normalized([0x31, 0xfe0f, 0x20e3]))

    def test_complete_pinned_metadata_is_mandatory(self):
        for field, value in [('sha', 'a' * 40), ('truncated', True), ('truncated', None)]:
            tree = self.tree('1f600'); tree[field] = value
            with self.assertRaises(ValueError):
                audit.candidates(tree)

    def test_duplicates_bad_scalars_and_paths_fail_closed(self):
        for names in [('1f600', '1f600'), ('110000',), ('d800',), ('../1f600',)]:
            with self.assertRaises(ValueError):
                audit.candidates(self.tree(*names))

    def test_colliding_candidates_are_retained_not_arbitrarily_selected(self):
        result = audit.candidates(self.tree('2764', '2764-fe0f'))
        self.assertEqual(2, len(result[(0x2764,)]))

    def test_assets_are_not_text_model_accuracy(self):
        self.assertEqual(1, len(audit.candidates(self.tree('1f600'))))
        with self.assertRaisesRegex(ValueError, 'pinned Git blob'):
            audit.audit(self.tree('1f600'), b'1F600 ; fully-qualified # fabricated fixture')

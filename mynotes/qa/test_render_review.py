import io
import tarfile
import unittest
from mynotes.tools.ci.render_review import preview_record, fixtures, MAX_IMAGE_BYTES, MAX_IMAGES


class RenderedFixtureReview(unittest.TestCase):
    def archive(self, names):
        stream = io.BytesIO()
        with tarfile.open(fileobj=stream, mode='w') as tar:
            for name in names:
                item = tarfile.TarInfo(name)
                item.size = 3
                tar.addfile(item, io.BytesIO(b'png'))
        stream.seek(0)
        return tarfile.open(fileobj=stream, mode='r')

    def test_regular_fixture_is_read_without_extracting(self):
        with self.archive(['ux-review/drawing.png']) as archive:
            self.assertEqual([('drawing', b'png')], list(fixtures(archive)))

    def test_non_fixture_and_traversal_paths_rejected(self):
        for name in ['../drawing.png', 'ux-review/../drawing.png', '/ux-review/drawing.png', 'private/drawing.png', 'ux-review/Name.png']:
            with self.archive([name]) as archive, self.assertRaises(ValueError):
                list(fixtures(archive))

    def test_empty_and_excessive_count_rejected(self):
        for names in [[], ['ux-review/drawing.png'] * (MAX_IMAGES + 1)]:
            with self.archive(names) as archive, self.assertRaises(ValueError):
                list(fixtures(archive))

    def test_oversize_declared_member_rejected_before_read(self):
        item = tarfile.TarInfo('ux-review/drawing.png')
        item.size = MAX_IMAGE_BYTES + 1
        with self.assertRaises(ValueError):
            list(fixtures([item]))

    def test_symlinks_are_not_followed(self):
        item = tarfile.TarInfo('ux-review/drawing.png')
        item.type = tarfile.SYMTYPE
        item.linkname = '/private/image.png'
        with self.assertRaisesRegex(ValueError, 'No rendered'):
            list(fixtures([item]))

    def test_preview_manifest_is_bounded_and_hashes_real_bytes(self):
        row = preview_record('drawing', b'fixture bytes', 480, 800)
        self.assertNotIn('data', row)
        self.assertEqual(13, row['bytes'])
        self.assertEqual('image/jpeg', row['mime'])
        self.assertEqual(64, len(row['sha256']))
        with self.assertRaises(ValueError):
            preview_record('drawing', b'x' * 46000, 480, 800)

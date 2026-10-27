"""M0 executable reference, not the Android repository or a SAF implementation.
Run tests with python3 -m unittest discover -s qa. Only stdlib dependencies.
"""
import hashlib
import json
import uuid
from pathlib import Path


class Conflict(Exception):
    pass


class IntegrityError(Exception):
    pass


def encode(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'),
                      ensure_ascii=False).encode('utf-8')


class LibraryProbe:
    def __init__(self, root):
        self.root = Path(root)
        self.root.mkdir(parents=True, exist_ok=True)

    def recover(self):
        valid = {}
        candidates = list(self.root.glob('*.json'))
        for path in candidates:
            try:
                envelope = json.loads(path.read_text(encoding='utf-8'))
                body = envelope['body']
                if hashlib.sha256(encode(body)).hexdigest() != envelope['sha256']:
                    continue
                if body['version'] != 1:
                    raise IntegrityError('unsupported library version')
                if (body['id'] != path.stem or not isinstance(body['documents'], dict)
                        or not isinstance(body['parent'], (str, type(None)))):
                    continue
                valid[body['id']] = body
            except (ValueError, KeyError, TypeError):
                continue
        if not valid:
            if candidates:
                raise IntegrityError('no valid generation; do not initialise over data')
            return None, {}
        for item in valid.values():
            parent = item['parent']
            if parent is not None and parent not in valid:
                raise IntegrityError('missing parent generation')
            seen = set()
            cursor = item
            while cursor is not None:
                if cursor['id'] in seen:
                    raise IntegrityError('generation cycle')
                seen.add(cursor['id'])
                cursor = valid.get(cursor['parent'])
        parents = {b['parent'] for b in valid.values()}
        heads = set(valid) - parents
        if len(heads) != 1:
            raise Conflict('divergent heads; explicit reconciliation required')
        head = heads.pop()
        return head, valid[head]['documents']

    def commit(self, documents, expected_head, fail_at=None):
        # Detach caller state and validate JSON before touching persistent state.
        documents = json.loads(encode(documents))
        if not isinstance(documents, dict):
            raise ValueError('documents must be an ID-to-document map')
        head, _ = self.recover()
        if head != expected_head:
            raise Conflict('stale writer')
        body = {'version': 1, 'id': uuid.uuid4().hex,
                'parent': head, 'documents': documents}
        payload = encode({'body': body, 'sha256': hashlib.sha256(encode(body)).hexdigest()})
        path = self.root / (body['id'] + '.json')
        if fail_at == 'before_write':
            raise OSError('simulated unavailable storage')
        with path.open('xb') as f:
            if fail_at == 'partial_write':
                f.write(payload[:len(payload)//2])
                raise OSError('simulated interrupted write')
            f.write(payload)
        if fail_at == 'after_close':
            raise OSError('simulated process death before acknowledgement')
        actual, saved = self.recover()
        if actual != body['id'] or saved != documents:
            raise IntegrityError('readback mismatch')
        return actual

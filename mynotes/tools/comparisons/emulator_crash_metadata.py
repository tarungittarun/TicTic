"""Extract bounded, non-memory metadata from emulator crashpad minidumps.

Raw dumps can contain inherited CI credentials or process memory: never copy
or upload them. Read only header/directory, exception scalars and the basename
of the loaded module containing the exception address. This is crash-location
metadata, not a root-cause diagnosis or evidence of an Android app crash.
"""
from pathlib import Path
import re
import struct


class InvalidDump(ValueError):
    pass


def parse_dump(path):
    path = Path(path)
    size = path.stat().st_size
    with path.open('rb') as file:
        def read(offset, length):
            if offset < 0 or length < 0 or offset > size or length > size - offset:
                raise InvalidDump('Out-of-bounds metadata record')
            file.seek(offset)
            data = file.read(length)
            if len(data) != length:
                raise InvalidDump('Incomplete metadata record')
            return data

        header = read(0, 32)
        if header[:4] != b'MDMP':
            raise InvalidDump('Not a little-endian minidump')
        count, directory = struct.unpack_from('<II', header, 8)
        if count > 128:
            raise InvalidDump('Stream count exceeds diagnostic bound')
        streams = {}
        for index in range(count):
            kind, length, offset = struct.unpack('<III', read(directory + index * 12, 12))
            if kind not in {4, 6}:
                continue  # Never read memory, environment, context or custom streams.
            if kind in streams:
                raise InvalidDump('Ambiguous duplicate metadata stream')
            if offset > size or length > size - offset:
                raise InvalidDump('Out-of-bounds metadata stream')
            streams[kind] = (offset, length)
        result = {'dump_bytes': size, 'exception': None, 'loaded_module_count': None,
                  'raw_memory_retained': False, 'thread_context_read': False}
        if 6 not in streams:
            return result
        offset, length = streams[6]
        if length < 32:
            raise InvalidDump('Incomplete exception stream')
        exception = read(offset, 32)
        code = struct.unpack_from('<I', exception, 8)[0]
        address = struct.unpack_from('<Q', exception, 24)[0]
        result['exception'] = {'code_hex': hex(code), 'address_hex': hex(address),
                               'containing_module': None}
        if 4 not in streams:
            return result
        offset, length = streams[4]
        if length < 4:
            raise InvalidDump('Incomplete module list')
        modules = struct.unpack('<I', read(offset, 4))[0]
        if modules > 4096 or length < 4 + modules * 108:
            raise InvalidDump('Module list exceeds diagnostic bounds')
        result['loaded_module_count'] = modules
        for index in range(modules):
            record = read(offset + 4 + index * 108, 24)
            base, image_size = struct.unpack_from('<QI', record)
            if not base <= address < base + image_size:
                continue
            name_offset = struct.unpack_from('<I', record, 20)[0]
            name_length = struct.unpack('<I', read(name_offset, 4))[0]
            if name_length > 4096 or name_length % 2:
                raise InvalidDump('Invalid module-name bound')
            name = read(name_offset + 4, name_length).decode('utf-16-le', errors='strict')
            basename = name.replace('\\', '/').rsplit('/', 1)[-1]
            # Suppress paths, terminal controls, and nonstandard arbitrary names.
            if not re.fullmatch(r'[A-Za-z0-9_.+\-]{1,128}', basename):
                basename = '[nonstandard name suppressed]'
            module = {'basename': basename, 'image_bytes': image_size,
                      'exception_offset_hex': hex(address - base)}
            if result['exception']['containing_module'] is not None:
                raise InvalidDump('Ambiguous overlapping exception modules')
            result['exception']['containing_module'] = module
        return result


def collect(tmp_root=Path('/tmp')):
    root = Path(tmp_root)
    reports = []
    candidates = []
    for database in sorted(root.glob('android-*/emu-crash-*.db')):
        if database.parent.is_symlink() or database.is_symlink() or not database.is_dir() or not database.resolve().is_relative_to(root.resolve()):
            continue
        for state in ['pending', 'completed', 'new']:
            folder = database / state
            if folder.is_symlink():
                continue
            for path in sorted(folder.glob('*.dmp')):
                if not path.is_symlink() and path.is_file():
                    candidates.append(path)
    for path in candidates[:32]:
        try:
            reports.append(parse_dump(path))
        except (OSError, ValueError, UnicodeError, struct.error) as error:
            reports.append({'metadata_unavailable': type(error).__name__,
                            'raw_memory_retained': False})
    return {'scope': 'Host emulator crash location only; not app fault attribution',
            'raw_dumps_uploaded': False, 'candidate_count': len(candidates),
            'unprocessed_count': max(0, len(candidates) - 32), 'reports': reports}

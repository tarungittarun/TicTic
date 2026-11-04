"""Retain only named public CI fixtures; missing/empty/corrupt capture is evidence, not success."""
from pathlib import Path, PurePosixPath
import hashlib, tarfile
ALLOWED={'image-text-selection.png','word-synthetic-paper-page.png','spreadsheet-grid-viewport.png'}
def retain(archive,destination):
    archive=Path(archive);destination=Path(destination)
    status={'available':False,'retained':[]}
    if not archive.exists():return {**status,'reason':'capture archive missing'}
    status['archive_bytes']=archive.stat().st_size
    if archive.stat().st_size==0:return {**status,'reason':'empty archive; original capture did not complete'}
    if archive.stat().st_size>20000000:return {**status,'reason':'archive exceeds bounded public capture limit'}
    status['archive_sha256']=hashlib.sha256(archive.read_bytes()).hexdigest()
    try:
        with tarfile.open(archive) as bundle:
            members=bundle.getmembers()
            if len(members)>1000:return {**status,'reason':'archive inventory exceeds safety bound'}
            selected=[];seen=set()
            for member in members:
                path=PurePosixPath(member.name)
                if path.is_absolute() or '..' in path.parts:return {**status,'reason':'unsafe archive member path'}
                if member.isfile() and path.name in ALLOWED:
                    if member.size>5000000 or path.name in seen:return {**status,'reason':'unbounded or duplicate public capture'}
                    seen.add(path.name);selected.append(member)
            destination.mkdir(parents=True,exist_ok=True)
            for member in selected:
                data=bundle.extractfile(member).read();(destination/PurePosixPath(member.name).name).write_bytes(data)
                status['retained'].append({'file':PurePosixPath(member.name).name,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()})
    except (tarfile.TarError,OSError) as error:return {**status,'reason':'unreadable capture archive: '+type(error).__name__}
    status['available']=bool(status['retained']);status['reason']='named public captures retained' if status['available'] else 'no named public captures in archive'
    return status

"""Static native-ABI/ELF evidence, deliberately not a physical/16-KiB device certification."""
import json
from pathlib import Path
import struct
import zipfile

ABIS={'armeabi-v7a':(32,40),'arm64-v8a':(64,183),'x86_64':(64,62)}

def elf(data):
    if len(data)<16 or data[:4]!=b'\x7fELF' or data[4] not in (1,2) or data[5] not in (1,2):raise ValueError('Invalid ELF identification')
    bits=32 if data[4]==1 else 64;order='<' if data[5]==1 else '>'
    fmt=order+('16sHHIIIIIHHHHHH' if bits==32 else '16sHHIQQQIHHHHHH')
    if len(data)<struct.calcsize(fmt):raise ValueError('Truncated ELF header')
    header=struct.unpack_from(fmt,data);phoff,entry_size,count=header[5],header[9],header[10]
    phfmt=order+('IIIIIIII' if bits==32 else 'IIQQQQQQ');size=struct.calcsize(phfmt)
    if count>256 or entry_size<size or phoff+entry_size*count>len(data):raise ValueError('Invalid ELF program-header table')
    loads=[];executable_stack=None
    for i in range(count):
        item=struct.unpack_from(phfmt,data,phoff+i*entry_size);kind=item[0];flags=item[6] if bits==32 else item[1]
        if kind==1:
            offset,filesize,memory=(item[1],item[4],item[5]) if bits==32 else (item[2],item[5],item[6])
            alignment=item[7]
            if filesize>memory or offset+filesize>len(data) or alignment and alignment&(alignment-1):raise ValueError('Invalid ELF load segment')
            loads.append({'alignment':alignment,'flags':flags,'writable_executable':bool(flags&1 and flags&2)})
        if kind==0x6474e551:executable_stack=bool(flags&1)
    if not loads:raise ValueError('ELF has no load segments')
    return {'bits':bits,'machine':header[2],'load_segments':loads,'elf_16k_segment_alignment':all(s['alignment']>=16384 for s in loads),'executable_stack':executable_stack}

def inventory(apk,expected_abis=None):
    result={};seen=set()
    with zipfile.ZipFile(apk) as archive:
        for info in archive.infolist():
            parts=info.filename.split('/')
            if len(parts)!=3 or parts[0]!='lib' or not parts[2].endswith('.so'):continue
            if info.filename in seen:raise ValueError('Duplicate native ZIP entry')
            seen.add(info.filename);abi=parts[1]
            if abi not in ABIS:raise ValueError('Unreviewed native ABI: '+abi)
            parsed=elf(archive.read(info))
            if (parsed['bits'],parsed['machine'])!=ABIS[abi]:raise ValueError('Native ELF architecture disagrees with ZIP ABI: '+info.filename)
            parsed.update({'file':parts[2],'bytes':info.file_size,'compressed':info.compress_type!=zipfile.ZIP_STORED})
            result.setdefault(abi,[]).append(parsed)
    if expected_abis is not None and set(result)!=set(expected_abis):raise ValueError('Required native ABI set missing or changed')
    for entries in result.values():entries.sort(key=lambda entry:entry['file'])
    return {'schema':1,'abis':result,'scope':'Static ABI/ELF architecture, load alignment and stack flags. ELF alignment is necessary but not sufficient for 16 KiB compatibility; no ARM/16-KiB physical-device execution or transitive CVE audit is implied.'}

def write(apk,output):
    result=inventory(apk,ABIS);Path(output).write_text(json.dumps(result,indent=2)+'\n')
    summary={'abis':{abi:len(entries) for abi,entries in result['abis'].items()},'low_16k_alignment':[abi+'/'+entry['file'] for abi,entries in result['abis'].items() if ABIS[abi][0]==64 for entry in entries if not entry['elf_16k_segment_alignment']],'scope':result['scope']}
    print('::notice title=Native ABI and ELF inventory::'+json.dumps(summary).replace('%','%25'))
    return result

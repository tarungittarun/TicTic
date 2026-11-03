"""Ephemeral same-key debug instrumentation interoperability; verify ALL unsigned payload entries.
Never changes or publishes a user release APK; retained proof distinguishes signatures from payload.
"""
import argparse,hashlib,json,os,re,subprocess,zipfile
from pathlib import Path

def payload(file):
    entries={}
    with zipfile.ZipFile(file) as apk:
        for entry in apk.infolist():
            if re.fullmatch(r'META-INF/(MANIFEST\.MF|[^/]+\.(SF|RSA|DSA|EC))',entry.filename,re.I):continue
            if entry.filename in entries:raise ValueError('Duplicate APK entry')
            entries[entry.filename]=hashlib.sha256(apk.read(entry)).hexdigest()
    return entries

def main():
    p=argparse.ArgumentParser();p.add_argument('app',type=Path);p.add_argument('test',type=Path);p.add_argument('--baseline',action='store_true');args=p.parse_args()
    if args.baseline and hashlib.sha256(args.app.read_bytes()).hexdigest()!='07bfb4f062124dd97d3e0bd29a2524004e62be7c8d975adabd60cfba025d897d':raise ValueError('Baseline58 bytes mismatch')
    tools=Path(os.environ['ANDROID_HOME'])/'build-tools/35.0.0';key=Path('ephemeral-benchmark.jks')
    subprocess.run(['keytool','-genkeypair','-keystore',str(key),'-storepass','android','-keypass','android','-alias','benchmark','-keyalg','RSA','-validity','2','-dname','CN=Public synthetic benchmark'],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    proof={'scope':'Fresh synthetic guest only. Debug APKs re-signed together for cross-build instrumentation; every non-signature ZIP entry is byte-identical. Not a user release APK or a rebuilt baseline.','baseline58':args.baseline,'files':[]}
    for label,file in [('app',args.app),('test',args.test)]:
        before=payload(file);out=Path('benchmark-'+label+'.apk');subprocess.run([str(tools/'apksigner'),'sign','--ks',str(key),'--ks-pass','pass:android','--key-pass','pass:android','--out',str(out),str(file)],check=True)
        if payload(out)!=before:raise ValueError('Re-sign changed unsigned payload')
        subprocess.run([str(tools/'apksigner'),'verify',str(out)],check=True)
        manifest=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(out)],text=True).splitlines()[0]
        proof['files'].append({'role':label,'original_sha256':hashlib.sha256(file.read_bytes()).hexdigest(),'resigned_sha256':hashlib.sha256(out.read_bytes()).hexdigest(),'payload_entries':len(before),'unsigned_payload_sha256':hashlib.sha256(json.dumps(before,sort_keys=True).encode()).hexdigest(),'manifest':manifest})
    key.unlink();Path('benchmark-signing-proof.json').write_text(json.dumps(proof,indent=2)+'\n')
if __name__=='__main__':main()

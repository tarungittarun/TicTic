"""Post-render layout/retention audit, not a model or physical usability approval."""
import argparse,hashlib,json
from pathlib import Path

def audit(folder):
    import fitz
    summary=json.loads((folder/'report-summary.json').read_text());data=json.loads((folder/'provenance.json').read_text());pdf=folder/'MyNotes-Module-E2E-Report.pdf'
    if hashlib.sha256(pdf.read_bytes()).hexdigest()!=summary['pdf_sha256']:raise ValueError('PDF hash mismatch')
    document=fitz.open(pdf);toc=document.get_toc();issues=[];blocks=0
    for page in document:
        for block in page.get_text('blocks'):
            if block[6]!=0:continue
            blocks+=1;x0,y0,x1,y1=block[:4]
            if min(x0,y0)<-1 or x1>page.rect.width+1 or y1>page.rect.height+1:issues.append({'page':page.number+1,'box':list(block[:4]),'text':block[4][:160]})
    for index,entry in enumerate(summary['module_order']):
        if [1,f'{index+1:02d}. '+entry['module'],entry['page']] not in toc:issues.append({'bookmark':entry})
    hashes=0
    for frame in data['screens']:
        if hashlib.sha256((folder/frame['path']).read_bytes()).hexdigest()!=frame['sha256']:raise ValueError('Retained image hash mismatch: '+frame['path'])
        hashes+=1
    result={'scope':'PDF bounds/bookmarks/retained-image hashes only; not automatic contrast, OCR accuracy or physical approval','pages':len(document),'expected_pages':summary['pages'],'text_blocks_checked':blocks,'images_hash_verified':hashes,'ordered_modules':len(summary['module_order']),'issues':issues}
    (folder/'pdf-layout-audit.json').write_text(json.dumps(result,indent=2)+'\n')
    if issues or len(document)!=summary['pages']:raise ValueError('PDF layout audit failed: '+json.dumps(issues[:6]))
    return result

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('folder',type=Path);a=p.parse_args();print(json.dumps(audit(a.folder),indent=2))

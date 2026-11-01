"""Nonshipping fast/best Tesseract weight comparison on synthetic system fonts.
Not training, a held-out real-photo corpus, Android latency, or emoji support.
"""
import base64, hashlib, json, re, subprocess, time, unicodedata
from pathlib import Path

BEST={'eng':('176dc3220de7db34d3b3aecbfa42043a6038348b',15400601),'hin':('bb1e1c2da68a2b56d74fdf49a9dd53e7198b3302',11895564),'kan':('8f2e98e1d36ee516a1638522e569d2c4d11ac86d',10233763)}
TEXT={'eng':['TOTAL 25 + 5 = 30','EMAIL @ HOME 50%'],'hin':['भारत नमस्ते','कल बैठक सुबह 10 बजे'],'kan':['ಕನ್ನಡ ಭಾಷೆ','ಸಭೆ ಬೆಳಗ್ಗೆ 10 ಗಂಟೆಗೆ']}
FONTS={'eng':'NotoSans-Regular.ttf','hin':'NotoSansDevanagari-Regular.ttf','kan':'NotoSansKannada-Regular.ttf'}

def cer(reference,hypothesis):
    # Whitespace normalization only: keep case, symbols and every Unicode codepoint.
    a=unicodedata.normalize('NFC',re.sub(r'\s+',' ',reference.strip()));b=unicodedata.normalize('NFC',re.sub(r'\s+',' ',hypothesis.strip()));previous=list(range(len(b)+1))
    for i,x in enumerate(a,1):
        row=[i]
        for j,y in enumerate(b,1):row.append(min(previous[j]+1,row[-1]+1,previous[j-1]+(x!=y)))
        previous=row
    return {'cer':previous[-1]/max(1,len(a)),'character_edits':previous[-1],'reference_codepoints':len(a)}

def main():
    from PIL import Image,ImageDraw,ImageFont,ImageFilter,features
    if not features.check_feature('raqm'):raise ValueError('Complex-script shaping support required for valid corpus')
    root=Path('.cache/ocr-weight-research');root.mkdir(parents=True,exist_ok=True);receipts=[];rows=[]
    manifest=json.loads(Path('mynotes/runtime/src/main/assets/ocr/models.json').read_text())
    for family in ['fast','best']:
        directory=root/family;directory.mkdir(exist_ok=True)
        for language in TEXT:
            info=manifest['files'][language+'.traineddata'];blob,size=(info['git_blob'],info['bytes']) if family=='fast' else BEST[language]
            obj=json.loads(subprocess.check_output(['gh','api',f'repos/tesseract-ocr/tessdata_{family}/git/blobs/{blob}'],text=True));data=base64.b64decode(obj['content'])
            if len(data)!=size or hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()!=blob:raise ValueError('Pinned OCR model blob mismatch')
            digest=hashlib.sha256(data).hexdigest()
            if family=='fast' and digest!=info['sha256']:raise ValueError('Production model checksum mismatch')
            (directory/(language+'.traineddata')).write_bytes(data);receipts.append({'family':family,'language':language,'git_blob':blob,'bytes':size,'sha256_observed':digest})
    for language,phrases in TEXT.items():
        font_path=Path('/usr/share/fonts/truetype/noto')/FONTS[language]
        if not font_path.is_file():raise ValueError('Required shaping font absent: '+str(font_path))
        font=ImageFont.truetype(str(font_path),72)
        for index,reference in enumerate(phrases):
            for variant in ['clean','low-contrast']:
                image=Image.new('RGB',(1600,240),'white');draw=ImageDraw.Draw(image);draw.text((50,60),reference,font=font,fill='black' if variant=='clean' else (120,120,120))
                if variant=='low-contrast':image=image.filter(ImageFilter.GaussianBlur(.4))
                path=root/f'{language}-{index}-{variant}.png';image.save(path)
                for family in ['fast','best']:
                    began=time.perf_counter();text=subprocess.check_output(['tesseract',str(path),'stdout','--tessdata-dir',str(root/family),'-l',language,'--oem','1','--psm','7'],text=True,timeout=60).strip()
                    row={'family':family,'language':language,'case':index,'variant':variant,'reference':reference,'recognized':text,'process_seconds':time.perf_counter()-began,'image_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'font_sha256':hashlib.sha256(font_path.read_bytes()).hexdigest()};row.update(cer(reference,text));rows.append(row)
    report={'schema':1,'scope':'24 desktop single-line synthetic system-font inferences, six phrases, clean/low-contrast. NOT training, real photos, handwriting/emoji support or Android performance. CER uses NFC and whitespace only; Hindi/Kannada counts Unicode codepoints, not grapheme clusters. This shared training-like font corpus cannot establish generalization.','tesseract_version':subprocess.check_output(['tesseract','--version'],text=True).splitlines()[0],'raqm_shaping_available':features.check_feature('raqm'),'models':receipts,'results':rows}
    Path('ocr-weight-research.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print('::notice title=OCR research receipt::'+json.dumps({k:v for k,v in report.items() if k!='results'},ensure_ascii=False))
    print('::notice title=OCR weight research grouped::'+json.dumps(rows,ensure_ascii=False))
if __name__=='__main__':main()

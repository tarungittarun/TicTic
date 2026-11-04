"""Independent additional-scene ingestion; never adds to the original4752 cohort."""
import json,shutil
from pathlib import Path
from mynotes.tools.ci.results import android
from mynotes.tools.ci.smallfont_scene_report import independent_row
PROFILES=['light-portrait','dark-portrait','light-landscape','dark-landscape']
NAMES=['app.space.notes.EnvironmentQualificationTest#'+m for m in ['fontScaleMatchesRequestedLane','orientationMatchesRequestedLane','nightResourcesMatchRequestedLane']]+['app.space.notes.AdditionalPublicSceneStudy#originalPosterAndObliqueUnsupportedLatinSignsRetainAllOutputs']
def collect(artifacts,output,manifest):
    frozen=json.loads(Path(manifest).read_text());expected={r['id']:r for r in frozen['records']};assert len(expected)==2
    result={'expected':8,'observed':0,'complete':True,'profiles':{},'rows':[],'scope':frozen['scope'],'physical_acceptance':False,'production_changed':False}
    dest=Path(output)/'additional-public-scenes';dest.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(manifest,dest/'manifest.json')
    for profile in PROFILES:
        folder=Path(artifacts)/('module-benchmark-'+profile+'-ocr');target=dest/profile;target.mkdir(exist_ok=True)
        for name in ['additional-public-scenes.json','additional-public-native.txt','additional-public-native-verified.json']:
            source=folder/name
            if source.exists():
                if source.stat().st_size>5000000:raise ValueError('Unbounded additional study artifact')
                shutil.copyfile(source,target/name)
        log=target/'additional-public-native.txt';native=android(log.read_text() if log.exists() else '',NAMES)
        source=target/'additional-public-scenes.json';data=json.loads(source.read_text()) if source.exists() else {'cases':[]}
        rows=data['cases'];complete=native['ok'] and data.get('expected')==2 and len(rows)==2 and {r['id'] for r in rows}==set(expected)
        if rows and (data.get('production_changed') is not False or data.get('physical_acceptance') is not False):raise ValueError('False study scope')
        for row in rows:
            original=expected[row['id']]
            if row['source_sha256']!=original['sha256'] or row.get('reference',original['reference'])!=original['reference']:raise ValueError('Frozen source/reference differs')
            if row['status']=='OK':independent_row(row)
            else:complete=False
            result['rows'].append({**row,'profile':profile})
        result['profiles'][profile]={'complete':bool(complete),'native':native,'observed':len(rows)};result['complete']=bool(result['complete'] and complete)
    result['observed']=len(result['rows']);result['exact']=sum(r.get('character_edits')==0 for r in result['rows'])
    (dest/'independent-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');return result

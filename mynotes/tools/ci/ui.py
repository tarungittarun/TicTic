"""Strict, bounded UIAutomator node lookup for real release-APK upgrade seeding."""
import argparse
import re
import xml.etree.ElementTree as ET

def coordinates(xml,text):
    all_nodes=[n for n in ET.fromstring(xml).iter('node') if n.get('enabled')=='true']
    aliases={'+ Text':'New text note'}
    nodes=[n for n in all_nodes if n.get('content-desc')==aliases.get(text,text)]
    if not nodes:nodes=[n for n in all_nodes if n.get('text')==text or n.get('content-desc')==text]
    if len(nodes)!=1:raise ValueError(f'Expected one enabled node {text!r}, found {len(nodes)}')
    match=re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',nodes[0].get('bounds',''))
    if not match:raise ValueError('Unusable node bounds')
    left,top,right,bottom=map(int,match.groups())
    if right<=left or bottom<=top:raise ValueError('Empty node bounds')
    return (left+right)//2,(top+bottom)//2

if __name__=='__main__':
    from pathlib import Path
    p=argparse.ArgumentParser();p.add_argument('--xml',required=True);p.add_argument('--text',required=True);a=p.parse_args()
    try:print(*coordinates(Path(a.xml).read_text(),a.text))
    except ValueError as error:raise SystemExit(str(error))

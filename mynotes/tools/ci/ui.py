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

def known_launcher_anr(xml):
    """Only the observed AOSP Quickstep system dialog; never dismiss a MyNotes ANR."""
    nodes=list(ET.fromstring(xml).iter('node'))
    titles=[n for n in nodes if n.get('package')=='android' and n.get('resource-id')=='android:id/alertTitle' and n.get('text')=="Quickstep isn't responding"]
    buttons=[n for n in nodes if n.get('package')=='android' and n.get('resource-id')=='android:id/aerr_close' and n.get('text')=='Close app' and n.get('enabled')=='true' and n.get('clickable')=='true']
    if len(titles)!=1 or len(buttons)!=1:raise ValueError('No exact known launcher ANR')
    # Reuse strict unique enabled-node and nonempty-bound checks.
    return coordinates(ET.tostring(buttons[0],encoding='unicode'),'Close app')

if __name__=='__main__':
    from pathlib import Path
    p=argparse.ArgumentParser();p.add_argument('--xml',required=True);choice=p.add_mutually_exclusive_group(required=True);choice.add_argument('--text');choice.add_argument('--known-launcher-anr',action='store_true');a=p.parse_args()
    try:print(*(known_launcher_anr(Path(a.xml).read_text()) if a.known_launcher_anr else coordinates(Path(a.xml).read_text(),a.text)))
    except ValueError as error:raise SystemExit(str(error))

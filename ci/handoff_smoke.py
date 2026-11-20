"""Synthetic receiver smoke. Runs after the original smoke, then clears its synthetic data."""
import json, re, subprocess, time
import xml.etree.ElementTree as ET
from pathlib import Path
P = 'com.vanipy.jumpa'
OUT = Path('ci/shots')

def adb(*args, check=True, data=None):
    r = subprocess.run(['adb', *args], input=data, capture_output=True)
    if check and r.returncode: raise AssertionError(r.stderr.decode())
    return r.stdout

def ui():
    for _ in range(10):
        adb('shell', 'uiautomator', 'dump', '/sdcard/handoff.xml', check=False)
        try: return [n.attrib for n in ET.fromstring(adb('exec-out', 'cat', '/sdcard/handoff.xml')).iter('node')]
        except ET.ParseError: time.sleep(1)
    raise AssertionError('UI XML unavailable')

def tap(text):
    for _ in range(6):
        for n in ui():
            if n.get('text') == text:
                x1,y1,x2,y2 = map(int,re.findall(r'\d+',n['bounds']))
                adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(1);return
        adb('shell','input','swipe','540','1750','540','600','300');time.sleep(1)
    raise AssertionError('missing '+text)

def shot(name):
    (OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'))

def launch(uri='jumpa://encounter/demo-v1'):
    adb('shell','am','force-stop',P)
    adb('shell','am','start','-W','-a','android.intent.action.VIEW','-c','android.intent.category.BROWSABLE','-d',uri,'-p',P)
    time.sleep(2)

def read(name):
    b=adb('exec-out','run-as',P,'cat','files/'+name,check=False)
    return json.loads(b) if b else None

def write(path,data):
    adb('shell','run-as',P,'mkdir','-p','files')
    adb('shell','run-as',P,'sh','-c',"'cat > "+path+"'",data=data)

def reset(): adb('shell','pm','clear',P)

# A canceled preview must not seed even the demo file.
reset();launch();shot('10-handoff-preview')
assert read('people-demo.json') is None
assert read('people.json') is None
adb('shell','input','keyevent','4');time.sleep(1)
assert read('people-demo.json') is None

# Invalid payloads cannot enter the preview or write anything.
launch('jumpa://encounter/demo-v1?name=unexpected')
assert any('Unsupported handoff' in n.get('text','') for n in ui())
assert read('people-demo.json') is None

# A new fictional person plus one note is committed only on Save.
launch();tap('Save to demo people');shot('11-handoff-saved')
saved=read('people-demo.json');assert len(saved['people']) == 4
new=next(p for p in saved['people'] if p['name']=='Rani Demo')
assert len(new['notes'])==1 and 'not proof of attendance' in new['notes'][0]['text']
assert new['encounterIds']==['kunjung-demo-design-2026-11']
assert read('people.json') is None
launch();assert any('already saved' in n.get('text','') for n in ui());shot('12-handoff-duplicate')
assert read('people-demo.json')==saved

# Existing archived person chosen explicitly: append only, never restore/rename.
reset()
original={'version':1,'people':[{'id':'archive-demo','name':'Archived Demo','archived':True,'updated':1,'affiliations':[],'notes':[],'promises':[]}]}
write('files/people-demo.json',json.dumps(original).encode())
# Sentinel real store and a real-mode preference are synthetic test fixtures.
real={'version':1,'people':[{'id':'real-sentinel','name':'Synthetic sentinel, do not change'}]}
write('files/people.json',json.dumps(real).encode())
adb('shell','run-as',P,'mkdir','-p','shared_prefs')
write('shared_prefs/jumpa.xml',b'<?xml version="1.0" encoding="utf-8"?><map><boolean name="demoMode" value="false" /></map>')
launch()
for n in ui():
    if n.get('class')=='android.widget.Spinner':
        x1,y1,x2,y2=map(int,re.findall(r'\d+',n['bounds']));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));break
else: raise AssertionError('person chooser missing')
time.sleep(1);tap('Archived Demo (archived)')
shot('13-handoff-existing-preview');tap('Save to demo people');shot('14-handoff-existing-saved')
saved=read('people-demo.json');assert len(saved['people'])==1
p=saved['people'][0];assert p['archived'] and p['name']=='Archived Demo' and len(p['notes'])==1
assert read('people.json')==real
prefs=adb('exec-out','run-as',P,'cat','shared_prefs/jumpa.xml').decode()
assert 'name="demoMode" value="false"' in prefs

# A damaged demo file must not be overwritten.
reset();write('files/people-demo.json',b'broken synthetic file');launch()
assert any('Could not read demo people' in n.get('text','') for n in ui())
assert adb('exec-out','run-as',P,'cat','files/people-demo.json')==b'broken synthetic file'
print('PASS: cancel, unsupported payload, create, duplicate, existing archived append, real-data isolation, mode preservation, corrupt-store protection')

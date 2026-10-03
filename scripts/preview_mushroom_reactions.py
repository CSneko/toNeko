"""Preview the generated game rig using Purwhite's existing Three.js viewer.

python3 scripts/preview_mushroom_reactions.py
python3 scripts/preview_mushroom_reactions.py --capture
The artist's reference assets remain untouched. Capture uses an isolated headless Chrome.
"""
from pathlib import Path
import argparse
import base64
import json
import shutil
import subprocess
import tempfile
import threading
import time
from functools import partial
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from urllib.request import urlopen
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'common/src/main/resources/assets/toneko'
OUT = ROOT / 'build/mushroom-reactions'
REF = ROOT / 'refer/purwhite/preview'


def build_preview():
    OUT.mkdir(parents=True, exist_ok=True)
    shutil.copytree(REF / 'vendor', OUT / 'vendor', dirs_exist_ok=True)
    shutil.copy2(ASSETS / 'textures/entity/purwhite.png', OUT / 'purwhite.png')
    geometry = json.loads((ASSETS / 'geckolib/models/entity/purwhite.geo.json').read_text())
    animations = json.loads((ASSETS / 'geckolib/animations/entity/purwhite.animation.json').read_text())
    (OUT / 'model-data.js').write_text('export const geometry=' + json.dumps(geometry) + ';\n'
        'export const animationFile=' + json.dumps(animations) + ';\nexport const textureURL="./purwhite.png";\n')
    html = (REF / 'index.html').read_text()
    html = html.replace('<select id="animation"></select>', '<select id="animation"></select>'
        '<p class="label" style="margin-top:18px">表情</p><select id="expression"></select>')
    html = html.replace('../reference/turnaround.png', './reference.png')
    shutil.copy2(ROOT / 'refer/purwhite/reference/turnaround.png', OUT / 'reference.png')
    (OUT / 'index.html').write_text(html)
    viewer = (REF / 'viewer.js').read_text()
    viewer = viewer.replace('function pose(t){', '''let expression=params.get('face')||'idle';
const expressionSelect=document.querySelector('#expression');
for(const name of Object.keys(anims).filter(n=>n.includes('.face_'))){
  const value=name.split('face_')[1],option=document.createElement('option');
  option.value=value;option.textContent=({admire:'敬佩',displeased:'不高兴',blush:'脸红',fear:'害怕',idle:'平常'})[value]||value;
  expressionSelect.appendChild(option);
}
expressionSelect.value=expression;
expressionSelect.onchange=()=>{expression=expressionSelect.value;render();};
function pose(t){
  const face=anims['animation.purwhite.face_'+expression];
  const ft=t%face.animation_length;''')
    viewer = viewer.replace('ch=a.bones[b.name]||{};', 'fromFace=face.bones[b.name],ch=fromFace||a.bones[b.name]||{};\n    const bt=fromFace?ft:at;')
    viewer = viewer.replace('sample(ch.position,at,', 'sample(ch.position,bt,').replace('sample(ch.rotation,at,', 'sample(ch.rotation,bt,').replace('sample(ch.scale,at,', 'sample(ch.scale,bt,')
    viewer = viewer.replace('if(blinkEnabled&&', 'if(!face&&blinkEnabled&&')
    viewer = viewer.replace('ready:true,renderer,scene,camera,nodes,', 'ready:true,renderer,scene,camera,nodes,controls,')
    viewer = viewer.replace("  document.querySelector('#timeline').value=at/a.animation_length;", '''  for(const side of ['left','right']){
    const closed=nodes['eye_'+side].scale.y<.15;
    nodes['eye_'+side].visible=!closed;
    nodes['eye_lid_'+side].visible=closed&&nodes['eye_lid_'+side].scale.x>=.5;
    for(const variant of ['happy','displeased','sparkle'])nodes['eye_'+variant+'_'+side].visible=nodes['eye_'+variant+'_'+side].scale.x>=.5;
  }
  const mouths=['mouth_smile','mouth_soft','mouth_open','mouth_o','mouth_pout'];
  const best=mouths.reduce((a,b)=>nodes[a].scale.x>=nodes[b].scale.x?a:b);
  for(const name of mouths)nodes[name].visible=name===best;
  nodes.blush_shy.visible=nodes.blush_shy.scale.x>=.5;
  nodes.sleep_cap.visible=selected.endsWith('.sleep');
  nodes.hips.visible=!selected.endsWith('.sleep');
  document.querySelector('#timeline').value=at/a.animation_length;''')
    (OUT / 'viewer.js').write_text(viewer)
    print(f'Preview: {OUT / "index.html"} (serve this directory with python3 -m http.server)')


def capture():
    import websocket
    chrome = shutil.which('google-chrome') or shutil.which('chromium')
    if not chrome:
        raise RuntimeError('Chrome is required for --capture')
    server = ThreadingHTTPServer(('127.0.0.1', 0), partial(SimpleHTTPRequestHandler, directory=str(OUT)))
    threading.Thread(target=server.serve_forever, daemon=True).start()
    url = f'http://127.0.0.1:{server.server_port}/index.html?capture'
    with tempfile.TemporaryDirectory(prefix='purwhite-preview-') as profile:
        log = (OUT / 'chrome.log').open('w')
        process = subprocess.Popen([chrome, '--headless', '--no-sandbox', '--enable-unsafe-swiftshader',
            '--use-angle=swiftshader', '--remote-debugging-port=0', '--remote-allow-origins=*',
            '--user-data-dir=' + profile, 'about:blank'], stdout=log, stderr=log)
        ws = None
        try:
            port_file = Path(profile) / 'DevToolsActivePort'
            for _ in range(100):
                if port_file.exists(): break
                time.sleep(.1)
            port = int(port_file.read_text().splitlines()[0])
            tabs = json.load(urlopen(f'http://127.0.0.1:{port}/json'))
            ws = websocket.create_connection(next(t['webSocketDebuggerUrl'] for t in tabs if t['type']=='page'), timeout=30)
            seq = 0
            def cdp(method, params=None):
                nonlocal seq
                seq += 1
                ws.send(json.dumps({'id':seq,'method':method,'params':params or {}}))
                while True:
                    message=json.loads(ws.recv())
                    if message.get('id')==seq:
                        if 'error' in message: raise RuntimeError(message['error'])
                        return message.get('result',{})
            def js(code):
                response=cdp('Runtime.evaluate',{'expression':code,'returnByValue':True,'awaitPromise':True})
                if 'exceptionDetails' in response: raise RuntimeError(response['exceptionDetails'])
                return response.get('result',{}).get('value')
            cdp('Emulation.setDeviceMetricsOverride',{'width':600,'height':640,'deviceScaleFactor':1,'mobile':False})
            renders=OUT/'renders'; renders.mkdir(exist_ok=True)
            samples=[('admire','idle','敬佩',True),('displeased','idle','不高兴',True),('blush','sit_on_player','脸红',True),
                     ('fear','cower','抱头蹲防',False),('fear','tremble','墙角发抖',False),('idle','inspect','好奇观察',False)]
            paths=[]
            for i,(face,body,label,close) in enumerate(samples):
                view='front' if close or body=='inspect' else 'hero'
                cdp('Page.navigate',{'url':url+f'&face={face}&animation=animation.purwhite.{body}&view={view}&time=0.2'})
                for _ in range(100):
                    if js('Boolean(window.previewApi?.ready)'): break
                    time.sleep(.05)
                else: raise RuntimeError('Preview failed to load')
                if close:
                    target=24.5-(8.6 if body=='sit_on_player' else 0)
                    js(f'previewApi.setView("front");previewApi.camera.zoom=2.35;previewApi.camera.position.y={target};'
                       f'previewApi.controls.target.set(0,{target},0);previewApi.controls.update();previewApi.camera.updateProjectionMatrix();'
                       'previewApi.renderer.render(previewApi.scene,previewApi.camera);')
                time.sleep(.08)
                path=renders/f'{body}-{face}.png'
                path.write_bytes(base64.b64decode(cdp('Page.captureScreenshot',{'format':'png'})['data']))
                paths.append((path,label))
            sheet=Image.new('RGB',(1200,940),'#F1EDF8');d=ImageDraw.Draw(sheet)
            font=ImageFont.truetype('/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc',24)
            for i,(path,label) in enumerate(paths):
                pic=Image.open(path).convert('RGB').resize((400,426),Image.Resampling.LANCZOS)
                x,y=(i%3)*400,(i//3)*470
                sheet.paste(pic,(x,y));d.text((x+200,y+442),label,font=font,fill='#7861B4',anchor='mm')
            sheet.save(ROOT/'docs/images/purwhite-reactions.png')
            print('Captured docs/images/purwhite-reactions.png')
        finally:
            if ws: ws.close()
            process.terminate(); process.wait(timeout=10); log.close(); server.shutdown(); server.server_close()


if __name__ == '__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--capture',action='store_true');args=parser.parse_args()
    build_preview()
    if args.capture: capture()

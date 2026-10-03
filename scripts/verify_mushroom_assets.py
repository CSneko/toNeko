"""Validate mushroom resource references, animation channels, and survival crop drops."""
from pathlib import Path
import json
from itertools import product
from math import cos, sin, radians, sqrt
from PIL import Image
ROOT = Path(__file__).resolve().parents[1]
R = ROOT/'common/src/main/resources'
A = R/'assets/toneko'
D = R/'data/toneko'
checks = 0
def check(condition, message):
    global checks
    checks += 1
    if not condition: raise AssertionError(message)
def read(path): return json.loads(path.read_text())
check('client.HallucinationCameraMixin' in read(R/'toneko.mixins.json')['client'], 'Missing hallucination camera mixin')
check('client.BedRestCameraMixin' in read(R/'toneko.mixins.json')['client'], 'Missing awake bed camera mixin')
check('client.BedRestBodyMixin' in read(R/'toneko.mixins.json')['client'], 'Missing awake bed body rendering mixin')
check('MushroomPlayerRidingMixin' in read(R/'toneko.mixins.json')['mixins'], 'Missing transient player companionship mixin')
check('HallucinationProjectileMixin' in read(R/'toneko.mixins.json')['mixins'], 'Missing server projectile disorientation')
geo = read(A/'geckolib/models/entity/purwhite.geo.json')['minecraft:geometry'][0]
names = [b['name'] for b in geo['bones']]
check(len(names) == len(set(names)), 'Duplicate bones')
for bone in geo['bones']:
    check(bone.get('parent') is None or bone['parent'] in names, 'Unknown parent')
    visited=set();cursor=bone
    while cursor.get('parent'):
        check(cursor['name'] not in visited, 'Cyclic bone hierarchy')
        visited.add(cursor['name']);cursor=next(b for b in geo['bones'] if b['name']==cursor['parent'])
check('sleep_cap' in names, 'Missing resting cap')
animations=read(A/'geckolib/animations/entity/purwhite.animation.json')['animations']
for name, anim in animations.items():
    check(anim['animation_length'] > 0, 'Zero animation length')
    for bone, channels in anim['bones'].items():
        check(bone in names, f'Unknown animated bone {bone}')
        for channel, values in channels.items():
            check(channel in ('position','rotation','scale'), f'Unknown channel {channel}')
            if isinstance(values,dict):
                for time,value in values.items():
                    check(0 <= float(time) <= anim['animation_length'], f'Key outside timeline: {name}')
                    vector=value if isinstance(value,list) else value.get('vector')
                    check(len(vector)==3 and all(isinstance(x,(int,float)) for x in vector), 'Malformed vector')
for name in ('idle','blink','walk','sit','sleep','sleep_eyes','drowsy','yawn','spores','wave','shy','happy', 'carry', 'sit_on_player', 'lie_on_player', 'cower', 'tremble', 'inspect'):
    check('animation.purwhite.'+name in animations, f'Missing animation {name}')
check(Image.open(A/'textures/entity/purwhite.png').size == (1024,1024), 'Wrong model atlas size')
# Regressions for the visible face: no slabs protruding from the skin, no overlapping controllers,
# and preserve the reference's distinctive blocky purple eye artwork.
atlas = Image.open(A/'textures/entity/purwhite.png').convert('RGBA')
reference = Image.open(ROOT/'refer/purwhite/model/purwhite.png').convert('RGBA')
reference_bones = {b['name']: b for b in read(ROOT/'refer/purwhite/model/purwhite.geo.json')['minecraft:geometry'][0]['bones']}
by_name = {b['name']: b for b in geo['bones']}
facial = {name for name in names if name.startswith(('eye_', 'iris_', 'mouth', 'brow_', 'blush_'))}
for name in facial:
    for cube in by_name[name].get('cubes', []):
        check(cube['size'][2] <= 0.002, f'Facial slab protrudes: {name}')
        check(-3.51 <= cube['origin'][2] < -3.5, f'Face layer detached from skin: {name}')
for name, anim in animations.items():
    if not name.startswith('animation.purwhite.face_') and not name.endswith(('.blink', '.sleep_eyes')):
        check(not facial.intersection(anim['bones']), f'Body animation overwrites expression: {name}')
for name in ('idle','drowsy','shy','happy','wave','wink','yawn','spores','surprised','angry','talk','sleep','admire','displeased','blush','fear'):
    check('animation.purwhite.face_'+name in animations, f'Missing face state {name}')
def front_texture(bone):
    uv = bone['cubes'][0]['uv']['north']; x,y = uv['uv']; w,h = uv['uv_size']
    return atlas.crop((x,y,x+w,y+h))
for side in ('left','right'):
    eye = by_name['eye_'+side]
    composed = Image.alpha_composite(front_texture(eye), front_texture(by_name['iris_'+side]))
    uv = reference_bones['eye_'+side]['cubes'][0]['uv']['north']; x,y = uv['uv']; w,h = uv['uv_size']
    expected = reference.crop((x,y,x+w,y+h))
    # Skin margins become transparent so the underlying head supplies the same skin color.
    for y in range(h):
        for x in range(w):
            if expected.getpixel((x,y)) == (255,238,229,255): expected.putpixel((x,y),(0,0,0,0))
    check(composed.tobytes() == expected.tobytes(), f'Original purple eye redrawn: {side}')
    gaze = animations['animation.purwhite.face_idle']['bones']['iris_'+side]['position']
    check(len({tuple(v['vector']) for v in gaze.values()}) > 1, 'Missing gaze movement')
    check(all(v['vector'][2] == 0 for v in gaze.values()), 'Gaze pushes iris off face')
    check(eye['cubes'][0]['size'][:2] == reference_bones['eye_'+side]['cubes'][0]['size'][:2], 'Original eye proportions changed')
mouth_motion = animations['animation.purwhite.face_talk']['bones']['mouth']['scale']
for mouth in ('smile','soft','open','o','pout'):
    check(by_name['mouth_'+mouth]['cubes'][0]['size'][:2] == [1.8,1.62], 'Mouth must be 1.5 times the old 1.2 by 1.08 plane')
    pixels = front_texture(by_name['mouth_'+mouth])
    if mouth in ('open','o'):
        colors = {pixel for pixel in pixels.getdata() if pixel[3]}
        check(len(colors) == 1, 'Open mouths must be a flat cavity without a contrasting border')
for side in ('left','right'):
    check(animations['animation.purwhite.face_admire']['bones']['eye_sparkle_'+side]['scale'] == [1,1,1], 'Admiration needs starry eyes')
    check(animations['animation.purwhite.face_displeased']['bones']['eye_displeased_'+side]['scale'] == [1,1,1], 'Displeasure needs half-lidded eyes')
check(animations['animation.purwhite.face_blush']['bones']['blush_shy']['scale'] == [1,1,1], 'Blush needs diagonal cheek strokes')
check(len({v['vector'][1] for v in mouth_motion.values()}) > 2, 'Speech has no lip movement')
wink = animations['animation.purwhite.face_wink']
check(wink['loop'], 'Seated wink must loop while the player sleeps')
wink_eye = wink['bones']['eye_left']['scale']
check(min(v['vector'][1] for v in wink_eye.values()) < 0.15
      and max(v['vector'][1] for v in wink_eye.values()) == 1, 'Wink never closes and reopens one eye')
check(all(v['vector'] == [1,1,1] for v in wink['bones']['eye_right']['scale'].values()),
      'Wink closes both eyes instead of keeping the other eye open')
check(wink['bones']['mouth_smile']['scale'] == [1,1,1], 'Bed wink loses its gentle smile')

# Reconstruct Gecko's mirrored coordinates, absolute pivots and Z/Y/X rotation order.
# Check the resulting anatomy, not only whether the animation contains chosen angles.
duck = animations['animation.purwhite.sit_on_player']['bones']
def mirror(v): return [-v[0], v[1], v[2]]
def rotated(v, degrees):
    x,y,z = v; rx,ry,rz = map(radians, [-degrees[0], -degrees[1], degrees[2]])
    y,z = y*cos(rx)-z*sin(rx), y*sin(rx)+z*cos(rx)
    x,z = x*cos(ry)+z*sin(ry), -x*sin(ry)+z*cos(ry)
    return [x*cos(rz)-y*sin(rz), x*sin(rz)+y*cos(rz), z]
def posed(name, point):
    bone = by_name[name]; channels = duck.get(name, {})
    pivot = mirror(bone['pivot']); offset = mirror(channels.get('position', [0,0,0]))
    scale = channels.get('scale', [1,1,1])
    local = [(point[i]-pivot[i])*scale[i] for i in range(3)]
    angles = [bone.get('rotation', [0,0,0])[i]+channels.get('rotation', [0,0,0])[i] for i in range(3)]
    result = [v+p+d for v,p,d in zip(rotated(local, angles), pivot, offset)]
    return posed(bone['parent'], result) if bone.get('parent') else result
def joint(name): return posed(name, mirror(by_name[name]['pivot']))
def cube_vertices(name, cube):
    pivot = mirror(cube.get('pivot', [0,0,0])); inflate = cube.get('inflate', by_name[name].get('inflate', 0))
    for corner in product((0,1), repeat=3):
        point = mirror([cube['origin'][i]-inflate+corner[i]*(cube['size'][i]+2*inflate) for i in range(3)])
        local = rotated([v-p for v,p in zip(point,pivot)], cube.get('rotation', [0,0,0]))
        yield posed(name, [v+p for v,p in zip(local,pivot)])
for side in ('left','right'):
    thigh = 'leg_'+side; calf = 'shin_'+side; foot = 'foot_'+side
    hip, knee = joint(thigh), joint(calf)
    ankle = posed(calf, mirror(by_name[foot]['pivot']))
    check(knee[2] < hip[2]-4 and abs(knee[0]) > abs(hip[0]), 'Duck sit knees are not forward and apart')
    check(ankle[2] > knee[2]+3 and abs(ankle[0]) > abs(knee[0]), 'Duck sit calves are not folded back outside the hips')
    check(abs(hip[1]-knee[1]) < 1e-6 and abs(knee[1]-ankle[1]) < 1,
          'Duck sit legs remain vertical as in a crouch')
    pivot = mirror(by_name[foot]['pivot'])
    base = posed(foot, pivot); up = posed(foot, [pivot[0],pivot[1]+1,pivot[2]])
    normal = [v-p for v,p in zip(up,base)]
    check(normal[1]/sqrt(sum(v*v for v in normal)) > 0.99, 'Duck sit soles do not stay parallel to the seat')
    vertices = [v for cube in by_name[foot]['cubes'] for v in cube_vertices(foot,cube)]
    check(0 <= min(v[1] for v in vertices) <= 0.3, 'Duck sit feet float above or penetrate the seat')
skirt_vertices = [v for name,bone in by_name.items() if name.startswith('skirt')
                  for cube in bone.get('cubes', []) for v in cube_vertices(name,cube)]
check(min(v[1] for v in skirt_vertices) >= 0, 'Seated skirt penetrates the seat')
duck = animations['animation.purwhite.cower']['bones']
for side in ('left','right'):
    feet = [v for cube in by_name['foot_'+side]['cubes'] for v in cube_vertices('foot_'+side,cube)]
    check(0 <= min(v[1] for v in feet) <= 0.3, 'Cowering feet must rest on the ground')
    check(joint('hand_'+side)[1] > joint('head')[1]+4, 'Cowering hands must reach the head')
variants=read(A/'blockstates/coffee_crop.json')['variants']
check(set(variants)=={f'age={i}' for i in range(8)}, 'Missing crop stages')
for variant in variants.values():
    model_path=variant['model'].split(':',1)[1]
    model=read(A/f'models/{model_path}.json')
    texture=model['textures']['cross'].split(':',1)[1]
    check((A/f'textures/{texture}.png').exists(), 'Missing crop texture')
    check(model['render_type']=='minecraft:cutout', 'Crop transparency missing')
for item in ('coffee_beans','wild_coffee','mushroom_girl_spawn_egg'):
    definition=read(A/f'items/{item}.json')
    model=read(A/('models/'+definition['model']['model'].split(':',1)[1]+'.json'))
    check((A/('textures/'+model['textures']['layer0'].split(':',1)[1]+'.png')).exists(), 'Missing item texture')
pools=read(D/'loot_table/blocks/coffee_crop.json')['pools']
check(pools[0]['entries'][0]['name']=='toneko:coffee_beans', 'Immature plants must return a bean for replanting')
check(pools[1]['conditions'][0]['properties']['age']=='7', 'Yield must require maturity')
check(pools[1]['entries'][0]['functions'][0]['count']['min']>=2, 'Mature harvest must sustain feeding and replanting')
habitats=read(D/'tags/worldgen/biome/mushroom_girl_habitats.json')['values']
check(len(habitats)==5 and 'minecraft:lush_caves' in habitats, 'Missing habitat')
for lang in ('zh_cn','en_us'):
    data=read(A/f'lang/{lang}.json')
    check(sum(k.startswith('name.toneko.mushroom_girl.') for k in data)==10, 'Missing character names')
    check('effect.toneko.hallucination' in data, 'Missing effect localization')
    for name in ('friend','shy','wake','coffee','feed','pet','stay','roam'):
        check('message.toneko.mushroom_girl.'+name in data, 'Missing interaction text')
    for category in ('greet_stranger','greet_friend','affection','sleep','drowsy','night','rain','shelter','carry','sit','lie','get_down','coffee','feed','hurt','mushroom','birth','admire'):
        for i in range(3):
            check(bool(data.get(f'dialogue.toneko.mushroom_girl.{category}.{i}')), 'Missing local dialogue')
    for action in ('companion','companion_help','bed_help','bed_wake_help','carry','sit_on_player','lie_on_player','get_down',
                   'carry.des','sit_on_player.des','lie_on_player.des','get_down.des','affection','relationship_help'):
        check('screen.toneko.mushroom_girl.'+action in data, 'Missing companion action label')
    check('message.toneko.mushroom_girl.affection_unlocked' in data, 'Missing relationship unlock message')
    for stage in ('fond','close','attached'):
        check('stage.toneko.mushroom_girl.'+stage in data, 'Missing affection stage name')
for path in list(A.glob('**/*coffee*.json'))+list(D.glob('**/*coffee*.json')):
    read(path)
print(f'Mushroom assets: {checks} checks passed; {len(names)} bones, {len(animations)} animations.')

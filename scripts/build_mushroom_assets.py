"""Rebuild mushroom gameplay assets from the untouched Purwhite reference model.
Run from the repository root with Python 3 and Pillow.
"""
from pathlib import Path
from copy import deepcopy
from math import cos, sin, tau
import json
from PIL import Image, ImageDraw
from mushroom_face import refine_face
from mushroom_dialogue import dialogue_translations

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'common/src/main/resources'
ASSETS = RES / 'assets/toneko'
DATA = RES / 'data'

def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')

def png(path, image):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)

# Copy the current second revision; do not rewrite the artist's reference files.
geometry = json.loads((ROOT / 'refer/purwhite/model/purwhite.geo.json').read_text())
g = geometry['minecraft:geometry'][0]
bones = g['bones']
cap_names = {'hat'}
for bone in bones:
    if bone.get('parent') in cap_names:
        cap_names.add(bone['name'])
# A cap at ground level covers a curled, compressed body in the resting animation.
bones.append({'name': 'sleep_cap', 'parent': 'root', 'pivot': [0, 0, 0]})
for bone in list(bones):
    if bone['name'] in cap_names:
        new = deepcopy(bone)
        new['name'] = 'sleep_' + bone['name']
        new['parent'] = 'sleep_cap' if bone['name'] == 'hat' else 'sleep_' + bone['parent']
        bones.append(new)
png_path = ASSETS / 'textures/entity/purwhite.png'
png_path.parent.mkdir(parents=True, exist_ok=True)
atlas = Image.open(ROOT / 'refer/purwhite/model/purwhite.png').convert('RGBA')
animations = json.loads((ROOT / 'refer/purwhite/model/purwhite.animation.json').read_text())
a = animations['animations']
def track(values):
    return {str(time): {'vector': vector} for time, vector in values}
def anim(name, length, bones, loop=True):
    a['animation.purwhite.' + name] = {'animation_length': length, 'loop': loop, 'bones': bones}

anim('sleep', 4, {
    'hips': {'position': [0, -15.2, 0], 'scale': [0.12, 0.12, 0.12]},
    'body': {'rotation': [30, 0, 0]},
    'leg_left': {'rotation': [-110, 0, -5]}, 'leg_right': {'rotation': [-110, 0, 5]},
    'shin_left': {'rotation': [120, 0, 0]}, 'shin_right': {'rotation': [120, 0, 0]},
    'arm_left': {'rotation': [-70, 0, -20]}, 'arm_right': {'rotation': [-70, 0, 20]},
    'sleep_cap': {'position': track([(0, [0, -28.4, 0]), (2, [0, -28.2, 0]), (4, [0, -28.4, 0])])},
})
closed = {f'eye_{side}': {'scale': [0, 0, 0]} for side in ('left', 'right')}
closed.update({f'eye_lid_{side}': {'scale': [1, 1, 1]} for side in ('left', 'right')})
anim('sleep_eyes', 4, closed)
anim('drowsy', 5, {
    'head': {'rotation': track([(0, [6, 0, 3]), (2, [14, 0, -2]), (3, [4, 0, 2]), (5, [6, 0, 3])])},
    'body': {'rotation': [5, 0, 0]},
    'arm_left': {'rotation': [-8, 0, -8]}, 'arm_right': {'rotation': [-8, 0, 8]},
})
anim('yawn', 2.8, {
    'head': {'rotation': track([(0, [0, 0, 0]), (1, [-10, 0, 3]), (2, [-6, 0, 2]), (2.8, [0, 0, 0])])},
    'arm_right': {'rotation': track([(0, [0, 0, 0]), (0.6, [-90, 0, -20]), (2, [-85, 0, -20]), (2.8, [0, 0, 0])])},
    'mouth': {'scale': track([(0, [1, 1, 1]), (0.8, [0.8, 2, 1]), (2, [0.8, 2, 1]), (2.8, [1, 1, 1])])},
}, False)
anim('spores', 2.4, {
    'hat': {'rotation': track([(0, [0, 0, 0]), (0.6, [-5, 0, -5]), (1.2, [3, 0, 4]), (1.8, [-3, 0, -3]), (2.4, [0, 0, 0])])},
    'arm_left': {'rotation': track([(0, [0, 0, 0]), (1.2, [-12, 0, -20]), (2.4, [0, 0, 0])])},
    'arm_right': {'rotation': track([(0, [0, 0, 0]), (1.2, [-12, 0, 20]), (2.4, [0, 0, 0])])},
}, False)
# Distinct animation names keep defensive and peaceful spore expressions distinguishable.
a['animation.purwhite.defense'] = deepcopy(a['animation.purwhite.spores'])
anim('surprised', 1.6, {
    'head': {'rotation': track([(0, [0, 0, 0]), (0.2, [-7, 0, 0]), (0.8, [-4, 0, 0]), (1.6, [0, 0, 0])])},
    'arm_left': {'rotation': track([(0, [0, 0, 0]), (0.2, [-8, 0, -12]), (1.6, [0, 0, 0])])},
    'arm_right': {'rotation': track([(0, [0, 0, 0]), (0.2, [-8, 0, 12]), (1.6, [0, 0, 0])])},
}, False)
seated = {
    'hips': {'position': [0, -6.5, 0]},
    'leg_left': {'rotation': [-90, 0, -12]}, 'leg_right': {'rotation': [-90, 0, 12]},
    'shin_left': {'rotation': [95, 0, 0]}, 'shin_right': {'rotation': [95, 0, 0]},
    'body': {'rotation': [5, 0, 0]},
    'arm_left': {'rotation': [-28, 0, -8]}, 'arm_right': {'rotation': [-28, 0, 8]},
    'sleep_cap': {'scale': [0, 0, 0]},
}
# W sitting: knees forward and apart, calves folded back outside the hips.
# Keep the carried pose's original leg arrangement independent of this floor pose.
duck_seated = deepcopy(seated)
duck_seated.update({
    'hips': {'position': [0, -8.6, 0]},
    'leg_left': {'rotation': [-90, 15, 0]}, 'leg_right': {'rotation': [-90, -15, 0]},
    'shin_left': {'rotation': [0, 0, 155]}, 'shin_right': {'rotation': [0, 0, -155]},
    'foot_left': {'rotation': [90, 0, 0], 'position': [0, 0, 1.1]},
    'foot_right': {'rotation': [90, 0, 0], 'position': [0, 0, 1.1]},
    'skirt': {'scale': [1, 0.78, 1]},
    'body': {'rotation': [4, 0, 0]},
    'arm_left': {'rotation': [-20, 0, -14]}, 'arm_right': {'rotation': [-20, 0, 14]},
})
for i in range(16):
    angle = tau * i / 16
    duck_seated[f'skirt_{i:02}'] = {'rotation': [-48 * cos(angle), 0, -48 * sin(angle)]}
anim('sit_on_player', 4, duck_seated)
carried = deepcopy(seated)
carried['hips']['position'] = [0, -4.4, 0]
carried.update({
    'body': {'rotation': [12, 0, 0]},
    'arm_left': {'rotation': [-82, 0, -12]}, 'arm_right': {'rotation': [-82, 0, 12]},
    'forearm_left': {'rotation': [-25, 0, 0]}, 'forearm_right': {'rotation': [-25, 0, 0]},
})
anim('carry', 4, carried)
cower = {
    'hips': {'position': [0, -5.25, 0]},
    'body': {'rotation': [18, 0, 0]}, 'head': {'rotation': [14, 0, 0]},
    'leg_left': {'rotation': [-78, 0, -10]}, 'leg_right': {'rotation': [-78, 0, 10]},
    'shin_left': {'rotation': [128, 0, 0]}, 'shin_right': {'rotation': [128, 0, 0]},
    'foot_left': {'rotation': [-50, 0, 0]}, 'foot_right': {'rotation': [-50, 0, 0]},
    'arm_left': {'rotation': [-150, 0, -10]}, 'arm_right': {'rotation': [-150, 0, 10]},
    'forearm_left': {'rotation': [-40, 0, 0]}, 'forearm_right': {'rotation': [-40, 0, 0]},
    'skirt': {'scale': [1, 0.72, 1]},
}
for i in range(16):
    angle = tau * i / 16
    cower[f'skirt_{i:02}'] = {'rotation': [-35 * cos(angle), 0, -35 * sin(angle)]}
anim('cower', 2, cower)
tremble = deepcopy(cower)
tremble['hips']['position'] = track([(0, [-0.09, -5.25, 0]), (0.1, [0.09, -5.25, 0]),
    (0.2, [-0.06, -5.25, 0]), (0.3, [0.06, -5.25, 0]), (0.4, [-0.09, -5.25, 0])])
tremble['head']['rotation'] = track([(0, [18, 0, -2]), (0.1, [18, 0, 2]), (0.2, [18, 0, -1]),
    (0.3, [18, 0, 1]), (0.4, [18, 0, -2])])
anim('tremble', 0.4, tremble)
anim('inspect', 4, {
    'head': {'rotation': track([(0, [5, -12, 6]), (1, [3, 10, -5]), (2, [5, -8, 6]), (4, [5, -12, 6])])},
    'body': {'rotation': [5, 0, 0]},
    'arm_left': {'rotation': [-12, 0, -8]}, 'arm_right': {'rotation': [-12, 0, 8]},
})
anim('lie_on_player', 4, {
    'hips': {'position': [0, -13.5, 0], 'rotation': [90, 0, 0]},
    'body': {'rotation': [-5, 0, 0]}, 'head': {'rotation': [-65, 0, 0]},
    'arm_left': {'rotation': [-145, 0, -12]}, 'arm_right': {'rotation': [-145, 0, 12]},
    'forearm_left': {'rotation': [-25, 0, 0]}, 'forearm_right': {'rotation': [-25, 0, 0]},
    'leg_left': {'rotation': [0, 0, -6]}, 'leg_right': {'rotation': [0, 0, 6]},
    'shin_left': {'rotation': [20, 0, 0]}, 'shin_right': {'rotation': [20, 0, 0]},
    'sleep_cap': {'scale': [0, 0, 0]},
})
refine_face(geometry, atlas, animations)
save(ASSETS / 'geckolib/models/entity/purwhite.geo.json', geometry)
atlas.save(png_path)
save(ASSETS / 'geckolib/animations/entity/purwhite.animation.json', animations)

# Pixel sprites use the mod's existing crop/item conventions.
stages = []
for age in range(8):
    im = Image.new('RGBA', (16, 16)); d = ImageDraw.Draw(im)
    height = 3 + age * 1.55; top = round(15 - height)
    d.line([(8, 15), (8, top)], fill='#567746', width=2)
    for row in range(2 + age // 2):
        y = 14 - row * 2
        if y < top: break
        reach = 2 + min(age // 2, 3)
        d.polygon([(8, y), (8-reach, y-2), (6-reach, y-1), (8, y+1)], fill='#497851')
        d.polygon([(9, y), (9+reach, y-2), (10+reach, y-1), (9, y+1)], fill='#679852')
        d.point((8-reach, y-1), fill='#9FBD67')
    if age >= 5:
        for x,y in [(5,7),(10,9),(5,12)]:
            color = '#BCA76A' if age == 5 else '#C64D64' if age == 6 else '#92354D'
            d.rectangle((x,y,x+1,y+1), fill=color)
            d.point((x,y), fill='#E2A075' if age == 5 else '#ED9B9A')
    stages.append(im)
    png(ASSETS / f'textures/block/coffee/coffee_stage{age}.png', im)
    save(ASSETS / f'models/block/coffee/coffee_stage{age}.json', {
        'parent': 'minecraft:block/cross', 'render_type': 'minecraft:cutout',
        'textures': {'cross': f'toneko:block/coffee/coffee_stage{age}'}})
save(ASSETS / 'blockstates/coffee_crop.json', {'variants': {f'age={age}': {'model': f'toneko:block/coffee/coffee_stage{age}'} for age in range(8)}})
save(ASSETS / 'blockstates/wild_coffee.json', {'variants': {'': {'model': 'toneko:block/coffee/coffee_stage7'}}})
beans = Image.new('RGBA', (16,16)); d = ImageDraw.Draw(beans)
for x,y in [(3,3),(8,8)]:
    d.rounded_rectangle((x,y,x+5,y+6),radius=2,fill='#57384C',outline='#362A3C')
    d.line([(x+3,y+1),(x+2,y+3),(x+3,y+5)],fill='#BC8A70')
    d.point((x+1,y+1),fill='#89645D')
png(ASSETS/'textures/item/coffee_beans.png',beans)
for name, texture in [('coffee_beans', 'toneko:item/coffee_beans'), ('wild_coffee','toneko:block/coffee/coffee_stage7')]:
    save(ASSETS/f'models/item/{name}.json', {'parent':'minecraft:item/generated','textures':{'layer0':texture}})
    save(ASSETS/f'items/{name}.json',{'model':{'type':'minecraft:model','model':f'toneko:item/{name}'}})
egg=Image.new('RGBA',(16,16));d=ImageDraw.Draw(egg)
d.ellipse((3,1,12,14),fill='#8C60A8',outline='#503464')
for x,y in [(5,4),(9,7),(5,10)]:d.rectangle((x,y,x+2,y+1),fill='#F2E9D5')
png(ASSETS/'textures/item/mushroom_girl_spawn_egg.png',egg)
save(ASSETS/'models/item/mushroom_girl_spawn_egg.json',{'parent':'minecraft:item/generated','textures':{'layer0':'toneko:item/mushroom_girl_spawn_egg'}})
save(ASSETS/'items/mushroom_girl_spawn_egg.json',{'model':{'type':'minecraft:model','model':'toneko:item/mushroom_girl_spawn_egg'}})
illusion=Image.new('RGBA',(32,32));d=ImageDraw.Draw(illusion)
d.rounded_rectangle((13,15,19,29),radius=3,fill='#E8DBF2')
d.polygon([(2,16),(4,10),(9,5),(16,2),(23,5),(28,10),(30,16)],fill='#B695DA')
d.rectangle((3,16,29,19),fill='#D9C9EC')
for x,y in [(9,9),(18,6),(22,12)]:d.ellipse((x,y,x+3,y+2),fill='#F8EDF9')
png(ASSETS/'textures/gui/mushroom_illusion.png',illusion)
png(ASSETS/'textures/mob_effect/hallucination.png',illusion.resize((18,18),Image.Resampling.NEAREST))

# Sustainably obtain beans by finding a wild bush; immature planted crops return one bean.
save(DATA/'toneko/loot_table/blocks/wild_coffee.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'toneko:coffee_beans','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':2,'max':4}}]}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
save(DATA/'toneko/loot_table/blocks/coffee_crop.json',{'type':'minecraft:block','pools':[
    {'rolls':1,'entries':[{'type':'minecraft:item','name':'toneko:coffee_beans'}],'conditions':[{'condition':'minecraft:survives_explosion'}]},
    {'rolls':1,'entries':[{'type':'minecraft:item','name':'toneko:coffee_beans','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':2,'max':4}},{'function':'minecraft:apply_bonus','enchantment':'minecraft:fortune','formula':'minecraft:binomial_with_bonus_count','parameters':{'extra':3,'probability':0.5714286}},{'function':'minecraft:explosion_decay'}]}],'conditions':[{'condition':'minecraft:block_state_property','block':'toneko:coffee_crop','properties':{'age':'7'}}]}
]})
# 26.1 removed random_patch: repetitions and patch offsets now belong to placed features.
save(DATA/'toneko/worldgen/configured_feature/patch_wild_coffee.json',{
    'type':'minecraft:simple_block',
    'config':{'to_place':{'type':'minecraft:simple_state_provider','state':{'Name':'toneko:wild_coffee'}}}
})
save(DATA/'toneko/worldgen/placed_feature/patch_wild_coffee.json',{
    'feature':'toneko:patch_wild_coffee',
    'placement':[
        {'type':'minecraft:rarity_filter','chance':24},
        {'type':'minecraft:in_square'},
        {'type':'minecraft:heightmap','heightmap':'MOTION_BLOCKING_NO_LEAVES'},
        {'type':'minecraft:biome'},
        {'type':'minecraft:count','count':12},
        {'type':'minecraft:random_offset',
         'xz_spread':{'type':'minecraft:trapezoid','min':-5,'max':5,'plateau':0},
         'y_spread':{'type':'minecraft:trapezoid','min':-2,'max':2,'plateau':0}},
        {'type':'minecraft:block_predicate_filter','predicate':{
            'type':'minecraft:all_of','predicates':[
                {'type':'minecraft:replaceable'},
                {'type':'minecraft:would_survive','state':{'Name':'toneko:wild_coffee'}}
            ]}}
    ]
})
save(DATA/'toneko/tags/worldgen/biome/mushroom_girl_habitats.json',{'replace':False,'values':['minecraft:mushroom_fields','minecraft:dark_forest','minecraft:swamp','minecraft:mangrove_swamp','minecraft:lush_caves']})
save(DATA/'toneko/tags/item/coffee_beans.json',{'replace':False,'values':['toneko:coffee_beans','#c:coffee_beans',{'id':'herbalbrews:coffee_beans','required':False},{'id':'farmersrespite:coffee_beans','required':False}]})
save(DATA/'c/tags/item/coffee_beans.json',{'replace':False,'values':['toneko:coffee_beans']})

zh={
 'entity.toneko.mushroom_girl':'蘑菇娘','item.toneko.mushroom_girl_spawn_egg':'蘑菇娘刷怪蛋',
 'item.toneko.coffee_beans':'咖啡豆','block.toneko.coffee_crop':'咖啡作物','block.toneko.wild_coffee':'野生咖啡植株',
 'effect.toneko.hallucination':'致幻',
 'message.toneko.mushroom_girl.friend':'%s轻轻牵住你的衣角：「已经记得你的气息啦……」',
 'message.toneko.mushroom_girl.shy':'%s往菌盖下缩了缩：「还、还不太熟悉呢……」',
 'message.toneko.mushroom_girl.wake':'%s慢慢探出头：「唔……天还没黑呀……」',
 'message.toneko.mushroom_girl.coffee':'%s晃了晃菌盖：「香香的豆子！这下可以陪你一会儿啦～」',
 'message.toneko.mushroom_girl.feed':'%s捧着礼物：「谢谢……是森林的味道呢。」',
 'message.toneko.mushroom_girl.pet':'%s轻轻摇晃：「菌盖要轻轻摸哦……」',
 'message.toneko.mushroom_girl.stay':'%s：「我就在这里等你，记得给我留一点阴凉哦。」',
 'message.toneko.mushroom_girl.roam':'%s：「去找一处湿润的小角落吧～」',
 'screen.toneko.mushroom_girl.familiarity':'熟悉度：%s / 100',
 'screen.toneko.mushroom_girl.state':'状态：%s','screen.toneko.mushroom_girl.stay':'停留 / 自由活动',
 'screen.toneko.mushroom_girl.help':'摸头40 · 跟随60 · 潜行空手拥抱80',
 'state.toneko.mushroom_girl.resting':'蜷缩休眠','state.toneko.mushroom_girl.drowsy':'困困的……','state.toneko.mushroom_girl.lively':'精神饱满',
}
names=['紫夜白','夜露紫','月霭白','暮雨铃','星苔眠','青露晚','紫霞霜','白雾谣','雨月堇','暮霜音']
ids=['ziye_bai','yelu_zi','yueai_bai','muyu_ling','xingtai_mian','qinglu_wan','zixia_shuang','baiwu_yao','yuyue_jin','mushuang_yin']
zh.update({f'name.toneko.mushroom_girl.{key}':name for key,name in zip(ids,names)})
en={
 'entity.toneko.mushroom_girl':'Mushroom Girl','item.toneko.mushroom_girl_spawn_egg':'Mushroom Girl Spawn Egg',
 'item.toneko.coffee_beans':'Coffee Beans','block.toneko.coffee_crop':'Coffee Crop','block.toneko.wild_coffee':'Wild Coffee Bush',
 'effect.toneko.hallucination':'Hallucination',
 'message.toneko.mushroom_girl.friend':'%s gently holds your sleeve: "I remember you now..."',
 'message.toneko.mushroom_girl.shy':'%s hides under her cap: "We are still getting to know each other..."',
 'message.toneko.mushroom_girl.wake':'%s peeks out sleepily: "Mmm... is it still daytime...?"',
 'message.toneko.mushroom_girl.coffee':'%s sways her cap: "Lovely beans! Now I can stay awake with you~"',
 'message.toneko.mushroom_girl.feed':'%s holds your gift: "Thank you... it smells like the forest."',
 'message.toneko.mushroom_girl.pet':'%s sways gently: "Please be gentle with my cap..."',
 'message.toneko.mushroom_girl.stay':'%s: "I will wait here. Please leave me a shady spot."',
 'message.toneko.mushroom_girl.roam':'%s: "Let us find a damp little corner~"',
 'screen.toneko.mushroom_girl.familiarity':'Familiarity: %s / 100','screen.toneko.mushroom_girl.state':'State: %s',
 'screen.toneko.mushroom_girl.stay':'Stay / Roam','screen.toneko.mushroom_girl.help':'Pet 40 · Follow 60 · Sneak + empty hand hug 80',
 'state.toneko.mushroom_girl.resting':'Curled up asleep','state.toneko.mushroom_girl.drowsy':'Sleepy...','state.toneko.mushroom_girl.lively':'Wide awake',
}
en.update({f'name.toneko.mushroom_girl.{key}':name for key,name in zip(ids,['Ziye Bai','Yelu Zi','Yueai Bai','Muyu Ling','Xingtai Mian','Qinglu Wan','Zixia Shuang','Baiwu Yao','Yuyue Jin','Mushuang Yin'])})
zh.update(dialogue_translations('zh_cn'))
en.update(dialogue_translations('en_us'))
for lang, updates in [('zh_cn',zh),('en_us',en)]:
    path=ASSETS/f'lang/{lang}.json';content=json.loads(path.read_text());content.update(updates);save(path,content)
# Small preview sheet for inspecting the added sprites.
sheet=Image.new('RGBA',(352,96),'#292337')
for i,im in enumerate(stages+[beans,egg,illusion.resize((16,16),Image.Resampling.NEAREST)]):
    sheet.alpha_composite(im.resize((32,32),Image.Resampling.NEAREST),(i*32,16))
preview=ROOT/'build/mushroom-assets.png';preview.parent.mkdir(exist_ok=True);sheet.save(preview)
print(f'Built model, {len(a)} animations, coffee crop resources, hallucination sprites, names and translations.')

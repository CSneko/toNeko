"""Native pixel face layers and animation tracks for the Purwhite game model."""
from copy import deepcopy
from PIL import Image, ImageDraw


def refine_face(geometry, atlas, animations):
    bones = geometry['minecraft:geometry'][0]['bones']
    by_name = {bone['name']: bone for bone in bones}
    tiles = {}

    def tile(name, im):
        # The reference atlas ends at row 544. Keep the original UV islands intact.
        x, y = 4 + len(tiles) * 40, 704
        w, h = im.size
        assert atlas.crop((x - 1, y - 1, x + w + 1, y + h + 1)).getbbox() is None
        atlas.paste(im, (x, y))
        tiles[name] = {'uv': [x, y], 'uv_size': [w, h]}

    # Preserve the original broad, blocky purple eyes, including their palette and highlights.
    # Split the existing painted iris from the lash/white so it can glance without redrawing it.
    skin, white_color = (255, 238, 229, 255), (251, 246, 255, 255)
    ink = '#403070'
    for side in ('left', 'right'):
        front = by_name['eye_' + side]['cubes'][0]['uv']['north']
        u, v = front['uv']; w, h = front['uv_size']
        original = atlas.crop((u, v, u + w, v + h))
        white, iris = Image.new('RGBA', original.size), Image.new('RGBA', original.size)
        for y in range(h):
            for x in range(w):
                color = original.getpixel((x, y))
                if color == skin:
                    continue
                if color in (white_color, (64, 48, 112, 255)):
                    white.putpixel((x, y), color)
                else:
                    white.putpixel((x, y), white_color)
                    iris.putpixel((x, y), color)
        tile('white_' + side, white)
        tile('iris_' + side, iris)

    for name, points in (
        ('closed', [(0, 2), (6, 2), (7, 1)]),
        ('happy', [(0, 2), (1, 2), (1, 1), (6, 1), (6, 2), (7, 2)]),
    ):
        im = Image.new('RGBA', (8, 4))
        ImageDraw.Draw(im).line(points, fill=ink, width=1)
        tile(name, im)

    mouths = ('smile', 'soft', 'open', 'o', 'pout')
    mouth_line, mouth_inside = '#B1858A', '#EFABB0'
    for name in mouths:
        im = Image.new('RGBA', (8, 8))
        d = ImageDraw.Draw(im)
        if name == 'smile':
            d.line([(2, 3), (2, 4), (3, 4), (3, 3), (4, 3), (4, 4), (5, 4), (5, 3)], fill=mouth_line)
        elif name == 'soft':
            d.line([(2, 3), (3, 4), (4, 4), (5, 3)], fill=mouth_line)
        elif name == 'open':
            # A single flat cavity color: no dark ring, lip outline or contrasting tongue inset.
            d.rounded_rectangle((1, 2, 6, 6), radius=2, fill=mouth_inside)
        elif name == 'pout':
            d.line([(2, 4), (3, 3), (4, 3), (5, 4)], fill=mouth_line)
        else:
            d.rounded_rectangle((2, 2, 5, 5), radius=1, fill=mouth_inside)
        tile('mouth_' + name, im)

    for side in ('left', 'right'):
        uv = by_name['eye_' + side]['cubes'][0]['uv']['north']
        u, v = uv['uv']; w, h = uv['uv_size']
        displeased = atlas.crop((u, v, u + w, v + h))
        d = ImageDraw.Draw(displeased)
        cutoff = round(h * 0.44)
        d.rectangle((0, 0, w - 1, cutoff), fill=skin)
        d.rectangle((0, cutoff, w - 1, cutoff + 1), fill=ink)
        tile('displeased_' + side, displeased)
        sparkle = Image.new('RGBA', (w, h))
        d = ImageDraw.Draw(sparkle)
        cx, cy = w // 2, h // 2
        d.line([(cx, cy - 3), (cx, cy + 3)], fill='#FFF1A8', width=2)
        d.line([(cx - 2, cy), (cx + 2, cy)], fill='#FFF1A8', width=2)
        d.rectangle((cx, cy - 1, cx + 1, cy + 1), fill='#FFFFE3')
        tile('sparkle_' + side, sparkle)

    blush = Image.new('RGBA', (8, 4))
    d = ImageDraw.Draw(blush)
    d.rectangle((1, 1, 6, 3), fill='#FFC9CD')
    d.rectangle((2, 1, 5, 2), fill='#FFB8C1')
    tile('blush', blush)
    strong_blush = Image.new('RGBA', (32, 8))
    d = ImageDraw.Draw(strong_blush)
    for x in range(3, 30, 4):
        d.line([(x, 5), (x + 2, 2)], fill='#F49DAB', width=1)
    tile('strong_blush', strong_blush)
    empty = Image.new('RGBA', (2, 2))
    tile('empty', empty)

    def plane(x, y, z, w, h, uv):
        # Only the front is painted. A 0.002 model-unit layer is virtually flush with the skin.
        faces = {face: deepcopy(tiles['empty']) for face in ('north', 'south', 'east', 'west', 'up', 'down')}
        faces['north'] = deepcopy(tiles[uv])
        return {'origin': [x - w / 2, y - h / 2, z], 'size': [w, h, 0.002], 'uv': faces}

    def bone(name, parent, pivot, cube):
        value = {'name': name, 'parent': parent, 'pivot': list(pivot), 'cubes': [cube]}
        bones.append(value)
        by_name[name] = value

    # Remove the old opaque blush slabs, leaving the actual skin solid underneath.
    by_name['head']['cubes'] = [c for c in by_name['head']['cubes'] if c['size'][2] >= 0.1]
    by_name['mouth']['pivot'] = [0, 22.0, -3.506]
    by_name['mouth']['cubes'] = []
    for name in mouths:
        bone('mouth_' + name, 'mouth', [0, 22.0, -3.506], plane(0, 22.0, -3.506, 1.8, 1.62, 'mouth_' + name))
    for side, x in (('left', -2), ('right', 2)):
        eye = by_name['eye_' + side]
        eye['pivot'] = [x, 24.1, -3.504]
        eye['cubes'] = [plane(x, 24.1, -3.504, 2.48, 2.77, 'white_' + side)]
        bone('iris_' + side, 'eye_' + side, [x, 24.1, -3.508], plane(x, 24.1, -3.508, 2.48, 2.77, 'iris_' + side))
        bone('eye_sparkle_' + side, 'eye_' + side, [x, 24.1, -3.509], plane(x, 24.1, -3.509, 2.48, 2.77, 'sparkle_' + side))
        bone('eye_displeased_' + side, 'head', [x, 24.1, -3.509], plane(x, 24.1, -3.509, 2.48, 2.77, 'displeased_' + side))
        lid = by_name['eye_lid_' + side]
        lid['pivot'] = [x, 24.1, -3.506]
        lid['cubes'] = [plane(x, 24.1, -3.506, 2.48, 0.95, 'closed')]
        bone('eye_happy_' + side, 'head', [x, 24.1, -3.506], plane(x, 24.1, -3.506, 2.48, 0.95, 'happy'))
        bone('blush_' + side, 'head', [x * 1.45, 22.35, -3.506], plane(x * 1.45, 22.35, -3.506, 1.5, 1.1, 'blush'))
    bone('blush_shy', 'head', [0, 22.8, -3.509], plane(0, 22.8, -3.509, 5.8, 1.35, 'strong_blush'))

    a = animations['animations']
    facial = {name for name in by_name if name.startswith(('eye_', 'iris_', 'mouth', 'brow_', 'blush_'))}
    # Body and facial controllers must never compete for the same bones.
    for animation in a.values():
        for name in facial:
            animation['bones'].pop(name, None)

    def track(values):
        return {str(t): {'vector': v} for t, v in values}

    def face(name, length, eye_y=1, mouth='smile', blink=True, gaze=None):
        channels = {}
        for side in ('left', 'right'):
            open_keys = [(0, [1, eye_y, 1]), (length, [1, eye_y, 1])]
            closed_keys = [(0, [0, 0, 1]), (length, [0, 0, 1])]
            if blink:
                for t in (length * 0.43,):
                    open_keys[1:1] = [(t, [1, eye_y, 1]), (t + 0.10, [1, 0.035, 1]),
                                     (t + 0.18, [1, 0.035, 1]), (t + 0.31, [1, eye_y, 1])]
                    closed_keys[1:1] = [(t, [0, 0, 1]), (t + 0.10, [1, 1, 1]),
                                       (t + 0.18, [1, 1, 1]), (t + 0.31, [0, 0, 1])]
            channels['eye_' + side] = {'scale': track(open_keys), 'rotation': [0, 0, 0]}
            channels['eye_lid_' + side] = {'scale': track(closed_keys)}
            channels['eye_happy_' + side] = {'scale': [0, 0, 1]}
            channels['eye_sparkle_' + side] = {'scale': [0, 0, 1]}
            channels['eye_displeased_' + side] = {'scale': [0, 0, 1]}
            channels['iris_' + side] = {'position': track(gaze or [(0, [0, 0, 0]), (length * 0.25, [0.06, 0, 0]),
                (length * 0.55, [-0.06, -0.03, 0]), (length * 0.8, [-0.03, 0, 0]), (length, [0, 0, 0])])}
            channels['blush_' + side] = {'scale': [1, 1, 1]}
        channels['blush_shy'] = {'scale': [0, 0, 1]}
        for variant in mouths:
            channels['mouth_' + variant] = {'scale': [1, 1, 1] if variant == mouth else [0, 0, 1]}
        channels['mouth'] = {'scale': track([(0, [1, 1, 1]), (length / 2, [0.94, 0.91, 1]), (length, [1, 1, 1])])}
        a['animation.purwhite.face_' + name] = {'animation_length': length, 'loop': True, 'bones': channels}
        return channels

    face('idle', 6)
    face('drowsy', 4.8, eye_y=0.55, mouth='soft', gaze=[(0, [0, -0.04, 0]), (2.4, [0.04, -0.06, 0]), (4.8, [0, -0.04, 0])])
    c = face('shy', 3.6, eye_y=0, mouth='soft', blink=False)
    for side, sign in (('left', -1), ('right', 1)):
        c['eye_lid_' + side] = {'scale': [1, 1, 1]}
        c['blush_' + side] = {'scale': [1.18, 1.16, 1]}
    c = face('happy', 2.4, mouth='open', blink=False)
    for side in ('left', 'right'):
        c['eye_' + side] = {'scale': [0, 0, 1]}
        c['eye_lid_' + side] = {'scale': [0, 0, 1]}
        c['eye_happy_' + side] = {'scale': [1, 1, 1]}
        c['blush_' + side] = {'scale': [1.1, 1.08, 1]}
    c['mouth'] = {'scale': track([(0, [1, 0.75, 1]), (0.6, [1, 1, 1]), (1.4, [0.96, 0.85, 1]), (2.4, [1, 0.75, 1])])}
    c = face('wave', 2.8, mouth='open', blink=False)
    c['eye_left'] = {'scale': track([(0, [1, 1, 1]), (0.5, [1, 1, 1]), (0.6, [1, 0.035, 1]), (1.6, [1, 0.035, 1]), (1.8, [1, 1, 1]), (2.8, [1, 1, 1])])}
    c['eye_lid_left'] = {'scale': track([(0, [0, 0, 1]), (0.5, [0, 0, 1]), (0.6, [1, 1, 1]), (1.6, [1, 1, 1]), (1.8, [0, 0, 1]), (2.8, [0, 0, 1])])}
    # Bed companionship uses the same unilateral wink with a small smile and no hand wave.
    wink_eye = deepcopy(c['eye_left'])
    wink_lid = deepcopy(c['eye_lid_left'])
    c = face('wink', 2.8, mouth='smile', blink=False)
    c['eye_left'] = wink_eye
    c['eye_lid_left'] = wink_lid
    c = face('yawn', 2.8, eye_y=0.45, mouth='o', blink=False)
    c['mouth'] = {'scale': track([(0, [0.75, 0.55, 1]), (0.7, [0.8, 1.3, 1]), (1.8, [0.8, 1.3, 1]), (2.8, [0.75, 0.55, 1])])}
    face('spores', 2.4, eye_y=0.86, mouth='o')
    face('surprised', 1.6, eye_y=1.12, mouth='o', blink=False)
    c = face('admire', 4, mouth='open', blink=True, gaze=[(0, [0, 0, 0]), (4, [0, 0, 0])])
    for side in ('left', 'right'):
        c['eye_sparkle_' + side] = {'scale': [1, 1, 1]}
    c = face('displeased', 4, mouth='pout', blink=False)
    for side in ('left', 'right'):
        c['eye_' + side] = {'scale': [0, 0, 1]}
        c['eye_displeased_' + side] = {'scale': [1, 1, 1]}
    c = face('blush', 3, eye_y=0.95, mouth='o', blink=True)
    c['blush_shy'] = {'scale': [1, 1, 1]}
    for side in ('left', 'right'):
        c['blush_' + side] = {'scale': [1.2, 1.3, 1]}
    c['mouth'] = {'scale': [1, 0.65, 1]}
    c = face('fear', 2, eye_y=0, mouth='pout', blink=False)
    for side in ('left', 'right'):
        c['eye_lid_' + side] = {'scale': [1, 1, 1], 'rotation': [0, 0, -10 if side == 'left' else 10]}
    c = face('angry', 2.4, eye_y=0.78, mouth='soft', blink=False)
    for side, sign in (('left', -1), ('right', 1)):
        c['eye_' + side]['rotation'] = [0, 0, -sign * 8]
    c = face('talk', 1.6, mouth='open', blink=False)
    c['mouth'] = {'scale': track([(0, [0.85, 0.3, 1]), (0.18, [0.95, 0.95, 1]), (0.36, [0.8, 0.35, 1]),
        (0.56, [0.85, 0.7, 1]), (0.8, [0.85, 0.25, 1]), (1.04, [0.94, 0.8, 1]), (1.3, [0.84, 0.4, 1]), (1.6, [0.85, 0.3, 1])])}
    c = face('sleep', 4, eye_y=0, mouth='soft', blink=False)
    for side in ('left', 'right'): c['eye_lid_' + side] = {'scale': [1, 1, 1]}

    # Keep reference previews and controller aliases usable, now with the corrected facial rig.
    a['animation.purwhite.blink'] = deepcopy(a['animation.purwhite.face_idle'])
    a['animation.purwhite.sleep_eyes'] = deepcopy(a['animation.purwhite.face_sleep'])

"""Собирает список модулей Aqua Client (имя, описание, категория, настройки, вкл. по умолчанию)
из исходников мода → launcher/src/ui/assets/client-modules.js для «Предпросмотра Aqua Client»."""
import json, os, re
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = f'{ROOT}/aqua-client/src/main/java/dev/aqua/client'
STR = r'"((?:[^"\\]|\\.)*)"'

def settings_of(body):
    out = []
    for m in re.finditer(r'new (Bool|Number|Mode|Color|Key|String|Button)Setting\((.*?)\)\s*[,)]', body):
        kind, args = m.group(1), m.group(2)
        strs = re.findall(STR, args)
        if not strs: continue
        name = strs[0]
        if '" +' in args or '+ "' in args: continue
        rest = re.sub(STR, '', args).split(',')
        nums = [x.strip() for x in rest if re.fullmatch(r'-?[\d.]+[fFdD]?|0x[0-9a-fA-F]+|true|false', x.strip())]
        s = {'t': kind.lower(), 'n': name}
        try:
            if kind == 'Bool': s['v'] = nums[0] == 'true'
            elif kind == 'Number': v = [float(x.rstrip('fFdD')) for x in nums[:4]]; s.update(v=v[0], min=v[1], max=v[2], step=v[3], u=strs[1] if len(strs) > 1 else '')
            elif kind == 'Mode': s.update(v=strs[1], o=strs[2:] or [strs[1]])
            elif kind == 'Color': s['v'] = '#%06x' % (int(nums[0], 16) & 0xFFFFFF)
            elif kind == 'Key': s['v'] = 'None'
            elif kind == 'String': s['v'] = strs[1] if len(strs) > 1 else ''
            elif kind == 'Button': s['v'] = strs[1] if len(strs) > 1 else name
        except (IndexError, ValueError):
            continue
        out.append(s)
    return out

classes = {}
for f in sorted(os.listdir(f'{SRC}/module/impl')) + ['../HudModule.java', '../TextHudModule.java']:
    text = open(f'{SRC}/module/impl/{f}', encoding='utf-8').read()
    heads = list(re.finditer(r'class (\w+)( extends (\w+))?', text))
    for i, h in enumerate(heads):
        body = text[h.start(): heads[i + 1].start() if i + 1 < len(heads) else len(text)]
        sup = re.search(r'super\(' + STR + r',\s*' + STR + r',\s*Category\.(\w+)', body)
        classes[h.group(1)] = {'parent': h.group(3), 'sup': sup and (sup.group(1), sup.group(2), sup.group(3)), 'set': settings_of(body)}

def all_settings(cls):
    out, c = [], cls
    while c in classes:
        out = classes[c]['set'] + out
        c = classes[c]['parent']
    return out

mm = open(f'{SRC}/module/ModuleManager.java', encoding='utf-8').read()
init = mm[mm.index('public static void init()'):]
mods = []
for m in re.finditer(r'\badd\(new ([\w.]+)\((.*?)\),\s*(true|false)\);', init, re.S):
    cls, args, on = m.group(1).split('.')[-1], m.group(2), m.group(3) == 'true'
    if cls == 'TextHudModule':
        s = re.match(r'\s*' + STR + r',\s*' + STR + r',\s*Category\.(\w+)', args)
        name, desc, cat = s.groups()
        sample = re.findall(r'\(\) -> (?:Lang\.t\()?' + STR, args)
        mods.append({'n': name, 'd': desc, 'c': cat, 'on': on, 'hud': True, 's': all_settings('TextHudModule'), 'x': sample[-1] if sample else name})
        continue
    c = classes.get(cls)
    if not c or not c['sup']: continue
    name, desc, cat = c['sup']
    hud = cls in ('Watermark',) or c['parent'] in ('HudModule', 'TextHudModule')
    mods.append({'n': name, 'd': desc, 'c': cat, 'on': on, 'hud': hud, 's': all_settings(cls)})

# ExtraModules: модули, собранные помощниками hud(...), Alert, OptionToggle и т.д.
ex = open(f'{SRC}/module/impl/ExtraModules.java', encoding='utf-8').read()
body = ex[ex.index('public static List<Module> all()'):ex.index('// =================================================================== помощники')]
CATS = {'H': 'HUD', 'C': 'COMBAT', 'M': 'MOVEMENT', 'R': 'RENDER', 'X': 'MISC', 'P': 'PERFORMANCE'}
for m in re.finditer(r'(hud|new Alert|new OptionToggle<>|new Volume|new ChatModule|new Action|new HoldKey|new (\w+)\(\))\(?(' + STR + r')?(?:,\s*' + STR + r')?(?:,\s*(\w)\b)?', body):
    kind = m.group(1)
    if m.group(2):
        c = classes.get(m.group(2))
        if not c or not c['sup']: continue
        name, desc, cat = c['sup']; mods.append({'n': name, 'd': desc, 'c': cat, 'on': False, 'hud': False, 's': all_settings(m.group(2))}); continue
    name, desc, cl = m.group(4), m.group(5), m.group(6)
    if not name: continue
    if kind == 'hud':
        sample = re.search(re.escape(name) + r'.*?,\s*' + STR + r'\)\);', body[m.start():m.start() + 900], re.S)
        mods.append({'n': name, 'd': desc, 'c': CATS.get(cl, 'HUD'), 'on': False, 'hud': True, 's': all_settings('TextHudModule'), 'x': sample.group(1) if sample else name})
    else:
        cat = 'MISC' if kind in ('new Alert', 'new Volume', 'new ChatModule') else CATS.get(cl, 'MISC')
        sets = {'new Alert': [{'t': 'bool', 'n': 'Звук', 'v': True}], 'new Volume': [{'t': 'number', 'n': 'Громкость', 'v': 0, 'min': 0, 'max': 100, 'step': 5, 'u': '%'}]}.get(kind, [])
        mods.append({'n': name, 'd': desc, 'c': cat, 'on': False, 'hud': False, 's': sets})

cats = re.findall(r'(\w+)\("([^"]+)", "([^"]+)"\)', open(f'{SRC}/module/Category.java', encoding='utf-8').read())
data = {'cats': [{'id': a, 't': b, 'i': c} for a, b, c in cats], 'mods': mods}
out = f'{ROOT}/launcher/src/ui/assets/client-modules.js'
open(out, 'w', encoding='utf-8').write('// Сгенерировано tools/gen_preview.py из исходников Aqua Client — не редактировать вручную\nwindow.AQUA_CLIENT = ' + json.dumps(data, ensure_ascii=False, separators=(',', ':')) + ';\n')
print(len(mods), 'modules,', sum(len(m['s']) for m in mods), 'settings,', sum(m['hud'] for m in mods), 'hud ->', out)

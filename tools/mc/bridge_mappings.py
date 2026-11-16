#!/usr/bin/env python3
"""Мост имён Yarn -> Mojang для 26.x (26.x без обфускации => intermediary пустой, нужен путь через Mojang).

Берём версию 1.21.11 (обфусцированную), где есть ВСЁ:
  - Mojang ProGuard: mojang-имя -> obf
  - Fabric intermediary: obf -> intermediary
  - Yarn: intermediary -> yarn-имя
Склеиваем: yarn <- intermediary <- obf <- mojang  =>  таблица mojang-имя -> yarn-имя.
В 26.x официальные имена = имена Mojang, поэтому эту таблицу прикладываем к 26.x:
official(26.x, =mojang) -> named(yarn). Loom подставит Yarn-имена, код компилируется как есть.
Символы, которых в 1.21.11 не было или которые поменялись в 26.x, остаются на официальном имени -> чиним //#if.

Запуск в CI: python3 tools/mc/bridge_mappings.py 1.21.11 out.tiny
"""
import io, json, re, sys, urllib.request, zipfile

UA = {'User-Agent': 'aqua-launcher-ci (github.com/mruliangg-cpu/aqua-launcher)'}
def getj(url): return json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60))
def getb(url): return urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=180).read()

def tiny_text(coord):
    g, a, v = coord.split(':')
    base = f'https://maven.fabricmc.net/{g.replace(".", "/")}/{a}/{v}'
    last = None
    for url in (f'{base}/{a}-{v}-v2.jar', f'{base}/{a}-{v}.jar'):
        try:
            z = zipfile.ZipFile(io.BytesIO(getb(url)))
            n = next(x for x in z.namelist() if x.endswith('mappings.tiny'))
            return z.read(n).decode('utf-8')
        except Exception as e:
            last = f'{url}: {e}'
    raise RuntimeError(f'нет jar для {coord} ({last})')

# ---- Mojang ProGuard: mojang -> obf ----
def to_desc(java_type):
    """'net.minecraft.world.Foo[]' -> '[Lnet/minecraft/world/Foo;' (в MOJANG-именах)."""
    arr = ''
    while java_type.endswith('[]'):
        arr += '['; java_type = java_type[:-2]
    prim = {'void': 'V', 'boolean': 'Z', 'byte': 'B', 'char': 'C', 'short': 'S',
            'int': 'I', 'long': 'J', 'float': 'F', 'double': 'D'}
    if java_type in prim:
        return arr + prim[java_type]
    return arr + 'L' + java_type.replace('.', '/') + ';'

def parse_proguard(text):
    """-> classes {moj_cls: obf_cls}, methods {obf_cls: {(obf_name, moj_desc): moj_name}}, fields {obf_cls: {obf_name: moj_name}}"""
    classes, methods, fields = {}, {}, {}
    cur_obf = None
    for ln in text.split('\n'):
        if not ln or ln.startswith('#'):
            continue
        if not ln.startswith('    '):
            m = re.match(r'(\S+) -> (\S+):', ln)
            if m:
                moj, obf = m.group(1), m.group(2)
                classes[moj] = obf
                cur_obf = obf
                methods.setdefault(obf, {}); fields.setdefault(obf, {})
        else:
            s = ln.strip()
            mm = re.match(r'(?:\d+:\d+:)?(\S+) (\w+)\((.*)\) -> (\S+)', s)
            if mm:
                ret, name, args, obf = mm.groups()
                argd = ''.join(to_desc(a) for a in args.split(',') if a)
                methods[cur_obf][(obf, '(' + argd + ')' + to_desc(ret))] = name
            else:
                fm = re.match(r'(\S+) (\w+) -> (\S+)', s)
                if fm:
                    fields[cur_obf][fm.group(3)] = fm.group(2)
    return classes, methods, fields

# ---- intermediary v1: obf -> intermediary (с obf-дескрипторами) ----
def parse_inter_v1(text):
    cls = {}   # obf -> inter
    meth = {}  # obf_cls -> {(obf_name, obf_desc): inter_name}
    fld = {}   # obf_cls -> {obf_name: inter_name}
    for ln in text.split('\n'):
        if not ln or ln.startswith('v1\t'):
            continue
        p = ln.split('\t')
        if p[0] == 'CLASS' and len(p) >= 3:
            cls[p[1]] = p[2]
        elif p[0] == 'METHOD' and len(p) >= 5:
            meth.setdefault(p[1], {})[(p[3], p[2])] = p[4]
        elif p[0] == 'FIELD' and len(p) >= 5:
            fld.setdefault(p[1], {})[p[3]] = p[4]
    return cls, meth, fld

# ---- yarn v2: intermediary -> named ----
def parse_yarn(text):
    cls = {}; meth = {}; fld = {}
    cur = None
    for ln in text.split('\n'):
        if not ln or ln.startswith('tiny\t'):
            continue
        p = ln.strip().split('\t')
        if ln[0] == 'c':
            cur = p[1]
            if len(p) >= 3: cls[p[1]] = p[2]
        elif p and p[0] == 'm' and cur and len(p) >= 4:
            meth[(cur, p[2])] = p[3]      # (owner_inter, method_inter) -> yarn
        elif p and p[0] == 'f' and cur and len(p) >= 4:
            fld[(cur, p[2])] = p[3]
    return cls, meth, fld

def main():
    base, out = sys.argv[1], sys.argv[2]
    # Mojang client mappings для base
    manifest = getj('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json')
    vurl = next(v['url'] for v in manifest['versions'] if v['id'] == base)
    vjson = getj(vurl)
    pg = urllib.request.urlopen(urllib.request.Request(vjson['downloads']['client_mappings']['url'], headers=UA), timeout=180).read().decode('utf-8')
    mcls, mmeth, mfld = parse_proguard(pg)
    print(f'mojang: classes={len(mcls)}', file=sys.stderr)
    # intermediary base
    icls, imeth, ifld = parse_inter_v1(tiny_text('net.fabricmc:intermediary:' + base))
    print(f'intermediary: classes={len(icls)}', file=sys.stderr)
    # yarn base
    yarn_ver = getj(f'https://meta.fabricmc.net/v2/versions/yarn/{base}')[0]['version']
    ycls, ymeth, yfld = parse_yarn(tiny_text('net.fabricmc:yarn:' + yarn_ver))
    print(f'yarn: classes={len(ycls)} ({yarn_ver})', file=sys.stderr)

    obf2moj = {o: m for m, o in mcls.items()}
    o = ['tiny\t2\t0\tofficial\tnamed']
    hits = 0
    for moj_cls, obf_cls in mcls.items():
        inter_cls = icls.get(obf_cls)
        if not inter_cls:
            continue
        yarn_cls = ycls.get(inter_cls)
        if not yarn_cls:
            continue
        hits += 1
        moj_slash = moj_cls.replace('.', '/')
        o.append(f'c\t{moj_slash}\t{yarn_cls}')
        # методы
        for (obf_name, moj_desc), moj_name in mmeth.get(obf_cls, {}).items():
            # intermediary для этого метода ищем по obf-имени: подберём любой с тем же obf name
            inter_name = None
            for (oN, oDesc), iName in imeth.get(obf_cls, {}).items():
                if oN == obf_name:
                    inter_name = iName; break
            if inter_name is None:
                continue
            yarn_name = ymeth.get((inter_cls, inter_name))
            if yarn_name and yarn_name != moj_name:
                o.append(f'\tm\t{moj_desc}\t{moj_name}\t{yarn_name}')
        # поля
        for obf_name, moj_name in mfld.get(obf_cls, {}).items():
            inter_name = ifld.get(obf_cls, {}).get(obf_name)
            if not inter_name:
                continue
            yarn_name = yfld.get((inter_cls, inter_name))
            if yarn_name and yarn_name != moj_name:
                o.append(f'\tf\t\t{moj_name}\t{yarn_name}')
    open(out, 'w', encoding='utf-8').write('\n'.join(o) + '\n')
    print(f'написал {out}: классов-мостов {hits}', file=sys.stderr)

if __name__ == '__main__':
    main()

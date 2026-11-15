#!/usr/bin/env python3
"""Находит для версии Minecraft: Yarn, Fabric API, Java. Пишет строку -P аргументов для Gradle.
Также умеет выдать список поддерживаемых версий (matrix) для GitHub Actions."""
import json, re, sys, urllib.request, urllib.parse

UA = {'User-Agent': 'aqua-launcher-ci (github.com/mruliangg-cpu/aqua-launcher)'}
def get(url):
    return json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=30))

def vkey(v): return tuple(int(x) for x in re.findall(r'\d+', v))

def versions():
    """Все стабильные версии от 1.16 до 1.21.x, у которых есть Yarn (26.x без Yarn — отдельная ветка)."""
    games = [g['version'] for g in get('https://meta.fabricmc.net/v2/versions/game') if g['stable']]
    yarn = {y['gameVersion'] for y in get('https://meta.fabricmc.net/v2/versions/yarn')}
    out = [v for v in games if re.fullmatch(r'1\.(1[6-9]|2[0-9])(\.\d+)?', v) and v in yarn]
    return sorted(out, key=vkey)

def props(mc):
    yarn = get(f'https://meta.fabricmc.net/v2/versions/yarn/{mc}')[0]['version']
    q = urllib.parse.urlencode({'game_versions': json.dumps([mc]), 'loaders': json.dumps(['fabric'])})
    api = get(f'https://api.modrinth.com/v2/project/fabric-api/version?{q}')[0]['version_number']
    loader = next(l['version'] for l in get('https://meta.fabricmc.net/v2/versions/loader') if l['stable'])
    java = 21 if vkey(mc) >= (1, 20, 5) else 17
    loom = '1.13-SNAPSHOT' if vkey(mc) >= (1, 21, 11) else '1.11-SNAPSHOT' if vkey(mc) >= (1, 21, 9) else '1.10-SNAPSHOT'
    return {'loom_version': loom, 'minecraft_version': mc, 'yarn_mappings': yarn, 'fabric_version': api, 'loader_version': loader, 'java_version': java}

if __name__ == '__main__':
    if sys.argv[1] == 'matrix':
        print(json.dumps(versions()))
    else:
        p = props(sys.argv[1])
        print(' '.join(f'-P{k}={v}' for k, v in p.items()))

#!/usr/bin/env python3
"""Проверяет, что нужно для порта мода на 26.x: есть ли у Fabric intermediary, Fabric API, loader, какой Loom.
Ничего не собирает — только спрашивает мета-сервисы и печатает, что доступно."""
import json, urllib.request, urllib.parse, urllib.error

UA = {'User-Agent': 'aqua-launcher-ci (github.com/mruliangg-cpu/aqua-launcher)'}

def get(url):
    return json.load(urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=30))

def head_ok(url):
    try:
        req = urllib.request.Request(url, headers=UA, method='HEAD')
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status == 200
    except Exception as e:
        return f'no ({e})'

print('== Fabric game versions containing "26" ==')
try:
    games = [g['version'] for g in get('https://meta.fabricmc.net/v2/versions/game')]
    v26 = [v for v in games if v.startswith('26')]
    print('  ', v26[:20] or 'none')
except Exception as e:
    print('  game list error:', e)

print('== Fabric intermediary for 26.x (ALL) ==')
try:
    allinter = get('https://meta.fabricmc.net/v2/versions/intermediary')
    inter26 = [i for i in allinter if i['version'].startswith('26') and 'snapshot' not in i['version'] and 'pre' not in i['version'] and 'rc' not in i['version']]
    for i in inter26:
        print('  ', i['version'], '->', i.get('maven'))
    print('  (total 26.x full:', len(inter26), ')')
except Exception as e:
    print('  intermediary meta error:', e)

print('== direct intermediary endpoint per version ==')
for v in ['26.1', '26.1.1', '26.2', '26.3']:
    try:
        r = get(f'https://meta.fabricmc.net/v2/versions/intermediary/{v}')
        print(f'  /{v}:', (r[0].get('maven') if r else 'EMPTY LIST'))
    except Exception as e:
        print(f'  /{v}: error {e}')

for v in ['26.1', '26.2', '26.3', '26.4']:
    print(f'== {v} ==')
    print('   intermediary jar:', head_ok(f'https://maven.fabricmc.net/net/fabricmc/intermediary/{v}/intermediary-{v}-v2.jar'))
    try:
        q = urllib.parse.urlencode({'game_versions': json.dumps([v]), 'loaders': json.dumps(['fabric'])})
        api = get(f'https://api.modrinth.com/v2/project/fabric-api/version?{q}')
        print('   fabric-api:', api[0]['version_number'] if api else 'none')
    except Exception as e:
        print('   fabric-api error:', e)

print('== latest yarn (expect none for 26.x) ==')
try:
    yarn26 = [y['version'] for y in get('https://meta.fabricmc.net/v2/versions/yarn') if y['gameVersion'].startswith('26')]
    print('  yarn for 26.x:', yarn26[:5] or 'none (as expected)')
except Exception as e:
    print('  yarn meta error:', e)

print('== Loom 1.17 required Gradle (from module metadata) ==')
try:
    import xml.etree.ElementTree as ET
    meta = urllib.request.urlopen(urllib.request.Request('https://maven.fabricmc.net/net/fabricmc/fabric-loom/1.17-SNAPSHOT/maven-metadata.xml', headers=UA), timeout=30).read().decode()
    root = ET.fromstring(meta)
    sv = root.find('.//snapshotVersions')
    modfile = None
    for sv2 in root.iter('snapshotVersion'):
        ext = sv2.findtext('extension')
        if ext == 'module':
            val = sv2.findtext('value')
            modfile = f'fabric-loom-1.17-SNAPSHOT-{val}.module'
    print('   module file:', modfile)
    if modfile:
        mod = get(f"https://maven.fabricmc.net/net/fabricmc/fabric-loom/1.17-SNAPSHOT/{modfile}")
        for var in mod.get('variants', []):
            attrs = var.get('attributes', {})
            av = attrs.get('org.gradle.plugin.api-version')
            jv = attrs.get('org.gradle.jvm.version')
            if av:
                print(f'   variant {var.get("name")}: gradle-api={av} jvm={jv}')
except Exception as e:
    print('   loom module error:', e)

print('== loader (latest stable) ==')
try:
    loader = next(l['version'] for l in get('https://meta.fabricmc.net/v2/versions/loader') if l['stable'])
    print('  loader:', loader)
except Exception as e:
    print('  loader error:', e)

# Гардероб для облачного теста: по одной вещи в каждом слоте.
import json
d = json.load(open('src/main/resources/assets/aquaclient/cosmetics.json', encoding='utf-8'))
o = {'stamp': 'selftest'}
for s in d['slots']:
    ids = [x['id'] for x in d['items'][s] if x['id'] != 'none']
    o[s] = ids[1] if len(ids) > 1 else ids[0]
json.dump(o, open('run/config/aquaclient-loadout.json', 'w'))
print(o)

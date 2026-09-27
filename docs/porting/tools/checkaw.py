import json, sys, re
vs = ['1.20.1', '1.21.1', '26.1', '26.1.2', '26.2', '26.3']
D = {v: json.load(open(f'{sys.argv[1]}/maps/{v}.json')) for v in vs}
RENAMES = {'net/minecraft/resources/ResourceLocation': 'net/minecraft/resources/Identifier',
           'net/minecraft/data/worldgen/BootstapContext': 'net/minecraft/data/worldgen/BootstrapContext',
           'net/minecraft/Util': 'net/minecraft/util/Util'}
def ren(s):
    for a, b in RENAMES.items():
        s = s.replace(a + ';', b + ';').replace(a + '$', b + '$')
        if s == a:
            s = b
    return s
for line in open(sys.argv[2]):
    parts = line.split()
    if len(parts) < 3 or parts[0].startswith('accessWidener') or parts[0].startswith('#'):
        continue
    acc, kind, owner = parts[0], parts[1], parts[2]
    name = parts[3] if len(parts) > 3 else None
    desc = parts[4] if len(parts) > 4 else None
    res = []
    for v in vs:
        o = owner if v in ('1.20.1',) else ren(owner)
        c = D[v].get(o)
        if c is None:
            res.append('CLASS-GONE'); continue
        if kind == 'class':
            res.append('ok'); continue
        d = ren(desc) if v != '1.20.1' else desc
        if kind == 'field':
            f = c['fields'].get(name)
            res.append('ok' if f and f[0] == d else ('TYPE:' + f[0] if f else 'MISSING'))
        else:
            if name + d in c['methods']:
                res.append('ok')
            else:
                alts = [k for k in c['methods'] if k.startswith(name + '(')]
                res.append('SIG' if alts else 'MISSING')
    first_bad = next((v for v, r in zip(vs, res) if r != 'ok'), None)
    short = owner.replace('net/minecraft/', '') + ('.' + name if name else '')
    print(f"{'ok      ' if not first_bad else 'BREAKS@' + first_bad:9} {kind:6} {short:75} " + ' '.join(f'{v}={r}' for v, r in zip(vs, res) if r != 'ok'))

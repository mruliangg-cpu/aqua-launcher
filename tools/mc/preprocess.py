#!/usr/bin/env python3
"""Препроцессор Aqua Client: один исходный код → отдельная сборка под каждую версию Minecraft.

В .java/.json файлах можно писать:
    //#if MC >= 1.20.5
    ...код для новых версий...
    //#elseif MC >= 1.20
    ...
    //#else
    ...
    //#endif
Условия: MC >= / <= / > / < / == / != версия, можно соединять через && и ||.
Использование: preprocess.py <исходный каталог aqua-client> <выходной каталог> <версия MC>
"""
import os, re, shutil, sys

def vkey(v):
    return tuple(int(x) for x in re.findall(r'\d+', v)) + (0,) * (4 - len(re.findall(r'\d+', v)))

def cond(expr, mc):
    def one(m):
        op, ver = m.group(1), m.group(2)
        a, b = vkey(mc), vkey(ver)
        return str({'>=': a >= b, '<=': a <= b, '>': a > b, '<': a < b, '==': a == b, '!=': a != b}[op])
    e = re.sub(r'MC\s*(>=|<=|==|!=|>|<)\s*([\d.]+)', one, expr)
    e = e.replace('&&', ' and ').replace('||', ' or ').replace('!', ' not ')
    return bool(eval(e, {'__builtins__': {}}, {'True': True, 'False': False}))

DIRECTIVE = re.compile(r'^\s*//#(if|elseif|else|endif)\b(.*)$')

def process(text, mc, path):
    out, stack = [], []  # stack: [active_parent, branch_taken, active_now]
    for n, line in enumerate(text.split('\n'), 1):
        m = DIRECTIVE.match(line)
        if m:
            kind, expr = m.group(1), m.group(2).strip()
            parent = all(s[2] for s in stack)
            if kind == 'if':
                c = cond(expr, mc); stack.append([parent, c, c])
            elif kind == 'elseif':
                s = stack[-1]; c = (not s[1]) and cond(expr, mc); s[2] = c; s[1] = s[1] or c
            elif kind == 'else':
                s = stack[-1]; s[2] = not s[1]; s[1] = True
            else:
                stack.pop()
            out.append('')
            continue
        out.append(line if all(s[2] for s in stack) else '')
    if stack: raise SystemExit(f'{path}: незакрытый //#if')
    return '\n'.join(out)

def main(src, dst, mc):
    if os.path.exists(dst): shutil.rmtree(dst)
    for root, dirs, files in os.walk(src):
        dirs[:] = [d for d in dirs if d not in ('build', '.gradle', 'run')]
        for f in files:
            a = os.path.join(root, f); b = os.path.join(dst, os.path.relpath(a, src))
            os.makedirs(os.path.dirname(b), exist_ok=True)
            if f.endswith(('.java', '.json')) and '/src/' in a.replace('\\', '/'):
                t = open(a, encoding='utf-8').read()
                if '//#' in t: t = process(t, mc, a)
                open(b, 'w', encoding='utf-8').write(t)
            else:
                shutil.copy2(a, b)

if __name__ == '__main__':
    main(*sys.argv[1:4])

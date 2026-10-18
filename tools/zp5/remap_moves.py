"""Rewrite references to classes that Minecraft 26.3 moved (mostly com.mojang.blaze3d -> com.mojang.renderpearl).

The move list, class-moves-26.3.txt, is the one ZP5's Create Fly NeoForge port generated from the 26.2 and 26.3 jars
(mods/create-fly-neoforge/tools/remap_26_3_imports.py). Rewrites imports, code and mixin descriptors in place.

    python -I tools/zp5/remap_moves.py src/main/java
"""
import pathlib
import re
import sys

moves = pathlib.Path(__file__).resolve().parent / 'class-moves-26.3.txt'
mapping = dict(line.split() for line in moves.read_text(encoding='utf-8').splitlines() if line.strip())
pattern = re.compile(r'\b(' + '|'.join(re.escape(k) for k in sorted(mapping, key=len, reverse=True)) + r')\b(?![\w])')
slash = re.compile(r'L(' + '|'.join(re.escape(k.replace('.', '/')) for k in sorted(mapping, key=len, reverse=True)) + r')([;$])')
changed = 0
for root in sys.argv[1:]:
    for f in pathlib.Path(root).rglob('*.java'):
        raw = f.read_bytes().decode('utf-8')
        out = pattern.sub(lambda m: mapping[m.group(1)], raw)
        out = slash.sub(lambda m: 'L' + mapping[m.group(1).replace('/', '.')].replace('.', '/') + m.group(2), out)
        if out != raw:
            f.write_bytes(out.encode('utf-8'))
            changed += 1
print(f'{len(mapping)} moved classes, {changed} files rewritten')

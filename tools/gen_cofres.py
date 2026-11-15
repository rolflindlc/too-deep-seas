#!/usr/bin/env python3
"""Genera los datos de los cofres del tesoro: tablas de botín, modelos, texturas provisionales y etiquetas de bioma.
Uso: python3 tools/gen_cofres.py   (desde la raíz del repositorio; las texturas necesitan Pillow)

Cada entrada es (objeto, peso, mín, máx) o (objeto, peso, mín, máx, extra):
  extra = {"lvl": 2}        accesorio del mod de ese nivel
  extra = {"enchant": True} encantamiento al azar (libros)
  extra = {"levels": (a,b)} encantado como en una mesa con esos niveles
"""
import json, os, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
M = 'toodeepseas:'
WATER = ['oceano', 'calido', 'helado', 'rio', 'pantano']
BIOMAS = WATER + ['lava', 'vacio']
CALIDADES = ['sencillo', 'reforzado', 'ornamentado', 'ancestral']

BIOMA = {
 'oceano': [('cod',10,2,4),('salmon',6,1,3),('kelp',8,2,6),('ink_sac',6,1,3),('prismarine_shard',6,1,4),
            ('prismarine_crystals',4,1,3),('nautilus_shell',2,1,1),('sea_pickle',3,1,2),('iron_nugget',5,3,8),
            (M+'lombriz',5,2,4),(M+'masa_pan',3,2,4)],
 'calido': [('tropical_fish',10,1,3),('pufferfish',5,1,2),('sea_pickle',6,1,3),('tube_coral',2,1,2),('brain_coral',2,1,2),
            ('bubble_coral',2,1,2),('fire_coral',2,1,2),('horn_coral',2,1,2),(M+'gamba_arrecife',6,2,4),('scute',2,1,1),
            ('nautilus_shell',2,1,1),('gold_nugget',5,2,6)],
 'helado': [('cod',8,2,4),('salmon',10,2,4),('ice',6,2,5),('packed_ice',4,1,3),('blue_ice',1,1,1),('snowball',5,4,8),
            (M+'cebo_helado',5,2,4),('bone',4,1,3),('iron_nugget',5,3,8),('string',4,1,3)],
 'rio':    [('salmon',10,2,4),('clay_ball',8,3,8),('gold_nugget',4,1,4),('sugar_cane',5,1,4),('stick',4,2,4),('string',5,1,3),
            (M+'lombriz',7,2,5),('bowl',2,1,1),('flint',4,1,3),('iron_nugget',5,3,8)],
 'pantano':[('slime_ball',8,1,3),('lily_pad',8,1,3),('vine',5,1,3),('mangrove_propagule',3,1,2),('bone',6,1,3),
            ('rotten_flesh',6,1,3),(M+'carne_podrida_aderezada',5,2,4),('brown_mushroom',4,1,3),('red_mushroom',4,1,3),
            ('glass_bottle',3,1,2),('glow_ink_sac',2,1,1),('gunpowder',3,1,2),('spider_eye',3,1,2)],
 'lava':   [('quartz',10,2,6),('gold_nugget',10,3,9),('magma_cream',7,1,3),('blaze_powder',5,1,2),('glowstone_dust',6,2,5),
            (M+'crema_magma_cebada',6,2,4),('obsidian',4,1,2),('gold_ingot',4,1,2),('nether_wart',4,1,3),('fire_charge',4,1,3)],
 'vacio':  [('ender_pearl',10,1,3),('chorus_fruit',8,2,5),('end_stone',5,2,6),('obsidian',5,1,3),(M+'fragmento_vacio',4,1,1),
            ('popped_chorus_fruit',5,2,4),('purpur_block',4,2,4),('phantom_membrane',4,1,2),('end_rod',3,1,2)],
}

AGUA_PROFUNDO = [('iron_ingot',6,1,3),('gold_ingot',4,1,2),('lapis_lazuli',5,2,5),('emerald',3,1,2),('name_tag',2,1,1),
                 ('saddle',2,1,1),('experience_bottle',4,1,3),(M+'baya_luminosa',4,2,4)]
AGUA_ABISAL = [('diamond',5,1,2),('gold_ingot',5,2,4),('emerald',5,1,3),('book',6,1,1,{'enchant':True}),
               ('experience_bottle',5,2,4),(M+'cebo_tinta_brillante',5,2,4),('amethyst_shard',4,2,5)]
PROFUNDO = {
 'oceano': AGUA_PROFUNDO + [('nautilus_shell',4,1,1),('prismarine_crystals',5,2,4)],
 'calido': AGUA_PROFUNDO + [('nautilus_shell',3,1,1),('scute',3,1,2),('sponge',2,1,1)],
 'helado': AGUA_PROFUNDO + [('blue_ice',4,1,3),('nautilus_shell',3,1,1)],
 'rio':    AGUA_PROFUNDO + [('gold_nugget',6,4,9),('raw_gold',3,1,2)],
 'pantano':AGUA_PROFUNDO + [('slime_block',3,1,1),('glow_ink_sac',4,1,3)],
 'lava':   [('gold_ingot',8,2,4),('gilded_blackstone',4,1,3),('crying_obsidian',5,1,3),('magma_cream',5,2,4),
            ('golden_carrot',4,2,4),('spectral_arrow',4,4,10),('iron_ingot',5,2,4),('experience_bottle',4,1,3)],
 'vacio':  [('shulker_shell',4,1,1),('dragon_breath',5,1,2),('ender_eye',5,1,2),('diamond',5,1,2),
            (M+'fragmento_vacio',6,1,2),('book',5,1,1,{'enchant':True}),('end_crystal',2,1,1)],
}
ABISAL = {
 'oceano': AGUA_ABISAL + [('heart_of_the_sea',1,1,1),('trident',1,1,1),('sponge',4,1,2)],
 'calido': AGUA_ABISAL + [('heart_of_the_sea',1,1,1),('sponge',5,1,2),('nautilus_shell',4,1,2)],
 'helado': AGUA_ABISAL + [('heart_of_the_sea',1,1,1),('trident',1,1,1),('blue_ice',5,2,4)],
 'rio':    AGUA_ABISAL + [('gold_block',2,1,1),('raw_gold',5,2,4)],
 'pantano':AGUA_ABISAL + [('emerald_block',2,1,1),('slime_block',4,1,2),('music_disc_13',1,1,1)],
 'lava':   [('ancient_debris',2,1,1),('netherite_scrap',2,1,1),('netherite_upgrade_smithing_template',1,1,1),('gold_block',4,1,2),
            ('diamond',5,1,3),('wither_skeleton_skull',1,1,1),('ghast_tear',4,1,2),('book',5,1,1,{'enchant':True}),
            ('experience_bottle',5,2,4)],
 'vacio':  [('elytra',1,1,1),('dragon_head',1,1,1),('shulker_shell',6,1,2),(M+'fragmento_vacio',8,2,3),('diamond',6,2,4),
            ('netherite_scrap',2,1,1),('book',6,1,1,{'enchant':True}),('experience_bottle',5,2,4)],
}

LLUVIA_COMUN = [(M+'lombriz',6,2,5),(M+'masa_pan',4,2,4),('string',4,1,3),('experience_bottle',3,1,2),('glow_ink_sac',3,1,2)]
TORMENTA_COMUN = [(M+'cebo_cobre_cargado',7,2,4),('copper_ingot',6,2,5),('lightning_rod',3,1,1),('experience_bottle',5,1,3),
                  ('book',2,1,1,{'enchant':True})]
LLUVIA = {
 'oceano': [('cod',6,2,4),('ink_sac',4,1,3)],
 'calido': [('tropical_fish',6,1,3),('pufferfish',4,1,2)],
 'helado': [('salmon',6,2,4),('packed_ice',4,1,3)],
 'rio':    [('salmon',6,2,4),('clay_ball',5,3,6)],
 'pantano':[('slime_ball',6,2,4),('lily_pad',4,1,3),('golden_helmet',1,1,1)],
}
TORMENTA = {
 'oceano': [('trident',1,1,1),('prismarine_shard',5,2,5)],
 'calido': [('prismarine_crystals',5,2,4),('nautilus_shell',3,1,1)],
 'helado': [('trident',1,1,1),('blue_ice',4,1,3)],
 'rio':    [('raw_copper',5,2,5),('gold_nugget',4,3,7)],
 'pantano':[('gunpowder',5,1,3),('glowstone_dust',4,2,4)],
}

def acc(name, w, lvl=1):
    return (M+name, w, 1, 1, {'lvl': lvl}) if lvl > 1 else (M+name, w, 1, 1)

CALIDAD = {
 'sencillo': [(M+'lombriz',8,2,4),(M+'masa_pan',6,2,4),(M+'corcho_abedul',5,1,3),('string',5,1,3),('iron_nugget',5,2,6),
              ('emerald',3,1,1),acc('plomada',3),acc('flotador',3),acc('sedal_reforzado',3),acc('anzuelo_afilado',3),acc('carrete',2)],
 'reforzado': [(M+'baya_luminosa',5,2,4),(M+'carne_podrida_aderezada',5,2,4),(M+'cebo_helado',4,2,4),('iron_ingot',6,1,3),
               ('gold_ingot',4,1,2),('experience_bottle',5,1,3),('fishing_rod',3,1,1,{'levels':(10,20)}),
               acc('sedal_seda',3),acc('sedal_trenzado',3),acc('anzuelo_iman',3),acc('lastre_buceo',3),acc('carrete_escape',2),
               acc('plomada',2,2),acc('sedal_reforzado',2,2),acc('anzuelo_afilado',2,2),acc('flotador',2,2)],
 'ornamentado': [('diamond',5,1,2),('gold_ingot',5,2,4),(M+'pepita_cebo',4,1,2),(M+'cebo_tinta_brillante',5,2,4),
                 ('book',5,1,1,{'enchant':True}),('fishing_rod',3,1,1,{'levels':(20,30)}),
                 acc('sedal_invisible',3),acc('anzuelo_doble',3),acc('ojo_nautilo',2),acc('perla_encantada',2),acc('carrete_freno',2),
                 acc('geoda_resonante',2),acc('sedal_seda',2,2),acc('anzuelo_iman',2,2),acc('lastre_buceo',2,2),acc('carrete',2,2)],
 'ancestral': [('diamond',6,2,3),('netherite_scrap',2,1,1),(M+'pepita_cebo',5,1,3),('book',6,1,1,{'enchant':True}),
               ('experience_bottle',6,3,6),(M+'fragmento_vacio',1,1,1),
               acc('escama_guardian',2),acc('corazon_marea',2),acc('linterna_abisal',2),acc('ojo_nautilo',2,2),acc('perla_encantada',2,2),
               acc('plomada',2,3),acc('sedal_reforzado',2,3),acc('anzuelo_afilado',2,3),acc('carrete',2,3)],
}

def entry(e):
    name, weight, lo, hi = e[:4]
    extra = e[4] if len(e) > 4 else {}
    out = {'type': 'minecraft:item', 'name': name if ':' in name else 'minecraft:' + name, 'weight': weight}
    fn = []
    if hi > 1:
        fn.append({'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}})
    if extra.get('lvl'):
        fn.append({'function': 'minecraft:set_nbt', 'tag': '{Lvl:%d}' % extra['lvl']})
    if extra.get('enchant'):
        fn.append({'function': 'minecraft:enchant_randomly'})
    if extra.get('levels'):
        a, b = extra['levels']
        fn.append({'function': 'minecraft:enchant_with_levels', 'levels': {'type': 'minecraft:uniform', 'min': a, 'max': b}, 'treasure': True})
    if fn:
        out['functions'] = fn
    return out

def write(path, obj):
    path = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')

def table(path, entries):
    write('data/toodeepseas/loot_tables/cofres/' + path + '.json',
          {'type': 'minecraft:chest', 'pools': [{'rolls': 1, 'entries': [entry(e) for e in entries]}]})

for b in BIOMAS:
    table('bioma/' + b, BIOMA[b])
    table('profundidad/%s_profundo' % b, PROFUNDO[b])
    table('profundidad/%s_abisal' % b, ABISAL[b])
for b in WATER:
    table('clima/%s_lluvia' % b, LLUVIA_COMUN + LLUVIA[b])
    table('clima/%s_tormenta' % b, TORMENTA_COMUN + TORMENTA[b])
for c in CALIDADES:
    table('calidad/' + c, CALIDAD[c])

# Etiquetas de bioma: qué bioma de Minecraft cuenta como cada bioma de pesca (lo que no esté en ninguna es río)
def opt(tag):
    return {'id': tag, 'required': False}
TAGS = {
 'pantano': ['minecraft:swamp', 'minecraft:mangrove_swamp', opt('#forge:is_swamp')],
 'helado': ['minecraft:frozen_ocean', 'minecraft:deep_frozen_ocean', 'minecraft:cold_ocean', 'minecraft:deep_cold_ocean',
            'minecraft:frozen_river', opt('#forge:is_snowy')],
 'calido': ['minecraft:warm_ocean', 'minecraft:lukewarm_ocean', 'minecraft:deep_lukewarm_ocean'],
 'oceano': ['#minecraft:is_ocean'],
}
for name, values in TAGS.items():
    write('data/toodeepseas/tags/worldgen/biome/pesca/%s.json' % name, {'replace': False, 'values': values})

# Pesca vanilla (provisional): 10 % de las capturas son un cofre
write('data/toodeepseas/loot_modifiers/cofre_pesca.json', {
    'type': 'toodeepseas:cofre_pesca',
    'conditions': [
        {'condition': 'forge:loot_table_id', 'loot_table_id': 'minecraft:gameplay/fishing'},
        {'condition': 'minecraft:random_chance', 'chance': 0.1},
    ]})
glm = 'data/forge/loot_modifiers/global_loot_modifiers.json'
with open(os.path.join(ROOT, glm), encoding='utf-8') as f:
    g = json.load(f)
if 'toodeepseas:cofre_pesca' not in g['entries']:
    g['entries'].append('toodeepseas:cofre_pesca')
write(glm, g)

# Modelos: uno por bioma y calidad; el del objeto elige con la propiedad toodeepseas:calidad (0, 1/3, 2/3, 1)
for b in BIOMAS:
    for i, c in enumerate(CALIDADES):
        write('assets/toodeepseas/models/item/cofre_%s%s.json' % (b, '' if i == 0 else '_' + c), {
            'parent': 'minecraft:item/generated', 'textures': {'layer0': 'toodeepseas:item/cofre_%s_%s' % (b, c)},
            **({'overrides': [{'predicate': {'toodeepseas:calidad': t}, 'model': 'toodeepseas:item/cofre_%s_%s' % (b, CALIDADES[j])}
                              for j, t in ((1, 0.3), (2, 0.6), (3, 0.9))]} if i == 0 else {})})

# Texturas provisionales 16×16: caja del color del bioma con herrajes del color de la calidad
try:
    from PIL import Image
except ImportError:
    print('Sin Pillow: no se generan las texturas'); sys.exit(0)
CUERPO = {'oceano': (46, 98, 168), 'calido': (30, 160, 170), 'helado': (150, 200, 232), 'rio': (122, 90, 52),
          'pantano': (78, 96, 50), 'lava': (150, 46, 30), 'vacio': (62, 38, 96)}
HERRAJE = {'sencillo': (104, 78, 46), 'reforzado': (176, 182, 190), 'ornamentado': (236, 196, 64), 'ancestral': (96, 232, 220)}
def sh(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c) + (255,)
for b in BIOMAS:
    for c in CALIDADES:
        im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        body, trim = CUERPO[b], HERRAJE[c]
        for y in range(3, 14):
            for x in range(1, 15):
                k = 1.15 if y < 7 else (0.8 if y == 7 else 0.95)
                if (x + y) % 5 == 0: k *= 0.9
                im.putpixel((x, y), sh(body, k))
        for x in range(1, 15):          # borde
            im.putpixel((x, 3), sh(body, 0.55)); im.putpixel((x, 13), sh(body, 0.55))
        for y in range(3, 14):
            im.putpixel((1, y), sh(body, 0.55)); im.putpixel((14, y), sh(body, 0.55))
        for y in range(3, 14):          # herrajes verticales
            for x in (3, 12):
                im.putpixel((x, y), sh(trim, 1.0 if y != 7 else 0.7))
        for x in range(1, 15):          # separación de la tapa
            im.putpixel((x, 7), sh(trim if x in (3, 12) else body, 0.6))
        for y in (7, 8, 9):             # cerradura
            for x in (7, 8):
                im.putpixel((x, y), sh(trim, 1.25 if y == 7 else 1.0))
        im.putpixel((7, 9), sh(trim, 0.5))
        if c == 'ancestral':            # brillo
            for p in ((5, 5), (10, 11), (9, 4)):
                im.putpixel(p, (255, 255, 255, 255))
        out = os.path.join(ROOT, 'assets/toodeepseas/textures/item/cofre_%s_%s.png' % (b, c))
        im.save(out)
print('Cofres generados')

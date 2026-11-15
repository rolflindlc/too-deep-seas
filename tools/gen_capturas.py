#!/usr/bin/env python3
"""Genera todo lo de las capturas como objetos: catálogo (toodeepseas_capturas.json, lo lee core/juego/Catalogo),
modelos, texturas provisionales, textos, recetas (horno, despiece, platos, Farmer's Delight), etiquetas y botín.
Uso: python3 tools/gen_capturas.py   (desde la raíz del repositorio; las texturas necesitan Pillow)

Los peces salen de data/toodeepseas/fish/*.json; los tesoros, de la lista del prototipo (mismo orden).
"""
import glob, hashlib, json, math, os, shutil

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
RES = os.path.join(BASE, 'src', 'main', 'resources')
DATA = os.path.join(RES, 'data', 'toodeepseas')
ASSETS = os.path.join(RES, 'assets', 'toodeepseas')
M = 'toodeepseas:'
FD = 'farmersdelight'
BIOMAS = ['oceano', 'calido', 'helado', 'rio', 'pantano', 'lava', 'vacio']
AGUA = BIOMAS[:5]  # un tesoro sin biomas sale en cualquier agua, no en la lava ni en el Vacío (como en el prototipo)
VANILLA_FISH = {'bacalao': 'minecraft:cod', 'salmon': 'minecraft:salmon', 'pez_tropical': 'minecraft:tropical_fish',
                'pez_globo': 'minecraft:pufferfish'}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        if isinstance(obj, str):
            f.write(obj)
        else:
            json.dump(obj, f, ensure_ascii=False, indent=2)
            f.write('\n')


def mc(i):
    return i if ':' in i else 'minecraft:' + i


# ---------------------------------------------------------------- peces
peces = []
for f in glob.glob(os.path.join(DATA, 'fish', '*.json')):
    d = json.load(open(f, encoding='utf-8'))
    d['id'] = os.path.basename(f)[:-5]
    peces.append(d)
peces.sort(key=lambda d: d['order'])
assert len(peces) == 100
PEZ = {d['id']: d for d in peces}

EN_PECES = {
 'bacalao': 'Cod', 'salmon': 'Salmon', 'pez_tropical': 'Tropical Fish', 'pez_globo': 'Pufferfish', 'sardina': 'Sardine',
 'caballa': 'Mackerel', 'arenque': 'Herring', 'lubina': 'Sea Bass', 'dorada': 'Gilt-head Bream', 'pez_volador': 'Flying Fish',
 'barracuda': 'Barracuda', 'atun_rojo': 'Bluefin Tuna', 'pez_espada': 'Swordfish', 'raya': 'Ray', 'raya_electrica': 'Electric Ray',
 'pez_relampago': 'Lightning Fish', 'pez_linterna': 'Lanternfish', 'rape': 'Anglerfish', 'calamar_vampiro': 'Vampire Squid',
 'pez_vibora': 'Viperfish', 'pez_hacha_abisal': 'Abyssal Hatchetfish', 'celacanto': 'Coelacanth',
 'cria_de_calamar_gigante': 'Giant Squid Hatchling', 'pez_loro_arcoiris': 'Rainbow Parrotfish', 'caballito_de_mar': 'Seahorse',
 'pez_payaso': 'Clownfish', 'pez_cirujano_azul': 'Blue Tang', 'pez_angel': 'Angelfish', 'pez_mariposa': 'Butterflyfish',
 'pez_leon': 'Lionfish', 'morena': 'Moray Eel', 'pez_ballesta': 'Triggerfish', 'mero_gigante': 'Giant Grouper',
 'pez_napoleon': 'Napoleon Wrasse', 'pez_piedra': 'Stonefish', 'manta_raya': 'Manta Ray', 'pez_trompeta': 'Trumpetfish',
 'pez_sapo_luminoso': 'Glowing Frogfish', 'cria_de_tiburon_martillo': 'Hammerhead Shark Pup', 'bacalao_artico': 'Arctic Cod',
 'pez_de_hielo_transparente': 'Clear Icefish', 'trucha_artica': 'Arctic Char', 'capelan': 'Capelin', 'fletan': 'Halibut',
 'platija': 'Flounder', 'lumpo': 'Lumpfish', 'pez_lobo': 'Wolffish', 'cria_de_tiburon_de_groenlandia': 'Greenland Shark Pup',
 'pez_de_escarcha': 'Frostfish', 'pez_cristal_de_aurora': 'Aurora Crystalfish', 'merluza_negra': 'Toothfish',
 'calamar_de_hielo': 'Ice Squid', 'pez_glaciar_ancestral': 'Ancient Glacier Fish', 'carpa': 'Carp', 'perca': 'Perch',
 'lucio': 'Pike', 'trucha_arcoiris': 'Rainbow Trout', 'esturion': 'Sturgeon', 'anguila_de_rio': 'River Eel', 'siluro': 'Wels Catfish',
 'barbo': 'Barbel', 'gobio': 'Gudgeon', 'pez_gato_de_cristal': 'Glass Catfish', 'salmon_real': 'King Salmon', 'pirana': 'Piranha',
 'pirana_dorada': 'Golden Piranha', 'tenca': 'Tench', 'pez_fango': 'Mudfish', 'anguila_electrica': 'Electric Eel',
 'pez_pulmonado': 'Lungfish', 'bagre_del_pantano': 'Swamp Catfish', 'pez_arquero': 'Archerfish', 'gambusia': 'Mosquitofish',
 'pez_de_turbera': 'Bogfish', 'salamandra_acuatica': 'Water Salamander', 'ajolote_gigante': 'Giant Axolotl',
 'pez_momificado': 'Mummified Fish', 'pez_de_obsidiana': 'Obsidian Fish', 'pez_de_brasa': 'Emberfish', 'pez_de_magma': 'Magma Fish',
 'anguila_de_basalto': 'Basalt Eel', 'pez_fantasma_de_almas': 'Soul Ghostfish', 'pez_carmesi': 'Crimson Fish',
 'pez_distorsionado': 'Warped Fish', 'pez_de_cuarzo': 'Quartz Fish', 'lucio_de_lava': 'Lava Pike', 'pez_de_verruga': 'Wart Fish',
 'raya_ignea': 'Igneous Ray', 'pez_de_bastion_dorado': 'Gilded Bastion Fish', 'pez_escoria': 'Slagfish',
 'cria_de_dragon_de_magma': 'Magma Dragon Hatchling', 'pez_perla': 'Pearlfish', 'medusa_del_vacio': 'Void Jellyfish',
 'anguila_estelar': 'Star Eel', 'pez_de_coral': 'Chorus Fish', 'pez_shulker': 'Shulker Fish', 'pez_de_purpura': 'Purpur Fish',
 'pez_espectral': 'Spectral Fish', 'mantarraya_del_end': 'End Manta Ray', 'pez_eterno': 'Eternal Fish',
}
assert set(EN_PECES) == set(PEZ), set(PEZ) ^ set(EN_PECES)

VENENOSOS = {'pez_leon': 0, 'pez_piedra': 1, 'raya': 0}
FUEL = {'pez_de_brasa': 4800, 'cria_de_dragon_de_magma': 20000}
SALMON_LIKE = {'trucha_artica', 'salmon_real', 'trucha_arcoiris'}


def tipo_pez(d):
    """Cómo se cocina: 'calamar', 'asar' (pequeño), 'brasa' (grande) o None (no se cocina)."""
    if d['id'] in VANILLA_FISH or d['rarity'] == 4 or d['family'] in ('igneo', 'vacio') or d['id'] == 'pez_momificado':
        return None
    if d.get('shape') == 'calamar' and d['id'] != 'ajolote_gigante':
        return 'calamar'
    return 'asar' if d['rarity'] <= 1 else 'brasa'


def efectos_pez(d):
    k = 4 if d['rarity'] == 4 else 1
    out = []
    fam = d['family']
    if d['id'] == 'pez_momificado':
        return [['minecraft:hunger', 30, 0, 1.0]]
    if fam == 'abisal': out.append(['minecraft:night_vision', 30 * k, 0, 1.0])
    if fam == 'tormenta': out.append(['minecraft:speed', 20 * k, 0, 1.0])
    if fam == 'igneo': out.append(['minecraft:fire_resistance', 30 * k, 0, 1.0])
    if fam == 'vacio': out.append(['minecraft:slow_falling', 15 * k, 0, 1.0])
    if d.get('ghost'): out.append(['minecraft:invisibility', 15, 0, 1.0])
    if d['id'] in VENENOSOS: out.append(['minecraft:poison', 10, VENENOSOS[d['id']], 1.0])
    if d['rarity'] == 4: out.append(['minecraft:luck', 120, 0, 1.0])
    return out


# ---------------------------------------------------------------- tesoros (orden del prototipo)
OC = ['oceano', 'calido', 'helado']
# (nombre, id, objeto vanilla o None=propio o 'cofre', silueta, rareza, prof., biomas, clima, color, nombre en inglés)
TESOROS_RAW = [
 ('Botella con mensaje', 'botella_con_mensaje', None, 'botella', 1, 3, None, None, '#7fd1b9', 'Message in a Bottle'),
 ('Pepita de río', 'pepita_de_rio', None, 'pepita', 1, 4, ['rio'], None, '#ffd24a', 'River Nugget'),
 ('Cofre hundido', 'cofre_hundido', 'cofre', 'cofre', 2, 15, None, None, '#a0672e', 'Sunken Chest'),
 ('Concha de nautilo', 'concha_de_nautilo', 'minecraft:nautilus_shell', 'concha', 2, 20, OC, None, '#f0d8b0', 'Nautilus Shell'),
 ('Corona del pantano', 'corona_del_pantano', None, 'corona', 3, 25, ['pantano'], ['lluvia', 'tormenta'], '#d8c040', 'Swamp Crown'),
 ('Perla negra', 'perla_negra', None, 'perla', 3, 30, ['calido'], None, '#3a3050', 'Black Pearl'),
 ('Cristal glacial', 'cristal_glacial', None, 'cristal', 3, 30, ['helado'], None, '#a8e8ff', 'Glacial Crystal'),
 ('Tridente oxidado', 'tridente_oxidado', None, 'tridente', 3, 35, OC, ['tormenta'], '#5fa090', 'Rusty Trident'),
 ('Reliquia del cieno', 'reliquia_del_cieno', None, 'gema', 4, 45, ['pantano', 'rio'], None, '#90ff60', 'Silt Relic'),
 ('Corazón del mar', 'corazon_del_mar', 'minecraft:heart_of_the_sea', 'gema', 4, 50, OC, None, '#3ad0ff', 'Heart of the Sea'),
 ('Moneda de oro', 'moneda_de_oro', None, 'moneda', 2, 20, None, None, '#ffd23a', 'Gold Coin'),
 ('Coral rojo', 'coral_rojo', None, 'coral', 2, 25, ['calido'], None, '#ff4a5a', 'Red Coral'),
 ('Ancla antigua', 'ancla_antigua', None, 'ancla', 2, 28, ['oceano', 'helado'], None, '#8a949c', 'Old Anchor'),
 ('Geoda', 'geoda', None, 'geoda', 3, 50, None, None, '#c08aff', 'Geode'),
 ('Cáliz hundido', 'caliz_hundido', None, 'caliz', 3, 55, None, None, '#e8c040', 'Sunken Chalice'),
 ('Ánfora sellada', 'anfora_sellada', None, 'anfora', 3, 70, None, None, '#b8683a', 'Sealed Amphora'),
 ('Reliquia del pantano negro', 'reliquia_del_pantano_negro', None, 'gema', 4, 85, ['pantano'], None, '#3aff9a', 'Black Swamp Relic'),
 ('Gema abisal', 'gema_abisal', None, 'gema', 4, 85, None, None, '#5a7aff', 'Abyssal Gem'),
 ('Lágrima del abismo', 'lagrima_del_abismo', None, 'lagrima', 4, 100, None, None, '#e0f0ff', 'Tear of the Abyss'),
 ('Corona del rey ahogado', 'corona_del_rey_ahogado', None, 'corona', 4, 105, None, None, '#ffd23a', "Drowned King's Crown"),
 ('Huevo de leviatán', 'huevo_de_leviatan', None, 'huevo', 4, 110, OC, None, '#7ad0c0', 'Leviathan Egg'),
 ('Cuarzo del Nether', 'cuarzo_del_nether', 'minecraft:quartz', 'cristal', 1, 0, ['lava'], None, '#f0e6e0', 'Nether Quartz'),
 ('Pepitas de oro', 'pepitas_de_oro', 'minecraft:gold_nugget', 'pepita', 1, 5, ['lava'], None, '#ffd24a', 'Gold Nuggets'),
 ('Escama ígnea', 'escama_ignea', None, 'escama', 2, 25, ['lava'], None, '#ff7a2a', 'Igneous Scale'),
 ('Tesoro de bastión', 'tesoro_de_bastion', 'cofre', 'cofre', 3, 25, ['lava'], None, '#3a3038', 'Bastion Treasure'),
 ('Plantilla de netherita', 'plantilla_de_netherita', 'minecraft:netherite_upgrade_smithing_template', 'anfora', 4, 60, ['lava'], None, '#4a4450', 'Netherite Upgrade'),
 ('Restos ancestrales', 'restos_ancestrales', 'minecraft:ancient_debris', 'huevo', 4, 95, ['lava'], None, '#6a4a40', 'Ancient Debris'),
 ('Fragmento del vacío', 'fragmento_del_vacio', M + 'fragmento_vacio', 'cristal', 2, 30, ['vacio'], None, '#b070ff', 'Void Shard'),
 ('Concha de shulker', 'concha_de_shulker', 'minecraft:shulker_shell', 'concha', 3, 100, ['vacio'], None, '#9a6a9a', 'Shulker Shell'),
 ('Aliento de dragón', 'aliento_de_dragon', 'minecraft:dragon_breath', 'botella', 3, 200, ['vacio'], None, '#e070d0', "Dragon's Breath"),
 ('Cabeza de dragón', 'cabeza_de_dragon', 'minecraft:dragon_head', 'corona', 4, 500, ['vacio'], None, '#3a3a44', 'Dragon Head'),
 ('Élitros', 'elitros', 'minecraft:elytra', 'gema', 4, 800, ['vacio'], None, '#8a7ab8', 'Elytra'),
 ('Trofeo «Leviatán del vacío»', 'trofeo_leviatan_del_vacio', None, 'trofeo', 4, 1000, ['vacio'], None, '#d0b0ff', 'Void Leviathan Trophy'),
]
# Comportamiento de los tesoros propios: tipo, efectos (id, nivel), tabla de botín, lo que queda, experiencia, montón
COMPORTA = {
 'botella_con_mensaje': dict(tipo='abrir', resto='minecraft:glass_bottle', xp=5),
 'geoda': dict(tipo='abrir', xp=0),
 'anfora_sellada': dict(tipo='abrir', xp=10),
 'caliz_hundido': dict(tipo='caliz', stack=1),
 'corona_del_pantano': dict(tipo='cabeza', stack=1, effects=[['minecraft:luck', 0]]),
 'corona_del_rey_ahogado': dict(tipo='cabeza', stack=1, effects=[['minecraft:conduit_power', 0]]),
 'trofeo_leviatan_del_vacio': dict(tipo='mano', stack=1, effects=[['minecraft:slow_falling', 0], ['minecraft:luck', 1]]),
 'tridente_oxidado': dict(tipo='material', stack=1),
}

# ---------------------------------------------------------------- comidas
# id: (es, en, hambre, saturación, efectos, recipiente, silueta, color)
COMIDAS = {
 'pescado_asado': ('Pescado asado', 'Grilled Fish', 5, 0.6, [], None, 'asado', '#c98d4e'),
 'filete_a_la_brasa': ('Filete a la brasa', 'Chargrilled Fillet', 8, 0.8, [], None, 'filete', '#d99a6a'),
 'anillas_de_calamar': ('Anillas de calamar', 'Calamari Rings', 6, 0.6, [], None, 'anillas', '#e6c27a'),
 'sopa_abisal': ('Sopa abisal', 'Abyssal Soup', 8, 0.7, [['minecraft:night_vision', 180, 0, 1.0], ['minecraft:water_breathing', 180, 0, 1.0]], 'minecraft:bowl', 'cuenco', '#3a4fb0'),
 'caldo_de_magma': ('Caldo de magma', 'Magma Broth', 8, 0.7, [['minecraft:fire_resistance', 300, 0, 1.0]], 'minecraft:bowl', 'cuenco', '#f0641e'),
 'estofado_artico': ('Estofado ártico', 'Arctic Stew', 9, 0.8, [['minecraft:absorption', 120, 1, 1.0], ['minecraft:resistance', 120, 0, 1.0]], 'minecraft:bowl', 'cuenco', '#bfe3f0'),
 'ceviche_tropical': ('Ceviche tropical', 'Tropical Ceviche', 7, 0.7, [['minecraft:speed', 180, 0, 1.0], [M + 'aletas', 180, 0, 1.0]], 'minecraft:bowl', 'cuenco', '#f08ac0'),
 'guiso_del_pantano': ('Guiso del pantano', 'Swamp Stew', 8, 0.7, [['minecraft:regeneration', 20, 0, 1.0], ['minecraft:luck', 300, 0, 1.0]], 'minecraft:bowl', 'cuenco', '#7a9a3e'),
 'anguila_de_tormenta': ('Anguila a la tormenta', 'Storm Eel Rice', 8, 0.8, [['minecraft:haste', 180, 1, 1.0], ['minecraft:speed', 180, 0, 1.0]], 'minecraft:bowl', 'cuenco', '#f2e25a'),
 'brocheta_del_vacio': ('Brocheta del vacío', 'Void Skewer', 6, 0.6, [['minecraft:slow_falling', 180, 0, 1.0], ['minecraft:night_vision', 180, 0, 1.0]], 'minecraft:stick', 'brocheta', '#b87ae0'),
 'festin_del_leviatan': ('Festín del leviatán', "Leviathan's Feast", 14, 1.0, [['minecraft:absorption', 120, 3, 1.0], ['minecraft:regeneration', 30, 1, 1.0], ['minecraft:resistance', 300, 0, 1.0], [M + 'coraza', 300, 0, 1.0]], 'minecraft:bowl', 'cuenco', '#7ad0c0'),
}
# Platos: ingredientes con Farmer's Delight (olla) y sin él (mesa de trabajo, recipiente incluido)
PLATOS = {
 'sopa_abisal': ([{'tag': M + 'peces/familia/abisal'}, {'tag': 'forge:crops/onion'}, {'item': 'minecraft:dried_kelp'}, {'item': 'minecraft:glow_ink_sac'}],
                 [{'tag': M + 'peces/familia/abisal'}, {'item': 'minecraft:dried_kelp'}, {'item': 'minecraft:glow_ink_sac'}]),
 'caldo_de_magma': ([{'tag': M + 'peces/familia/igneo'}, {'item': FD + ':tomato_sauce'}, {'item': 'minecraft:nether_wart'}],
                    [{'tag': M + 'peces/familia/igneo'}, {'item': 'minecraft:magma_cream'}, {'item': 'minecraft:nether_wart'}]),
 'estofado_artico': ([{'tag': M + 'peces/familia/helado'}, {'tag': 'forge:crops/potato'}, {'tag': 'forge:milk'}, {'tag': 'forge:crops/cabbage'}],
                     [{'tag': M + 'peces/familia/helado'}, {'item': 'minecraft:baked_potato'}, {'item': 'minecraft:snowball'}]),
 'ceviche_tropical': ([{'tag': M + 'peces/calido'}, {'tag': 'forge:crops/tomato'}, {'tag': 'forge:crops/onion'}, {'item': 'minecraft:sea_pickle'}],
                      [{'tag': M + 'peces/calido'}, {'item': 'minecraft:melon_slice'}, {'item': 'minecraft:sea_pickle'}]),
 'guiso_del_pantano': ([{'tag': M + 'peces/pantano'}, {'item': 'minecraft:brown_mushroom'}, {'item': 'minecraft:red_mushroom'}, {'tag': 'forge:crops/cabbage'}],
                       [{'tag': M + 'peces/pantano'}, {'item': 'minecraft:brown_mushroom'}, {'item': 'minecraft:red_mushroom'}, {'item': 'minecraft:lily_pad'}]),
 'anguila_de_tormenta': ([{'tag': M + 'peces/familia/tormenta'}, {'tag': 'forge:crops/rice'}, {'item': 'minecraft:honey_bottle'}],
                         [{'tag': M + 'peces/familia/tormenta'}, {'item': 'minecraft:bread'}, {'item': 'minecraft:honey_bottle'}]),
 'brocheta_del_vacio': ([{'tag': M + 'peces/familia/vacio'}, {'item': 'minecraft:chorus_fruit'}, {'item': 'minecraft:chorus_fruit'}],
                        [{'tag': M + 'peces/familia/vacio'}, {'item': 'minecraft:chorus_fruit'}, {'item': 'minecraft:chorus_fruit'}]),
 'festin_del_leviatan': ([{'item': M + 'huevo_de_leviatan'}, {'tag': M + 'peces/legendarios'}, {'item': 'minecraft:golden_carrot'}, {'tag': 'forge:milk'}],
                         [{'item': M + 'huevo_de_leviatan'}, {'tag': M + 'peces/legendarios'}, {'item': 'minecraft:golden_carrot'}, {'item': 'minecraft:milk_bucket'}]),
}

# ---------------------------------------------------------------- pociones
POCIONES = {  # id: (es, en)
 'prisa': ('prisa', 'Haste'), 'gracia_delfin': ('gracia del delfín', "Dolphin's Grace"), 'conducto': ('poder del conducto', 'Conduit Power'),
 'resistencia': ('resistencia', 'Resistance'), 'absorcion': ('absorción', 'Absorption'), 'brillo': ('brillo', 'Glowing'),
 'levitacion': ('levitación', 'Levitation'), 'descomposicion': ('descomposición', 'Decay'), 'aletas': ('aletas', 'Fins'),
 'coraza': ('coraza', 'Carapace'),
}
VANILLA_POCION = {  # para los textos
 'water_breathing': ('respiración acuática', 'Water Breathing'), 'night_vision': ('visión nocturna', 'Night Vision'),
 'fire_resistance': ('resistencia al fuego', 'Fire Resistance'), 'invisibility': ('invisibilidad', 'Invisibility'),
 'regeneration': ('regeneración', 'Regeneration'), 'healing': ('curación', 'Healing'), 'swiftness': ('rapidez', 'Swiftness'),
 'leaping': ('salto', 'Leaping'), 'strength': ('fuerza', 'Strength'), 'slow_falling': ('caída lenta', 'Slow Falling'),
 'poison': ('veneno', 'Poison'), 'slowness': ('lentitud', 'Slowness'), 'harming': ('daño', 'Harming'), 'luck': ('suerte', 'Luck'),
}
# poción resultante: ingredientes (sobre una poción rara)
MEZCLAS = {
 M + 'prisa': ['anguila_electrica', 'pez_relampago', 'raya_electrica'],
 M + 'gracia_delfin': ['pez_volador', 'salmon_real'],
 M + 'conducto': ['gema_abisal', 'celacanto'],
 M + 'resistencia': ['pez_de_obsidiana', 'pez_escoria'],
 M + 'absorcion': ['lagrima_del_abismo', 'pez_glaciar_ancestral'],
 M + 'brillo': ['pez_linterna', 'pez_sapo_luminoso'],
 M + 'levitacion': ['pez_shulker'],
 M + 'descomposicion': ['pez_momificado', 'pez_fantasma_de_almas'],
 M + 'aletas': ['manta_raya', 'pez_espada', 'atun_rojo'],
 M + 'coraza': ['esturion', 'pez_de_cuarzo'],
 'minecraft:luck': ['reliquia_del_cieno', 'pirana_dorada', 'pez_de_bastion_dorado'],
 'minecraft:water_breathing': ['pez_pulmonado', 'caballito_de_mar'],
 'minecraft:night_vision': ['pez_vibora', 'rape'],
 'minecraft:fire_resistance': ['pez_de_magma', 'escama_ignea', 'pez_de_brasa'],
 'minecraft:invisibility': ['pez_de_hielo_transparente', 'pez_gato_de_cristal', 'pez_espectral'],
 'minecraft:regeneration': ['ajolote_gigante', 'salamandra_acuatica'],
 'minecraft:healing': ['coral_rojo', 'pez_angel'],
 'minecraft:swiftness': ['barracuda', 'trucha_arcoiris'],
 'minecraft:leaping': ['pez_arquero'],
 'minecraft:strength': ['cria_de_dragon_de_magma', 'lucio_de_lava', 'mero_gigante'],
 'minecraft:slow_falling': ['medusa_del_vacio'],
 'minecraft:poison': ['pez_leon', 'pez_piedra', 'raya'],
 'minecraft:slowness': ['pez_de_escarcha', 'calamar_de_hielo'],
 'minecraft:harming': ['pirana'],
}

# ---------------------------------------------------------------- despieces y otras recetas
NOMBRES = {  # objetos vanilla que salen en los textos
 'ink_sac': ('saco de tinta', 'Ink Sac'), 'glow_ink_sac': ('saco de tinta luminosa', 'Glow Ink Sac'), 'ender_pearl': ('perla de ender', 'Ender Pearl'),
 'chorus_fruit': ('fruta de coro', 'Chorus Fruit'), 'purple_dye': ('tinte morado', 'Purple Dye'), 'quartz': ('cuarzo', 'Nether Quartz'),
 'obsidian': ('obsidiana', 'Obsidian'), 'magma_cream': ('crema de magma', 'Magma Cream'), 'nether_wart': ('verruga del Nether', 'Nether Wart'),
 'crimson_fungus': ('hongo carmesí', 'Crimson Fungus'), 'warped_fungus': ('hongo distorsionado', 'Warped Fungus'),
 'prismarine_crystals': ('cristales de prismarina', 'Prismarine Crystals'), 'glowstone_dust': ('polvo de piedra luminosa', 'Glowstone Dust'),
 'slime_ball': ('bola de slime', 'Slimeball'), 'blaze_powder': ('polvo de blaze', 'Blaze Powder'), 'snowball': ('bola de nieve', 'Snowball'),
 'gold_ingot': ('lingote de oro', 'Gold Ingot'), 'netherite_scrap': ('chatarra de netherita', 'Netherite Scrap'),
 'gold_nugget': ('pepita de oro', 'Gold Nugget'), 'red_dye': ('tinte rojo', 'Red Dye'), 'iron_ingot': ('lingote de hierro', 'Iron Ingot'),
 'blue_ice': ('hielo azul', 'Blue Ice'), 'trident': ('tridente', 'Trident'), 'totem_of_undying': ('tótem de la inmortalidad', 'Totem of Undying'),
}
DESPIECE = {  # objeto: (resultado, cantidad)
 'calamar_de_hielo': ('ink_sac', 2), 'calamar_vampiro': ('glow_ink_sac', 2), 'pez_perla': ('ender_pearl', 1), 'pez_de_coral': ('chorus_fruit', 2),
 'pez_de_purpura': ('purple_dye', 2), 'pez_de_cuarzo': ('quartz', 3), 'pez_de_obsidiana': ('obsidian', 2), 'pez_de_magma': ('magma_cream', 1),
 'pez_de_verruga': ('nether_wart', 2), 'pez_carmesi': ('crimson_fungus', 1), 'pez_distorsionado': ('warped_fungus', 1),
 'pez_cristal_de_aurora': ('prismarine_crystals', 3), 'anguila_estelar': ('glowstone_dust', 3), 'medusa_del_vacio': ('slime_ball', 2),
 'raya_ignea': ('blaze_powder', 2), 'anguila_de_basalto': ('blaze_powder', 1), 'pez_linterna': ('glowstone_dust', 2),
 'pez_de_escarcha': ('snowball', 4),
 'pepita_de_rio': ('gold_nugget', 3), 'coral_rojo': ('red_dye', 2), 'ancla_antigua': ('iron_ingot', 5), 'escama_ignea': ('blaze_powder', 2),
 'perla_negra': ('ender_pearl', 4),
}
FUNDIR = {'pez_de_bastion_dorado': 'gold_ingot', 'pez_escoria': 'netherite_scrap'}

# Textos de uso propios (además de los que se deducen de las tablas de arriba)
USO_EXTRA = {
 'botella_con_mensaje': ('Clic derecho: ábrela (papel, mapas, experiencia…)', 'Right-click to open (paper, maps, experience…)'),
 'geoda': ('Clic derecho: pártela (amatista, lapislázuli, diamantes…)', 'Right-click to crack it open (amethyst, lapis, diamonds…)'),
 'anfora_sellada': ('Clic derecho: rompe el sello (miel, oro, libros encantados…)', 'Right-click to break the seal (honey, gold, enchanted books…)'),
 'caliz_hundido': ('Clic derecho: bebe y quita los efectos dañinos. No se gasta (5 min de espera)', 'Right-click to drink: clears harmful effects. Not consumed (5 min cooldown)'),
 'corona_del_pantano': ('En la cabeza: Suerte', 'Worn on the head: Luck'),
 'corona_del_rey_ahogado': ('En la cabeza: Poder del conducto', 'Worn on the head: Conduit Power'),
 'trofeo_leviatan_del_vacio': ('En la mano secundaria: Caída lenta y Suerte II', 'In the off hand: Slow Falling and Luck II'),
 'moneda_de_oro': ('4 monedas = 1 lingote de oro. El pescador las cambia por esmeraldas', '4 coins = 1 gold ingot. Fishermen trade emeralds for them'),
 'cristal_glacial': ('Rodeado de 8 de hielo: 8 de hielo azul', 'Surrounded by 8 ice: 8 blue ice'),
 'tridente_oxidado': ('Con 4 fragmentos y 2 cristales de prismarina: un tridente', 'With 4 prismarine shards and 2 crystals: a trident'),
 'reliquia_del_pantano_negro': ('Rodeada de 8 lingotes de oro: tótem de la inmortalidad', 'Surrounded by 8 gold ingots: Totem of Undying'),
 'huevo_de_leviatan': ('Ingrediente del Festín del leviatán', "Ingredient of the Leviathan's Feast"),
}

tesoros_propios = [t for t in TESOROS_RAW if t[2] is None]
PROPIOS = {t[1] for t in tesoros_propios}
nombre_es = {d['id']: d['name'] for d in peces}
nombre_en = dict(EN_PECES)
for t in TESOROS_RAW:
    nombre_es[t[1]], nombre_en[t[1]] = t[0], t[9]
for k, v in COMIDAS.items():
    nombre_es[k], nombre_en[k] = v[0], v[1]


def item_de(i):
    """Id de objeto de una captura (los 4 peces vanilla son los de Minecraft)."""
    if i in VANILLA_FISH: return VANILLA_FISH[i]
    assert i in PEZ or i in PROPIOS, i
    return M + i


# ---------------------------------------------------------------- textos de uso
def usos(i):
    es, en = [], []
    d = PEZ.get(i)
    if d:
        t = tipo_pez(d)
        if t:
            c = {'asar': 'pescado_asado', 'brasa': 'filete_a_la_brasa', 'calamar': 'anillas_de_calamar'}[t]
            es.append('Al horno, ahumador u hoguera: ' + COMIDAS[c][0]); en.append('Furnace, smoker or campfire: ' + COMIDAS[c][1])
            if t != 'calamar':
                n, s = corte(d)
                es.append("Tabla de cortar (Farmer's Delight): %d filetes de %s" % (n, 'salmón' if s == 'salmon' else 'bacalao'))
                en.append("Cutting board (Farmer's Delight): %d %s slices" % (n, s))
        for plato, (con, sin) in PLATOS.items():
            tag = con[0].get('tag', '') if plato != 'festin_del_leviatan' else con[1]['tag']
            if tag and i in TAGS.get(tag[len(M):], ()):
                es.append('Plato: ' + COMIDAS[plato][0]); en.append('Dish: ' + COMIDAS[plato][1])
        if d['family'] == 'igneo':
            es.append('Combustible de horno'); en.append('Furnace fuel')
    for pocion, ings in MEZCLAS.items():
        if i in ings:
            pid = pocion.split(':')[1]
            pes, pen = POCIONES[pid] if pocion.startswith(M) else VANILLA_POCION[pid]
            es.append('Poción de %s (sobre una poción rara)' % pes); en.append('Potion of %s (brewed into an awkward potion)' % pen)
    if i in DESPIECE:
        r, n = DESPIECE[i]
        es.append('En la mesa de trabajo: %d × %s' % (n, NOMBRES[r][0])); en.append('Crafting: %d × %s' % (n, NOMBRES[r][1]))
    if i in FUNDIR:
        es.append('Fundido: ' + NOMBRES[FUNDIR[i]][0]); en.append('Smelted: ' + NOMBRES[FUNDIR[i]][1])
    if i in USO_EXTRA:
        es.append(USO_EXTRA[i][0]); en.append(USO_EXTRA[i][1])
    return es, en


def corte(d):
    n = [2, 2, 3, 4][d['rarity']]
    return n, 'salmon' if d['family'] == 'fluvial' or d['id'] in SALMON_LIKE else 'cod'


# ---------------------------------------------------------------- etiquetas
TAGS = {}


def tag(name, value):
    TAGS.setdefault(name, []).append(value)


for d in peces:
    i = d['id']
    tag('peces', i)
    for b in d['biomes']: tag('peces/' + b, i)
    tag('peces/familia/' + d['family'], i)
    tag('peces/rareza/%d' % d['rarity'], i)
    if d['rarity'] == 4: tag('peces/legendarios', i)
    t = tipo_pez(d)
    if t:
        tag('peces/cocina/' + t, i)
        if t != 'calamar':
            n, s = corte(d)
            tag('peces/corte/%s_%d' % (s, n), i)
            tag('peces/crudos', i)
            if d['rarity'] <= 2 and d['family'] in ('comun', 'helado'): tag('peces/como_bacalao', i)
            if d['rarity'] <= 2 and s == 'salmon': tag('peces/como_salmon', i)
for t in tesoros_propios: tag('tesoros', t[1])


def gen_tags():
    shutil.rmtree(os.path.join(DATA, 'tags', 'items', 'peces'), ignore_errors=True)
    for name, ids in TAGS.items():
        write(os.path.join(DATA, 'tags', 'items', name + '.json'), {'replace': False, 'values': [item_de(i) for i in ids]})
    ext = {
        ('minecraft', 'fishes'): ['#' + M + 'peces/crudos'],
        ('forge', 'raw_fishes'): ['#' + M + 'peces/crudos'],
        ('forge', 'raw_fishes/cod'): ['#' + M + 'peces/como_bacalao'],
        ('forge', 'raw_fishes/salmon'): ['#' + M + 'peces/como_salmon'],
        ('forge', 'cooked_fishes'): [M + 'pescado_asado', M + 'filete_a_la_brasa'],
    }
    for (ns, name), values in ext.items():
        write(os.path.join(RES, 'data', ns, 'tags', 'items', name + '.json'), {'replace': False, 'values': values})


# ---------------------------------------------------------------- recetas
CON_FD = [{'type': 'forge:mod_loaded', 'modid': FD}]
SIN_FD = [{'type': 'forge:not', 'value': {'type': 'forge:mod_loaded', 'modid': FD}}]
RECETAS = os.path.join(DATA, 'recipes', 'capturas')


def gen_recetas():
    shutil.rmtree(RECETAS, ignore_errors=True)
    for t, c in (('asar', 'pescado_asado'), ('brasa', 'filete_a_la_brasa'), ('calamar', 'anillas_de_calamar')):
        for tipo, tiempo in (('smelting', 200), ('smoking', 100), ('campfire_cooking', 600)):
            write(os.path.join(RECETAS, 'cocina', '%s_%s.json' % (c, tipo)), {
                'type': 'minecraft:' + tipo, 'category': 'food', 'ingredient': {'tag': M + 'peces/cocina/' + t},
                'result': M + c, 'experience': 0.35, 'cookingtime': tiempo})
    for name in TAGS:
        if not name.startswith('peces/corte/'): continue
        s, n = name.split('/')[-1].rsplit('_', 1)
        write(os.path.join(RECETAS, 'corte', '%s_%s.json' % (s, n)), {
            'type': FD + ':cutting', 'conditions': CON_FD, 'ingredients': [{'tag': M + name}],
            'result': [{'item': '%s:%s_slice' % (FD, s), 'count': int(n)}, {'item': 'minecraft:bone_meal'}],
            'tool': [{'type': FD + ':tool_action', 'action': 'knife_dig'}, {'tag': 'forge:tools/knives'}]})
    for i, (r, n) in DESPIECE.items():
        write(os.path.join(RECETAS, 'despiece', i + '.json'), {
            'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [{'item': item_de(i)}],
            'result': {'item': mc(r), 'count': n}})
    for i, r in FUNDIR.items():
        for tipo, tiempo in (('smelting', 200), ('blasting', 100)):
            write(os.path.join(RECETAS, 'fundir', '%s_%s.json' % (i, tipo)), {
                'type': 'minecraft:' + tipo, 'category': 'misc', 'ingredient': {'item': item_de(i)},
                'result': mc(r), 'experience': 1.0, 'cookingtime': tiempo})
    write(os.path.join(RECETAS, 'lingote_de_monedas.json'), {
        'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [{'item': M + 'moneda_de_oro'}] * 4,
        'result': {'item': 'minecraft:gold_ingot'}})
    write(os.path.join(RECETAS, 'hielo_azul.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'building', 'pattern': ['III', 'ICI', 'III'],
        'key': {'I': {'item': 'minecraft:ice'}, 'C': {'item': M + 'cristal_glacial'}}, 'result': {'item': 'minecraft:blue_ice', 'count': 8}})
    write(os.path.join(RECETAS, 'tridente.json'), {
        'type': 'minecraft:crafting_shapeless', 'category': 'equipment',
        'ingredients': [{'item': M + 'tridente_oxidado'}] + [{'item': 'minecraft:prismarine_shard'}] * 4 + [{'item': 'minecraft:prismarine_crystals'}] * 2,
        'result': {'item': 'minecraft:trident'}})
    write(os.path.join(RECETAS, 'totem.json'), {
        'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['OOO', 'ORO', 'OOO'],
        'key': {'O': {'item': 'minecraft:gold_ingot'}, 'R': {'item': M + 'reliquia_del_pantano_negro'}}, 'result': {'item': 'minecraft:totem_of_undying'}})
    for plato, (con, sin) in PLATOS.items():
        recipiente = COMIDAS[plato][5]
        write(os.path.join(RECETAS, 'platos', plato + '_olla.json'), {
            'type': FD + ':cooking', 'conditions': CON_FD, 'cookingtime': 200, 'experience': 1.0, 'ingredients': con,
            'container': {'item': recipiente}, 'recipe_book_tab': 'meals', 'result': {'item': M + plato}})
        write(os.path.join(RECETAS, 'platos', plato + '.json'), {
            'type': 'minecraft:crafting_shapeless', 'conditions': SIN_FD, 'category': 'misc',
            'ingredients': sin + [{'item': recipiente}], 'result': {'item': M + plato}})


# ---------------------------------------------------------------- botín de los tesoros que se abren
def loot(rolls, entries):
    out = []
    for e in entries:
        name, w, a, b = e[:4]
        o = {'type': 'minecraft:item', 'name': mc(name), 'weight': w}
        fs = []
        if b > 1: fs.append({'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': a, 'max': b}})
        if len(e) > 4: fs.append({'function': 'minecraft:enchant_randomly'})
        if fs: o['functions'] = fs
        out.append(o)
    return {'type': 'minecraft:chest', 'pools': [{'rolls': rolls, 'entries': out}]}


def gen_loot():
    d = os.path.join(DATA, 'loot_tables', 'tesoros')
    write(os.path.join(d, 'botella_con_mensaje.json'), loot(1, [
        ('paper', 6, 1, 3), ('map', 3, 1, 1), ('experience_bottle', 4, 1, 2), ('emerald', 3, 1, 1), ('name_tag', 1, 1, 1),
        ('book', 1, 1, 1, True), (M + 'lombriz', 3, 2, 4)]))
    write(os.path.join(d, 'geoda.json'), loot(2, [
        ('amethyst_shard', 8, 2, 6), ('quartz', 5, 2, 5), ('lapis_lazuli', 5, 3, 6), ('emerald', 3, 1, 2), ('diamond', 2, 1, 1),
        ('amethyst_block', 2, 1, 1)]))
    write(os.path.join(d, 'anfora_sellada.json'), loot(3, [
        ('honey_bottle', 6, 1, 3), ('gold_nugget', 5, 5, 12), ('emerald', 4, 2, 4), ('experience_bottle', 5, 3, 6),
        ('golden_apple', 2, 1, 1), ('book', 3, 1, 1, True), ('diamond', 2, 1, 2)]))
    write(os.path.join(DATA, 'loot_modifiers', 'captura_pesca.json'), {
        'type': M + 'captura_pesca',
        'conditions': [{'condition': 'forge:loot_table_id', 'loot_table_id': 'minecraft:gameplay/fishing'}]})
    p = os.path.join(RES, 'data', 'forge', 'loot_modifiers', 'global_loot_modifiers.json')
    g = json.load(open(p))
    if M + 'captura_pesca' not in g['entries']:
        # antes que el cofre: así el 10 % de cofres sigue sustituyendo a lo que haya salido
        g['entries'].insert(g['entries'].index(M + 'cofre_pesca'), M + 'captura_pesca')
        write(p, g)


# ---------------------------------------------------------------- catálogo
def gen_catalogo():
    lineas = {}
    cat = {'peces': [], 'tesoros': [], 'comidas': [], 'mezclas': []}
    for d in peces:
        # Los 4 peces de Minecraft no llevan descripción propia (su objeto no es del mod)
        es, en = usos(d['id']) if d['id'] not in VANILLA_FISH else ([], [])
        lineas[d['id']] = (es, en)
        fam = d['family']
        cat['peces'].append({
            'id': d['id'], 'item': item_de(d['id']), 'own': d['id'] not in VANILLA_FISH, 'rarity': d['rarity'], 'family': fam,
            'nutrition': [2, 2, 3, 3, 5][d['rarity']], 'saturation': [0.1, 0.1, 0.2, 0.3, 0.4][d['rarity']],
            'effects': efectos_pez(d), 'always': fam in ('igneo', 'vacio') or d['rarity'] == 4,
            'fuel': FUEL.get(d['id'], 1600 if fam == 'igneo' else 0), 'lines': len(es)})
    for n, t in enumerate(TESOROS_RAW):
        name, i, vanilla, sil, r, depth, biomes, weather, col, en_name = t
        es, en = usos(i) if vanilla is None else ([], [])
        lineas[i] = (es, en)
        c = COMPORTA.get(i, {})
        cat['tesoros'].append({
            'id': i, 'name': name, 'order': n, 'item': None if vanilla == 'cofre' else (vanilla or M + i), 'own': vanilla is None,
            'chest': vanilla == 'cofre', 'rarity': r, 'min_depth': depth, 'biomes': biomes or AGUA, 'weather': weather or [],
            'kind': c.get('tipo', 'material'), 'effects': c.get('effects', []), 'leftover': c.get('resto'), 'xp': c.get('xp', 0),
            'stack': c.get('stack', 64), 'lines': len(es)})
    for i, (es_n, en_n, hambre, sat, efectos, recipiente, sil, col) in COMIDAS.items():
        cat['comidas'].append({'id': i, 'nutrition': hambre, 'saturation': sat, 'effects': efectos,
                               'container': recipiente, 'lines': 0})
    for pocion, ings in MEZCLAS.items():
        cat['mezclas'].append({'to': pocion, 'with': [item_de(i) for i in ings]})
    write(os.path.join(RES, 'toodeepseas_capturas.json'), cat)
    return lineas


# ---------------------------------------------------------------- textos
RAREZA = (['Común', 'Poco común', 'Raro', 'Épico', 'Legendario'], ['Common', 'Uncommon', 'Rare', 'Epic', 'Legendary'])
FAMILIA = {'comun': ('Pez común', 'Common fish'), 'fluvial': ('Pez de agua dulce', 'Freshwater fish'), 'carnivoro': ('Depredador', 'Predator'),
           'abisal': ('Pez abisal', 'Abyssal fish'), 'helado': ('Pez de aguas heladas', 'Cold-water fish'), 'tormenta': ('Pez de tormenta', 'Storm fish'),
           'igneo': ('Pez de lava', 'Lava fish'), 'vacio': ('Pez del vacío', 'Void fish')}
BIOMA_TXT = {'oceano': ('océano', 'ocean'), 'calido': ('océano cálido', 'warm ocean'), 'helado': ('aguas heladas', 'frozen waters'),
             'rio': ('río', 'river'), 'pantano': ('pantano', 'swamp'), 'lava': ('lava del Nether', 'Nether lava'), 'vacio': ('Vacío del End', 'End void')}


def gen_lang(lineas):
    for n, lang in enumerate(('es_es', 'en_us')):
        p = os.path.join(ASSETS, 'lang', lang + '.json')
        cur = json.load(open(p, encoding='utf-8'))
        cur = {k: v for k, v in cur.items() if not k.startswith(('captura.', 'item.toodeepseas.potion.', 'item.toodeepseas.splash_potion.',
               'item.toodeepseas.lingering_potion.', 'item.toodeepseas.tipped_arrow.', 'effect.toodeepseas.', 'tooltip.toodeepseas.captura.'))}
        cur['itemGroup.toodeepseas.capturas'] = ['Too Deep Seas: capturas', 'Too Deep Seas: Catches'][n]
        cur['tooltip.toodeepseas.captura.tesoro'] = ['Tesoro de pesca', 'Fishing treasure'][n]
        cur['tooltip.toodeepseas.captura.donde'] = ['Vive en: %s (desde %s m)', 'Found in: %s (from %s m)'][n]
        cur['tooltip.toodeepseas.captura.vende'] = ['El pescador lo compra', 'Fishermen buy it'][n]
        for r in range(5): cur['tooltip.toodeepseas.captura.rareza.%d' % r] = RAREZA[n][r]
        for f, t in FAMILIA.items(): cur['tooltip.toodeepseas.captura.familia.' + f] = t[n]
        for b, t in BIOMA_TXT.items(): cur['tooltip.toodeepseas.captura.bioma.' + b] = t[n]
        nombres = (nombre_es, nombre_en)[n]
        for i in list(PEZ) + [t[1] for t in tesoros_propios] + list(COMIDAS):
            if i in VANILLA_FISH: continue
            cur['captura.toodeepseas.' + i] = nombres[i]
            for k, line in enumerate(lineas.get(i, ((), ()))[n]):
                cur['captura.toodeepseas.%s.uso.%d' % (i, k + 1)] = line
        for i, (es, en) in POCIONES.items():
            name = (es, en)[n]
            formas = ([('potion', 'Poción de '), ('splash_potion', 'Poción arrojadiza de '), ('lingering_potion', 'Poción persistente de '),
                       ('tipped_arrow', 'Flecha de ')],
                      [('potion', 'Potion of '), ('splash_potion', 'Splash Potion of '), ('lingering_potion', 'Lingering Potion of '),
                       ('tipped_arrow', 'Arrow of ')])[n]
            for forma, pre in formas:
                cur['item.minecraft.%s.effect.%s' % (forma, i)] = pre + name
        cur['effect.toodeepseas.aletas'] = ['Aletas', 'Fins'][n]
        cur['effect.toodeepseas.coraza'] = ['Coraza', 'Carapace'][n]
        write(p, cur)


# ---------------------------------------------------------------- texturas provisionales (Pillow)
def rgb(h):
    h = h.lstrip('#')
    return tuple(int(h[k:k + 2], 16) for k in (0, 2, 4))


def sh(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def mix(a, b, t):
    return tuple(int(a[k] * (1 - t) + b[k] * t) for k in range(3))


def rnd(seed):
    return int(hashlib.md5(seed.encode()).hexdigest()[:8], 16)


def outline(px, col):
    """Borde oscuro alrededor de lo pintado."""
    out = dict(px)
    for (x, y) in px:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if q not in px and 0 <= q[0] < 16 and 0 <= q[1] < 16: out[q] = col
    return out


def fish_pixels(d):
    c = rgb(d['color'])
    h = rnd(d['id'])
    shape = d.get('shape')
    px = {}

    def put(x, y, col):
        if 0 <= x < 16 and 0 <= y < 16: px[(x, y)] = col

    if shape == 'anguila':
        for x in range(1, 15):
            y = 8 + round(2.2 * math.sin(x * 0.9 + h % 3))
            put(x, y, c); put(x, y + 1, sh(c, 1.25))
        put(13, 8 + round(2.2 * math.sin(13 * 0.9 + h % 3)), (20, 20, 30))
    elif shape == 'raya':
        for y in range(3, 13):
            w = 5 - abs(y - 7.5)
            for x in range(16):
                if abs(x - 9) <= w * 1.1: put(x, y, sh(c, 1.2) if y > 8 else c)
        for x in range(0, 5): put(x, 8, sh(c, 0.8))
        put(11, 6, (20, 20, 30)); put(11, 9, (20, 20, 30))
    elif shape == 'calamar':
        for y in range(1, 9):
            w = 1.2 + (y - 1) * 0.35 if y < 6 else 3
            for x in range(16):
                if abs(x - 7.5) <= w: put(x, y, c)
        for k, x in enumerate((5, 7, 8, 10)):
            for y in range(9, 14 - (k % 2)): put(x, y, sh(c, 1.2))
        put(6, 7, (20, 20, 30)); put(9, 7, (20, 20, 30))
    elif shape == 'medusa':
        for y in range(2, 8):
            w = 2.5 + (y - 2) * 0.8 if y < 5 else 5
            for x in range(16):
                if abs(x - 7.5) <= w: put(x, y, sh(c, 1.15) if y < 4 else c)
        for x in (4, 6, 9, 11):
            for y in range(8, 13 + (x % 3) - 1): put(x + ((y // 2) % 2 if x % 2 else 0), y, sh(c, 0.85))
    elif shape == 'caballito':
        for (x, y) in ((8, 2), (9, 2), (7, 3), (8, 3), (9, 3), (10, 3), (11, 4), (12, 4), (8, 4), (9, 4), (8, 5), (9, 5), (7, 6), (8, 6), (9, 6),
                       (7, 7), (8, 7), (9, 7), (10, 7), (7, 8), (8, 8), (9, 8), (8, 9), (9, 9), (8, 10), (9, 10), (9, 11), (9, 12), (8, 13),
                       (7, 13), (6, 12), (7, 11)):
            put(x, y, c)
        for (x, y) in ((6, 6), (6, 8), (10, 9)): put(x, y, sh(c, 0.75))
        put(9, 3, (20, 20, 30))
    else:
        rape = shape == 'rape'
        build = h % 3
        rx, ry = [(5.2, 2.6), (4.6, 3.4), (5.8, 2.0)][build] if not rape else (4.4, 3.8)
        cx, cy = 9.0, 8.0
        for y in range(16):
            for x in range(16):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0:
                    col = c
                    if y - cy > ry * 0.35: col = sh(c, 1.28)
                    elif y - cy < -ry * 0.55: col = sh(c, 0.78)
                    px[(x, y)] = col
        pat = (h >> 4) % 4
        dark = sh(c, 0.62)
        if pat == 1:
            for x in (7, 10):
                for y in range(16):
                    if (x, y) in px and abs(y - cy) < ry * 0.8: px[(x, y)] = dark
        elif pat == 2:
            for (x, y) in ((7, 7), (9, 9), (11, 7)):
                if (x, y) in px: px[(x, y)] = dark
        elif pat == 3:
            for x in range(5, 13):
                if (x, 8) in px: px[(x, 8)] = sh(c, 1.4)
        tail = sh(c, 0.8)
        for k, x in enumerate((1, 2, 3)):
            for y in range(int(cy) - (3 - k), int(cy) + (3 - k) + 1): put(x, y, tail)
        put(4, 8, tail)
        for x in (8, 9, 10): put(x, int(cy - ry) - 1, tail)
        put(9, int(cy + ry) + 1, tail)
        put(12, 7, (255, 255, 255)); put(12, 7, (20, 20, 30))
        if rape:
            for (x, y) in ((12, 3), (13, 2), (14, 2)): put(x, y, sh(c, 0.7))
            put(15, 3, (255, 240, 120))
            for x in (12, 13): put(x, 10, (240, 240, 240))
    if d['rarity'] >= 3: px[(13, 4)] = (255, 255, 255) if (13, 4) not in px else px[(13, 4)]
    return outline(px, sh(c, 0.38))


MASCARAS = {
 'botella': ['......oo', '......ww', '.....obbo', '....obbbbo', '...obblbbbo', '..obbblwbbo', '..obbbwwbbo', '..obbbbbbo', '...obbbbo', '....oooo'],
 'pepita': ['', '', '....oooo', '...obblbo', '..obbllbbo', '..oblbbbdo', '..obbbbddo', '...obbddo', '....oooo'],
 'corona': ['', '..o...o...o', '.obo.obo.obo', '.oboobboobo', '.obbbblbbbbo', '.obwbblbbwbo', '.obbbbbbbbbo', '.odddddddddo', '..ooooooooo'],
 'perla': ['', '', '....oooo', '...obbbbo', '..obwlbbbo', '..oblbbbbo', '..obbbbbdo', '..obbbbddo', '...obbddo', '....oooo'],
 'cristal': ['......o', '.....olo', '....ollbo', '....olbbo..o', '.o..olbbo.olo', 'olo.olbbo.obo', 'obo.olbbdoobo', 'obdoolbbdobdo', 'obdolbbbddbdo', '.ooooooooooo'],
 'tridente': ['.........o.o.o', '.........obobobo', '.........obbbbo', '..........obbo', '.........obo', '........obo', '.......obo', '......obo', '.....obo', '....obo', '...obo', '..obo', '.odo', '.oo'],
 'gema': ['', '', '...ooooooo', '..obllllbbo', '.oblwlbbbbdo', '.obbbbbbbddo', '..obbbbbddo', '...obbbddo', '....obddo', '.....odo', '......o'],
 'lagrima': ['......o', '.....obo', '.....obo', '....oblbo', '....oblbo', '...obwlbbo', '...oblbbbo', '...obbbbdo', '...obbbddo', '....obddo', '.....ooo'],
 'moneda': ['', '', '....ooooo', '...obbbbbo', '..oblllbbdo', '..oblbdbbdo', '..oblbdbbdo', '..obbdddbdo', '...obbbddo', '....ooooo'],
 'coral': ['..o.....o', '.obo...obo..o', '.obo...obo.obo', '.obo.o.obo.obo', '..obobooboobo', '..obbbobbbbo', '...obbbbbbo', '....obbbbo', '.....obbo', '.....obbo', '....odddo', '....ooooo'],
 'ancla': ['......oo', '.....obbo', '.....obbo', '...ooobboooo', '...obbbbbbbo', '...ooobboooo', '.....obbo', '.o...obbo...o', 'obo..obbo..obo', 'obbo.obbo.obbo', '.obboobboobbo', '..obbbbbbbbo', '...oobbbboo', '.....oooo'],
 'geoda': ['', '....oooooo', '...oddddddo', '..oddlwllddo', '.oddlbwbblddo', '.oddlbbbblddo', '.odddlbbldddo', '..odddllddo', '...oddddddo', '....oooooo'],
 'caliz': ['.ooooooooo', '.oblllllbdo', '.obwbbbbbdo', '..obbbbbdo', '...obbbdo', '....obdo', '....obdo', '....obdo', '...obbbdo', '..obbbbbdo', '..ooooooo'],
 'anfora': ['....ooooo', '....obbbo', '.....obo', '..oooobooo', '.obooblboobo', '.oboblbbbobo', '..ooblbbbdoo', '...oblbbbdo', '...obbbbbdo', '....obbbdo', '.....obdo', '......oo'],
 'huevo': ['......ooo', '.....obbbo', '....oblbbbo', '...oblwbbbbo', '...oblbbbdbo', '..obbbbdbbbdo', '..obbdbbbbbdo', '..obbbbbbdddo', '...obbbbdddo', '....obbdddo', '.....ooooo'],
 'escama': ['', '.....oooo', '...oobbbbo', '..oblllbbbo', '.oblwlbbbbdo', '.oblbbbbbddo', '.obbbbbbdddo', '..obbbbdddo', '...obbdddo', '....ooooo'],
 'trofeo': ['.ooooooooooo', 'oobwllllbbdoo', 'obobllbbbdobo', 'obobbbbbbdobo', '.oobbbbbbdoo', '...obbbbdo', '....obbdo', '.....obo', '.....obo', '....obbdo', '..oobbbbdoo', '..obbbbbbdo', '..ooooooooo'],
 'cuenco': ['', '', '', '....cc.cc', '...cCccCcc', '.oooooooooooo', '.obcCcccCccbo', '.obbbbbbbbbbo', '..obbbbbbbdo', '..obbbbbbddo', '...obbbbddo', '....oooooo'],
 'brocheta': ['.............oo', '............oso', '..........ooso', '.........occo', '........occCo', '.......ooco', '......occo', '.....occCo', '....ooco', '...occo', '..occCo', '..ooso', '.oso', 'oso', 'oo'],
 'asado': ['', '', '', '......ddd', 'o...oodddoo', 'oo.obblbbbbbo', 'odobblbblbbebo', 'oddbblbblbbbbo', 'odobblbblbbbo', 'oo.obbbbbbbo', 'o...oooooo'],
 'filete': ['', '', '', '...ooooooo', '..obbbllbbbo', '.obbdbbbbdbbo', '.obbbdbbdbbbo', '.obbdbbbbdbbo', '..obbbbbbbbo', '...ooooooo'],
 'anillas': ['', '...ooo', '..obbbo...ooo', '.obo.obo.obbbo', '.obo.oboobo.obo', '..obbbo.obo.obo', '...ooooo.obbbo', '....obbbo.ooo', '...obo.obo', '...obo.obo', '....obbbo', '.....ooo'],
}


def mask_pixels(name, color, extra=None):
    c = rgb(color)
    pal = {'o': sh(c, 0.38), 'b': c, 'l': sh(c, 1.3), 'd': sh(c, 0.68), 'w': (255, 255, 255), 'e': (20, 20, 30),
           's': (133, 94, 54), 'c': c, 'C': sh(c, 1.35)}
    if extra: pal.update(extra)
    rows = MASCARAS[name]
    assert len(rows) <= 16 and all(len(r) <= 16 for r in rows), name
    off = (16 - len(rows)) // 2
    width = max(len(r) for r in rows)
    xo = (16 - width) // 2
    px = {}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != '.': px[(x + xo, y + off)] = pal[ch]
    return px


def save(px, path, size=16, alpha=255):
    from PIL import Image
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for (x, y), col in px.items():
        if 0 <= x < size and 0 <= y < size: img.putpixel((x, y), tuple(col) + (alpha,))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def gen_modelos_y_texturas():
    madera = {'o': (74, 50, 28), 'b': (148, 108, 62), 'd': (112, 80, 44)}
    hechos = []
    for d in peces:
        if d['id'] in VANILLA_FISH: continue
        save(fish_pixels(d), os.path.join(ASSETS, 'textures', 'item', 'capturas', d['id'] + '.png'), alpha=200 if d.get('ghost') else 255)
        hechos.append(d['id'])
    for name, i, vanilla, sil, r, depth, biomes, weather, col, en in tesoros_propios:
        save(mask_pixels(sil, col), os.path.join(ASSETS, 'textures', 'item', 'capturas', i + '.png'))
        hechos.append(i)
    for i, v in COMIDAS.items():
        extra = dict(madera) if v[6] == 'cuenco' else None
        save(mask_pixels(v[6], v[7], extra), os.path.join(ASSETS, 'textures', 'item', 'capturas', i + '.png'))
        hechos.append(i)
    for i in hechos:
        write(os.path.join(ASSETS, 'models', 'item', i + '.json'),
              {'parent': 'minecraft:item/generated', 'textures': {'layer0': M + 'item/capturas/' + i}})
    # iconos de los dos efectos nuevos (18×18)
    from PIL import Image
    for name, col, rows in (('aletas', '#4fc3e8', ['', '', '.........oo', '.......oobbo', '.....oobbbbo', '...oobbblbbo', '..obbbblbbbo', '.obbbblbbbbo',
                                                    '.obbblbbbbbo', '..obbbbbbbo', '...obbbbbo', '....ooooo', '..o..o..o', '.o..o..o']),
                            ('coraza', '#9aa7b4', ['', '...oooooooooo', '..obbbbllbbbbo', '..obblbbbbdbbo', '..oblbbbbbbdbo', '..oblbbbbbbdbo',
                                                    '..obbbbbbbbdbo', '..obbdbbbbdbbo', '...obbdbbdbbo', '....obbddbbo', '.....obbbbo', '......obbo',
                                                    '.......oo'])):
        c = rgb(col)
        pal = {'o': sh(c, 0.4), 'b': c, 'l': sh(c, 1.3), 'd': sh(c, 0.7)}
        img = Image.new('RGBA', (18, 18), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch != '.': img.putpixel((x + 1, y + 2), pal[ch] + (255,))
        p = os.path.join(ASSETS, 'textures', 'mob_effect', name + '.png')
        os.makedirs(os.path.dirname(p), exist_ok=True)
        img.save(p)


if __name__ == '__main__':
    gen_tags()
    gen_recetas()
    gen_loot()
    lineas = gen_catalogo()
    gen_lang(lineas)
    gen_modelos_y_texturas()
    print('peces propios:', sum(1 for d in peces if d['id'] not in VANILLA_FISH), '· tesoros propios:', len(tesoros_propios),
          '· comidas:', len(COMIDAS), '· etiquetas:', len(TAGS), '· mezclas:', sum(len(v) for v in MEZCLAS.values()))

# Too Deep Seas — Forge 1.20.1 (phase 7c: the minigame is playable)

Class, field and file names in the code are in Spanish (e.g. `Partida` = a minigame run, `Terreno` = terrain,
`Cofres` = chests, `cana_*` = fishing rods); this README explains them.

## What is in it
- `core/` (pure Java): `RodTier` (the 8 rods), `RodStats` (gear → minigame parameters, capped at 4 blocks/s), `Loadout` (slots), `AccessoryDef` and `BaitDef` (datapack definitions).
- `item/`: the 7 new rods (the wooden one is vanilla), the 20 accessories and the 10 baits as items, and the bundle-style inventory shortcut (`RodBundleHandler`).
- `block/` and `menu/`: the Fisher's Table and its window; the slots are a view of the rod's NBT.
- `client/`: table screen, rod tooltip (`RodTooltip`) and models.
- `data/`: loading of `data/toodeepseas/accessories/*.json` and `baits/*.json`, gear stored in the rod's NBT (`RodData{Slots:[{id,lvl,uses}],Baits:[{id,count}]}`) and the `/tds` test command.
- `net/`: sends the definitions to the client on join and after `/reload`.
- `recipe/`: a rod upgrade recipe that keeps accessories, bait and enchantments, and workshop recipes (`toodeepseas:taller`) for the Fisher's Table.
- `loot/`: a loot modifier that adds void shards (End cities, shulkers) and worms (shovel on dirt or grass).
- Treasure chests (`core/Cofres`, `item/CofreItem`, `world/Biomas`, `loot/CofrePescaModifier`): one chest per fishing biome, with quality, depth and weather; their loot tables are in `data/toodeepseas/loot_tables/cofres` and are generated with `tools/gen_cofres.py`.
- Villages (`world/Aldeas`, `mixin/`, `Config`): the Fisher's Table is the fisherman's job site block and replaces the barrel in any fisherman's houses that generate.
- Weather-exclusive chests (`item/CofreClimaItem`): rain chest and storm chest, with their own loot (`loot_tables/cofres/exclusivo`).
- Minigame, phase 7a (`core/juego/`, pure Java): `Azar` (the prototype's deterministic randomness), `Perfil` (terrain of each biome and Nether sub-biome), `Terreno` (a run's tunnel and caves from the seed) and `PezDef` (the 100 fish, in `data/toodeepseas/fish/*.json`, sent to the client).
- Minigame, phases 7b and 7c: `core/juego/Partida` (what is in each segment — rocks, hazards, bands, fish and treasures — and the fixed-step simulation, both deterministic) and `Grabacion` (the run's key presses); `juego/Sesiones` (the server measures biome, weather, sub-biome and depth when the hook touches the water, opens the screen and, at the end, replays the run with the client's key presses to hand out the loot and wear the rod); `net/AbrirMinijuegoPacket`, `FinMinijuegoPacket`, `RecogerPacket`; `client/MinijuegoScreen` (placeholder drawing with rectangles). Monsters are still missing (7d).
- Catches as items (`core/juego/Catalogo`, `item/PezItem`, `TesoroItem`, `CoronaItem`, `ComidaItem`, `effect/`, `loot/CapturaPescaModifier`): the 96 new fish (the other 4 are Minecraft's), 20 treasures, 11 foods, 2 effects and 10 potions. Everything comes from `tools/gen_capturas.py` (catalog `toodeepseas_capturas.json`, models, placeholder textures, texts, tags, recipes and loot).
- `prototipo/pesca.html`: web prototype of the minigame. `tools/paridad/`: loads the prototype in Node (`harness.js`) to dump the fish (`gen_peces.js`), the reference terrain (`gen_terreno.js`) and the segments (`gen_tramos.js`) that Java is compared against.

## Downloading the jar
The repository's Actions tab → latest green run → Artifacts → `toodeepseas-jar`. Copy the `.jar` into `mods` (Forge 1.20.1, 47.x).

## Testing in game
- **Fishing minigame**: cast any rod into the water. When the hook touches the water the screen opens: A/D or the arrow keys move the hook, S or the down arrow dives fast, and Space, E, Esc or right click reels in and leaves with what you caught. If you hit something you lose the loot. Fishing needs a body of at least 30 connected blocks of water (or lava), not necessarily vertical. A run's depth is the rod's line depth (tier, accessories and Depth), no matter how deep the water is: wood 30 m, copper 45, iron 70, gold 60, prismarine 80, diamond 95, netherite and void 120; the Void has no bottom. Leaving costs the rod half of its items (minimum 1) and dying costs all of them; each run uses one unit of bait. Lava: netherite or void rod. End void: void rod, cast from outside the island. With `minijuego.activo = false` in `config/toodeepseas-common.toml` you fish like in vanilla Minecraft again.
- Everything is in the "Too Deep Seas" creative tab: rods, Fisher's Table, accessories (at every level) and baits.
- **Fisher's Table** (2 rods + string on top of 3 planks): put a rod in the center and its slots appear. Accessories above and below, bait on the sides, stats on the right. Everything is saved to the rod immediately.
- **Inventory shortcut**: right click with an accessory or bait on a rod to put it in; right click with the rod on the cursor over an empty slot to take the last one out.
- Rules: 1 accessory per category (2 different charms), maximum level according to the rod's tier and as many slots as the rod has.
- The rod's tooltip shows tier, slots, gear with uses and bait; with Shift, the final stats.
- Rod recipes: the previous rod in the center surrounded by 8 materials (copper, iron, gold or diamond; prismarine: shards on the sides and crystals in the corners). Netherite: smithing table.
- **Workshop** (bottom row of the table): accessory + materials.
  - Upgrade: a level I or II accessory (even if worn) + the next level's materials → you get the next level, brand new. The materials are listed in each accessory's tooltip.
  - Repair: accessory + its repair material. Each unit restores 25% of the uses and costs 1 experience level.
  - Void rod: netherite rod + 2 shulker shells + 4 void shards. Keeps the gear.
- Recipes: every level I accessory and every bait is crafted on the crafting table (`data/toodeepseas/recipes/accesorios` and `cebos`). Birch cork comes from 2 birch logs placed vertically. Worms drop with an 8% chance when digging dirt or grass with a shovel. Void shards are in End city chests (50%, 1–3) and dropped by shulkers (20%).
- The Guardian Scale only fits in rods of tier 2.5 or higher.
- **Treasure chests** (like Terraria's fishing crates): right click to open them.
  - There is one per biome (ocean, warm ocean, frozen ocean, river, swamp, lava, Void). Each chest remembers its quality (plain, reinforced, ornate, ancient: set by the rod's tier), depth (surface, deep waters, abyssal) and weather (clear, rain, storm).
  - Loot: 2–5 rolls from the biome depending on quality, plus depth rolls, weather rolls and one fishing-gear roll depending on quality.
  - **Weather-exclusive chests**: the rain chest and the storm chest can only be fished in that weather, in any water biome (35% of the chests that drop while it rains or storms; the rest belong to the biome). They have their own loot: trident, conduit, loyalty and heads in the storm; riptide, respiration, heart of the sea and conduit in the rain. They also improve with quality and depth.
  - In the minigame, the "Sunken Chest" treasure is that biome's chest, with the rod's quality, the depth you reached and the weather. With the minigame off they drop when fishing the vanilla way (10% of catches, always surface). The rest, with `/tds cofre <biome|rain|storm> [quality 1-4] [band 1-3] [weather 1-3]`.
  - Which Minecraft biome counts as which: tags in `data/toodeepseas/tags/worldgen/biome/pesca/`. Anything not in any of them is "river".
- **Fish and treasures** ("Too Deep Seas: catches" creative tab). Each item's description says where it lives and what it is for.
  - The minigame's 100 fish are items (96 new; cod, salmon, tropical fish and pufferfish are Minecraft's). All can be eaten raw; abyssal ones give night vision, storm ones speed, lava ones fire resistance, Void ones slow falling, and the lionfish, stonefish and ray poison you.
  - Cooking (furnace, smoker or campfire): roast fish (small fish), grilled fillet (large) and squid rings. Legendary fish and lava and Void fish cannot be cooked.
  - Dishes with effects: abyssal soup, magma broth, arctic stew, tropical ceviche, swamp stew, storm eel, void skewer and leviathan feast. **With Farmer's Delight** they are made in the cooking pot (with onion, tomato, cabbage, rice, milk…) and fish are cut on the cutting board (cod or salmon fillets); they also count as fish in Farmer's Delight recipes (`forge:raw_fishes`). **Without it**, the dishes are crafted on the crafting table with Minecraft ingredients.
  - Potions (fish or treasure on an awkward potion): 10 new ones —haste, dolphin's grace, conduit power, resistance, absorption, glowing, levitation, decay, Fins (swim 40% faster per level) and Carapace (+4 armor per level)— with redstone, glowstone, gunpowder and dragon's breath as usual; plus other ways to make 14 of Minecraft's (luck, water breathing, night vision, fire resistance, invisibility, regeneration, healing, swiftness, leaping, strength, slow falling, poison, slowness and harming).
  - Materials: 23 catches can be broken down on the crafting table (ender pearls, quartz, obsidian, blaze powder, ink, prismarine crystals…), the golden bastion fish and the slag fish can be smelted (gold ingot, netherite scrap) and lava fish work as fuel.
  - Treasures: the message in a bottle, the geode and the sealed amphora open with a right click; the sunken chalice removes harmful effects (not consumed, 5 min cooldown); the two crowns are worn on the head (Luck / Conduit Power) and the trophy in the off hand; 4 gold coins = 1 ingot; glacial crystal + 8 ice = 8 blue ice; rusty trident + prismarine = trident; black bog relic + 8 gold ingots = totem of undying.
  - The fisherman villager buys the mod's fish (more emeralds the rarer they are) and gold coins.
  - They are obtained in the minigame. With the minigame off, fishing the vanilla way: the fish that comes up is from that biome and weather, down to the rod's line depth and counting its baits; half of the vanilla treasures are swapped for one from the mod.
- **Villages**: a jobless villager next to a Fisher's Table becomes a fisherman; the barrel no longer works for that. New villages come with the table in the fisherman's house (existing ones keep their barrel, which still works for the villager who had it). With `barrilLegado = true` in `config/toodeepseas-common.toml` the barrel works again and villages generate as usual (restart required).
- `/tds equipar <accessory> <level>`, `/tds cebo <bait> <amount>`, `/tds vaciar` with the rod in hand (operator).

## Status
- Builds on GitHub Actions and the tests in `core/` and the in-game tests pass (`test/GearGameTests`: data, recipes, table, workshop, shortcut, loot, chests and villages; `test/CapturasGameTests`: items, food, potions, recipes, fishing and treasures, and everything again with Farmer's Delight installed). Java's terrain is compared against the prototype's in 44 runs (11 biomes × 4 seeds and gear sets) and what is in each segment in another 42. `test/MinijuegoGameTests` checks the spot measurement, the packet and the loot on leaving and on dying; `PartidaTest` checks that the server gets the same result as the client when replaying the run and that every biome can be reached to the bottom. Actions fails if the test server does not start and checks that the jar carries the Mixin refmap.
- The visuals (minigame screen, table window, tooltips, textures) have only been checked by compiling: they still need to be seen in game.
- The minigame has no monsters yet (phase 7d), accessories do not wear down when fishing and their special effects (`special`) are not implemented. The vanilla hook disappears when the screen opens. Rods use the vanilla texture; the textures of accessories, baits and the table are placeholders.
- The reef shrimp and the tiger shark tooth cannot be obtained in survival yet: they are loot from the minigame and from the monster.

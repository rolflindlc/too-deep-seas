package com.rolflin.toodeepseas.test;

import com.mojang.logging.LogUtils;
import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.core.Cofres;
import com.rolflin.toodeepseas.core.juego.Catalogo;
import com.rolflin.toodeepseas.core.juego.PezDef;
import com.rolflin.toodeepseas.data.Defs;
import com.rolflin.toodeepseas.effect.ModEffects;
import com.rolflin.toodeepseas.effect.ModPotions;
import com.rolflin.toodeepseas.item.CapturasEventos;
import com.rolflin.toodeepseas.item.ComidaItem;
import com.rolflin.toodeepseas.item.ModItems;
import com.rolflin.toodeepseas.item.PezItem;
import com.rolflin.toodeepseas.item.TesoroItem;
import com.rolflin.toodeepseas.loot.CapturaPescaModifier;
import com.rolflin.toodeepseas.world.Biomas;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Pruebas dentro de un servidor real de las capturas como objetos: peces, tesoros, comida, pociones, recetas
 * (también las de Farmer's Delight cuando está instalado) y la pesca.
 */
@GameTestHolder(TooDeepSeas.MODID)
@PrefixGameTestTemplate(false)
public final class CapturasGameTests {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String FD = "farmersdelight";

    private CapturasGameTests() {
    }

    private static void check(boolean condition, String what) {
        if (!condition) throw new GameTestAssertException(what);
    }

    private static Item item(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        check(item != null && item != Items.AIR, "no existe el objeto " + id);
        return item;
    }

    private static Item propio(String id) {
        return item(TooDeepSeas.MODID + ":" + id);
    }

    private static Optional<? extends Recipe<?>> receta(ServerLevel level, String id) {
        return level.getRecipeManager().byKey(new ResourceLocation(TooDeepSeas.MODID, "capturas/" + id));
    }

    private static <T extends Recipe<Container>> ItemStack cocinar(ServerLevel level, RecipeType<T> type, Item crudo) {
        Optional<T> r = level.getRecipeManager().getRecipeFor(type, new SimpleContainer(new ItemStack(crudo)), level);
        return r.isPresent() ? r.get().getResultItem(level.registryAccess()) : ItemStack.EMPTY;
    }

    private static ItemStack resultado(ServerLevel level, String id) {
        Optional<? extends Recipe<?>> r = receta(level, id);
        check(r.isPresent(), "falta la receta capturas/" + id);
        return r.get().getResultItem(level.registryAccess());
    }

    @GameTest(template = "empty")
    public static void capturasObjetos(GameTestHelper helper) {
        check(ModItems.PECES.size() == 96, "peces como objeto: " + ModItems.PECES.size());
        check(ModItems.TESOROS.size() == 20, "tesoros como objeto: " + ModItems.TESOROS.size());
        check(ModItems.COMIDAS.size() == 11, "comidas: " + ModItems.COMIDAS.size());
        // Los 100 peces del datapack tienen objeto (96 del mod y los 4 de Minecraft)
        for (String id : Defs.PECES.ids()) check(!ModItems.pezStack(id).isEmpty(), "pez sin objeto: " + id);
        for (RegistryObject<PezItem> pez : ModItems.PECES.values()) {
            ItemStack stack = new ItemStack(pez.get());
            check(stack.isEdible(), "no se come: " + pez.getId());
            // Los efectos al comer existen (un id mal escrito daría Suerte)
            for (Catalogo.Efecto e : pez.get().def().effects()) check(ForgeRegistries.MOB_EFFECTS.containsKey(new ResourceLocation(e.id())), "efecto: " + e.id());
        }
        for (Catalogo.Tesoro t : Catalogo.TESOROS) {
            if (!t.chest()) check(!ModItems.tesoroStack(t.id()).isEmpty(), "tesoro sin objeto: " + t.id());
            for (Catalogo.Efecto e : t.effects()) check(ForgeRegistries.MOB_EFFECTS.containsKey(new ResourceLocation(e.id())), "efecto: " + e.id());
        }
        for (Catalogo.Comida c : Catalogo.COMIDAS) {
            for (Catalogo.Efecto e : c.effects()) check(ForgeRegistries.MOB_EFFECTS.containsKey(new ResourceLocation(e.id())), "efecto: " + e.id());
            if (c.container() != null) item(c.container());
        }
        // Etiquetas: cuentan como pescado para Minecraft y para otros mods
        check(new ItemStack(propio("sardina")).is(ItemTags.FISHES), "la sardina no está en minecraft:fishes");
        check(!new ItemStack(propio("pez_eterno")).is(ItemTags.FISHES), "un legendario no es pescado corriente");
        // Los peces de lava arden y no se queman
        check(ForgeHooks.getBurnTime(new ItemStack(propio("pez_de_brasa")), RecipeType.SMELTING) == 4800, "pez de brasa como combustible");
        check(ForgeHooks.getBurnTime(new ItemStack(propio("sardina")), RecipeType.SMELTING) == 0, "la sardina no arde");
        check(propio("pez_de_magma").isFireResistant(), "pez de magma resistente al fuego");
        check(Mob.getEquipmentSlotForItem(new ItemStack(propio("corona_del_pantano"))) == EquipmentSlot.HEAD, "la corona va en la cabeza");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void capturasComida(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer();

        // Plato en cuenco: da sus efectos y devuelve el cuenco
        ComidaItem sopa = ModItems.COMIDAS.get("sopa_abisal").get();
        ItemStack two = new ItemStack(sopa, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, two);
        ItemStack rest = two.finishUsingItem(level, player);
        check(rest.getItem() == sopa && rest.getCount() == 1, "comer una sopa de dos: " + rest);
        check(player.hasEffect(MobEffects.NIGHT_VISION) && player.hasEffect(MobEffects.WATER_BREATHING), "efectos de la sopa abisal");
        check(player.getInventory().countItem(Items.BOWL) == 1, "el cuenco vuelve al inventario");
        ItemStack last = new ItemStack(sopa).finishUsingItem(level, player);
        check(last.is(Items.BOWL), "la última sopa deja el cuenco en la mano: " + last);

        // Efectos del mod en la comida y pez venenoso
        new ItemStack(ModItems.COMIDAS.get("ceviche_tropical").get()).finishUsingItem(level, player);
        check(player.hasEffect(ModEffects.ALETAS.get()), "el ceviche da Aletas");
        new ItemStack(propio("pez_piedra")).finishUsingItem(level, player);
        MobEffectInstance poison = player.getEffect(MobEffects.POISON);
        check(poison != null && poison.getAmplifier() == 1, "el pez piedra envenena (II)");

        // Aletas: +40 % de nado por nivel. Coraza: +4 de armadura por nivel
        player.removeAllEffects();
        double swim = player.getAttributeValue(ForgeMod.SWIM_SPEED.get()), armor = player.getAttributeValue(Attributes.ARMOR);
        player.addEffect(new MobEffectInstance(ModEffects.ALETAS.get(), 200, 1));
        player.addEffect(new MobEffectInstance(ModEffects.CORAZA.get(), 200, 0));
        check(Math.abs(player.getAttributeValue(ForgeMod.SWIM_SPEED.get()) - swim * 1.8) < 1e-6, "Aletas II: " + player.getAttributeValue(ForgeMod.SWIM_SPEED.get()));
        check(Math.abs(player.getAttributeValue(Attributes.ARMOR) - (armor + 4)) < 1e-6, "Coraza: " + player.getAttributeValue(Attributes.ARMOR));
        player.removeAllEffects();
        check(Math.abs(player.getAttributeValue(ForgeMod.SWIM_SPEED.get()) - swim) < 1e-6 && Math.abs(player.getAttributeValue(Attributes.ARMOR) - armor) < 1e-6,
                "al acabar el efecto todo vuelve a su valor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void capturasPociones(GameTestHelper helper) {
        ItemStack awkward = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.AWKWARD);
        int mezclas = 0;
        for (Catalogo.Mezcla m : Catalogo.MEZCLAS) {
            Potion to = ForgeRegistries.POTIONS.getValue(new ResourceLocation(m.to()));
            check(to != null && to != Potions.EMPTY, "poción desconocida: " + m.to());
            for (String id : m.with()) {
                ItemStack ing = new ItemStack(item(id));
                check(BrewingRecipeRegistry.isValidIngredient(ing), "el soporte no acepta " + id);
                ItemStack out = BrewingRecipeRegistry.getOutput(awkward.copy(), ing);
                check(out.is(Items.POTION) && PotionUtils.getPotion(out) == to, id + " sobre poción rara da " + PotionUtils.getPotion(out).getName("") + ", no " + m.to());
                mezclas++;
            }
        }
        check(mezclas == 51, "mezclas probadas: " + mezclas);
        // Alargar, reforzar y pasar a arrojadiza
        ModPotions.Familia prisa = ModPotions.FAMILIAS.get("prisa");
        ItemStack base = PotionUtils.setPotion(new ItemStack(Items.POTION), prisa.base().get());
        check(PotionUtils.getPotion(BrewingRecipeRegistry.getOutput(base.copy(), new ItemStack(Items.REDSTONE))) == prisa.larga().get(), "prisa + redstone");
        check(PotionUtils.getPotion(BrewingRecipeRegistry.getOutput(base.copy(), new ItemStack(Items.GLOWSTONE_DUST))) == prisa.fuerte().get(), "prisa + piedra luminosa");
        ItemStack splash = BrewingRecipeRegistry.getOutput(base.copy(), new ItemStack(Items.GUNPOWDER));
        check(splash.is(Items.SPLASH_POTION) && PotionUtils.getPotion(splash) == prisa.base().get(), "prisa + pólvora: " + splash);
        ItemStack splashAwkward = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.AWKWARD);
        ItemStack splashOut = BrewingRecipeRegistry.getOutput(splashAwkward, new ItemStack(propio("pez_shulker")));
        check(splashOut.is(Items.SPLASH_POTION) && PotionUtils.getPotion(splashOut) == ModPotions.FAMILIAS.get("levitacion").base().get(), "arrojadiza rara + pez shulker");
        // Lo de Minecraft sigue igual, y un pez no hace nada sobre agua
        ItemStack sugar = BrewingRecipeRegistry.getOutput(awkward.copy(), new ItemStack(Items.SUGAR));
        check(PotionUtils.getPotion(sugar) == Potions.SWIFTNESS, "poción rara + azúcar");
        ItemStack water = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
        check(BrewingRecipeRegistry.getOutput(water, new ItemStack(propio("pez_shulker"))).isEmpty(), "agua + pez shulker no da nada");
        check(prisa.fuerte().get().getEffects().get(0).getAmplifier() == 1, "prisa II");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void capturasRecetas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RecipeManager recipes = level.getRecipeManager();
        // Cocina: cada pez que se cocina tiene receta de horno, ahumador y hoguera
        Map<String, String> cocinado = Map.of("sardina", "pescado_asado", "atun_rojo", "filete_a_la_brasa", "calamar_de_hielo", "anillas_de_calamar");
        for (Map.Entry<String, String> e : cocinado.entrySet()) {
            Item crudo = propio(e.getKey()), hecho = propio(e.getValue());
            check(cocinar(level, RecipeType.SMELTING, crudo).is(hecho), e.getKey() + " al horno");
            check(cocinar(level, RecipeType.SMOKING, crudo).is(hecho), e.getKey() + " al ahumador");
            check(cocinar(level, RecipeType.CAMPFIRE_COOKING, crudo).is(hecho), e.getKey() + " a la hoguera");
        }
        int cocinables = 0;
        for (RegistryObject<PezItem> pez : ModItems.PECES.values()) {
            if (!cocinar(level, RecipeType.SMELTING, pez.get()).isEmpty()) cocinables++;
        }
        check(cocinables > 50, "peces que se pueden cocinar o fundir: " + cocinables);
        check(cocinar(level, RecipeType.SMELTING, propio("pez_eterno")).isEmpty(), "un legendario no se asa");
        check(cocinar(level, RecipeType.BLASTING, propio("pez_de_bastion_dorado")).is(Items.GOLD_INGOT), "pez de bastión dorado fundido");

        // Despieces y recetas sueltas
        ItemStack perlas = resultado(level, "despiece/perla_negra");
        check(perlas.is(Items.ENDER_PEARL) && perlas.getCount() == 4, "perla negra: " + perlas);
        check(resultado(level, "despiece/pez_de_cuarzo").is(Items.QUARTZ), "pez de cuarzo");
        check(resultado(level, "lingote_de_monedas").is(Items.GOLD_INGOT), "monedas");
        check(resultado(level, "tridente").is(Items.TRIDENT), "tridente");
        check(resultado(level, "totem").is(Items.TOTEM_OF_UNDYING), "tótem");
        ItemStack hielo = resultado(level, "hielo_azul");
        check(hielo.is(Items.BLUE_ICE) && hielo.getCount() == 8, "hielo azul: " + hielo);

        // Platos: con Farmer's Delight, en la olla; sin él, en la mesa de trabajo
        boolean fd = ModList.get().isLoaded(FD);
        LOG.info("Too Deep Seas (pruebas): Farmer's Delight cargado = {}", fd);
        Set<String> platos = Set.of("sopa_abisal", "caldo_de_magma", "estofado_artico", "ceviche_tropical", "guiso_del_pantano", "anguila_de_tormenta",
                "brocheta_del_vacio", "festin_del_leviatan");
        for (String plato : platos) {
            check(receta(level, "platos/" + plato).isPresent() == !fd, "receta de mesa de " + plato + " (Farmer's Delight: " + fd + ")");
            check(receta(level, "platos/" + plato + "_olla").isPresent() == fd, "receta de olla de " + plato + " (Farmer's Delight: " + fd + ")");
            String id = fd ? "platos/" + plato + "_olla" : "platos/" + plato;
            check(resultado(level, id).is(propio(plato)), "resultado de " + id);
        }
        for (String corte : new String[]{"cod_2", "cod_3", "cod_4", "salmon_2", "salmon_3", "salmon_4"}) {
            check(receta(level, "corte/" + corte).isPresent() == fd, "receta de tabla de cortar " + corte + " (Farmer's Delight: " + fd + ")");
        }
        int revisadas = 0;
        for (Recipe<?> r : recipes.getRecipes()) {
            if (!r.getId().getNamespace().equals(TooDeepSeas.MODID) || !r.getId().getPath().startsWith("capturas/")) continue;
            revisadas++;
            String type = String.valueOf(ForgeRegistries.RECIPE_TYPES.getKey(r.getType()));
            if (r.getId().getPath().contains("_olla")) check(type.equals(FD + ":cooking"), r.getId() + " es " + type);
            if (r.getId().getPath().contains("/corte/")) check(type.equals(FD + ":cutting"), r.getId() + " es " + type);
            // Ningún ingrediente se queda sin objetos (etiqueta vacía o mal escrita)
            for (var ing : r.getIngredients()) check(ing.isEmpty() || ing.getItems().length > 0, r.getId() + ": ingrediente sin objetos");
        }
        check(revisadas == (fd ? 54 : 48), "recetas de capturas cargadas: " + revisadas);
        if (fd) {
            // Los objetos de Farmer's Delight que usan las recetas existen de verdad
            item(FD + ":tomato_sauce");
            item(FD + ":cod_slice");
            item(FD + ":salmon_slice");
            // Los peces del mod valen en las recetas de pescado de Farmer's Delight
            check(new ItemStack(propio("sardina")).is(ItemTags.create(new ResourceLocation("forge", "raw_fishes"))), "sardina en forge:raw_fishes");
            check(new ItemStack(propio("trucha_arcoiris")).is(ItemTags.create(new ResourceLocation("forge", "raw_fishes/salmon"))), "trucha en forge:raw_fishes/salmon");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void capturasPesca(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        Vec3 origin = Vec3.atCenterOf(pos);
        Cofres.Bioma bioma = Biomas.bioma(level, pos);
        Cofres.Clima clima = Biomas.clima(level, pos);
        Map<Item, PezDef> porObjeto = new HashMap<>();
        for (String id : Defs.PECES.ids()) porObjeto.put(ModItems.pezStack(id).getItem(), Defs.PECES.get(id));

        // Pescando a la manera vanilla salen los peces del mod de ese bioma, hasta la profundidad del sedal
        LootTable fishing = level.getServer().getLootData().getLootTable(new ResourceLocation("minecraft", "gameplay/fishing"));
        LootParams cast = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD)).create(LootContextParamSets.FISHING);
        int peces = 0, propios = 0;
        java.util.Set<Item> distintos = new java.util.HashSet<>();
        for (int i = 0; i < 2000; i++) {
            for (ItemStack s : fishing.getRandomItems(cast)) {
                PezDef def = porObjeto.get(s.getItem());
                if (def == null) continue;
                peces++;
                if (s.getItem() instanceof PezItem) propios++;
                distintos.add(s.getItem());
                check(def.aparece(bioma, clima, null, Set.of(), 30), "con caña de madera en " + bioma + " ha salido " + def.id());
            }
        }
        check(peces > 1200, "peces en 2000 capturas: " + peces);
        check(propios > peces * 0.35 && distintos.size() >= 5, "peces del mod: " + propios + " de " + peces + ", distintos: " + distintos.size());

        // En todos los biomas de agua hay algo que pescar a 30 m y tesoros propios a 120 m; en lava y Vacío también
        RandomSource random = RandomSource.create(7);
        for (Cofres.Bioma b : Cofres.Bioma.values()) {
            for (int i = 0; i < 50; i++) {
                ItemStack pez = CapturaPescaModifier.pez(b, Cofres.Clima.DESPEJADO, null, Set.of(), 30, random);
                check(!pez.isEmpty() && porObjeto.get(pez.getItem()).biomes().contains(b), "pez de " + b + ": " + pez);
                ItemStack tesoro = CapturaPescaModifier.tesoro(b, Cofres.Clima.DESPEJADO, b == Cofres.Bioma.VACIO ? 1000 : 120, random);
                check(tesoro.getItem() instanceof TesoroItem t && t.def().biomes().contains(b), "tesoro de " + b + ": " + tesoro);
            }
        }
        // Los legendarios salen, pero poco
        int legendarios = 0;
        for (int i = 0; i < 4000; i++) {
            ItemStack pez = CapturaPescaModifier.pez(Cofres.Bioma.OCEANO, Cofres.Clima.TORMENTA, null, Set.of(), 200, random);
            if (porObjeto.get(pez.getItem()).rarity() == 4) legendarios++;
        }
        check(legendarios > 0 && legendarios < 400, "legendarios en 4000 capturas a 200 m: " + legendarios);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void capturasTesoros(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO));
        Player player = helper.makeMockPlayer();

        // Los que se abren: su tabla existe y siempre da algo
        for (Catalogo.Tesoro t : Catalogo.TESOROS) {
            if (!t.kind().equals("abrir")) continue;
            LootTable table = level.getServer().getLootData().getLootTable(new ResourceLocation(TooDeepSeas.MODID, "tesoros/" + t.id()));
            check(table != LootTable.EMPTY, "falta la tabla tesoros/" + t.id());
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin).create(LootContextParamSets.CHEST);
            for (int i = 0; i < 40; i++) check(!table.getRandomItems(params).isEmpty(), "tesoros/" + t.id() + " no da nada");
        }
        TesoroItem botella = ModItems.TESOROS.get("botella_con_mensaje").get();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(botella, 2));
        botella.use(level, player, InteractionHand.MAIN_HAND);
        check(player.getMainHandItem().getCount() == 1, "abrir gasta una botella: " + player.getMainHandItem());
        check(player.getInventory().countItem(Items.GLASS_BOTTLE) == 1, "queda la botella vacía");
        int otros = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() != botella && !s.is(Items.GLASS_BOTTLE)) otros++;
        }
        check(otros >= 1, "el botín de la botella no ha llegado al inventario");

        // Cáliz: quita lo dañino, deja lo bueno, no se gasta y tiene espera
        TesoroItem caliz = ModItems.TESOROS.get("caliz_hundido").get();
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 600));
        player.addEffect(new MobEffectInstance(MobEffects.WITHER, 600));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 600));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(caliz));
        caliz.use(level, player, InteractionHand.MAIN_HAND);
        check(!player.hasEffect(MobEffects.POISON) && !player.hasEffect(MobEffects.WITHER) && player.hasEffect(MobEffects.MOVEMENT_SPEED), "el cáliz purifica");
        check(player.getMainHandItem().getItem() == caliz && player.getCooldowns().isOnCooldown(caliz), "el cáliz no se gasta y queda en espera");

        // Coronas y trofeo: efectos mientras se llevan, y solo en su sitio
        player.removeAllEffects();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(propio("corona_del_rey_ahogado")));
        CapturasEventos.aplicarReliquias(player);
        check(!player.hasEffect(MobEffects.CONDUIT_POWER), "la corona en la mano no hace nada");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(propio("corona_del_rey_ahogado")));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(propio("trofeo_leviatan_del_vacio")));
        CapturasEventos.aplicarReliquias(player);
        check(player.hasEffect(MobEffects.CONDUIT_POWER), "corona del rey ahogado en la cabeza");
        MobEffectInstance luck = player.getEffect(MobEffects.LUCK);
        check(player.hasEffect(MobEffects.SLOW_FALLING) && luck != null && luck.getAmplifier() == 1, "trofeo en la mano secundaria");

        // El pescador compra peces del mod y monedas
        Villager villager = EntityType.VILLAGER.create(level);
        var trades = VillagerTrades.TRADES.get(VillagerProfession.FISHERMAN);
        RandomSource random = RandomSource.create(3);
        int compras = 0;
        boolean monedas = false;
        for (int nivel = 1; nivel <= 5; nivel++) {
            boolean pez = false;
            for (VillagerTrades.ItemListing listing : trades.get(nivel)) {
                MerchantOffer offer = listing.getOffer(villager, random);
                if (offer == null) continue;
                if (offer.getBaseCostA().getItem() instanceof PezItem p) {
                    pez = true;
                    check(p.def().rarity() == nivel - 1 && offer.getResult().is(Items.EMERALD), "compra de nivel " + nivel + ": " + offer.getBaseCostA());
                }
                if (offer.getBaseCostA().is(propio("moneda_de_oro"))) monedas = true;
            }
            if (pez) compras++;
        }
        check(compras == 5 && monedas, "compras del pescador: " + compras + ", monedas: " + monedas);
        helper.succeed();
    }
}

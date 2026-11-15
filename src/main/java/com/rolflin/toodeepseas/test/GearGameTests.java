package com.rolflin.toodeepseas.test;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.core.Cofres;
import com.rolflin.toodeepseas.core.RodTier;
import com.rolflin.toodeepseas.data.Defs;
import com.rolflin.toodeepseas.data.RodData;
import com.rolflin.toodeepseas.item.AccessoryItem;
import com.rolflin.toodeepseas.item.BaitItem;
import com.rolflin.toodeepseas.item.CofreItem;
import com.rolflin.toodeepseas.item.ModItems;
import com.rolflin.toodeepseas.menu.MesaPescadorMenu;
import com.rolflin.toodeepseas.recipe.ModRecipes;
import com.rolflin.toodeepseas.block.ModBlocks;
import com.rolflin.toodeepseas.mixin.StructureTemplatePoolAccessor;
import com.rolflin.toodeepseas.world.Aldeas;
import com.rolflin.toodeepseas.world.Biomas;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Pruebas dentro de un servidor real (gradlew runGameTestServer): que el mod carga con sus datos
 * y que ni la mesa del pescador ni el atajo de inventario pierden o duplican objetos.
 * Solo se registran cuando la propiedad forge.enabledGameTestNamespaces incluye el mod.
 */
@GameTestHolder(TooDeepSeas.MODID)
@PrefixGameTestTemplate(false)
public final class GearGameTests {
    private GearGameTests() {
    }

    private static void check(boolean condition, String what) {
        if (!condition) throw new GameTestAssertException(what);
    }

    private static boolean isAccessory(ItemStack stack, String id, int level) {
        return stack.getItem() instanceof AccessoryItem acc && acc.id().equals(id) && AccessoryItem.level(stack) == level;
    }

    @GameTest(template = "empty")
    public static void datosCargados(GameTestHelper helper) {
        check(Defs.ACCESSORIES.ids().size() == 20, "accesorios cargados: " + Defs.ACCESSORIES.ids().size());
        check(Defs.BAITS.ids().size() == 10, "cebos cargados: " + Defs.BAITS.ids().size());
        for (String id : Defs.ACCESSORIES.ids()) check(ModItems.ACCESSORIES.containsKey(id), "accesorio sin objeto: " + id);
        for (String id : Defs.BAITS.ids()) check(ModItems.BAITS.containsKey(id), "cebo sin objeto: " + id);
        for (String recipe : new String[]{"mesa_pescador", "cana_cobre", "cana_hierro", "cana_oro", "cana_prismarina", "cana_diamante", "cana_netherita", "taller/cana_vacio"}) {
            check(helper.getLevel().getRecipeManager().byKey(new ResourceLocation(TooDeepSeas.MODID, recipe)).isPresent(), "falta la receta " + recipe);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mesaDelPescador(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        MesaPescadorMenu menu = new MesaPescadorMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        int acc = MesaPescadorMenu.ACC_START, bait = MesaPescadorMenu.BAIT_START;

        // Hierro: 3 ranuras, accesorios hasta nivel II, 1 cebo
        ItemStack rod = new ItemStack(ModItems.CANA_HIERRO.get());
        check(RodData.tryAdd(rod, "plomada", 2) == null, "equipar plomada II");
        check(RodData.addBait(rod, "lombriz", 10) == 10, "meter 10 lombrices");

        check(!menu.getSlot(acc).mayPlace(ModItems.accessoryStack("carrete", 1, -1)), "sin caña no se puede equipar");
        menu.getSlot(MesaPescadorMenu.ROD).set(rod);
        check(isAccessory(menu.getSlot(acc).getItem(), "plomada", 2), "la plomada aparece en la primera ranura");
        check(menu.getSlot(bait).getItem().getItem() instanceof BaitItem && menu.getSlot(bait).getItem().getCount() == 10, "el cebo aparece en su ranura");
        check(!menu.getSlot(acc + 3).isActive(), "la cuarta ranura no existe en la caña de hierro");
        check(!menu.getSlot(bait + 1).isActive(), "la segunda ranura de cebo es solo de la prismarina");

        ItemStack carrete = ModItems.accessoryStack("carrete", 1, -1);
        check(menu.getSlot(acc + 1).mayPlace(carrete), "el carrete cabe");
        menu.getSlot(acc + 1).set(carrete);
        check(RodData.slots(rod).size() == 2, "el carrete queda escrito en la caña");
        check(!menu.getSlot(acc + 2).mayPlace(ModItems.accessoryStack("flotador", 1, -1)), "dos pesos no caben");
        check(!menu.getSlot(acc + 2).mayPlace(ModItems.accessoryStack("carrete", 2, -1)), "el mismo accesorio no cabe dos veces");
        check(!menu.getSlot(acc + 2).mayPlace(ModItems.accessoryStack("sedal_reforzado", 3, -1)), "nivel III no cabe en hierro");
        check(menu.getSlot(acc + 2).mayPlace(ModItems.accessoryStack("sedal_reforzado", 2, -1)), "nivel II sí cabe");
        check(menu.getSlot(acc).mayPlace(ModItems.accessoryStack("flotador", 1, -1)), "se puede cambiar la plomada por un flotador");
        check(!menu.getSlot(bait).mayPlace(carrete), "un accesorio no va en la ranura de cebo");

        menu.getSlot(bait).remove(4);
        check(RodData.baits(rod).size() == 1 && RodData.baits(rod).get(0).count() == 6, "quedan 6 lombrices en la caña");
        menu.getSlot(acc).remove(1);
        check(RodData.slots(rod).size() == 1 && RodData.slots(rod).get(0).id().equals("carrete"), "solo queda el carrete");

        ItemStack taken = menu.getSlot(MesaPescadorMenu.ROD).remove(1);
        check(!menu.getSlot(acc + 1).hasItem() && !menu.getSlot(bait).hasItem(), "al retirar la caña se vacían las ranuras");
        check(RodData.slots(taken).size() == 1 && RodData.baits(taken).get(0).count() == 6, "la caña retirada conserva su equipo");

        // Otra caña en la misma mesa no hereda nada de la anterior
        ItemStack other = new ItemStack(ModItems.CANA_COBRE.get());
        menu.getSlot(MesaPescadorMenu.ROD).set(other);
        check(!menu.getSlot(acc).hasItem() && !menu.getSlot(acc + 1).hasItem() && !menu.getSlot(bait).hasItem(), "una caña nueva empieza vacía");
        check(RodData.slots(other).isEmpty() && RodData.baits(other).isEmpty(), "y no recibe el equipo de la anterior");
        menu.getSlot(MesaPescadorMenu.ROD).remove(1);

        // Mayús + clic desde la barra rápida (casilla 0 = índice INV_START + 27)
        player.getInventory().setItem(0, taken);
        player.getInventory().setItem(1, ModItems.accessoryStack("anzuelo_afilado", 1, -1));
        player.getInventory().setItem(2, ModItems.baitStack("lombriz", 60));
        menu.quickMoveStack(player, MesaPescadorMenu.INV_START + 27);
        check(!menu.rod().isEmpty() && player.getInventory().getItem(0).isEmpty(), "la caña sube a la mesa");
        check(isAccessory(menu.getSlot(acc).getItem(), "carrete", 1), "y vuelve a desplegar su equipo");
        menu.quickMoveStack(player, MesaPescadorMenu.INV_START + 28);
        check(RodData.slots(menu.rod()).size() == 2 && player.getInventory().getItem(1).isEmpty(), "el anzuelo se equipa con Mayús + clic");
        menu.quickMoveStack(player, MesaPescadorMenu.INV_START + 29);
        check(RodData.baits(menu.rod()).get(0).count() == 64 && player.getInventory().getItem(2).getCount() == 2, "el cebo se apila hasta 64 y sobran 2");

        // Al cerrar, el equipo viaja dentro de la caña y no queda nada suelto
        ItemStack inTable = menu.rod();
        menu.removed(player);
        check(!menu.getSlot(acc).hasItem() && !menu.getSlot(bait).hasItem(), "las ranuras se vacían al cerrar");
        check(RodData.slots(inTable).size() == 2 && RodData.baits(inTable).get(0).count() == 64, "la caña conserva todo al cerrar");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recetas(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String id : ModItems.ACCESSORIES.keySet()) {
            check(recipes.byKey(new ResourceLocation(TooDeepSeas.MODID, "accesorios/" + id)).isPresent(), "falta la receta de " + id);
        }
        for (String id : ModItems.BAITS.keySet()) {
            // La lombriz sale al cavar y la gamba es botín del minijuego
            if (id.equals("lombriz") || id.equals("gamba_arrecife")) continue;
            check(recipes.byKey(new ResourceLocation(TooDeepSeas.MODID, "cebos/" + id)).isPresent(), "falta la receta de " + id);
        }
        check(recipes.byKey(new ResourceLocation(TooDeepSeas.MODID, "corcho_abedul")).isPresent(), "falta la receta del corcho");
        int taller = recipes.getAllRecipesFor(ModRecipes.TALLER_TYPE.get()).size();
        check(taller == 29, "recetas de taller cargadas: " + taller);
        check(RodData.validateAdd(RodTier.HIERRO, List.of(), "escama_guardian", 1) != null, "la escama no entra en una caña de hierro");
        check(RodData.validateAdd(RodTier.PRISMARINA, List.of(), "escama_guardian", 1) == null, "la escama entra en la de prismarina");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void taller(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        MesaPescadorMenu menu = new MesaPescadorMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        int base = MesaPescadorMenu.TALLER_START, m1 = base + 1, m2 = base + 2, out = MesaPescadorMenu.TALLER_RESULT;

        // Mejora con un material: plomada I gastada + 8 pepitas de oro = plomada II nueva
        menu.getSlot(base).set(ModItems.accessoryStack("plomada", 1, 0));
        check(!menu.getSlot(out).hasItem() && menu.tallerMode() == MesaPescadorMenu.NADA, "sin materiales no sale nada");
        menu.getSlot(m1).set(new ItemStack(Items.GOLD_NUGGET, 7));
        check(!menu.getSlot(out).hasItem() && menu.tallerMode() == MesaPescadorMenu.FALTA, "con 7 pepitas no llega");
        menu.getSlot(m1).set(new ItemStack(Items.GOLD_NUGGET, 10));
        ItemStack result = menu.getSlot(out).getItem();
        check(isAccessory(result, "plomada", 2) && menu.tallerMode() == MesaPescadorMenu.MEJORA, "sale la plomada II");
        check(((AccessoryItem) result.getItem()).uses(result) == 60, "y sale con todos sus usos");
        check(menu.getSlot(m1).getItem().getCount() == 10, "nada se gasta hasta coger el resultado");
        menu.clicked(out, 0, ClickType.PICKUP, player);
        check(isAccessory(menu.getCarried(), "plomada", 2), "se coge la plomada II");
        check(!menu.getSlot(base).hasItem() && menu.getSlot(m1).getItem().getCount() == 2, "se gastan la plomada I y 8 pepitas");
        check(!menu.getSlot(out).hasItem(), "y no queda resultado");
        menu.setCarried(ItemStack.EMPTY);
        menu.getSlot(m1).set(ItemStack.EMPTY);

        // Mejora con dos materiales, en cualquier orden; el nivel de la base importa
        menu.getSlot(base).set(ModItems.accessoryStack("sedal_reforzado", 1, -1));
        menu.getSlot(m1).set(new ItemStack(Items.STRING, 2));
        check(!menu.getSlot(out).hasItem(), "con un solo material de dos no sale");
        menu.getSlot(m2).set(new ItemStack(Items.IRON_INGOT, 2));
        check(isAccessory(menu.getSlot(out).getItem(), "sedal_reforzado", 2), "cuerda + hierro en orden inverso: sedal II");
        menu.getSlot(base).set(ModItems.accessoryStack("sedal_reforzado", 2, -1));
        check(!menu.getSlot(out).hasItem(), "los materiales del nivel II no suben un sedal II");
        menu.getSlot(m2).set(new ItemStack(Items.DIAMOND, 2));
        check(isAccessory(menu.getSlot(out).getItem(), "sedal_reforzado", 3), "con diamante sale el III");
        menu.getSlot(base).set(ModItems.accessoryStack("sedal_reforzado", 3, -1));
        check(!menu.getSlot(out).hasItem(), "el nivel III no sube más");
        for (int i = base; i <= m2; i++) menu.getSlot(i).set(ItemStack.EMPTY);

        // Reparación: carrete I (600 usos) con 100; cada lingote de cobre da 150 y cuesta 1 nivel
        menu.getSlot(base).set(ModItems.accessoryStack("carrete", 1, 100));
        menu.getSlot(m2).set(new ItemStack(Items.COPPER_INGOT, 3));
        check(!menu.getSlot(out).hasItem() && menu.tallerMode() == MesaPescadorMenu.SIN_XP, "sin niveles no se repara");
        player.giveExperienceLevels(2);
        menu.broadcastChanges();
        result = menu.getSlot(out).getItem();
        check(isAccessory(result, "carrete", 1) && ((AccessoryItem) result.getItem()).uses(result) == 400, "con 2 niveles se reparan 2 unidades: 400 usos");
        check(menu.tallerMode() == MesaPescadorMenu.REPARAR && menu.tallerCost() == 2, "y cuesta 2 niveles");
        menu.clicked(out, 0, ClickType.PICKUP, player);
        check(player.experienceLevel == 0 && menu.getSlot(m2).getItem().getCount() == 1 && !menu.getSlot(base).hasItem(), "se gastan 2 niveles y 2 lingotes");
        check(((AccessoryItem) menu.getCarried().getItem()).uses(menu.getCarried()) == 400, "el carrete reparado tiene 400 usos");
        player.giveExperienceLevels(10);
        menu.getSlot(base).set(menu.getCarried());
        menu.setCarried(ItemStack.EMPTY);
        menu.getSlot(m2).set(new ItemStack(Items.COPPER_INGOT, 9));
        result = menu.getSlot(out).getItem();
        check(menu.tallerCost() == 2 && (result.getTag() == null || !result.getTag().contains(AccessoryItem.USES)), "solo se gasta lo que falta y queda como nuevo");
        menu.getSlot(base).set(ModItems.accessoryStack("carrete", 1, -1));
        check(!menu.getSlot(out).hasItem() && menu.tallerMode() == MesaPescadorMenu.NADA, "un accesorio entero no se repara");
        menu.getSlot(m2).set(new ItemStack(Items.STRING, 9));
        menu.getSlot(base).set(ModItems.accessoryStack("carrete", 1, 100));
        check(!menu.getSlot(out).hasItem(), "con otro material no se repara");
        for (int i = base; i <= m2; i++) menu.getSlot(i).set(ItemStack.EMPTY);

        // Caña del vacío: netherita + 2 conchas de shulker + 4 fragmentos; conserva el equipo y sale entera
        ItemStack rod = new ItemStack(ModItems.CANA_NETHERITA.get());
        check(RodData.tryAdd(rod, "plomada", 3) == null && RodData.addBait(rod, "lombriz", 5) == 5, "equipar la caña de netherita");
        rod.setDamageValue(500);
        menu.getSlot(base).set(rod);
        menu.getSlot(m1).set(new ItemStack(ModItems.FRAGMENTO_VACIO.get(), 4));
        menu.getSlot(m2).set(new ItemStack(Items.SHULKER_SHELL, 1));
        check(!menu.getSlot(out).hasItem(), "con 1 concha no sale");
        menu.getSlot(m2).set(new ItemStack(Items.SHULKER_SHELL, 2));
        result = menu.getSlot(out).getItem();
        check(result.is(ModItems.CANA_VACIO.get()) && result.getDamageValue() == 0, "sale la caña del vacío, entera");
        check(RodData.slots(result).size() == 1 && RodData.baits(result).get(0).count() == 5, "con el equipo de la anterior");
        menu.quickMoveStack(player, out);
        check(player.getInventory().countItem(ModItems.CANA_VACIO.get()) == 1, "Mayús + clic la lleva al inventario");
        check(!menu.getSlot(base).hasItem() && !menu.getSlot(m1).hasItem() && !menu.getSlot(m2).hasItem(), "y gasta la caña y los materiales");
        check(!menu.getSlot(out).hasItem(), "sin dejar otro resultado");

        // Mayús + clic sin caña en la mesa: el accesorio va al taller y el material, a su casilla
        player.getInventory().clearContent();
        player.getInventory().setItem(0, ModItems.accessoryStack("anzuelo_afilado", 1, -1));
        player.getInventory().setItem(1, new ItemStack(Items.IRON_INGOT, 2));
        menu.quickMoveStack(player, MesaPescadorMenu.INV_START + 27);
        menu.quickMoveStack(player, MesaPescadorMenu.INV_START + 28);
        check(isAccessory(menu.getSlot(out).getItem(), "anzuelo_afilado", 2), "anzuelo + 2 lingotes con Mayús + clic: anzuelo II");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void botin(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO));
        Item fragmento = ModItems.FRAGMENTO_VACIO.get(), lombriz = ModItems.BAITS.get("lombriz").get();

        LootTable city = level.getServer().getLootData().getLootTable(new ResourceLocation("minecraft", "chests/end_city_treasure"));
        LootParams chest = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin).create(LootContextParamSets.CHEST);
        int withShard = 0;
        for (int i = 0; i < 400; i++) {
            if (city.getRandomItems(chest).stream().anyMatch(s -> s.is(fragmento))) withShard++;
        }
        check(withShard > 140 && withShard < 260, "cofres de ciudad del End con fragmento (de 400): " + withShard);

        LootTable dirt = level.getServer().getLootData().getLootTable(new ResourceLocation("minecraft", "blocks/dirt"));
        int worms = 0, wormsPick = 0;
        for (Item tool : new Item[]{Items.IRON_SHOVEL, Items.IRON_PICKAXE}) {
            LootParams dig = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
                    .withParameter(LootContextParams.BLOCK_STATE, Blocks.DIRT.defaultBlockState())
                    .withParameter(LootContextParams.TOOL, new ItemStack(tool)).create(LootContextParamSets.BLOCK);
            for (int i = 0; i < 1000; i++) {
                int n = (int) dirt.getRandomItems(dig).stream().filter(s -> s.is(lombriz)).count();
                if (tool == Items.IRON_SHOVEL) worms += n;
                else wormsPick += n;
            }
        }
        check(worms > 40 && worms < 130, "lombrices en 1000 bloques de tierra con pala: " + worms);
        check(wormsPick == 0, "sin pala no salen lombrices: " + wormsPick);

        Player player = helper.makeMockPlayer();
        Shulker shulker = EntityType.SHULKER.create(level);
        LootTable shulkerTable = level.getServer().getLootData().getLootTable(new ResourceLocation("minecraft", "entities/shulker"));
        int byPlayer = 0, alone = 0;
        for (boolean killed : new boolean[]{true, false}) {
            LootParams.Builder b = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
                    .withParameter(LootContextParams.THIS_ENTITY, shulker)
                    .withParameter(LootContextParams.DAMAGE_SOURCE, killed ? level.damageSources().playerAttack(player) : level.damageSources().generic());
            if (killed) b.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player);
            LootParams params = b.create(LootContextParamSets.ENTITY);
            for (int i = 0; i < 500; i++) {
                int n = (int) shulkerTable.getRandomItems(params).stream().filter(s -> s.is(fragmento)).count();
                if (killed) byPlayer += n;
                else alone += n;
            }
        }
        check(byPlayer > 60 && byPlayer < 145, "fragmentos en 500 shulkers matados por un jugador: " + byPlayer);
        check(alone == 0, "si no lo mata un jugador no suelta fragmento: " + alone);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cofres(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO));
        Player player = helper.makeMockPlayer();

        // Todas las tablas existen (una tabla con un objeto mal escrito no carga y queda vacía)
        for (String name : Cofres.todasLasTablas()) {
            LootTable table = level.getServer().getLootData().getLootTable(new ResourceLocation(TooDeepSeas.MODID, name));
            check(table != LootTable.EMPTY, "falta la tabla de botín " + name);
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin).create(LootContextParamSets.CHEST);
            for (int i = 0; i < 40; i++) check(!table.getRandomItems(params).isEmpty(), "la tabla " + name + " no da nada");
        }

        // Cualquier combinación de bioma, calidad, profundidad y clima da botín, y mejor cuanto mejor es el cofre
        for (Cofres.Bioma bioma : Cofres.Bioma.values()) {
            CofreItem item = ModItems.COFRES.get(bioma).get();
            int worst = 0, best = 0;
            for (Cofres.Calidad calidad : Cofres.Calidad.values()) for (Cofres.Banda banda : Cofres.Banda.values()) for (Cofres.Clima clima : Cofres.Clima.values()) {
                ItemStack stack = item.create(calidad, banda, clima);
                check(CofreItem.calidad(stack) == calidad && CofreItem.banda(stack) == banda, "el cofre no guarda calidad o banda: " + stack.getTag());
                check(item.clima(stack) == Cofres.climaEfectivo(bioma, clima), "el cofre no guarda el clima: " + stack.getTag());
                int total = 0;
                for (int i = 0; i < 5; i++) {
                    List<ItemStack> loot = item.roll(level, stack, origin, player);
                    check(!loot.isEmpty(), "cofre vacío: " + bioma + " " + calidad + " " + banda + " " + clima);
                    for (ItemStack s : loot) {
                        check(!s.isEmpty() && s.getCount() <= s.getMaxStackSize(), "montón imposible en el botín: " + s);
                        total += s.getCount();
                    }
                }
                if (calidad == Cofres.Calidad.SENCILLO && banda == Cofres.Banda.SUPERFICIE && clima == Cofres.Clima.DESPEJADO) worst = total;
                if (calidad == Cofres.Calidad.ANCESTRAL && banda == Cofres.Banda.ABISAL && clima == Cofres.Clima.TORMENTA) best = total;
            }
            check(best > worst, "el mejor cofre de " + bioma + " no da más que el peor: " + best + " <= " + worst);
        }
        // Sin NBT es el cofre más sencillo, y dos iguales se apilan
        ItemStack plain = new ItemStack(ModItems.COFRES.get(Cofres.Bioma.RIO).get());
        check(ItemStack.isSameItemSameTags(plain, ModItems.COFRES.get(Cofres.Bioma.RIO).get().create(Cofres.Calidad.SENCILLO, Cofres.Banda.SUPERFICIE, Cofres.Clima.DESPEJADO)), "el cofre sencillo lleva NBT de más");

        // Los accesorios del cofre ancestral pueden salir de nivel III
        LootTable ancestral = level.getServer().getLootData().getLootTable(new ResourceLocation(TooDeepSeas.MODID, "cofres/calidad/ancestral"));
        LootParams chest = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin).create(LootContextParamSets.CHEST);
        boolean level3 = false;
        for (int i = 0; i < 400 && !level3; i++) {
            level3 = ancestral.getRandomItems(chest).stream().anyMatch(s -> s.getItem() instanceof AccessoryItem && AccessoryItem.level(s) == 3);
        }
        check(level3, "en 400 cofres ancestrales no ha salido ningún accesorio de nivel III");

        // Abrir uno: gasta un cofre del montón y el botín va al inventario
        CofreItem rio = ModItems.COFRES.get(Cofres.Bioma.RIO).get();
        ItemStack two = rio.create(Cofres.Calidad.REFORZADO, Cofres.Banda.PROFUNDO, Cofres.Clima.LLUVIA);
        two.setCount(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, two);
        rio.use(level, player, InteractionHand.MAIN_HAND);
        check(player.getMainHandItem().getCount() == 1 && player.getMainHandItem().getItem() == rio, "abrir no gasta un cofre: " + player.getMainHandItem());
        int others = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() != rio) others++;
        }
        check(others >= 1, "el botín no ha llegado al inventario");

        // Pesca vanilla (provisional): 1 de cada 10 capturas es un cofre de la calidad de la caña, en lugar del pez
        LootTable fishing = level.getServer().getLootData().getLootTable(new ResourceLocation("minecraft", "gameplay/fishing"));
        LootParams cast = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.TOOL, new ItemStack(ModItems.CANA_DIAMANTE.get())).create(LootContextParamSets.FISHING);
        int chests = 0;
        for (int i = 0; i < 1000; i++) {
            List<ItemStack> loot = fishing.getRandomItems(cast);
            if (loot.stream().anyMatch(s -> s.getItem() instanceof CofreItem)) {
                chests++;
                check(loot.size() == 1, "el cofre no sustituye a la captura: " + loot);
                check(CofreItem.calidad(loot.get(0)) == Cofres.Calidad.ORNAMENTADO && CofreItem.banda(loot.get(0)) == Cofres.Banda.SUPERFICIE,
                        "cofre pescado con caña de diamante: " + loot.get(0).getTag());
            }
        }
        check(chests > 60 && chests < 145, "cofres en 1000 capturas: " + chests);

        // Biomas de Minecraft → biomas de pesca
        Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        check(Biomas.bioma(biomes.getHolderOrThrow(Biomes.WARM_OCEAN)) == Cofres.Bioma.CALIDO, "océano cálido");
        check(Biomas.bioma(biomes.getHolderOrThrow(Biomes.DEEP_FROZEN_OCEAN)) == Cofres.Bioma.HELADO, "océano helado");
        check(Biomas.bioma(biomes.getHolderOrThrow(Biomes.FROZEN_RIVER)) == Cofres.Bioma.HELADO, "río helado");
        check(Biomas.bioma(biomes.getHolderOrThrow(Biomes.DEEP_OCEAN)) == Cofres.Bioma.OCEANO, "océano");
        check(Biomas.bioma(biomes.getHolderOrThrow(Biomes.MANGROVE_SWAMP)) == Cofres.Bioma.PANTANO, "manglar");
        check(Biomas.bioma(biomes.getHolderOrThrow(Biomes.RIVER)) == Cofres.Bioma.RIO, "río");
        check(Biomas.bioma(biomes.getHolderOrThrow(Biomes.PLAINS)) == Cofres.Bioma.RIO, "llanura (agua dulce)");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aldeas(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState mesa = ModBlocks.MESA_PESCADOR.get().defaultBlockState();
        BlockState barrel = Blocks.BARREL.defaultBlockState();
        Holder<PoiType> fisherman = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolderOrThrow(PoiTypes.FISHERMAN);

        // La mesa es el puesto del pescador y el barril ya no (barrilLegado = false por defecto)
        check(PoiTypes.forState(mesa).filter(h -> h.is(PoiTypes.FISHERMAN)).isPresent(), "la mesa no es el puesto del pescador");
        check(fisherman.value().is(mesa), "el puesto del pescador no reconoce la mesa");
        check(VillagerProfession.FISHERMAN.heldJobSite().test(PoiTypes.forState(mesa).get()), "la profesión de pescador no acepta la mesa");
        check(PoiTypes.forState(barrel).isEmpty() && !fisherman.value().is(barrel), "el barril sigue siendo un puesto de trabajo");
        check(PoiTypes.forState(Blocks.COMPOSTER.defaultBlockState()).isPresent(), "se han perdido otros puestos de trabajo");

        // Las casas de pescador, y solo ellas, llevan el procesador que cambia el barril por la mesa
        Registry<StructureTemplatePool> pools = level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        int fisherHouses = 0;
        StructurePoolElement house = null;
        for (String biome : new String[]{"plains", "desert", "savanna", "snowy", "taiga"}) {
            StructureTemplatePool pool = pools.get(new ResourceLocation("village/" + biome + "/houses"));
            check(pool != null, "no existe el grupo de casas de " + biome);
            int here = 0;
            for (StructurePoolElement element : ((StructureTemplatePoolAccessor) (Object) pool).toodeepseas$templates()) {
                if (Aldeas.esCasaDePescador(element)) {
                    here++;
                    check(Aldeas.llevaMesa(element), "casa de pescador sin mesa en " + biome + ": " + element);
                    if (house == null) house = element;
                } else {
                    check(!Aldeas.llevaMesa(element), "una casa que no es de pescador lleva la mesa: " + element);
                }
            }
            check(here > 0, "no hay casas de pescador en la aldea de " + biome);
            fisherHouses += here;
        }
        check(fisherHouses >= 5, "casas de pescador: " + fisherHouses);

        // Generar una de verdad: sale con mesa, sin barril, y el juego la registra como puesto de pescador
        BlockPos at = helper.absolutePos(new BlockPos(0, 6, 0));
        BoundingBox box = house.getBoundingBox(level.getStructureManager(), at, Rotation.NONE);
        house.place(level.getStructureManager(), level, level.structureManager(), level.getChunkSource().getGenerator(), at, at, Rotation.NONE, box, level.getRandom(), false);
        List<BlockPos> mesas = BlockPos.betweenClosedStream(box).filter(p -> level.getBlockState(p).is(ModBlocks.MESA_PESCADOR.get())).map(BlockPos::immutable).toList();
        long barrels = BlockPos.betweenClosedStream(box).filter(p -> level.getBlockState(p).is(Blocks.BARREL)).count();
        check(!mesas.isEmpty(), "la casa de pescador generada no tiene mesa: " + house);
        check(barrels == 0, "la casa de pescador generada conserva barriles: " + barrels);

        BlockPos barrelPos = helper.absolutePos(new BlockPos(1, 0, 0));
        level.setBlockAndUpdate(barrelPos, barrel);
        // El registro de puestos se actualiza en el siguiente tick del servidor
        helper.runAfterDelay(3, () -> {
            for (BlockPos p : mesas) {
                check(level.getPoiManager().getType(p).filter(h -> h.is(PoiTypes.FISHERMAN)).isPresent(), "la mesa de la casa no cuenta como puesto en " + p);
            }
            check(level.getPoiManager().getType(barrelPos).isEmpty(), "un barril nuevo cuenta como puesto de trabajo");
            BlockPos.betweenClosedStream(box).forEach(p -> level.setBlock(p, Blocks.AIR.defaultBlockState(), 2));
            level.setBlockAndUpdate(barrelPos, Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void atajoDeInventario(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        AbstractContainerMenu inv = player.containerMenu;
        int first = 36; // primera casilla de la barra rápida en el inventario del jugador

        // Caña vanilla: tier 0, 1 ranura, accesorios de nivel I
        player.getInventory().setItem(0, new ItemStack(Items.FISHING_ROD));
        ItemStack rod = player.getInventory().getItem(0);

        inv.setCarried(ModItems.accessoryStack("anzuelo_afilado", 1, -1));
        inv.clicked(first, 1, ClickType.PICKUP, player);
        check(inv.getCarried().isEmpty() && RodData.slots(rod).size() == 1, "clic derecho mete el accesorio en la caña");

        inv.setCarried(ModItems.accessoryStack("carrete", 1, -1));
        inv.clicked(first, 1, ClickType.PICKUP, player);
        check(isAccessory(inv.getCarried(), "carrete", 1) && RodData.slots(rod).size() == 1, "sin ranura libre no entra y no se intercambia");
        check(player.getInventory().getItem(0) == rod, "la caña sigue en su casilla");

        inv.setCarried(ModItems.baitStack("lombriz", 20));
        inv.clicked(first, 1, ClickType.PICKUP, player);
        check(inv.getCarried().isEmpty() && RodData.baits(rod).get(0).count() == 20, "clic derecho mete el cebo");

        inv.clicked(first, 0, ClickType.PICKUP, player);
        check(ModItems.tierOf(inv.getCarried()) != null, "se coge la caña");
        inv.clicked(first + 1, 1, ClickType.PICKUP, player);
        check(isAccessory(player.getInventory().getItem(1), "anzuelo_afilado", 1) && RodData.slots(inv.getCarried()).isEmpty(), "clic derecho en casilla vacía saca el accesorio");
        inv.clicked(first + 2, 1, ClickType.PICKUP, player);
        check(player.getInventory().getItem(2).getCount() == 20 && RodData.baits(inv.getCarried()).isEmpty(), "después saca el cebo");
        check(inv.getCarried().getTagElement(RodData.ROOT) == null, "la caña vacía queda sin datos de equipo, como recién fabricada");
        inv.clicked(first + 3, 1, ClickType.PICKUP, player);
        check(inv.getCarried().isEmpty() && ModItems.tierOf(player.getInventory().getItem(3)) != null, "con la caña vacía, el clic derecho la deja en la casilla");
        helper.succeed();
    }
}

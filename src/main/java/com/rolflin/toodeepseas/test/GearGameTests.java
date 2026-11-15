package com.rolflin.toodeepseas.test;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.data.Defs;
import com.rolflin.toodeepseas.data.RodData;
import com.rolflin.toodeepseas.item.AccessoryItem;
import com.rolflin.toodeepseas.item.BaitItem;
import com.rolflin.toodeepseas.item.ModItems;
import com.rolflin.toodeepseas.menu.MesaPescadorMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

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
        for (String recipe : new String[]{"mesa_pescador", "cana_cobre", "cana_hierro", "cana_oro", "cana_prismarina", "cana_diamante", "cana_netherita"}) {
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

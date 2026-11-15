package com.rolflin.toodeepseas.menu;

import com.rolflin.toodeepseas.block.ModBlocks;
import com.rolflin.toodeepseas.core.Repair;
import com.rolflin.toodeepseas.core.RodTier;
import com.rolflin.toodeepseas.data.RodData;
import com.rolflin.toodeepseas.item.AccessoryItem;
import com.rolflin.toodeepseas.item.BaitItem;
import com.rolflin.toodeepseas.item.ModItems;
import com.rolflin.toodeepseas.recipe.ModRecipes;
import com.rolflin.toodeepseas.recipe.TallerRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ventana de la mesa del pescador: la caña en el centro y, alrededor, sus accesorios y su cebo.
 *
 * <p>Las ranuras de equipo son una <b>vista</b> del NBT de la caña: al dejar la caña se rellenan con lo que lleva,
 * cada cambio se escribe en la caña al momento y, al retirarla, se vacían sin soltar nada (todo viaja dentro de ella).
 * Toda esa lógica corre solo en el servidor; el cliente recibe las ranuras por la sincronización normal.</p>
 *
 * <p>Debajo está el <b>taller</b>: un accesorio (o una caña) más materiales. Con los materiales de una receta de taller
 * sale el nivel siguiente, nuevo; con el material de reparación del accesorio, recupera un 25 % de usos por unidad
 * a cambio de 1 nivel de experiencia cada una.</p>
 */
public class MesaPescadorMenu extends AbstractContainerMenu {
    public static final int ROD = 0;
    public static final int ACC_START = 1;
    public static final int ACC_COUNT = 6;
    public static final int BAIT_START = ACC_START + ACC_COUNT;
    public static final int BAIT_COUNT = 2;
    public static final int GEAR_END = BAIT_START + BAIT_COUNT; // exclusivo
    /** Taller: objeto base y 2 materiales; después, el resultado. */
    public static final int TALLER_START = GEAR_END;
    public static final int TALLER_RESULT = TALLER_START + 3;
    public static final int INV_START = TALLER_RESULT + 1;
    public static final int INV_END = INV_START + 36; // exclusivo

    /** Tamaño de la ventana y altura a la que empieza el inventario del jugador. */
    public static final int WIDTH = 176, HEIGHT = 220, INV_Y = 138;
    public static final int ROD_X = 26, ROD_Y = 44;
    /** Posiciones de las 6 ranuras de accesorio (arriba y abajo de la caña). */
    public static final int[][] ACC_POS = {{8, 26}, {26, 26}, {44, 26}, {8, 62}, {26, 62}, {44, 62}};
    /** Posiciones de las 2 ranuras de cebo (a los lados de la caña). */
    public static final int[][] BAIT_POS = {{8, 44}, {44, 44}};
    /** Fila del taller: base, 2 materiales y resultado. */
    public static final int TALLER_Y = 106;
    public static final int[] TALLER_X = {8, 30, 48};
    public static final int TALLER_RESULT_X = 88;

    /** Qué ofrece ahora mismo el taller (se envía al cliente para el texto de la ventana). */
    public static final int NADA = 0, MEJORA = 1, REPARAR = 2, SIN_XP = 3, FALTA = 4;

    private final ContainerLevelAccess access;
    private final Player player;
    private final boolean server;
    private final SimpleContainer rodInv = new Watched(1);
    /** 0–5 accesorios, 6–7 cebos. */
    private final SimpleContainer gear = new Watched(ACC_COUNT + BAIT_COUNT);

    /** Caña cuyo equipo está ahora mismo desplegado en las ranuras (solo servidor). */
    private ItemStack loadedRod = ItemStack.EMPTY;
    private boolean loaded;
    /** Accesorios de la caña que no tienen objeto (de otro datapack): se conservan tal cual. */
    private final List<RodData.Slot> hidden = new ArrayList<>();
    private final List<RodData.Bait> hiddenBaits = new ArrayList<>();
    private boolean updating;

    private final SimpleContainer taller = new Watched(3);
    private final ResultContainer tallerOut = new ResultContainer();
    private final DataSlot tallerMode = DataSlot.standalone();
    private final DataSlot tallerCost = DataSlot.standalone();
    /** Lo que se gastará al coger el resultado (solo servidor). */
    @Nullable
    private TallerRecipe pendingRecipe;
    private int repairUnits, repairSlot;
    private int seenLevels = -1;

    /** Cliente. */
    public MesaPescadorMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public MesaPescadorMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.MESA_PESCADOR.get(), id);
        this.access = access;
        this.player = inventory.player;
        this.server = !inventory.player.level().isClientSide;

        addSlot(new Slot(rodInv, 0, ROD_X, ROD_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // Sin intercambio directo de cañas: hay que retirar la que está puesta
                return !hasItem() && ModItems.tierOf(stack) != null;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < ACC_COUNT; i++) addSlot(new AccessorySlot(i, ACC_POS[i][0], ACC_POS[i][1]));
        for (int i = 0; i < BAIT_COUNT; i++) addSlot(new BaitSlot(i, BAIT_POS[i][0], BAIT_POS[i][1]));

        addSlot(new Slot(taller, 0, TALLER_X[0], TALLER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof AccessoryItem || ModItems.tierOf(stack) != null;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 1; i <= 2; i++) addSlot(new Slot(taller, i, TALLER_X[i], TALLER_Y));
        addSlot(new Slot(tallerOut, 0, TALLER_RESULT_X, TALLER_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player who, ItemStack stack) {
                takeResult(who);
                super.onTake(who, stack);
            }
        });
        addDataSlot(tallerMode);
        addDataSlot(tallerCost);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, INV_Y + 58));
    }

    /** Caña puesta en la mesa (vacío si no hay). */
    public ItemStack rod() {
        return rodInv.getItem(0);
    }

    @Nullable
    public RodTier tier() {
        return ModItems.tierOf(rod());
    }

    /** Qué ofrece el taller: {@link #NADA}, {@link #MEJORA}, {@link #REPARAR}, {@link #SIN_XP} o {@link #FALTA}. */
    public int tallerMode() {
        return tallerMode.get();
    }

    /** Niveles de experiencia que cuesta la reparación que se ofrece. */
    public int tallerCost() {
        return tallerCost.get();
    }

    // ---------- Vista del equipo (servidor) ----------

    @Override
    public void slotsChanged(Container container) {
        if (!server || updating) return;
        updating = true;
        try {
            if (container == taller) {
                updateTaller();
                return;
            }
            ItemStack rod = rodInv.getItem(0);
            if (!loaded || rod.isEmpty() || rod != loadedRod) {
                // La caña desplegada ya no está: su equipo se fue con ella en el NBT.
                // (Al retirarla, su casilla conserva el mismo objeto con cantidad 0: por eso hace falta «loaded».)
                if (loaded) gear.clearContent();
                hidden.clear();
                hiddenBaits.clear();
                loaded = false;
                loadedRod = ItemStack.EMPTY;
                if (!rod.isEmpty() && ModItems.tierOf(rod) != null) load(rod);
            } else if (container == gear) {
                save(rod);
            }
        } finally {
            updating = false;
        }
    }

    private void load(ItemStack rod) {
        RodTier tier = ModItems.tierOf(rod);
        int next = 0;
        for (RodData.Slot s : RodData.slots(rod)) {
            ItemStack stack = ModItems.accessoryStack(s.id(), s.level(), s.uses());
            if (stack.isEmpty() || next >= Math.min(ACC_COUNT, tier.accessorySlots)) hidden.add(s);
            else gear.setItem(next++, stack);
        }
        next = 0;
        List<RodData.Bait> rest = new ArrayList<>();
        for (RodData.Bait b : RodData.baits(rod)) {
            ItemStack stack = ModItems.baitStack(b.id(), b.count());
            if (stack.isEmpty() || next >= Math.min(BAIT_COUNT, tier.baitSlots)) rest.add(b);
            else gear.setItem(ACC_COUNT + next++, stack);
        }
        hiddenBaits.addAll(rest);
        loadedRod = rod;
        loaded = true;
    }


    private void save(ItemStack rod) {
        List<RodData.Slot> slots = new ArrayList<>();
        for (int i = 0; i < ACC_COUNT; i++) {
            ItemStack stack = gear.getItem(i);
            if (stack.getItem() instanceof AccessoryItem acc) slots.add(new RodData.Slot(acc.id(), AccessoryItem.level(stack), acc.uses(stack)));
        }
        slots.addAll(hidden);
        List<RodData.Bait> baits = new ArrayList<>();
        for (int i = 0; i < BAIT_COUNT; i++) {
            ItemStack stack = gear.getItem(ACC_COUNT + i);
            if (stack.getItem() instanceof BaitItem bait) baits.add(new RodData.Bait(bait.id(), stack.getCount()));
        }
        baits.addAll(hiddenBaits);
        RodData.setSlots(rod, slots);
        RodData.setBaits(rod, baits);
    }

    /** Accesorios que hay ahora en las ranuras, sin contar la indicada (para validar lo que se quiere meter en ella). */
    private List<RodData.Slot> othersThan(int index) {
        List<RodData.Slot> out = new ArrayList<>();
        for (int i = 0; i < ACC_COUNT; i++) {
            ItemStack stack = gear.getItem(i);
            if (i != index && stack.getItem() instanceof AccessoryItem acc) out.add(new RodData.Slot(acc.id(), AccessoryItem.level(stack), 1));
        }
        out.addAll(hidden);
        return out;
    }

    // ---------- Taller: mejorar y reparar (servidor) ----------

    /** Calcula qué saldría con lo que hay puesto. No gasta nada: eso pasa al coger el resultado. */
    private void updateTaller() {
        ItemStack base = taller.getItem(0);
        ItemStack out = ItemStack.EMPTY;
        int mode = NADA, cost = 0;
        pendingRecipe = null;
        repairUnits = 0;
        seenLevels = player.experienceLevel;
        if (!base.isEmpty()) {
            Level level = player.level();
            TallerRecipe recipe = level.getRecipeManager().getRecipeFor(ModRecipes.TALLER_TYPE.get(), taller, level).orElse(null);
            if (recipe != null) {
                out = recipe.assemble(taller, level.registryAccess());
                pendingRecipe = recipe;
                mode = MEJORA;
            } else {
                mode = taller.getItem(1).isEmpty() && taller.getItem(2).isEmpty() ? NADA : FALTA;
                int slot = taller.getItem(2).isEmpty() ? 1 : taller.getItem(1).isEmpty() ? 2 : 0;
                if (slot != 0 && base.getItem() instanceof AccessoryItem acc && acc.repairItem() != null && taller.getItem(slot).is(acc.repairItem())) {
                    int max = acc.maxUses(base), uses = acc.uses(base);
                    boolean free = player.getAbilities().instabuild;
                    int units = Repair.units(max, uses, taller.getItem(slot).getCount(), free ? Integer.MAX_VALUE : player.experienceLevel);
                    if (units > 0) {
                        out = base.copy();
                        acc.setUses(out, Repair.usesAfter(max, uses, units));
                        repairUnits = units;
                        repairSlot = slot;
                        mode = REPARAR;
                        cost = free ? 0 : units;
                    } else if (uses < max) {
                        mode = SIN_XP;
                        cost = 1;
                    } else {
                        mode = NADA;
                    }
                }
            }
        }
        tallerOut.setItem(0, out);
        tallerMode.set(mode);
        tallerCost.set(cost);
    }

    /** Al coger el resultado: se gastan la base, los materiales y, si es una reparación, los niveles. */
    private void takeResult(Player who) {
        if (!server) return;
        updating = true;
        try {
            if (pendingRecipe != null) {
                pendingRecipe.consume(taller);
            } else if (repairUnits > 0) {
                taller.removeItem(repairSlot, repairUnits);
                taller.setItem(0, ItemStack.EMPTY);
                if (!who.getAbilities().instabuild) who.giveExperienceLevels(-repairUnits);
            }
            updateTaller();
        } finally {
            updating = false;
        }
    }

    @Override
    public void broadcastChanges() {
        // Si el jugador gana o pierde niveles con la ventana abierta, cambia lo que puede reparar
        if (server && !updating && seenLevels != player.experienceLevel && !taller.getItem(0).isEmpty()) {
            updating = true;
            try {
                updateTaller();
            } finally {
                updating = false;
            }
        }
        super.broadcastChanges();
    }

    // ---------- Ranuras ----------

    private class AccessorySlot extends Slot {
        private final int n;

        AccessorySlot(int n, int x, int y) {
            super(gear, n, x, y);
            this.n = n;
        }

        @Override
        public boolean isActive() {
            RodTier tier = tier();
            return tier != null && n < tier.accessorySlots;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            RodTier tier = tier();
            if (tier == null || n >= tier.accessorySlots || !(stack.getItem() instanceof AccessoryItem acc)) return false;
            return RodData.validateAdd(tier, othersThan(n), acc.id(), AccessoryItem.level(stack)) == null;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private class BaitSlot extends Slot {
        private final int n;

        BaitSlot(int n, int x, int y) {
            super(gear, ACC_COUNT + n, x, y);
            this.n = n;
        }

        @Override
        public boolean isActive() {
            RodTier tier = tier();
            return tier != null && n < tier.baitSlots;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            RodTier tier = tier();
            if (tier == null || n >= tier.baitSlots || !(stack.getItem() instanceof BaitItem bait)) return false;
            // Dos cebos a la vez (prismarina) tienen que ser distintos
            for (int i = 0; i < BAIT_COUNT; i++) {
                if (i != n && gear.getItem(ACC_COUNT + i).getItem() instanceof BaitItem other && other.id().equals(bait.id())) return false;
            }
            return true;
        }
    }

    /** Inventario que avisa al menú de cada cambio. */
    private class Watched extends SimpleContainer {
        Watched(int size) {
            super(size);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    }

    // ---------- Mayús + clic, cierre ----------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < INV_START) {
                if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
            } else if (ModItems.tierOf(stack) != null) {
                // La caña va a su sitio; si ya hay una puesta, al taller (para mejorarla)
                if (!moveItemStackTo(stack, ROD, ROD + 1, false) && !moveItemStackTo(stack, TALLER_START, TALLER_START + 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof AccessoryItem) {
                // Con caña puesta, el accesorio se equipa; si no cabe o no hay caña, va al taller
                if (!moveItemStackTo(stack, ACC_START, ACC_START + ACC_COUNT, false) && !moveItemStackTo(stack, TALLER_START, TALLER_START + 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof BaitItem && moveToBait(stack)) {
                // equipado como cebo
            } else if (!moveItemStackTo(stack, TALLER_START + 1, TALLER_START + 3, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }

    /** El cebo va primero a la ranura que ya tiene ese mismo cebo y, si no, a una libre que lo admita. */
    private boolean moveToBait(ItemStack stack) {
        boolean moved = false;
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int i = BAIT_START; i < BAIT_START + BAIT_COUNT && !stack.isEmpty(); i++) {
                Slot slot = slots.get(i);
                if (!slot.isActive()) continue;
                ItemStack in = slot.getItem();
                if (pass == 0 && !in.isEmpty() && ItemStack.isSameItemSameTags(in, stack)) {
                    int add = Math.min(stack.getCount(), Math.min(slot.getMaxStackSize(in), in.getMaxStackSize()) - in.getCount());
                    if (add > 0) {
                        in.grow(add);
                        stack.shrink(add);
                        slot.setChanged();
                        moved = true;
                    }
                } else if (pass == 1 && in.isEmpty() && slot.mayPlace(stack)) {
                    slot.setByPlayer(stack.split(Math.min(stack.getCount(), slot.getMaxStackSize(stack))));
                    moved = true;
                }
            }
        }
        return moved;
    }

    /** El doble clic no recoge del resultado del taller. */
    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != tallerOut && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.MESA_PESCADOR.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!server) return;
        // Se devuelven la caña y lo que quede en el taller; las ranuras de equipo son una vista del NBT de la caña
        updating = true;
        gear.clearContent();
        clearContainer(player, rodInv);
        clearContainer(player, taller);
        tallerOut.clearContent();
        loadedRod = ItemStack.EMPTY;
        loaded = false;
        updating = false;
    }
}

package com.rolflin.toodeepseas.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.Cofres.Clima;
import com.rolflin.toodeepseas.core.RodTier;
import com.rolflin.toodeepseas.core.juego.Catalogo;
import com.rolflin.toodeepseas.core.juego.Perfil;
import com.rolflin.toodeepseas.core.juego.PezDef;
import com.rolflin.toodeepseas.data.Defs;
import com.rolflin.toodeepseas.data.RodData;
import com.rolflin.toodeepseas.item.ModItems;
import com.rolflin.toodeepseas.world.Biomas;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Provisional hasta que exista el minijuego en Java: al pescar a la manera vanilla, el pez que sale es uno de los del
 * mod que viven en ese bioma y con ese clima (hasta la profundidad del sedal de la caña y contando los cebos que lleva),
 * y la mitad de los tesoros vanilla se cambian por un tesoro del mod de ese sitio.
 */
public class CapturaPescaModifier extends LootModifier {
    public static final Supplier<Codec<CapturaPescaModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, CapturaPescaModifier::new)));

    public static final RegistryObject<Codec<CapturaPescaModifier>> CAPTURA_PESCA = AddItemModifier.SERIALIZERS.register("captura_pesca", CODEC);

    /** Peso de cada rareza (común … legendario). */
    public static final int[] PESOS = {60, 30, 14, 5, 1};
    public static final double PROB_TESORO = 0.5;

    public CapturaPescaModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null) return generatedLoot;
        BlockPos pos = BlockPos.containing(origin);
        Bioma bioma = Biomas.bioma(context.getLevel(), pos);
        Clima clima = Biomas.clima(context.getLevel(), pos);
        Perfil.SubBioma sub = Biomas.subBioma(context.getLevel(), pos);
        ItemStack rod = context.getParamOrNull(LootContextParams.TOOL);
        RodTier tier = rod == null ? null : ModItems.tierOf(rod);
        int profundidad = (tier == null ? RodTier.MADERA : tier).lineDepth;
        Set<String> cebos = new HashSet<>();
        if (rod != null) for (RodData.Bait bait : RodData.baits(rod)) cebos.add(bait.id());
        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack stack = generatedLoot.get(i);
            if (stack.is(ItemTags.FISHES)) {
                ItemStack pez = pez(bioma, clima, sub, cebos, profundidad, context.getRandom());
                if (!pez.isEmpty()) generatedLoot.set(i, pez);
            } else if (esTesoroVanilla(stack) && context.getRandom().nextDouble() < PROB_TESORO) {
                ItemStack tesoro = tesoro(bioma, clima, profundidad, context.getRandom());
                if (!tesoro.isEmpty()) generatedLoot.set(i, tesoro);
            }
        }
        return generatedLoot;
    }

    private static boolean esTesoroVanilla(ItemStack stack) {
        return stack.is(Items.NAME_TAG) || stack.is(Items.SADDLE) || stack.is(Items.NAUTILUS_SHELL) || stack.is(Items.ENCHANTED_BOOK)
                || stack.is(Items.BOW) || (stack.is(Items.FISHING_ROD) && stack.isEnchanted());
    }

    /** Un pez de los que pueden salir ahí, al azar según su rareza; vacío si no hay ninguno. */
    public static ItemStack pez(Bioma bioma, Clima clima, Perfil.SubBioma sub, Set<String> cebos, double profundidad, RandomSource random) {
        List<PezDef> options = new ArrayList<>();
        int total = 0;
        for (String id : Defs.PECES.ids()) {
            PezDef def = Defs.PECES.get(id);
            if (def == null || !def.aparece(bioma, clima, sub, cebos, profundidad) || ModItems.pezStack(id).isEmpty()) continue;
            options.add(def);
            total += PESOS[def.rarity()];
        }
        if (options.isEmpty()) return ItemStack.EMPTY;
        // Orden estable: el mapa de definiciones no garantiza ninguno
        options.sort((a, b) -> a.order() != b.order() ? Integer.compare(a.order(), b.order()) : a.id().compareTo(b.id()));
        int roll = random.nextInt(total);
        for (PezDef def : options) {
            roll -= PESOS[def.rarity()];
            if (roll < 0) return ModItems.pezStack(def.id());
        }
        return ItemStack.EMPTY;
    }

    /** Un tesoro propio del mod de los que salen ahí; vacío si no hay ninguno. */
    public static ItemStack tesoro(Bioma bioma, Clima clima, double profundidad, RandomSource random) {
        List<Catalogo.Tesoro> options = new ArrayList<>();
        int total = 0;
        for (Catalogo.Tesoro t : Catalogo.TESOROS) {
            if (!t.own() || !t.aparece(bioma, clima, profundidad)) continue;
            options.add(t);
            total += PESOS[t.rarity()];
        }
        if (options.isEmpty()) return ItemStack.EMPTY;
        int roll = random.nextInt(total);
        for (Catalogo.Tesoro t : options) {
            roll -= PESOS[t.rarity()];
            if (roll < 0) return ModItems.tesoroStack(t.id());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}

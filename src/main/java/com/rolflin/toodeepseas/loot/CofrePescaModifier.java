package com.rolflin.toodeepseas.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rolflin.toodeepseas.core.Cofres.Banda;
import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.Cofres.Calidad;
import com.rolflin.toodeepseas.item.ModItems;
import com.rolflin.toodeepseas.world.Biomas;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Provisional hasta que exista el minijuego en Java: al pescar a la manera vanilla, a veces (lo decide la condición
 * de azar del JSON) sale un cofre del tesoro en lugar del pez
 * (lloviendo o con tormenta, a veces el cofre exclusivo de ese clima). El bioma y el clima son los del anzuelo, la calidad
 * la da la caña y la profundidad es siempre «superficie».
 */
public class CofrePescaModifier extends LootModifier {
    public static final Supplier<Codec<CofrePescaModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, CofrePescaModifier::new)));

    public static final RegistryObject<Codec<CofrePescaModifier>> COFRE_PESCA = AddItemModifier.SERIALIZERS.register("cofre_pesca", CODEC);

    public CofrePescaModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null) return generatedLoot;
        BlockPos pos = BlockPos.containing(origin);
        Bioma bioma = Biomas.bioma(context.getLevel(), pos);
        ItemStack rod = context.getParamOrNull(LootContextParams.TOOL);
        Calidad calidad = Calidad.of(rod == null ? null : ModItems.tierOf(rod));
        generatedLoot.clear();
        generatedLoot.add(ModItems.cofrePescado(bioma, calidad, Banda.SUPERFICIE, Biomas.clima(context.getLevel(), pos),
                context.getRandom().nextDouble()));
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}

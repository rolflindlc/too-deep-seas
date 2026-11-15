package com.rolflin.toodeepseas.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rolflin.toodeepseas.TooDeepSeas;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Añade un objeto al botín de otras tablas cuando se cumplen sus condiciones
 * ({@code data/toodeepseas/loot_modifiers}): fragmentos del vacío en las ciudades del End y en los shulkers,
 * y lombrices al cavar tierra con pala.
 */
public class AddItemModifier extends LootModifier {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TooDeepSeas.MODID);

    public static final Supplier<Codec<AddItemModifier>> CODEC = Suppliers.memoize(() -> RecordCodecBuilder.create(inst -> codecStart(inst)
            .and(ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item))
            .and(Codec.INT.optionalFieldOf("min", 1).forGetter(m -> m.min))
            .and(Codec.INT.optionalFieldOf("max", 1).forGetter(m -> m.max))
            .apply(inst, AddItemModifier::new)));

    public static final RegistryObject<Codec<AddItemModifier>> ADD_ITEM = SERIALIZERS.register("add_item", CODEC);

    private final Item item;
    private final int min;
    private final int max;

    public AddItemModifier(LootItemCondition[] conditions, Item item, int min, int max) {
        super(conditions);
        this.item = item;
        this.min = min;
        this.max = Math.max(min, max);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        int count = min + (max > min ? context.getRandom().nextInt(max - min + 1) : 0);
        if (count > 0) generatedLoot.add(new ItemStack(item, count));
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}

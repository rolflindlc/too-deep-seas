package com.rolflin.toodeepseas.effect;

import com.rolflin.toodeepseas.TooDeepSeas;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.UUID;
import java.util.function.Supplier;

/** Los dos efectos nuevos: Aletas (nadas más rápido) y Coraza (más armadura). */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TooDeepSeas.MODID);

    /** +40 % de velocidad de nado por nivel. */
    public static final RegistryObject<MobEffect> ALETAS = EFFECTS.register("aletas", () -> new AtributoEffect(0x4FC3E8,
            ForgeMod.SWIM_SPEED, UUID.fromString("5d1f6a0e-7a3c-4c57-9f0b-2f1f6a4f1a01"), 0.4, AttributeModifier.Operation.MULTIPLY_TOTAL));
    /** +4 de armadura por nivel. */
    public static final RegistryObject<MobEffect> CORAZA = EFFECTS.register("coraza", () -> new AtributoEffect(0x9AA7B4,
            () -> Attributes.ARMOR, UUID.fromString("5d1f6a0e-7a3c-4c57-9f0b-2f1f6a4f1a02"), 4.0, AttributeModifier.Operation.ADDITION));

    private ModEffects() {
    }

    /**
     * Efecto que modifica un atributo. No usa el mapa de MobEffect porque el atributo de nado de Forge se registra
     * después que los efectos: se busca al aplicarlo.
     */
    public static class AtributoEffect extends MobEffect {
        private final Supplier<? extends Attribute> attribute;
        private final UUID uuid;
        private final double amount;
        private final AttributeModifier.Operation operation;

        public AtributoEffect(int color, Supplier<? extends Attribute> attribute, UUID uuid, double amount, AttributeModifier.Operation operation) {
            super(MobEffectCategory.BENEFICIAL, color);
            this.attribute = attribute;
            this.uuid = uuid;
            this.amount = amount;
            this.operation = operation;
        }

        @Override
        public void addAttributeModifiers(LivingEntity entity, AttributeMap map, int amplifier) {
            AttributeInstance instance = map.getInstance(attribute.get());
            if (instance != null) {
                instance.removeModifier(uuid);
                instance.addPermanentModifier(new AttributeModifier(uuid, getDescriptionId(), amount * (amplifier + 1), operation));
            }
            super.addAttributeModifiers(entity, map, amplifier);
        }

        @Override
        public void removeAttributeModifiers(LivingEntity entity, AttributeMap map, int amplifier) {
            AttributeInstance instance = map.getInstance(attribute.get());
            if (instance != null) instance.removeModifier(uuid);
            super.removeAttributeModifiers(entity, map, amplifier);
        }
    }
}

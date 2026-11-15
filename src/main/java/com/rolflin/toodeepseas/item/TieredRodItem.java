package com.rolflin.toodeepseas.item;

import com.rolflin.toodeepseas.core.RodTier;
import net.minecraft.world.item.FishingRodItem;

/**
 * Caña con tier. Hereda de la caña vanilla, así que se lanza y se encanta igual.
 * Su descripción la pone {@code client.RodTooltip}, que también cubre la caña vanilla (tier 0).
 */
public class TieredRodItem extends FishingRodItem {
    private final RodTier tier;

    public TieredRodItem(RodTier tier, Properties properties) {
        super(properties.durability(tier.durability));
        this.tier = tier;
    }

    public RodTier getTier() {
        return tier;
    }

    @Override
    public int getEnchantmentValue() {
        return tier.enchantability;
    }
}

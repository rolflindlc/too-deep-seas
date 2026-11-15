package com.rolflin.toodeepseas.mixin;

import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/** Permite cambiar los bloques que cuentan como un puesto de trabajo (el del pescador). */
@Mixin(PoiType.class)
public interface PoiTypeAccessor {
    @Mutable
    @Accessor("matchingStates")
    void toodeepseas$setMatchingStates(Set<BlockState> states);
}

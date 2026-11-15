package com.rolflin.toodeepseas.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** Mapa bloque → puesto de trabajo que consulta el juego al poner o quitar un bloque. */
@Mixin(PoiTypes.class)
public interface PoiTypesAccessor {
    @Accessor("TYPE_BY_STATE")
    static Map<BlockState, Holder<PoiType>> toodeepseas$typeByState() {
        throw new AssertionError();
    }
}

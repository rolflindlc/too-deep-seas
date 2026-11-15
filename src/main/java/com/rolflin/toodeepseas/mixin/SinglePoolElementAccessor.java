package com.rolflin.toodeepseas.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Pieza de una estructura (una casa de aldea): qué plantilla es y qué procesadores se le aplican al generarla. */
@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccessor {
    @Accessor("template")
    Either<ResourceLocation, StructureTemplate> toodeepseas$template();

    @Accessor("processors")
    Holder<StructureProcessorList> toodeepseas$processors();

    @Mutable
    @Accessor("processors")
    void toodeepseas$setProcessors(Holder<StructureProcessorList> processors);
}

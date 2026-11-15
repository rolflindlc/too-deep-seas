package com.rolflin.toodeepseas.block;

import com.rolflin.toodeepseas.TooDeepSeas;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TooDeepSeas.MODID);

    public static final RegistryObject<MesaPescadorBlock> MESA_PESCADOR = BLOCKS.register("mesa_pescador",
            () -> new MesaPescadorBlock(BlockBehaviour.Properties.copy(Blocks.FLETCHING_TABLE)));

    private ModBlocks() {
    }
}

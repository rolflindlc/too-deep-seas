package com.rolflin.toodeepseas.block;

import com.rolflin.toodeepseas.menu.MesaPescadorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Mesa del pescador. Como la mesa de trabajo, no guarda nada: la caña que se deja en la ventana
 * vuelve al jugador al cerrarla, con el equipo ya escrito en su NBT.
 */
public class MesaPescadorBlock extends Block {
    private static final Component TITLE = Component.translatable("container.toodeepseas.mesa_pescador");

    public MesaPescadorBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) -> new MesaPescadorMenu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
    }
}

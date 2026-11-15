package com.rolflin.toodeepseas.data;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.core.Cofres;
import com.rolflin.toodeepseas.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Comando provisional para probar el equipo hasta que exista la mesa del pescador (fase 4):
 * /tds equipar <accesorio> <nivel> · /tds cebo <cebo> <cantidad> · /tds vaciar
 * /tds cofre <bioma> [calidad 1-4] [banda 1-3] [clima 1-3]: da un cofre del tesoro con esas características
 */
@Mod.EventBusSubscriber(modid = TooDeepSeas.MODID)
public final class DebugCommand {
    private DebugCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tds").requires(s -> s.hasPermission(2))
                .then(Commands.literal("equipar").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Defs.ACCESSORIES.ids(), b))
                        .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 3)).executes(c -> {
                            String error = RodData.tryAdd(rod(c), StringArgumentType.getString(c, "id"), IntegerArgumentType.getInteger(c, "nivel"));
                            return reply(c, error == null ? "Accesorio equipado" : "No se puede equipar: " + error, error == null);
                        }))))
                .then(Commands.literal("cebo").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Defs.BAITS.ids(), b))
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(0, 64)).executes(c -> {
                            String id = StringArgumentType.getString(c, "id");
                            if (Defs.BAITS.get(id) == null) return reply(c, "Cebo desconocido: " + id, false);
                            RodData.setBait(rod(c), id, IntegerArgumentType.getInteger(c, "cantidad"));
                            return reply(c, "Cebo puesto", true);
                        }))))
                .then(Commands.literal("cofre").then(Commands.argument("bioma", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Cofres.Bioma.values()).map(Cofres.Bioma::id), b))
                        .executes(c -> cofre(c, 1, 1, 1))
                        .then(Commands.argument("calidad", IntegerArgumentType.integer(1, 4)).executes(c -> cofre(c, IntegerArgumentType.getInteger(c, "calidad"), 1, 1))
                                .then(Commands.argument("banda", IntegerArgumentType.integer(1, 3)).executes(c -> cofre(c, IntegerArgumentType.getInteger(c, "calidad"), IntegerArgumentType.getInteger(c, "banda"), 1))
                                        .then(Commands.argument("clima", IntegerArgumentType.integer(1, 3)).executes(c -> cofre(c, IntegerArgumentType.getInteger(c, "calidad"),
                                                IntegerArgumentType.getInteger(c, "banda"), IntegerArgumentType.getInteger(c, "clima"))))))))
                .then(Commands.literal("vaciar").executes(c -> {
                    ItemStack rod = rod(c);
                    RodData.setSlots(rod, List.of());
                    RodData.setBait(rod, "", 0);
                    return reply(c, "Caña vaciada", true);
                })));
    }

    private static int cofre(CommandContext<CommandSourceStack> c, int calidad, int banda, int clima) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Cofres.Bioma bioma = Cofres.Bioma.byId(StringArgumentType.getString(c, "bioma"));
        if (bioma == null) return reply(c, "Bioma desconocido", false);
        ServerPlayer player = c.getSource().getPlayerOrException();
        ItemStack stack = ModItems.COFRES.get(bioma).get().create(Cofres.Calidad.byIndex(calidad - 1), Cofres.Banda.byIndex(banda - 1), Cofres.Clima.byIndex(clima - 1));
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        return reply(c, "Cofre entregado", true);
    }

    private static ItemStack rod(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return c.getSource().getPlayerOrException().getMainHandItem();
    }

    private static int reply(CommandContext<CommandSourceStack> c, String text, boolean ok) {
        if (ok) c.getSource().sendSuccess(() -> Component.literal(text), false);
        else c.getSource().sendFailure(Component.literal(text));
        return ok ? 1 : 0;
    }
}

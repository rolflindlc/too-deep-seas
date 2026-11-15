package com.rolflin.toodeepseas.juego;

import com.rolflin.toodeepseas.Config;
import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.core.Cofres;
import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.FishingMedium;
import com.rolflin.toodeepseas.core.RodStats;
import com.rolflin.toodeepseas.core.RodTier;
import com.rolflin.toodeepseas.core.juego.Grabacion;
import com.rolflin.toodeepseas.core.juego.Partida;
import com.rolflin.toodeepseas.core.juego.Perfil;
import com.rolflin.toodeepseas.core.juego.PezDef;
import com.rolflin.toodeepseas.data.Defs;
import com.rolflin.toodeepseas.data.RodData;
import com.rolflin.toodeepseas.item.ModItems;
import com.rolflin.toodeepseas.net.AbrirMinijuegoPacket;
import com.rolflin.toodeepseas.net.Network;
import com.rolflin.toodeepseas.net.RecogerPacket;
import com.rolflin.toodeepseas.world.Biomas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * La pesca con minijuego, en el servidor: cuando el anzuelo de un jugador toca el agua (o la lava, o cae al Vacío) se
 * mide el sitio, se abre el minijuego en su cliente y, al terminar, se repite la partida con las teclas que envía para
 * saber qué ha pescado de verdad, se le da el botín y se gasta la caña.
 */
@Mod.EventBusSubscriber(modid = TooDeepSeas.MODID)
public final class Sesiones {
    /** Bloques de agua (o lava) conectados que hacen falta para pescar. No tienen por qué estar uno debajo de otro. */
    public static final int MIN_BLOQUES = 30;

    /** Una partida en curso: lo que se midió al lanzar. */
    public record Sesion(Partida.Ajustes ajustes, FishingMedium medium) {
    }

    /** Lo que ha pasado al terminar. */
    public record Resultado(boolean murio, @Nullable String causa, List<ItemStack> botin, int coste, double profundidad) {
    }

    private static final Map<UUID, Sesion> SESIONES = new HashMap<>();

    private Sesiones() {
    }

    public static boolean jugando(Player player) {
        return SESIONES.containsKey(player.getUUID());
    }

    /** Todos los objetos que pueden salir, en el mismo orden en el cliente y en el servidor. */
    public static List<Partida.Objeto> objetos() {
        List<PezDef> peces = new ArrayList<>();
        for (String id : Defs.PECES.ids()) {
            PezDef def = Defs.PECES.get(id);
            if (def != null) peces.add(def);
        }
        return Partida.objetos(peces);
    }

    /** La caña con la que pesca: la de la mano principal y, si no, la de la secundaria. */
    @Nullable
    public static InteractionHand manoDeLaCana(Player player) {
        if (ModItems.tierOf(player.getMainHandItem()) != null) return InteractionHand.MAIN_HAND;
        if (ModItems.tierOf(player.getOffhandItem()) != null) return InteractionHand.OFF_HAND;
        return null;
    }

    /** Dónde ha caído el anzuelo, o null si todavía no está en ningún sitio donde se pesque. */
    @Nullable
    public static FishingMedium medio(Level level, BlockPos pos, double anzueloY, double jugadorY) {
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            Bioma b = Biomas.bioma(level, pos);
            return b == Bioma.OCEANO || b == Bioma.CALIDO ? FishingMedium.OCEAN : FishingMedium.WATER;
        }
        if (level.getFluidState(pos).is(FluidTags.LAVA)) return FishingMedium.LAVA;
        // Vacío del End: el anzuelo ha caído por debajo del jugador y no tiene ningún bloque en su columna
        if (level.dimension() == Level.END && anzueloY < jugadorY - 6
                && level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= level.getMinBuildHeight()) return FishingMedium.VOID;
        return null;
    }

    /** Bloques de ese líquido conectados con {@code pos} (por las caras), contados hasta {@code tope}. */
    public static int conectados(Level level, BlockPos pos, TagKey<Fluid> fluido, int tope) {
        Set<BlockPos> vistos = new HashSet<>();
        ArrayDeque<BlockPos> cola = new ArrayDeque<>();
        if (!level.getFluidState(pos).is(fluido)) return 0;
        vistos.add(pos);
        cola.add(pos);
        while (!cola.isEmpty() && vistos.size() < tope) {
            BlockPos actual = cola.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = actual.relative(dir);
                if (vistos.contains(next) || !level.isLoaded(next) || !level.getFluidState(next).is(fluido)) continue;
                vistos.add(next);
                cola.add(next);
                if (vistos.size() >= tope) break;
            }
        }
        return vistos.size();
    }

    /** ¿Hay bastante líquido para pescar aquí? En el Vacío no hace falta. */
    public static boolean haySitio(Level level, BlockPos pos, FishingMedium medium) {
        return switch (medium) {
            case VOID -> true;
            case LAVA -> conectados(level, pos, FluidTags.LAVA, MIN_BLOQUES) >= MIN_BLOQUES;
            default -> conectados(level, pos, FluidTags.WATER, MIN_BLOQUES) >= MIN_BLOQUES;
        };
    }

    /** Mide el sitio y la caña. Null si eso no es una caña o si esa caña no puede pescar ahí. */
    @Nullable
    public static Sesion medir(Level level, BlockPos pos, FishingMedium medium, ItemStack rod, int seed) {
        RodStats stats = RodData.stats(rod, medium);
        if (stats == null || !stats.canFish) return null;
        Bioma bioma = switch (medium) {
            case LAVA -> Bioma.LAVA;
            case VOID -> Bioma.VACIO;
            default -> {
                Bioma b = Biomas.bioma(level, pos);
                yield b.hasWeather ? b : Bioma.RIO;
            }
        };
        Perfil.SubBioma sub = bioma == Bioma.LAVA ? Biomas.subBioma(level, pos) : Perfil.SubBioma.YERMOS;
        Set<String> cebos = new HashSet<>();
        for (RodData.Bait bait : RodData.baits(rod)) cebos.add(bait.id());
        int lure = Math.max(0, Math.min(4, EnchantmentHelper.getFishingSpeedBonus(rod)));
        Partida.Ajustes aj = new Partida.Ajustes(seed, bioma, sub, Biomas.clima(level, pos), stats.effectiveDepth, stats.base, stats.k, stats.vh,
                stats.dive, stats.hookR, stats.luck, lure, stats.attractR, stats.magnetR, cebos);
        return new Sesion(aj, medium);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        FishingHook hook = player.fishing;
        if (hook == null || !Config.MINIJUEGO.get()) return;
        InteractionHand hand = manoDeLaCana(player);
        if (hand == null) return;
        Level level = hook.level();
        BlockPos pos = hook.blockPosition();
        FishingMedium medium = medio(level, pos, hook.getY(), player.getY());
        if (medium == null) return;
        if (!haySitio(level, pos, medium)) {
            hook.discard();
            player.displayClientMessage(Component.translatable("minijuego.toodeepseas.poca_" + (medium == FishingMedium.LAVA ? "lava" : "agua"), MIN_BLOQUES), true);
            return;
        }
        Sesion sesion = medir(level, pos, medium, player.getItemInHand(hand), level.getRandom().nextInt());
        hook.discard();
        if (sesion == null) {
            player.displayClientMessage(Component.translatable("minijuego.toodeepseas.no_puede." + (medium == FishingMedium.VOID ? "vacio" : "lava")), true);
            return;
        }
        SESIONES.put(player.getUUID(), sesion);
        Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AbrirMinijuegoPacket(sesion.ajustes()));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESIONES.remove(event.getEntity().getUUID());
    }

    /** Golpe externo: si te hacen daño mientras pescas, el cliente recoge y sales con lo que llevabas. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && jugando(player)) {
            Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RecogerPacket());
        }
    }

    /** El cliente ha terminado: llega la grabación de sus teclas. */
    public static void alTerminar(ServerPlayer player, byte[] grabacion) {
        Sesion sesion = SESIONES.remove(player.getUUID());
        if (sesion == null) return;
        Resultado r = terminar(player, sesion, grabacion);
        TooDeepSeas.LOGGER.debug("Minijuego de {}: {} objetos, {} m, {}", player.getGameProfile().getName(), r.botin().size(), r.profundidad(),
                r.murio() ? "muerte por " + r.causa() : "salida");
    }

    /** Repite la partida, da el botín (si no ha muerto), gasta la caña y consume una unidad de cada cebo. */
    public static Resultado terminar(Player player, Sesion sesion, byte[] grabacion) {
        List<Partida.Objeto> objs = objetos();
        Partida p = new Partida(sesion.ajustes(), objs, false);
        Grabacion.repetir(p, grabacion);
        boolean murio = p.estado == Partida.Estado.MUERTO;
        List<ItemStack> botin = new ArrayList<>();
        InteractionHand hand = manoDeLaCana(player);
        if (hand == null) return new Resultado(murio, murio ? p.causa.clave() : null, botin, 0, p.profFinal); // ya no tiene la caña: no hay botín
        ItemStack rod = player.getItemInHand(hand);
        RodTier tier = ModItems.tierOf(rod);
        RodStats stats = RodData.stats(rod, sesion.medium());
        RandomSource random = player.getRandom();
        Bioma bioma = sesion.ajustes().bioma();
        if (!murio) {
            int xp = 0;
            for (int id : p.botin) {
                Partida.Objeto o = objs.get(id);
                ItemStack stack;
                if (o.pez() != null) stack = ModItems.pezStack(o.pez().id());
                else if (o.tesoro().chest()) stack = ModItems.cofrePescado(bioma, Cofres.Calidad.of(tier), Cofres.Banda.of(bioma, p.profFinal), sesion.ajustes().clima(), random.nextDouble());
                else stack = ModItems.tesoroStack(o.tesoro().id());
                if (stack.isEmpty()) continue;
                xp += 1 + o.rarity();
                botin.add(stack.copy());
                if (stats != null && random.nextDouble() < stats.dupChance) botin.add(stack.copy()); // captura doble
            }
            for (ItemStack stack : botin) ItemHandlerHelper.giveItemToPlayer(player, stack.copy());
            if (xp > 0) player.giveExperiencePoints(xp);
            if (!p.botin.isEmpty()) player.awardStat(Stats.FISH_CAUGHT, p.botin.size());
        }
        int coste = stats == null ? 1 : stats.durabilityCost(p.botin.size(), murio, p.profFinal, sesion.medium());
        // el cebo se gasta antes que la caña, por si la caña se rompe
        List<RodData.Bait> cebos = new ArrayList<>();
        for (RodData.Bait bait : RodData.baits(rod)) cebos.add(new RodData.Bait(bait.id(), bait.count() - 1));
        if (!cebos.isEmpty() && !player.getAbilities().instabuild) RodData.setBaits(rod, cebos);
        rod.hurtAndBreak(coste, player, pl -> pl.broadcastBreakEvent(hand));
        return new Resultado(murio, murio ? p.causa.clave() : null, botin, coste, p.profFinal);
    }
}

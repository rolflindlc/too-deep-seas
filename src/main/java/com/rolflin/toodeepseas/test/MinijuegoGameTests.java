package com.rolflin.toodeepseas.test;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.core.Cofres.Bioma;
import com.rolflin.toodeepseas.core.FishingMedium;
import com.rolflin.toodeepseas.core.juego.Grabacion;
import com.rolflin.toodeepseas.core.juego.Partida;
import com.rolflin.toodeepseas.data.RodData;
import com.rolflin.toodeepseas.item.ModItems;
import com.rolflin.toodeepseas.juego.Sesiones;
import com.rolflin.toodeepseas.net.AbrirMinijuegoPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** El minijuego dentro del juego: medir el sitio, el paquete que abre la pantalla y el botín al terminar. */
@GameTestHolder(TooDeepSeas.MODID)
@PrefixGameTestTemplate(false)
public final class MinijuegoGameTests {
    private MinijuegoGameTests() {
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new GameTestAssertException(message);
    }

    private static Partida.Ajustes conSemilla(Partida.Ajustes a, int seed) {
        return new Partida.Ajustes(seed, a.bioma(), a.sub(), a.clima(), a.maxD(), a.base(), a.k(), a.vh(), a.dive(), a.hookR(), a.luck(), a.lure(),
                a.attractR(), a.magnetR(), a.cebos());
    }

    /** Una piscina de {@code n} bloques de líquido, repartidos en capas de 3×3 (no están en vertical): devuelve el primero. */
    private static BlockPos piscina(GameTestHelper helper, boolean lava, int n) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(1, 3, 1));
        for (int i = 0; i < n; i++) level.setBlockAndUpdate(base.offset(i % 3, -(i / 9), (i / 3) % 3), (lava ? Blocks.LAVA : Blocks.WATER).defaultBlockState());
        return base;
    }

    private static void limpiar(GameTestHelper helper, BlockPos base, int n) {
        for (int i = 0; i < n; i++) helper.getLevel().setBlockAndUpdate(base.offset(i % 3, -(i / 9), (i / 3) % 3), Blocks.AIR.defaultBlockState());
    }

    @GameTest(template = "empty")
    public static void minijuegoMedir(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        check(Sesiones.objetos().size() == 134, "tienen que ser 100 peces, 33 tesoros y el pez globo: " + Sesiones.objetos().size());
        BlockPos agua = piscina(helper, false, 29);
        check(Sesiones.conectados(level, agua, FluidTags.WATER, 100) == 29, "29 bloques conectados");
        check(!Sesiones.haySitio(level, agua, FishingMedium.WATER), "con 29 bloques de agua no se pesca");
        limpiar(helper, agua, 29);
        agua = piscina(helper, false, 30);
        check(Sesiones.haySitio(level, agua, FishingMedium.WATER), "con 30 bloques de agua conectados se pesca (aunque no estén en vertical)");
        check(Sesiones.conectados(level, agua.east(10), FluidTags.WATER, 100) == 0, "fuera del agua no hay nada conectado");
        FishingMedium medio = Sesiones.medio(level, agua, agua.getY(), agua.getY() + 2);
        check(medio == FishingMedium.WATER || medio == FishingMedium.OCEAN, "el agua es agua: " + medio);
        check(Sesiones.medio(level, agua.above(3), agua.getY() + 3, agua.getY() + 5) == null, "en el aire no se pesca");
        ItemStack cana = new ItemStack(Items.FISHING_ROD);
        RodData.setBait(cana, "lombriz", 5);
        Sesiones.Sesion s = Sesiones.medir(level, agua, medio, cana, 42);
        check(s != null, "la caña de madera pesca en el agua");
        check(s.ajustes().maxD() == 30, "la profundidad es la del sedal de la caña de madera (30 m), no la del agua: " + s.ajustes().maxD());
        Sesiones.Sesion sc = Sesiones.medir(level, agua, medio, new ItemStack(ModItems.CANA_COBRE.get()), 1);
        check(sc != null && sc.ajustes().maxD() == 45, "la de cobre baja 45 m: " + (sc == null ? "-" : sc.ajustes().maxD()));
        check(s.ajustes().bioma().hasWeather && s.ajustes().seed() == 42, "bioma de agua y semilla");
        check(s.ajustes().cebos().contains("lombriz"), "el cebo de la caña cuenta");
        check(Sesiones.medir(level, agua, medio, new ItemStack(Items.STICK), 1) == null, "un palo no es una caña");
        // el paquete que abre la pantalla lleva los mismos ajustes
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        new AbrirMinijuegoPacket(s.ajustes()).encode(buf);
        check(AbrirMinijuegoPacket.decode(buf).ajustes().equals(s.ajustes()), "el paquete cambia los ajustes");
        limpiar(helper, agua, 30);

        BlockPos lava = piscina(helper, true, 30);
        check(Sesiones.haySitio(level, lava, FishingMedium.LAVA), "30 bloques de lava conectados bastan");
        check(Sesiones.medio(level, lava, lava.getY(), lava.getY() + 2) == FishingMedium.LAVA, "la lava es lava");
        check(Sesiones.medir(level, lava, FishingMedium.LAVA, new ItemStack(Items.FISHING_ROD), 1) == null, "la caña de madera no pesca en lava");
        Sesiones.Sesion sl = Sesiones.medir(level, lava, FishingMedium.LAVA, new ItemStack(ModItems.CANA_NETHERITA.get()), 1);
        check(sl != null && sl.ajustes().bioma() == Bioma.LAVA && sl.ajustes().maxD() == 120, "la caña de netherita pesca en lava, a 120 m");
        check(Sesiones.medir(level, lava, FishingMedium.VOID, new ItemStack(ModItems.CANA_NETHERITA.get()), 1) == null, "la de netherita no pesca en el Vacío");
        Sesiones.Sesion sv = Sesiones.medir(level, lava, FishingMedium.VOID, new ItemStack(ModItems.CANA_VACIO.get()), 1);
        check(sv != null && sv.ajustes().bioma() == Bioma.VACIO && Double.isInfinite(sv.ajustes().maxD()), "la caña del vacío pesca en el Vacío, sin fondo");
        limpiar(helper, lava, 30);
        helper.succeed();
    }

    /** Juega como lo haría el cliente: hacia el centro del hueco, y recoge al llegar a {@code hasta} metros. */
    private static Partida jugar(Partida.Ajustes aj, List<Partida.Objeto> objs, Grabacion g, double hasta, boolean suicida) {
        Partida p = new Partida(aj, objs, true);
        for (int i = 0; i < 60 * 60 && (p.estado == Partida.Estado.CAST || p.estado == Partida.Estado.PLAY); i++) {
            double[] sp = p.terreno.spanAt(p.hookX, p.hookY + 30);
            double cx = (sp[0] + sp[1]) / 2;
            int teclas = suicida ? Partida.IZQ : p.hookX < cx - 2 ? Partida.DER : p.hookX > cx + 2 ? Partida.IZQ : 0;
            if (!suicida && (p.profundidad() >= hasta || p.atFloor)) teclas |= Partida.RECOGER;
            g.add(teclas);
            p.paso(teclas);
        }
        return p;
    }

    @GameTest(template = "empty")
    public static void minijuegoBotin(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos agua = piscina(helper, false, 30);
        FishingMedium medio = Sesiones.medio(level, agua, agua.getY(), agua.getY() + 2);
        List<Partida.Objeto> objs = Sesiones.objetos();
        ItemStack base = new ItemStack(ModItems.CANA_COBRE.get());
        RodData.setBait(base, "lombriz", 5);
        Sesiones.Sesion medida = Sesiones.medir(level, agua, medio, base, 1);
        check(medida != null, "la caña de cobre pesca en el agua");

        // Salir con botín: se busca una semilla en la que el jugador de pruebas pesque algo y salga vivo
        boolean probado = false;
        for (int seed = 1; seed <= 200 && !probado; seed++) {
            Sesiones.Sesion sesion = new Sesiones.Sesion(conSemilla(medida.ajustes(), seed), medida.medium());
            Grabacion g = new Grabacion();
            Partida cliente = jugar(sesion.ajustes(), objs, g, 9, false);
            if (cliente.estado == Partida.Estado.MUERTO || cliente.botin.isEmpty()) continue;
            probado = true;
            Player player = helper.makeMockPlayer();
            ItemStack cana = base.copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, cana);
            Sesiones.Resultado r = Sesiones.terminar(player, sesion, g.bytes());
            check(!r.murio(), "el servidor dice que ha muerto y el cliente que no (semilla " + seed + ")");
            check(r.botin().size() >= cliente.botin.size(), "el servidor da " + r.botin().size() + " objetos y el cliente pescó " + cliente.botin.size());
            check(Math.abs(r.profundidad() - cliente.profFinal) < 1e-9, "profundidad distinta en el servidor");
            for (ItemStack stack : r.botin()) check(player.getInventory().contains(stack), "no está en el inventario: " + stack);
            int esperado = Math.max(1, (cliente.botin.size() + 1) / 2);
            check(r.coste() == esperado && cana.getDamageValue() == esperado, "salir cuesta la mitad de los objetos: " + r.coste() + " / " + cana.getDamageValue() + " / " + esperado);
            check(RodData.baits(cana).get(0).count() == 4, "se gasta una unidad de cebo");
        }
        check(probado, "ninguna semilla da una partida con botín");

        // Morir: se pierde todo y la caña se gasta
        Sesiones.Sesion sesion = new Sesiones.Sesion(conSemilla(medida.ajustes(), 7), medida.medium());
        Grabacion g = new Grabacion();
        Partida cliente = jugar(sesion.ajustes(), objs, g, 9, true);
        check(cliente.estado == Partida.Estado.MUERTO, "pegado a la pared izquierda se muere");
        Player player = helper.makeMockPlayer();
        ItemStack cana = base.copy();
        player.setItemInHand(InteractionHand.OFF_HAND, cana);
        Sesiones.Resultado r = Sesiones.terminar(player, sesion, g.bytes());
        check(r.murio() && r.causa().equals(cliente.causa.clave()), "el servidor tiene que ver la misma muerte: " + r.causa());
        check(r.botin().isEmpty(), "al morir no hay botín");
        check(cana.getDamageValue() == Math.max(1, cliente.botin.size()), "morir cuesta tantos puntos como objetos (mín. 1): " + cana.getDamageValue());

        // Sin caña en la mano no hay botín ni error
        Player sinCana = helper.makeMockPlayer();
        check(Sesiones.terminar(sinCana, sesion, g.bytes()).coste() == 0, "sin caña no se gasta nada");
        limpiar(helper, agua, 30);
        helper.succeed();
    }
}

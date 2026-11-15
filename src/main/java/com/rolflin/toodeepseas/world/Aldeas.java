package com.rolflin.toodeepseas.world;

import com.rolflin.toodeepseas.Config;
import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.block.ModBlocks;
import com.rolflin.toodeepseas.mixin.PoiTypeAccessor;
import com.rolflin.toodeepseas.mixin.PoiTypesAccessor;
import com.rolflin.toodeepseas.mixin.SinglePoolElementAccessor;
import com.rolflin.toodeepseas.mixin.StructureTemplatePoolAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Aldeas: la mesa del pescador sustituye al barril como puesto de trabajo del pescador.
 * <ul>
 * <li>{@link #aplicarPuesto}: la mesa pasa a ser un bloque del puesto «fisherman» y, salvo con {@code barrilLegado},
 * el barril deja de serlo. Los barriles que ya eran el puesto de un aldeano siguen valiendo hasta que se rompan.</li>
 * <li>{@link #ponerMesaEnCasas}: las casas de pescador que se generen a partir de ahora llevan la mesa donde iba el barril.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TooDeepSeas.MODID)
public final class Aldeas {
    private static final Logger LOGGER = LogManager.getLogger();
    @Nullable
    private static StructureProcessor barrilAMesa;

    private Aldeas() {
    }

    /** Se llama una vez al arrancar (FMLCommonSetupEvent), con los bloques ya registrados y la configuración leída. */
    public static void aplicarPuesto(boolean barrilLegado) {
        Holder<PoiType> fisherman = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolderOrThrow(PoiTypes.FISHERMAN);
        Map<BlockState, Holder<PoiType>> byState = PoiTypesAccessor.toodeepseas$typeByState();
        Set<BlockState> states = new HashSet<>(fisherman.value().matchingStates());
        for (BlockState state : ModBlocks.MESA_PESCADOR.get().getStateDefinition().getPossibleStates()) {
            states.add(state);
            byState.put(state, fisherman);
        }
        for (BlockState state : Blocks.BARREL.getStateDefinition().getPossibleStates()) {
            Holder<PoiType> current = byState.get(state);
            if (barrilLegado) {
                // Solo si nadie más (otro mod) se ha quedado con el barril
                if (current == null || current.is(PoiTypes.FISHERMAN)) {
                    states.add(state);
                    byState.put(state, fisherman);
                }
            } else {
                states.remove(state);
                if (current != null && current.is(PoiTypes.FISHERMAN)) byState.remove(state);
            }
        }
        ((PoiTypeAccessor) (Object) fisherman.value()).toodeepseas$setMatchingStates(Set.copyOf(states));
        LOGGER.info("Too Deep Seas: puesto del pescador = mesa del pescador{}", barrilLegado ? " y barril" : "");
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (Config.BARRIL_LEGADO.get()) return;
        int n = ponerMesaEnCasas(event.getServer().registryAccess());
        LOGGER.info("Too Deep Seas: {} casas de pescador llevarán la mesa del pescador", n);
    }

    /** Una pieza cuenta como casa de pescador si su plantilla lleva «fisher» en el nombre (también las de otros mods). */
    public static boolean esCasaDePescador(StructurePoolElement element) {
        if (!(element instanceof SinglePoolElement)) return false;
        Optional<ResourceLocation> id = ((SinglePoolElementAccessor) element).toodeepseas$template().left();
        return id.isPresent() && id.get().getPath().contains("fisher");
    }

    public static boolean llevaMesa(StructurePoolElement element) {
        return barrilAMesa != null && element instanceof SinglePoolElement
                && ((SinglePoolElementAccessor) element).toodeepseas$processors().value().list().contains(barrilAMesa);
    }

    /** Añade a cada casa de pescador un procesador que cambia los barriles por la mesa. Devuelve cuántas piezas ha cambiado. */
    public static int ponerMesaEnCasas(RegistryAccess access) {
        if (barrilAMesa == null) {
            barrilAMesa = new RuleProcessor(List.of(new ProcessorRule(new BlockMatchTest(Blocks.BARREL), AlwaysTrueTest.INSTANCE,
                    ModBlocks.MESA_PESCADOR.get().defaultBlockState())));
        }
        Registry<StructureTemplatePool> pools = access.registryOrThrow(Registries.TEMPLATE_POOL);
        int changed = 0;
        for (StructureTemplatePool pool : pools) {
            // La misma pieza aparece repetida según su peso: llevaMesa evita cambiarla dos veces
            for (StructurePoolElement element : ((StructureTemplatePoolAccessor) (Object) pool).toodeepseas$templates()) {
                if (!esCasaDePescador(element) || llevaMesa(element)) continue;
                SinglePoolElementAccessor accessor = (SinglePoolElementAccessor) element;
                List<StructureProcessor> list = new ArrayList<>(accessor.toodeepseas$processors().value().list());
                list.add(barrilAMesa);
                accessor.toodeepseas$setProcessors(Holder.direct(new StructureProcessorList(list)));
                changed++;
            }
        }
        return changed;
    }
}

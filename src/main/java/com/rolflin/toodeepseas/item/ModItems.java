package com.rolflin.toodeepseas.item;

import com.rolflin.toodeepseas.TooDeepSeas;
import com.rolflin.toodeepseas.block.ModBlocks;
import com.rolflin.toodeepseas.core.Cofres;
import com.rolflin.toodeepseas.core.RodTier;
import com.rolflin.toodeepseas.core.juego.Catalogo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TooDeepSeas.MODID);

    // La caña de madera (tier 0) es la caña vanilla
    public static final RegistryObject<TieredRodItem> CANA_COBRE = rod("cana_cobre", RodTier.COBRE, new Item.Properties());
    public static final RegistryObject<TieredRodItem> CANA_HIERRO = rod("cana_hierro", RodTier.HIERRO, new Item.Properties());
    public static final RegistryObject<TieredRodItem> CANA_ORO = rod("cana_oro", RodTier.ORO, new Item.Properties());
    public static final RegistryObject<TieredRodItem> CANA_PRISMARINA = rod("cana_prismarina", RodTier.PRISMARINA, new Item.Properties());
    public static final RegistryObject<TieredRodItem> CANA_DIAMANTE = rod("cana_diamante", RodTier.DIAMANTE, new Item.Properties());
    public static final RegistryObject<TieredRodItem> CANA_NETHERITA = rod("cana_netherita", RodTier.NETHERITA, new Item.Properties().fireResistant());
    public static final RegistryObject<TieredRodItem> CANA_VACIO = rod("cana_vacio", RodTier.VACIO, new Item.Properties().fireResistant().rarity(Rarity.EPIC));

    public static final List<RegistryObject<TieredRodItem>> RODS = List.of(
            CANA_COBRE, CANA_HIERRO, CANA_ORO, CANA_PRISMARINA, CANA_DIAMANTE, CANA_NETHERITA, CANA_VACIO);

    public static final RegistryObject<BlockItem> MESA_PESCADOR = ITEMS.register("mesa_pescador",
            () -> new BlockItem(ModBlocks.MESA_PESCADOR.get(), new Item.Properties()));

    /** Materiales propios: corcho para los flotadores, diente del tiburón tigre (silbato) y fragmento del vacío (caña del vacío). */
    public static final RegistryObject<MaterialItem> CORCHO_ABEDUL = ITEMS.register("corcho_abedul", () -> new MaterialItem("corcho_abedul", new Item.Properties()));
    public static final RegistryObject<MaterialItem> DIENTE_TIBURON = ITEMS.register("diente_tiburon", () -> new MaterialItem("diente_tiburon", new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<MaterialItem> FRAGMENTO_VACIO = ITEMS.register("fragmento_vacio", () -> new MaterialItem("fragmento_vacio", new Item.Properties().rarity(Rarity.RARE)));
    public static final List<RegistryObject<MaterialItem>> MATERIALS = List.of(CORCHO_ABEDUL, DIENTE_TIBURON, FRAGMENTO_VACIO);

    /** Accesorios por id de definición (el mismo que el del objeto), en el orden del catálogo. */
    public static final Map<String, RegistryObject<AccessoryItem>> ACCESSORIES = new LinkedHashMap<>();
    /** Cebos por id de definición. */
    public static final Map<String, RegistryObject<BaitItem>> BAITS = new LinkedHashMap<>();

    /** Cofres del tesoro: un objeto por bioma de pesca (cofre_oceano, cofre_lava...). */
    public static final Map<Cofres.Bioma, RegistryObject<CofreItem>> COFRES = new EnumMap<>(Cofres.Bioma.class);

    /** Cofres exclusivos de clima: cofre_lluvia y cofre_tormenta. */
    public static final Map<Cofres.Clima, RegistryObject<CofreClimaItem>> COFRES_CLIMA = new EnumMap<>(Cofres.Clima.class);

    /** Peces del minijuego como objetos (96: los otros 4 son los de Minecraft), por id de pez. */
    public static final Map<String, RegistryObject<PezItem>> PECES = new LinkedHashMap<>();
    /** Tesoros de pesca propios del mod, por id. */
    public static final Map<String, RegistryObject<TesoroItem>> TESOROS = new LinkedHashMap<>();
    /** Pescado cocinado y platos. */
    public static final Map<String, RegistryObject<ComidaItem>> COMIDAS = new LinkedHashMap<>();

    static {
        for (Catalogo.Pez pez : Catalogo.PECES) {
            if (pez.own()) PECES.put(pez.id(), ITEMS.register(pez.id(), () -> new PezItem(pez)));
        }
        for (Catalogo.Tesoro tesoro : Catalogo.TESOROS) {
            if (!tesoro.own()) continue;
            TESOROS.put(tesoro.id(), ITEMS.register(tesoro.id(),
                    () -> tesoro.kind().equals("cabeza") ? new CoronaItem(tesoro) : new TesoroItem(tesoro)));
        }
        for (Catalogo.Comida comida : Catalogo.COMIDAS) {
            COMIDAS.put(comida.id(), ITEMS.register(comida.id(), () -> new ComidaItem(comida)));
        }
        for (Cofres.Clima clima : Cofres.Clima.values()) {
            if (!Cofres.tieneCofrePropio(clima)) continue;
            COFRES_CLIMA.put(clima, ITEMS.register("cofre_" + clima.id(), () -> new CofreClimaItem(clima, new Item.Properties())));
        }
        for (Cofres.Bioma bioma : Cofres.Bioma.values()) {
            Item.Properties properties = bioma == Cofres.Bioma.LAVA ? new Item.Properties().fireResistant() : new Item.Properties();
            COFRES.put(bioma, ITEMS.register("cofre_" + bioma.id(), () -> new CofreItem(bioma, properties)));
        }
        // id y número de niveles, igual que en data/toodeepseas/accessories
        accessory("sedal_reforzado", 3);
        accessory("sedal_seda", 3);
        accessory("sedal_trenzado", 3);
        accessory("sedal_invisible", 3);
        accessory("anzuelo_afilado", 3);
        accessory("anzuelo_doble", 3);
        accessory("anzuelo_iman", 3);
        accessory("plomada", 3);
        accessory("flotador", 3);
        accessory("lastre_buceo", 3);
        accessory("carrete", 3);
        accessory("carrete_freno", 1);
        accessory("carrete_escape", 3);
        accessory("escama_guardian", 1);
        accessory("ojo_nautilo", 3);
        accessory("perla_encantada", 3);
        accessory("corazon_marea", 1);
        accessory("geoda_resonante", 1);
        accessory("linterna_abisal", 1);
        accessory("silbato_tiburon", 1);

        for (String id : List.of("lombriz", "masa_pan", "carne_podrida_aderezada", "gamba_arrecife", "baya_luminosa",
                "cebo_helado", "cebo_tinta_brillante", "crema_magma_cebada", "cebo_cobre_cargado", "pepita_cebo")) {
            BAITS.put(id, ITEMS.register(id, () -> new BaitItem(id, new Item.Properties())));
        }
    }

    private ModItems() {
    }

    /**
     * El cofre que se pesca en un sitio: normalmente el del bioma (que recuerda el clima); lloviendo o con tormenta,
     * a veces el exclusivo de ese clima ({@link Cofres#saleCofreDeClima}). {@code azar} es un número al azar entre 0 y 1.
     */
    public static ItemStack cofrePescado(Cofres.Bioma bioma, Cofres.Calidad calidad, Cofres.Banda banda, Cofres.Clima clima, double azar) {
        clima = Cofres.climaEfectivo(bioma, clima);
        if (Cofres.saleCofreDeClima(bioma, clima, azar)) return COFRES_CLIMA.get(clima).get().create(calidad, banda);
        return COFRES.get(bioma).get().create(calidad, banda, clima);
    }

    /** El objeto de un pez del minijuego (los 4 vanilla, el de Minecraft); vacío si ese pez no tiene objeto. */
    public static ItemStack pezStack(String id) {
        Catalogo.Pez pez = Catalogo.pez(id);
        return pez == null ? ItemStack.EMPTY : stackOf(pez.item());
    }

    /** El objeto de un tesoro del catálogo; vacío si no existe o si es un cofre (esos dependen del sitio: {@link #cofrePescado}). */
    public static ItemStack tesoroStack(String id) {
        Catalogo.Tesoro tesoro = Catalogo.tesoro(id);
        return tesoro == null || tesoro.item() == null ? ItemStack.EMPTY : stackOf(tesoro.item());
    }

    private static ItemStack stackOf(String itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static RegistryObject<TieredRodItem> rod(String name, RodTier tier, Item.Properties properties) {
        return ITEMS.register(name, () -> new TieredRodItem(tier, properties));
    }

    private static void accessory(String id, int levels) {
        ACCESSORIES.put(id, ITEMS.register(id, () -> new AccessoryItem(id, levels, new Item.Properties())));
    }

    /** Tier de cualquier caña: las del mod, el suyo; la vanilla (u otra caña de otro mod), madera. */
    @Nullable
    public static RodTier tierOf(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof TieredRodItem rod) return rod.getTier();
        return stack.getItem() instanceof FishingRodItem ? RodTier.MADERA : null;
    }

    /** Objeto de un accesorio equipado, o vacío si ese id no tiene objeto (accesorio de otro datapack). */
    public static ItemStack accessoryStack(String id, int level, int uses) {
        RegistryObject<AccessoryItem> item = ACCESSORIES.get(id);
        return item == null ? ItemStack.EMPTY : item.get().create(level, uses);
    }

    public static ItemStack baitStack(String id, int count) {
        RegistryObject<BaitItem> item = BAITS.get(id);
        return item == null || count <= 0 ? ItemStack.EMPTY : new ItemStack(item.get(), Math.min(64, count));
    }
}

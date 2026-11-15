package com.rolflin.toodeepseas.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/** Carga definiciones JSON de un datapack y guarda el JSON original para enviarlo a los clientes. */
public class DefRegistry<T> extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private final String folder;
    private final BiFunction<String, JsonObject, T> parser;
    private Map<String, T> defs = Map.of();
    private Map<String, String> raw = Map.of();

    public DefRegistry(String folder, BiFunction<String, JsonObject, T> parser) {
        super(new Gson(), folder);
        this.folder = folder;
        this.parser = parser;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, String> json = new LinkedHashMap<>();
        map.forEach((id, el) -> json.put(id.getPath(), el.toString()));
        setFromRaw(json);
        LOG.info("Too Deep Seas: {} definiciones cargadas de '{}'", defs.size(), folder);
    }

    /** Servidor: tras leer el datapack. Cliente: al recibir el paquete de sincronización. */
    public void setFromRaw(Map<String, String> json) {
        Map<String, T> parsed = new LinkedHashMap<>();
        Map<String, String> kept = new LinkedHashMap<>();
        json.forEach((id, text) -> {
            try {
                parsed.put(id, parser.apply(id, JsonParser.parseString(text).getAsJsonObject()));
                kept.put(id, text);
            } catch (RuntimeException e) {
                LOG.error("Too Deep Seas: '{}/{}' no es válido: {}", folder, id, e.getMessage());
            }
        });
        defs = Map.copyOf(parsed);
        raw = Map.copyOf(kept);
    }

    @Nullable
    public T get(String id) {
        return defs.get(id);
    }

    public Collection<String> ids() {
        return defs.keySet();
    }

    public Map<String, String> raw() {
        return raw;
    }
}

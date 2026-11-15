# Too Deep Seas — Forge 1.20.1 (fase 2: núcleo de cañas)

## Qué hay
- `core/` (Java puro): `RodTier` (las 8 cañas), `RodStats` (equipo → parámetros del minijuego, tope de 4 bloques/s), `Loadout` (ranuras), `AccessoryDef` y `BaitDef` (definiciones de datapack).
- `item/`: `TieredRodItem` y el registro de las 7 cañas nuevas (la de madera es la vanilla).
- `data/`: carga de `data/toodeepseas/accessories/*.json` y `baits/*.json`, equipo guardado en el NBT de la caña (`RodData`) y comando de pruebas `/tds`.
- `net/`: envío de las definiciones al cliente al entrar y tras `/reload`.
- `recipe/`: receta de mejora que conserva accesorios, cebo y encantamientos.
- `prototipo/pesca.html`: prototipo web del minijuego.

## Descargar el jar
Pestaña Actions del repositorio → última ejecución en verde → Artifacts → `toodeepseas-jar`. Copia el `.jar` a `mods` (Forge 1.20.1, 47.x).

## Probar en el juego
- Recetas: caña anterior en el centro rodeada de 8 materiales (cobre, hierro, oro o diamante; prismarina: fragmentos en los lados y cristales en las esquinas). Netherita: mesa de herrería.
- `/tds equipar <accesorio> <nivel>`, `/tds cebo <cebo> <cantidad>`, `/tds vaciar` con la caña en la mano (requiere permisos de operador).

## Estado
- Compila en GitHub Actions y pasan las 13 pruebas de `core/`.
- Sin probar dentro del juego.
- Las cañas pescan todavía como la vanilla y usan su textura; los accesorios y cebos aún no son objetos (fase 4).

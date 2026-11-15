# Too Deep Seas — Forge 1.20.1 (fase 4: mesa del pescador)

## Qué hay
- `core/` (Java puro): `RodTier` (las 8 cañas), `RodStats` (equipo → parámetros del minijuego, tope de 4 bloques/s), `Loadout` (ranuras), `AccessoryDef` y `BaitDef` (definiciones de datapack).
- `item/`: las 7 cañas nuevas (la de madera es la vanilla), los 20 accesorios y los 10 cebos como objetos, y el atajo de inventario tipo saco (`RodBundleHandler`).
- `block/` y `menu/`: la mesa del pescador y su ventana; las ranuras son una vista del NBT de la caña.
- `client/`: pantalla de la mesa, descripción de las cañas (`RodTooltip`) y modelos.
- `data/`: carga de `data/toodeepseas/accessories/*.json` y `baits/*.json`, equipo guardado en el NBT de la caña (`RodData{Slots:[{id,lvl,uses}],Baits:[{id,count}]}`) y comando de pruebas `/tds`.
- `net/`: envío de las definiciones al cliente al entrar y tras `/reload`.
- `recipe/`: receta de mejora que conserva accesorios, cebo y encantamientos.
- `prototipo/pesca.html`: prototipo web del minijuego.

## Descargar el jar
Pestaña Actions del repositorio → última ejecución en verde → Artifacts → `toodeepseas-jar`. Copia el `.jar` a `mods` (Forge 1.20.1, 47.x).

## Probar en el juego
- Todo está en la pestaña «Too Deep Seas» del creativo: cañas, mesa del pescador, accesorios (en cada nivel) y cebos.
- **Mesa del pescador** (2 cañas + cuerda encima de 3 tablones): deja una caña en el centro y aparecen sus ranuras. Accesorios arriba y abajo, cebo a los lados, estadísticas a la derecha. Todo queda guardado en la caña al momento.
- **Atajo de inventario**: clic derecho con un accesorio o cebo sobre una caña para meterlo; clic derecho con la caña en el cursor sobre una casilla vacía para sacar lo último.
- Reglas: 1 accesorio por categoría (2 amuletos distintos), nivel máximo según el tier de la caña y tantas ranuras como tenga la caña.
- La descripción de la caña muestra tier, ranuras, equipo con usos y cebo; con Mayús, las estadísticas finales.
- Recetas de caña: la anterior en el centro rodeada de 8 materiales (cobre, hierro, oro o diamante; prismarina: fragmentos en los lados y cristales en las esquinas). Netherita: mesa de herrería.
- `/tds equipar <accesorio> <nivel>`, `/tds cebo <cebo> <cantidad>`, `/tds vaciar` con la caña en la mano (operador).

## Estado
- Compila en GitHub Actions y pasan las pruebas de `core/`.
- Sin probar dentro del juego.
- Las cañas pescan todavía como la vanilla y usan su textura. Las texturas de accesorios, cebos y mesa son provisionales.
- Accesorios y cebos solo se consiguen en creativo: sus recetas, la mejora de nivel y la reparación van en la fase 5.

# Too Deep Seas — Forge 1.20.1 (fase 5: recetas, mejora y reparación)

## Qué hay
- `core/` (Java puro): `RodTier` (las 8 cañas), `RodStats` (equipo → parámetros del minijuego, tope de 4 bloques/s), `Loadout` (ranuras), `AccessoryDef` y `BaitDef` (definiciones de datapack).
- `item/`: las 7 cañas nuevas (la de madera es la vanilla), los 20 accesorios y los 10 cebos como objetos, y el atajo de inventario tipo saco (`RodBundleHandler`).
- `block/` y `menu/`: la mesa del pescador y su ventana; las ranuras son una vista del NBT de la caña.
- `client/`: pantalla de la mesa, descripción de las cañas (`RodTooltip`) y modelos.
- `data/`: carga de `data/toodeepseas/accessories/*.json` y `baits/*.json`, equipo guardado en el NBT de la caña (`RodData{Slots:[{id,lvl,uses}],Baits:[{id,count}]}`) y comando de pruebas `/tds`.
- `net/`: envío de las definiciones al cliente al entrar y tras `/reload`.
- `recipe/`: receta de mejora de caña que conserva accesorios, cebo y encantamientos, y recetas de taller (`toodeepseas:taller`) para la mesa del pescador.
- `loot/`: modificador de botín que añade fragmentos del vacío (ciudades del End, shulkers) y lombrices (pala sobre tierra o hierba).
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
- **Taller** (fila de abajo de la mesa): accesorio + materiales.
  - Mejorar: accesorio de nivel I o II (aunque esté gastado) + los materiales del nivel siguiente → sale el nivel siguiente, nuevo. Los materiales salen en la descripción de cada accesorio.
  - Reparar: accesorio + su material de reparación. Cada unidad devuelve el 25 % de los usos y cuesta 1 nivel de experiencia.
  - Caña del vacío: caña de netherita + 2 conchas de shulker + 4 fragmentos del vacío. Conserva el equipo.
- Recetas: cada accesorio de nivel I y cada cebo se fabrican en la mesa de trabajo (`data/toodeepseas/recipes/accesorios` y `cebos`). El corcho de abedul sale de 2 troncos de abedul en vertical. La lombriz sale con un 8 % al cavar tierra o hierba con pala. Los fragmentos del vacío están en los cofres de las ciudades del End (50 %, 1–3) y los sueltan los shulkers (20 %).
- La Escama de guardián solo entra en cañas de tier 2,5 o más.
- `/tds equipar <accesorio> <nivel>`, `/tds cebo <cebo> <cantidad>`, `/tds vaciar` con la caña en la mano (operador).

## Estado
- Compila en GitHub Actions y pasan las pruebas de `core/` y las de dentro del juego (`test/GearGameTests`: datos, recetas, mesa, taller, atajo y botín).
- Lo visual (ventana de la mesa, descripciones, texturas) solo se ha comprobado compilando: falta verlo en el juego.
- Las cañas pescan todavía como la vanilla y usan su textura. Las texturas de accesorios, cebos y mesa son provisionales.
- La gamba de arrecife y el diente de tiburón tigre no se consiguen todavía en supervivencia: son botín del minijuego y del monstruo.

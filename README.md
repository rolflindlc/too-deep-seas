# Mod de pesca — Forge 1.20.1 (fase 2: núcleo de cañas)

## Qué hay
- `core/` (Java puro, sin Minecraft): `RodTier` (las 8 cañas), `RodStats` (equipo → parámetros del minijuego, con el tope de 4 bloques/s de bajada), `Loadout` (reglas de ranuras), `Stat`/`Modifier` (efectos de accesorios).
- `item/`: `TieredRodItem` y el registro de las 7 cañas nuevas (la de madera es la vanilla).
- `client/`: el modelo «lanzada» de cada caña.
- `src/test`: pruebas JUnit de `RodStats` y `Loadout`.

## Cómo usarlo
Hace falta conexión a internet la primera vez (descarga Gradle 8.8, Forge 47.3.0 y, si falta, un JDK 17).

    gradlew test        # pruebas de la parte pura
    gradlew runClient   # abre Minecraft con el mod

Las cañas están en la pestaña creativa «Herramientas y utilidades». En Linux o macOS: `./gradlew`.

## Estado
- Las pruebas de `core/` pasan (10 de 10).
- El código que usa Minecraft y Forge (`PescaMod`, `ModItems`, `TieredRodItem`, `ClientSetup`) y el `build.gradle` no se han podido compilar donde se escribieron: si `gradlew runClient` da un error, copia el mensaje.
- Las cañas usan la textura de la caña vanilla como provisional y aún no tienen receta (fase 5).
- Todavía pescan como la caña vanilla: el Mixin de `FishingHook` llega con el minijuego en Java (fase 7).

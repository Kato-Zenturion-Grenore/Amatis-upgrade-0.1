# Amatis Upgrade — Fabric 26.3

La mejora de amatista: el nivel siguiente a la netherita, con **x2.9** de sus estadísticas.
Morado y negro, con cristales rosas.

> **Estado:** escrito sin poder compilar (el entorno de creación no tiene acceso a los repositorios de Minecraft).
> Fabric 26.x cambió bastante respecto a 1.21.x, así que es probable que la primera compilación dé algún error.
> Pega el log y se corrige.

## Requisitos
| Qué | Versión |
|---|---|
| Minecraft Java | 26.3 |
| Fabric Loader | 0.20.1 o superior |
| Fabric API | 0.161.0+26.3 (obligatorio: instálalo en `mods`) |
| Java | 25 |
| Gradle | 9.8.0 (lo baja solo el wrapper) |

Si algo no resuelve al compilar, cambia SOLO estas líneas de `gradle.properties`: `loader_version`, `loom_version`,
`fabric_api_version`. (El ejemplo oficial de Fabric para 26.1.2 usa loader 0.20.1 y Loom 1.19-SNAPSHOT.)

## Generar el .jar
- **GitHub (sin instalar nada):** sube el contenido de la carpeta a un repositorio. El workflow `.github/workflows/build.yml`
  compila solo; el .jar se baja desde *Actions → ejecución → Artifacts → amatis-upgrade-jar*.
- **En tu PC (Java 25):** `./gradlew build` → `build/libs/amatis_upgrade-1.0.0.jar` (el `-sources.jar` no se usa).

## Instalación
Copia `amatis_upgrade-1.0.0.jar` **y** Fabric API a la carpeta `mods` de tu instancia de Fabric 26.3.

**Dónde está todo:** en el modo creativo hay una pestaña propia, **Amatis Upgrade** (icono: la guadaña), con los
materiales, las armas, las herramientas, el escudo y la armadura.

**Si no aparece nada, comprueba esto:**
1. En el juego escribe `/give @s amatis_upgrade:amatis_sword`. Si dice que el ítem no existe, el mod no se cargó.
2. Abre `logs/latest.log` de tu carpeta de Minecraft y busca `Amatis Upgrade`. Debe decir
   `[Amatis Upgrade] cargado: items y pestana creativa registrados`. Si no está, o hay líneas con `ERROR`/`Exception`,
   mándalas.
3. Asegúrate de usar el .jar principal (`amatis_upgrade-1.0.0.jar`), no el `-sources.jar`.

## Cadena de crafteo
1. **Plantilla de mejora de amatista:** en un yunque, *plantilla de mejora de netherita* + *fragmento de amatista*. (Consume ambos.)
2. **Amatista resonante descargada:** mesa de crafteo, 8 fragmentos de amatista rodeando un fragmento resonante (echo shard).
3. **Fragmentos de amatista resonante sobrecargada:** en un yunque, *amatista resonante descargada* + *aliento de dragón* → **2 fragmentos**.
4. **Mejoras (mesa de herrería):** plantilla de amatista + pieza de netherita + 1 fragmento sobrecargado → pieza de amatista.
   Sirve para espada, hacha, pico, pala, azada, casco, peto, pantalones y botas. El **escudo** se mejora igual a partir de un escudo vanilla.
   Los encantamientos y el daño de la pieza original se conservan.

## Armas nuevas
| Arma | Receta | Daño total | Velocidad |
|---|---|---|---|
| **Espada pesada** (punta plana) | Yunque: espada de netherita con **Filo V** + 3 fragmentos sobrecargados | 30 (3 × hacha de netherita) | lenta (1.0/s, como un hacha) |
| **Daga** | Mesa de crafteo: dos columnas paralelas `fragmento / lingote de netherita / palo` | 8 (como espada de netherita) | rápida (3.0/s) |
| **Guadaña** | Mesa de crafteo: `fragmento, fragmento, lingote de netherita` arriba, palo al centro, palo abajo a la izquierda | 26.1 (2.9 × tridente) | 1.1/s |

**Golpe pesado:** con la espada pesada y la guadaña, cada 3 golpes seguidos el tercero es un golpe contra el suelo
(x1.5 de daño) que lanza una pequeña onda de choque (25 % del daño, empuja y daña a los cercanos).

**Escudo de amatista:** al mantener el botón derecho levanta una barrera de 3x3 alrededor del jugador que **anula el daño
de cualquier dirección** (menos vacío y daño que ignora la invulnerabilidad). Cada golpe gasta durabilidad.

## Estadísticas x2.9 (sobre netherita)
| | Netherita | Amatista |
|---|---|---|
| Durabilidad herramientas | 2031 | 5890 |
| Velocidad de minería | 9 | 26.1 |
| Daño espada / hacha / pico / pala / azada | 8 / 10 / 6 / 6.5 / 1 | 23.2 / 29 / 17.4 / 18.85 / 2.9 |
| Armadura casco/peto/pantalones/botas | 3/8/6/3 | 9/23/17/9 |
| Dureza | 3 | 8.7 |
| Resistencia al empuje | 0.1 | 0.29 |
| Durabilidad armadura (multiplicador) | 37 | 107 |

Minecraft limita la armadura efectiva a 30 y la dureza a 20, así que el set completo da protección máxima y los
puntos de sobra no suman.

## Configuración
`config/amatis_upgrade.json` (se crea al primer arranque): fragmentos por receta de yunque, coste de experiencia,
cada cuántos golpes es el golpe pesado, multiplicador de daño, radio/daño/empuje de la onda, barrera del escudo.

## Cómo está hecho (por si hay que corregir algo)
- **Yunque:** Fabric no tiene evento de yunque, así que `mixin/AnvilMenuMixin.java` añade las 3 recetas del mod y deja
  el resto del yunque vanilla igual. La lógica está en `events/AnvilLogic.java`.
- **Escudo y golpe pesado:** `ServerLivingEntityEvents` de Fabric API (`events/AmatisEvents.java`).
- **Pestaña creativa:** `registry/ModItems.java` (`FabricCreativeModeTab`).
- **Animación al golpear:** la vanilla (los ítems son armas normales; no hay animaciones personalizadas).
- **Config:** JSON propio con Gson (`config/AmatisConfig.java`).

## Limitaciones conocidas / pendientes
- **Texturas:** son dibujos **originales** hechos por script con la paleta de tu referencia; **no son** el arte de AlanizMc9.
  Si tienes permiso del artista (o tus propios sprites), reemplaza los PNG de
  `src/main/resources/assets/amatis_upgrade/textures/item/` y `.../textures/entity/equipment/` conservando los nombres.
  Para regenerar los de este proyecto: `python3 tools/generate_assets.py` (requiere numpy y Pillow).
- **Sin resistencia al fuego/lava** (la netherita la tiene): quedó fuera porque no pude comprobar esa API en 26.x.
- **Plantilla:** es un ítem normal; en la mesa de herrería funciona, pero no muestra los iconos y textos descriptivos de las
  plantillas vanilla.
- **Escudo:** no tiene animación propia de bloqueo; se ve el sprite y las partículas de la barrera. No usa el sistema de
  escudo vanilla, así que no se desactiva con hachas.
- **Espada pesada:** exige Filo ≥ V en la espada de netherita (configurable). Conserva los encantamientos.
- El golpe pesado aplica el daño extra como un segundo impacto inmediato.
- El yunque pide que la plantilla de netherita y la amatista descargada vayan de a una (stack de 1).

# Glossary

Modding has a lot of jargon, much of it historical. This is what the words mean
in 2026.

---

**Access Transformer (AT)** — A config line that widens the visibility of a
private Minecraft member at load time. Global to the whole instance. See
[06](./06-mixins-and-access-transformers.md).

**Architectury** — A cross-loader toolkit providing `@ExpectPlatform` and a
unified API. An alternative to this repo's `ServiceLoader` approach.

**Blaze3D** — Minecraft's rendering abstraction layer. Since 26.2 it fronts both
an OpenGL and a Vulkan backend; direct GL calls no longer work.

**Brigadier** — Mojang's command-parsing library. What you use to build
`/yourcommand` with arguments, suggestions, and permissions.

**Common** — In a multiloader repo, the subproject holding loader-agnostic code.
Here it compiles against plain Minecraft via NeoForm.

**Data pack / Resource pack** — Vanilla's built-in extension mechanisms for
server data (recipes, loot, tags, worldgen) and client assets (textures, models,
sounds). Mods ship both inside their jar.

**Datagen** — Generating JSON (recipes, tags, loot tables, models) from Java code
rather than writing it by hand. `./gradlew :neoforge:runData`.

**Dedicated server** — The `server.jar` process. Same as *physical server*.

**Dist** — NeoForge's enum for the physical side: `Dist.CLIENT` or
`Dist.DEDICATED_SERVER`.

**Fabric** — A lightweight mod loader. Fast to update, popular for performance
and quality-of-life mods.

**Fabric API** — The shared library layered on Fabric Loader that provides
events, registries, and networking helpers. Fabric Loader alone provides very
little; almost every Fabric mod needs this.

**FML** — Forge Mod Loader. The loading layer inside NeoForge; you meet it as
`FMLEnvironment`, `FMLPaths`, and `javafml`.

**Game bus / Mod bus** — NeoForge's two event buses. Mod bus = startup and
registration; game bus = gameplay. Using the wrong one means your handler never
fires. See [01 §3](./01-architecture.md#3-neoforges-two-event-buses).

**GameTest** — An automated test that runs inside a real server against a real
world. See [07](./07-testing.md).

**Intermediary** — *Historical.* Stable intermediate names Fabric used so mods
survived obfuscation changes between builds. Removed with obfuscation in 26.1.

**Jar-in-jar (JiJ)** — Bundling a library inside your mod jar so users do not
install it separately. Both loaders support it. A common source of duplicate-class
conflicts; use sparingly.

**Loom** — The Gradle plugin for Fabric development. Two plugin ids:
`net.fabricmc.fabric-loom` (unobfuscated, 26.1+) and
`net.fabricmc.fabric-loom-remap` (obfuscated, ≤1.21.11).

**Logical side** — Which half of the game code is running: the logical server
decides, the logical client displays. Both run inside a singleplayer client. See
[02](./02-sides-client-vs-server.md).

**Mappings** — *Mostly historical.* Translations from obfuscated names to
readable ones. Yarn (Fabric's), MCP (old Forge), and Mojang's official mappings.
Unnecessary from 26.1 since Minecraft ships unobfuscated.

**Mixin** — A library that rewrites Minecraft's bytecode at load time to insert
your code. Both loaders ship it. See
[06](./06-mixins-and-access-transformers.md).

**ModDevGradle (MDG)** — The modern Gradle plugin for NeoForge development. Its
"vanilla mode" (`neoFormVersion`) is what gives `common/` plain Minecraft.

**Mod loader** — The layer that loads mods into Minecraft: Fabric, NeoForge,
Forge, Quilt. Jars are not cross-compatible between them.

**NeoForge** — The actively developed successor to Forge, forked in 2023. The
default choice for content and tech mods on modern versions.

**NeoForm** — Decompiled, source-mapped Minecraft with **no** loader attached.
Used by `common/` so shared code cannot accidentally depend on a loader.

**Obfuscation** — *Historical.* Mojang used to ship Minecraft with meaningless
class and method names. **Removed in 26.1** — Java Edition now ships with real
names. This is the largest change in modding since Fabric existed. See
[05 §4](./05-upgrading-minecraft-versions.md#4-the-2026-unobfuscation-cliff-12111--261).

**Parchment** — Community-supplied parameter names layered on Mojang mappings.
Largely obsolete now that official parameter names ship with the game.

**Payload** — A custom network packet. Registered per-loader; must be marked
*optional* if unmodded clients should still be able to connect. See
[08](./08-networking.md).

**Physical side** — Which program is running: the launcher client, or the
dedicated server. Determines whether `net.minecraft.client` classes exist.

**Refmap** — *Historical.* A generated mapping file that let mixins written
against dev names find obfuscated runtime members. Unnecessary since 26.1;
delete the field from your mixin config.

**Registry** — Minecraft's central catalogues of blocks, items, entities, etc.
Registration must happen at the right point in startup, identically on both
sides, or clients and servers disagree about IDs.

**SavedData** — Minecraft's mechanism for per-world data that persists across
restarts. Where mod state belongs if it must survive a reboot.

**Split source sets** — Loom's `splitEnvironmentSourceSets()`, which creates a
`src/client/java` compiled against client-only Minecraft. Makes side violations a
compile error. Fabric only.

**Stonecutter** — A Gradle plugin for maintaining several Minecraft versions from
one source tree using preprocessor comments. Powerful, and a real maintenance
cost.

**Yarn** — Fabric's community mapping set. No longer maintained for versions
after 1.21.11.

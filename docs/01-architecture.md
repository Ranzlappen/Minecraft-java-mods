# Architecture

Why this repo is shaped the way it is, and what the constraints buy you.

---

## 1. Three subprojects

```
        ┌──────────────┐
        │   common/    │   plain Minecraft (via NeoForm), no loader APIs
        └──────┬───────┘
               │  sources shared into both
      ┌────────┴────────┐
      ▼                 ▼
┌───────────┐     ┌────────────┐
│  fabric/  │     │ neoforge/  │
└───────────┘     └────────────┘
      │                 │
      ▼                 ▼
 fabric jar       neoforge jar
```

**`common/`** compiles against plain Minecraft and nothing else — no Fabric API,
no NeoForge event bus. That restriction is the entire value: if it compiles
there, it is provably portable.

**`fabric/`** and **`neoforge/`** hold entrypoints, event wiring, and the
loader-specific implementations of the platform interfaces.

### How common's code reaches the loader jars

Each loader subproject adds common's source directories to its own source set:

```groovy
sourceSets.main {
    java.srcDir project(':common').file('src/main/java')
    resources.srcDir project(':common').file('src/main/resources')
}
```

Common's *sources* are compiled by each loader, rather than each loader
depending on common's compiled jar. This is deliberate:

- No cross-project publication leakage — nothing bogus ends up in the POM.
- Each loader compiles common against its own view of Minecraft, so an
  incompatibility surfaces at compile time.
- Mixins declared in common are processed by the loader's annotation processor,
  which is what makes them actually apply.

Cost: common is compiled three times (once standalone, once per loader). It is
fast and it buys the guarantees above.

The standalone `common` build still matters — it is the thing that *proves* the
shared code has no loader dependencies. If you delete it, nothing stops someone
importing a Fabric class into shared code and only discovering it when the
NeoForge build breaks.

---

## 2. The platform abstraction

Common code sometimes needs an answer only the loader knows. That goes through
one interface:

```java
public interface IPlatformHelper {
    String getPlatformName();
    boolean isModLoaded(String modId);
    boolean isDevelopmentEnvironment();
    Path getConfigDirectory();
    boolean isDedicatedServer();
}
```

Each loader implements it, and registers the implementation through a plain JDK
`ServiceLoader` file:

```
fabric/src/main/resources/META-INF/services/com.example.examplemod.platform.IPlatformHelper
  -> com.example.examplemod.fabric.FabricPlatformHelper

neoforge/src/main/resources/META-INF/services/com.example.examplemod.platform.IPlatformHelper
  -> com.example.examplemod.neoforge.NeoForgePlatformHelper
```

Common calls it through `Services.PLATFORM`.

### Adding a method

1. Add it to `IPlatformHelper`.
2. Implement it in both loaders.

Miss one and that subproject fails to compile. The build is the enforcement.

### Keeping the abstraction thin

Every method here is code written twice and tested twice. Before adding one, ask
whether plain Minecraft classes can express it — they usually can. A platform
helper with thirty methods means the split is in the wrong place.

### Why not Architectury?

[Architectury](https://docs.architectury.dev/) does the same job with
`@ExpectPlatform`, and it is a fine choice — especially for large content mods
that want a unified registry API.

This template uses `ServiceLoader` instead because:

- No extra Gradle plugin, no runtime library for users to install.
- Missing implementations fail at **compile** time, not at runtime.
- It is plain JDK behaviour, so there is nothing loader-specific to debug.

If you outgrow it, switching to Architectury later is mechanical — the seam is
already in the right place.

---

## 3. NeoForge's two event buses

The most common NeoForge beginner bug: a handler that never fires because it is
on the wrong bus.

| | Mod bus | Game bus |
| --- | --- | --- |
| Purpose | Lifecycle: registration, setup, datagen | Gameplay: ticks, commands, players, blocks |
| Reached via | The `IEventBus` passed to your `@Mod` constructor | `@EventBusSubscriber`, or `NeoForge.EVENT_BUS` |
| Fires | Once, during startup | Continuously, during play |
| Example events | `RegisterEvent`, `FMLCommonSetupEvent`, `RegisterPayloadHandlersEvent` | `RegisterCommandsEvent`, `PlayerEvent.*`, `ServerTickEvent` |

In this repo: `ExampleModNeoForge`'s constructor takes the mod bus, and the
nested `GameEvents` class carries `@EventBusSubscriber` for the game bus.

Fabric has no equivalent split — every event is a callback object you register
on directly, which is simpler but gives you less structure.

---

## 4. Where state lives

`ExampleMod.playtime()` is a static singleton holding server-side state. That is
correct here because there is exactly one logical server per process.

Scaling up:

| Need | Use |
| --- | --- |
| Per-process, transient | A static field, as here |
| Per-world, must survive restarts | `SavedData` attached to the overworld |
| Per-player, must survive restarts | `SavedData` keyed by UUID, or player NBT / attachments |
| Per-entity or per-block | Data attachments (NeoForge) / component API (Fabric) |

Anything that must persist should **not** be a static field. Static state also
leaks between singleplayer worlds in the same session unless you clear it —
which is why this repo resets the tracker on `SERVER_STOPPED`.

---

## 5. Dropping a loader

Only shipping for NeoForge?

1. `rm -rf fabric/`
2. Remove `include 'fabric'` from `settings.gradle`
3. Remove the Loom plugin line from the root `build.gradle`
4. Remove the Fabric job from `.github/workflows/build.yml`

Nothing else changes. Keep `common/` even with one loader — the separation stays
useful for testing, and it keeps the door open.

---

## 6. Adding a third loader

The shape generalises. For a new loader (Quilt, or a future one):

1. Create `quilt/` with its own `build.gradle`.
2. Add the common source dirs to its source set.
3. Implement `IPlatformHelper` and add the `META-INF/services` file.
4. Write the entrypoint; call `ExampleMod.init()`.
5. `include 'quilt'` in `settings.gradle`.
6. Add a CI job and a `publishMods` block.

No common code changes. That is the payoff for the discipline.

---

## 7. Deliberate non-choices

| Not used | Why |
| --- | --- |
| Architectury | Extra plugin + runtime dep; §2 |
| Kotlin | Needs a loader-specific Kotlin runtime shipped to users; Java keeps the template dependency-free |
| A config library | Minecraft ships GSON; loader config APIs cannot live in common |
| Jar-in-jar bundling | Only add it when you actually have a library dependency — it is a common source of duplicate-class conflicts |
| Stonecutter / multi-version source | Real cost, only pays off with several supported versions at once ([05 §3](./05-upgrading-minecraft-versions.md#3-should-i-keep-supporting-the-old-version)) |

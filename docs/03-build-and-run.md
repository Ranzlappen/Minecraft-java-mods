# Building and running

---

## Prerequisites

| Need | Notes |
| --- | --- |
| Git | — |
| A JDK | **Any** modern JDK to bootstrap Gradle. Gradle downloads the exact JDK the build needs (Java 25 here) via the `foojay-resolver` plugin |
| ~8 GB free disk | Minecraft, its dependencies, and the decompiled sources |
| IntelliJ IDEA | Strongly recommended. Eclipse support in the modding toolchain is poor and both loaders say so |

Nothing else. Do not install Forge, Fabric, or Minecraft by hand — Gradle
handles all of it.

---

## First build

```bash
./gradlew build
```

The first run takes **5–20 minutes**: Gradle downloads a JDK, fetches Minecraft,
and decompiles it. Later builds take seconds. If it looks frozen at
"Decompiling", it is not — that step is single-threaded and slow.

Output jars:

```
fabric/build/libs/examplemod-fabric-0.1.0+mc26.2.jar
neoforge/build/libs/examplemod-neoforge-0.1.0+mc26.2.jar
```

Also produced: `-sources.jar` and `-javadoc.jar` (for other developers), and on
Fabric a `-dev.jar` (build intermediate — never distribute it).

---

## Run configurations

```bash
./gradlew :fabric:runClient       # dev client, mod loaded
./gradlew :fabric:runServer       # dev dedicated server
./gradlew :neoforge:runClient
./gradlew :neoforge:runServer
./gradlew :neoforge:runData       # regenerate datagen output
```

Working directories are `fabric/run/client`, `fabric/run/server`, etc. — all
gitignored. Delete them any time to get a clean world.

In IntelliJ these appear as run configurations after the first Gradle sync, so
you can launch them with the debugger attached. **Use the debugger.** Breakpoints
in Minecraft code work, and stepping through vanilla is the fastest way to learn
how a system works.

### The EULA

A dedicated server refuses to start until `eula=true` exists in `eula.txt`. On
first `runServer` the file is created and the server exits. Set it and re-run:

```bash
echo "eula=true" > fabric/run/server/eula.txt
```

This only applies to the throwaway dev server in this repo.

### Connecting a real client to the dev server

Useful for testing whether unmodded clients work (see
[02 §6](./02-sides-client-vs-server.md#6-the-four-installation-shapes)):

1. `./gradlew :fabric:runServer`
2. Launch a normal Minecraft client on the matching version
3. Multiplayer → Direct Connect → `localhost`

---

## Where versions come from

Everything lives in `gradle.properties`. Nothing is duplicated elsewhere.

| Property | Source of truth |
| --- | --- |
| `minecraft_version` | [Version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) |
| `java_version` | `javaVersion.majorVersion` for that release in the manifest |
| `loom_version`, `loader_version`, `fabric_api_version` | <https://fabricmc.net/develop> |
| `neoforge_version` | <https://projects.neoforged.net/neoforged/neoforge> |
| `neoform_version` | <https://projects.neoforged.net/neoforged/neoform> |
| `moddev_version` | <https://projects.neoforged.net/neoforged/moddevgradle> |

Metadata files (`fabric.mod.json`, `neoforge.mods.toml`) contain `${...}`
placeholders that the root `build.gradle`'s `processResources` block substitutes
at build time. **Never hard-code a version in those files** — it will silently
disagree with the build.

---

## Useful flags

```bash
./gradlew build --refresh-dependencies   # after changing any dependency version
./gradlew clean build                    # when Gradle is confused
./gradlew build --scan                   # publish a build report to diagnose slowness
./gradlew build --stacktrace             # full stack on failure
./gradlew --stop                         # kill daemons; fixes odd stale-state bugs
```

---

## Performance

The build is configured with `org.gradle.parallel=true` and
`org.gradle.caching=true` in `gradle.properties`.

**Configuration cache is off** because Fabric Loom is not yet compatible with it
inside IntelliJ ([loom#1349](https://github.com/FabricMC/fabric-loom/issues/1349)).
If you drop the Fabric subproject you can turn it on and builds get noticeably
faster.

Out of memory during decompilation? Raise the heap:

```properties
org.gradle.jvmargs=-Xmx6G
```

---

## Reproducible builds

Archive tasks set `preserveFileTimestamps = false` and
`reproducibleFileOrder = true`, so the same source produces a byte-identical
jar. That makes build caching effective and lets anyone verify that a published
jar matches its source tag.

The Gradle distribution itself is pinned by SHA-256 in
`gradle/wrapper/gradle-wrapper.properties`. Re-pin it whenever you bump Gradle —
[05 §2 step 4](./05-upgrading-minecraft-versions.md#step-4--bump-gradle-if-the-loaders-ask-for-it).

---

## IDE notes

**IntelliJ IDEA.** Open the repo root as a project; do not import
`fabric/build.gradle` on its own. Set the Gradle JVM and project SDK to the
`java_version` in `gradle.properties`. Re-sync Gradle after editing any build
script.

**VS Code.** Workable with the Java extension pack, but run configurations and
decompiled-source navigation are much worse. Fine for editing, painful for
debugging.

**Eclipse.** Both loaders document it as unsupported. Do not.

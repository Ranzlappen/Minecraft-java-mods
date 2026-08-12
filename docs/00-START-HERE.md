# Start here

You have never written a Minecraft mod, or you have and you want to know how
*this* repo is arranged. Either way, read this page top to bottom once. It is
the map; everything else is a detail.

---

## 1. Fifteen-minute first run

Do this before reading anything else. Seeing the game boot with your code in it
makes the rest concrete.

```bash
git clone https://github.com/Ranzlappen/Minecraft-java-mods.git my-mod
cd my-mod

# Boot a dev client with the mod loaded. First run takes 5-15 minutes:
# Gradle downloads a JDK, downloads Minecraft, and decompiles it.
./gradlew :fabric:runClient
```

You do **not** need Java 25 installed. Gradle downloads the right JDK itself
(that is the `foojay-resolver` plugin in `settings.gradle`).

Once in game: create a world and look at the top-left corner of the HUD — a
"Saturation: X.X" line is already there. Mount a horse and a "Speed: X.X
blocks/s" line joins it. Both come from
`fabric/src/client/java/com/statusoverlay/mod/fabric/client/StatusOverlayFabricClient.java`
(the NeoForge equivalent lives at
`neoforge/src/main/java/com/statusoverlay/mod/neoforge/client/StatusOverlayNeoForgeClient.java`).

Now try the other loader and a dedicated server:

```bash
./gradlew :neoforge:runClient
./gradlew :fabric:runServer     # then connect to localhost from a real client
```

---

## 2. Make it yours

One script renames everything — mod id, package, group, directories, and the
`META-INF/services` files that no refactoring tool would catch:

```bash
./scripts/rename-mod.sh --id coolmod --name "Cool Mod" \
                        --group com.yourname.coolmod --author "Your Name"
```

Then open `gradle.properties` and fill in the description, URLs, and licence.
Everything else reads from there.

Commit that as its own commit before writing features. It touches many files and
you do not want it tangled with real work.

---

## 3. Where to put code — the docking points

This is the question that stalls beginners: *"I want to do X, where does the
code go?"*

### The five entrypoints

| File | Runs when | Runs where |
| --- | --- | --- |
| `common/.../StatusOverlayMod.java` → `init()` | Startup, once | Both sides, both loaders |
| `fabric/.../StatusOverlayFabric.java` | Startup | Fabric, both sides |
| `fabric/src/client/.../StatusOverlayFabricClient.java` | Startup | Fabric, client only |
| `neoforge/.../StatusOverlayNeoForge.java` | Startup | NeoForge, both sides |
| `neoforge/.../client/StatusOverlayNeoForgeClient.java` | Startup | NeoForge, client only |

**Default answer: write it in `common/`, call it from the loader entrypoints.**

### "I want to…" → go here

| Goal | Start in | Read |
| --- | --- | --- |
| Add a command | `common/.../command/` — a plain Brigadier tree, called from both loader entrypoints | [01](./01-architecture.md) |
| React to a player joining / dying / breaking a block | The loader entrypoints; call into `common/` | [01](./01-architecture.md) |
| Add a config option | A plain Java class in `common/`, loaded by each loader's `IPlatformHelper#getConfigDirectory()` | — |
| Add an item, block, or entity | `common/` registration + a loader hook | [01](./01-architecture.md) |
| Change vanilla behaviour with no event for it | `common/.../mixin/` — see `MinecraftServerMixin` for the pattern | [06](./06-mixins-and-access-transformers.md) |
| Reach a private vanilla field | An `@Accessor` mixin, or an Access Transformer | [06](./06-mixins-and-access-transformers.md) |
| Send data between server and client | Payload type in `common/`, handlers per side | [08](./08-networking.md) |
| Draw something on screen | Client-only source set / class — copy `StatusOverlayFabricClient` / `StatusOverlayNeoForgeClient` | [02](./02-sides-client-vs-server.md) |
| Generate recipes, tags, loot tables | `neoforge/` datagen, `./gradlew :neoforge:runData` | [07](./07-testing.md) |
| Write a test | `common/src/test/java/` | [07](./07-testing.md) |
| Ship a release | Tag it; CI does the rest | [04](./04-versioning-and-releases.md), [09](./09-publishing.md) |
| Port to a new Minecraft version | — | [05](./05-upgrading-minecraft-versions.md) |

---

## 4. The one decision to make early

**Who has to install your mod — the server, the client, or both?**

It shapes your metadata, your networking, and your support burden. There are
four answers and they are laid out in
[`02-sides-client-vs-server.md` §6](./02-sides-client-vs-server.md#6-the-four-installation-shapes).

Short version for a *server* mod: keep everything on the server, talk to clients
using vanilla packets (chat, titles, scoreboards, display entities, resource
packs), and you get to support unmodded clients. That is dramatically less work
than a matched-pair mod.

Short version for a *client* mod — like the Status Overlay HUD this template
ships with: write nothing in `common/` beyond what genuinely has no Minecraft
imports, put everything in the client entrypoints, and set
`"environment": "client"` in `fabric.mod.json` so a dedicated server refuses to
even load the jar rather than loading it and doing nothing.

---

## 5. Repository layout

```
gradle.properties         Every version number. The only file an upgrade touches.
settings.gradle           Which subprojects exist.
build.gradle              Config shared by all three subprojects.

common/                   Loader-agnostic code. Most of your mod lives here.
  src/main/java/            Game logic, commands, config, mixins.
  src/main/resources/       Mixin config, access transformers, assets.
  src/test/java/            Plain JUnit tests. No game needed.

fabric/                   Fabric entrypoints + Fabric-only code.
  src/main/java/            Both sides.
  src/client/java/          Client only — enforced by the compiler.
  src/main/resources/       fabric.mod.json, services file.

neoforge/                 NeoForge entrypoints + NeoForge-only code.
  src/main/java/            Both sides; .client package is client-only by convention.
  src/main/resources/       neoforge.mods.toml, services file.

docs/                     This documentation.
scripts/                  rename-mod.sh and other helpers.
.github/workflows/        CI, release, version bump, security.
```

---

## 6. Everyday commands

```bash
./gradlew build                  # build every jar; output in */build/libs/
./gradlew :common:test           # fast unit tests, no game
./gradlew :fabric:runClient      # dev client
./gradlew :fabric:runServer      # dev dedicated server
./gradlew :neoforge:runClient
./gradlew :neoforge:runServer
./gradlew :neoforge:runData      # regenerate datagen output
./gradlew clean                  # when Gradle gets confused
```

Distributable jars land in `fabric/build/libs/` and `neoforge/build/libs/`.
Ignore the `-sources` and `-dev` jars; the plain one is what users install.

---

## 7. Learning resources worth your time

**Official documentation** — start here, they are genuinely good:

- [NeoForge docs](https://docs.neoforged.net/) — the best conceptual writing in
  modding. Read it even if you use Fabric; the vanilla parts apply either way.
- [Fabric docs](https://docs.fabricmc.net/)
- [NeoForge migration primers](https://docs.neoforged.net/primer/docs/) —
  version-to-version vanilla changes

**Reading Minecraft's own code** — as of 26.1, Minecraft ships unobfuscated, so
your IDE can decompile and navigate it directly. This is the highest-leverage
thing you can do: when you wonder how a vanilla feature works, go read it.
Ctrl-click any `net.minecraft` class after the first Gradle sync.

**Community**

- [NeoForge Discord](https://discord.neoforged.net/) — `#modder-support`
- [Fabric Discord](https://discord.gg/v6v4pMv) — `#modding-help`
- Read other mods' source. Most good mods are open source, and jar files can be
  opened in your IDE.

---

## 8. Things that will bite you

| Symptom | Cause |
| --- | --- |
| Works in singleplayer, crashes on the server | Client class reached from common code — [02](./02-sides-client-vs-server.md) |
| Change does not appear in game | Edited the wrong side, or a stale build. `./gradlew clean` |
| `ServiceConfigurationError` | The `META-INF/services` file is stale after a rename |
| Mixin silently does nothing | Injection point not found. Check the log for mixin warnings |
| Nothing happens on a NeoForge event | Registered on the mod bus instead of the game bus, or the reverse |
| First build takes forever | Normal — Minecraft is being downloaded and decompiled. Once |

Full list: [`10-troubleshooting.md`](./10-troubleshooting.md).

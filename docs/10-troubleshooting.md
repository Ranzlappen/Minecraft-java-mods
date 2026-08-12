# Troubleshooting

Symptom first. Most of these are a five-minute fix once you recognise them.

---

## Build problems

### First build takes forever / looks frozen

Normal. Gradle is downloading a JDK, downloading Minecraft, and decompiling it.
Decompilation is single-threaded and slow. 5–20 minutes on the first run;
seconds afterwards.

### `OutOfMemoryError` during the build

Raise the Gradle heap in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx6G
```

Then `./gradlew --stop` to kill the old daemon.

### `Could not resolve net.fabricmc:fabric-api:...`

The `fabric_api_version` suffix must match `minecraft_version` exactly
(`0.156.0+26.2`, not `0.156.0`). Check <https://fabricmc.net/develop>.

### `Plugin [id: 'net.fabricmc.fabric-loom'] was not found`

Either `loom_version` does not exist, or you are on the wrong plugin id for your
Minecraft version:

- `net.fabricmc.fabric-loom` → 26.1+ (unobfuscated)
- `net.fabricmc.fabric-loom-remap` → 1.21.11 and older (obfuscated)

See [05 §4](./05-upgrading-minecraft-versions.md#4-the-2026-unobfuscation-cliff-12111--261).

### `Unresolved reference` to Minecraft classes after a version bump

```bash
./gradlew --refresh-dependencies clean build
```

If that does not help, the class genuinely moved. Check the
[migration primer](https://docs.neoforged.net/primer/docs/) for that version.

### Gradle wants a Java version I do not have

It should download it. If it does not, the `foojay-resolver` plugin is missing
from `settings.gradle`, or your network blocks the download. Install the JDK
manually and point `org.gradle.java.home` at it.

### `The mappings block is not supported` / `mod* configurations do not exist`

You are using the unobfuscated Loom plugin with an obfuscated-era build script.
Remove the `mappings` line and change `modImplementation` → `implementation`.

---

## Runtime problems

### Works in singleplayer, crashes on a dedicated server

Almost always a client class reached from common code. The stack trace names it:

```
java.lang.NoClassDefFoundError: net/minecraft/client/Minecraft
```

Fix: move that code into the client source set (Fabric) or a
`@Mod(dist = Dist.CLIENT)` class (NeoForge). Read
[`02-sides-client-vs-server.md`](./02-sides-client-vs-server.md).

Prevention: `./gradlew :neoforge:runServer` before every release. CI's
`side-safety` job also greps for `net.minecraft.client` under `common/`.

### `ServiceConfigurationError` / `Failed to load service for IPlatformHelper`

The `META-INF/services/...IPlatformHelper` file is missing, or names a class that
was renamed. That file is plain text and no refactoring tool updates it.

Check both:
```
fabric/src/main/resources/META-INF/services/com.statusoverlay.mod.platform.IPlatformHelper
neoforge/src/main/resources/META-INF/services/com.statusoverlay.mod.platform.IPlatformHelper
```

The filename must be the **fully-qualified interface name**, and the contents the
fully-qualified implementation name.

### My mixin does nothing

1. Is the class listed in `statusoverlay.mixins.json`?
2. Is it in the right list (`mixins` / `client` / `server`)?
3. Is `require = 0` hiding a failure? Remove it temporarily.
4. Search the log:
   ```bash
   grep -iE "mixin" fabric/run/server/logs/latest.log | grep -iE "error|warn|fail"
   ```

The template's `MinecraftServerMixin` logs at debug on server construction. No
line means the mixin pipeline itself is broken, not your mixin.

### `Critical injection failure: ... could not find any targets matching 'foo'`

Mojang renamed or removed the method. Ctrl-click the target class in your IDE —
Minecraft is decompiled and readable — and find the new name.

### A NeoForge event never fires

Wrong bus. Registration/lifecycle events are on the **mod** bus (the `IEventBus`
in your `@Mod` constructor); gameplay events are on the **game** bus
(`@EventBusSubscriber` or `NeoForge.EVENT_BUS`). See
[01 §3](./01-architecture.md#3-neoforges-two-event-buses).

### Unmodded clients cannot connect to my server

You registered a required payload. Either register nothing, or use
`PayloadRegistrar#optional()` and guard sends with
`player.connection.hasChannel(TYPE)`. See [`08-networking.md`](./08-networking.md).

### Config changes have no effect

If your mod loads config once at startup (the usual pattern — see
`docs/00-START-HERE.md`'s "Add a config option" row), restarting is required.
For live reload, add a `/reload`-style command that calls your config class's
`load()` again.

### Changes do not appear in game

Stale build. `./gradlew clean` then re-run. If you edited resources only, the run
task may not have re-processed them.

---

## Release problems

### The release workflow did not run

It triggers on `v*` tags only. Pushing the commit is not enough:

```bash
git push origin main --follow-tags
```

Check the tag exists on the remote: `git ls-remote --tags origin`.

### Modrinth upload rejected

- The version already exists — every upload needs a unique version string.
- `MODRINTH_TOKEN` lacks the **Create versions** scope.
- `modrinth_id` is the URL slug instead of the project ID.

### CurseForge rejects the game version

CurseForge maintains its own list of valid game-version strings. A brand-new
Minecraft release may not be listed for a day or two.

---

## Getting unstuck

1. **Read the full stack trace**, not the first line. The cause is usually in a
   `Caused by:` further down.
2. **Search the exact error string.** Someone has hit it.
3. **Ctrl-click into Minecraft.** Since 26.1 the source is readable — this is now
   the fastest way to answer "what does this method actually do".
4. **Ask, with detail.** [NeoForge Discord](https://discord.neoforged.net/)
   `#modder-support`, [Fabric Discord](https://discord.gg/v6v4pMv)
   `#modding-help`. Post the full log to a paste site, say which Minecraft and
   loader version, and what you already tried.

### Collecting a log to share

```bash
./gradlew :neoforge:runServer 2>&1 | tee /tmp/server.log
```

Share `neoforge/run/server/logs/latest.log` or the crash report from
`run/server/crash-reports/`. Do not paste a screenshot of a terminal.

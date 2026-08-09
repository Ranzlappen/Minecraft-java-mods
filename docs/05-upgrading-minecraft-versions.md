# Upgrading to a new Minecraft version

> Minecraft breaks internal APIs on every release. There is no "just bump the
> number" path, and there never has been. This document turns the upgrade from
> an open-ended slog into a checklist.

---

## 1. Know which era you are in

Two things changed in 2026 that split modding history in half. If you are moving
across that line, read §4 before anything else.

### Version numbering

| Era | Format | Example | Notes |
| --- | --- | --- | --- |
| Until Dec 2025 | `1.MAJOR.MINOR` | `1.21.11` | `1.21.11` was the last of these |
| From Mar 2026 | `YY.DROP[.PATCH]` | `26.1`, `26.2`, `26.3` | Year + which "game drop" of that year |

`26.2` is **newer** than `1.21.11`, despite sorting lower as a string. This bites
version-comparison code, sorting in scripts, and Maven ranges. Old versions were
not renumbered.

### Obfuscation removal

Before 26.1, Mojang shipped Minecraft with obfuscated class and method names
(`a()`, `b()`, `cfk`). Modding depended on community mapping projects (Yarn,
MCP, Mojang's own official mappings) to turn those back into readable names, and
on *intermediary* names to keep mods stable across builds.

**From 26.1, Minecraft ships unobfuscated.** Consequences:

- Intermediary is gone (Fabric's meta API reports version `0.0.0` for it).
- Yarn is no longer maintained for new versions.
- Refmaps are unnecessary — Mixin's dev names and runtime names are identical.
- Crash reports name real methods.
- Loaders can support a new version almost immediately.

---

## 2. The routine upgrade (within one era)

For example `26.2` → `26.3`, or `1.21.4` → `1.21.5`. Budget an afternoon.

### Step 1 — Branch

```bash
git checkout -b chore/mc-26.3
```

Never upgrade on `main`. You will want to ship a patch for the old version while
the port is in progress.

### Step 2 — Look up the new numbers

| Value | Where to look |
| --- | --- |
| `minecraft_version` | [minecraft.net](https://www.minecraft.net/en-us/download/server) or the [version manifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json) |
| `java_version` | The manifest's `javaVersion.majorVersion` for that release |
| `loom_version`, `loader_version`, `fabric_api_version` | <https://fabricmc.net/develop> |
| `neoforge_version` | <https://projects.neoforged.net/neoforged/neoforge> |
| `neoform_version` | <https://projects.neoforged.net/neoforged/neoform> |
| `moddev_version` | <https://projects.neoforged.net/neoforged/moddevgradle> |
| Gradle version | The Fabric announcement post for that Minecraft version |

A quick way to check the Java requirement without a browser:

```bash
curl -s https://piston-meta.mojang.com/mc/game/version_manifest_v2.json \
  | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['latest'])"
```

### Step 3 — Edit `gradle.properties` only

Every version lives in that one file. Change:

```properties
minecraft_version=26.3
minecraft_version_range=[26.3,26.4)
fabric_minecraft_version_range=~26.3
java_version=25

loom_version=...
loader_version=...
fabric_api_version=...

neoforge_version=...
neoform_version=...
moddev_version=...

publish_game_versions=26.3
```

Do **not** edit `fabric.mod.json` or `neoforge.mods.toml`. They read from here.

### Step 4 — Bump Gradle if the loaders ask for it

```bash
./gradlew wrapper --gradle-version 9.5.1 --distribution-type bin
```

Then re-pin the checksum, because supply-chain integrity matters more during an
upgrade, not less:

```bash
SHA=$(curl -sL https://services.gradle.org/distributions/gradle-9.5.1-bin.zip.sha256)
sed -i "s/^distributionSha256Sum=.*/distributionSha256Sum=$SHA/" gradle/wrapper/gradle-wrapper.properties
```

### Step 5 — Refresh and build

```bash
./gradlew --refresh-dependencies clean build
```

Expect compile errors. That is the job. Work through them:

1. **`common/` first.** It has the smallest surface and everything else depends
   on it.
2. **Then each loader.** Loader API breakage is usually smaller than vanilla
   breakage.
3. **Mixins last.** They fail at *runtime*, not compile time, so they are found
   by launching rather than building.

### Step 6 — Read the migration primer

NeoForge publishes a per-version primer covering **vanilla** changes. It is
useful whichever loader you use, because the vanilla breakage is identical:

<https://docs.neoforged.net/primer/docs/>

Fabric's porting notes cover Fabric API changes:

<https://docs.fabricmc.net/develop/porting/>

### Step 7 — Run everything

```bash
./gradlew :common:test        # fast, no game
./gradlew :fabric:runServer   # must stay up
./gradlew :neoforge:runServer # must stay up
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
```

Then join the dev server with an **unmodded** client if your mod is Shape A or D
(see [`02-sides-client-vs-server.md`](./02-sides-client-vs-server.md#6-the-four-installation-shapes)).

### Step 8 — Check mixins actually applied

A mixin that fails to apply is often silent. Search the log:

```
grep -iE "mixin|refmap" fabric/run/server/logs/latest.log | grep -iE "error|warn|fail"
```

The example mixin in this repo logs at debug level on server construction, which
is a cheap canary that the mixin pipeline is working at all.

### Step 9 — Update the docs and ship

- `README.md` — the supported-version badge and table
- `CHANGELOG.md` — a `### Changed` entry naming the Minecraft version
- `docs/UPGRADE-HISTORY.md` — append a row (see §6)

Then bump the mod version (**minor** at least, since the supported game version
changed) and release. See [`04-versioning-and-releases.md`](./04-versioning-and-releases.md).

---

## 3. Should I keep supporting the old version?

Three strategies. Pick deliberately.

| Strategy | How | Good for | Cost |
| --- | --- | --- | --- |
| **Move on** | `main` targets the newest version only | Hobby mods, fast-moving features | Cheapest. Old users stay on the old jar |
| **Maintenance branch** | `mc/1.21.1` branch kept alive for fixes | Mods used on big servers that upgrade slowly | Cherry-picks; two CI matrices |
| **Multi-version from one tree** | Stonecutter, or a version-per-branch generator | Library mods with many consumers | High. Only worth it if you have real demand |

This template is built for the first two. Branch naming convention:

```
main            -> current Minecraft
mc/1.21.1       -> long-term-support branch for the 1.21.1 line
chore/mc-26.3   -> in-progress port
```

Tag releases from a maintenance branch with the game version included:
`v1.4.2+mc1.21.1`.

---

## 4. The 2026 unobfuscation cliff (1.21.11 → 26.1)

This is the big one. Everything below changes in the same PR.

### Fabric build script — exact diff

```diff
 plugins {
-    id 'net.fabricmc.fabric-loom-remap' version "${loom_version}"
+    id 'net.fabricmc.fabric-loom' version "${loom_version}"
 }

 dependencies {
     minecraft "com.mojang:minecraft:${minecraft_version}"
-    mappings loom.officialMojangMappings()
-    modImplementation "net.fabricmc:fabric-loader:${loader_version}"
-    modImplementation "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}"
+    implementation "net.fabricmc:fabric-loader:${loader_version}"
+    implementation "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}"
 }

 tasks.withType(JavaCompile).configureEach {
-    it.options.release = 21
+    it.options.release = 25
 }
```

Three separate rules, and getting one wrong produces errors that point somewhere
else entirely:

1. **Plugin id.** `net.fabricmc.fabric-loom-remap` for obfuscated Minecraft
   (1.21.11 and older); `net.fabricmc.fabric-loom` for unobfuscated (26.1+).
2. **No `mappings` line.** There is nothing to map. Leaving it in fails.
3. **`modImplementation` → `implementation`.** The `mod*` configurations exist to
   remap mod dependencies; with no remapping they are wrong. Same for
   `modCompileOnly` → `compileOnly` and `modApi` → `api`.

### Everything else that changes

| Thing | Before (≤1.21.11) | After (26.1+) |
| --- | --- | --- |
| Java | 21 | **25** |
| Gradle | 8.x | **9.1+** (9.5.1 for 26.2) |
| Mappings | Yarn or Mojang official | None — names are real |
| Intermediary | Required | Gone |
| Mixin refmap | Generated and referenced | **Delete** `"refmap"` from your mixin JSON |
| NeoForge version | `21.11.x` | `26.1.x.y` (mirrors Minecraft) |
| Yarn-named code | `class_1234`, Yarn names | Must be migrated to Mojang names |

### If your code used Yarn mappings

You must migrate to Mojang (official) names. Two tools:

- **Loom's `migrateMappings` task** — semi-automated, does not handle Kotlin.
- **The Ravel IntelliJ plugin** — handles Kotlin, resolves ambiguity via the IDE.

Do this migration **on the old Minecraft version first**, as its own commit, and
verify the mod still builds and runs. Then bump the version in a second commit.
Doing both at once means every error is ambiguous.

> This template already uses Mojang names, so a port from here to 26.x does not
> need the mapping migration step.

### Recommended order for crossing the cliff

1. On `1.21.11`: migrate Yarn → Mojang names. Commit. Verify it still runs.
2. Switch the Loom plugin id and drop `mappings` / `mod*` configurations. Commit.
3. Bump Java 21 → 25 and Gradle. Commit.
4. Bump Minecraft, Fabric API, NeoForge. Commit.
5. Fix the vanilla API breakage using the 26.1 primer. This is the long part.
6. Delete refmap references from mixin configs. Verify mixins apply.

Five small commits beat one enormous one when you have to bisect a regression.

---

## 5. Known breakage in recent versions

Quick orientation before you start; the primers are authoritative.

### 1.21.11 → 26.1

- Minecraft is unobfuscated (see §4).
- Java 25, Gradle 9.1+.
- `ItemStackTemplate` introduced for pre-registry stack representation.
- `Screen#render` split — `Screen#extractRenderState` is the new shape.
- `ChunkPos` construction moved to factories: `ChunkPos.containing()`,
  `ChunkPos.pack()`.

### 26.1 → 26.2

- **Rendering rewritten around Vulkan.** Vanilla now supports an OpenGL and a
  Vulkan backend. Direct OpenGL calls must go through Blaze3D.
- `DestFactor`/`SourceFactor` merged into `BlendFactor`; `TextureFormat` →
  `GpuFormat`; `VertexFormat` is dynamic with up to 16 attributes.
- `MultiBufferSource` and direct vertex uploads are gone outside chunk
  rendering — use `SubmitNode` / `FeatureRenderer`.
- GUI moved off `Minecraft`: `Minecraft.getInstance().setScreen()` →
  `Minecraft.getInstance().gui.setScreen()`.
- Block/item IDs split into `BlockIds`, `BlockItemIds`, `ItemIds`.

**If your mod is server-side, almost none of this affects you.** That is a real
argument for keeping rendering out of your mod where you can.

---

## 6. Keep an upgrade log

Append a row to `docs/UPGRADE-HISTORY.md` every time. Future-you porting to
`27.1` will want to know how long `26.2` took and what broke.

| Date | From → To | Mod version | Effort | What broke |
| --- | --- | --- | --- | --- |
| 2026-06-20 | 26.1 → 26.2 | 0.3.0 → 0.4.0 | 3 h | Rendering rewrite; we only touched the HUD |

---

## 7. Upgrade checklist

Copy into the port PR:

```markdown
- [ ] Branch `chore/mc-<version>` created off `main`
- [ ] All versions updated in `gradle.properties` (and nowhere else)
- [ ] Gradle wrapper bumped + SHA-256 re-pinned
- [ ] `./gradlew --refresh-dependencies clean build` passes
- [ ] `:common:test` passes
- [ ] `:fabric:runServer` and `:neoforge:runServer` both stay up
- [ ] `:fabric:runClient` and `:neoforge:runClient` both reach the main menu
- [ ] Joined the dev server with an unmodded client (Shape A/D only)
- [ ] Mixins verified as applying (no mixin errors in the log)
- [ ] Migration primer read for every version stepped over
- [ ] README version table updated
- [ ] CHANGELOG entry added
- [ ] `docs/UPGRADE-HISTORY.md` row appended
- [ ] `publish_game_versions` updated
```

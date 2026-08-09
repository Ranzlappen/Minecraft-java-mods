# CLAUDE.md

Guidance for AI coding agents working in this repository. Human contributors
should read [`.github/CONTRIBUTING.md`](./.github/CONTRIBUTING.md) and
[`docs/00-START-HERE.md`](./docs/00-START-HERE.md) instead — this file assumes
that context and adds what an agent specifically needs.

---

## What this is

A multiloader Minecraft mod template: one codebase producing a Fabric jar and a
NeoForge jar for **Minecraft 26.2** (Java 25, Gradle 9.5.1).

```
common/     Loader-agnostic logic. Compiles against plain Minecraft via NeoForm.
fabric/     Fabric entrypoints. src/client/java is a separate, client-only source set.
neoforge/   NeoForge entrypoints. The .client package is client-only by convention.
```

Loader subprojects add `common/`'s source directories to their own source sets,
so common code is compiled once per loader rather than depended on as a jar.

---

## Non-negotiables

**1. Never import `net.minecraft.client` under `common/` or
`fabric/src/main/java`.** These run on dedicated servers, where those classes do
not exist. CI fails the build on this (`build.yml` → `side-safety`). Client code
goes in `fabric/src/client/java` or a `@Mod(dist = Dist.CLIENT)` class.

**2. Versions live only in `gradle.properties`.** `fabric.mod.json` and
`neoforge.mods.toml` use `${...}` placeholders expanded by `processResources`.
Never hard-code a version in a metadata file.

**3. The two version ranges are different grammars.** `minecraft_version_range`
is Maven (`[26.2,26.3)`) for NeoForge; `fabric_minecraft_version_range` is
Fabric's own (`~26.2`). Swapping them parses fine and fails at runtime.
`scripts/validate-metadata.py` catches it.

**4. Renaming the mod means running `./scripts/rename-mod.sh`.** Doing it by
hand misses the `META-INF/services` filenames and their contents, and the mixin
config name — all of which fail at runtime, not at build time.

**5. `Constants.MOD_ID` must equal `mod_id` in `gradle.properties`.**
Annotations cannot read Gradle properties, so the constant is a necessary
duplicate. The validator asserts they match.

---

## Verification

Local toolchain permitting, run these before claiming a change works:

```bash
python3 scripts/validate-metadata.py       # seconds, no Gradle
./scripts/selftest-rename.sh               # seconds, no Gradle
./gradlew :common:test                     # fast, no game
./gradlew build
./gradlew :fabric:runServer                # must reach "Done (…)! For help"
./gradlew :neoforge:runServer
```

**A build that compiles proves very little about a Minecraft mod.** The failure
mode that matters — a client class reached from server code — compiles cleanly
and crashes on a dedicated server. The server smoke test is the real check.

If you cannot run Gradle in your environment (it needs Java 25 and network
access to Mojang, Fabric, and NeoForged mavens), **say so plainly** rather than
implying the build was verified. The Python and shell tooling runs anywhere and
should always be run.

---

## Where things go

| Task | Location |
| --- | --- |
| Game logic, commands, config | `common/src/main/java/` |
| Something needing a loader API | Behind `IPlatformHelper`, implemented twice |
| Client rendering, GUI, keybinds | `fabric/src/client/java/` and `neoforge/.../client/` |
| Vanilla behaviour change with no event | `common/src/main/java/.../mixin/` |
| Unit-testable logic | Plain Java classes with no Minecraft imports |

Adding a method to `IPlatformHelper` requires implementing it in **both**
`FabricPlatformHelper` and `NeoForgePlatformHelper`, plus the `META-INF/services`
entry if the class is new. Keep that interface small.

---

## Conventions

- **Comments explain why, not what.** The existing comments are the standard;
  match their density and tone. Do not add comments that restate the code.
- **Conventional Commits**, scopes: `command`, `config`, `network`, `mixin`,
  `fabric`, `neoforge`, `common`, `build`, `ci`, `docs`, `deps`.
- **Mixin methods are prefixed** with the mod id: `examplemod$onTick`.
- Four-space indent, 120-column soft limit, UTF-8, LF.
- Every user-visible change gets a `CHANGELOG.md` entry under `## [Unreleased]`
  in the same commit.

---

## Facts that are easy to get wrong

The modding ecosystem changed substantially in 2026. Training data older than
that will be confidently wrong about these:

| | |
| --- | --- |
| **Versioning** | Calendar-based since 2026: `26.1`, `26.2`, `26.3`. `1.21.11` was the last `1.x`. `26.2` is *newer* than `1.21.11` despite sorting lower. |
| **Obfuscation** | **Removed in 26.1.** No mappings, no intermediary, no refmaps. Yarn is unmaintained past 1.21.11. |
| **Loom plugin id** | `net.fabricmc.fabric-loom` for 26.1+; `net.fabricmc.fabric-loom-remap` for ≤1.21.11. |
| **Fabric dependencies** | `implementation`, not `modImplementation`, on unobfuscated versions. No `mappings` line. |
| **Java** | 25 for 26.1+. 21 for 1.20.5–1.21.11. |
| **`displayTest`** | **Gone** from `neoforge.mods.toml`. Connection matching is derived from payload registration; use `PayloadRegistrar#optional()` for a mod vanilla clients should still be able to join with. |
| **NeoForge versions** | Mirror Minecraft: `26.2.0.57` for Minecraft 26.2. |

Do not "correct" these back to the pre-2026 forms. If unsure, check
`scripts/check-toolchain-updates.py` output or the upstream sources listed in
[`docs/03-build-and-run.md`](./docs/03-build-and-run.md#where-versions-come-from).

---

## Do not

- Bump Minecraft, Fabric API, or NeoForge versions as a routine dependency
  update. That is a **port**, not a bump — follow
  [`docs/05-upgrading-minecraft-versions.md`](./docs/05-upgrading-minecraft-versions.md)
  and expect compile errors. Dependabot is configured to ignore them for this
  reason.
- Add a runtime dependency to `common/` without a strong reason. It ships to
  every user of both jars.
- Use `@Overwrite` in a mixin when `@Inject` would do — it makes your mod
  exclusive with every other mod touching that method.
- Commit anything under `run/`, or the datagen `.cache/` directory.
- Weaken a CI check to make a build pass.

# Minecraft Java mod template

[![Build](https://github.com/Ranzlappen/Minecraft-java-mods/actions/workflows/build.yml/badge.svg)](https://github.com/Ranzlappen/Minecraft-java-mods/actions/workflows/build.yml)
[![Security scan](https://github.com/Ranzlappen/Minecraft-java-mods/actions/workflows/security-scan.yml/badge.svg)](https://github.com/Ranzlappen/Minecraft-java-mods/actions/workflows/security-scan.yml)
[![Minecraft 26.2](https://img.shields.io/badge/Minecraft-26.2-brightgreen)](./gradle.properties)
[![Fabric + NeoForge](https://img.shields.io/badge/loaders-Fabric%20%2B%20NeoForge-blue)](./docs/01-architecture.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](./LICENSE)

A production-shaped starting point for **Minecraft Java mods**, targeting
**Minecraft 26.2** on both **Fabric** and **NeoForge** from one codebase —
with the documentation, CI, and release automation already wired up. It ships
with a real, working mod (**Status Overlay**, a client-side HUD) rather than a
throwaway "hello world", so every mechanism in the template — the multiloader
split, the platform abstraction, the CI smoke tests — is exercised by
something that actually does something.

> **New here? → [`docs/00-START-HERE.md`](./docs/00-START-HERE.md)**

---

## Quick start

```bash
git clone https://github.com/Ranzlappen/Minecraft-java-mods.git my-mod
cd my-mod

# Make it yours (renames package, mod id, services files, mixin config, assets)
./scripts/rename-mod.sh --id coolmod --name "Cool Mod" \
                        --group com.yourname.coolmod --author "Your Name"

# Launch a dev client with the mod loaded
./gradlew :fabric:runClient
```

You do **not** need a specific JDK installed — Gradle downloads the right one.
The first build takes 5–20 minutes while Minecraft is fetched and decompiled;
after that it is seconds.

In game, look at the top-left corner of the HUD — a "Saturation: X.X" line is
already there. Mount a horse and a "Speed: X.X blocks/s" line joins it. Both
are Status Overlay, the mod this template ships with, reading data the client
already has and drawing it on screen — no server component required.

---

## What you get

| | |
| --- | --- |
| **One codebase, two loaders** | `common/` holds the real logic and compiles against plain Minecraft. `fabric/` and `neoforge/` are thin adapters. Adding a third loader touches no shared code. |
| **Side safety enforced by the build** | Loom's split source sets make "client code on a server" a *compile error* on Fabric, and CI greps for it everywhere else. This is the bug that breaks most first mods. |
| **Working example** | Status Overlay: a client-side food-saturation and mounted-horse-speed HUD, unit-tested formatting logic, a mixin, and access-transformer plumbing. Small, but every mechanism is present and wired. |
| **CI that means something** | Compiling proves little for a mod. Every PR boots a real dedicated server on **both** loaders and fails if it does not reach the ready state or logs a mixin failure. |
| **Release automation** | One click bumps the version, rewrites the changelog, tags, builds, publishes a GitHub Release, and uploads to Modrinth and CurseForge. |
| **Keeps itself current** | A weekly job checks Minecraft, Fabric, and NeoForge upstream and opens a tracking issue when something newer exists — Dependabot cannot see these. |
| **Documentation that answers real questions** | Twelve documents, including a full treatment of who has to install what, and a step-by-step Minecraft version upgrade guide. |

---

## Supported versions

| Minecraft | Java | Loaders | Status |
| --- | --- | --- | --- |
| **26.2** | 25 | Fabric `0.19.3` · NeoForge `26.2.0.57` | Current (`main`) |

Minecraft moved to calendar versioning in 2026 and **dropped obfuscation** in
26.1 — the biggest change to modding in years. If you are porting a mod from
`1.21.x`, read
[the unobfuscation section](./docs/05-upgrading-minecraft-versions.md#4-the-2026-unobfuscation-cliff-12111--261)
before anything else: the Loom plugin id, the mappings line, the dependency
configurations, Java, and Gradle all change together.

---

## Do my players need to install this?

The question every mod page gets asked. There are four answers, and picking one
early decides your metadata, your networking, and your support load:

| Shape | Server | Client | Typical mod |
| --- | --- | --- | --- |
| **A** | required | not needed | Anti-grief, economy, `/home`, custom loot |
| **B** | not needed | required | Minimap, HUD tweak, performance mod |
| **C** | required | **required, matching version** | Blocks, items, entities, machines |
| **D** | required | optional, adds polish | A server mod with a nicer UI if you have it |

**This template ships as Shape B** — Status Overlay is entirely client-side.
Both HUD lines read data the client already has (its own food data, the
entity it is riding), so the server needs nothing and does not even know the
mod exists. See [`docs/02-sides-client-vs-server.md`](./docs/02-sides-client-vs-server.md#6-the-four-installation-shapes)
for the other three shapes and how to build them from this same template.

A lot of what looks like it needs a client mod does not: server resource packs,
item components, display entities, bossbars and scoreboards cover a surprising
amount. Full treatment, including how to keep vanilla clients able to connect:
[`docs/02-sides-client-vs-server.md`](./docs/02-sides-client-vs-server.md).

---

## Layout

```
gradle.properties      Every version number. The only file a Minecraft upgrade edits.
common/                Loader-agnostic logic. Most of your mod. Also unit-tested here.
fabric/                Fabric entrypoints; src/client/java is client-only, enforced.
neoforge/              NeoForge entrypoints; .client package is client-only.
docs/                  The documentation set below.
scripts/               rename-mod.sh, metadata validation, release tooling, self-tests.
.github/workflows/     CI, release, version bump, toolchain watch, security.
```

---

## Documentation

| | |
| --- | --- |
| [00 — Start here](./docs/00-START-HERE.md) | First run, where to put code, "I want to… → go here" |
| [01 — Architecture](./docs/01-architecture.md) | Why three subprojects; the platform abstraction; NeoForge's two event buses |
| [02 — Sides](./docs/02-sides-client-vs-server.md) | **Read this one.** Physical vs logical sides, who installs what, optional payloads |
| [03 — Build and run](./docs/03-build-and-run.md) | Commands, run configs, where versions come from, IDE notes |
| [04 — Versioning and releases](./docs/04-versioning-and-releases.md) | Semver, Conventional Commits, cutting a release |
| [05 — Upgrading Minecraft](./docs/05-upgrading-minecraft-versions.md) | **The porting guide.** Routine bumps, and the 2026 unobfuscation cliff |
| [06 — Mixins and ATs](./docs/06-mixins-and-access-transformers.md) | When to use them, how to target stably, reading a mixin crash |
| [07 — Testing](./docs/07-testing.md) | Unit tests, GameTest, the manual pass, what CI covers |
| [08 — Networking](./docs/08-networking.md) | Custom payloads, and keeping vanilla clients able to connect |
| [09 — Publishing](./docs/09-publishing.md) | Modrinth, CurseForge, GitHub Releases, environment flags |
| [10 — Troubleshooting](./docs/10-troubleshooting.md) | Symptom-first index of the usual failures |
| [11 — Glossary](./docs/11-glossary.md) | Mixin, refmap, intermediary, NeoForm, dist… |
| [Upgrade history](./docs/UPGRADE-HISTORY.md) | A row per port. Fill it in as you go |

---

## Everyday commands

```bash
./gradlew build                    # both jars -> */build/libs/
./gradlew :common:test             # fast unit tests, no game
./gradlew :fabric:runClient        # dev client
./gradlew :fabric:runServer        # dev dedicated server
./gradlew :neoforge:runClient
./gradlew :neoforge:runServer
./gradlew :neoforge:runData        # regenerate datagen output

python3 scripts/check-toolchain-updates.py   # is anything upstream newer?
python3 scripts/validate-metadata.py         # mod metadata sane?
./scripts/selftest-rename.sh                 # rename script still correct?
```

---

## Contributing

See [`.github/CONTRIBUTING.md`](./.github/CONTRIBUTING.md). The short version:
shared logic in `common/`, never import `net.minecraft.client` there, test on a
dedicated server, add a changelog entry.

Built to the conventions in
[Ranzlappen/repo-standards](https://github.com/Ranzlappen/repo-standards) —
SHA-pinned actions, least-privilege workflow permissions, job timeouts,
dependency review, CodeQL, and secret scanning.

## Licence

MIT — see [`LICENSE`](./LICENSE).

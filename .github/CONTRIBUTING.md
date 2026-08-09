# Contributing

Thanks for taking the time.

## Before you start

- **New to the codebase?** Read [docs/00-START-HERE.md](../docs/00-START-HERE.md).
- **Anything touching client or server behaviour?** Read
  [docs/02-sides-client-vs-server.md](../docs/02-sides-client-vs-server.md)
  first. It is short and it prevents the most common class of bug here.
- **Large change?** Open an issue first so we can agree on the shape before you
  spend the time.

## Setup

```bash
git clone https://github.com/Ranzlappen/Minecraft-java-mods.git
cd Minecraft-java-mods
./gradlew :fabric:runClient
```

You do not need a specific JDK installed — Gradle downloads the right one.
First build takes 5–20 minutes.

## The rules that matter

1. **Shared logic goes in `common/`.** Only put code in `fabric/` or
   `neoforge/` when it genuinely needs that loader's API.
2. **Never reference `net.minecraft.client` from `common/`.** CI fails the
   build if you do. Client code belongs in `fabric/src/client/java` or a
   `@Mod(dist = Dist.CLIENT)` class.
3. **Versions live in `gradle.properties` and nowhere else.** Metadata files
   use `${...}` placeholders.
4. **Test on a dedicated server**, not just in singleplayer. Singleplayer runs
   both logical sides in one process and hides side bugs completely.
5. **Add a CHANGELOG entry** under `## [Unreleased]` in the same PR.

## Style

- Match the surrounding code. Four-space indent, 120-column soft limit.
- Comments should explain *why*, not restate the code. The existing comments are
  the standard to aim at.
- Prefix mixin methods with the mod id: `examplemod$onTick`.
- Conventional Commits for commit messages —
  [docs/04-versioning-and-releases.md](../docs/04-versioning-and-releases.md#2-conventional-commits).

## Before opening a PR

```bash
./gradlew build
./gradlew :common:test
./gradlew :fabric:runServer     # must stay up
./gradlew :neoforge:runServer   # must stay up
python3 scripts/validate-metadata.py
```

The PR template has the full checklist.

## What CI checks

Metadata validity, side safety, both builds, unit tests, a dedicated-server
smoke test on both loaders, workflow linting, shellcheck, the rename self-test,
link checking, CodeQL, and secret scanning.

## Conduct

By participating you agree to the [Code of Conduct](./CODE_OF_CONDUCT.md).

# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
with the Minecraft version appended as build metadata (`0.1.0+mc26.2`).

Write entries for players, not for yourself: "fixed a crash when joining with a
full inventory" beats "fixed a null pointer in the config loader".

## [Unreleased]

### Added

- Food saturation HUD overlay, always visible outside spectator mode.
- Mounted-horse speed HUD overlay (blocks/second), visible while riding a
  horse, donkey, mule, or llama.

### Changed

- Renamed the template's example mod to **Status Overlay** and re-shaped it
  from a server-side `/playtime` command (Shape A/D) to a client-only HUD mod
  (Shape B) — see `docs/02-sides-client-vs-server.md#6-the-four-installation-shapes`.
  `fabric.mod.json` now declares `"environment": "client"` accordingly.

### Removed

- The `/playtime` command, its JSON config, and the unit-tested
  `PlaytimeTracker` — replaced by the overlays above.

### Fixed

## [0.1.0] - 2026-08-09

### Added

- Initial multiloader template targeting Minecraft 26.2 on Fabric and NeoForge.
- Shared `common/` subproject compiled against plain Minecraft via NeoForm, so
  loader-agnostic code is enforced by the compiler rather than by convention.
- `ServiceLoader`-based platform abstraction (`IPlatformHelper`) with Fabric and
  NeoForge implementations.
- Example server-side feature: `/playtime` command, JSON config, and a
  unit-tested `PlaytimeTracker` with no Minecraft dependencies.
- Mixin and Access Transformer plumbing shared by both loaders.
- CI covering metadata validation, side safety, build, unit tests, and a
  dedicated-server smoke test on both loaders.
- Release automation: version bump, changelog generation, GitHub Releases, and
  optional Modrinth/CurseForge publishing.
- Weekly toolchain watcher that opens an issue when a new Minecraft, Fabric, or
  NeoForge version is available.
- `scripts/rename-mod.sh` with a self-test covering the rename steps that only
  fail at runtime.
- Documentation set covering architecture, sides and installation shapes,
  building, versioning, Minecraft version upgrades, mixins, testing, networking,
  publishing, troubleshooting, and a glossary.

[Unreleased]: https://github.com/Ranzlappen/Minecraft-java-mods/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/Ranzlappen/Minecraft-java-mods/releases/tag/v0.1.0

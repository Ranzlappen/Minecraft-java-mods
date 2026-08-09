# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
with the Minecraft version appended as build metadata (`0.1.0+mc26.2`).

Write entries for players, not for yourself: "fixed a crash when joining with a
full inventory" beats "fixed NPE in PlaytimeTracker".

## [Unreleased]

### Added

### Changed

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

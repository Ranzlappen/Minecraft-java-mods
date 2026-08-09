# Upgrade history

One row per Minecraft version port. Append a row every time you complete one —
future-you porting to the next version will want to know how long the last one
took and what actually broke.

Process: [`05-upgrading-minecraft-versions.md`](./05-upgrading-minecraft-versions.md)

| Date | From → To | Mod version | Java | Gradle | Effort | What broke |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-08-09 | — → 26.2 | 0.1.0 | 25 | 9.5.1 | — | Initial template. Targets 26.2 from the start. |

<!--
Row template:

| YYYY-MM-DD | 26.2 → 26.3 | 0.4.0 → 0.5.0 | 25 | 9.5.1 | 3 h | Short notes on what needed changing. |

Worth recording:
  - Anything that took more than 30 minutes to work out
  - Mixins that stopped applying, and what the new target was
  - APIs that moved, with old -> new names
  - Loader-specific surprises
-->

---

## Reference: the version landscape

| Minecraft | Java | Obfuscated | Fabric Loom plugin | NeoForge line |
| --- | --- | --- | --- | --- |
| 26.2 | 25 | No | `net.fabricmc.fabric-loom` | `26.2.x.y` |
| 26.1 | 25 | No | `net.fabricmc.fabric-loom` | `26.1.x.y` |
| 1.21.11 | 21 | **Yes** | `net.fabricmc.fabric-loom-remap` | `21.11.x` |
| 1.21.1 (LTS) | 21 | **Yes** | `net.fabricmc.fabric-loom-remap` | `21.1.x` |

`1.21.11` was the last release using `1.x` numbering and the last obfuscated
release. `26.1` (March 2026) was the first calendar-versioned, unobfuscated one.
Versions before 2026 were not renumbered, so `26.2` is newer than `1.21.11`
despite sorting lower as a string.

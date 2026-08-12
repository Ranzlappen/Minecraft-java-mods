# Testing

Three layers, cheapest first. Most modders skip all three; doing even the first
one puts you ahead.

---

## 1. Plain unit tests — fast, no game

```bash
./gradlew :common:test
```

Tests live in `common/src/test/java/`. They run in milliseconds because nothing
Minecraft-related loads.

This is why `OverlayFormat` has no Minecraft imports. It is deliberate: game
logic expressed in plain Java is logic you can test.

```java
@Test
void blocksPerSecondScalesByTicksPerSecond() {
    assertEquals(4.3, OverlayFormat.blocksPerSecond(0.215), 1e-9);
}
```

### Designing for this

Push the interesting decision out of the Minecraft-facing code:

```java
// Hard to test: takes a ServerPlayer, reads the world, sends a packet.
public void handleJoin(ServerPlayer player) { /* 40 lines */ }

// Easy to test: takes a UUID and a timestamp, returns a decision.
public JoinDecision decide(UUID id, long now, PlayerRecord record) { /* pure */ }
```

The thin Minecraft-facing wrapper stays untested; the logic inside gets covered.

---

## 2. GameTest — real game, automated

NeoForge ships a GameTest framework that runs tests inside a real server against
a real world, with no human. Good for block interactions, redstone behaviour,
entity AI, and anything involving actual world state.

`neoforge/build.gradle` already enables the unit-test integration:

```groovy
neoForge {
    unitTest {
        enable()
        testedMod = mods.named(mod_id)
    }
}
```

A GameTest looks like:

```java
@GameTest
public static void playerJoinIsTracked(GameTestHelper helper) {
    // arrange the world, act, then:
    helper.succeedWhen(() -> helper.assertTrue(condition, "message"));
}
```

Run them with a `gameTestServer` run configuration. Full docs:
[NeoForge — Game Tests](https://docs.neoforged.net/docs/misc/gametest/).

Fabric has no equivalent built in; the community
[fabric-gametest-api](https://github.com/FabricMC/fabric) module covers part of
it. In a multiloader repo it is reasonable to run GameTests only on the NeoForge
side — the logic under test lives in `common/` anyway.

---

## 3. Manual testing — the part you cannot skip

Automated tests do not catch side violations, mixin failures, or "the item
renders as a purple cube". A short manual pass before each release:

```markdown
- [ ] :fabric:runClient reaches the main menu
- [ ] :neoforge:runClient reaches the main menu
- [ ] :fabric:runServer starts and stays up
- [ ] :neoforge:runServer starts and stays up
- [ ] Singleplayer: the feature works
- [ ] Multiplayer: connected a real client to the dev server, the feature works
- [ ] Unmodded client against the dev server (Shape A/D only) — connects, and
      degrades gracefully
- [ ] No mixin errors or warnings in either server log
- [ ] Config file is created on first run, and a corrupted one does not crash
- [ ] Rejoining preserves state that should persist
```

CI runs the build and boots a dedicated server on every PR, but it cannot log in
and play.

---

## 4. What CI checks

`.github/workflows/build.yml`, on every push and PR:

| Job | Checks |
| --- | --- |
| `build` | Both jars compile; unit tests pass; artifacts uploaded |
| `server-smoke` | Both dedicated servers start, tick, and shut down cleanly |
| `side-safety` | No `net.minecraft.client` import anywhere under `common/` |
| `metadata` | `fabric.mod.json` / `neoforge.mods.toml` expand and parse; no unresolved `${...}` |

The `side-safety` job is worth calling out: it is a two-line grep that catches
the single most common cause of "works for me, crashes on the server".

---

## 5. Debugging a running game

Launch a run configuration from IntelliJ in debug mode. Breakpoints work,
including inside Minecraft's own code — which, since 26.1, is fully decompiled
and readable.

Useful JVM flags for the run configuration:

```
-Dmixin.debug.export=true    # dump transformed classes to run/.mixin.out
-Dmixin.debug.verbose=true   # verbose mixin logging
```

For a server you cannot attach an IDE to, remote debugging:

```
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

---

## 6. Logging

Minecraft ships SLF4J; use `Constants.LOG`. No dependency needed.

```java
Constants.LOG.debug("Player {} joined at {}", uuid, now);   // dev only
Constants.LOG.info("Loaded config from {}", path);          // startup facts
Constants.LOG.warn("Config value out of range, clamping");  // recoverable
Constants.LOG.error("Could not write config", exception);   // broken
```

Use `{}` placeholders rather than string concatenation — the arguments are only
formatted if the level is enabled.

Do not log per-tick at `info`. Twenty times a second, on a server with a hundred
players, is how you fill someone's disk.

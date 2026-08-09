# Sides: client, server, and who needs to install what

> The single most important document in this repo. Nearly every "works in
> singleplayer, crashes on my server" bug traces back to something on this page.

---

## 1. Two different things are both called "sides"

Minecraft uses the word "side" for two unrelated concepts. Mixing them up is the
root cause of most modding confusion.

### Physical side — *which program is running*

| | Physical client | Physical (dedicated) server |
| --- | --- | --- |
| What it is | The game you launch from the launcher | The `server.jar` you run in a terminal |
| Has rendering, sound, input | Yes | No |
| Has `net.minecraft.client` classes | Yes | **No** |
| Touching a client class | Fine | `NoClassDefFoundError` → crash |

### Logical side — *which half of the game code is running*

| | Logical client | Logical server |
| --- | --- | --- |
| Responsible for | Displaying things | Deciding things |
| Owns | Rendering, the local view of the world | Inventories, entities, weather, time, damage |
| Authoritative | No | **Yes** |

### The part that trips everyone up

**A singleplayer world runs both logical sides inside one physical client.**

When you press "Create New World", your physical client starts an *integrated
server* — a full logical server, in the same JVM, on a different thread. Your
logical client then connects to it, conceptually like a `localhost` connection.

So:

- Physical server → always logical server.
- Physical client → logical client, **and** logical server too, in singleplayer.

That is why testing in singleplayer proves almost nothing about whether your mod
works on a real server. In singleplayer, a client class is always available to
your "server" code, because there is no separation to violate. On a dedicated
server the class is simply not in the jar.

> **Rule:** Run `./gradlew :neoforge:runServer` and `./gradlew :fabric:runServer`
> before every release. CI does this too (`.github/workflows/build.yml`), but a
> local run catches it minutes instead of hours later.

---

## 2. Which check do I use?

There are two checks and they answer different questions. Using the wrong one is
a bug even when it appears to work.

### `level.isClientSide()` — the default, 95% of the time

Answers: *is this the logical client?*

```java
if (!level.isClientSide()) {
    // Logical server. Change the world here: damage entities, move items,
    // set blocks, award advancements. This is the authoritative copy.
}
```

Use it whenever you have a `Level`. Running game logic on the logical client
causes desync: the player sees a block break that the server never agreed to,
then it pops back a tick later.

**Do not** use it to decide whether client classes are safe to touch. It returns
`false` for the integrated server inside a physical client, where client classes
*are* present, and `false` on a dedicated server, where they are not. It tells
you nothing about the physical side.

### The physical check — rare, and only for class safety

Answers: *am I in a process that has client classes at all?*

```java
// Behind the platform abstraction so common code can call it:
if (!Services.PLATFORM.isDedicatedServer()) {
    // Safe to touch net.minecraft.client here.
}
```

Under the hood that is `FMLEnvironment.getDist() == Dist.DEDICATED_SERVER` on
NeoForge and `FabricLoader.getInstance().getEnvironmentType()` on Fabric.

**Important caveat:** an `if` check is *not* enough on its own. The JVM verifies
and loads a class when it first reaches a method that references it — which can
happen before your `if` ever executes. The reliable pattern is to put the
client-only code in a **separate class** and only reach that class from a
client-only entrypoint. See §4.

---

## 3. Decision table — where does my code go?

| What you are writing | Where it goes | Why |
| --- | --- | --- |
| Game rules, commands, loot, world changes | `common/src/main/java` | Runs on the logical server; portable |
| Registration (items, blocks, entities) | `common/` + a loader hook | Both sides register; the server is authoritative |
| Rendering, models, particles, sound | Fabric `src/client/java` / NeoForge `client` package | Client classes; server does not have them |
| Keybinds, screens, HUD | Fabric `src/client/java` / NeoForge `client` package | Input and GUI only exist on a client |
| Config that affects gameplay | `common/`, loaded on the server | The server must be the source of truth |
| Config that affects only visuals | Client-side config file | The server has no opinion on your HUD colour |
| Networking payload **type** | `common/` | Both sides must agree on the format |
| Networking **send** from server | `common/` or loader hook | Server-side |
| Networking **receive** handler on client | Client-only class | Touches client state |

---

## 4. Compile-time enforcement

### Fabric gives you a real guarantee

`fabric/build.gradle` calls `splitEnvironmentSourceSets()`. That creates a second
source set, `src/client/java`, compiled against the client-only half of
Minecraft. Code in `src/main/java` **cannot see** code in `src/client/java`.

The result: calling a renderer from server-side code is a **compile error**. You
find out in three seconds, not from a bug report.

```
fabric/
  src/main/java/     <- both sides. No client classes visible.
  src/client/java/   <- client only. Can see src/main and client Minecraft.
```

### NeoForge does not — you get discipline instead

NeoForge has one source set. The equivalent guarantee comes from splitting your
`@Mod` classes by dist:

```java
@Mod(Constants.MOD_ID)
public class ExampleModNeoForge {           // loaded on both
}

@Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
public class ExampleModNeoForgeClient {     // never loaded on a dedicated server
}
```

Multiple `@Mod` classes sharing one mod id is the documented, supported pattern.

Because nothing stops you referencing the client class from the common one,
**NeoForge is where the dedicated-server smoke test earns its keep**. This repo
keeps all NeoForge client code under a `.client` package as a visual signal, and
CI boots a dedicated server on every PR.

---

## 5. What makes a mod "server-side only"?

A mod is server-side only when a **vanilla client can play on the server and get
the full experience**. That is possible because a lot of Minecraft is already
driven entirely by the server over the vanilla protocol:

| Server-only feature | How the vanilla client sees it |
| --- | --- |
| Commands (`/playtime`) | Ordinary chat packets |
| Chat messages, action bar, titles | Vanilla packets |
| Teleports, gamemode, effects, damage | Vanilla state sync |
| Loot tables, recipes, advancements | Sent as datapack sync on join |
| Scoreboards, bossbars, team colours | Vanilla packets |
| World generation, block changes | Vanilla chunk packets |
| Custom item names / lore / components | Vanilla item sync |

The example mod in this repo is exactly this shape. `/playtime` works for a
player running an unmodded client, because the reply is a normal chat packet.

### The line you cannot cross without a client install

A **client install becomes mandatory** as soon as you need something the vanilla
protocol has no packet for:

- **New rendering** — custom entity models, shaders, particles, GUI screens.
- **New blocks/items with new appearances.** The server can send you an item, but
  a vanilla client has no model or texture for it and renders it as a missing
  model. (Some of this can be faked with resource packs and item components — see
  §7.)
- **New input** — keybinds. The server cannot read a key the client does not send.
- **Custom payloads.** If your feature depends on a packet you invented, both
  ends need code to understand it.
- **Client-side-only behaviour** — minimaps, FPS tweaks, sound replacers.

---

## 6. The four installation shapes

Decide which one you are building **before** you write code. It determines your
metadata, your networking, and your release notes.

### Shape A — Server-only

Install: server `mods/` folder. Clients need nothing.

Examples: anti-grief, economy, `/home` commands, world management, punishment
systems, custom loot.

```jsonc
// fabric.mod.json
"environment": "server"
```
```toml
# neoforge.mods.toml — do not register any required payloads.
# Use PayloadRegistrar#optional() if you register any at all.
```

> **Careful with `"environment": "server"` on Fabric.** It means "do not load
> this on a physical client at all". A player who drops the jar into their own
> client's `mods/` folder gets nothing — including in singleplayer, where the
> integrated server would otherwise have run it. If you want the mod to work in
> singleplayer too, use `"*"` and simply write no client code. **This template
> uses `"*"` for that reason.**

### Shape B — Client-only

Install: client `mods/` folder. The server needs nothing and usually does not
know the mod exists.

Examples: minimaps, HUD tweaks, performance mods, sound packs.

```jsonc
"environment": "client"
```

On NeoForge, keep everything under `@Mod(dist = Dist.CLIENT)` and register no
required payloads, so servers do not reject the client.

### Shape C — Both required (matched pair)

Install: **both** sides, at the **same version**. This is the default for
content mods that add blocks, items, entities, or machines.

Say so loudly in your README and on your Modrinth page — mismatched versions are
the number one support question for this shape.

- Fabric: `"environment": "*"`, and register your payloads normally so the
  handshake fails loudly on a mismatch.
- NeoForge: register payloads normally (not `optional()`); NeoForge refuses the
  connection when a required payload is missing on the other end.

### Shape D — Server required, client optional (the nicest shape)

Install: server required; client install adds polish but is not needed to play.

This is what a well-built server mod looks like. The server does everything over
vanilla packets; if it detects the mod on the client, it additionally sends a
custom payload to drive a nicer UI.

Requirements:

1. Register the custom payload as **optional** so vanilla clients can still
   connect (NeoForge: `registrar.optional()`; Fabric: check
   `ServerPlayNetworking.canSend(player, TYPE)` before sending).
2. Never assume the client has the mod. Always have a vanilla fallback path.
3. Send nothing custom until you have confirmed the client can receive it.

See [`08-networking.md`](./08-networking.md) for the code.

---

## 7. Faking client content without a client mod

Worth knowing before you commit to Shape C — you can often stay server-only:

- **Resource pack via `server-resource-pack`.** A server can require a resource
  pack, which is downloaded by vanilla clients automatically. Combined with item
  model overrides, this gives custom-looking items with no client mod.
- **Item components** (`custom_model_data`, `item_name`, `lore`, `custom_data`)
  let the server drive appearance on vanilla clients.
- **Display entities** (`block_display`, `item_display`, `text_display`) render
  arbitrary transformed blocks/items/text with no client code at all.
- **Custom bossbars, scoreboards, and titles** cover a lot of HUD needs.

Many "impossible without a client mod" features are achievable this way, and
Shape A is dramatically easier to support than Shape C.

---

## 8. NeoForge connection matching

Older Forge and early NeoForge had a `displayTest` field in `mods.toml` with
values like `MATCH_VERSION` and `IGNORE_ALL_VERSION`. **That field no longer
exists.**

Modern NeoForge derives the answer from your networking:

- Register a payload normally → both sides must have it → a client without your
  mod is refused, and a mismatched version fails the handshake.
- Register via `PayloadRegistrar#optional()` → the connection is allowed even if
  the other side does not know the payload.
- Register nothing → your mod never blocks a connection.

From the NeoForge source: *"If any non-optional payloads are missing during a
connection attempt, the connection will fail."*

So for a Shape A or Shape D mod, either register no payloads or mark them
optional. There is no metadata flag to set.

---

## 9. Fabric's `environment` field

| Value | Loaded on physical client | Loaded on dedicated server | Use for |
| --- | --- | --- | --- |
| `"*"` | Yes | Yes | Default. Shapes A (singleplayer-friendly), C, D |
| `"client"` | Yes | No | Shape B |
| `"server"` | **No** | Yes | Shape A, when you deliberately want it inert on clients |

The same field can be applied per-mixin-config, which is how you keep a
client-only mixin from being applied on a server:

```jsonc
"mixins": [
  "examplemod.mixins.json",
  { "config": "examplemod.client.mixins.json", "environment": "client" }
]
```

Unlike NeoForge, Fabric does **not** perform a mod-list handshake by default. A
Fabric server will happily accept a vanilla client unless a mod explicitly
enforces otherwise. That makes Shape A and D easy, and means Shape C has to
enforce matching itself (usually by registering a required payload).

---

## 10. Pre-release checklist

- [ ] `./gradlew :fabric:runServer` starts and stays up
- [ ] `./gradlew :neoforge:runServer` starts and stays up
- [ ] Connected to the dev dedicated server with a client that **does not** have
      the mod (for Shapes A and D) and it worked
- [ ] No `net.minecraft.client` import appears anywhere under `common/`
      (`grep -rn "net.minecraft.client" common/src/main/java` returns nothing —
      CI enforces this)
- [ ] README states which side(s) to install on, in the first paragraph
- [ ] Modrinth/CurseForge "Environments" set correctly (client: required/optional/unsupported, server: same)

---

## Further reading

- [NeoForge — Sides](https://docs.neoforged.net/docs/concepts/sides/)
- [Fabric — fabric.mod.json spec](https://wiki.fabricmc.net/documentation:fabric_mod_json_spec)
- [`08-networking.md`](./08-networking.md) — optional payloads in practice
- [`01-architecture.md`](./01-architecture.md) — why `common/` cannot see loader APIs

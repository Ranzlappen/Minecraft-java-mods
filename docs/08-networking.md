# Networking

How to send data between server and client — and, more importantly, how to do it
without forcing every player to install your mod.

> The mod this template ships, Status Overlay, does not use any of this — both
> HUD elements only read data the client already has, so there is no packet to
> send. This page is a worked example for when you *do* need one: a
> hypothetical `PlaytimePayload` that syncs a server-tracked value to the
> client, the shape a Shape D (§6 in [02](./02-sides-client-vs-server.md))
> feature usually takes.

---

## 1. First: do you actually need a packet?

A lot of what people write custom packets for is already sent by vanilla. Before
inventing a payload, check whether the server can just say it:

| Want | Vanilla mechanism | Works on unmodded clients |
| --- | --- | --- |
| Show text | Chat, action bar, title, subtitle | Yes |
| Show persistent numbers | Scoreboard sidebar, tab list, bossbar | Yes |
| Show something in the world | Display entities, particles, block updates | Yes |
| Custom item appearance | Item components + a server resource pack | Yes |
| Custom sounds | `playsound` with a server resource pack | Yes |
| Track state per player | Server-side storage + any of the above | Yes |

Every row you can use is one fewer reason for players to install anything. See
[02 §7](./02-sides-client-vs-server.md#7-faking-client-content-without-a-client-mod).

Custom payloads are for things with no vanilla equivalent: a bespoke GUI, a
data-heavy HUD, client input you invented.

---

## 2. The optional-payload rule

If you register a payload the ordinary way, **both sides must have your mod** or
the connection fails. NeoForge's own source says it plainly:

> If any non-optional payloads are missing during a connection attempt, the
> connection will fail.

So for a server mod that wants to keep supporting unmodded clients
([Shape D](./02-sides-client-vs-server.md#shape-d--server-required-client-optional-the-nicest-shape)),
you must do two things:

1. Register the payload as **optional**.
2. **Check before sending.** Never send a custom payload to a client that cannot
   decode it.

---

## 3. NeoForge

### Define the payload

```java
// Identifier, not ResourceLocation -- Mojang renamed it as part of the 26.1
// unobfuscation pass. See CLAUDE.md's "facts that are easy to get wrong".
public record PlaytimePayload(long millis) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlaytimePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(Constants.MOD_ID, "playtime"));

    public static final StreamCodec<FriendlyByteBuf, PlaytimePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, PlaytimePayload::millis,
                    PlaytimePayload::new);

    @Override
    public CustomPacketPayload.Type<PlaytimePayload> type() {
        return TYPE;
    }
}
```

### Register it — optional

```java
@SubscribeEvent  // MOD bus, not the game bus
public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
    final PayloadRegistrar registrar = event.registrar("1").optional();
    registrar.playToClient(PlaytimePayload.TYPE, PlaytimePayload.CODEC);
}
```

`.optional()` returns a *copy* of the registrar with optional mode on — assign
it, do not call it and discard the result.

The `"1"` is a protocol version string. Bump it when you change a payload's
shape, so old and new clients fail cleanly rather than misparsing.

### Handle it on the client only

```java
@SubscribeEvent  // MOD bus, in a Dist.CLIENT class
public static void onRegisterClientHandlers(RegisterClientPayloadHandlersEvent event) {
    event.register(PlaytimePayload.TYPE, ClientPlaytimeHandler::handle);
}
```

Handlers run on the main thread by default. For expensive work use
`registrar.executesOn(HandlerThread.NETWORK)`, then hop back before touching game
state.

### Send it, but only if they can receive it

```java
if (player.connection.hasChannel(PlaytimePayload.TYPE)) {
    PacketDistributor.sendToPlayer(player, new PlaytimePayload(millis));
}
```

That guard is the whole game. Without it, an unmodded client is disconnected.

---

## 4. Fabric

### Register the payload type — on both sides

```java
// In the MAIN entrypoint, so client and server both know the type.
PayloadTypeRegistry.playS2C().register(PlaytimePayload.TYPE, PlaytimePayload.CODEC);
```

### Handle it on the client

```java
// In the CLIENT entrypoint only.
ClientPlayNetworking.registerGlobalReceiver(PlaytimePayload.TYPE,
        (payload, context) -> context.client().execute(() -> {
            // Now on the render thread — safe to touch client state.
        }));
```

`context.client().execute(...)` is not optional. The handler arrives on a netty
thread; touching client state from there causes rare, unreproducible crashes.

### Send it, guarded

```java
if (ServerPlayNetworking.canSend(player, PlaytimePayload.TYPE)) {
    ServerPlayNetworking.send(player, new PlaytimePayload(millis));
}
```

Fabric does not perform a mod handshake by default, so an unguarded send is
usually ignored rather than fatal — but `canSend` is still correct, and it saves
the bandwidth.

---

## 5. Where each piece lives

| Piece | Location | Why |
| --- | --- | --- |
| Payload record + codec | `common/` | Both sides must agree on the wire format |
| Registration of the type | Loader entrypoint (both sides) | Loader-specific API |
| Server-side send | `common/`, called from a loader hook | Server logic |
| Client-side handler | Client-only source set / `Dist.CLIENT` class | Touches client state |

---

## 6. Rules that save you pain

**The server is authoritative.** Never trust a client packet. A client saying
"I mined 64 diamonds" is a claim, not a fact. Validate everything: is the player
in range, do they have permission, is the value sane?

**Rate-limit inbound packets.** A malicious client can send thousands per second.

**Keep payloads small.** They go to every relevant player, every time. Send
deltas, not full state.

**Do not send per-tick.** The example config uses a 30-second sync interval for
exactly this reason. If you think you need per-tick, you probably need a
different design.

**Version your protocol.** Bump the registrar version string when a payload
changes shape.

**Never send secrets.** Anything sent to a client is readable by that player.

---

## Further reading

- [NeoForge — Registering payloads](https://docs.neoforged.net/docs/networking/payload)
- [Fabric — Networking](https://docs.fabricmc.net/develop/networking)
- [02 §8](./02-sides-client-vs-server.md#8-neoforge-connection-matching) — how
  payload registration decides whether a client is allowed to connect

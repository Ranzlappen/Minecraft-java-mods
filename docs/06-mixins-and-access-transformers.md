# Mixins and Access Transformers

Minecraft was not designed to be extended. These are the two tools that let you
change it anyway. Both are powerful, both are the most fragile part of any mod,
and both should be your **last** resort.

---

## 1. Reach for these in order

1. **A loader event.** Stable, composable, cannot conflict with other mods.
   Check the NeoForge event list and the Fabric API callbacks first.
2. **A vanilla extension point.** Data packs, loot table modifiers, recipe types,
   `SavedData`, item components — a surprising amount is already pluggable.
3. **An Access Transformer**, if you only need visibility.
4. **A Mixin**, if you need to change behaviour.

Every Mixin you write is porting work you have signed up for on every future
Minecraft release.

---

## 2. Access Transformers

An AT rewrites an access modifier at load time so your code can reach a private
member.

`common/src/main/resources/META-INF/accesstransformer.cfg`:

```
public net.minecraft.server.MinecraftServer tickCount
public net.minecraft.world.entity.Entity setRot(FF)V
public-f net.minecraft.world.level.Level isClientSide
```

| Keyword | Effect |
| --- | --- |
| `public` | Make it public |
| `protected` | Make it protected |
| `public-f` | Make it public **and** strip `final` |

The descriptor (`(FF)V`) is only needed for methods, to disambiguate overloads.

Both loaders read this same file: NeoForge natively via `[[accessTransformers]]`
in `neoforge.mods.toml`, and the Fabric build is configured to consume it too.
One list, both jars.

`validateAccessTransformers = true` is set in both build scripts, so a typo or a
stale entry fails the build instead of silently doing nothing.

### When not to use one

ATs are **global**: they change the class for every mod in the instance. Usually
harmless, but stripping `final` from a field another mod assumes is final causes
genuinely baffling bugs.

If you only need to read one private field, an `@Accessor` mixin is narrower and
affects nobody else. Prefer that.

---

## 3. Mixins

Mixin rewrites Minecraft's bytecode at load time. Both loaders ship the same
library, which is why mixins live in `common/`.

### The pieces

**The config** — `common/src/main/resources/statusoverlay.mixins.json`:

```json
{
  "required": true,
  "minVersion": "0.8.7",
  "package": "com.statusoverlay.mod.mixin",
  "mixins": ["MinecraftServerMixin"],
  "client": [],
  "server": [],
  "injectors": { "defaultRequire": 1 }
}
```

- `mixins` — applied on both sides
- `client` — applied on the physical client only
- `server` — applied on the dedicated server only

Every mixin class must be listed, or it is silently ignored. Paths are relative
to `package`.

**Referenced from both loaders:**

```jsonc
// fabric.mod.json
"mixins": ["statusoverlay.mixins.json"]
```
```toml
# neoforge.mods.toml
[[mixins]]
config = "statusoverlay.mixins.json"
```

> **No `refmap` since 26.1.** Refmaps translated dev-time names to obfuscated
> runtime names. Minecraft is unobfuscated now, so they are unnecessary — leave
> the field out. If you are porting a pre-26.1 mod, deleting it is part of the
> job ([05 §4](./05-upgrading-minecraft-versions.md#4-the-2026-unobfuscation-cliff-12111--261)).

### The common injection types

```java
// Run extra code at a point in an existing method.
@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
private void onTick(CallbackInfo ci) {
    if (shouldSkip()) ci.cancel();
}

// Change a value being passed to a call or returned.
@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
private float scaleDamage(float amount) {
    return amount * 0.5f;
}

// Read a private field with no bytecode rewriting of behaviour.
@Mixin(SomeClass.class)
public interface SomeClassAccessor {
    @Accessor("privateField")
    int getPrivateField();
}

// Replace a method wholesale. Almost always the wrong tool: any other mod
// doing the same thing to the same method is now broken.
@Overwrite
public void doThing() { }
```

`@At` values worth knowing: `HEAD`, `TAIL`, `RETURN`, `INVOKE` (at a specific
call), `FIELD` (at a field access).

### Naming convention

Prefix injected methods with your mod id:

```java
private void statusoverlay$onServerConstructed(CallbackInfo ci) { }
```

Mixin merges your methods into the target class. Two mods that both add
`onTick()` to `MinecraftServer` collide. The prefix removes the whole category of
problem, and both loaders' style guides ask for it.

### `require`

```java
@Inject(method = "<init>", at = @At("TAIL"), require = 0)
```

`require = 0` means "do not fail if the injection point is missing". Right for
informational or cosmetic hooks. **Wrong** for anything your mod depends on —
there you want a loud crash at startup rather than a feature that quietly does
nothing.

`defaultRequire: 1` in the config sets the default for everything else.

### Targeting stable things

Some targets survive updates better than others:

| Target | Stability |
| --- | --- |
| Constructors (`<init>`) | Very stable — the name never changes |
| Public API methods | Stable |
| Private helper methods | Fragile — renamed freely |
| Lambdas and synthetic methods | Very fragile — avoid |

---

## 4. Debugging

### The mixin did not apply

1. Is the class listed in the mixins JSON?
2. Is it in the `package` declared there?
3. Is it in the right list — `mixins` vs `client` vs `server`?
4. Search the log:
   ```bash
   grep -iE "mixin" fabric/run/server/logs/latest.log | grep -iE "error|warn|fail"
   ```

The example `MinecraftServerMixin` in this repo logs at debug level when it
fires. If you never see that line, the pipeline itself is broken, not your mixin.

### Reading a mixin crash

```
Critical injection failure: @Inject annotation on onTick could not find any
targets matching 'tick' in net.minecraft.server.MinecraftServer
```

Means: the class was found, the method was not. Mojang renamed or removed it.
Open the decompiled class in your IDE (Ctrl-click the target) and find the new
name.

```
Mixin apply failed statusoverlay.mixins.json:MinecraftServerMixin -> ...
```

Means: the mixin applied but produced invalid bytecode. Usually a wrong
signature on the injected method.

### Useful JVM flags

```
-Dmixin.debug.export=true      # write transformed classes to run/.mixin.out
-Dmixin.debug.verbose=true     # verbose mixin logging
```

Add them to the run configuration's JVM arguments.

---

## 5. Playing well with other mods

- Prefer `@Inject` over `@Overwrite`. Overwrites are exclusive; injections stack.
- Prefix everything with your mod id.
- Use `cancellable = true` sparingly — cancelling a vanilla method can break
  other mods' assumptions.
- Keep injections small. Do the work in a normal class and call into it.
- If two mods must cooperate, add a real integration behind
  `Services.PLATFORM.isModLoaded("othermod")` instead of mixing into them.

---

## Further reading

- [NeoForge — Access Transformers](https://docs.neoforged.net/docs/advanced/accesstransformers/)
- [Fabric — Mixin introduction](https://wiki.fabricmc.net/tutorial:mixin_introduction)
- [The Mixin Javadoc](https://javadoc.io/doc/org.spongepowered/mixin) — the
  authoritative reference for every annotation

package com.example.examplemod.fabric.client;

import com.example.examplemod.Constants;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric's {@code client} entrypoint — runs on the physical client only.
 *
 * <p><b>Note the source set.</b> This file is in {@code src/client/java}, not
 * {@code src/main/java}. That is not a naming convention: Loom's
 * {@code splitEnvironmentSourceSets()} compiles this source set against the
 * client-only half of Minecraft, and excludes it from the server's view. The
 * practical effect is that calling client code from {@code src/main} is a
 * <i>compile error</i> rather than a crash on somebody's server three weeks
 * after release.
 *
 * <p>NeoForge has no equivalent split. There, the same guarantee comes from
 * {@code @Mod(dist = Dist.CLIENT)} plus discipline — see
 * {@code docs/02-sides-client-vs-server.md#the-two-loaders-differ}.
 *
 * <p><b>This mod does not need a client install.</b> Everything gameplay-facing
 * happens on the server, and vanilla clients get the full experience. This
 * entrypoint exists to show where client-side polish would go, and to keep the
 * plumbing ready if you add some.
 */
public final class ExampleModFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Constants.LOG.info("{} client-side extras active.", Constants.MOD_NAME);

        // Client-side things that would go here:
        //
        //   - Registering a keybind (KeyBindingHelper.registerKeyBinding)
        //   - A HUD overlay (HudLayerRegistrationCallback)
        //   - A client-only command (ClientCommandRegistrationCallback)
        //   - A receiver for a custom payload the server sends
        //     (ClientPlayNetworking.registerGlobalReceiver)
        //
        // If you add a payload receiver, register the payload type itself in
        // the *main* entrypoint, not here: both sides must agree on the type,
        // and the server needs it too. Only the receiving handler is client-side.
        // Worked example: docs/08-networking.md
    }
}

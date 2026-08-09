package com.example.examplemod.fabric;

import com.example.examplemod.Constants;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.command.PlaytimeCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/**
 * Fabric's {@code main} entrypoint — runs on <b>both</b> the physical client
 * and the dedicated server.
 *
 * <p>Declared in {@code fabric.mod.json} under {@code entrypoints.main}.
 *
 * <p>Everything here must be side-safe. Touching a {@code net.minecraft.client}
 * class from this file crashes a dedicated server with
 * {@code NoClassDefFoundError}. Client behaviour goes in
 * {@link com.example.examplemod.fabric.client.ExampleModFabricClient} instead,
 * which lives in the {@code src/client} source set and physically cannot be
 * loaded on a server.
 */
public final class ExampleModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // Shared setup first — config must be loaded before anything reads it.
        ExampleMod.init();

        // Commands. The lambda's third parameter tells you whether this is a
        // dedicated server or an integrated (singleplayer) one; we register on
        // both so /playtime works in singleplayer too.
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> PlaytimeCommand.register(dispatcher));

        // Player join/leave feed the tracker. These fire on the server only,
        // which is exactly where the state lives.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ExampleMod.playtime().onJoin(handler.getPlayer().getUUID(), System.currentTimeMillis()));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ExampleMod.playtime().onLeave(handler.getPlayer().getUUID(), System.currentTimeMillis()));

        // Clear state between worlds so singleplayer sessions do not bleed into
        // each other. On a dedicated server this fires once, at shutdown.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ExampleMod.playtime().reset());

        Constants.LOG.info("{} loaded on Fabric.", Constants.MOD_NAME);
    }
}

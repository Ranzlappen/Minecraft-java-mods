package com.example.examplemod.neoforge;

import com.example.examplemod.Constants;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.command.PlaytimeCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * NeoForge's main entrypoint — runs on <b>both</b> the physical client and the
 * dedicated server.
 *
 * <p>Discovered via the {@code @Mod} annotation; the id must match a
 * {@code [[mods]]} entry in {@code neoforge.mods.toml}.
 *
 * <p><b>Two event buses.</b> NeoForge has a mod bus (lifecycle: registration,
 * setup, datagen) and a game bus (gameplay: commands, ticks, players). The mod
 * bus is handed to your constructor; the game bus is reached with
 * {@code @EventBusSubscriber} or {@code NeoForge.EVENT_BUS}. Registering on the
 * wrong one is the most common NeoForge beginner bug — the handler simply never
 * fires. See {@code docs/01-architecture.md#neoforges-two-event-buses}.
 */
@Mod(Constants.MOD_ID)
public final class ExampleModNeoForge {

    /**
     * @param modBus the mod-lifecycle bus, injected by NeoForge. Other optional
     *               parameters ({@code ModContainer}, {@code Dist}) can be added
     *               in any order — NeoForge matches by type.
     */
    public ExampleModNeoForge(IEventBus modBus) {
        ExampleMod.init();
        Constants.LOG.info("{} loaded on NeoForge.", Constants.MOD_NAME);
        // Registration handlers (blocks, items, entities) would attach to modBus
        // here, e.g. DEFERRED_ITEMS.register(modBus).
    }

    /**
     * Game-bus handlers.
     *
     * <p>{@code @EventBusSubscriber} with no {@code value} registers on both
     * physical sides. All three events below only ever fire on a server, so no
     * side guard is needed — but note that "server" includes the integrated
     * server inside a singleplayer client.
     */
    @EventBusSubscriber(modid = Constants.MOD_ID)
    public static final class GameEvents {

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            PlaytimeCommand.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            ExampleMod.playtime().onJoin(event.getEntity().getUUID(), System.currentTimeMillis());
        }

        @SubscribeEvent
        public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
            ExampleMod.playtime().onLeave(event.getEntity().getUUID(), System.currentTimeMillis());
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            ExampleMod.playtime().reset();
        }

        private GameEvents() {
        }
    }
}

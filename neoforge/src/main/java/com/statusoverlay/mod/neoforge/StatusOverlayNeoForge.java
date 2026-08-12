package com.statusoverlay.mod.neoforge;

import com.statusoverlay.mod.Constants;
import com.statusoverlay.mod.StatusOverlayMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge's main entrypoint — runs on <b>both</b> the physical client and the
 * dedicated server.
 *
 * <p>Discovered via the {@code @Mod} annotation; the id must match a
 * {@code [[mods]]} entry in {@code neoforge.mods.toml}.
 *
 * <p>This mod has no gameplay logic, so unlike a typical NeoForge mod there is
 * nothing to attach to a game-bus {@code @EventBusSubscriber} here. Both HUD
 * overlays are registered from
 * {@link com.statusoverlay.mod.neoforge.client.StatusOverlayNeoForgeClient}
 * instead, on the mod bus, because GUI layer registration is a client-only
 * setup event — see {@code docs/01-architecture.md#neoforges-two-event-buses}.
 */
@Mod(Constants.MOD_ID)
public final class StatusOverlayNeoForge {

    /**
     * @param modBus the mod-lifecycle bus, injected by NeoForge. Unused here —
     *               kept in the signature because NeoForge requires this exact
     *               constructor shape to discover the mod at all.
     */
    public StatusOverlayNeoForge(IEventBus modBus) {
        StatusOverlayMod.init();
        Constants.LOG.info("{} loaded on NeoForge.", Constants.MOD_NAME);
    }
}

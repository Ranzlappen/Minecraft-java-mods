package com.statusoverlay.mod;

import com.statusoverlay.mod.platform.Services;

/**
 * Shared initialisation, called by every loader.
 *
 * <p>Each loader subproject has its own tiny entrypoint class whose only job is
 * to call {@link #init()} and to wire loader-specific events into the methods
 * on this class. Keeping the real work here means adding a third loader later
 * is a small job.
 *
 * <p><b>This class runs on both physical sides.</b> It must not touch any class
 * from {@code net.minecraft.client}. If you need client behaviour, put it in
 * the loader's client entrypoint instead —
 * {@code docs/02-sides-client-vs-server.md} explains the rules.
 *
 * <p><b>This mod has nothing to do on a dedicated server</b> — Shape B,
 * client-only, in {@code docs/02-sides-client-vs-server.md#shape-b--client-only}.
 * Both HUD overlays only read data the client already has (its own food data,
 * the entity it is riding) and draw it on screen: no gameplay logic, no config,
 * no network payload.
 *
 * <p>The two loaders enforce that differently. Fabric's {@code fabric.mod.json}
 * declares {@code "environment": "client"}, so Fabric Loader refuses to even
 * load this jar on a dedicated server — {@link #init()} never runs there.
 * NeoForge has no equivalent "refuse to load" toggle for a plain (non-{@code
 * dist = Dist.CLIENT}) {@code @Mod} class, so on NeoForge {@link #init()} does
 * run on a dedicated server, and logs that there is nothing for it to do.
 */
public final class StatusOverlayMod {

    /**
     * Called once, early, on both client and dedicated server.
     */
    public static void init() {
        Constants.LOG.info("Initialising {} on {}", Constants.MOD_NAME,
                Services.PLATFORM.getPlatformName());

        if (Services.PLATFORM.isDedicatedServer()) {
            Constants.LOG.info("{} is a client-only HUD mod; nothing to do on a dedicated server.",
                    Constants.MOD_NAME);
        }
    }

    private StatusOverlayMod() {
    }
}

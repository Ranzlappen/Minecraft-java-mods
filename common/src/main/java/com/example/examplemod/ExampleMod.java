package com.example.examplemod;

import com.example.examplemod.config.ExampleConfig;
import com.example.examplemod.platform.Services;
import com.example.examplemod.playtime.PlaytimeTracker;

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
 */
public final class ExampleMod {

    /**
     * Server-side playtime state.
     *
     * <p>Static because there is exactly one logical server per process. If you
     * later need per-world state, move this into a {@code SavedData} attached to
     * the overworld so it persists across restarts.
     */
    private static final PlaytimeTracker PLAYTIME = new PlaytimeTracker();

    /**
     * Called once, early, on both client and dedicated server.
     *
     * <p>Do registration and config loading here. Do <b>not</b> touch a
     * {@code Level} or a player — none exist yet at this point.
     */
    public static void init() {
        Constants.LOG.info("Initialising {} on {}", Constants.MOD_NAME,
                Services.PLATFORM.getPlatformName());

        ExampleConfig.load();

        if (!ExampleConfig.get().enabled) {
            Constants.LOG.warn("{} is disabled in its config; skipping feature setup.",
                    Constants.MOD_NAME);
        }
    }

    public static PlaytimeTracker playtime() {
        return PLAYTIME;
    }
}

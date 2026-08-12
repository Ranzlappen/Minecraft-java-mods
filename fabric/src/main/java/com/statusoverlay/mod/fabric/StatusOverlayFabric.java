package com.statusoverlay.mod.fabric;

import com.statusoverlay.mod.Constants;
import com.statusoverlay.mod.StatusOverlayMod;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric's {@code main} entrypoint — runs on <b>both</b> the physical client
 * and the dedicated server.
 *
 * <p>Declared in {@code fabric.mod.json} under {@code entrypoints.main}.
 *
 * <p>Everything here must be side-safe. Touching a {@code net.minecraft.client}
 * class from this file crashes a dedicated server with
 * {@code NoClassDefFoundError}. Client behaviour — which is where all of this
 * mod's actual features live — goes in
 * {@link com.statusoverlay.mod.fabric.client.StatusOverlayFabricClient} instead,
 * which lives in the {@code src/client} source set and physically cannot be
 * loaded on a server.
 */
public final class StatusOverlayFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        StatusOverlayMod.init();
        Constants.LOG.info("{} loaded on Fabric.", Constants.MOD_NAME);
    }
}
